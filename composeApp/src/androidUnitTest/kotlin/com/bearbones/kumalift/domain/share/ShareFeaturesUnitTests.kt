package com.bearbones.kumalift.domain.share

import com.bearbones.kumalift.core.localization.EnStrings
import com.bearbones.kumalift.core.localization.IdStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.RoutineSetConfig
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.ShareItemOffset
import com.bearbones.kumalift.domain.model.ShareSettings
import com.bearbones.kumalift.domain.model.SharedExerciseSpec
import com.bearbones.kumalift.domain.model.SharedPlanType
import com.bearbones.kumalift.domain.model.SharedRoutineSpec
import com.bearbones.kumalift.domain.model.SharedWorkoutPlanPackage
import com.bearbones.kumalift.domain.model.UnitSettings
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import com.bearbones.kumalift.domain.model.WorkoutPlanShareCodec
import com.bearbones.kumalift.core.media.encodeQrCodeMatrix
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ShareFeaturesUnitTests {

    private val bench = Exercise(
        id = "ex_bench",
        name = "Barbell Bench Press",
        primaryMuscle = MuscleGroup.CHEST,
        equipment = Equipment.BARBELL,
        movementType = MovementType.COMPOUND
    )

    private val squat = Exercise(
        id = "ex_squat",
        name = "Barbell Squat",
        primaryMuscle = MuscleGroup.LEGS,
        equipment = Equipment.BARBELL,
        movementType = MovementType.COMPOUND
    )

    private val curl = Exercise(
        id = "ex_curl",
        name = "Dumbbell Curl",
        primaryMuscle = MuscleGroup.ARMS,
        equipment = Equipment.DUMBBELL,
        movementType = MovementType.ISOLATION
    )

    @Test
    fun testStatSelectionMaxCapReorderAndPersistence() {
        var selected = ShareSettings.DEFAULT_WORKOUT_SHARE_STAT_IDS
        assertEquals(listOf("DURATION", "VOLUME", "PR_COUNT"), selected)

        // Add up to 6 stats
        selected = WorkoutShareStatsBuilder.toggleStatSelection(selected, "SETS_COMPLETED")
        selected = WorkoutShareStatsBuilder.toggleStatSelection(selected, "EXERCISES_DONE")
        selected = WorkoutShareStatsBuilder.toggleStatSelection(selected, "EST_CALORIES")
        assertEquals(6, selected.size)

        // Attempting to add a 7th stat is blocked by maxCount = 6
        val capped = WorkoutShareStatsBuilder.toggleStatSelection(selected, "WEEKLY_STREAK")
        assertEquals(6, capped.size)
        assertFalse(capped.contains("WEEKLY_STREAK"))

        // Reorder upward moves item correctly
        val moved = WorkoutShareStatsBuilder.moveStatSelection(capped, "EST_CALORIES", -1)
        assertEquals("EST_CALORIES", moved[4])
        assertEquals("EXERCISES_DONE", moved[5])

        // Removing down to 1 retains the last stat (never drops to 0)
        var minimal = listOf("DURATION")
        minimal = WorkoutShareStatsBuilder.toggleStatSelection(minimal, "DURATION")
        assertEquals(listOf("DURATION"), minimal)

        // Persist through SettingsRepository and JSON serialization roundtrip
        var savedSettings: UserSettings? = null
        val settingsRepo = SettingsRepository(
            initialSettings = UserSettings(),
            onSave = { savedSettings = it }
        )
        settingsRepo.update { it.copy(share = it.share.copy(selectedWorkoutStatIds = moved)) }
        assertEquals(moved, savedSettings?.share?.selectedWorkoutStatIds)

        val encoded = Json.encodeToString(savedSettings!!)
        val decoded = Json.decodeFromString<UserSettings>(encoded)
        assertEquals(moved, decoded.share.selectedWorkoutStatIds)
    }

    @Test
    fun testWorkoutShareStatsRespectsUnitsDateAndSummarySource() {
        val workout = Workout(
            id = "w_share_1",
            title = "Upper Power",
            splitType = "Upper",
            startTimeEpochMs = 1764288000000L, // 2025-11-28 UTC
            durationSeconds = 3665L, // 61:05
            exercises = listOf(
                WorkoutExercise(
                    id = "we_1",
                    exercise = bench,
                    sets = listOf(
                        SetLog("s1", 1, SetType.NORMAL, weightKg = 100.0, reps = 5, isCompleted = true, isPr = true),
                        SetLog("s2", 2, SetType.NORMAL, weightKg = 80.0, reps = 10, isCompleted = true, isPr = false)
                    )
                )
            ),
            sessionRpe = 8.5,
            activeCaloriesKcal = 320.0,
            isFinished = true
        )

        val settingsLb = UserSettings(
            units = UnitSettings(weightUnit = WeightUnit.LB, dateFormat = DateFormatPreference.YMD),
            profile = com.bearbones.kumalift.domain.model.ProfileSettings(),
            appearance = com.bearbones.kumalift.domain.model.AppearanceSettings(language = AppLanguage.EN)
        )

        val data = WorkoutShareStatsBuilder.build(
            workout = workout,
            completedWorkouts = listOf(workout),
            settings = settingsLb,
            s = EnStrings
        )

        val byId = data.candidates.associateBy { it.id }
        assertEquals("61:05", byId["DURATION"]?.value)
        assertEquals("lb", byId["VOLUME"]?.unit)
        assertEquals("1", byId["PR_COUNT"]?.value)
        assertEquals("2 / 2", byId["SETS_COMPLETED"]?.value)
        assertEquals("320", byId["EST_CALORIES"]?.value)
        assertEquals("8.5", byId["SESSION_RPE"]?.value)
        assertEquals("2025-11-28", byId["WORKOUT_DATE"]?.value)
        assertTrue(byId["TOP_SET"]?.value?.contains("Barbell Bench Press") == true)
    }

    @Test
    fun testProgramShareScopeSelectionAndEmptyDayOmission() {
        val routineA = RoutineBlueprint(
            id = "rt_upper",
            programId = "prog_ul",
            title = "Workout A: Upper",
            splitCategory = "Upper / Lower",
            muscleFocus = "CHEST,BICEPS",
            exerciseConfigs = listOf(
                RoutineExerciseConfig(
                    exercise = bench,
                    targetSets = 3,
                    targetReps = 8,
                    setConfigs = listOf(
                        RoutineSetConfig(1, targetReps = 8, targetWeightKg = 80.0, targetRpe = 8.0),
                        RoutineSetConfig(2, targetReps = 10, targetWeightKg = 80.0, targetRpe = 8.0),
                        RoutineSetConfig(3, targetReps = 10, targetWeightKg = 80.0, targetRpe = 8.0)
                    )
                ),
                RoutineExerciseConfig(exercise = curl, targetSets = 2, targetReps = 12)
            )
        )
        val routineB = RoutineBlueprint(
            id = "rt_lower",
            programId = "prog_ul",
            title = "Workout B: Lower",
            splitCategory = "Upper / Lower",
            muscleFocus = "QUADS",
            exerciseConfigs = listOf(
                RoutineExerciseConfig(exercise = squat, targetSets = 3, targetReps = 6)
            )
        )
        val routines = listOf(routineA, routineB)

        // 1. Whole program includes both days
        val whole = ProgramShareBuilder.build(
            programTitle = "Upper / Lower Split",
            routines = routines,
            scope = ProgramShareScope.WHOLE_PROGRAM,
            selectedRoutineId = null,
            selectedExerciseKeys = emptySet(),
            showTargets = true,
            weightUnit = WeightUnit.KG,
            language = AppLanguage.EN,
            s = EnStrings
        )
        assertEquals(2, whole.columns.size)
        assertEquals(3, whole.totalExercisesCount)
        assertEquals("3 sets × 8–10 reps", whole.columns[0].exercises[0].setsRepsLabel)
        assertNotNull(whole.columns[0].exercises[0].targetDetailsLabel)

        // 2. Single routine returns only the chosen day
        val single = ProgramShareBuilder.build(
            programTitle = "Upper / Lower Split",
            routines = routines,
            scope = ProgramShareScope.SINGLE_ROUTINE,
            selectedRoutineId = "rt_lower",
            selectedExerciseKeys = emptySet(),
            showTargets = false,
            weightUnit = WeightUnit.KG,
            language = AppLanguage.ID,
            s = IdStrings
        )
        assertEquals(1, single.columns.size)
        assertEquals("Workout B: Lower", single.columns[0].title)
        assertEquals(MuscleGroup.LEGS.label(IdStrings), single.columns[0].exercises[0].muscleLabel)

        // 3. Custom selection preserves day order and omits days with 0 selected exercises
        val onlyCurlKey = ProgramShareBuilder.buildExerciseItemKey("rt_upper", 1, "ex_curl")
        val custom = ProgramShareBuilder.build(
            programTitle = "Upper / Lower Split",
            routines = routines,
            scope = ProgramShareScope.CUSTOM_SELECTION,
            selectedRoutineId = null,
            selectedExerciseKeys = setOf(onlyCurlKey),
            showTargets = false,
            weightUnit = WeightUnit.KG,
            language = AppLanguage.EN,
            s = EnStrings
        )
        assertEquals(1, custom.columns.size)
        assertEquals("rt_upper", custom.columns[0].routineId)
        assertEquals(listOf("ex_curl"), custom.columns[0].exercises.map { it.exerciseId })
    }

    @Test
    fun testExportFileNameAndHeightCappingMath() {
        assertEquals("kumalift_workout_1700000000.png", buildShareExportFileName("KumaLift Workout!!", 1700000000L))
        assertEquals("kumalift_share_42.png", buildShareExportFileName("   ", 42L))

        val normal = calculateCappedExportDimensions(naturalWidthPx = 1080, naturalHeightPx = 2400)
        assertFalse(normal.isCapped)
        assertEquals(1080, normal.widthPx)
        assertEquals(2400, normal.heightPx)
        assertEquals(1f, normal.scaleFactor)

        val huge = calculateCappedExportDimensions(naturalWidthPx = 1080, naturalHeightPx = 12000)
        assertTrue(huge.isCapped)
        assertEquals(540, huge.widthPx)
        assertEquals(6000, huge.heightPx)
        assertEquals(0.5f, huge.scaleFactor)
    }

    @Test
    fun testCustomDragPositionsClampingDefaultsAndPersistence() {
        val defaults = ShareLayoutKeys.defaultPositions(
            style = ShareCardTemplateStyle.HEVY_CLASSIC,
            visibleStatIds = listOf("DURATION", "VOLUME", "PR_COUNT")
        )
        assertTrue(defaults.containsKey("DURATION"))
        assertTrue(defaults.containsKey(ShareLayoutKeys.WORKOUT_TITLE))
        assertTrue(defaults.containsKey(ShareLayoutKeys.BRAND_MARK))

        val clampedLow = ShareLayoutKeys.clampOffset(-0.4f, -1.2f)
        assertEquals(0f, clampedLow.x)
        assertEquals(0f, clampedLow.y)

        val clampedHigh = ShareLayoutKeys.clampOffset(1.8f, 2.5f)
        assertEquals(1f, clampedHigh.x)
        assertEquals(1f, clampedHigh.y)

        val customPos = mapOf(
            "DURATION" to ShareItemOffset(0.25f, 0.72f),
            ShareLayoutKeys.BRAND_MARK to ShareItemOffset(0.88f, 0.12f)
        )
        val settings = UserSettings(
            share = ShareSettings(
                selectedWorkoutStatIds = listOf("DURATION", "VOLUME"),
                customItemPositions = customPos,
                showBrandWatermark = false
            )
        )
        val roundtrip = Json.decodeFromString<UserSettings>(Json.encodeToString(settings))
        assertEquals(0.25f, roundtrip.share.customItemPositions["DURATION"]?.x)
        assertEquals(0.72f, roundtrip.share.customItemPositions["DURATION"]?.y)
        assertFalse(roundtrip.share.showBrandWatermark)
    }

    @Test
    fun testDynamicBarcodePayloadChangesWithScopeAndPreservesSchedule() {
        val routineUpper = RoutineBlueprint(
            id = "rt_u",
            programId = "prog_1",
            title = "Upper",
            splitCategory = "Upper / Lower",
            muscleFocus = "CHEST",
            scheduledDays = listOf(1, 4),
            exerciseConfigs = listOf(
                RoutineExerciseConfig(exercise = bench, targetSets = 3, targetReps = 10),
                RoutineExerciseConfig(exercise = curl, targetSets = 3, targetReps = 12)
            )
        )
        val routineLower = RoutineBlueprint(
            id = "rt_l",
            programId = "prog_1",
            title = "Lower",
            splitCategory = "Upper / Lower",
            muscleFocus = "LEGS",
            scheduledDays = listOf(2, 5),
            exerciseConfigs = listOf(
                RoutineExerciseConfig(exercise = squat, targetSets = 4, targetReps = 6)
            )
        )
        val routines = listOf(routineUpper, routineLower)

        val pkgWhole = ProgramShareBuilder.buildScopedPlanPackage(
            programTitle = "UL Split",
            routines = routines,
            scope = ProgramShareScope.WHOLE_PROGRAM,
            selectedRoutineId = null,
            selectedExerciseKeys = emptySet(),
            creatorTag = "@gabriel",
            s = EnStrings
        )
        val pkgSingle = ProgramShareBuilder.buildScopedPlanPackage(
            programTitle = "UL Split",
            routines = routines,
            scope = ProgramShareScope.SINGLE_ROUTINE,
            selectedRoutineId = "rt_l",
            selectedExerciseKeys = emptySet(),
            creatorTag = "@gabriel",
            s = EnStrings
        )
        val onlyCurlKey = ProgramShareBuilder.buildExerciseItemKey("rt_u", 1, "ex_curl")
        val pkgCustom = ProgramShareBuilder.buildScopedPlanPackage(
            programTitle = "UL Split",
            routines = routines,
            scope = ProgramShareScope.CUSTOM_SELECTION,
            selectedRoutineId = null,
            selectedExerciseKeys = setOf(onlyCurlKey),
            creatorTag = "@gabriel",
            s = EnStrings
        )

        val qrWhole = WorkoutPlanShareCodec.encodeQrCompactPlanCode(pkgWhole)
        val qrSingle = WorkoutPlanShareCodec.encodeQrCompactPlanCode(pkgSingle)
        val qrCustom = WorkoutPlanShareCodec.encodeQrCompactPlanCode(pkgCustom)

        // Barcode payload changes per user choice and decodes back cleanly with schedules intact
        assertTrue(qrWhole.startsWith(WorkoutPlanShareCodec.PLAN_CODE_V2_PREFIX))
        assertFalse(qrWhole == qrSingle)
        assertFalse(qrWhole == qrCustom)
        assertFalse(qrSingle == qrCustom)

        val decodedWhole = WorkoutPlanShareCodec.decodePlanInput(qrWhole)
        assertNotNull(decodedWhole)
        assertEquals(listOf(1, 2, 4, 5), decodedWhole.activeDays)
        assertEquals(listOf(1, 4), decodedWhole.routines[0].scheduledDays)

        val decodedCustom = WorkoutPlanShareCodec.decodePlanInput(qrCustom)
        assertNotNull(decodedCustom)
        assertEquals(1, decodedCustom.routines.size)
        assertEquals(listOf("ex_curl"), decodedCustom.routines[0].exercises.map { it.exerciseId })

        val qrMatrix = encodeQrCodeMatrix(qrCustom)
        assertNotNull(qrMatrix)
        assertTrue(qrMatrix.size >= 21)
    }

    @Test
    fun testDecodePlanRejectsPostSanitizeEmptyRoutinesAndHandlesUnpaddedBase64AndLowercasePercentLinks() {
        val rawValid = WorkoutPlanShareCodec.encodeQrCompactPlanCode(
            SharedWorkoutPlanPackage(
                planType = SharedPlanType.ROUTINE,
                title = "Valid Plan",
                routines = listOf(
                    SharedRoutineSpec(
                        title = "Day 1",
                        exercises = listOf(SharedExerciseSpec(name = "Squat"))
                    )
                )
            )
        )
        val unpaddedV2 = rawValid.trimEnd('=')
        val decodedUnpadded = WorkoutPlanShareCodec.decodePlanInput(unpaddedV2)
        assertNotNull(decodedUnpadded)
        assertEquals("Valid Plan", decodedUnpadded.title)

        val emptyAfterSanitizeJson = """{"planType":"ROUTINE","title":"Bad Plan","routines":[{"title":"Empty","exercises":[]}]}"""
        assertEquals(null, WorkoutPlanShareCodec.decodePlanInput(emptyAfterSanitizeJson))

        val pkgForDeepLink = SharedWorkoutPlanPackage(
            planType = SharedPlanType.ROUTINE,
            title = "Upper",
            routines = listOf(
                SharedRoutineSpec(
                    title = "A / B",
                    exercises = listOf(SharedExerciseSpec(name = "Row"))
                )
            )
        )
        val linkWithLowerPct = WorkoutPlanShareCodec.encodeDeepLink(pkgForDeepLink)
            .replace("%3D", "%3d")
            .replace("%2B", "%2b")
            .replace("%2F", "%2f")
        val decodedLowerPct = WorkoutPlanShareCodec.decodePlanInput(linkWithLowerPct)
        assertNotNull(decodedLowerPct)
        assertEquals("Upper", decodedLowerPct.title)
    }

    @Test
    fun testCompactQrStripsUniformSetsAndRoundTripsLosslessly() {
        val uniformRoutine = RoutineBlueprint(
            id = "rt_compact_test",
            programId = "prog_1",
            title = "Push Day",
            splitCategory = "Push",
            muscleFocus = "CHEST",
            scheduledDays = listOf(1, 4),
            exerciseConfigs = List(6) { idx ->
                RoutineExerciseConfig(
                    exercise = bench.copy(id = "ex_$idx", name = "Exercise $idx"),
                    targetSets = 4,
                    targetReps = 10,
                    targetWeightKg = 60.0,
                    restTimerSeconds = 90,
                    setConfigs = (1..4).map { setNum ->
                        RoutineSetConfig(setNumber = setNum, targetReps = 10, targetWeightKg = 60.0, restTimerSeconds = 90)
                    }
                )
            }
        )
        val pkg = ProgramShareBuilder.buildScopedPlanPackage(
            programTitle = "Hypertrophy",
            routines = listOf(uniformRoutine),
            scope = ProgramShareScope.SINGLE_ROUTINE,
            selectedRoutineId = uniformRoutine.id,
            selectedExerciseKeys = emptySet(),
            creatorTag = "@user",
            s = EnStrings
        )
        val compactToken = WorkoutPlanShareCodec.encodeQrCompactPlanCode(pkg)
        val decoded = WorkoutPlanShareCodec.decodePlanInput(compactToken)
        assertNotNull(decoded)
        assertEquals("Push Day", decoded.routines.first().title)
        assertEquals(6, decoded.routines.first().exercises.size)
        assertEquals(4, decoded.routines.first().exercises.first().targetSets)
        assertEquals(10, decoded.routines.first().exercises.first().targetReps)
        assertEquals(60.0, decoded.routines.first().exercises.first().targetWeightKg)
        val matrix = encodeQrCodeMatrix(compactToken)
        assertNotNull(matrix)
        assertTrue(matrix.size <= 77, "Compact 6-exercise routine QR should remain sparse and easy to scan (size=${matrix.size})")
    }

    @Test
    fun testShareStatsUseRealPersonalRecordsAndAvoidHardcodedFallbacks() {
        val set1 = SetLog(
            id = "set_real_pr",
            setNumber = 1,
            weightKg = 100.0,
            reps = 5,
            isCompleted = true,
            isPr = false,
            restTimerSeconds = null
        )
        val workout = Workout(
            id = "w_real",
            title = "Upper",
            splitType = "Upper / Lower",
            startTimeEpochMs = 1_700_000_000_000L,
            durationSeconds = 3600L,
            exercises = listOf(WorkoutExercise(id = "we_1", exercise = bench, sets = listOf(set1), restTimerSeconds = 0))
        )
        val realPr = PersonalRecord(
            exerciseId = bench.id,
            exerciseName = bench.name,
            weightKg = 100.0,
            reps = 5,
            estimated1RmKg = 116.7,
            dateAchieved = "2026-11-28",
            sourceSetId = set1.id
        )
        val dataWithoutWeight = WorkoutShareStatsBuilder.build(
            workout = workout,
            completedWorkouts = listOf(workout),
            personalRecords = listOf(realPr),
            settings = UserSettings(),
            s = EnStrings
        )
        val prCount = dataWithoutWeight.candidates.first { it.id == WorkoutShareStatId.PR_COUNT.storageKey }
        val calories = dataWithoutWeight.candidates.first { it.id == WorkoutShareStatId.EST_CALORIES.storageKey }
        val avgRest = dataWithoutWeight.candidates.first { it.id == WorkoutShareStatId.AVG_REST.storageKey }
        assertEquals("1", prCount.value)
        assertEquals("—", calories.value, "Must not fabricate 70 kg calorie estimate when bodyweight and health record are missing")
        assertEquals("—", avgRest.value, "Must not fabricate avg rest from settings default when sets lack restTimerSeconds")

        val customScales = mapOf("STAT:DURATION" to 1.45f, ShareLayoutKeys.WORKOUT_TITLE to 0.85f)
        val repo = SettingsRepository(
            initialSettings = UserSettings(
                share = ShareSettings(customItemScales = customScales)
            )
        )
        assertEquals(customScales, repo.settings.value.share.customItemScales)
    }
}
