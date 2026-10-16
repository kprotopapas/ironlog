package com.example.gymtracker

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.WeightAction
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.Workout
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.PresetExercise
import com.example.gymtracker.data.Recommendation
import com.example.gymtracker.data.prefilledSets
import com.example.gymtracker.data.recommend
import com.example.gymtracker.data.strengthCall
import com.example.gymtracker.data.weightChange
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecommendationTest {
    private fun ex(vararg sets: Pair<Double, Int>, equipment: Equipment? = Equipment.BARBELL, unit: WeightUnit = WeightUnit.KG, perSide: Boolean = false) =
        WorkoutExercise("e", "Bench Press", "Chest", sets.map { (w, r) -> ExerciseSet(r, w, unit, true) }, equipment = equipment, perSide = perSide)

    private fun history(vararg exercises: WorkoutExercise) =
        exercises.mapIndexed { i, e -> Workout("w$i", "W", i * 1000L, 0, listOf(e)) }

    private fun rec(vararg exercises: WorkoutExercise, current: WorkoutExercise = exercises.last()) =
        recommend(current, history(*exercises), WeightUnit.KG)!!

    private fun weights(r: Recommendation) = r.nextSets.map { it.weight }

    @Test
    fun beatingRecentBestGoesUpAndKeepsReps() {
        val r = rec(ex(60.0 to 8, 60.0 to 8), ex(60.0 to 10, 60.0 to 9))
        assertEquals(WeightAction.INCREASE, r.action)
        assertEquals(listOf(62.5 to 10, 62.5 to 9), r.nextSets.map { it.weight to it.reps })
    }

    @Test
    fun matchingBestStays() {
        val r = rec(ex(60.0 to 10), ex(60.0 to 10))
        assertEquals(WeightAction.HOLD, r.action)
        assertEquals(listOf(60.0), weights(r))
    }

    @Test
    fun plannedHeavyDoubleAtUsualStrengthStays() {
        // 75 × 2 and 60 × 10 are both an estimated 80 kg: on target, not a failed set.
        assertEquals(WeightAction.HOLD, rec(ex(60.0 to 10), ex(75.0 to 2)).action)
    }

    @Test
    fun heavyPyramidThatBeatsBestGoesUpAndStaysAPyramid() {
        // A side: 26 × 8 the time before, then a pyramid topping out at 32 × 2.
        val before = ex(20.0 to 15, 26.0 to 8, 24.0 to 8, equipment = Equipment.DUMBBELL, perSide = true)
        val pyramid = ex(24.0 to 12, 26.0 to 6, 28.0 to 4, 30.0 to 3, 32.0 to 2, equipment = Equipment.DUMBBELL, perSide = true)
        val r = rec(before, pyramid)
        assertEquals(WeightAction.INCREASE, r.action)
        assertEquals(listOf(26.0 to 12, 28.0 to 6, 30.0 to 4, 32.0 to 3, 34.0 to 2), r.nextSets.map { it.weight to it.reps })
        assertEquals(32.0 to 2, r.bestSet.weight to r.bestSet.reps)
    }

    @Test
    fun oneBadSessionStays() {
        val r = rec(ex(60.0 to 10), ex(60.0 to 4))
        assertEquals(WeightAction.HOLD, r.action)
    }

    @Test
    fun twoBadSessionsRunningGoDown() {
        val r = rec(ex(60.0 to 10), ex(60.0 to 4), ex(60.0 to 4))
        assertEquals(WeightAction.DECREASE, r.action)
        assertEquals(listOf(57.5), weights(r))
    }

    @Test
    fun firstSessionHasNothingToBeat() {
        assertEquals(WeightAction.HOLD, rec(ex(60.0 to 12, 60.0 to 12)).action)
    }

    @Test
    fun onlyTheLastFewSessionsSetTheBar() {
        // A best from four sessions ago no longer counts, so 60 × 8 beats the three after it.
        val r = rec(ex(100.0 to 5), ex(60.0 to 6), ex(60.0 to 6), ex(60.0 to 6), ex(60.0 to 8))
        assertEquals(WeightAction.INCREASE, r.action)
    }

    @Test
    fun strengthCallThresholds() {
        assertEquals(WeightAction.HOLD, strengthCall(listOf(100.0)))
        assertEquals(WeightAction.INCREASE, strengthCall(listOf(100.0, 101.0)))
        assertEquals(WeightAction.HOLD, strengthCall(listOf(100.0, 100.3)))
        assertEquals(WeightAction.HOLD, strengthCall(listOf(100.0, 91.0, 91.0)))
        assertEquals(WeightAction.DECREASE, strengthCall(listOf(100.0, 89.0, 89.0)))
        assertEquals(WeightAction.HOLD, strengthCall(listOf(100.0, 89.0, 95.0)))
    }

    @Test
    fun stepsFollowEquipment() {
        fun up(equipment: Equipment, w: Double, perSide: Boolean = false) =
            rec(ex(w to 10, equipment = equipment, perSide = perSide), ex(w to 12, equipment = equipment, perSide = perSide)).nextWeight
        assertEquals(24.0, up(Equipment.DUMBBELL, 22.0, perSide = true), 1e-9)
        assertEquals(48.0, up(Equipment.DUMBBELL, 44.0), 1e-9) // a pair, as a total
        assertEquals(55.0, up(Equipment.MACHINE, 50.0), 1e-9)
    }

    @Test
    fun unassistedPullUpsNeverGoBelowBodyweight() {
        val weak = rec(ex(0.0 to 10, equipment = Equipment.BODYWEIGHT), ex(0.0 to 4, equipment = Equipment.BODYWEIGHT), ex(0.0 to 4, equipment = Equipment.BODYWEIGHT))
        assertEquals(WeightAction.HOLD, weak.action)
        val strong = rec(ex(0.0 to 8, equipment = Equipment.BODYWEIGHT), ex(0.0 to 12, equipment = Equipment.BODYWEIGHT))
        assertEquals(WeightAction.INCREASE, strong.action)
        assertEquals(2.5, strong.nextWeight, 1e-9)
    }

    @Test
    fun otherEquipmentCountsInTheCurrentConvention() {
        // Last time 10 kg a side on cables; now on a machine logged as a total.
        val machine = ex(equipment = Equipment.MACHINE)
        val r = recommend(machine, history(ex(10.0 to 10, equipment = Equipment.CABLE, perSide = true)), WeightUnit.KG)!!
        assertEquals(20.0, r.lastWeight, 1e-9)
        assertEquals(0, weightChange(ex(20.0 to 8, equipment = Equipment.MACHINE), r, WeightUnit.KG))
        // And back: 20 kg total reads as 10 kg a side.
        val cable = ex(equipment = Equipment.CABLE, perSide = true)
        assertEquals(10.0, recommend(cable, history(ex(20.0 to 10, equipment = Equipment.MACHINE)), WeightUnit.KG)!!.lastWeight, 1e-9)
    }

    @Test
    fun bodyweightIsADifferentExercise() {
        assertNull(recommend(ex(equipment = Equipment.BODYWEIGHT), history(ex(60.0 to 12)), WeightUnit.KG))
    }

    @Test
    fun convertsToDisplayUnit() {
        val r = rec(ex(100.0 to 10, unit = WeightUnit.LBS), ex(100.0 to 12, unit = WeightUnit.LBS))
        assertEquals(100.0 / 2.20462262 + 2.5, r.nextWeight, 1e-6)
    }

    private fun preset(sets: Int = 3) = PresetExercise("Bench Press", "Chest", sets, 10, 40.0, Equipment.BARBELL)

    @Test
    fun presetWithoutHistoryUsesItsDefaults() {
        val sets = prefilledSets(preset(), emptyList(), WeightUnit.KG)
        assertEquals(List(3) { 40.0 to 10 }, sets.map { it.weight to it.reps })
    }

    @Test
    fun presetCarriesLastSessionsShape() {
        // The preset says 3 sets, but last time was 2 at different weights: those are what's repeated, a step up.
        val sets = prefilledSets(preset(), history(ex(60.0 to 8), ex(60.0 to 12, 50.0 to 10)), WeightUnit.KG)
        assertEquals(listOf(62.5 to 12, 52.5 to 10), sets.map { it.weight to it.reps })
    }

    @Test
    fun presetHoldsWithLastRepsToBeat() {
        val sets = prefilledSets(preset(), history(ex(60.0 to 11, 60.0 to 10, 60.0 to 9)), WeightUnit.KG)
        assertEquals(listOf(60.0 to 11, 60.0 to 10, 60.0 to 9), sets.map { it.weight to it.reps })
    }

    @Test
    fun changeMarkerOnlyWhenWeightDiffers() {
        val r = rec(ex(60.0 to 10))
        assertEquals(0, weightChange(ex(60.0 to 8), r, WeightUnit.KG))
        assertEquals(1, weightChange(ex(62.5 to 8), r, WeightUnit.KG))
        assertEquals(-1, weightChange(ex(57.5 to 8), r, WeightUnit.KG))
    }
}
