package com.bearbones.kumalift

import com.bearbones.kumalift.core.util.OneRepMaxCalculator
import com.bearbones.kumalift.core.util.VolumeCalculator
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KumaLiftTests {

    @Test
    fun testOneRepMaxFormulas() {
        // Single rep should always equal the weight itself
        assertEquals(100.0, OneRepMaxCalculator.calculateEpley(100.0, 1))
        assertEquals(100.0, OneRepMaxCalculator.calculateBrzycki(100.0, 1))

        // 100 kg x 10 reps with Epley: 100 * (1 + 10 / 30) = 133.33 kg
        val epley = OneRepMaxCalculator.calculateEpley(100.0, 10)
        assertTrue(epley > 133.3 && epley < 133.4)

        // 100 kg x 10 reps with Brzycki: 100 / (1.0278 - 0.0278 * 10) = 133.36 kg
        val brzycki = OneRepMaxCalculator.calculateBrzycki(100.0, 10)
        assertTrue(brzycki > 133.3 && brzycki < 133.4)

        // Blended/rounded test
        val estimated = OneRepMaxCalculator.calculateEstimated1Rm(100.0, 10)
        assertEquals(133.3, estimated)
    }

    @Test
    fun testVolumeCalculations() {
        val benchPress = Exercise(
            id = "bench",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL
        )

        val sets = listOf(
            SetLog("1", 1, weightKg = 100.0, reps = 10, isCompleted = true),  // 1,000 kg
            SetLog("2", 2, weightKg = 100.0, reps = 8, isCompleted = true),   // 800 kg
            SetLog("3", 3, weightKg = 100.0, reps = 6, isCompleted = false)  // Not completed -> 0
        )

        val workoutExercise = WorkoutExercise("we1", benchPress, sets)
        assertEquals(1800.0, workoutExercise.totalVolumeKg)
        assertEquals(2, workoutExercise.completedSetsCount)

        val workout = Workout(
            id = "w1",
            title = "Chest Day",
            startTimeEpochMs = 0L,
            exercises = listOf(workoutExercise)
        )

        assertEquals(1800.0, VolumeCalculator.calculateTotalVolumeKg(workout))

        val distribution = VolumeCalculator.calculateMuscleDistribution(workout)
        assertEquals(100f, distribution[MuscleGroup.CHEST])
    }

    @Test
    fun testPersonalRecordDetection() {
        val prRepo = PersonalRecordRepository()
        val squat = Exercise(
            id = "0043",
            name = "Barbell Full Squat",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL,
            currentPrWeightKg = 150.0,
            currentPrReps = 5
        )

        // Lower weight: should not trigger PR
        val noPr = prRepo.detectAndRecordPr(squat, 140.0, 5)
        assertNull(noPr)

        // Higher weight (155 kg): should trigger PR
        val newPr = prRepo.detectAndRecordPr(squat, 155.0, 5)
        assertNotNull(newPr)
        assertEquals(155.0, newPr.weightKg)
        assertEquals(5.0, newPr.deltaWeightKg)
    }

    @Test
    fun testDomainMathEdgeCasesAndPlateEpsilon() {
        assertEquals(0.0, OneRepMaxCalculator.calculateEpley(Double.NaN, 5))
        assertEquals(0.0, OneRepMaxCalculator.calculateEstimated1Rm(Double.POSITIVE_INFINITY, 3))
        assertEquals(100.0, SetLog(id = "s1", setNumber = 1, weightKg = 100.0, reps = 1).estimated1RmEpley)

        // Binary float drift just below 22.5 kg must still match 1.25 kg plate per side cleanly
        val plateResult = com.bearbones.kumalift.domain.model.PlateCalculator.calculate(
            targetWeightKg = 22.499999,
            barWeightKg = 20.0
        )
        assertEquals(1, plateResult.platesPerSide.size)
        assertEquals(1.25, plateResult.platesPerSide.first().weightKg)

        // Dumbbell (barWeightKg = 0.0) warmup sets must not be 0.0 kg
        val dbWarmups = com.bearbones.kumalift.domain.model.WarmupSetCalculator.generateWarmupSets(
            workingWeightKg = 30.0,
            barWeightKg = 0.0,
            weightStepKg = 2.5
        )
        assertTrue(dbWarmups.isNotEmpty())
        assertTrue(dbWarmups.all { it.weightKg > 0.0 && it.setType == com.bearbones.kumalift.domain.model.SetType.WARMUP })

        val formattedMinLong = com.bearbones.kumalift.core.util.NumberFormatUtils.formatIntegerWithSeparators(
            Long.MIN_VALUE,
            com.bearbones.kumalift.domain.model.AppLanguage.EN
        )
        assertTrue(formattedMinLong.startsWith("-9,223"), "Unexpected MIN_VALUE format: $formattedMinLong")
        kotlin.test.assertFalse(formattedMinLong.startsWith("--"))
    }
}
