package com.bearbones.kumalift.core.util

import kotlin.math.roundToInt

object OneRepMaxCalculator {

    /**
     * Epley Formula: 1RM = Weight * (1 + Reps / 30.0)
     */
    fun calculateEpley(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + (reps / 30.0))
    }

    /**
     * Brzycki Formula: 1RM = Weight / (1.0278 - 0.0278 * Reps)
     */
    fun calculateBrzycki(weightKg: Double, reps: Int): Double {
        if (weightKg <= 0.0 || reps <= 0) return 0.0
        if (reps == 1) return weightKg
        if (reps >= 37) return calculateEpley(weightKg, reps)
        return weightKg / (1.0278 - (0.0278 * reps))
    }

    /**
     * Standard blended/rounded 1RM for telemetry display.
     */
    fun calculateEstimated1Rm(weightKg: Double, reps: Int): Double {
        val epley = calculateEpley(weightKg, reps)
        return (epley * 10.0).roundToInt() / 10.0
    }
}
