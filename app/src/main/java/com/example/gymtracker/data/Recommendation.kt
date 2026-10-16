package com.example.gymtracker.data

// ─── Next-weight recommendation ──────────────────────────────────────────────
//
// Judged by strength rather than a rep range, so heavy singles and high-rep sets both read
// right. A session's strength is its best estimated one-rep max (see computeProgress), and
// it's compared with the best of the few sessions before it:
//   - beat that best: go up one step;
//   - more than a tenth under it two sessions running: go down one step (one bad day isn't enough);
//   - otherwise stay and beat last session's reps.
// The next session repeats last session's sets, every weight moved by that step and the reps
// kept, so a pyramid stays a pyramid and heavy doubles stay doubles.

/** Sessions before the latest that its strength is compared with. */
private const val BASELINE_SESSIONS = 3
/** Below this share of the baseline a session counts as too heavy. */
private const val TOO_HEAVY = 0.9
/** Allowance for rounding, so matching the best isn't counted as beating it. */
private const val BEAT_MARGIN = 1.005

enum class WeightAction { INCREASE, HOLD, DECREASE }

data class Recommendation(
    val action: WeightAction,
    /** Last session's done sets in order, as logged (added load for bodyweight), in the display unit. */
    val lastSets: List<ExerciseSet>,
    val lastDate: Long,
    /** [lastSets] with every weight moved one step for [action]; the reps are the ones to beat. */
    val nextSets: List<ExerciseSet>,
) {
    /** Heaviest weight last session. */
    val lastWeight get() = lastSets.maxOf { it.weight }
    /** Heaviest weight next session. */
    val nextWeight get() = nextSets.maxOf { it.weight }
    /** Last session's strongest set, by estimated max. */
    val bestSet get() = lastSets.maxWith(compareBy({ estimatedOneRepMax(it.weight, it.reps) }, { it.reps }))

    /** The weight for set [i] next session; sets beyond last session's repeat its last one. */
    fun nextWeightFor(i: Int) = nextSets.getOrElse(i) { nextSets.last() }.weight
}

/** One plate / pin / dumbbell step, in [unit]. A pair of dumbbells logged as a total steps both. */
fun weightStep(equipment: Equipment?, unit: WeightUnit, perSide: Boolean = false): Double {
    val kg = when (equipment) {
        Equipment.DUMBBELL -> 2.0
        Equipment.MACHINE -> 5.0
        else -> 2.5
    }
    val step = if (unit == WeightUnit.KG) kg else if (equipment == Equipment.MACHINE) 10.0 else 5.0
    return if (equipment == Equipment.DUMBBELL && !perSide) step * 2 else step
}

/** Whether the latest of [values] (oldest first) beat, held, or fell well under the ones before it. */
internal fun strengthCall(values: List<Double>): WeightAction {
    fun baseline(i: Int) = values.subList((i - BASELINE_SESSIONS).coerceAtLeast(0), i).maxOrNull()
    val last = values.lastIndex
    val best = baseline(last) ?: return WeightAction.HOLD
    if (values[last] > best * BEAT_MARGIN) return WeightAction.INCREASE
    val tooHeavy = { i: Int -> baseline(i)?.let { values[i] < it * TOO_HEAVY } == true }
    return if (tooHeavy(last) && tooHeavy(last - 1)) WeightAction.DECREASE else WeightAction.HOLD
}

/**
 * Recommendation for [exercise] from the most recent finished workout that has it, or null.
 * Weights are in [exercise]'s convention: last session's 10 kg a side reads as 20 kg if it's now total.
 * [bodyweight] lets bodyweight exercises be judged by load; without it they're judged by reps.
 */
fun recommend(
    exercise: WorkoutExercise,
    workouts: List<Workout>,
    unit: WeightUnit,
    bodyweight: List<BodyweightEntry> = emptyList(),
): Recommendation? {
    val key = seriesKey(exercise)
    val isBodyweight = exercise.equipment?.isBodyweight == true
    val last = workouts.sortedByDescending { it.date }.firstNotNullOfOrNull { w ->
        w.exercises.filter { seriesKey(it) == key }
            .flatMap { ex -> ex.sets.filter { it.done && it.reps > 0 }.map { ex to it } }
            .map { (ex, s) -> ExerciseSet(s.reps, s.unit.convert(s.weight, unit) * ex.sides / exercise.sides, unit, false) }
            .takeIf { it.isNotEmpty() }
            ?.let { w to it }
    } ?: return null
    val (workout, lastSets) = last
    // Without a weight there's nothing to adjust, except bodyweight where 0 means no added load.
    if (lastSets.all { it.weight <= 0 } && !isBodyweight) return null

    val history = computeProgress(workouts.filter { it.date <= workout.date }, bodyweight, unit)
        .firstOrNull { it.key == key }?.sessions.orEmpty()
    var action = strengthCall(history.map { it.value })
    // Can't go below no weight (e.g. unassisted pull-ups): keep working on reps instead.
    if (action == WeightAction.DECREASE && lastSets.all { it.weight <= 0 }) action = WeightAction.HOLD

    val step = when (action) {
        WeightAction.INCREASE -> weightStep(exercise.equipment, unit, exercise.sides == 2)
        WeightAction.DECREASE -> -weightStep(exercise.equipment, unit, exercise.sides == 2)
        WeightAction.HOLD -> 0.0
    }
    val nextSets = lastSets.map { it.copy(weight = (it.weight + step).coerceAtLeast(0.0)) }
    return Recommendation(action, lastSets, workout.date, nextSets)
}

/**
 * Sets for a preset exercise: last session's sets with the recommended weights when there's
 * history, so the shape of the session carries over; without history, the preset's own defaults.
 */
fun prefilledSets(
    preset: PresetExercise,
    workouts: List<Workout>,
    unit: WeightUnit,
    bodyweight: List<BodyweightEntry> = emptyList(),
): List<ExerciseSet> {
    val probe = WorkoutExercise("", preset.name, preset.category, emptyList(), equipment = preset.equipment, perSide = preset.perSide)
    return recommend(probe, workouts, unit, bodyweight)?.nextSets
        ?: List(preset.defaultSets) { ExerciseSet(preset.defaultReps, preset.defaultWeight, unit, false) }
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
