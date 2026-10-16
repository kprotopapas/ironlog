package com.example.gymtracker

import android.app.Application
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.gymtracker.data.ActiveWorkout
import com.example.gymtracker.data.BodyweightEntry
import com.example.gymtracker.data.GymStore
import com.example.gymtracker.data.GymTimer
import com.example.gymtracker.data.PresetWorkout
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.genId
import com.example.gymtracker.data.prefilledSets
import com.example.gymtracker.data.sameDay
import com.example.gymtracker.data.withBodyweightEquipment
import com.example.gymtracker.data.withCurrentDefaults
import com.example.gymtracker.data.withCurrentName
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Tab(val label: String) { LOG("Log"), HISTORY("History"), PROGRESS("Progress"), PRESETS("Presets") }

class GymViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GymStore(app)

    var tab by mutableStateOf(Tab.LOG)

    var unit by mutableStateOf(store.loadUnit())
        private set

    var workouts by mutableStateOf(store.loadWorkouts())
        private set

    var presets by mutableStateOf(store.loadPresets())
        private set

    var bodyweight by mutableStateOf(store.loadBodyweight())
        private set

    /** Unlike the web version, this is persisted so a workout survives the app being killed. */
    var activeWorkout by mutableStateOf(store.loadActiveWorkout())
        private set

    /** Shared by every tab; persisted so it survives the app being killed. */
    var timer by mutableStateOf(store.loadTimer())
        private set
    private var timerAlarm: Job? = null

    init {
        // A countdown that ran out while the app was dead has nothing left to alert about.
        if (timer.finished(System.currentTimeMillis())) saveTimer(timer.reset()) else scheduleTimerAlarm()
        // One-off: pull-ups and dips logged before the Bodyweight equipment type existed.
        if (!store.isMigrated(MIGRATION_BODYWEIGHT)) {
            workouts = workouts.map { w -> w.copy(exercises = w.exercises.map { it.withBodyweightEquipment() }) }
            presets = presets.map { p -> p.copy(exercises = p.exercises.map { it.withBodyweightEquipment() }) }
            activeWorkout?.let { a -> setActive(a.copy(exercises = a.exercises.map { it.withBodyweightEquipment() })) }
            store.saveWorkouts(workouts)
            store.savePresets(presets)
            store.setMigrated(MIGRATION_BODYWEIGHT)
        }
        if (!store.isMigrated(MIGRATION_NAMES)) {
            workouts = workouts.map { w -> w.copy(exercises = w.exercises.map { it.withCurrentName() }) }
            presets = presets.map { p -> p.copy(exercises = p.exercises.map { it.withCurrentDefaults() }) }
            activeWorkout?.let { a -> setActive(a.copy(exercises = a.exercises.map { it.withCurrentName() })) }
            store.saveWorkouts(workouts)
            store.savePresets(presets)
            store.setMigrated(MIGRATION_NAMES)
        }
    }

    fun toggleUnit() {
        unit = unit.toggled()
        store.saveUnit(unit)
    }

    private fun setActive(active: ActiveWorkout?) {
        activeWorkout = active
        store.saveActiveWorkout(active)
    }

    fun updateActiveExercises(exercises: List<WorkoutExercise>) {
        activeWorkout?.let { setActive(it.copy(exercises = exercises)) }
    }

    fun renameActiveWorkout(name: String) {
        activeWorkout?.let { setActive(it.copy(name = name)) }
    }

    fun startWorkout(name: String = "Morning Workout", exercises: List<WorkoutExercise> = emptyList()) {
        setActive(ActiveWorkout(name, exercises, System.currentTimeMillis()))
        tab = Tab.LOG
    }

    /** Weights and reps come from each exercise's last session when there is one; see [prefilledSets]. */
    fun startFromPreset(p: PresetWorkout) {
        val exs = p.exercises.map { e ->
            WorkoutExercise(
                id = genId(),
                name = e.name,
                category = e.category,
                equipment = e.equipment,
                perSide = e.perSide,
                sets = prefilledSets(e, workouts, unit, bodyweight),
            )
        }
        startWorkout(p.name, exs)
    }

    /** Logs a weigh-in in the current unit, replacing any earlier one from the same day. */
    fun logBodyweight(weight: Double, date: Long = System.currentTimeMillis()) {
        if (weight <= 0) return
        bodyweight = (bodyweight.filterNot { sameDay(it.date, date) } + BodyweightEntry(date, weight, unit))
            .sortedBy { it.date }
        store.saveBodyweight(bodyweight)
    }

    fun deleteBodyweight(date: Long) {
        bodyweight = bodyweight.filter { it.date != date }
        store.saveBodyweight(bodyweight)
    }

    fun finishWorkout(bodyweightToday: Double? = null) {
        val active = activeWorkout ?: return
        val now = System.currentTimeMillis()
        bodyweightToday?.let { logBodyweight(it, now) }
        val w = Workout(
            id = genId(),
            name = active.name.ifEmpty { "Workout" },
            date = now,
            duration = (now - active.startTime) / 1000,
            exercises = active.exercises,
        )
        workouts = listOf(w) + workouts
        store.saveWorkouts(workouts)
        setActive(null)
        tab = Tab.HISTORY
    }

    fun discardWorkout() = setActive(null)

    fun updateWorkout(w: Workout) {
        workouts = workouts.map { if (it.id == w.id) w else it }
        store.saveWorkouts(workouts)
    }

    fun deleteWorkout(id: String) {
        workouts = workouts.filter { it.id != id }
        store.saveWorkouts(workouts)
    }

    fun savePreset(p: PresetWorkout) {
        presets = if (presets.any { it.id == p.id }) presets.map { if (it.id == p.id) p else it } else presets + p
        store.savePresets(presets)
    }

    fun deletePreset(id: String) {
        presets = presets.filter { it.id != id }
        store.savePresets(presets)
    }

    fun updateTimer(change: GymTimer.(now: Long) -> GymTimer) {
        saveTimer(timer.change(System.currentTimeMillis()))
        scheduleTimerAlarm()
    }

    private fun saveTimer(t: GymTimer) {
        timer = t
        store.saveTimer(t)
    }

    /** Alerts when a running countdown ends, then resets it so the same length is ready again. */
    private fun scheduleTimerAlarm() {
        timerAlarm?.cancel()
        if (!timer.running || !timer.countdown) return
        timerAlarm = viewModelScope.launch {
            delay(timer.remaining(System.currentTimeMillis()))
            saveTimer(timer.reset())
            alert()
        }
    }

    private fun alert() {
        val pattern = longArrayOf(0, 400, 200, 400, 200, 400)
        getApplication<Application>().getSystemService(Vibrator::class.java)?.let { v ->
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, -1))
            else @Suppress("DEPRECATION") v.vibrate(pattern, -1)
        }
        // ToneGenerator throws if the audio system can't give it a track; the vibration is enough then.
        runCatching { ToneGenerator(AudioManager.STREAM_ALARM, 80) }.getOrNull()?.let { tone ->
            tone.startTone(ToneGenerator.TONE_PROP_BEEP2, 1000)
            viewModelScope.launch {
                delay(1500)
                tone.release()
            }
        }
    }
}

private const val MIGRATION_BODYWEIGHT = "bodyweight-equipment"
private const val MIGRATION_NAMES = "equipment-free-names"
