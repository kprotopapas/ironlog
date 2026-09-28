package com.example.gymtracker

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.PresetExercise
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.withBodyweightEquipment
import org.junit.Assert.assertEquals
import org.junit.Test

class MigrationTest {
    private fun ex(name: String, weight: Double, equipment: Equipment? = null) =
        WorkoutExercise("e", name, "Back", listOf(ExerciseSet(8, weight, WeightUnit.KG, true)), equipment = equipment)

    @Test
    fun unloadedPullUpsBecomeBodyweight() {
        assertEquals(Equipment.BODYWEIGHT, ex("Pull-ups", 0.0).withBodyweightEquipment().equipment)
        assertEquals(Equipment.BODYWEIGHT, ex(" pull-ups ", 0.0).withBodyweightEquipment().equipment)
        assertEquals(Equipment.BODYWEIGHT, PresetExercise("Pull-ups", "Back", 3, 8, 0.0).withBodyweightEquipment().equipment)
    }

    @Test
    fun leavesAmbiguousOrOtherExercisesAlone() {
        assertEquals(null, ex("Pull-ups", 74.0).withBodyweightEquipment().equipment) // bodyweight typed in?
        assertEquals(Equipment.CABLE, ex("Pull-ups", 0.0, Equipment.CABLE).withBodyweightEquipment().equipment)
        assertEquals(null, ex("Shrugs", 0.0).withBodyweightEquipment().equipment)
    }
}
