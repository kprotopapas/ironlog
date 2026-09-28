package com.example.gymtracker

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.PresetExercise
import com.example.gymtracker.data.prefilledSets
import com.example.gymtracker.data.recommend
import com.example.gymtracker.data.weightChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommendationTest {
    private fun ex(vararg sets: Pair<Double, Int>, equipment: Equipment? = Equipment.BARBELL, unit: WeightUnit = WeightUnit.KG) =
        WorkoutExercise("e", "Bench Press", "Chest", sets.map { (w, r) -> ExerciseSet(r, w, unit, true) }, equipment = equipment)

    private fun history(vararg exercises: WorkoutExercise) =
        exercises.mapIndexed { i, e -> Workout("w$i", "W", i * 1000L, 0, listOf(e)) }

    private fun rec(vararg exercises: WorkoutExercise, current: WorkoutExercise = exercises.last()) =
        recommend(current, history(*exercises), WeightUnit.KG)!!

    @Test
    fun allSetsAtTopOfRangeGoUp() {
        val r = rec(ex(60.0 to 12, 60.0 to 12, 60.0 to 13))
        assertEquals(WeightAction.INCREASE, r.action)
        assertEquals(62.5, r.nextWeight, 1e-9)
    }

    @Test
    fun oneSetShortOfTwelveStays() {
        val r = rec(ex(60.0 to 12, 60.0 to 12, 60.0 to 10))
        assertEquals(WeightAction.HOLD, r.action)
        assertEquals(60.0, r.nextWeight, 1e-9)
    }

    @Test
    fun bestSetUnderEightGoesDown() {
        val r = rec(ex(60.0 to 7, 60.0 to 6))
        assertEquals(WeightAction.DECREASE, r.action)
        assertEquals(57.5, r.nextWeight, 1e-9)
    }

    @Test
    fun warmupsAndBackoffsIgnored() {
        // Only the 60 kg sets count: 12, 12 -> go up, despite the 40 kg warm-up at 8.
        val r = rec(ex(40.0 to 8, 60.0 to 12, 60.0 to 12, 50.0 to 9))
        assertEquals(WeightAction.INCREASE, r.action)
        assertEquals(listOf(12, 12), r.lastReps)
    }

    @Test
    fun usesMostRecentSession() {
        val r = rec(ex(60.0 to 12, 60.0 to 12), ex(62.5 to 8, 62.5 to 7))
        assertEquals(WeightAction.HOLD, r.action)
        assertEquals(62.5, r.lastWeight, 1e-9)
    }

    @Test
    fun stepsFollowEquipment() {
        assertEquals(24.0, rec(ex(22.0 to 12, equipment = Equipment.DUMBBELL)).nextWeight, 1e-9)
        assertEquals(55.0, rec(ex(50.0 to 12, equipment = Equipment.MACHINE)).nextWeight, 1e-9)
    }

    @Test
    fun unassistedPullUpsNeverGoBelowBodyweight() {
        val weak = rec(ex(0.0 to 5, 0.0 to 4, equipment = Equipment.BODYWEIGHT))
        assertEquals(WeightAction.HOLD, weak.action)
        val strong = rec(ex(0.0 to 12, 0.0 to 12, equipment = Equipment.BODYWEIGHT))
        assertEquals(WeightAction.INCREASE, strong.action)
        assertEquals(2.5, strong.nextWeight, 1e-9)
    }

    @Test
    fun differentEquipmentIsADifferentExercise() {
        val cable = ex(equipment = Equipment.CABLE)
        assertNull(recommend(cable, history(ex(60.0 to 12)), WeightUnit.KG))
    }

    @Test
    fun convertsToDisplayUnit() {
        val r = rec(ex(100.0 to 12, unit = WeightUnit.LBS))
        assertEquals(100.0 / 2.20462262 + 2.5, r.nextWeight, 1e-6)
    }

    private fun preset(sets: Int = 3) = PresetExercise("Bench Press", "Chest", sets, 10, 40.0, Equipment.BARBELL)

    @Test
    fun presetWithoutHistoryUsesItsDefaults() {
        val sets = prefilledSets(preset(), emptyList(), WeightUnit.KG)
        assertEquals(List(3) { 40.0 to 10 }, sets.map { it.weight to it.reps })
    }

    @Test
    fun presetGoesUpAndResetsReps() {
        val sets = prefilledSets(preset(), history(ex(60.0 to 12, 60.0 to 12, 60.0 to 12)), WeightUnit.KG)
        assertEquals(List(3) { 62.5 to 8 }, sets.map { it.weight to it.reps })
    }

    @Test
    fun presetHoldsWithLastRepsToBeat() {
        // Four preset sets, three logged: the extra set repeats the last one.
        val sets = prefilledSets(preset(sets = 4), history(ex(60.0 to 11, 60.0 to 10, 60.0 to 9)), WeightUnit.KG)
        assertEquals(listOf(11, 10, 9, 9), sets.map { it.reps })
        assertEquals(List(4) { 60.0 }, sets.map { it.weight })
    }

    @Test
    fun changeMarkerOnlyWhenWeightDiffers() {
        val r = rec(ex(60.0 to 10))
        assertEquals(0, weightChange(ex(60.0 to 8), r, WeightUnit.KG))
        assertEquals(1, weightChange(ex(62.5 to 8), r, WeightUnit.KG))
        assertEquals(-1, weightChange(ex(57.5 to 8), r, WeightUnit.KG))
    }
}
