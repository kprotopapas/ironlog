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

    /** Converts [value], expressed in this unit, to [target]. */
    fun convert(value: Double, target: WeightUnit): Double = when {
        this == target -> value
        this == KG -> value * LBS_PER_KG
        else -> value / LBS_PER_KG
    }

    companion object {
        fun fromLabel(s: String?) = entries.firstOrNull { it.label == s } ?: KG
        private const val LBS_PER_KG = 2.20462262
    }
}

enum class Equipment(val label: String) {
    BARBELL("Barbell"), DUMBBELL("Dumbbell"), CABLE("Cable"), SMITH("Smith machine"), MACHINE("Machine"), BODYWEIGHT("Bodyweight");

    /** For bodyweight exercises the logged weight is added load (belt, vest) on top of bodyweight. */
    val isBodyweight get() = this == BODYWEIGHT

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

data class BodyweightEntry(
    val date: Long, // epoch millis
    val weight: Double,
    val unit: WeightUnit,
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

// Names describe the movement only; the equipment is picked in the dropdown and these are just defaults.
val EXERCISE_LIBRARY = listOf(
    LibraryExercise("Squat", "Legs", Equipment.BARBELL),
    LibraryExercise("Romanian Deadlift", "Legs", Equipment.BARBELL),
    LibraryExercise("Leg Press", "Legs", Equipment.MACHINE),
    LibraryExercise("Lunges", "Legs", Equipment.DUMBBELL),
    LibraryExercise("Leg Curl", "Legs", Equipment.MACHINE),
    LibraryExercise("Hip Thrust", "Legs", Equipment.BARBELL),
    LibraryExercise("Calf Raises", "Legs", Equipment.SMITH),
    LibraryExercise("Bench Press", "Chest", Equipment.BARBELL),
    LibraryExercise("Incline Press", "Chest", Equipment.DUMBBELL),
    LibraryExercise("Chest Fly", "Chest", Equipment.CABLE),
    LibraryExercise("Dips", "Chest", Equipment.BODYWEIGHT),
    LibraryExercise("Deadlift", "Back", Equipment.BARBELL),
    LibraryExercise("Pull-ups", "Back", Equipment.BODYWEIGHT),
    LibraryExercise("Bent-over Row", "Back", Equipment.BARBELL),
    LibraryExercise("Lat Pulldown", "Back", Equipment.CABLE),
    LibraryExercise("Low Row", "Back", Equipment.CABLE),
    LibraryExercise("Overhead Press", "Shoulders", Equipment.BARBELL),
    LibraryExercise("Lateral Raise", "Shoulders", Equipment.DUMBBELL),
    LibraryExercise("Face Pull", "Shoulders", Equipment.CABLE),
    LibraryExercise("Rear Delt", "Shoulders", Equipment.CABLE),
    LibraryExercise("Shrugs", "Shoulders", Equipment.DUMBBELL),
    LibraryExercise("Curl", "Arms", Equipment.CABLE),
    LibraryExercise("Hammer Curl", "Arms", Equipment.DUMBBELL),
    LibraryExercise("Preacher Curl", "Arms", Equipment.DUMBBELL),
    LibraryExercise("Tricep Pushdown", "Arms", Equipment.CABLE),
    LibraryExercise("Skull Crushers", "Arms", Equipment.BARBELL),
    LibraryExercise("Plank", "Core", Equipment.BODYWEIGHT),
    LibraryExercise("Crunch", "Core", Equipment.CABLE),
    LibraryExercise("Hanging Leg Raise", "Core", Equipment.BODYWEIGHT),
    LibraryExercise("Dragon Flag", "Core", Equipment.BODYWEIGHT),
)

/** Old library names that had the equipment baked in. */
private val RENAMED_EXERCISES = mapOf(
    "barbell back squat" to "Squat",
    "incline dumbbell press" to "Incline Press",
    "cable fly" to "Chest Fly",
    "barbell row" to "Bent-over Row",
    "cable row" to "Low Row",
    "barbell curl" to "Curl",
    "cable crunch" to "Crunch",
)

private fun renamed(name: String) = RENAMED_EXERCISES[name.trim().lowercase()]

private fun libraryEntry(name: String) = EXERCISE_LIBRARY.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }

/** Logged history keeps the equipment it was done with; only the name changes. */
fun WorkoutExercise.withCurrentName() = renamed(name)?.let { copy(name = it) } ?: this

/**
 * Presets are defaults, so a renamed exercise takes the library's equipment (the old name implied
 * the old equipment), and an exercise without equipment picks up the library's default.
 */
fun PresetExercise.withCurrentDefaults(): PresetExercise {
    val newName = renamed(name)
    val default = libraryEntry(newName ?: name)?.equipment?.takeIf { newName != null || equipment == null }
    return copy(
        name = newName ?: name,
        equipment = default ?: equipment,
        perSide = default?.defaultPerSide ?: perSide,
    )
}


val CATEGORIES = listOf("All", "Chest", "Back", "Legs", "Shoulders", "Arms", "Core")

private val BODYWEIGHT_EXERCISES = EXERCISE_LIBRARY
    .filter { it.equipment == Equipment.BODYWEIGHT }
    .map { it.name.lowercase() }
    .toSet()

/**
 * Entries logged before the Bodyweight equipment existed have no equipment. Only unloaded ones
 * (weight 0) are upgraded; a non-zero weight might be bodyweight typed in, which would double-count.
 */
private fun needsBodyweight(name: String, equipment: Equipment?, weights: List<Double>) =
    equipment == null && name.trim().lowercase() in BODYWEIGHT_EXERCISES && weights.all { it == 0.0 }

fun WorkoutExercise.withBodyweightEquipment() =
    if (needsBodyweight(name, equipment, sets.map { it.weight })) copy(equipment = Equipment.BODYWEIGHT, perSide = false) else this

fun PresetExercise.withBodyweightEquipment() =
    if (needsBodyweight(name, equipment, listOf(defaultWeight))) copy(equipment = Equipment.BODYWEIGHT, perSide = false) else this

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

fun formatShortDate(epochMillis: Long): String =
    SimpleDateFormat("MMM d", Locale.US).format(Date(epochMillis))

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

fun BodyweightEntry.toJson(): JSONObject = JSONObject()
    .put("date", date).put("weight", weight).put("unit", unit.label)

fun JSONObject.toBodyweightEntry() = BodyweightEntry(
    date = optLong("date"),
    weight = optDouble("weight", 0.0),
    unit = WeightUnit.fromLabel(optString("unit")),
)

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
