package com.example.gymtracker.data

import org.json.JSONObject
import java.util.Locale

// ─── Timer ───────────────────────────────────────────────────────────────────
//
// A countdown for rest and timed holds, or a stopwatch for holding as long as you can.
// Times are wall-clock millis, so the display is right whenever it's drawn and nothing
// needs to tick in the model.

val TIMER_PRESETS_SEC = listOf(30, 45, 60, 90, 120, 180)
const val TIMER_NUDGE_SEC = 10

data class GymTimer(
    val countdown: Boolean = true,
    /** Countdown length in millis; unused by the stopwatch. */
    val duration: Long = 90_000,
    /** When the current run started; null while paused or reset. */
    val startedAt: Long? = null,
    /** Millis run before the last pause. */
    val banked: Long = 0,
) {
    val running get() = startedAt != null
    /** Started at some point and not reset since, running or paused. */
    val active get() = running || banked > 0

    fun elapsed(now: Long) = banked + (startedAt?.let { (now - it).coerceAtLeast(0) } ?: 0)
    fun remaining(now: Long) = (duration - elapsed(now)).coerceAtLeast(0)
    /** What the clock shows: time left for a countdown, time held for a stopwatch. */
    fun shown(now: Long) = if (countdown) remaining(now) else elapsed(now)
    /** [centis] only applies to the stopwatch; a countdown is read in whole seconds. */
    fun label(now: Long, centis: Boolean = false) =
        if (countdown) formatClock(shown(now), roundUp = true) else formatClock(shown(now), centis = centis)
    fun finished(now: Long) = countdown && elapsed(now) >= duration

    fun start(now: Long) = if (running) this else copy(startedAt = now)
    fun pause(now: Long) = if (running) copy(startedAt = null, banked = elapsed(now)) else this
    fun reset() = copy(startedAt = null, banked = 0)

    /** Changes the length; a running countdown keeps going with the new end. */
    fun withDuration(millis: Long) = copy(duration = millis.coerceAtLeast(0))
    fun nudged(sec: Int) = withDuration((duration + sec * 1000L).coerceAtLeast(1000))
}

fun GymTimer.toJson(): JSONObject = JSONObject()
    .put("countdown", countdown).put("duration", duration).put("startedAt", startedAt ?: JSONObject.NULL).put("banked", banked)

fun JSONObject.toGymTimer() = GymTimer(
    countdown = optBoolean("countdown", true),
    duration = optLong("duration", 90_000),
    startedAt = if (isNull("startedAt")) null else optLong("startedAt"),
    banked = optLong("banked"),
)

/**
 * "1:30", "0:05", or "1:05.42" with [centis]; hours only when needed. Countdowns round up so they
 * read 0:00 only when over.
 */
fun formatClock(millis: Long, roundUp: Boolean = false, centis: Boolean = false): String {
    val total = if (roundUp) (millis + 999) / 1000 else millis / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    val clock = if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s) else String.format(Locale.US, "%d:%02d", m, s)
    return if (centis) clock + String.format(Locale.US, ".%02d", millis % 1000 / 10) else clock
}

/**
 * A typed time, entered like a microwave: the last two digits are seconds, the rest minutes, so
 * "130" is 1:30 and "90" is 90 seconds. Returns the time in millis and how it reads ("1:30", "0:90").
 */
fun typedClock(digits: String): Pair<Long, String> {
    val n = digits.filter { it.isDigit() }.takeLast(4).toIntOrNull() ?: 0
    return (n / 100 * 60 + n % 100) * 1000L to String.format(Locale.US, "%d:%02d", n / 100, n % 100)
}
