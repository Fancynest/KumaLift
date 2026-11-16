package com.bearbones.kumalift.data

import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.domain.model.DEFAULT_PROGRAM_ID
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TrainingProgramTests {

    @Test
    fun defaultTrainingProgramIsHypertrophy() {
        val repo = TrainingProgramRepository()
        assertEquals(1, repo.programs.value.size)
        val defaultProg = repo.programs.value.first()
        assertEquals(DEFAULT_PROGRAM_ID, defaultProg.id)
        assertEquals("Hypertrophy Program", defaultProg.name)
        assertEquals(TrainingGoal.HYPERTROPHY, defaultProg.category)
        assertEquals(3, defaultProg.targetDaysPerWeek)
        assertEquals(listOf(1, 3, 5), defaultProg.activeDays)
        assertTrue(defaultProg.isDefault)
        assertEquals(DEFAULT_PROGRAM_ID, repo.activeProgramId.value)
    }

    @Test
    fun createMultipleProgramsMaintainsDistinctEntities() {
        val repo = TrainingProgramRepository()

        val fatLoss = repo.createProgram(
            name = "Fat Loss Program",
            description = "High intensity and caloric burn",
            category = TrainingGoal.FAT_LOSS,
            activeDays = listOf(2, 4)
        )

        val strength = repo.createProgram(
            name = "Strength Program",
            description = "Focus on 1RM and compound lifts",
            category = TrainingGoal.STRENGTH,
            activeDays = listOf(1, 3, 5)
        )

        assertEquals(3, repo.programs.value.size)
        assertTrue(repo.programs.value.any { it.name == "Hypertrophy Program" })
        assertTrue(repo.programs.value.any { it.name == "Fat Loss Program" })
        assertTrue(repo.programs.value.any { it.name == "Strength Program" })

        assertEquals(2, fatLoss.targetDaysPerWeek)
        assertEquals(listOf(2, 4), fatLoss.activeDays)
        assertEquals(3, strength.targetDaysPerWeek)
        assertEquals(listOf(1, 3, 5), strength.activeDays)
    }

    @Test
    fun programActiveDaysClampedToMaximumThreeDays() {
        val repo = TrainingProgramRepository()

        // Attempt to pass 5 active days
        val programWithExcessDays = repo.createProgram(
            name = "Overloaded Program",
            category = TrainingGoal.GENERAL,
            activeDays = listOf(1, 2, 3, 4, 5)
        )

        // Must strictly clamp to max 3 days
        assertEquals(3, programWithExcessDays.activeDays.size)
        assertEquals(3, programWithExcessDays.targetDaysPerWeek)
        assertEquals(listOf(1, 2, 3), programWithExcessDays.activeDays)

        // Update program with 4 days
        repo.updateProgram(programWithExcessDays.copy(activeDays = listOf(2, 4, 6, 7)))
        val updated = repo.programs.value.find { it.id == programWithExcessDays.id }!!
        assertEquals(3, updated.activeDays.size)
        assertEquals(3, updated.targetDaysPerWeek)
        assertEquals(listOf(2, 4, 6), updated.activeDays)
    }

    @Test
    fun independentWorkoutRoutinesAreIsolatedPerProgram() {
        val programRepo = TrainingProgramRepository()
        val routineRepo = RoutineRepository()

        val hypertrophy = programRepo.activeProgram
        val strength = programRepo.createProgram(
            name = "Strength Program",
            category = TrainingGoal.STRENGTH,
            targetDaysPerWeek = 3
        )

        // Add 2 routines to Hypertrophy
        val pushDay = RoutineBlueprint(
            id = "r_push",
            programId = hypertrophy.id,
            title = "Push Day",
            splitCategory = "PPL Split",
            muscleFocus = "Chest, Shoulders, Triceps"
        )
        val pullDay = RoutineBlueprint(
            id = "r_pull",
            programId = hypertrophy.id,
            title = "Pull Day",
            splitCategory = "PPL Split",
            muscleFocus = "Back, Biceps"
        )
        routineRepo.addRoutine(pushDay)
        routineRepo.addRoutine(pullDay)

        // Add 1 routine to Strength
        val benchDay = RoutineBlueprint(
            id = "r_bench",
            programId = strength.id,
            title = "Heavy Bench Day",
            splitCategory = "Strength Split",
            muscleFocus = "Chest"
        )
        routineRepo.addRoutine(benchDay)

        // Verify Hypertrophy routines
        val hypRoutines = routineRepo.getRoutinesForProgram(hypertrophy.id)
        assertEquals(2, hypRoutines.size)
        assertTrue(hypRoutines.any { it.title == "Push Day" })
        assertTrue(hypRoutines.any { it.title == "Pull Day" })
        assertFalse(hypRoutines.any { it.title == "Heavy Bench Day" })

        // Verify Strength routines
        val strRoutines = routineRepo.getRoutinesForProgram(strength.id)
        assertEquals(1, strRoutines.size)
        assertEquals("Heavy Bench Day", strRoutines.first().title)
    }

    @Test
    fun switchingActiveProgramUpdatesActiveState() {
        val repo = TrainingProgramRepository()
        var updatedId: String? = null
        val repoWithListener = TrainingProgramRepository(
            onActiveProgramChanged = { updatedId = it }
        )

        val newProg = repoWithListener.createProgram(
            name = "Endurance Program",
            category = TrainingGoal.ENDURANCE,
            targetDaysPerWeek = 4
        )

        repoWithListener.setActiveProgramId(newProg.id)
        assertEquals(newProg.id, repoWithListener.activeProgramId.value)
        assertEquals(newProg.id, repoWithListener.activeProgram.id)
        assertEquals(newProg.id, updatedId)
    }

    @Test
    fun deletingProgramDeletesAssociatedRoutines() {
        val programRepo = TrainingProgramRepository()
        val routineRepo = RoutineRepository()

        val fatLoss = programRepo.createProgram(
            name = "Fat Loss Program",
            category = TrainingGoal.FAT_LOSS,
            targetDaysPerWeek = 5
        )

        routineRepo.addRoutine(
            RoutineBlueprint(
                id = "r_fl1",
                programId = fatLoss.id,
                title = "Cardio Circuit",
                splitCategory = "Full Body",
                muscleFocus = "Full Body"
            )
        )

        assertEquals(1, routineRepo.getRoutinesForProgram(fatLoss.id).size)

        // Delete program
        val success = programRepo.deleteProgram(fatLoss.id, routineRepo)
        assertTrue(success)

        // Program removed
        assertEquals(1, programRepo.programs.value.size)
        assertFalse(programRepo.programs.value.any { it.id == fatLoss.id })

        // Associated routines deleted
        assertEquals(0, routineRepo.getRoutinesForProgram(fatLoss.id).size)
    }

    @Test
    fun cannotDeleteOnlyRemainingProgram() {
        val repo = TrainingProgramRepository()
        assertEquals(1, repo.programs.value.size)

        val success = repo.deleteProgram(repo.programs.value.first().id)
        assertFalse(success)
        assertEquals(1, repo.programs.value.size)
    }

    @Test
    fun deletingActiveProgramSwitchesToRemainingProgram() {
        val repo = TrainingProgramRepository()
        val p2 = repo.createProgram(name = "Program 2")
        repo.setActiveProgramId(p2.id)
        assertEquals(p2.id, repo.activeProgramId.value)

        val success = repo.deleteProgram(p2.id)
        assertTrue(success)
        assertEquals(DEFAULT_PROGRAM_ID, repo.activeProgramId.value)
    }

    @Test
    fun routineBlueprintDefaultProgramIdIsHypertrophyDefault() {
        val legacyRoutine = RoutineBlueprint(
            id = "r_legacy",
            title = "Legacy Push",
            splitCategory = "PPL",
            muscleFocus = "Chest"
        )
        assertEquals(DEFAULT_PROGRAM_ID, legacyRoutine.programId)
    }

    @Test
    fun programsCanShareSameActiveDaySchedule() {
        val repo = TrainingProgramRepository()
        // Default Hypertrophy is Mon (1), Wed (3), Fri (5)

        val fatLoss = repo.createProgram(
            name = "Fat Loss Program",
            category = TrainingGoal.FAT_LOSS,
            activeDays = listOf(1, 2, 4) // Mon, Tue, Thu
        )

        val strength = repo.createProgram(
            name = "Strength Program",
            category = TrainingGoal.STRENGTH,
            activeDays = listOf(1, 3, 6) // Mon, Wed, Sat
        )

        val allPrograms = repo.programs.value

        // Monday (1) should match Hypertrophy, Fat Loss, and Strength (3 programs)
        val mondayPrograms = allPrograms.filter { it.activeDays.contains(1) }
        assertEquals(3, mondayPrograms.size)
        assertTrue(mondayPrograms.any { it.name == "Hypertrophy Program" })
        assertTrue(mondayPrograms.any { it.name == "Fat Loss Program" })
        assertTrue(mondayPrograms.any { it.name == "Strength Program" })

        // Tuesday (2) should only match Fat Loss (1 program)
        val tuesdayPrograms = allPrograms.filter { it.activeDays.contains(2) }
        assertEquals(1, tuesdayPrograms.size)
        assertEquals("Fat Loss Program", tuesdayPrograms.first().name)

        // Wednesday (3) should match Hypertrophy and Strength (2 programs)
        val wednesdayPrograms = allPrograms.filter { it.activeDays.contains(3) }
        assertEquals(2, wednesdayPrograms.size)
        assertTrue(wednesdayPrograms.any { it.name == "Hypertrophy Program" })
        assertTrue(wednesdayPrograms.any { it.name == "Strength Program" })

        // Sunday (7) has no programs scheduled (Rest Day / Empty State)
        val sundayPrograms = allPrograms.filter { it.activeDays.contains(7) }
        assertTrue(sundayPrograms.isEmpty())
    }
}
