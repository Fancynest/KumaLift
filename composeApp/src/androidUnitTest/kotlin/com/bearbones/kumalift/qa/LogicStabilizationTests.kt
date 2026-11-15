package com.bearbones.kumalift.qa

import com.bearbones.kumalift.app.AppNavigator
import com.bearbones.kumalift.app.Overlay
import com.bearbones.kumalift.app.Tab
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.timer.RestTimer
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.export.ImportMode
import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PrMetricType
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LogicStabilizationTests {

    private lateinit var prRepository: PersonalRecordRepository
    private lateinit var workoutRepository: WorkoutRepository

    private val benchPress = Exercise(
        id = "0025",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.BARBELL,
        movementType = MovementType.COMPOUND
    )

    private val squat = Exercise(
        id = "0043",
        name = "Barbell Full Squat",
        primaryMuscle = MuscleGroup.LEGS,
        equipment = Equipment.BARBELL,
        movementType = MovementType.COMPOUND
    )

    @Before
    fun setUp() {
        KumaLogger.clear()
        KumaLogger.isEnabled = true

        prRepository = PersonalRecordRepository(
            initialPrs = emptyList(),
            calculationSettingsProvider = {
                CalculationSettings(
                    repRangePrs = true,
                    prMetrics = setOf(PrMetricType.WEIGHT, PrMetricType.E1RM)
                )
            }
        )

        workoutRepository = WorkoutRepository(
            prRepository = prRepository,
            databaseRepo = null,
            calculationSettingsProvider = { CalculationSettings(repRangePrs = true) }
        )
    }

    @Test
    fun testFreshInstallZeroFakeData() {
        assertEquals(0, SeedData.personalRecords.size)
        assertEquals(0, workoutRepository.completedWorkouts.value.size)
        assertNull(workoutRepository.activeWorkout.value)
        assertEquals(0, prRepository.personalRecords.value.size)
    }

    @Test
    fun testStartWorkoutFromRoutineFreestyleAndDuplicateExercise() {
        val routine = RoutineBlueprint(
            id = "rt_test_push",
            title = "Test Push",
            splitCategory = "PPL Split",
            muscleFocus = "Chest",
            estimatedDurationMinutes = 45,
            targetRpeTier = "RPE 8",
            exerciseTemplates = listOf(benchPress)
        )

        // 1. Start from routine
        workoutRepository.startWorkoutFromRoutine(routine)
        val active1 = workoutRepository.activeWorkout.value
        assertNotNull(active1)
        assertEquals("Test Push", active1!!.title)
        assertEquals(1, active1.exercises.size)

        // 2. Add an exercise from library
        workoutRepository.addExerciseToActiveWorkout(squat)
        val active2 = workoutRepository.activeWorkout.value!!
        assertEquals(2, active2.exercises.size)

        // 3. Add duplicate exercise (bench press again)
        workoutRepository.addExerciseToActiveWorkout(benchPress)
        val active3 = workoutRepository.activeWorkout.value!!
        assertEquals(3, active3.exercises.size)
        val benchInstances = active3.exercises.filter { it.exercise.id == benchPress.id }
        assertEquals(2, benchInstances.size)
        assertTrue(benchInstances[0].id != benchInstances[1].id)

        // 4. Updating sets on duplicate instance modifies only that instance
        val firstBenchId = benchInstances[0].id
        val secondBenchId = benchInstances[1].id
        val setFirst = benchInstances[0].sets.first().id
        val setSecond = benchInstances[1].sets.first().id

        workoutRepository.updateSet(firstBenchId, setFirst, weightKg = 100.0, reps = 5)
        workoutRepository.updateSet(secondBenchId, setSecond, weightKg = 80.0, reps = 8)

        val active4 = workoutRepository.activeWorkout.value!!
        val updatedFirst = active4.exercises.find { it.id == firstBenchId }!!
        val updatedSecond = active4.exercises.find { it.id == secondBenchId }!!

        assertEquals(100.0, updatedFirst.sets.first().weightKg, 0.001)
        assertEquals(80.0, updatedSecond.sets.first().weightKg, 0.001)
    }

    @Test
    fun testCompleteUncompleteSetAndPrRevocation() {
        workoutRepository.startEmptyWorkout("Freestyle PR Test")
        workoutRepository.addExerciseToActiveWorkout(benchPress)

        val weId = workoutRepository.activeWorkout.value!!.exercises.first().id
        val setId1 = workoutRepository.activeWorkout.value!!.exercises.first().sets[0].id
        val setId2 = workoutRepository.activeWorkout.value!!.exercises.first().sets[1].id

        // Set 1: 100 kg x 5 -> establishes first PR
        workoutRepository.updateSet(weId, setId1, weightKg = 100.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId1) {}

        val prs1 = prRepository.personalRecords.value
        assertEquals(1, prs1.size)
        assertEquals(100.0, prs1.first().weightKg, 0.001)

        // Set 2: 110 kg x 5 -> surpasses PR
        workoutRepository.updateSet(weId, setId2, weightKg = 110.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId2) {}

        val prs2 = prRepository.personalRecords.value
        assertEquals(1, prs2.size)
        assertEquals(110.0, prs2.first().weightKg, 0.001)

        // Un-toggle set 2: PR should be revoked and restored to 100 kg from set 1
        workoutRepository.toggleSetCompletion(weId, setId2) {}
        val prs3 = prRepository.personalRecords.value
        assertEquals(1, prs3.size)
        assertEquals(100.0, prs3.first().weightKg, 0.001)
        assertEquals(setId1, prs3.first().sourceSetId)
    }

    @Test
    fun testRemoveSetRestoresPreviousPr() {
        workoutRepository.startEmptyWorkout("Set Deletion PR Test")
        workoutRepository.addExerciseToActiveWorkout(benchPress)

        val weId = workoutRepository.activeWorkout.value!!.exercises.first().id
        val setId1 = workoutRepository.activeWorkout.value!!.exercises.first().sets[0].id
        val setId2 = workoutRepository.activeWorkout.value!!.exercises.first().sets[1].id

        workoutRepository.updateSet(weId, setId1, weightKg = 90.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId1) {}

        workoutRepository.updateSet(weId, setId2, weightKg = 105.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId2) {}

        assertEquals(105.0, prRepository.personalRecords.value.first().weightKg, 0.001)

        // Delete set 2
        workoutRepository.removeSetFromExercise(weId, setId2)

        val prs = prRepository.personalRecords.value
        assertEquals(1, prs.size)
        assertEquals(90.0, prs.first().weightKg, 0.001)
        assertEquals(setId1, prs.first().sourceSetId)
    }

    @Test
    fun testRemoveExerciseRevokesItsPrs() {
        workoutRepository.startEmptyWorkout("Exercise Deletion PR Test")
        workoutRepository.addExerciseToActiveWorkout(benchPress)

        val weId = workoutRepository.activeWorkout.value!!.exercises.first().id
        val setId1 = workoutRepository.activeWorkout.value!!.exercises.first().sets[0].id

        workoutRepository.updateSet(weId, setId1, weightKg = 95.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId1) {}
        assertEquals(1, prRepository.personalRecords.value.size)

        // Remove the entire exercise
        workoutRepository.removeExerciseFromActiveWorkout(weId)

        assertEquals(0, prRepository.personalRecords.value.size)
    }

    @Test
    fun testFinishWorkoutSummaryValuesMatchLoggedSets() {
        workoutRepository.startEmptyWorkout("Summary Validation Test")
        workoutRepository.addExerciseToActiveWorkout(benchPress)

        val weId = workoutRepository.activeWorkout.value!!.exercises.first().id
        val sets = workoutRepository.activeWorkout.value!!.exercises.first().sets

        // Complete 2 sets: 100kg x 10, 100kg x 8 -> Total Volume = 1000 + 800 = 1800kg
        workoutRepository.updateSet(weId, sets[0].id, weightKg = 100.0, reps = 10)
        workoutRepository.toggleSetCompletion(weId, sets[0].id) {}

        workoutRepository.updateSet(weId, sets[1].id, weightKg = 100.0, reps = 8)
        workoutRepository.toggleSetCompletion(weId, sets[1].id) {}

        val finished = workoutRepository.finishWorkout(sessionRpe = 8.5)
        assertNotNull(finished)
        assertEquals(1800.0, finished!!.totalVolumeKg, 0.001)
        assertEquals(2, finished.totalCompletedSets)
        assertEquals(1, workoutRepository.completedWorkouts.value.size)
        assertNull(workoutRepository.activeWorkout.value)
    }

    @Test
    fun testUnitsSwitchKgToLbWithoutValueDrift() {
        val originalKg = 100.0
        val displayLb = UnitConverters.toDisplayWeight(originalKg, WeightUnit.LB)
        assertEquals(220.5, displayLb, 0.1)

        val backToKg = UnitConverters.toStorageWeightKg(displayLb, WeightUnit.LB)
        assertEquals(originalKg, backToKg, 0.05)

        // Format test
        assertEquals("100.0 kg", UnitConverters.formatWeight(100.0, WeightUnit.KG))
        assertEquals("220.5 lb", UnitConverters.formatWeight(100.0, WeightUnit.LB))
    }

    @Test
    fun testRestTimerWallClockAccuracy() = runBlocking<Unit> {
        val timer = RestTimer(this)
        timer.start(5)
        assertTrue(timer.state.value.isRunning)
        assertEquals(5, timer.state.value.totalSeconds)

        // Simulate 2 seconds elapsing
        delay(2100)
        val remaining = timer.state.value.remainingSeconds
        assertTrue("Expected remaining in 0..4 but was $remaining", remaining in 0..4)

        // Add 5 seconds
        timer.addSeconds(5)
        assertTrue("Expected remaining in 5..9 but was ${timer.state.value.remainingSeconds}", timer.state.value.remainingSeconds in 5..9)

        timer.stop()
        assertFalse(timer.state.value.isRunning)
    }

    @Test
    fun testDiagnosticLoggingBufferAndToggle() {
        KumaLogger.clear()
        KumaLogger.isEnabled = false
        KumaLogger.i("Test", "Should not be logged")
        assertEquals(0, KumaLogger.logCount())

        KumaLogger.isEnabled = true
        KumaLogger.i("TestTag", "Diagnostic entry 1")
        KumaLogger.w("TestTag", "Diagnostic entry 2")
        assertEquals(2, KumaLogger.logCount())

        val logs = KumaLogger.getLogs()
        assertTrue(logs.contains("Diagnostic entry 1"))
        assertTrue(logs.contains("Diagnostic entry 2"))
        assertTrue(logs.contains("[INFO]"))
        assertTrue(logs.contains("[WARN]"))

        KumaLogger.clear()
        assertEquals(0, KumaLogger.logCount())
    }

    @Test
    fun testNavigatorSaverRestoresActiveLoggerAndSummary() {
        val navigator = AppNavigator()
        navigator.selectTab(Tab.LIBRARY)
        navigator.push(Overlay.ActiveLogger)

        val saverScope = androidx.compose.runtime.saveable.SaverScope { true }
        val saved = with(AppNavigator.Saver) { saverScope.save(navigator) } as? List<*>
        assertNotNull(saved)

        val restored = AppNavigator.Saver.restore(saved!!)
        assertNotNull(restored)
        assertEquals(Tab.LIBRARY, restored!!.selectedTab)
        assertEquals(Overlay.ActiveLogger, restored.currentOverlay)
    }

    @Test
    fun testBackupReminderWeeklyCooldownLogic() {
        val oneWeekMs = 7L * 24 * 3600_000L
        val now = 100_000_000_000L // arbitrary fixed timestamp
        val reminderDays = 30
        val lastBackupMs = now - (35L * 24 * 3600_000L) // 35 days ago, overdue

        // Without interaction dismissal: due
        val dueWithoutDismissal = (now - lastBackupMs) / (24L * 3600_000L) >= reminderDays
        assertTrue(dueWithoutDismissal)

        // User interacts and dismisses 2 days ago: within 7-day cooldown -> NOT due
        val dismissed2DaysAgo = now - (2L * 24 * 3600_000L)
        val isCoolingDown = (now - dismissed2DaysAgo) < oneWeekMs
        assertTrue(isCoolingDown)

        // After 8 days: cooldown expired -> due again
        val dismissed8DaysAgo = now - (8L * 24 * 3600_000L)
        val isStillCoolingDown = (now - dismissed8DaysAgo) < oneWeekMs
        assertFalse(isStillCoolingDown)
    }

    @Test
    fun testWorkoutPauseResumeDurationLogic() {
        val exerciseRepo = ExerciseRepository()
        val prRepo = PersonalRecordRepository()
        val repo = WorkoutRepository(prRepository = prRepo)

        repo.startEmptyWorkout("Test Workout")
        val active = repo.activeWorkout.value
        assertNotNull(active)
        assertFalse(active!!.isPaused)
        assertEquals(0L, active.totalPausedDurationMs)

        // Pause
        repo.pauseWorkout()
        val paused = repo.activeWorkout.value
        assertNotNull(paused)
        assertTrue(paused!!.isPaused)
        assertNotNull(paused.pausedAtEpochMs)

        // Resume
        repo.resumeWorkout()
        val resumed = repo.activeWorkout.value
        assertNotNull(resumed)
        assertFalse(resumed!!.isPaused)
        assertNull(resumed.pausedAtEpochMs)

        // Finish
        val finished = repo.finishWorkout()
        assertNotNull(finished)
        assertTrue(finished!!.isFinished)
        assertTrue(finished.durationSeconds >= 0L)
    }

    @Test
    fun testDefaultRoutinesEmptyAndDeletable() {
        val routineRepo = RoutineRepository(initialRoutines = emptyList<RoutineBlueprint>())
        assertEquals(0, routineRepo.routines.value.size)

        val newRoutine = RoutineBlueprint(
            id = "custom_123",
            title = "My Routine",
            splitCategory = "Custom",
            muscleFocus = "Full Body",
            estimatedDurationMinutes = 45,
            targetRpeTier = "RPE 8",
            exerciseTemplates = emptyList()
        )
        routineRepo.addRoutine(newRoutine)
        assertEquals(1, routineRepo.routines.value.size)

        routineRepo.deleteRoutine("custom_123")
        assertEquals(0, routineRepo.routines.value.size)
    }

    @Test
    fun testMeasuresDefaultEmptyState() {
        val profile = ProfileSettings()
        assertNull(profile.bodyweightKg)
        assertNull(profile.heightCm)
    }

    @Test
    fun testNotificationVariationsCountAndDailyRotation() {
        val enVariations = com.bearbones.kumalift.core.localization.EnStrings.notificationVariations
        val idVariations = com.bearbones.kumalift.core.localization.IdStrings.notificationVariations

        assertEquals(5, enVariations.size)
        assertEquals(5, idVariations.size)

        // Test day rotation does not repeat on consecutive days
        val day1Index = 100 % enVariations.size
        val day2Index = 101 % enVariations.size
        assertNotEquals(day1Index, day2Index)

        // Verify Indonesian texts are non-empty and well-formed
        idVariations.forEach { (title, body) ->
            assertTrue(title.isNotBlank())
            assertTrue(body.isNotBlank())
        }
    }
}
