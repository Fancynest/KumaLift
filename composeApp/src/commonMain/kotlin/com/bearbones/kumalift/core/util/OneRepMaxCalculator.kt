package com.bearbones.kumalift.core.util

import com.bearbones.kumalift.domain.model.OneRepMaxFormula
import kotlin.math.pow
import kotlin.math.roundToInt

object OneRepMaxCalculator {

    /**
     * Epley Formula: 1RM = w × (1 + r / 30)
     */
    fun calculateEpley(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    /**
     * Brzycki Formula: 1RM = w × 36 / (37 − r)
     */
    fun calculateBrzycki(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        if (reps >= 37) return calculateEpley(weightKg, reps)
        return weightKg * 36.0 / (37.0 - reps)
    }

    /**
     * Lombardi Formula: 1RM = w × r^0.10
     */
    fun calculateLombardi(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * reps.toDouble().pow(0.10)
    }

    /**
     * O'Conner Formula: 1RM = w × (1 + r / 40)
     */
    fun calculateOConner(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 40.0)
    }

    /**
     * Unified calculator parameterized by formula and max reps threshold.
     */
    fun calculate(
        weightKg: Double,
        reps: Int,
        formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY,
        maxReps: Int = 10
    ): Double {
        if (reps > maxReps || reps <= 0 || weightKg <= 0.0) return 0.0
        return when (formula) {
            OneRepMaxFormula.EPLEY -> calculateEpley(weightKg, reps)
            OneRepMaxFormula.BRZYCKI -> calculateBrzycki(weightKg, reps)
            OneRepMaxFormula.LOMBARDI -> calculateLombardi(weightKg, reps)
            OneRepMaxFormula.OCONNER -> calculateOConner(weightKg, reps)
        }
    }

    /**
     * Standard rounded 1RM for telemetry display (1 decimal place).
     */
    fun calculateEstimated1Rm(
        weightKg: Double,
        reps: Int,
        formula: OneRepMaxFormula = OneRepMaxFormula.EPLEY
    ): Double {
        val raw = calculate(weightKg, reps, formula)
        return (raw * 10.0).roundToInt() / 10.0
    }
}
