package com.bearbones.kumalift.core.localization

import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.formatEpochToDateString
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.WeightUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NumberAndDateLocalizationTests {

    @Test
    fun testIndonesianDecimalCommaAndEnglishDecimalDot() {
        val value = 62.5

        // Indonesian uses comma
        val idFormatted = NumberFormatUtils.formatDecimal(value, AppLanguage.ID, decimals = 1)
        assertEquals("62,5", idFormatted)

        // English uses dot
        val enFormatted = NumberFormatUtils.formatDecimal(value, AppLanguage.EN, decimals = 1)
        assertEquals("62.5", enFormatted)
    }

    @Test
    fun testWeightFormattingPerLanguage() {
        val weight = 72.5

        val idWeight = NumberFormatUtils.formatWeight(weight, WeightUnit.KG, AppLanguage.ID)
        assertEquals("72,5 kg", idWeight)

        val enWeight = NumberFormatUtils.formatWeight(weight, WeightUnit.KG, AppLanguage.EN)
        assertEquals("72.5 kg", enWeight)

        val enLb = NumberFormatUtils.formatWeight(100.0, WeightUnit.LB, AppLanguage.EN)
        assertEquals("220.5 lb", enLb)
    }

    @Test
    fun testDecimalInputParsingAcceptsBothCommaAndDot() {
        val parsedDot = NumberFormatUtils.parseDecimalInput("62.5")
        assertEquals(62.5, parsedDot!!, 0.0001)

        val parsedComma = NumberFormatUtils.parseDecimalInput("62,5")
        assertEquals(62.5, parsedComma!!, 0.0001)

        val parsedWithSpaces = NumberFormatUtils.parseDecimalInput("  80,25  ")
        assertEquals(80.25, parsedWithSpaces!!, 0.0001)

        val invalid = NumberFormatUtils.parseDecimalInput("abc")
        assertNull(invalid)
    }

    @Test
    fun testDateFormattingPerLanguage() {
        // Fixed epoch: 1700000000000L = 2023-11-14T22:13:20Z (approx)
        val epoch = 1700000000000L

        val idDate = formatEpochToDateString(epoch, AppLanguage.ID)
        // Indonesian uses dd/MM/yyyy
        assertTrue("Indonesian date should contain '/' separators: $idDate", idDate.contains("/"))

        val enDate = formatEpochToDateString(epoch, AppLanguage.EN)
        // English uses yyyy-MM-dd
        assertTrue("English date should contain '-' separators: $enDate", enDate.contains("-"))
    }
}
