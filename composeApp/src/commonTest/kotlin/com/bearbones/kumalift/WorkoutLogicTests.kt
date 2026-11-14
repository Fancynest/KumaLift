package com.bearbones.kumalift

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
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
}
