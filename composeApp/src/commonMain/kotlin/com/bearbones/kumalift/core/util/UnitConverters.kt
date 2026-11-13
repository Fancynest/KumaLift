package com.bearbones.kumalift.core.util

import com.bearbones.kumalift.domain.model.LengthUnit
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlin.math.roundToInt

object UnitConverters {

    const val KG_TO_LB_FACTOR = 2.20462262185
    const val CM_TO_IN_FACTOR = 0.3937007874

    /**
     * Converts kilograms to pounds.
     */
    fun kgToLb(kg: Double): Double = kg * KG_TO_LB_FACTOR

    /**
     * Converts pounds to kilograms.
     */
    fun lbToKg(lb: Double): Double = lb / KG_TO_LB_FACTOR

    /**
     * Converts centimeters to inches.
     */
    fun cmToIn(cm: Double): Double = cm * CM_TO_IN_FACTOR

    /**
     * Converts inches to centimeters.
     */
    fun inToCm(inches: Double): Double = inches / CM_TO_IN_FACTOR

    /**
     * Converts centimeters to feet and inches (pair of ft, in).
     */
    fun cmToFtIn(cm: Double): Pair<Int, Int> {
        val totalInches = cmToIn(cm).roundToInt()
        val feet = totalInches / 12
        val inches = totalInches % 12
        return Pair(feet, inches)
    }

    /**
     * Converts canonical stored weight (kg) to display value in active unit.
     */
    fun toDisplayWeight(weightKg: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> weightKg
            WeightUnit.LB -> (kgToLb(weightKg) * 10.0).roundToInt() / 10.0
        }
    }

    /**
     * Converts user-entered display weight to canonical stored weight (kg).
     */
    fun toStorageWeightKg(displayWeight: Double, unit: WeightUnit): Double {
        return when (unit) {
            WeightUnit.KG -> displayWeight
            WeightUnit.LB -> lbToKg(displayWeight)
        }
    }

    /**
     * Formats weight with unit label (e.g. "100.0 kg" or "220.5 lb").
     */
    fun formatWeight(weightKg: Double, unit: WeightUnit): String {
        val display = toDisplayWeight(weightKg, unit)
        return when (unit) {
            WeightUnit.KG -> "$display kg"
            WeightUnit.LB -> "$display lb"
        }
    }
}
