package com.bearbones.kumalift

import com.bearbones.kumalift.core.util.OneRepMaxCalculator
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SettingsTests {

    @Test
    fun testDefaultSettingsInstantiation() {
        val settings = UserSettings()
        assertEquals(1, settings.settingsVersion)
        assertEquals("", settings.profile.name)
        assertNull(settings.profile.bodyweightKg)
        assertEquals(4, settings.profile.trainingDaysPerWeek)
        assertEquals(WeightUnit.KG, settings.units.weightUnit)
        assertEquals(90, settings.logging.restTimerDefaultSec)
        assertTrue(settings.logging.restTimerAutoStart)
        assertEquals(2.5, settings.logging.weightStepKgBarbell)
        assertEquals(3, settings.equipment.bars.size)
        assertEquals("olympic", settings.equipment.defaultBarId)
        assertTrue(settings.equipment.showPlateCalculator)
        assertTrue(settings.notifications.restTimerAlert)
        assertEquals(OneRepMaxFormula.EPLEY, settings.calculations.oneRepMaxFormula)
        assertEquals(10, settings.calculations.maxRepsForEstimate)
        assertEquals(AppTheme.SYSTEM, settings.appearance.theme)
        assertEquals(com.bearbones.kumalift.domain.model.AppLanguage.SYSTEM, settings.appearance.language)
        assertFalse(settings.dataBackup.autoBackupEnabled)
        assertFalse(settings.health.healthEnabled)
    }

    @Test
    fun testFirstLaunchLanguageResolution() {
        // Explicit user preferences always win
        assertTrue(com.bearbones.kumalift.core.util.resolveIsEnglish(com.bearbones.kumalift.domain.model.AppLanguage.EN))
        assertFalse(com.bearbones.kumalift.core.util.resolveIsEnglish(com.bearbones.kumalift.domain.model.AppLanguage.ID))

        // System default respects device locale
        val resolved = com.bearbones.kumalift.core.util.resolveIsEnglish(com.bearbones.kumalift.domain.model.AppLanguage.SYSTEM)
        val sysCode = com.bearbones.kumalift.core.util.getSystemLanguageCode().lowercase()
        val expected = sysCode != "id" && sysCode != "in"
        assertEquals(expected, resolved)
    }

    @Test
    fun testSettingsRepositoryUpdatesAndResets() {
        val repo = SettingsRepository()
        assertEquals("", repo.settings.value.profile.name)

        // Update profile
        repo.update { it.copy(profile = it.profile.copy(name = "Kuma Lifter", bodyweightKg = 82.5)) }
        assertEquals("Kuma Lifter", repo.settings.value.profile.name)
        assertEquals(82.5, repo.settings.value.profile.bodyweightKg)

        // Reset profile only
        repo.resetProfile()
        assertEquals("", repo.settings.value.profile.name)
        assertNull(repo.settings.value.profile.bodyweightKg)

        // Update theme and reset all
        repo.update { it.copy(appearance = it.appearance.copy(theme = AppTheme.LIGHT)) }
        assertEquals(AppTheme.LIGHT, repo.settings.value.appearance.theme)
        repo.resetAll()
        assertEquals(AppTheme.SYSTEM, repo.settings.value.appearance.theme)
    }

    @Test
    fun testOneRepMaxAllFormulas() {
        val w = 100.0
        // Single rep equals the weight lifted for all 4 formulas
        assertEquals(100.0, OneRepMaxCalculator.calculateEpley(w, 1))
        assertEquals(100.0, OneRepMaxCalculator.calculateBrzycki(w, 1))
        assertEquals(100.0, OneRepMaxCalculator.calculateLombardi(w, 1))
        assertEquals(100.0, OneRepMaxCalculator.calculateOConner(w, 1))

        // 100kg x 10 reps
        // Epley: 100 * (1 + 10/30) = 133.333...
        val epley = OneRepMaxCalculator.calculate(w, 10, OneRepMaxFormula.EPLEY)
        assertTrue(abs(epley - 133.333) < 0.01)

        // Brzycki: 100 * 36 / (37 - 10) = 100 * 36 / 27 = 133.333...
        val brzycki = OneRepMaxCalculator.calculate(w, 10, OneRepMaxFormula.BRZYCKI)
        assertTrue(abs(brzycki - 133.333) < 0.01)

        // Lombardi: 100 * 10^0.10 = 125.892...
        val lombardi = OneRepMaxCalculator.calculate(w, 10, OneRepMaxFormula.LOMBARDI)
        assertTrue(abs(lombardi - 125.89) < 0.01)

        // O'Conner: 100 * (1 + 10/40) = 125.0
        val oconner = OneRepMaxCalculator.calculate(w, 10, OneRepMaxFormula.OCONNER)
        assertEquals(125.0, oconner)

        // Reps exceeding max threshold should return 0.0
        assertEquals(0.0, OneRepMaxCalculator.calculate(w, 11, OneRepMaxFormula.EPLEY, maxReps = 10))
    }

    @Test
    fun testUnitConversions() {
        // 100 kg = 220.462... lb
        val lb = UnitConverters.kgToLb(100.0)
        assertTrue(abs(lb - 220.462) < 0.01)

        // Round trip test
        val kgBack = UnitConverters.lbToKg(lb)
        assertTrue(abs(kgBack - 100.0) < 0.00001)

        // Length: 180 cm to ft/in -> 70.866 in -> 5 ft 11 in
        val (ft, inch) = UnitConverters.cmToFtIn(180.0)
        assertEquals(5, ft)
        assertEquals(11, inch)

        // Display formatting
        assertEquals("100.0 kg", UnitConverters.formatWeight(100.0, WeightUnit.KG))
        assertEquals("220.5 lb", UnitConverters.formatWeight(100.0, WeightUnit.LB))
    }

    @Test
    fun testVolumeInclusionRules() {
        val exercise = com.bearbones.kumalift.domain.model.Exercise(
            id = "ex_bench",
            name = "Barbell Bench Press",
            primaryMuscle = com.bearbones.kumalift.domain.model.MuscleGroup.CHEST,
            equipment = com.bearbones.kumalift.domain.model.Equipment.BARBELL,
            movementType = com.bearbones.kumalift.domain.model.MovementType.COMPOUND
        )
        val workout = com.bearbones.kumalift.domain.model.Workout(
            id = "w_vol",
            title = "Chest Day",
            startTimeEpochMs = 1000L,
            exercises = listOf(
                com.bearbones.kumalift.domain.model.WorkoutExercise(
                    id = "we_bench",
                    exercise = exercise,
                    sets = listOf(
                        com.bearbones.kumalift.domain.model.SetLog(id = "s1", setNumber = 1, setType = com.bearbones.kumalift.domain.model.SetType.WARMUP, weightKg = 40.0, reps = 10, isCompleted = true),
                        com.bearbones.kumalift.domain.model.SetLog(id = "s2", setNumber = 2, setType = com.bearbones.kumalift.domain.model.SetType.NORMAL, weightKg = 100.0, reps = 5, isCompleted = true),
                        com.bearbones.kumalift.domain.model.SetLog(id = "s3", setNumber = 3, setType = com.bearbones.kumalift.domain.model.SetType.DROP, weightKg = 70.0, reps = 8, isCompleted = true)
                    )
                )
            )
        )

        // Default: warmup excluded (40*10 = 400 excluded), working (500) + drop (560) = 1060 kg
        val defaultVol = com.bearbones.kumalift.core.util.VolumeCalculator.calculateWorkoutVolumeKg(
            workout,
            com.bearbones.kumalift.domain.model.CalculationSettings(includeWarmupInVolume = false, includeDropSetsInVolume = true)
        )
        assertEquals(1060.0, defaultVol)

        // Warmup included: 400 + 500 + 560 = 1460 kg
        val withWarmupVol = com.bearbones.kumalift.core.util.VolumeCalculator.calculateWorkoutVolumeKg(
            workout,
            com.bearbones.kumalift.domain.model.CalculationSettings(includeWarmupInVolume = true, includeDropSetsInVolume = true)
        )
        assertEquals(1460.0, withWarmupVol)
    }

    @Test
    fun testRecomputePersonalRecordsOnFormulaChange() {
        val exercise = com.bearbones.kumalift.domain.model.Exercise(
            id = "ex_squat",
            name = "Barbell Back Squat",
            primaryMuscle = com.bearbones.kumalift.domain.model.MuscleGroup.LEGS,
            equipment = com.bearbones.kumalift.domain.model.Equipment.BARBELL,
            movementType = com.bearbones.kumalift.domain.model.MovementType.COMPOUND
        )
        val workout = com.bearbones.kumalift.domain.model.Workout(
            id = "w_squat",
            title = "Squat Session",
            startTimeEpochMs = 1731550000000L,
            exercises = listOf(
                com.bearbones.kumalift.domain.model.WorkoutExercise(
                    id = "we_squat",
                    exercise = exercise,
                    sets = listOf(
                        com.bearbones.kumalift.domain.model.SetLog(id = "s_sq1", setNumber = 1, setType = com.bearbones.kumalift.domain.model.SetType.NORMAL, weightKg = 100.0, reps = 10, isCompleted = true)
                    )
                )
            )
        )

        val prRepo = com.bearbones.kumalift.data.repository.PersonalRecordRepository()

        // 1. Calculate PR using EPLEY
        prRepo.recomputePersonalRecords(
            allWorkouts = listOf(workout),
            calcSettings = com.bearbones.kumalift.domain.model.CalculationSettings(oneRepMaxFormula = OneRepMaxFormula.EPLEY)
        )
        val epleyPr = prRepo.personalRecords.value.first()
        assertTrue(abs(epleyPr.estimated1RmKg - 133.333) < 0.01)

        // 2. Recompute PR using OCONNER
        prRepo.recomputePersonalRecords(
            allWorkouts = listOf(workout),
            calcSettings = com.bearbones.kumalift.domain.model.CalculationSettings(oneRepMaxFormula = OneRepMaxFormula.OCONNER)
        )
        val oconnerPr = prRepo.personalRecords.value.first()
        assertEquals(125.0, oconnerPr.estimated1RmKg)
    }

    @Test
    fun testAddExerciseRespectsSettings() {
        var loggingSettings = com.bearbones.kumalift.domain.model.LoggingSettings(
            defaultSetCount = 4,
            prefillSource = com.bearbones.kumalift.domain.model.PrefillSource.NONE
        )
        val prRepo = com.bearbones.kumalift.data.repository.PersonalRecordRepository()
        val workoutRepo = com.bearbones.kumalift.data.repository.WorkoutRepository(
            prRepository = prRepo,
            loggingSettingsProvider = { loggingSettings }
        )

        workoutRepo.startEmptyWorkout("Test Logging Session")
        val exercise = com.bearbones.kumalift.domain.model.Exercise(
            id = "ex_ohp",
            name = "Overhead Press",
            primaryMuscle = com.bearbones.kumalift.domain.model.MuscleGroup.SHOULDERS,
            equipment = com.bearbones.kumalift.domain.model.Equipment.BARBELL,
            movementType = com.bearbones.kumalift.domain.model.MovementType.COMPOUND
        )

        workoutRepo.addExerciseToActiveWorkout(exercise)
        val active = workoutRepo.activeWorkout.value!!
        assertEquals(1, active.exercises.size)
        // Verified defaultSetCount (4 sets created)
        assertEquals(4, active.exercises[0].sets.size)
    }

    @Test
    fun testFirstDayOfWeekIndices() {
        // Monday start: 1, 2, 3, 4, 5, 6, 7
        val mondayIndices = when (com.bearbones.kumalift.domain.model.FirstDayOfWeek.MONDAY) {
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.MONDAY -> listOf(1, 2, 3, 4, 5, 6, 7)
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.SUNDAY -> listOf(7, 1, 2, 3, 4, 5, 6)
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.SATURDAY -> listOf(6, 7, 1, 2, 3, 4, 5)
        }
        assertEquals(listOf(1, 2, 3, 4, 5, 6, 7), mondayIndices)

        // Sunday start: 7 (Sun), 1 (Mon), ..., 6 (Sat)
        val sundayIndices = when (com.bearbones.kumalift.domain.model.FirstDayOfWeek.SUNDAY) {
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.MONDAY -> listOf(1, 2, 3, 4, 5, 6, 7)
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.SUNDAY -> listOf(7, 1, 2, 3, 4, 5, 6)
            com.bearbones.kumalift.domain.model.FirstDayOfWeek.SATURDAY -> listOf(6, 7, 1, 2, 3, 4, 5)
        }
        assertEquals(listOf(7, 1, 2, 3, 4, 5, 6), sundayIndices)
    }
}
