package com.example.gymtracker.data

// ─── Next-weight recommendation (double progression) ─────────────────────────
//
// Stay at a weight and add reps until every working set reaches the top of the range, then
// go up one step. If even the best set falls below the bottom of the range, go down one step.
// Working sets are the sets at the heaviest weight of the last session, so warm-ups and
// back-off sets don't count.

val TARGET_REPS = 8..12

enum class WeightAction { INCREASE, HOLD, DECREASE }

data class Recommendation(
    val action: WeightAction,
    /** Working weight last session, as logged (added load for bodyweight), in the display unit. */
    val lastWeight: Double,
    val lastReps: List<Int>,
    val lastDate: Long,
    val nextWeight: Double,
)

/** One plate / pin / dumbbell step, in [unit]. Dumbbells are per dumbbell when logged per side. */
fun weightStep(equipment: Equipment?, unit: WeightUnit): Double {
    val kg = when (equipment) {
        Equipment.DUMBBELL -> 2.0
        Equipment.MACHINE -> 5.0
        else -> 2.5
    }
    return if (unit == WeightUnit.KG) kg else if (equipment == Equipment.MACHINE) 10.0 else 5.0
}

/** Recommendation for [exercise] from the most recent finished workout that has it, or null. */
fun recommend(
    exercise: WorkoutExercise,
    workouts: List<Workout>,
    unit: WeightUnit,
    range: IntRange = TARGET_REPS,
): Recommendation? {
    val key = seriesKey(exercise)
    val isBodyweight = exercise.equipment?.isBodyweight == true
    for (w in workouts.sortedByDescending { it.date }) {
        val sets = w.exercises.filter { seriesKey(it) == key }
            .flatMap { it.sets }
            .filter { it.done && it.reps > 0 }
            .map { it.unit.convert(it.weight, unit) to it.reps }
        if (sets.isEmpty()) continue

        val working = sets.maxOf { it.first }
        // Without a weight there's nothing to adjust, except bodyweight where 0 means no added load.
        if (working <= 0 && !isBodyweight) return null
        val reps = sets.filter { it.first >= working - 0.01 }.map { it.second }
        val step = weightStep(exercise.equipment, unit)

        var action = when {
            reps.all { it >= range.last } -> WeightAction.INCREASE
            reps.max() < range.first -> WeightAction.DECREASE
            else -> WeightAction.HOLD
        }
        // Can't go below no weight (e.g. unassisted pull-ups): keep working on reps instead.
        if (action == WeightAction.DECREASE && working <= 0) action = WeightAction.HOLD
        val next = when (action) {
            WeightAction.INCREASE -> working + step
            WeightAction.DECREASE -> (working - step).coerceAtLeast(0.0)
            WeightAction.HOLD -> working
        }
        return Recommendation(action, working, reps, w.date, next)
    }
    return null
}

/**
 * Sets for a preset exercise: the recommended weight when there's history, with last session's
 * reps to beat if the weight stays and the bottom of the range if it changes. Without history,
 * the preset's own defaults.
 */
fun prefilledSets(preset: PresetExercise, workouts: List<Workout>, unit: WeightUnit): List<ExerciseSet> {
    val probe = WorkoutExercise("", preset.name, preset.category, emptyList(), equipment = preset.equipment, perSide = preset.perSide)
    val rec = recommend(probe, workouts, unit)
        ?: return List(preset.defaultSets) { ExerciseSet(preset.defaultReps, preset.defaultWeight, unit, false) }
    return List(preset.defaultSets) { i ->
        val reps = if (rec.action == WeightAction.HOLD) rec.lastReps.getOrElse(i) { rec.lastReps.last() } else TARGET_REPS.first
        ExerciseSet(reps, rec.nextWeight, unit, false)
    }
}

/** Whether [exercise]'s heaviest set is above (+1) or below (−1) last session's working weight; 0 if the same. */
fun weightChange(exercise: WorkoutExercise, rec: Recommendation, unit: WeightUnit): Int {
    val current = exercise.sets.maxOfOrNull { it.unit.convert(it.weight, unit) } ?: return 0
    return when {
        current > rec.lastWeight + 0.01 -> 1
        current < rec.lastWeight - 0.01 -> -1
        else -> 0
    }
}

/** A muscle group's overall call: whichever of up or down its exercises suggest more often; null on a tie or none. */
fun groupSuggestion(actions: List<WeightAction>): WeightAction? {
    val up = actions.count { it == WeightAction.INCREASE }
    val down = actions.count { it == WeightAction.DECREASE }
    return when {
        up > down -> WeightAction.INCREASE
        down > up -> WeightAction.DECREASE
        else -> null
    }
}
