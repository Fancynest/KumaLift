package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MesocyclePhase
import com.bearbones.kumalift.domain.model.MesocycleStatus
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.RoutineSetConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MesocycleRepositoryTests {

    private val benchPress = Exercise(
        id = "ex_bench",
        name = "Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        movementType = MovementType.COMPOUND,
        equipment = Equipment.BARBELL
    )

    private val testRoutine = RoutineBlueprint(
        id = "rt_chest_day",
        title = "Chest Hypertrophy",
        splitCategory = "Push",
        muscleFocus = "Chest",
        exerciseConfigs = listOf(
            RoutineExerciseConfig(
                exercise = benchPress,
                targetSets = 4,
                targetReps = 10,
                targetWeightKg = 80.0,
                setConfigs = listOf(
                    RoutineSetConfig(1, targetWeightKg = 80.0, targetReps = 10),
                    RoutineSetConfig(2, targetWeightKg = 80.0, targetReps = 10),
                    RoutineSetConfig(3, targetWeightKg = 80.0, targetReps = 10),
                    RoutineSetConfig(4, targetWeightKg = 80.0, targetReps = 10)
                )
            )
        )
    )

    @Test
    fun testStartNewMesocycleInitialState() {
        val repo = MesocycleRepository()
        val block = repo.startNewMesocycle(
            name = "Hypertrophy Block 1",
            totalWeeks = 5,
            deloadWeek = 5
        )

        assertEquals("Hypertrophy Block 1", block.name)
        assertEquals(5, block.totalWeeks)
        assertEquals(1, block.currentWeek)
        assertEquals(5, block.deloadWeek)
        assertEquals(MesocycleStatus.ACTIVE, block.status)
        assertEquals(MesocyclePhase.INTRODUCTORY, block.phase)
        assertFalse(block.isDeloadActive)
        assertEquals(block, repo.activeMesocycle.value)
    }

    @Test
    fun testAdvanceWeekProgressionThroughPhases() {
        val repo = MesocycleRepository()
        repo.startNewMesocycle(name = "Block", totalWeeks = 5, deloadWeek = 5)

        // Week 1: Introductory
        assertEquals(MesocyclePhase.INTRODUCTORY, repo.activeMesocycle.value?.phase)

        // Advance to Week 2: Accumulation
        val w2 = repo.advanceWeek()
        assertNotNull(w2)
        assertEquals(2, w2.currentWeek)
        assertEquals(MesocyclePhase.ACCUMULATION, w2.phase)

        // Advance to Week 3: Accumulation
        val w3 = repo.advanceWeek()
        assertNotNull(w3)
        assertEquals(3, w3.currentWeek)
        assertEquals(MesocyclePhase.ACCUMULATION, w3.phase)

        // Advance to Week 4: Peak Overload (week totalWeeks - 1)
        val w4 = repo.advanceWeek()
        assertNotNull(w4)
        assertEquals(4, w4.currentWeek)
        assertEquals(MesocyclePhase.PEAK_OVERLOAD, w4.phase)

        // Advance to Week 5: Deload Week
        val w5 = repo.advanceWeek()
        assertNotNull(w5)
        assertEquals(5, w5.currentWeek)
        assertEquals(MesocyclePhase.DELOAD, w5.phase)
        assertTrue(repo.isDeloadActive())

        // Advance beyond Week 5: Block Completed
        val finished = repo.advanceWeek()
        assertNotNull(finished)
        assertEquals(MesocycleStatus.COMPLETED, finished.status)
        assertNull(repo.activeMesocycle.value)
    }

    @Test
    fun testToggleDeload() {
        val repo = MesocycleRepository()
        repo.startNewMesocycle(name = "Block", totalWeeks = 5, deloadWeek = 5)

        assertFalse(repo.isDeloadActive())

        // Manual early deload activation
        repo.toggleDeload(true)
        assertTrue(repo.isDeloadActive())
        assertEquals(MesocyclePhase.DELOAD, repo.activeMesocycle.value?.phase)

        // Deactivate deload
        repo.toggleDeload(false)
        assertFalse(repo.isDeloadActive())
    }

    @Test
    fun testDeloadReducesWorkoutVolumeInWorkoutRepository() {
        val mesoRepo = MesocycleRepository()
        val prRepo = PersonalRecordRepository()
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            mesocycleRepository = mesoRepo
        )

        // 1. Normal workout (4 sets)
        mesoRepo.startNewMesocycle("Block", totalWeeks = 5, deloadWeek = 5)
        workoutRepo.startWorkoutFromRoutine(testRoutine)
        val normalWorkout = workoutRepo.activeWorkout.value
        assertNotNull(normalWorkout)
        assertEquals(4, normalWorkout.exercises.first().sets.size)
        assertEquals("Chest Hypertrophy", normalWorkout.title)

        workoutRepo.cancelWorkout()

        // 2. Deload workout (volume reduced by ~40%, 4 * 0.6 = 2 sets)
        mesoRepo.toggleDeload(true)
        workoutRepo.startWorkoutFromRoutine(testRoutine)
        val deloadWorkout = workoutRepo.activeWorkout.value
        assertNotNull(deloadWorkout)
        assertEquals(2, deloadWorkout.exercises.first().sets.size)
        assertTrue(deloadWorkout.title.contains("[Deload]"))
    }
}
