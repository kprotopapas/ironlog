package com.example.gymtracker

import com.example.gymtracker.data.BodyweightEntry
import com.example.gymtracker.data.DAY_MILLIS
import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.ProgressMetric
import com.example.gymtracker.data.Trend
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.bodyweightSeries
import com.example.gymtracker.data.computeProgress
import com.example.gymtracker.data.estimatedOneRepMax
import com.example.gymtracker.ui.niceTicks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressTest {
    private val now = 1_000 * DAY_MILLIS

    private fun workout(daysAgo: Int, vararg exercises: WorkoutExercise) =
        Workout("w$daysAgo", "W", now - daysAgo * DAY_MILLIS, 3600, exercises.toList())

    private fun exercise(
        name: String,
        vararg sets: Pair<Double, Int>,
        equipment: Equipment? = Equipment.BARBELL,
        unit: WeightUnit = WeightUnit.KG,
        done: Boolean = true,
    ) = WorkoutExercise("e", name, "Chest", sets.map { (w, r) -> ExerciseSet(r, w, unit, done) }, equipment = equipment)

    @Test
    fun epley() {
        assertEquals(100.0, estimatedOneRepMax(100.0, 1), 1e-9)
        assertEquals(80.0 * (1 + 10 / 30.0), estimatedOneRepMax(80.0, 10), 1e-9)
    }

    @Test
    fun bestSetPerSessionAndPrs() {
        val p = computeProgress(
            listOf(
                workout(21, exercise("Bench Press", 60.0 to 10, 40.0 to 12)),
                workout(14, exercise("Bench Press", 65.0 to 8)),
                workout(7, exercise("Bench Press", 60.0 to 8)),
            ),
            emptyList(), WeightUnit.KG, now,
        ).single()
        assertEquals(ProgressMetric.E1RM, p.metric)
        assertEquals(80.0, p.sessions[0].value, 1e-9) // 60×10 beats 40×12
        assertEquals(65.0 * (1 + 8 / 30.0), p.sessions[1].value, 1e-9)
        assertEquals(76.0, p.sessions[2].value, 1e-9)
        assertEquals(60.0, p.sessions[0].load, 1e-9)
        assertEquals(listOf(false, true, false), p.sessions.map { it.isPr })
    }

    @Test
    fun unfinishedSetsIgnored() {
        val p = computeProgress(
            listOf(workout(1, exercise("Squat", 200.0 to 5, done = false), exercise("Bench", 50.0 to 5))),
            emptyList(), WeightUnit.KG, now,
        )
        assertEquals(listOf("Bench"), p.map { it.name })
    }

    @Test
    fun mixedUnitsConverted() {
        val p = computeProgress(
            listOf(workout(1, exercise("Bench", 220.462262 to 1, unit = WeightUnit.LBS))),
            emptyList(), WeightUnit.KG, now,
        ).single()
        assertEquals(100.0, p.latest.value, 1e-6)
    }

    @Test
    fun pullUpsAddBodyweightFromClosestEarlierWeighIn() {
        val bw = listOf(
            BodyweightEntry(now - 30 * DAY_MILLIS, 80.0, WeightUnit.KG),
            BodyweightEntry(now - 5 * DAY_MILLIS, 76.0, WeightUnit.KG),
        )
        val p = computeProgress(
            listOf(
                workout(40, exercise("Pull-ups", 0.0 to 8, equipment = Equipment.BODYWEIGHT)), // before first weigh-in
                workout(10, exercise("Pull-ups", 10.0 to 5, equipment = Equipment.BODYWEIGHT)),
                workout(1, exercise("Pull-ups", 0.0 to 10, equipment = Equipment.BODYWEIGHT)),
            ),
            bw, WeightUnit.KG, now,
        ).single()
        assertEquals(ProgressMetric.E1RM, p.metric)
        assertEquals(listOf(80.0, 90.0, 76.0), p.sessions.map { it.load })
        assertEquals(listOf(0.0, 10.0, 0.0), p.sessions.map { it.logged })
        assertEquals(76.0, p.sessions.last().bodyweight!!, 1e-9)
    }

    @Test
    fun pullUpsWithoutBodyweightFallBackToReps() {
        val p = computeProgress(
            listOf(
                workout(3, exercise("Pull-ups", 0.0 to 8, 0.0 to 6, equipment = Equipment.BODYWEIGHT)),
                workout(1, exercise("Pull-ups", 0.0 to 9, equipment = Equipment.BODYWEIGHT)),
            ),
            emptyList(), WeightUnit.KG, now,
        ).single()
        assertEquals(ProgressMetric.REPS, p.metric)
        assertEquals(listOf(8.0, 9.0), p.sessions.map { it.value })
    }

    @Test
    fun equipmentVariantsAreSeparateSeries() {
        val p = computeProgress(
            listOf(workout(1, exercise("Row", 50.0 to 8), exercise("Row", 20.0 to 8, equipment = Equipment.CABLE))),
            emptyList(), WeightUnit.KG, now,
        )
        assertEquals(2, p.size)
    }

    @Test
    fun trends() {
        fun trendFor(vararg weights: Double) = computeProgress(
            weights.mapIndexed { i, w -> workout((weights.size - i) * 7, exercise("Bench", w to 5)) },
            emptyList(), WeightUnit.KG, now,
        ).single()

        assertEquals(Trend.PROGRESSING, trendFor(60.0, 62.5, 65.0, 67.5).trend)
        assertEquals(Trend.DECLINING, trendFor(67.5, 65.0, 62.5, 60.0).trend)
        assertEquals(Trend.STALLED, trendFor(60.0, 60.0, 60.0, 60.0).trend)
        val few = trendFor(60.0, 65.0)
        assertEquals(Trend.NOT_ENOUGH_DATA, few.trend)
        assertNull(few.changePct)
        assertTrue(trendFor(60.0, 62.5, 65.0, 67.5).changePct!! > 10)
    }

    @Test
    fun sessionsOutsideWindowDontCountForTrend() {
        val p = computeProgress(
            listOf(100, 90, 80, 70).map { workout(it, exercise("Bench", (170.0 - it) to 5)) },
            emptyList(), WeightUnit.KG, now,
        ).single()
        assertEquals(Trend.NOT_ENOUGH_DATA, p.trend)
    }

    @Test
    fun repRecordsKeepHeaviestPerRepCount() {
        val p = computeProgress(
            listOf(
                workout(3, exercise("Bench", 60.0 to 8, 70.0 to 5)),
                workout(1, exercise("Bench", 62.5 to 8)),
            ),
            emptyList(), WeightUnit.KG, now,
        ).single()
        assertEquals(listOf(5 to 70.0, 8 to 62.5), p.repRecords.map { it.reps to it.load })
    }

    @Test
    fun bodyweightMovingAverage() {
        val entries = listOf(0, 1, 2, 10).map { BodyweightEntry(it * DAY_MILLIS, 80.0 + it, WeightUnit.KG) }
        val s = bodyweightSeries(entries, WeightUnit.KG)
        assertEquals(81.0, s[2].average, 1e-9) // 80, 81, 82
        assertEquals(90.0, s[3].average, 1e-9) // alone in its week
    }

    @Test
    fun ticksSpanDataWithRoundSteps() {
        val t = niceTicks(61.3, 97.8)
        assertTrue(t.first() <= 61.3 && t.last() >= 97.8)
        assertTrue(t.size in 3..7)
        assertFalse(niceTicks(80.0, 80.0).isEmpty())
    }
}
