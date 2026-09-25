package com.example.gymtracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.example.gymtracker.data.ActiveWorkout
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.GymStore
import com.example.gymtracker.data.PresetWorkout
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.genId

enum class Tab(val label: String) { LOG("Log"), HISTORY("History"), PRESETS("Presets") }

class GymViewModel(app: Application) : AndroidViewModel(app) {
    private val store = GymStore(app)

    var tab by mutableStateOf(Tab.LOG)

    var unit by mutableStateOf(store.loadUnit())
        private set

    var workouts by mutableStateOf(store.loadWorkouts())
        private set

    var presets by mutableStateOf(store.loadPresets())
        private set

    /** Unlike the web version, this is persisted so a workout survives the app being killed. */
    var activeWorkout by mutableStateOf(store.loadActiveWorkout())
        private set

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

    fun startFromPreset(p: PresetWorkout) {
        val exs = p.exercises.map { e ->
            WorkoutExercise(
                id = genId(),
                name = e.name,
                category = e.category,
                equipment = e.equipment,
                perSide = e.perSide,
                sets = List(e.defaultSets) { ExerciseSet(e.defaultReps, e.defaultWeight, unit, false) },
            )
        }
        startWorkout(p.name, exs)
    }

    fun finishWorkout() {
        val active = activeWorkout ?: return
        val now = System.currentTimeMillis()
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
}
