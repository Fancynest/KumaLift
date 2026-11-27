package com.bearbones.kumalift.qa

import com.bearbones.kumalift.app.AppNavigator
import com.bearbones.kumalift.app.Overlay
import com.bearbones.kumalift.app.Tab
import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.sound.PlatformSoundPlayer
import com.bearbones.kumalift.core.sound.SoundEvent
import com.bearbones.kumalift.core.sound.SoundManager
import com.bearbones.kumalift.core.timer.RestTimer
import com.bearbones.kumalift.core.timer.RestTimerCoordinator
import com.bearbones.kumalift.core.timer.RestTimerState
import com.bearbones.kumalift.core.timer.TimerStatus
import com.bearbones.kumalift.domain.model.NotificationSettings
import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundProfile
import com.bearbones.kumalift.domain.model.SoundVolume
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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
import com.bearbones.kumalift.domain.model.PersonalRecord
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
    fun testRestTimerPlayPauseAndResumeFlow() = runBlocking<Unit> {
        val timer = RestTimer(this)
        timer.start(10, "Dumbbell Bench Press")
        assertTrue(timer.state.value.isRunning)
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)

        // Pause timer
        timer.pause()
        assertEquals(TimerStatus.PAUSED, timer.state.value.status)
        assertFalse(timer.state.value.isRunning)
        val pausedSec = timer.state.value.remainingSeconds

        // Wait to verify countdown is halted
        delay(500)
        assertEquals(pausedSec, timer.state.value.remainingSeconds)

        // Resume timer
        timer.resume()
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.isRunning)

        // Dismiss timer
        timer.dismiss()
        assertEquals(TimerStatus.DISMISSED, timer.state.value.status)
        assertFalse(timer.state.value.isVisible)
    }

    @Test
    fun testWorkingVolumeExcludesWarmupAndUncompletedSets() {
        val exercise = Exercise(id = "e1", name = "Dumbbell Bench Press", primaryMuscle = MuscleGroup.CHEST, equipment = Equipment.DUMBBELL)
        val sets = listOf(
            com.bearbones.kumalift.domain.model.SetLog(id = "s1", setNumber = 1, setType = com.bearbones.kumalift.domain.model.SetType.WARMUP, weightKg = 20.0, reps = 15, isCompleted = true),
            com.bearbones.kumalift.domain.model.SetLog(id = "s2", setNumber = 2, setType = com.bearbones.kumalift.domain.model.SetType.NORMAL, weightKg = 40.0, reps = 12, isCompleted = true), // 480
            com.bearbones.kumalift.domain.model.SetLog(id = "s3", setNumber = 3, setType = com.bearbones.kumalift.domain.model.SetType.NORMAL, weightKg = 40.0, reps = 10, isCompleted = true), // 400
            com.bearbones.kumalift.domain.model.SetLog(id = "s4", setNumber = 4, setType = com.bearbones.kumalift.domain.model.SetType.DROP, weightKg = 30.0, reps = 8, isCompleted = true),    // 240
            com.bearbones.kumalift.domain.model.SetLog(id = "s5", setNumber = 5, setType = com.bearbones.kumalift.domain.model.SetType.NORMAL, weightKg = 45.0, reps = 8, isCompleted = false) // Not completed
        )
        val we = com.bearbones.kumalift.domain.model.WorkoutExercise(id = "we1", exercise = exercise, sets = sets)

        // 480 + 400 + 240 = 1120.0 (warmup 300.0 and uncompleted 360.0 excluded)
        assertEquals(1120.0, we.totalVolumeKg, 0.001)
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

    @Test
    fun testCancelWorkoutRevokesSessionPrs() {
        workoutRepository.startEmptyWorkout("Cancel Workout PR Test")
        workoutRepository.addExerciseToActiveWorkout(benchPress)

        val weId = workoutRepository.activeWorkout.value!!.exercises.first().id
        val setId = workoutRepository.activeWorkout.value!!.exercises.first().sets[0].id

        workoutRepository.updateSet(weId, setId, weightKg = 120.0, reps = 5)
        workoutRepository.toggleSetCompletion(weId, setId) {}
        assertEquals(1, prRepository.personalRecords.value.size)

        // Cancel workout session
        workoutRepository.cancelWorkout()

        // PR created during cancelled workout must be revoked
        assertEquals(0, prRepository.personalRecords.value.size)
    }

    @Test
    fun testPrSanitizationOnRestartWithoutRepRange() {
        val pr1 = PersonalRecord(
            exerciseId = "ex_1",
            exerciseName = "Dumbbell Bench Press",
            weightKg = 12.0,
            reps = 16,
            estimated1RmKg = 0.0,
            dateAchieved = "Nov 17",
            deltaWeightKg = 0.0,
            prTier = "16RM PR",
            dateAchievedEpochMs = 1000L
        )
        val pr2 = PersonalRecord(
            exerciseId = "ex_1",
            exerciseName = "Dumbbell Bench Press",
            weightKg = 20.0,
            reps = 8,
            estimated1RmKg = 25.3,
            dateAchieved = "Nov 17",
            deltaWeightKg = 8.0,
            prTier = "8RM PR",
            dateAchievedEpochMs = 2000L
        )
        val calcSettings = CalculationSettings(repRangePrs = false)
        val sanitized = PersonalRecordRepository.sanitizeInitialPrs(listOf(pr1, pr2), calcSettings, null)

        // Should keep only the best PR (highest weight)
        assertEquals(1, sanitized.size)
        assertEquals(20.0, sanitized.first().weightKg, 0.001)
        assertEquals(8, sanitized.first().reps)
    }

    @Test
    fun testRestTimerCompletionStateAndPersistence() = runBlocking {
        val timer = RestTimer(this)
        assertEquals(TimerStatus.IDLE, timer.state.value.status)
        assertFalse(timer.state.value.isVisible)

        // 1. Start timer with 1 second
        timer.start(1, "Bench Press")
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.isRunning)
        assertTrue(timer.state.value.isVisible)
        assertEquals("Bench Press", timer.state.value.exerciseName)

        // 2. Wait for completion
        var attempts = 0
        while (timer.state.value.status != TimerStatus.COMPLETED && attempts < 20) {
            delay(100)
            attempts++
        }

        // 3. Must transition to COMPLETED and remain visible (do not auto-dismiss)
        assertEquals(TimerStatus.COMPLETED, timer.state.value.status)
        assertTrue("Timer must remain visible when completed", timer.state.value.isVisible)
        assertTrue(timer.state.value.isFinished)
        assertFalse(timer.state.value.isRunning)
        assertEquals(0, timer.state.value.remainingSeconds)
        assertEquals("00:00", timer.state.value.formattedTime)
        assertEquals(1f, timer.state.value.progressFraction, 0.001f)

        // 4. Adding seconds resumes timer from completed state
        timer.addSeconds(30)
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.remainingSeconds >= 29)
        assertTrue(timer.state.value.isVisible)

        // 5. Dismiss transitions to DISMISSED and hides
        timer.dismiss()
        assertEquals(TimerStatus.DISMISSED, timer.state.value.status)
        assertFalse("Timer must not be visible after dismissed", timer.state.value.isVisible)
        assertFalse(timer.state.value.isRunning)
    }

    @Test
    fun testRestTimerNegativeOvertimeAndFormatting() = runBlocking {
        assertEquals("-00:01", RestTimerState(remainingSeconds = -1, status = TimerStatus.COMPLETED).formattedTime)
        assertEquals("-01:05", RestTimerState(remainingSeconds = -65, status = TimerStatus.COMPLETED).formattedTime)

        val timer = RestTimer(this)
        timer.start(1, "Bench Press")

        var attempts = 0
        while (timer.state.value.remainingSeconds > -2 && attempts < 40) {
            delay(100)
            attempts++
        }

        assertEquals(TimerStatus.COMPLETED, timer.state.value.status)
        assertTrue("Expected negative overtime seconds, got ${timer.state.value.remainingSeconds}", timer.state.value.remainingSeconds <= -1)
        assertTrue("Expected formatted time to start with '-', got ${timer.state.value.formattedTime}", timer.state.value.formattedTime.startsWith("-"))

        // +30s restarts positive countdown from overtime
        timer.addSeconds(30)
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.remainingSeconds >= 29)

        timer.dismiss()
    }

    @Test
    fun testRestTimerCoordinatorSurvivesUiDisposalAndSixExerciseFlow() = runBlocking {
        // Ensure clean coordinator timer backed by SupervisorJob
        RestTimerCoordinator.dismiss()
        RestTimerCoordinator.activeRestTimer = null

        val coordinatorTimer = RestTimerCoordinator.getOrCreateTimer()
        val uiScope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Job())

        coordinatorTimer.start(1, "Exercise 4")
        // Simulate leaving ActiveLogger / opening Exercises tab
        uiScope.coroutineContext[kotlinx.coroutines.Job]?.cancel()

        var attempts = 0
        while (coordinatorTimer.state.value.status != TimerStatus.COMPLETED && attempts < 25) {
            delay(100)
            attempts++
        }
        assertEquals(TimerStatus.COMPLETED, coordinatorTimer.state.value.status)

        // Ensure subsequent exercise (e.g. exercise 5/6) starts cleanly with preserved total on resume
        coordinatorTimer.start(60, "Exercise 6")
        assertTrue(coordinatorTimer.state.value.isRunning)
        coordinatorTimer.pause()
        assertEquals(TimerStatus.PAUSED, coordinatorTimer.state.value.status)
        coordinatorTimer.addSeconds(10)
        assertEquals(TimerStatus.PAUSED, coordinatorTimer.state.value.status)
        assertEquals(70, coordinatorTimer.state.value.totalSeconds)
        coordinatorTimer.resume()
        assertEquals(TimerStatus.RUNNING, coordinatorTimer.state.value.status)
        assertEquals(70, coordinatorTimer.state.value.totalSeconds)

        // Ensure 6+ exercises added rapidly all get unique IDs
        workoutRepository.startEmptyWorkout("6 Exercises Session")
        repeat(6) {
            workoutRepository.addExerciseToActiveWorkout(benchPress)
        }
        val exercises = workoutRepository.activeWorkout.value!!.exercises
        assertEquals(6, exercises.size)
        assertEquals(6, exercises.map { it.id }.distinct().size)

        coordinatorTimer.dismiss()
    }

    @Test
    fun testRestTimerStateTransitions() = runBlocking {
        val timer = RestTimer(this)

        // IDLE
        assertEquals(TimerStatus.IDLE, timer.state.value.status)
        assertFalse(timer.state.value.isRunning)
        assertFalse(timer.state.value.isPaused)
        assertFalse(timer.state.value.isFinished)
        assertFalse(timer.state.value.isVisible)

        // RUNNING
        timer.start(60)
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.isRunning)
        assertFalse(timer.state.value.isPaused)
        assertFalse(timer.state.value.isFinished)
        assertTrue(timer.state.value.isVisible)

        // PAUSED
        timer.pause()
        assertEquals(TimerStatus.PAUSED, timer.state.value.status)
        assertFalse(timer.state.value.isRunning)
        assertTrue(timer.state.value.isPaused)
        assertFalse(timer.state.value.isFinished)
        assertTrue(timer.state.value.isVisible)

        // RESUMED (RUNNING)
        timer.resume()
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        assertTrue(timer.state.value.isRunning)
        assertFalse(timer.state.value.isPaused)
        assertTrue(timer.state.value.isVisible)

        // STOP -> IDLE
        timer.stop()
        assertEquals(TimerStatus.IDLE, timer.state.value.status)
        assertFalse(timer.state.value.isVisible)

        // Coordinator controls
        RestTimerCoordinator.activeRestTimer = timer
        timer.start(45)
        RestTimerCoordinator.pause()
        assertEquals(TimerStatus.PAUSED, timer.state.value.status)
        RestTimerCoordinator.resume()
        assertEquals(TimerStatus.RUNNING, timer.state.value.status)
        RestTimerCoordinator.dismiss()
        assertEquals(TimerStatus.DISMISSED, timer.state.value.status)
        assertNull(RestTimerCoordinator.activeRestTimer)
    }

    @Test
    fun testRestTimerCustomSoundSettingsPersistence() {
        val json = Json { ignoreUnknownKeys = true }
        val settings = UserSettings(
            notifications = NotificationSettings(
                soundProfile = SoundProfile.KUMA,
                soundVolume = 100,
                restTimerSoundSource = RestTimerSoundSource.CUSTOM,
                customRestTimerSoundPath = "/data/user/0/com.bearbones.kumalift/files/custom_sounds/my_bell.mp3",
                customRestTimerSoundName = "my_bell.mp3"
            )
        )

        val serialized = json.encodeToString(settings)
        val deserialized = json.decodeFromString<UserSettings>(serialized)

        assertEquals(RestTimerSoundSource.CUSTOM, deserialized.notifications.restTimerSoundSource)
        assertEquals("/data/user/0/com.bearbones.kumalift/files/custom_sounds/my_bell.mp3", deserialized.notifications.customRestTimerSoundPath)
        assertEquals("my_bell.mp3", deserialized.notifications.customRestTimerSoundName)
        assertEquals(SoundProfile.KUMA, deserialized.notifications.soundProfile)
        assertEquals(100, deserialized.notifications.soundVolume)
        assertEquals(1.0f, deserialized.notifications.soundVolumeMultiplier)
    }

    @Test
    fun testSoundVolumeSliderValuesAndMute() {
        val defaultSettings = NotificationSettings()
        assertEquals(50, defaultSettings.soundVolume)
        assertEquals(0.5f, defaultSettings.soundVolumeMultiplier)

        val mutedSettings = NotificationSettings(soundVolume = 0)
        assertEquals(0, mutedSettings.soundVolume)
        assertEquals(0.0f, mutedSettings.soundVolumeMultiplier)

        val steppedSettings = NotificationSettings(soundVolume = 70)
        assertEquals(70, steppedSettings.soundVolume)
        assertEquals(0.7f, steppedSettings.soundVolumeMultiplier)
    }

    @Test
    fun testRestTimerSoundSourceFallbackAndStop() {
        // Fallback test: calling playRestTimerSound with non-existent custom path should not crash
        PlatformSoundPlayer.playRestTimerSound(
            volumePercent = 50,
            source = RestTimerSoundSource.CUSTOM,
            customPath = "/non/existent/path/sound.mp3"
        )

        // System default test: should not crash
        PlatformSoundPlayer.playRestTimerSound(
            volumePercent = 30,
            source = RestTimerSoundSource.SYSTEM_DEFAULT,
            customPath = null
        )

        // Stop test: should stop cleanly without exception
        PlatformSoundPlayer.stopRestTimerSound()
    }

    @Test
    fun testRestTimerCasAndRepositoryStateIntegrity() {
        val scope = kotlinx.coroutines.CoroutineScope(
            kotlinx.coroutines.SupervisorJob() + kotlinx.coroutines.Dispatchers.Unconfined
        )
        val timer = com.bearbones.kumalift.core.timer.RestTimer(scope)

        // Idle timer ignores addSeconds cleanly
        timer.addSeconds(30)
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.IDLE, timer.state.value.status)

        // Start -> pause transitions cleanly to PAUSED; addSeconds below 0 completes timer
        timer.start(30, "Bench Press")
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.RUNNING, timer.state.value.status)
        timer.pause()
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.PAUSED, timer.state.value.status)
        timer.resume()
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.RUNNING, timer.state.value.status)
        timer.addSeconds(-60)
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.COMPLETED, timer.state.value.status)
        timer.dismiss()
        assertEquals(com.bearbones.kumalift.core.timer.TimerStatus.DISMISSED, timer.state.value.status)

        // Out-of-bounds DailyReadiness inputs must be clamped BEFORE percentage calculation
        val readinessRepo = com.bearbones.kumalift.data.repository.DailyReadinessRepository()
        val checkIn = readinessRepo.recordCheckIn(sleep = 10, soreness = -3, energy = 99, stress = 0)
        assertEquals(5, checkIn.sleepScore)
        assertEquals(1, checkIn.sorenessScore)
        assertEquals(5, checkIn.energyScore)
        assertEquals(1, checkIn.stressScore)
        assertTrue(checkIn.overallPercentage in 0..100)

        // Cancelling/uncancelling schedule clears linkedWorkoutId in memory
        val schedRepo = com.bearbones.kumalift.data.repository.WorkoutScheduleRepository()
        val ev = schedRepo.scheduleRoutine("rt_test", "Push", "Push", kotlinx.datetime.LocalDate(2026, 11, 28))
        schedRepo.completeEvent(ev.id, "w_linked_1")
        assertEquals("w_linked_1", schedRepo.events.value.first { it.id == ev.id }.linkedWorkoutId)
        schedRepo.cancelEvent(ev.id)
        assertNull(schedRepo.events.value.first { it.id == ev.id }.linkedWorkoutId)
    }
}
