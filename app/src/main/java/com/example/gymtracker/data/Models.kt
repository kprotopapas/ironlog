package com.example.gymtracker.data

import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// ─── Types ───────────────────────────────────────────────────────────────────

enum class WeightUnit(val label: String) {
    KG("kg"), LBS("lbs");

    fun toggled() = if (this == KG) LBS else KG

    companion object {
        fun fromLabel(s: String?) = entries.firstOrNull { it.label == s } ?: KG
    }
}

enum class Equipment(val label: String) {
    BARBELL("Barbell"), DUMBBELL("Dumbbell"), CABLE("Cable"), SMITH("Smith machine");

    /** Only one-handed implements can be loaded per side (arm or leg); bars are always total. */
    val supportsPerSide get() = this == DUMBBELL || this == CABLE

    /** Dumbbell weight is conventionally logged per dumbbell. */
    val defaultPerSide get() = this == DUMBBELL

    companion object {
        fun fromName(s: String?) = entries.firstOrNull { it.name == s }
    }
}

/** e.g. "Dumbbell · Per", or null when no equipment is set. */
fun equipmentSummary(equipment: Equipment?, perSide: Boolean): String? =
    equipment?.let { if (perSide && it.supportsPerSide) "${it.label} · Per" else it.label }

data class ExerciseSet(
    val reps: Int,
    val weight: Double,
    val unit: WeightUnit,
    val done: Boolean,
)

data class WorkoutExercise(
    val id: String,
    val name: String,
    val category: String,
    val sets: List<ExerciseSet>,
    val notes: String = "",
    val equipment: Equipment? = null,
    val perSide: Boolean = false,
)

data class Workout(
    val id: String,
    val name: String,
    val date: Long, // epoch millis
    val duration: Long, // seconds
    val exercises: List<WorkoutExercise>,
)

data class PresetExercise(
    val name: String,
    val category: String,
    val defaultSets: Int,
    val defaultReps: Int,
    val defaultWeight: Double,
    val equipment: Equipment? = null,
    val perSide: Boolean = false,
)

data class PresetWorkout(
    val id: String,
    val name: String,
    val exercises: List<PresetExercise>,
)

data class ActiveWorkout(
    val name: String,
    val exercises: List<WorkoutExercise>,
    val startTime: Long, // epoch millis
)

// ─── Constants ───────────────────────────────────────────────────────────────

data class LibraryExercise(val name: String, val category: String, val equipment: Equipment? = null)

val EXERCISE_LIBRARY = listOf(
    LibraryExercise("Barbell Back Squat", "Legs", Equipment.BARBELL),
    LibraryExercise("Romanian Deadlift", "Legs", Equipment.BARBELL),
    LibraryExercise("Leg Press", "Legs"),
    LibraryExercise("Lunges", "Legs"),
    LibraryExercise("Leg Curl", "Legs"),
    LibraryExercise("Bench Press", "Chest", Equipment.BARBELL),
    LibraryExercise("Incline Dumbbell Press", "Chest", Equipment.DUMBBELL),
    LibraryExercise("Cable Fly", "Chest", Equipment.CABLE),
    LibraryExercise("Dips", "Chest"),
    LibraryExercise("Deadlift", "Back", Equipment.BARBELL),
    LibraryExercise("Pull-ups", "Back"),
    LibraryExercise("Barbell Row", "Back", Equipment.BARBELL),
    LibraryExercise("Lat Pulldown", "Back", Equipment.CABLE),
    LibraryExercise("Cable Row", "Back", Equipment.CABLE),
    LibraryExercise("Overhead Press", "Shoulders", Equipment.BARBELL),
    LibraryExercise("Lateral Raise", "Shoulders", Equipment.DUMBBELL),
    LibraryExercise("Face Pull", "Shoulders", Equipment.CABLE),
    LibraryExercise("Barbell Curl", "Arms", Equipment.BARBELL),
    LibraryExercise("Hammer Curl", "Arms", Equipment.DUMBBELL),
    LibraryExercise("Tricep Pushdown", "Arms", Equipment.CABLE),
    LibraryExercise("Skull Crushers", "Arms", Equipment.BARBELL),
    LibraryExercise("Plank", "Core"),
    LibraryExercise("Cable Crunch", "Core", Equipment.CABLE),
    LibraryExercise("Hanging Leg Raise", "Core"),
)

val CATEGORIES = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

fun genId(): String = UUID.randomUUID().toString().take(8)

// ─── Formatting ──────────────────────────────────────────────────────────────

fun formatDuration(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return when {
        h > 0 -> "${h}h ${m}m"
        m > 0 -> "${m}m ${s}s"
        else -> "${s}s"
    }
}

fun formatDate(epochMillis: Long): String =
    SimpleDateFormat("EEE, MMM d", Locale.US).format(Date(epochMillis))

/** Like JS `toLocaleString()`: grouping separators, up to 3 decimals, no trailing zeros. */
fun formatNumber(value: Double): String =
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 3 }.format(value)

/** A weight as typed: "60", "62.5" — never "60.0". */
fun formatWeight(value: Double): String =
    if (value == Math.floor(value) && !value.isInfinite()) value.toLong().toString() else value.toString()

fun totalVolume(exercises: List<WorkoutExercise>): Double =
    exercises.sumOf { ex ->
        val sides = if (ex.perSide) 2 else 1
        ex.sets.filter { it.done }.sumOf { it.weight * it.reps * sides }
    }

// ─── JSON ────────────────────────────────────────────────────────────────────

private fun ExerciseSet.toJson() = JSONObject()
    .put("reps", reps).put("weight", weight).put("unit", unit.label).put("done", done)

private fun JSONObject.toExerciseSet() = ExerciseSet(
    reps = optInt("reps"),
    weight = optDouble("weight", 0.0),
    unit = WeightUnit.fromLabel(optString("unit")),
    done = optBoolean("done"),
)

private fun WorkoutExercise.toJson() = JSONObject()
    .put("id", id).put("name", name).put("category", category).put("notes", notes)
    .put("equipment", equipment?.name).put("perSide", perSide)
    .put("sets", JSONArray(sets.map { it.toJson() }))

private fun JSONObject.toWorkoutExercise() = WorkoutExercise(
    id = optString("id").ifEmpty { genId() },
    name = optString("name"),
    category = optString("category"),
    notes = optString("notes"),
    equipment = Equipment.fromName(optString("equipment")),
    perSide = optBoolean("perSide"),
    sets = optJSONArray("sets").mapObjects { it.toExerciseSet() },
)

fun Workout.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name).put("date", date).put("duration", duration)
    .put("exercises", JSONArray(exercises.map { it.toJson() }))

fun JSONObject.toWorkout() = Workout(
    id = optString("id").ifEmpty { genId() },
    name = optString("name"),
    date = optLong("date"),
    duration = optLong("duration"),
    exercises = optJSONArray("exercises").mapObjects { it.toWorkoutExercise() },
)

private fun PresetExercise.toJson() = JSONObject()
    .put("name", name).put("category", category).put("defaultSets", defaultSets)
    .put("defaultReps", defaultReps).put("defaultWeight", defaultWeight)
    .put("equipment", equipment?.name).put("perSide", perSide)

private fun JSONObject.toPresetExercise() = PresetExercise(
    name = optString("name"),
    category = optString("category"),
    defaultSets = optInt("defaultSets"),
    defaultReps = optInt("defaultReps"),
    defaultWeight = optDouble("defaultWeight", 0.0),
    equipment = Equipment.fromName(optString("equipment")),
    perSide = optBoolean("perSide"),
)

fun PresetWorkout.toJson(): JSONObject = JSONObject()
    .put("id", id).put("name", name)
    .put("exercises", JSONArray(exercises.map { it.toJson() }))

fun JSONObject.toPresetWorkout() = PresetWorkout(
    id = optString("id").ifEmpty { genId() },
    name = optString("name"),
    exercises = optJSONArray("exercises").mapObjects { it.toPresetExercise() },
)

fun ActiveWorkout.toJson(): JSONObject = JSONObject()
    .put("name", name).put("startTime", startTime)
    .put("exercises", JSONArray(exercises.map { it.toJson() }))

fun JSONObject.toActiveWorkout() = ActiveWorkout(
    name = optString("name"),
    startTime = optLong("startTime"),
    exercises = optJSONArray("exercises").mapObjects { it.toWorkoutExercise() },
)

fun <T> JSONArray?.mapObjects(transform: (JSONObject) -> T): List<T> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { i -> optJSONObject(i)?.let(transform) }
}
