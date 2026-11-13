package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.MuscleGroup
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExerciseDatasetTests {

    @Test
    fun testTotalExerciseCountIs1324() {
        val exercises = SeedData.defaultExercises
        assertEquals(1324, exercises.size, "Must contain exactly 1,324 exercises from exercises-dataset repo")
    }

    @Test
    fun testCategoryCountsMatchRepositoryDataset() {
        val exercises = SeedData.defaultExercises
        val byCategory = exercises.groupBy { it.primaryMuscle }

        assertEquals(163, byCategory[MuscleGroup.CHEST]?.size, "Dada count")
        assertEquals(203, byCategory[MuscleGroup.BACK]?.size, "Punggung count")
        assertEquals(286, byCategory[MuscleGroup.LEGS]?.size, "Kaki count")
        assertEquals(143, byCategory[MuscleGroup.SHOULDERS]?.size, "Bahu count")
        assertEquals(329, byCategory[MuscleGroup.ARMS]?.size, "Lengan count")
        assertEquals(169, byCategory[MuscleGroup.CORE]?.size, "Core count")
        assertEquals(29, byCategory[MuscleGroup.CARDIO]?.size, "Kardio count")
        assertEquals(2, byCategory[MuscleGroup.NECK]?.size, "Leher count")
    }

    @Test
    fun testInvertedRowHasCorrectDataAndMediaPaths() {
        val invertedRow = SeedData.defaultExercises.find { it.id == "0499" }
        assertNotNull(invertedRow, "Inverted Row (0499) must exist")
        assertEquals("Inverted Row", invertedRow.name)
        assertEquals(MuscleGroup.BACK, invertedRow.primaryMuscle)
        assertEquals("upper back", invertedRow.subMuscle)
        assertEquals("images/0499-bZGHsAZ.jpg", invertedRow.thumbnailUrl)
        assertEquals("videos/0499-bZGHsAZ.gif", invertedRow.gifUrl)
        assertEquals(Equipment.BODYWEIGHT, invertedRow.equipment)
        assertTrue(invertedRow.instructionSteps.isNotEmpty(), "Instruction steps must be present")
    }

    @Test
    fun testBarbellBenchPressHasCorrectDataAndMediaPaths() {
        val benchPress = SeedData.defaultExercises.find { it.id == "0025" }
        assertNotNull(benchPress, "Barbell Bench Press (0025) must exist")
        assertEquals("Barbell Bench Press", benchPress.name)
        assertEquals(MuscleGroup.CHEST, benchPress.primaryMuscle)
        assertEquals("pectorals", benchPress.subMuscle)
        assertEquals("images/0025-EIeI8Vf.jpg", benchPress.thumbnailUrl)
        assertEquals("videos/0025-EIeI8Vf.gif", benchPress.gifUrl)
        assertEquals(Equipment.BARBELL, benchPress.equipment)
        assertTrue(benchPress.instructionSteps.isNotEmpty(), "Instruction steps must be present")
    }

    @Test
    fun testAttributionIsIntactForEveryExercise() {
        val exercises = SeedData.defaultExercises
        for (ex in exercises) {
            assertTrue(
                ex.attribution.contains("Gym visual"),
                "Exercise ${ex.id} (${ex.name}) must have Gym visual attribution"
            )
        }
    }
}
