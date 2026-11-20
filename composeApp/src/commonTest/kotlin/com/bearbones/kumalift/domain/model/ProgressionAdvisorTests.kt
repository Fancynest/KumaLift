package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.data.repository.WorkoutRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ProgressionAdvisorTests {

    private val benchPress = Exercise(
        id = "ex_bench_press",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val squat = Exercise(
        id = "ex_squat",
        name = "Barbell Back Squat",
        primaryMuscle = MuscleGroup.LEGS,
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val bicepCurl = Exercise(
        id = "ex_curl",
        name = "Dumbbell Bicep Curl",
        primaryMuscle = MuscleGroup.ARMS,
        movementType = MovementType.ISOLATION,
        equipment = Equipment.DUMBBELL,
        defaultIncrementKg = 1.25
    )

    @Test
    fun testBaselineAdviceWhenNoPreviousSets() {
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = emptyList(),
            currentWeightKg = 50.0
        )
        assertTrue(advice is ProgressionAdvice.Baseline)
        assertEquals(50.0, advice.suggestedWeightKg)
    }

    @Test
    fun testIncreaseWeightWhenUpperRepLimitReached() {
        // Hypertrophy compound range: 6..10
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 80.0, reps = 10, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 80.0, reps = 10, isCompleted = true),
            SetLog(id = "3", setNumber = 3, weightKg = 80.0, reps = 10, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = sets,
            currentWeightKg = 80.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.IncreaseWeight)
        assertEquals(2.5, advice.deltaKg)
        assertEquals(82.5, advice.suggestedWeightKg)
        assertEquals(6, advice.targetReps)
        assertNotNull(advice.previousE1RmKg)
        assertNotNull(advice.projectedE1RmKg)
    }

    @Test
    fun testLegCompoundUsesLargerIncrement() {
        // Leg compound (Squat) uses 5.0 kg increment
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 100.0, reps = 10, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = squat,
            previousSets = sets,
            currentWeightKg = 100.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.IncreaseWeight)
        assertEquals(5.0, advice.deltaKg)
        assertEquals(105.0, advice.suggestedWeightKg)
    }

    @Test
    fun testAggressiveJumpWhenRpeIsLow() {
        // Low RPE (<= 7.0 / RIR 3+) allows an accelerated jump
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 80.0, reps = 10, rpe = 6.5, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 80.0, reps = 10, rpe = 7.0, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = sets,
            currentWeightKg = 80.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.IncreaseWeight)
        assertEquals(3.75, advice.deltaKg) // 2.5 * 1.5 = 3.75
        assertEquals(83.75, advice.suggestedWeightKg)
    }

    @Test
    fun testIncreaseRepsWhenInsideRange() {
        // 80kg for 8, 8, 8 (target range 6..10) -> aim for +1 rep (target 9)
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 80.0, reps = 8, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 80.0, reps = 8, isCompleted = true),
            SetLog(id = "3", setNumber = 3, weightKg = 80.0, reps = 8, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = sets,
            currentWeightKg = 80.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.IncreaseReps)
        assertEquals(80.0, advice.suggestedWeightKg)
        assertEquals(9, advice.targetReps)
    }

    @Test
    fun testConsolidateWhenRpeIsAtLimit() {
        // Sets inside range but RPE is 9.5+ -> consolidate form
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 80.0, reps = 7, rpe = 9.5, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 80.0, reps = 7, rpe = 10.0, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = sets,
            currentWeightKg = 80.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.MaintainOrConsolidate)
        assertEquals(80.0, advice.suggestedWeightKg)
    }

    @Test
    fun testDeloadWhenExcessiveFatigueAndFailure() {
        // Failed sets (< 6 reps) with RPE >= 9.8 -> recommend Deload (-10%)
        val sets = listOf(
            SetLog(id = "1", setNumber = 1, weightKg = 100.0, reps = 4, rpe = 10.0, isCompleted = true),
            SetLog(id = "2", setNumber = 2, weightKg = 100.0, reps = 3, rpe = 10.0, isCompleted = true)
        )
        val advice = ProgressionAdvisor.advise(
            exercise = benchPress,
            previousSets = sets,
            currentWeightKg = 100.0,
            trainingGoal = TrainingGoal.HYPERTROPHY
        )

        assertTrue(advice is ProgressionAdvice.Deload)
        assertEquals(90.0, advice.suggestedWeightKg) // 100 * 0.90 = 90.0
    }

    @Test
    fun testApplyProgressionToExerciseInWorkoutRepository() {
        val prRepo = com.bearbones.kumalift.data.repository.PersonalRecordRepository()
        val repo = WorkoutRepository(prRepository = prRepo)
        repo.startEmptyWorkout("Overload Test")
        repo.addExerciseToActiveWorkout(benchPress)

        val active = repo.activeWorkout.value
        assertNotNull(active)
        val we = active.exercises.first()

        repo.applyProgressionToExercise(we.id, newWeightKg = 82.5, targetReps = 8)

        val updated = repo.activeWorkout.value
        assertNotNull(updated)
        val updatedWe = updated.exercises.first()

        val workingSets = updatedWe.sets.filter { it.setType != SetType.WARMUP }
        assertTrue(workingSets.isNotEmpty())
        for (set in workingSets) {
            assertEquals(82.5, set.weightKg)
            assertEquals(82.5, set.targetWeightKg)
            assertEquals(8, set.targetReps)
        }
    }
}
