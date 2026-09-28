package com.example.gymtracker

import com.example.gymtracker.data.Equipment
import com.example.gymtracker.data.ExerciseSet
import com.example.gymtracker.data.PresetExercise
import com.example.gymtracker.data.WeightUnit
import com.example.gymtracker.data.WorkoutExercise
import com.example.gymtracker.data.withBodyweightEquipment
import com.example.gymtracker.data.withCurrentDefaults
import com.example.gymtracker.data.withCurrentName
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

class RenameMigrationTest {
    private fun preset(name: String, equipment: Equipment?, perSide: Boolean = false) =
        PresetExercise(name, "Arms", 3, 8, 0.0, equipment, perSide)

    @Test
    fun presetsGetNewNamesAndDefaults() {
        val curl = preset("Barbell Curl", Equipment.BARBELL).withCurrentDefaults()
        assertEquals("Curl", curl.name)
        assertEquals(Equipment.CABLE, curl.equipment)
        assertEquals(false, curl.perSide)

        val preacher = preset("Preacher Curl", null).withCurrentDefaults()
        assertEquals(Equipment.DUMBBELL, preacher.equipment)
        assertEquals(true, preacher.perSide)

        assertEquals("Low Row", preset("Cable Row", Equipment.CABLE).withCurrentDefaults().name)
        // An explicit choice on an unrenamed exercise is kept.
        assertEquals(Equipment.SMITH, preset("Squat", Equipment.SMITH).withCurrentDefaults().equipment)
        assertEquals(Equipment.BARBELL, preset("Squat", null).withCurrentDefaults().equipment)
        assertEquals(Equipment.MACHINE, preset("Leg Press", null).withCurrentDefaults().equipment)
        // Custom exercises the library doesn't know stay as they are.
        assertEquals(null, preset("Zercher Carry", null).withCurrentDefaults().equipment)
    }

    @Test
    fun historyKeepsLoggedEquipment() {
        val ex = WorkoutExercise("e", "Barbell Curl", "Arms", emptyList(), equipment = Equipment.BARBELL).withCurrentName()
        assertEquals("Curl", ex.name)
        assertEquals(Equipment.BARBELL, ex.equipment)
    }
}

class LibraryTest {
    @Test
    fun namesCarryNoEquipment() {
        val words = Equipment.entries.flatMap { it.label.lowercase().split(" ") }.toSet()
        com.example.gymtracker.data.EXERCISE_LIBRARY.forEach { ex ->
            val clash = ex.name.lowercase().split(" ", "-").filter { it in words }
            assertEquals("${ex.name} names its equipment", emptyList<String>(), clash)
        }
    }
}
