package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.util.IdGenerator
import kotlinx.serialization.Serializable
import kotlin.math.round

@Serializable
data class WarmupSetTemplate(
    val percentageOfWorkingWeight: Double,
    val targetWeightKg: Double,
    val targetReps: Int
)

object WarmupSetCalculator {

    /**
     * Rounds a weight to the nearest step (default 2.5 kg).
     */
    fun roundToStep(weight: Double, step: Double = 2.5): Double {
        if (step <= 0.0) return weight
        return round(weight / step) * step
    }

    /**
     * Computes progressive warm-up sets for a given [workingWeightKg].
     * Returns a list of [SetLog] ready to be prepended to the workout exercise.
     */
    fun generateWarmupSets(
        workingWeightKg: Double,
        barWeightKg: Double = 20.0,
        weightStepKg: Double = 2.5
    ): List<SetLog> {
        if (workingWeightKg <= barWeightKg) {
            return emptyList()
        }

        val templates = mutableListOf<WarmupSetTemplate>()

        when {
            workingWeightKg <= 40.0 -> {
                // Light compound: single bar warmup
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = barWeightKg / workingWeightKg,
                        targetWeightKg = barWeightKg,
                        targetReps = 8
                    )
                )
            }
            workingWeightKg <= 70.0 -> {
                // Moderate weight: 2 sets
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = barWeightKg / workingWeightKg,
                        targetWeightKg = barWeightKg,
                        targetReps = 10
                    )
                )
                val set2Weight = roundToStep(workingWeightKg * 0.60, weightStepKg).coerceAtLeast(barWeightKg)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.60,
                        targetWeightKg = set2Weight,
                        targetReps = 5
                    )
                )
            }
            workingWeightKg <= 110.0 -> {
                // Heavier weight: 3 sets
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = barWeightKg / workingWeightKg,
                        targetWeightKg = barWeightKg,
                        targetReps = 10
                    )
                )
                val set2Weight = roundToStep(workingWeightKg * 0.50, weightStepKg).coerceAtLeast(barWeightKg)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.50,
                        targetWeightKg = set2Weight,
                        targetReps = 5
                    )
                )
                val set3Weight = roundToStep(workingWeightKg * 0.75, weightStepKg).coerceAtLeast(set2Weight)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.75,
                        targetWeightKg = set3Weight,
                        targetReps = 3
                    )
                )
            }
            else -> {
                // Heavy lifts: 4 sets
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = barWeightKg / workingWeightKg,
                        targetWeightKg = barWeightKg,
                        targetReps = 10
                    )
                )
                val set2Weight = roundToStep(workingWeightKg * 0.50, weightStepKg).coerceAtLeast(barWeightKg)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.50,
                        targetWeightKg = set2Weight,
                        targetReps = 5
                    )
                )
                val set3Weight = roundToStep(workingWeightKg * 0.70, weightStepKg).coerceAtLeast(set2Weight)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.70,
                        targetWeightKg = set3Weight,
                        targetReps = 3
                    )
                )
                val set4Weight = roundToStep(workingWeightKg * 0.85, weightStepKg).coerceAtLeast(set3Weight)
                templates.add(
                    WarmupSetTemplate(
                        percentageOfWorkingWeight = 0.85,
                        targetWeightKg = set4Weight,
                        targetReps = 1
                    )
                )
            }
        }

        return templates.mapIndexed { index, t ->
            SetLog(
                id = IdGenerator.nextId("wu"),
                setNumber = index + 1,
                setType = SetType.WARMUP,
                weightKg = t.targetWeightKg,
                reps = t.targetReps,
                rpe = null,
                isCompleted = false,
                targetWeightKg = t.targetWeightKg,
                targetReps = t.targetReps
            )
        }
    }
}
