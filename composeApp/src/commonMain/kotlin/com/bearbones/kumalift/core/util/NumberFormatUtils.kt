package com.bearbones.kumalift.core.util

import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlin.math.pow
import kotlin.math.roundToLong

object NumberFormatUtils {

    /**
     * Formats a double with specified decimal places using locale-specific decimal separator:
     * - Indonesian: comma (',')
     * - English: dot ('.')
     */
    fun formatDecimal(
        value: Double,
        language: AppLanguage,
        decimals: Int = 1,
        trimTrailingZeros: Boolean = false
    ): String {
        if (value.isNaN() || value.isInfinite()) return "0"
        val followsEnglish = resolveIsEnglish(language)
        val factor = 10.0.pow(decimals.toDouble())
        val roundedLong = (value * factor).roundToLong()
        val absLong = kotlin.math.abs(roundedLong)
        val factorLong = factor.toLong()

        val whole = if (factorLong > 0) absLong / factorLong else absLong
        val frac = if (factorLong > 0 && decimals > 0) (absLong % factorLong).toString().padStart(decimals, '0') else ""
        val sign = if (roundedLong < 0) "-" else ""

        val raw = if (decimals == 0) {
            "$sign$whole"
        } else if (trimTrailingZeros && (frac.isEmpty() || frac.all { it == '0' })) {
            "$sign$whole"
        } else {
            "$sign$whole.$frac"
        }

        return if (followsEnglish) raw else raw.replace('.', ',')
    }

    /**
     * Formats weight with unit symbol (e.g. "62,5 kg" or "137.8 lb").
     */
    fun formatWeight(
        weightKg: Double,
        unit: WeightUnit,
        language: AppLanguage = AppLanguage.EN,
        decimals: Int = 1
    ): String {
        val display = UnitConverters.toDisplayWeight(weightKg, unit)
        val formatted = formatDecimal(display, language, decimals = decimals, trimTrailingZeros = false)
        return "$formatted ${if (unit == WeightUnit.KG) "kg" else "lb"}"
    }

    /**
     * Formats percentage (e.g. "75%" or "75,5%").
     */
    fun formatPercentage(
        value: Double,
        language: AppLanguage,
        decimals: Int = 0
    ): String {
        val formatted = formatDecimal(value, language, decimals = decimals, trimTrailingZeros = true)
        return "$formatted%"
    }

    /**
     * Formats volume in kg or lb.
     */
    fun formatVolume(
        volumeKg: Double,
        unit: WeightUnit,
        language: AppLanguage
    ): String {
        val display = UnitConverters.toDisplayWeight(volumeKg, unit)
        val formatted = formatDecimal(display, language, decimals = 1, trimTrailingZeros = true)
        return "$formatted ${if (unit == WeightUnit.KG) "kg" else "lb"}"
    }

    /**
     * Cleanly parses decimal inputs accepting both comma (',') and period ('.').
     */
    fun parseDecimalInput(input: String): Double? {
        val cleaned = input.trim().replace(',', '.')
        return cleaned.toDoubleOrNull()
    }
}
