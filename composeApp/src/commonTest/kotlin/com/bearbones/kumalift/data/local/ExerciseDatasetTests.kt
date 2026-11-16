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

    @Test
    fun testExerciseDataDictionariesParityAndNonBlankLabels() {
        val exercises = SeedData.defaultExercises
        val idStrings = com.bearbones.kumalift.core.localization.IdStrings
        val enStrings = com.bearbones.kumalift.core.localization.EnStrings

        for (ex in exercises) {
            if (ex.subMuscle.isNotBlank()) {
                val idSub = idStrings.subMuscleLabel(ex.subMuscle)
                val enSub = enStrings.subMuscleLabel(ex.subMuscle)
                assertTrue(idSub.isNotBlank(), "Id subMuscleLabel must not be blank for ${ex.subMuscle}")
                assertTrue(enSub.isNotBlank(), "En subMuscleLabel must not be blank for ${ex.subMuscle}")
            }

            if (ex.equipmentDetail.isNotBlank()) {
                val idEq = idStrings.equipmentDetailLabel(ex.equipmentDetail)
                val enEq = enStrings.equipmentDetailLabel(ex.equipmentDetail)
                assertTrue(idEq.isNotBlank(), "Id equipmentDetailLabel must not be blank for ${ex.equipmentDetail}")
                assertTrue(enEq.isNotBlank(), "En equipmentDetailLabel must not be blank for ${ex.equipmentDetail}")
            }

            if (ex.secondaryMuscleDetails.isNotBlank()) {
                val tokens = ex.secondaryMuscleDetails.split(",").map { it.trim() }.filter { it.isNotBlank() }
                for (token in tokens) {
                    val idSec = idStrings.secondaryMuscleDetailLabel(token)
                    val enSec = enStrings.secondaryMuscleDetailLabel(token)
                    assertTrue(idSec.isNotBlank(), "Id secondaryMuscleDetailLabel must not be blank for $token")
                    assertTrue(enSec.isNotBlank(), "En secondaryMuscleDetailLabel must not be blank for $token")
                }
            }
        }
    }

    @Test
    fun testExercisesIdJsonCoverageAndParity() {
        val payload = ExerciseDataset.indonesianInstructionsPayload
        assertNotNull(payload, "exercises_id.json must load successfully")
        assertEquals("machine-translated", payload.source, "Source must be recorded as machine-translated")
        assertEquals(1324, payload.exercises.size, "exercises_id.json must contain instructions for all 1,324 exercises")

        val allIds = ExerciseDataset.allExerciseIds
        for ((id, steps) in payload.exercises) {
            assertTrue(allIds.contains(id), "Every id ($id) in exercises_id.json must exist in exercises.json")
            assertTrue(steps.isNotEmpty(), "Exercise $id in exercises_id.json must have non-empty instruction steps")
            for (step in steps) {
                assertTrue(step.isNotBlank(), "Step in exercise $id must not be blank")
            }
        }
    }

    @Test
    fun testEffectiveStepsLocalization() {
        val idStrings = com.bearbones.kumalift.core.localization.IdStrings
        val enStrings = com.bearbones.kumalift.core.localization.EnStrings

        val ex = SeedData.defaultExercises.first { it.id == "0001" }
        val idSteps = ex.getEffectiveSteps(idStrings)
        val enSteps = ex.getEffectiveSteps(enStrings)

        assertTrue(idSteps.isNotEmpty(), "Indonesian steps should not be empty")
        assertTrue(enSteps.isNotEmpty(), "English steps should not be empty")
        assertTrue(idSteps.first().contains("Berbaring telentang"), "Indonesian step 1 should be in Indonesian: ${idSteps.first()}")
        assertTrue(enSteps.first().contains("Lie flat on your back"), "English step 1 should be in English: ${enSteps.first()}")
    }
}
