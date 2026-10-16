package com.example.gymtracker

import com.example.gymtracker.data.GymTimer
import com.example.gymtracker.data.formatClock
import com.example.gymtracker.data.typedClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TimerTest {
    @Test
    fun countdownRunsOut() {
        val t = GymTimer(duration = 60_000).start(1_000)
        assertEquals("1:00", t.label(1_000))
        assertEquals("0:01", t.label(60_500)) // half a second left still reads a second
        assertEquals("1:00", t.label(1_000, centis = true)) // countdowns stay in whole seconds
        assertFalse(t.finished(60_999))
        assertTrue(t.finished(61_000))
        assertEquals("0:00", t.label(90_000))
    }

    @Test
    fun pauseKeepsTimeAndResumeContinues() {
        val paused = GymTimer(duration = 60_000).start(0).pause(20_000)
        assertFalse(paused.running)
        assertTrue(paused.active)
        assertEquals(40_000, paused.remaining(999_999))
        assertEquals(30_000, paused.start(100_000).remaining(110_000))
        assertFalse(paused.reset().active)
    }

    @Test
    fun stopwatchCountsUpAndRoundsDown() {
        val t = GymTimer(countdown = false).start(0)
        assertEquals("0:00", t.label(900))
        assertEquals("1:05", t.label(65_400))
        assertEquals("1:05.42", t.label(65_429, centis = true))
        assertFalse(t.finished(10_000_000))
    }

    @Test
    fun nudgingNeverGoesBelowASecond() {
        assertEquals(100_000, GymTimer(duration = 90_000).nudged(10).duration)
        assertEquals(1_000, GymTimer(duration = 5_000).nudged(-10).duration)
    }

    @Test
    fun clockShowsHoursOnlyWhenNeeded() {
        assertEquals("59:59", formatClock(3_599_000))
        assertEquals("1:00:00", formatClock(3_600_000))
    }

    @Test
    fun typedTimesReadLikeAMicrowave() {
        assertEquals(90_000L to "1:30", typedClock("130"))
        assertEquals(90_000L to "0:90", typedClock("90"))
        assertEquals(45_000L to "0:45", typedClock("0:45"))
        assertEquals(13_000L to "0:13", typedClock("1:3")) // backspace on 1:30
        assertEquals(785_000L to "13:05", typedClock("1:305")) // typing 5 after 1:30
        assertEquals(0L to "0:00", typedClock(""))
    }
}
