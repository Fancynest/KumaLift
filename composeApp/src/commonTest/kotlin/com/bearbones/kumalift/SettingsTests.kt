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
        assertEquals(AppTheme.DARK, settings.appearance.theme)
        assertFalse(settings.dataBackup.autoBackupEnabled)
        assertFalse(settings.health.healthEnabled)
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
        assertEquals(AppTheme.DARK, repo.settings.value.appearance.theme)
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
}
