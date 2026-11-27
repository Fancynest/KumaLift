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
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import com.bearbones.kumalift.domain.model.ScheduleStatus
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        val snapEn = backupManager.restoreSafetySnapshot(UserSettings(), strings = com.bearbones.kumalift.core.localization.EnStrings)
        val snapId = backupManager.restoreSafetySnapshot(UserSettings(), strings = com.bearbones.kumalift.core.localization.IdStrings)
        assertEquals(com.bearbones.kumalift.core.localization.EnStrings.noSnapshotAvailable, snapEn.message)
        assertEquals(com.bearbones.kumalift.core.localization.IdStrings.noSnapshotAvailable, snapId.message)

        // 2. Invalid JSON messages
        val errEn = backupManager.importBackup("not-json", ImportMode.MERGE, UserSettings(), strings = com.bearbones.kumalift.core.localization.EnStrings)
        val errId = backupManager.importBackup("not-json", ImportMode.MERGE, UserSettings(), strings = com.bearbones.kumalift.core.localization.IdStrings)
        assertTrue(errEn.message.startsWith("Invalid or corrupted JSON format"))
        assertTrue(errId.message.startsWith("Format JSON tidak valid atau rusak"))

        // 3. Successful REPLACE messages
        val payload = backupManager.createBackupPayload(UserSettings())
        val jsonStr = kotlinx.serialization.json.Json.encodeToString(com.bearbones.kumalift.data.export.KumaBackupData.serializer(), payload)
        val resEn = backupManager.importBackup(jsonStr, ImportMode.REPLACE, UserSettings(), strings = com.bearbones.kumalift.core.localization.EnStrings)
        val resId = backupManager.importBackup(jsonStr, ImportMode.REPLACE, UserSettings(), strings = com.bearbones.kumalift.core.localization.IdStrings)
        assertTrue(resEn.message.startsWith("Successfully replaced"))
        assertTrue(resId.message.startsWith("Berhasil memulihkan (Timpa)"))

        // 4. Successful MERGE messages
        val mergeEn = backupManager.importBackup(jsonStr, ImportMode.MERGE, UserSettings(), strings = com.bearbones.kumalift.core.localization.EnStrings)
        val mergeId = backupManager.importBackup(jsonStr, ImportMode.MERGE, UserSettings(), strings = com.bearbones.kumalift.core.localization.IdStrings)
        assertTrue(mergeEn.message.startsWith("Successfully merged"))
        assertTrue(mergeId.message.startsWith("Berhasil menggabungkan"))
    }

    @Test
    fun testAutoBackupSettingsModelAndPersistence() {
        val initial = repository.loadUserSettings() ?: UserSettings()
        val customBackupSettings = initial.dataBackup.copy(
            autoBackupEnabled = true,
            autoBackupFrequency = com.bearbones.kumalift.domain.model.BackupFrequency.DAILY,
            autoBackupFolderUri = "content://com.android.externalstorage.documents/tree/primary%3ABackups",
            autoBackupFolderName = "Backups",
            autoBackupKeepLast = 10,
            lastBackupEpochMs = 1731553600000L,
            lastBackupResultSuccess = true,
            lastBackupResultError = null,
            lastBackupFileName = "KumaLift-backup-20261114-1200.kumalift"
        )
        val updated = initial.copy(dataBackup = customBackupSettings)
        repository.saveUserSettings(updated)

        val loaded = repository.loadUserSettings()
        assertNotNull(loaded)
        assertTrue(loaded.dataBackup.autoBackupEnabled)
        assertEquals(com.bearbones.kumalift.domain.model.BackupFrequency.DAILY, loaded.dataBackup.autoBackupFrequency)
        assertEquals("content://com.android.externalstorage.documents/tree/primary%3ABackups", loaded.dataBackup.autoBackupFolderUri)
        assertEquals("Backups", loaded.dataBackup.autoBackupFolderName)
        assertEquals(10, loaded.dataBackup.autoBackupKeepLast)
        assertEquals(1731553600000L, loaded.dataBackup.lastBackupEpochMs)
        assertEquals(true, loaded.dataBackup.lastBackupResultSuccess)
        assertEquals(null, loaded.dataBackup.lastBackupResultError)
        assertEquals("KumaLift-backup-20261114-1200.kumalift", loaded.dataBackup.lastBackupFileName)
    }

    @Test
    fun testAutoBackupPruningSimulation() {
        val keepLast = 3
        val mockFiles = listOf(
            "KumaLift-backup-20261110-1000.kumalift",
            "KumaLift-backup-20261111-1000.kumalift",
            "KumaLift-backup-20261112-1000.kumalift",
            "KumaLift-backup-20261113-1000.kumalift",
            "KumaLift-backup-20261114-1000.kumalift"
        )
        val sorted = mockFiles
            .filter { it.startsWith("KumaLift-backup-") && it.endsWith(".kumalift") }
            .sortedDescending()

        val retained = sorted.take(keepLast)
        val pruned = sorted.drop(keepLast)

        assertEquals(3, retained.size)
        assertEquals("KumaLift-backup-20261114-1000.kumalift", retained[0])
        assertEquals("KumaLift-backup-20261113-1000.kumalift", retained[1])
        assertEquals("KumaLift-backup-20261112-1000.kumalift", retained[2])

        assertEquals(2, pruned.size)
        assertTrue(pruned.contains("KumaLift-backup-20261111-1000.kumalift"))
        assertTrue(pruned.contains("KumaLift-backup-20261110-1000.kumalift"))
    }

    @Test
    fun testEncryptedPackageExportAndImportRoundTrip() {
        val exercise = Exercise(
            id = "custom_enc_1",
            name = "Encrypted Romanian Deadlift",
            primaryMuscle = MuscleGroup.LEGS,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        repository.saveCustomExercise(exercise)

        val settings = UserSettings()
        val password = "UltraSafePassword#2026"
        val encryptedPackage = backupManager.exportPackage(settings, password)

        // Wipe DB before restore
        repository.wipeAllUserData()
        assertTrue(repository.getAllCustomExercises().isEmpty())

        // Restore with correct password
        val result = backupManager.importBackupPackage(
            fileBytes = encryptedPackage,
            mode = ImportMode.REPLACE,
            currentSettings = settings,
            password = password
        )

        assertTrue(result.success, "Expected successful restore but got: ${result.message}")
        val restoredExercises = repository.getAllCustomExercises()
        assertEquals(1, restoredExercises.size)
        assertEquals("Encrypted Romanian Deadlift", restoredExercises[0].name)
    }

    @Test
    fun testEncryptedPackageWrongPasswordFails() {
        val settings = UserSettings()
        val password = "CorrectPassword123"
        val encryptedPackage = backupManager.exportPackage(settings, password)

        val result = backupManager.importBackupPackage(
            fileBytes = encryptedPackage,
            mode = ImportMode.REPLACE,
            currentSettings = settings,
            password = "WrongPassword999"
        )

        assertFalse(result.success)
        assertTrue(
            result.message.contains("Incorrect password or corrupted backup file") ||
            result.message.contains("Password required"),
            "Expected failure message but got: ${result.message}"
        )
    }

    @Test
    fun testScheduleEventsRoundTripReplace() {
        val event = WorkoutScheduleEvent(
            id = "sched_test_1",
            routineId = "rt_1",
            scheduledDate = "2026-11-28",
            status = ScheduleStatus.PLANNED,
            routineTitleSnapshot = "Leg Hypertrophy",
            splitTypeSnapshot = "Legs",
            notes = "Target 100kg squats",
            createdAtEpochMs = 1732000000000L,
            updatedAtEpochMs = 1732000000000L
        )
        repository.insertScheduleEvent(event)

        val settings = UserSettings()
        val jsonStr = backupManager.exportJson(settings)

        // Wipe data
        repository.wipeAllUserData()
        assertEquals(0, repository.getAllScheduleEvents().size)

        // Restore via replace
        val result = backupManager.importBackup(jsonStr, ImportMode.REPLACE, settings)
        assertTrue(result.success, "Replace import should succeed: ${result.message}")

        val restoredEvents = repository.getAllScheduleEvents()
        assertEquals(1, restoredEvents.size)
        val restored = restoredEvents[0]
        assertEquals("sched_test_1", restored.id)
        assertEquals("rt_1", restored.routineId)
        assertEquals("2026-11-28", restored.scheduledDate)
        assertEquals(ScheduleStatus.PLANNED, restored.status)
        assertEquals("Leg Hypertrophy", restored.routineTitleSnapshot)
        assertEquals("Target 100kg squats", restored.notes)
    }

    @Test
    fun testScheduleEventsRoundTripMerge() {
        val existingEvent = WorkoutScheduleEvent(
            id = "sched_existing",
            routineId = "rt_existing",
            scheduledDate = "2026-11-27",
            status = ScheduleStatus.COMPLETED,
            routineTitleSnapshot = "Chest Day",
            createdAtEpochMs = 1731900000000L,
            updatedAtEpochMs = 1731900000000L
        )
        repository.insertScheduleEvent(existingEvent)

        val newEvent = WorkoutScheduleEvent(
            id = "sched_imported",
            routineId = "rt_new",
            scheduledDate = "2026-11-29",
            status = ScheduleStatus.PLANNED,
            routineTitleSnapshot = "Back & Biceps",
            createdAtEpochMs = 1732100000000L,
            updatedAtEpochMs = 1732100000000L
        )

        val settings = UserSettings()
        val backupData = com.bearbones.kumalift.data.export.KumaBackupData(
            schemaVersion = 1,
            exportedAtEpochMs = 1732100000000L,
            settings = settings,
            scheduledEvents = listOf(existingEvent, newEvent)
        )
        val jsonStr = kotlinx.serialization.json.Json { encodeDefaults = true }.encodeToString(
            com.bearbones.kumalift.data.export.KumaBackupData.serializer(),
            backupData
        )

        val result = backupManager.importBackup(jsonStr, ImportMode.MERGE, settings)
        assertTrue(result.success, "Merge import should succeed: ${result.message}")

        val allEvents = repository.getAllScheduleEvents()
        assertEquals(2, allEvents.size)
        assertTrue(allEvents.any { it.id == "sched_existing" })
        assertTrue(allEvents.any { it.id == "sched_imported" })
    }

    @Test
    fun testLegacyBackupWithoutScheduledEventsImportsCleanly() {
        val settings = UserSettings()
        // Legacy JSON without "scheduledEvents" field
        val legacyJson = """
            {
              "schemaVersion": 1,
              "exportedAtEpochMs": 1730000000000,
              "appVersion": "1.9.0",
              "settings": ${kotlinx.serialization.json.Json.encodeToString(UserSettings.serializer(), settings)},
              "workouts": [],
              "personalRecords": [],
              "customRoutines": [],
              "customExercises": [],
              "trainingPrograms": []
            }
        """.trimIndent()

        val result = backupManager.importBackup(legacyJson, ImportMode.REPLACE, settings)
        assertTrue(result.success, "Legacy backup without scheduledEvents field must import without error")
        val events = repository.getAllScheduleEvents()
        assertEquals(0, events.size)
    }

    @Test
    fun testDuplicateBackupGuardBlocksSameDayUnchangedAndAllowsAnyChangeOrNewDay() {
        val baseSettings = UserSettings()
        val initialHash = backupManager.computeContentHash(baseSettings)
        val todayNoonMs = 1764244800000L // fixed UTC timestamp

        val afterTodayBackup = baseSettings.copy(
            dataBackup = baseSettings.dataBackup.copy(
                lastBackupEpochMs = todayNoonMs,
                lastBackupResultSuccess = true,
                lastBackupFileName = "KumaLift-backup-test.kumalift",
                lastBackupContentHash = initialHash
            )
        )

        // Same day + unchanged data (ignoring transient lastBackupEpochMs/FileName) => duplicate detected!
        assertTrue(
            backupManager.isSameDataBackedUpToday(
                settings = afterTodayBackup,
                nowEpochMs = todayNoonMs + 3_600_000L, // +1 hour same day
                timeZone = kotlinx.datetime.TimeZone.UTC
            )
        )

        // Different day (+24 hours) with identical data => NOT duplicate, new daily backup allowed!
        assertEquals(
            false,
            backupManager.isSameDataBackedUpToday(
                settings = afterTodayBackup,
                nowEpochMs = todayNoonMs + 86_400_000L,
                timeZone = kotlinx.datetime.TimeZone.UTC
            )
        )

        // Tiny change in settings (e.g. bodyweight updated by 0.1 kg) => NOT duplicate!
        val tinySettingChange = afterTodayBackup.copy(
            profile = afterTodayBackup.profile.copy(bodyweightKg = 72.5)
        )
        assertEquals(
            false,
            backupManager.isSameDataBackedUpToday(
                settings = tinySettingChange,
                nowEpochMs = todayNoonMs + 3_600_000L,
                timeZone = kotlinx.datetime.TimeZone.UTC
            )
        )

        // Tiny change in DB data (e.g. 1 new schedule event added) => NOT duplicate!
        repository.insertScheduleEvent(
            WorkoutScheduleEvent(
                id = "sched_tiny_change",
                routineId = "rt_1",
                scheduledDate = "2026-11-28",
                status = ScheduleStatus.PLANNED,
                routineTitleSnapshot = "Push Day",
                createdAtEpochMs = todayNoonMs,
                updatedAtEpochMs = todayNoonMs
            )
        )
        assertEquals(
            false,
            backupManager.isSameDataBackedUpToday(
                settings = afterTodayBackup,
                nowEpochMs = todayNoonMs + 3_600_000L,
                timeZone = kotlinx.datetime.TimeZone.UTC
            )
        )
    }
}
