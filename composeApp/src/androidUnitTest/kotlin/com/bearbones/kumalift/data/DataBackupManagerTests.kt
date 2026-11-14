package com.bearbones.kumalift.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.export.DataBackupManager
import com.bearbones.kumalift.data.export.ImportMode
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.AppearanceSettings
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.RoutineBlueprint
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
import kotlin.test.assertTrue

class DataBackupManagerTests {

    private lateinit var database: KumaDatabase
    private lateinit var repository: KumaDatabaseRepository
    private lateinit var backupManager: DataBackupManager

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KumaDatabase.Schema.create(driver)
        database = KumaDatabase(driver)
        repository = KumaDatabaseRepository(database)
        backupManager = DataBackupManager(repository)
    }

    @Test
    fun testExportJsonAndRoundTripReplace() {
        val customExercise = Exercise(
            id = "custom_1",
            name = "Incline Deficit Push-Up",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BODYWEIGHT,
            movementType = MovementType.COMPOUND
        )
        repository.saveCustomExercise(customExercise)

        val pr = PersonalRecord(
            exerciseId = "custom_1",
            exerciseName = "Incline Deficit Push-Up",
            weightKg = 40.0,
            reps = 12,
            estimated1RmKg = 56.0,
            dateAchieved = "2026-11-14",
            prTier = "12RM PR"
        )
        repository.savePersonalRecord(pr)

        val workout = Workout(
            id = "w_test_roundtrip",
            title = "Chest Hypertrophy",
            splitType = "Push",
            startTimeEpochMs = 1731550000000L,
            endTimeEpochMs = 1731553600000L,
            durationSeconds = 3600,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_1",
                    exercise = customExercise,
                    sets = listOf(
                        SetLog(
                            id = "s_1",
                            setNumber = 1,
                            setType = SetType.NORMAL,
                            weightKg = 40.0,
                            reps = 12,
                            isCompleted = true
                        )
                    )
                )
            )
        )
        repository.saveCompletedWorkout(workout)

        val routine = RoutineBlueprint(
            id = "rt_custom_1",
            title = "Chest & Shoulders Blast",
            splitCategory = "Push",
            muscleFocus = "Chest · Delts",
            estimatedDurationMinutes = 45,
            targetRpeTier = "RPE 8.5",
            exerciseTemplates = listOf(customExercise)
        )
        repository.saveCustomRoutine(routine)

        val settings = UserSettings(
            profile = ProfileSettings(name = "Kuma Athlete", username = "kuma_pro"),
            units = UnitSettings(weightUnit = WeightUnit.LB),
            appearance = AppearanceSettings(theme = AppTheme.DARK, language = AppLanguage.EN)
        )
        repository.saveUserSettings(settings)

        // 1. Export JSON
        val exportedJson = backupManager.exportJson(settings)
        assertTrue(exportedJson.contains("\"schemaVersion\": 1"))
        assertTrue(exportedJson.contains("Incline Deficit Push-Up"))
        assertTrue(exportedJson.contains("w_test_roundtrip"))
        assertTrue(exportedJson.contains("kuma_pro"))

        // 2. Wipe DB
        repository.wipeAllUserData()
        assertEquals(0, repository.getCompletedWorkouts().size)
        assertEquals(0, repository.getAllPersonalRecords().size)
        assertEquals(0, repository.getAllCustomExercises().size)

        // 3. Restore with REPLACE
        var restoredSettings: UserSettings? = null
        val result = backupManager.importBackup(
            jsonString = exportedJson,
            mode = ImportMode.REPLACE,
            currentSettings = UserSettings(),
            onSettingsRestored = { restoredSettings = it }
        )

        assertTrue(result.success)
        assertEquals(1, result.workoutsCount)
        assertEquals(1, result.setsCount)
        assertEquals(1, result.prsCount)
        assertEquals(1, result.exercisesCount)
        assertEquals(1, result.routinesCount)

        // Verify data in DB matches original
        val restoredWorkouts = repository.getCompletedWorkouts()
        assertEquals(1, restoredWorkouts.size)
        assertEquals("w_test_roundtrip", restoredWorkouts.first().id)
        assertEquals(1, restoredWorkouts.first().exercises.size)
        assertEquals(40.0, restoredWorkouts.first().exercises.first().sets.first().weightKg)

        val restoredPrs = repository.getAllPersonalRecords()
        assertEquals(1, restoredPrs.size)
        assertEquals(40.0, restoredPrs.first().weightKg)

        assertNotNull(restoredSettings)
        assertEquals("Kuma Athlete", restoredSettings?.profile?.name)
        assertEquals(WeightUnit.LB, restoredSettings?.units?.weightUnit)

        // 4. Verify safety snapshot was automatically preserved
        val snapshot = repository.getLatestSafetySnapshot()
        assertNotNull(snapshot)
    }

    @Test
    fun testMergeImportDoesNotDuplicateWorkouts() {
        val customExercise = Exercise(
            id = "ex_db_1",
            name = "Dumbbell Curl",
            primaryMuscle = MuscleGroup.ARMS,
            equipment = Equipment.DUMBBELL,
            movementType = MovementType.ISOLATION
        )
        repository.saveCustomExercise(customExercise)

        val workout1 = Workout(
            id = "w_existing",
            title = "Arm Day",
            startTimeEpochMs = 1731550000000L,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_arm",
                    exercise = customExercise,
                    sets = listOf(SetLog(id = "s_arm", setNumber = 1, weightKg = 16.0, reps = 10, isCompleted = true))
                )
            )
        )
        repository.saveCompletedWorkout(workout1)

        val workout2 = Workout(
            id = "w_new",
            title = "Leg Day",
            startTimeEpochMs = 1731553600000L,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_leg",
                    exercise = customExercise,
                    sets = listOf(SetLog(id = "s_leg", setNumber = 1, weightKg = 80.0, reps = 5, isCompleted = true))
                )
            )
        )

        val backupData = backupManager.createBackupPayload(UserSettings()).copy(
            workouts = listOf(workout1, workout2)
        )
        val backupJson = kotlinx.serialization.json.Json.encodeToString(com.bearbones.kumalift.data.export.KumaBackupData.serializer(), backupData)

        val result = backupManager.importBackup(backupJson, ImportMode.MERGE, UserSettings())

        assertTrue(result.success)
        assertEquals(1, result.workoutsCount) // Only workout2 was newly added

        val allWorkouts = repository.getCompletedWorkouts()
        assertEquals(2, allWorkouts.size)
    }

    @Test
    fun testExportCsvGeneration() {
        val settings = UserSettings()
        val csvMap = backupManager.exportCsvFiles(settings)

        assertTrue(csvMap.containsKey("workouts.csv"))
        assertTrue(csvMap.containsKey("sets.csv"))
        assertTrue(csvMap.containsKey("personal_records.csv"))
        assertTrue(csvMap.containsKey("custom_routines.csv"))
        assertTrue(csvMap.containsKey("custom_exercises.csv"))

        assertTrue(csvMap["workouts.csv"]!!.startsWith("id,title,splitType"))
        assertTrue(csvMap["sets.csv"]!!.startsWith("workoutId,exerciseId"))
    }

    @Test
    fun testImportMessagesFollowActiveLanguage() {
        // 1. Missing safety snapshot message (on fresh DB)
        val snapEn = backupManager.restoreSafetySnapshot(UserSettings(), isEnglish = true)
        val snapId = backupManager.restoreSafetySnapshot(UserSettings(), isEnglish = false)
        assertEquals("No safety snapshot available.", snapEn.message)
        assertEquals("Tidak ada snapshot keamanan yang tersedia.", snapId.message)

        // 2. Invalid JSON messages
        val errEn = backupManager.importBackup("not-json", ImportMode.MERGE, UserSettings(), isEnglish = true)
        val errId = backupManager.importBackup("not-json", ImportMode.MERGE, UserSettings(), isEnglish = false)
        assertTrue(errEn.message.startsWith("Invalid or corrupted JSON format"))
        assertTrue(errId.message.startsWith("Format JSON tidak valid atau rusak"))

        // 3. Successful REPLACE messages
        val payload = backupManager.createBackupPayload(UserSettings())
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(com.bearbones.kumalift.data.export.KumaBackupData.serializer(), payload)
        val resEn = backupManager.importBackup(jsonStr, ImportMode.REPLACE, UserSettings(), isEnglish = true)
        val resId = backupManager.importBackup(jsonStr, ImportMode.REPLACE, UserSettings(), isEnglish = false)
        assertTrue(resEn.message.startsWith("Successfully restored (Replace)"))
        assertTrue(resId.message.startsWith("Berhasil memulihkan (Replace)"))

        // 4. Successful MERGE messages
        val mergeEn = backupManager.importBackup(jsonStr, ImportMode.MERGE, UserSettings(), isEnglish = true)
        val mergeId = backupManager.importBackup(jsonStr, ImportMode.MERGE, UserSettings(), isEnglish = false)
        assertTrue(mergeEn.message.startsWith("Successfully merged (Merge)"))
        assertTrue(mergeId.message.startsWith("Berhasil menggabungkan (Merge)"))
    }
}
