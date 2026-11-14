package com.bearbones.kumalift.core.util

import com.bearbones.kumalift.domain.model.BodyweightVolumeMode
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.Workout
import kotlin.math.roundToInt

object VolumeCalculator {

    fun calculateTotalVolumeKg(workout: Workout): Double {
        return workout.totalVolumeKg
    }

    /**
     * Calculates volume with respect to user's calculation settings
     * (warm-up inclusion, drop set inclusion, bodyweight volume mode).
     */
    fun calculateWorkoutVolumeKg(
        workout: Workout,
        calcSettings: CalculationSettings = CalculationSettings(),
        userBodyweightKg: Double? = null
    ): Double {
        return workout.exercises.sumOf { we ->
            we.sets.filter { it.isCompleted }.sumOf { set ->
                when (set.setType) {
                    SetType.WARMUP -> if (!calcSettings.includeWarmupInVolume) 0.0 else set.volume
                    SetType.DROP -> if (!calcSettings.includeDropSetsInVolume) 0.0 else set.volume
                    SetType.FAILURE -> if (!calcSettings.includeFailureSetsInVolume) 0.0 else set.volume
                    SetType.NORMAL -> {
                        if (we.exercise.equipment == Equipment.BODYWEIGHT &&
                            calcSettings.bodyweightVolumeMode == BodyweightVolumeMode.BODYWEIGHT_PLUS_ADDED &&
                            userBodyweightKg != null
                        ) {
                            (userBodyweightKg + set.weightKg) * set.reps
                        } else {
                            set.weightKg * set.reps
                        }
                    }
                }
            }
        }
    }

    /**
     * Calculates volume breakdown by MuscleGroup in percentage.
     */
    fun calculateMuscleDistribution(workout: Workout): Map<MuscleGroup, Float> {
        val distribution = mutableMapOf<MuscleGroup, Double>()
        var grandTotal = 0.0

        for (workoutExercise in workout.exercises) {
            val volume = workoutExercise.totalVolumeKg
            if (volume > 0) {
                val primary = workoutExercise.exercise.primaryMuscle
                distribution[primary] = (distribution[primary] ?: 0.0) + volume
                grandTotal += volume
            }
        }

        if (grandTotal == 0.0) return emptyMap()

        return distribution.mapValues { (_, volume) ->
            ((volume / grandTotal) * 100.0).toFloat()
        }
    }

    fun formatWeight(weightKg: Double): String {
        return if (weightKg % 1.0 == 0.0) {
            "${weightKg.toInt()} kg"
        } else {
            "${((weightKg * 10.0).roundToInt() / 10.0)} kg"
        }
    }
}
