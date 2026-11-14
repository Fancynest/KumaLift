package com.bearbones.kumalift.data.local

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppearanceSettings
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.UnitSettings
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalDatabasePersistenceTests {

    private lateinit var database: KumaDatabase
    private lateinit var repository: KumaDatabaseRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KumaDatabase.Schema.create(driver)
        database = KumaDatabase(driver)
        repository = KumaDatabaseRepository(database)
    }

    @Test
    fun testSaveAndRetrieveCompletedWorkout() {
        val exercise = Exercise(
            id = "test_bench",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )

        val sets = listOf(
            SetLog(
                id = "set_1",
                setNumber = 1,
                setType = SetType.NORMAL,
                weightKg = 100.0,
                reps = 5,
                isCompleted = true,
                heartRateBpm = 142
            ),
            SetLog(
                id = "set_2",
                setNumber = 2,
                setType = SetType.NORMAL,
                weightKg = 100.0,
                reps = 5,
                isCompleted = true,
                heartRateBpm = 145
            )
        )

        val workoutExercise = WorkoutExercise(
            id = "we_1",
            exercise = exercise,
            sets = sets,
            notes = "Felt strong"
        )

        val workout = Workout(
            id = "w_completed_1",
            title = "Chest Day",
            splitType = "Push",
            startTimeEpochMs = currentEpochMillis() - 3600_000L,
            endTimeEpochMs = currentEpochMillis(),
            durationSeconds = 3600,
            exercises = listOf(workoutExercise),
            sessionRpe = 8.5,
            notes = "Great workout",
            isFinished = true,
            avgHeartRateBpm = 138,
            peakHeartRateBpm = 162,
            activeCaloriesKcal = 420.0
        )

        repository.saveCompletedWorkout(workout)

        val retrieved = repository.getCompletedWorkouts()
        assertEquals(1, retrieved.size)
        val loaded = retrieved.first()

        assertEquals("w_completed_1", loaded.id)
        assertEquals("Chest Day", loaded.title)
        assertEquals(1, loaded.exercises.size)
        assertEquals(2, loaded.exercises.first().sets.size)
        assertEquals(100.0, loaded.exercises.first().sets.first().weightKg)
        assertEquals(5, loaded.exercises.first().sets.first().reps)
        assertTrue(loaded.exercises.first().sets.first().isCompleted)
        assertEquals(8.5, loaded.sessionRpe)
        assertEquals(138, loaded.avgHeartRateBpm)
    }

    @Test
    fun testActiveWorkoutPersistenceAndResume() {
        val exercise = Exercise(
            id = "test_squat",
            name = "Barbell Squat",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL
        )

        val activeWorkout = Workout(
            id = "w_active_123",
            title = "Leg Day Active",
            splitType = "Legs",
            startTimeEpochMs = currentEpochMillis(),
            exercises = listOf(
                WorkoutExercise(
                    id = "we_squat",
                    exercise = exercise,
                    sets = listOf(
                        SetLog(id = "s_1", setNumber = 1, weightKg = 140.0, reps = 5, isCompleted = true)
                    )
                )
            ),
            isFinished = false
        )

        // Save active workout
        repository.saveActiveWorkout(activeWorkout)

        // Verify it can be loaded if not stale
        val restored = repository.loadActiveWorkout(staleWorkoutHours = 24)
        assertNotNull(restored)
        assertEquals("w_active_123", restored.id)
        assertEquals("Leg Day Active", restored.title)
        assertEquals(1, restored.exercises.size)
        assertEquals(140.0, restored.exercises.first().sets.first().weightKg)

        // Verify clearActiveWorkout removes it
        repository.clearActiveWorkout()
        val emptyRestored = repository.loadActiveWorkout(staleWorkoutHours = 24)
        assertNull(emptyRestored)
    }

    @Test
    fun testUserSettingsPersistenceFullFidelity() {
        val customSettings = UserSettings(
            settingsVersion = 1,
            profile = ProfileSettings(
                name = "Kuma Athlete",
                username = "kuma_heavy",
                bio = "Heavy lifting daily",
                bodyweightKg = 84.5,
                heightCm = 178.0
            ),
            units = UnitSettings(
                weightUnit = WeightUnit.KG
            ),
            appearance = AppearanceSettings(
                theme = AppTheme.DARK,
                highContrast = true,
                language = AppLanguage.EN
            )
        )

        repository.saveUserSettings(customSettings)

        val loaded = repository.loadUserSettings()
        assertNotNull(loaded)
        assertEquals("Kuma Athlete", loaded.profile.name)
        assertEquals("kuma_heavy", loaded.profile.username)
        assertEquals(84.5, loaded.profile.bodyweightKg)
        assertEquals(178.0, loaded.profile.heightCm)
        assertEquals(WeightUnit.KG, loaded.units.weightUnit)
        assertEquals(AppTheme.DARK, loaded.appearance.theme)
        assertTrue(loaded.appearance.highContrast)
        assertEquals(AppLanguage.EN, loaded.appearance.language)
    }

    @Test
    fun testPersonalRecordDetectionAndRevocation() {
        val prRepo = PersonalRecordRepository(
            initialPrs = emptyList(),
            databaseRepo = repository
        )

        val exercise = Exercise(
            id = "0025",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL
        )

        // Normal set with 100kg x 5 -> Should record PR
        val pr1 = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 100.0,
            reps = 5,
            sourceSetId = "set_bench_1",
            setType = SetType.NORMAL
        )
        assertNotNull(pr1)
        assertEquals(100.0, pr1.weightKg)
        assertEquals(5, pr1.reps)
        assertEquals(1, prRepo.personalRecords.value.size)

        // Warmup set with higher weight -> Should NOT record PR
        val warmupPr = prRepo.detectAndRecordPr(
            exercise = exercise,
            weightKg = 120.0,
            reps = 5,
            sourceSetId = "set_bench_warmup",
            setType = SetType.WARMUP
        )
        assertNull(warmupPr)

        // Verify PR was saved in DB
        val dbPrs = repository.getAllPersonalRecords()
        assertEquals(1, dbPrs.size)
        assertEquals(100.0, dbPrs.first().weightKg)

        // Un-toggling set revokes PR
        prRepo.revokePrBySetId("set_bench_1")
        assertEquals(0, prRepo.personalRecords.value.size)
        assertEquals(0, repository.getAllPersonalRecords().size)
    }

    @Test
    fun testWorkoutRepositoryEndToEnd() {
        val prRepo = PersonalRecordRepository(
            initialPrs = emptyList(),
            databaseRepo = repository
        )
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            databaseRepo = repository
        )

        // Fresh install should have NO active and NO completed workouts
        assertNull(workoutRepo.activeWorkout.value)
        assertEquals(0, workoutRepo.completedWorkouts.value.size)

        // Start empty workout
        workoutRepo.startEmptyWorkout("Full Body Workout")
        val active = workoutRepo.activeWorkout.value
        assertNotNull(active)
        assertEquals("Full Body Workout", active.title)

        // Add exercise
        val deadlift = Exercise(
            id = "0032",
            name = "Barbell Deadlift",
            primaryMuscle = MuscleGroup.BACK,
            equipment = Equipment.BARBELL
        )
        workoutRepo.addExerciseToActiveWorkout(deadlift)
        assertEquals(1, workoutRepo.activeWorkout.value!!.exercises.size)

        val we = workoutRepo.activeWorkout.value!!.exercises.first()
        val set1 = we.sets.first()

        // Update set weight and toggle complete
        workoutRepo.updateSet(we.id, set1.id, weightKg = 180.0, reps = 5)
        var toggleCallbackInvoked = false
        workoutRepo.toggleSetCompletion(we.id, set1.id) { isCompleted ->
            toggleCallbackInvoked = true
            assertTrue(isCompleted)
        }
        assertTrue(toggleCallbackInvoked)

        // PR should be detected
        assertEquals(1, prRepo.personalRecords.value.size)
        assertEquals(180.0, prRepo.personalRecords.value.first().weightKg)

        // Finish workout
        val finished = workoutRepo.finishWorkout(sessionRpe = 9.0)
        assertNotNull(finished)
        assertTrue(finished.isFinished)
        assertNull(workoutRepo.activeWorkout.value)
        assertEquals(1, workoutRepo.completedWorkouts.value.size)

        // Verify stored in DB
        val dbWorkouts = repository.getCompletedWorkouts()
        assertEquals(1, dbWorkouts.size)
        assertEquals(180.0, dbWorkouts.first().exercises.first().sets.first().weightKg)
    }
}
