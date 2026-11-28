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
import com.bearbones.kumalift.domain.model.PlateCalculator
import com.bearbones.kumalift.domain.model.PrMetricType
import com.bearbones.kumalift.domain.model.RoutineBlueprint
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

    @Test
    fun testCancelWorkoutRestoresPreviousPrFromHistory() {
        val calcSettings = CalculationSettings()
        val prRepo = PersonalRecordRepository(calculationSettingsProvider = { calcSettings })
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            calculationSettingsProvider = { calcSettings }
        )
        val bench = Exercise(
            id = "ex_bench_cancel",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        val pastWorkout = Workout(
            id = "w_past_cancel",
            title = "Past Push",
            startTimeEpochMs = 1000L,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_past_cancel",
                    exercise = bench,
                    sets = listOf(
                        SetLog(
                            id = "s_past_cancel_1",
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

        workoutRepo.startEmptyWorkout("Active Cancel")
        workoutRepo.addExerciseToActiveWorkout(bench)
        val we = workoutRepo.activeWorkout.value!!.exercises.first()
        val setId = we.sets.first().id
        workoutRepo.updateSet(we.id, setId, weightKg = 120.0, reps = 5)
        workoutRepo.toggleSetCompletion(we.id, setId) {}
        assertEquals(120.0, prRepo.personalRecords.value.find { it.exerciseId == bench.id }?.weightKg)

        workoutRepo.cancelWorkout()
        assertNull(workoutRepo.activeWorkout.value)
        val restored = prRepo.personalRecords.value.find { it.exerciseId == bench.id }
        assertNotNull(restored)
        assertEquals(100.0, restored.weightKg)
    }

    @Test
    fun testUpdateSetWeightDropSetTypeWarmupRevokesPrAndFinishClearsPausedState() {
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(prRepository = prRepo)
        val bench = Exercise(
            id = "ex_bench_rev",
            name = "Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        workoutRepo.startEmptyWorkout("Active Revoke")
        workoutRepo.addExerciseToActiveWorkout(bench)
        val we = workoutRepo.activeWorkout.value!!.exercises.first()
        val setId = we.sets.first().id

        // Record PR then drop weight -> revoked
        workoutRepo.updateSet(we.id, setId, weightKg = 110.0, reps = 5)
        workoutRepo.toggleSetCompletion(we.id, setId) {}
        assertEquals(110.0, prRepo.personalRecords.value.find { it.exerciseId == bench.id }?.weightKg)

        workoutRepo.updateSet(we.id, setId, weightKg = 0.0, reps = 5)
        assertNull(prRepo.personalRecords.value.find { it.exerciseId == bench.id })
        assertFalse(workoutRepo.activeWorkout.value!!.exercises.first().sets.first().isPr)

        // Restore PR then mark as WARMUP -> revoked
        workoutRepo.updateSet(we.id, setId, weightKg = 110.0, reps = 5)
        assertEquals(110.0, prRepo.personalRecords.value.find { it.exerciseId == bench.id }?.weightKg)
        workoutRepo.updateSetType(we.id, setId, SetType.WARMUP)
        assertNull(prRepo.personalRecords.value.find { it.exerciseId == bench.id })
        assertFalse(workoutRepo.activeWorkout.value!!.exercises.first().sets.first().isPr)

        // Pausing then finishing must clear isPaused, and a second finish call returns null
        workoutRepo.pauseWorkout()
        val finished = workoutRepo.finishWorkout(sessionRpe = 8.0)
        assertNotNull(finished)
        assertFalse(finished.isPaused)
        assertNull(finished.pausedAtEpochMs)
        assertNull(workoutRepo.finishWorkout())
    }

    @Test
    fun testStartingNewWorkoutCancelsPreviousActiveSessionAndRevokesSessionPrs() {
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(prRepository = prRepo)
        val squat = Exercise(
            id = "ex_squat_overwrite",
            name = "Barbell Squat",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        workoutRepo.startEmptyWorkout("First Unfinished")
        workoutRepo.addExerciseToActiveWorkout(squat)
        val we1 = workoutRepo.activeWorkout.value!!.exercises.first()
        val s1 = we1.sets.first()
        workoutRepo.updateSet(we1.id, s1.id, weightKg = 200.0, reps = 3)
        workoutRepo.toggleSetCompletion(we1.id, s1.id) {}
        assertEquals(200.0, prRepo.personalRecords.value.find { it.exerciseId == squat.id }?.weightKg)

        // Starting another workout must revoke discarded active-session PRs
        workoutRepo.startEmptyWorkout("Replacement Session")
        assertNull(prRepo.personalRecords.value.find { it.exerciseId == squat.id })

        // Same when replacing via startWorkoutFromRoutine
        val we2Id = workoutRepo.activeWorkout.value?.let {
            workoutRepo.addExerciseToActiveWorkout(squat)
            val addedWe = workoutRepo.activeWorkout.value!!.exercises.first()
            val addedSet = addedWe.sets.first()
            workoutRepo.updateSet(addedWe.id, addedSet.id, weightKg = 190.0, reps = 3)
            workoutRepo.toggleSetCompletion(addedWe.id, addedSet.id) {}
            addedWe.id
        }
        assertNotNull(we2Id)
        assertEquals(190.0, prRepo.personalRecords.value.find { it.exerciseId == squat.id }?.weightKg)

        val routine = RoutineBlueprint(
            id = "rt_replace_check",
            title = "Leg Routine",
            splitCategory = "Legs",
            muscleFocus = "Quads",
            estimatedDurationMinutes = 45,
            exerciseTemplates = listOf(squat)
        )
        workoutRepo.startWorkoutFromRoutine(routine)
        assertNull(prRepo.personalRecords.value.find { it.exerciseId == squat.id })
    }

    @Test
    fun testFinishWorkoutAggregatesCompletedSetHeartRateBpm() {
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(prRepository = prRepo)
        val row = Exercise(
            id = "ex_row_hr",
            name = "Barbell Row",
            primaryMuscle = MuscleGroup.BACK,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        workoutRepo.startEmptyWorkout("HR Aggregate")
        workoutRepo.addExerciseToActiveWorkout(row)
        val we = workoutRepo.activeWorkout.value!!.exercises.first()
        val set1 = we.sets[0]
        val set2 = we.sets[1]
        workoutRepo.toggleSetCompletion(we.id, set1.id, currentHeartRateBpm = 140) {}
        workoutRepo.toggleSetCompletion(we.id, set2.id, currentHeartRateBpm = 160) {}

        val finished = workoutRepo.finishWorkout()
        assertNotNull(finished)
        assertEquals(150, finished.avgHeartRateBpm)
        assertEquals(160, finished.peakHeartRateBpm)
    }

    @Test
    fun testPlateCalculatorSupportsImperialPlatesPalette() {
        val calc = PlateCalculator.calculate(
            targetWeightKg = 225.0,
            barWeightKg = 45.0,
            availablePlatesKg = listOf(45.0, 35.0, 25.0, 10.0, 5.0, 2.5),
            useImperialPalette = true
        )
        assertTrue(calc.isExactMatch)
        assertEquals(90.0, calc.weightPerSideKg, 0.001)
        assertEquals(1, calc.platesPerSide.size)
        assertEquals(45.0, calc.platesPerSide.first().weightKg)
        assertEquals(2, calc.platesPerSide.first().countPerSide)
        assertEquals(0xFF1E88E5, calc.platesPerSide.first().hexColor)
    }
}
