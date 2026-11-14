package com.bearbones.kumalift

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import com.bearbones.kumalift.domain.model.PrMetricType
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WorkoutLogicTests {

    @Test
    fun testDuplicateExerciseInWorkoutCanBeToggledIndependently() {
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(prRepository = prRepo)

        val benchExercise = Exercise(
            id = "ex_bench_flat",
            name = "Barbell Flat Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )

        // Start workout and add bench press TWICE (e.g. main strength work + burnout sets at the end)
        workoutRepo.startEmptyWorkout("Push Day")
        workoutRepo.addExerciseToActiveWorkout(benchExercise)
        workoutRepo.addExerciseToActiveWorkout(benchExercise)

        val active = workoutRepo.activeWorkout.value!!
        assertEquals(2, active.exercises.size)

        val firstInstance = active.exercises[0]
        val secondInstance = active.exercises[1]

        // Verified unique instance IDs
        assertNotEquals(firstInstance.id, secondInstance.id)
        assertEquals(benchExercise.id, firstInstance.exercise.id)
        assertEquals(benchExercise.id, secondInstance.exercise.id)

        val setFirst = firstInstance.sets[0]
        val setSecond = secondInstance.sets[0]
        assertNotEquals(setFirst.id, setSecond.id)

        // 1. Toggle set on the FIRST instance
        workoutRepo.toggleSetCompletion(
            exerciseId = firstInstance.id,
            setId = setFirst.id
        ) {}

        val updatedWorkout1 = workoutRepo.activeWorkout.value!!
        assertTrue(updatedWorkout1.exercises[0].sets[0].isCompleted, "First exercise set 1 should be completed")
        assertFalse(updatedWorkout1.exercises[1].sets[0].isCompleted, "Second exercise set 1 must remain uncompleted")

        // 2. Update weight on the SECOND instance
        workoutRepo.updateSet(
            exerciseId = secondInstance.id,
            setId = setSecond.id,
            weightKg = 60.0,
            reps = 15
        )

        val updatedWorkout2 = workoutRepo.activeWorkout.value!!
        assertEquals(benchExercise.currentPrWeightKg?.let { it * 0.7 } ?: 20.0, updatedWorkout2.exercises[0].sets[0].weightKg)
        assertEquals(60.0, updatedWorkout2.exercises[1].sets[0].weightKg)
        assertEquals(15, updatedWorkout2.exercises[1].sets[0].reps)

        // 3. Toggle set on the SECOND instance
        workoutRepo.toggleSetCompletion(
            exerciseId = secondInstance.id,
            setId = setSecond.id
        ) {}

        val updatedWorkout3 = workoutRepo.activeWorkout.value!!
        assertTrue(updatedWorkout3.exercises[0].sets[0].isCompleted)
        assertTrue(updatedWorkout3.exercises[1].sets[0].isCompleted)
    }

    @Test
    fun testUniqueIdGeneration() {
        val id1 = IdGenerator.workoutId("freestyle")
        val id2 = IdGenerator.workoutId("freestyle")
        assertNotEquals(id1, id2)

        val setId1 = IdGenerator.setId("ex_1", 1)
        val setId2 = IdGenerator.setId("ex_1", 1)
        assertNotEquals(setId1, setId2)

        val exId1 = IdGenerator.customExerciseId("My Pushup")
        val exId2 = IdGenerator.customExerciseId("My Pushup")
        assertNotEquals(exId1, exId2)
    }

    @Test
    fun testPrDetectionMetricsAndRepRangeAndDateFormat() {
        var currentDateFormat = DateFormatPreference.DMY
        var currentCalcSettings = CalculationSettings(
            oneRepMaxFormula = OneRepMaxFormula.EPLEY,
            maxRepsForEstimate = 10,
            prMetrics = setOf(PrMetricType.WEIGHT, PrMetricType.E1RM),
            repRangePrs = true
        )

        val prRepo = PersonalRecordRepository(
            calculationSettingsProvider = { currentCalcSettings },
            dateFormatProvider = { currentDateFormat }
        )

        val exercise = Exercise(
            id = "ex_squat",
            name = "Barbell Squat",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND,
            currentPrWeightKg = 90.0
        )

        // 1. Warmup and Drop sets should never trigger a PR
        val warmupPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 100.0,
            reps = 5,
            sourceSetId = "s_warmup",
            setType = SetType.WARMUP
        )
        assertNull(warmupPr, "Warmup sets must not be PRs")

        val dropPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 100.0,
            reps = 5,
            sourceSetId = "s_drop",
            setType = SetType.DROP
        )
        assertNull(dropPr, "Drop sets must not be PRs")

        // 2. Normal set beats baseline (90kg) and triggers a PR with DMY date format and 5RM tier
        val firstPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 100.0,
            reps = 5,
            sourceSetId = "s_1",
            setType = SetType.NORMAL
        )
        assertNotNull(firstPr)
        assertEquals(100.0, firstPr.weightKg)
        assertEquals(5, firstPr.reps)
        assertEquals("5RM PR", firstPr.prTier)
        assertTrue(firstPr.dateAchieved.contains("/"), "Date should be formatted as DD/MM/YYYY: ${firstPr.dateAchieved}")

        // 3. Lower weight set with fewer reps should not trigger a PR
        val lowerSet = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 90.0,
            reps = 3,
            sourceSetId = "s_2",
            setType = SetType.NORMAL
        )
        assertNull(lowerSet)

        // 4. Rep-range coexistence: 90 kg x 10 is lower weight than 100 kg, but it is a NEW 10RM PR
        val tenRmPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 90.0,
            reps = 10,
            sourceSetId = "s_3",
            setType = SetType.NORMAL
        )
        assertNotNull(tenRmPr, "90kg x 10 reps should qualify as a 10RM PR")
        assertEquals(10, tenRmPr.reps)
        assertEquals("10RM PR", tenRmPr.prTier)

        // Verify that BOTH the 5RM PR and 10RM PR coexist in personalRecords
        val prList = prRepo.personalRecords.value.filter { it.exerciseId == exercise.id }
        assertEquals(2, prList.size, "Both 5RM and 10RM records must coexist")
        assertTrue(prList.any { it.reps == 5 && it.weightKg == 100.0 })
        assertTrue(prList.any { it.reps == 10 && it.weightKg == 90.0 })

        // 5. Test Revoking PR when source set is unchecked: 10RM PR is removed, 5RM PR remains
        prRepo.revokePrBySetId("s_3")
        assertNull(prRepo.personalRecords.value.find { it.sourceSetId == "s_3" })
        assertNotNull(prRepo.personalRecords.value.find { it.reps == 5 })

        // 6. Test YMD Date format with WEIGHT metric
        currentDateFormat = DateFormatPreference.YMD
        currentCalcSettings = currentCalcSettings.copy(prMetrics = setOf(PrMetricType.WEIGHT, PrMetricType.E1RM))
        val ymdPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 120.0,
            reps = 1,
            sourceSetId = "s_4",
            setType = SetType.NORMAL
        )
        assertNotNull(ymdPr)
        assertEquals("1RM PR", ymdPr.prTier)
        assertTrue(ymdPr.dateAchieved.contains("-"), "Date should be formatted as YYYY-MM-DD: ${ymdPr.dateAchieved}")
    }

    @Test
    fun testUncheckingSetRestoresPreviousPrFromHistory() {
        val calcSettings = CalculationSettings(
            repRangePrs = true,
            prMetrics = setOf(PrMetricType.WEIGHT, PrMetricType.E1RM)
        )
        val prRepo = PersonalRecordRepository(
            calculationSettingsProvider = { calcSettings }
        )
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            calculationSettingsProvider = { calcSettings }
        )

        val exercise = Exercise(
            id = "ex_bench",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND,
            currentPrWeightKg = 80.0
        )

        // 1. Establish an earlier completed workout in history with 100 kg x 5 reps
        val pastWorkout = Workout(
            id = "w_past",
            title = "Past Push",
            startTimeEpochMs = 1000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_past",
                    exercise = exercise,
                    sets = listOf(
                        SetLog(
                            id = "s_past_1",
                            setNumber = 1,
                            weightKg = 100.0,
                            reps = 5,
                            isCompleted = true,
                            setType = SetType.NORMAL
                        )
                    )
                )
            ),
            isFinished = true
        )
        workoutRepo.setCompletedWorkoutsForTesting(listOf(pastWorkout))
        prRepo.recomputePersonalRecords(listOf(pastWorkout), calcSettings)

        // Ensure 100kg is the active PR
        val recordBefore = prRepo.personalRecords.value.find { it.exerciseId == exercise.id }
        assertNotNull(recordBefore)
        assertEquals(100.0, recordBefore.weightKg)

        // 2. In active workout, user logs 110 kg x 5 reps (triggers new PR)
        workoutRepo.startEmptyWorkout("Active Push")
        workoutRepo.addExerciseToActiveWorkout(exercise)
        val activeWe = workoutRepo.activeWorkout.value!!.exercises[0]
        val activeSetId = activeWe.sets[0].id

        workoutRepo.updateSet(activeWe.id, activeSetId, weightKg = 110.0, reps = 5)
        workoutRepo.toggleSetCompletion(activeWe.id, activeSetId) {}

        // New PR of 110kg is active
        val recordAfter = prRepo.personalRecords.value.find { it.exerciseId == exercise.id }
        assertNotNull(recordAfter)
        assertEquals(110.0, recordAfter.weightKg)

        // 3. User un-checks the 110 kg set (cancelling completion)
        workoutRepo.toggleSetCompletion(activeWe.id, activeSetId) {}

        // 4. VERIFY: The previous 100kg record from past workout is fully restored!
        val recordRestored = prRepo.personalRecords.value.find { it.exerciseId == exercise.id }
        assertNotNull(recordRestored, "Previous PR must be restored from history, not left empty")
        assertEquals(100.0, recordRestored.weightKg, "Restored PR weight should be 100kg from past session")
    }
}
