package com.example.gymtracker.data

// ─── Progression analytics ───────────────────────────────────────────────────
//
// Progress is measured per exercise as the best estimated one-rep max (Epley) of each
// session, so heavier weight and more reps at the same weight both count. Bodyweight
// exercises add the bodyweight logged closest before the session to the added load.

const val DAY_MILLIS = 24L * 60 * 60 * 1000
const val TREND_WINDOW_DAYS = 56 // 8 weeks

/** Weekly change, as % of the average, needed to count as progressing or declining. */
private const val TREND_THRESHOLD_PCT = 0.5
private const val MIN_TREND_SESSIONS = 3

/** Epley estimate; a single rep is its own max. */
fun estimatedOneRepMax(load: Double, reps: Int): Double =
    if (reps <= 1) load else load * (1 + reps / 30.0)

enum class ProgressMetric { E1RM, REPS }

enum class Trend { PROGRESSING, STALLED, DECLINING, NOT_ENOUGH_DATA }

/** The top set of one session. Weights are in the display unit. */
data class SessionBest(
    val date: Long,
    /** e1RM for [ProgressMetric.E1RM], reps for [ProgressMetric.REPS]. */
    val value: Double,
    /** Total load moved, including bodyweight for bodyweight exercises. */
    val load: Double,
    /** Weight as logged; the added load for bodyweight exercises. */
    val logged: Double,
    val reps: Int,
    /** Bodyweight at the time, if any has been logged. */
    val bodyweight: Double?,
    val isPr: Boolean,
)

/** Best logged set at a given rep count. */
data class RepRecord(val reps: Int, val load: Double, val logged: Double, val date: Long)

data class ExerciseProgress(
    val key: String,
    val name: String,
    val category: String,
    val equipment: Equipment?,
    val perSide: Boolean,
    val metric: ProgressMetric,
    /** Oldest first; never empty. */
    val sessions: List<SessionBest>,
    val repRecords: List<RepRecord>,
    val trend: Trend,
    /** Change over the trend window along the fitted line, in %. Null without enough data. */
    val changePct: Double?,
) {
    val latest get() = sessions.last()
    val best get() = sessions.maxOf { it.value }
}

/** Bodyweight lookup: the latest entry at or before a date, else the earliest one after it. */
class BodyweightLog(entries: List<BodyweightEntry>, private val unit: WeightUnit) {
    private val sorted = entries.sortedBy { it.date }

    val isEmpty get() = sorted.isEmpty()

    fun at(date: Long): Double? {
        val entry = sorted.lastOrNull { it.date <= date } ?: sorted.firstOrNull() ?: return null
        return entry.unit.convert(entry.weight, unit)
    }
}

private fun seriesKey(ex: WorkoutExercise) =
    "${ex.name.trim().lowercase()}|${ex.equipment?.name}|${ex.perSide && ex.equipment?.supportsPerSide == true}"

private class LoggedSet(val date: Long, val load: Double, val logged: Double, val reps: Int, val bodyweight: Double?)

fun computeProgress(
    workouts: List<Workout>,
    bodyweight: List<BodyweightEntry>,
    unit: WeightUnit,
    now: Long = System.currentTimeMillis(),
): List<ExerciseProgress> {
    val bw = BodyweightLog(bodyweight, unit)
    // key -> (latest exercise seen, sets grouped per session)
    val latestExercise = mutableMapOf<String, WorkoutExercise>()
    val setsBySession = mutableMapOf<String, MutableList<List<LoggedSet>>>()

    for (w in workouts.sortedBy { it.date }) {
        val bodyweightThen = bw.at(w.date)
        w.exercises.groupBy(::seriesKey).forEach { (key, instances) ->
            val sets = instances.flatMap { ex ->
                val isBodyweight = ex.equipment?.isBodyweight == true
                ex.sets.filter { it.done && it.reps > 0 }.map { s ->
                    val logged = s.unit.convert(s.weight, unit)
                    val load = if (isBodyweight) logged + (bodyweightThen ?: 0.0) else logged
                    LoggedSet(w.date, load, logged, s.reps, bodyweightThen)
                }
            }
            if (sets.isNotEmpty()) {
                latestExercise[key] = instances.last()
                setsBySession.getOrPut(key) { mutableListOf() }.add(sets)
            }
        }
    }

    return setsBySession.mapNotNull { (key, sessions) ->
        val ex = latestExercise.getValue(key)
        val isBodyweight = ex.equipment?.isBodyweight == true
        val hasLoad = sessions.any { s -> s.any { it.load > 0 } }
        val metric = if (hasLoad && !(isBodyweight && bw.isEmpty)) ProgressMetric.E1RM else ProgressMetric.REPS

        var bestSoFar = Double.NEGATIVE_INFINITY
        val bests = sessions.mapNotNull { sets ->
            val top = when (metric) {
                ProgressMetric.E1RM -> sets.maxBy { estimatedOneRepMax(it.load, it.reps) }
                ProgressMetric.REPS -> sets.maxWith(compareBy({ it.reps }, { it.load }))
            }
            val value = when (metric) {
                ProgressMetric.E1RM -> estimatedOneRepMax(top.load, top.reps)
                ProgressMetric.REPS -> top.reps.toDouble()
            }
            if (value <= 0) return@mapNotNull null
            val isPr = value > bestSoFar
            if (isPr) bestSoFar = value
            SessionBest(top.date, value, top.load, top.logged, top.reps, top.bodyweight, isPr)
        }
        if (bests.isEmpty()) return@mapNotNull null
        // The very first session is trivially a "PR"; only count beating an earlier session.
        val sessionsOut = bests.mapIndexed { i, b -> if (i == 0) b.copy(isPr = false) else b }

        val repRecords = if (metric == ProgressMetric.E1RM) {
            sessions.flatten()
                .groupBy { it.reps }
                .map { (reps, sets) -> sets.maxBy { it.load }.let { RepRecord(reps, it.load, it.logged, it.date) } }
                .sortedBy { it.reps }
        } else emptyList()

        val (trend, change) = trendOf(sessionsOut, now)
        ExerciseProgress(
            key = key,
            name = ex.name,
            category = ex.category,
            equipment = ex.equipment,
            perSide = ex.perSide && ex.equipment?.supportsPerSide == true,
            metric = metric,
            sessions = sessionsOut,
            repRecords = repRecords,
            trend = trend,
            changePct = change,
        )
    }.sortedByDescending { it.latest.date }
}

/** Least-squares slope of the sessions inside the trend window, classified by weekly % change. */
internal fun trendOf(sessions: List<SessionBest>, now: Long): Pair<Trend, Double?> {
    val window = sessions.filter { it.date >= now - TREND_WINDOW_DAYS * DAY_MILLIS }
    if (window.size < MIN_TREND_SESSIONS) return Trend.NOT_ENOUGH_DATA to null
    val fit = linearFit(window.map { it.date.toDouble() / DAY_MILLIS }, window.map { it.value }) ?: return Trend.STALLED to 0.0
    val mean = window.sumOf { it.value } / window.size
    val weeklyPct = fit.slope * 7 / mean * 100
    val start = fit.at(window.first().date.toDouble() / DAY_MILLIS)
    val end = fit.at(window.last().date.toDouble() / DAY_MILLIS)
    val changePct = if (start > 0) (end - start) / start * 100 else 0.0
    val trend = when {
        weeklyPct >= TREND_THRESHOLD_PCT -> Trend.PROGRESSING
        weeklyPct <= -TREND_THRESHOLD_PCT -> Trend.DECLINING
        else -> Trend.STALLED
    }
    return trend to changePct
}

internal class LinearFit(val slope: Double, val intercept: Double) {
    fun at(x: Double) = intercept + slope * x
}

/** Null when all x are equal (e.g. every session on the same day). */
internal fun linearFit(xs: List<Double>, ys: List<Double>): LinearFit? {
    val mx = xs.average()
    val my = ys.average()
    val sxx = xs.sumOf { (it - mx) * (it - mx) }
    if (sxx == 0.0) return null
    val sxy = xs.indices.sumOf { (xs[it] - mx) * (ys[it] - my) }
    val slope = sxy / sxx
    return LinearFit(slope, my - slope * mx)
}

// ─── Bodyweight ──────────────────────────────────────────────────────────────

/** A weigh-in plus the average of all weigh-ins in the 7 days up to and including it. */
data class BodyweightPoint(val date: Long, val weight: Double, val average: Double)

fun bodyweightSeries(entries: List<BodyweightEntry>, unit: WeightUnit): List<BodyweightPoint> {
    val sorted = entries.sortedBy { it.date }.map { it.date to it.unit.convert(it.weight, unit) }
    return sorted.map { (date, weight) ->
        val week = sorted.filter { it.first in (date - 7 * DAY_MILLIS + 1)..date }
        BodyweightPoint(date, weight, week.sumOf { it.second } / week.size)
    }
}

/** Change of the 7-day average over the last [days], or null if the log doesn't reach that far back. */
fun bodyweightChange(series: List<BodyweightPoint>, days: Int = 30): Double? {
    val latest = series.lastOrNull() ?: return null
    val then = series.lastOrNull { it.date <= latest.date - days * DAY_MILLIS } ?: return null
    return latest.average - then.average
}

/** Same calendar day in the device's time zone. */
fun sameDay(a: Long, b: Long): Boolean {
    val cal = java.util.Calendar.getInstance()
    cal.timeInMillis = a
    val y = cal.get(java.util.Calendar.YEAR)
    val d = cal.get(java.util.Calendar.DAY_OF_YEAR)
    cal.timeInMillis = b
    return y == cal.get(java.util.Calendar.YEAR) && d == cal.get(java.util.Calendar.DAY_OF_YEAR)
}
