package com.bearbones.kumalift.domain.model

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.bearbones.kumalift.core.localization.EnStrings
import com.bearbones.kumalift.core.localization.IdStrings
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.ProgressPhotoRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.database.KumaDatabase
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FitnessFeaturesIntegrationTests {

    private lateinit var database: KumaDatabase
    private lateinit var dbRepo: KumaDatabaseRepository

    @BeforeTest
    fun setup() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        KumaDatabase.Schema.create(driver)
        database = KumaDatabase(driver)
        dbRepo = KumaDatabaseRepository(database)
    }

    @Test
    fun testPersonalRecordsGroupedByExerciseAndWeightPreservesHistory() {
        val records = listOf(
            PersonalRecord(
                exerciseId = "ex_fly",
                exerciseName = "Dumbbell One Arm Bench Fly",
                weightKg = 12.0,
                reps = 16,
                estimated1RmKg = 18.4,
                dateAchieved = "Nov 26",
                prTier = "16RM PR",
                dateAchievedEpochMs = Instant.parse("2026-11-26T10:00:00Z").toEpochMilliseconds()
            ),
            PersonalRecord(
                exerciseId = "ex_fly",
                exerciseName = "Dumbbell One Arm Bench Fly",
                weightKg = 12.0,
                reps = 14,
                estimated1RmKg = 17.6,
                dateAchieved = "Nov 18",
                prTier = "14RM PR",
                dateAchievedEpochMs = Instant.parse("2026-11-18T10:00:00Z").toEpochMilliseconds()
            ),
            PersonalRecord(
                exerciseId = "ex_fly",
                exerciseName = "Dumbbell One Arm Bench Fly",
                weightKg = 12.0,
                reps = 13,
                estimated1RmKg = 17.2,
                dateAchieved = "Oct 30",
                prTier = "13RM PR",
                dateAchievedEpochMs = Instant.parse("2026-10-30T10:00:00Z").toEpochMilliseconds()
            ),
            PersonalRecord(
                exerciseId = "ex_fly",
                exerciseName = "Dumbbell One Arm Bench Fly",
                weightKg = 16.0,
                reps = 10,
                estimated1RmKg = 21.3,
                dateAchieved = "Nov 27",
                prTier = "10RM PR",
                dateAchievedEpochMs = Instant.parse("2026-11-27T10:00:00Z").toEpochMilliseconds()
            ),
            PersonalRecord(
                exerciseId = "ex_lat",
                exerciseName = "Dumbbell Lateral Raise",
                weightKg = 10.0,
                reps = 15,
                estimated1RmKg = 15.0,
                dateAchieved = "Nov 20",
                prTier = "15RM PR",
                dateAchievedEpochMs = Instant.parse("2026-11-20T10:00:00Z").toEpochMilliseconds()
            )
        )

        records.forEach { dbRepo.savePersonalRecord(it) }
        val prRepo = PersonalRecordRepository(initialPrs = dbRepo.getAllPersonalRecords(), databaseRepo = dbRepo)
        val storedAll = prRepo.personalRecords.value
        assertEquals(5, storedAll.size, "Grouping must never delete or merge historical PR records")

        val grouped = FitnessHistoryOrganizer.organizePersonalRecords(storedAll)
        assertEquals(2, grouped.size, "Should display one main group per unique exercise")

        val flyGroup = grouped.first { it.exerciseId == "ex_fly" }
        assertEquals(4, flyGroup.totalAchievements)
        assertEquals(2, flyGroup.weightGroups.size)

        val twelveKgGroup = flyGroup.weightGroups.first { it.weightKg == 12.0 }
        assertEquals(3, twelveKgGroup.records.size)
        assertEquals(listOf(16, 14, 13), twelveKgGroup.records.map { it.reps })

        val formattedEn = FitnessHistoryOrganizer.formatFullDateWithYear(
            twelveKgGroup.records.last().dateAchievedEpochMs,
            "",
            EnStrings,
            timeZone = TimeZone.UTC
        )
        val formattedId = FitnessHistoryOrganizer.formatFullDateWithYear(
            twelveKgGroup.records.last().dateAchievedEpochMs,
            "",
            IdStrings,
            timeZone = TimeZone.UTC
        )
        assertTrue(formattedEn.contains("2026"))
        assertTrue(formattedId.contains("2026"))
    }

    @Test
    fun testHomeRecentWorkoutsFiltersLastMonthAndKeepsOlderSessionsInDb() {
        val now = Instant.parse("2026-11-28T12:00:00Z")
        val workouts = listOf(
            sampleWorkout("w_1d", "Push Recent", Instant.parse("2026-11-27T10:00:00Z").toEpochMilliseconds()),
            sampleWorkout("w_20d", "Pull Mid", Instant.parse("2026-11-08T10:00:00Z").toEpochMilliseconds()),
            sampleWorkout("w_30d", "Boundary Month", Instant.parse("2026-10-28T00:00:00Z").toEpochMilliseconds()),
            sampleWorkout("w_35d", "Old October", Instant.parse("2026-10-24T10:00:00Z").toEpochMilliseconds()),
            sampleWorkout("w_90d", "August Session", Instant.parse("2026-08-15T10:00:00Z").toEpochMilliseconds())
        )
        workouts.forEach { dbRepo.saveCompletedWorkout(it) }

        val recent = FitnessHistoryOrganizer.filterRecentWorkoutsLastMonth(workouts, now, TimeZone.UTC)
        assertEquals(listOf("w_1d", "w_20d", "w_30d"), recent.map { it.id })
        assertEquals(5, dbRepo.getCompletedWorkouts().size, "Older workouts must remain stored in DB")

        val grouped = FitnessHistoryOrganizer.groupWorkoutsByMonth(workouts, TimeZone.UTC)
        assertEquals(3, grouped.size)
        assertEquals(listOf("2026_11", "2026_10", "2026_08"), grouped.map { it.key })
        assertEquals(listOf("w_30d", "w_35d"), grouped[1].workouts.map { it.id })
    }

    @Test
    fun testHealthBodySyncCoordinatorProtectsAgainstSilentOverwrite() {
        val ownPkg = "com.bearbones.kumalift"
        val pickedWeight = HealthBodySyncCoordinator.pickLatestValidSample(
            samples = listOf(
                HealthBodyMeasurementSample("bad_neg", -10.0, 1000L, "other.app"),
                HealthBodyMeasurementSample("self_echo", 82.0, 2000L, ownPkg),
                HealthBodyMeasurementSample("ext_same_ts", 81.4, 2000L, "com.withings.app"),
                HealthBodyMeasurementSample("older", 80.0, 1500L, "com.fitbit")
            ),
            minValid = 20.0,
            maxValid = 400.0,
            ownPackageName = ownPkg
        )
        assertEquals("ext_same_ts", pickedWeight?.recordId)

        val payload = HealthBodyMetricsSyncPayload(
            status = HealthSyncStatus.CONNECTED,
            weightSample = pickedWeight,
            heightSample = HealthBodyMeasurementSample("ht_1", 174.0, 2500L, "com.fitbit"),
            hasWeightReadPermission = true,
            hasHeightReadPermission = true
        )
        val autoFillDecision = HealthBodySyncCoordinator.evaluateSync(
            profile = ProfileSettings(bodyweightKg = null, heightCm = null),
            healthSettings = HealthSettings(healthEnabled = true, syncBodyMetrics = true),
            payload = payload
        )
        assertFalse(autoFillDecision.hasConflicts)
        assertEquals(81.4, autoFillDecision.autoFillWeightKg)
        assertEquals(174.0, autoFillDecision.autoFillHeightCm)

        val conflictDecision = HealthBodySyncCoordinator.evaluateSync(
            profile = ProfileSettings(bodyweightKg = 79.0, heightCm = 172.0),
            healthSettings = HealthSettings(healthEnabled = true, syncBodyMetrics = true),
            payload = payload
        )
        assertTrue(conflictDecision.hasConflicts)
        assertNotNull(conflictDecision.weightConflict)
        assertNotNull(conflictDecision.heightConflict)
        assertNull(conflictDecision.autoFillWeightKg)

        val dedupDecision = HealthBodySyncCoordinator.evaluateSync(
            profile = ProfileSettings(bodyweightKg = 79.0, heightCm = 172.0),
            healthSettings = HealthSettings(
                healthEnabled = true,
                syncBodyMetrics = true,
                lastWeightSyncRecordId = "ext_same_ts",
                lastHeightSyncRecordId = "ht_1"
            ),
            payload = payload
        )
        assertFalse(dedupDecision.hasConflicts)
        assertTrue(dedupDecision.alreadyUpToDate)
    }

    @Test
    fun testWorkoutPlanShareEncodeDecodeAndIndependentImportWithAttribution() {
        val exerciseRepo = ExerciseRepository(databaseRepo = dbRepo)
        val routineRepo = RoutineRepository(databaseRepo = dbRepo)
        val settingsRepo = SettingsRepository()
        val programRepo = TrainingProgramRepository(databaseRepo = dbRepo)

        val creatorProfile = ProfileSettings(
            name = "Gabriel",
            username = "kuma_coach",
            goal = TrainingGoal.STRENGTH,
            hasCompletedOnboarding = true
        )
        val sourceRoutine = RoutineBlueprint(
            id = "src_upper_1",
            programId = DEFAULT_PROGRAM_ID,
            title = "Upper Strength Focus",
            splitCategory = "Upper / Lower",
            muscleFocus = "Chest · Back · Shoulders",
            estimatedDurationMinutes = 55,
            scheduledDays = listOf(1, 4),
            exerciseConfigs = listOf(
                RoutineExerciseConfig(
                    exercise = exerciseRepo.exercises.value.first(),
                    targetSets = 4,
                    targetReps = 6,
                    targetWeightKg = 85.0,
                    restTimerSeconds = 120
                )
            )
        )

        val pkg = WorkoutPlanShareCodec.fromRoutine(sourceRoutine, EnStrings, creatorProfile)
        val code = WorkoutPlanShareCodec.encodePlanCode(pkg)
        val deepLink = WorkoutPlanShareCodec.encodeDeepLink(pkg)
        val shareText = WorkoutPlanShareCodec.buildShareMessage(pkg, EnStrings)
        val prettyJson = WorkoutPlanShareCodec.encodePrettyJson(pkg)

        assertFalse(shareText.contains("Hevy", ignoreCase = true))
        assertTrue(shareText.contains("KumaLift"))

        val decodedFromText = WorkoutPlanShareCodec.decodePlanInput(shareText)
        val decodedFromLink = WorkoutPlanShareCodec.decodePlanInput(deepLink)
        val decodedFromCode = WorkoutPlanShareCodec.decodePlanInput(code)
        val decodedFromJson = WorkoutPlanShareCodec.decodePlanInput(prettyJson)
        assertNull(WorkoutPlanShareCodec.decodePlanInput("not a valid plan"))

        listOf(decodedFromText, decodedFromLink, decodedFromCode, decodedFromJson).forEach { decoded ->
            assertNotNull(decoded)
            assertEquals("Upper Strength Focus", decoded.title)
            assertEquals("@kuma_coach", decoded.creatorDisplayLabel)
            assertEquals(1, decoded.routines.size)
            assertEquals(85.0, decoded.routines.first().exercises.first().targetWeightKg)
        }

        val imported = WorkoutPlanShareImporter.importAsPersonalCopy(
            pkg = decodedFromText!!,
            exerciseRepository = exerciseRepo,
            routineRepository = routineRepo,
            trainingProgramRepository = programRepo,
            settingsRepository = settingsRepo
        )
        assertEquals(1, imported.importedRoutines.size)
        val copiedRoutine = imported.importedRoutines.first()
        assertNotEquals(sourceRoutine.id, copiedRoutine.id)
        assertEquals("@kuma_coach", copiedRoutine.sharedByCreator)
        assertEquals("Upper Strength Focus", copiedRoutine.sharedSourceTitle)

        val persisted = dbRepo.getAllCustomRoutines { id -> exerciseRepo.getExerciseById(id) }
            .firstOrNull { it.id == copiedRoutine.id }
        assertNotNull(persisted)
        assertEquals("@kuma_coach", persisted.sharedByCreator)
        assertEquals("Upper Strength Focus", persisted.sharedSourceTitle)
    }

    @Test
    fun testProgressPhotoRepositoryPrivacyPersistenceAndDeleteCleanup() {
        val deletedFiles = mutableListOf<String>()
        val repo = ProgressPhotoRepository(
            initialPhotos = emptyList(),
            databaseRepo = dbRepo,
            deleteLocalFile = { deletedFiles.add(it) }
        )

        val pJan = ProgressPhotoEntry(
            id = "photo_jan",
            imagePath = "/data/user/0/app/files/progress_photos/jan.jpg",
            capturedDateEpochMs = Instant.parse("2026-01-10T09:00:00Z").toEpochMilliseconds(),
            bodyweightKg = 78.5,
            waistCm = 84.0,
            note = "Block 1 baseline",
            isPrivate = false
        )
        val pMar = ProgressPhotoEntry(
            id = "photo_mar",
            imagePath = "/data/user/0/app/files/progress_photos/mar.jpg",
            capturedDateEpochMs = Instant.parse("2026-03-15T09:00:00Z").toEpochMilliseconds(),
            bodyweightKg = 75.2,
            waistCm = 80.5,
            chestCm = 104.0,
            armsCm = 38.0,
            note = "Cut week 8"
        )
        repo.addPhoto(pJan)
        repo.addPhoto(pMar)

        val reloadedRepo = ProgressPhotoRepository(
            initialPhotos = emptyList(),
            databaseRepo = dbRepo,
            deleteLocalFile = { deletedFiles.add(it) }
        )
        reloadedRepo.reloadFromDatabase()
        val persisted = reloadedRepo.photos.value
        assertEquals(2, persisted.size)
        assertTrue(persisted.all { it.isPrivate }, "All progress photos must remain private by default")
        assertEquals(listOf("photo_mar", "photo_jan"), persisted.map { it.id })

        val monthly = groupProgressPhotosByMonth(persisted, TimeZone.UTC)
        assertEquals(listOf("2026_03", "2026_01"), monthly.map { it.key })

        reloadedRepo.deletePhoto("photo_jan")
        assertEquals(1, reloadedRepo.photos.value.size)
        assertEquals(1, dbRepo.getAllProgressPhotos().size)
        assertEquals(listOf(pJan.imagePath), deletedFiles)
    }

    private fun sampleWorkout(id: String, title: String, startMs: Long): Workout {
        val bench = Exercise(
            id = "bench_barbell",
            name = "Barbell Bench Press",
            primaryMuscle = MuscleGroup.CHEST,
            equipment = Equipment.BARBELL,
            movementType = MovementType.COMPOUND
        )
        return Workout(
            id = id,
            title = title,
            splitType = "Push",
            startTimeEpochMs = startMs,
            endTimeEpochMs = startMs + 3_000_000L,
            durationSeconds = 3000L,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    id = "we_$id",
                    exercise = bench,
                    sets = listOf(
                        SetLog(
                            id = "s_$id",
                            setNumber = 1,
                            weightKg = 80.0,
                            reps = 8,
                            isCompleted = true
                        )
                    )
                )
            )
        )
    }
}
