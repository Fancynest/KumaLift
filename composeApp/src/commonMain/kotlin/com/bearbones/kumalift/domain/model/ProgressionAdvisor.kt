package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
sealed class ProgressionAdvice {
    abstract val messageId: String
    abstract val messageEn: String
    abstract val suggestedWeightKg: Double?

    @Serializable
    data class IncreaseWeight(
        val deltaKg: Double,
        override val suggestedWeightKg: Double,
        val details: String,
        val detailsId: String = details,
        override val messageId: String = "Siap naik beban! Tambah +$deltaKg kg (target repetisi tercapai di sesi sebelumnya)",
        override val messageEn: String = "Progression ready! Add +$deltaKg kg (target reps reached in previous session)"
    ) : ProgressionAdvice()

    @Serializable
    data class IncreaseReps(
        val targetReps: Int,
        override val suggestedWeightKg: Double,
        val details: String,
        val detailsId: String = details,
        override val messageId: String = "Pertahankan beban $suggestedWeightKg kg, fokus tambah +1 repetisi",
        override val messageEn: String = "Maintain $suggestedWeightKg kg, aim for +1 rep per set"
    ) : ProgressionAdvice()

    @Serializable
    data class MaintainOrConsolidate(
        override val suggestedWeightKg: Double,
        val details: String,
        val detailsId: String = details,
        override val messageId: String = "Konsolidasi beban $suggestedWeightKg kg. Prioritaskan teknik dan kontrol gerakan",
        override val messageEn: String = "Consolidate at $suggestedWeightKg kg. Prioritize form and tempo control"
    ) : ProgressionAdvice()

    @Serializable
    data class Baseline(
        override val suggestedWeightKg: Double,
        override val messageId: String = "Sesi awal. Catat repetisi dan beban untuk membangun baseline progresi",
        override val messageEn: String = "Initial session. Log sets to build your progressive overload baseline"
    ) : ProgressionAdvice()
}

object ProgressionAdvisor {

    /**
     * Analyzes performance of previous session and provides actionable progressive overload advice.
     */
    fun advise(
        exercise: Exercise,
        previousSets: List<SetLog>,
        currentWeightKg: Double,
        trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY
    ): ProgressionAdvice {
        val completedWorkingSets = previousSets.filter { it.isCompleted && it.setType != SetType.WARMUP }
        if (completedWorkingSets.isEmpty()) {
            return ProgressionAdvice.Baseline(suggestedWeightKg = currentWeightKg.coerceAtLeast(20.0))
        }

        val repRange = when (trainingGoal) {
            TrainingGoal.STRENGTH -> if (exercise.movementType == MovementType.COMPOUND) 3..5 else 6..8
            TrainingGoal.HYPERTROPHY -> if (exercise.movementType == MovementType.COMPOUND) 6..10 else 8..12
            TrainingGoal.ENDURANCE -> if (exercise.movementType == MovementType.COMPOUND) 12..15 else 15..20
            TrainingGoal.FAT_LOSS, TrainingGoal.GENERAL -> if (exercise.movementType == MovementType.COMPOUND) 8..12 else 10..15
        }
        val lowerRep = repRange.first
        val upperRep = repRange.last

        val minReps = completedWorkingSets.minOf { it.reps }
        val avgReps = completedWorkingSets.map { it.reps }.average()
        val lastWeight = completedWorkingSets.firstOrNull { it.weightKg > 0.0 }?.weightKg ?: currentWeightKg

        return when {
            // Reached or surpassed upper target rep limit across all working sets
            minReps >= upperRep || avgReps >= upperRep -> {
                val deltaKg = when {
                    exercise.movementType == MovementType.COMPOUND &&
                            (exercise.primaryMuscle == MuscleGroup.LEGS || exercise.primaryMuscle == MuscleGroup.BACK) -> 5.0
                    exercise.movementType == MovementType.COMPOUND -> 2.5
                    else -> exercise.defaultIncrementKg.takeIf { it > 0.0 } ?: 1.25
                }
                val newWeight = lastWeight + deltaKg
                ProgressionAdvice.IncreaseWeight(
                    deltaKg = deltaKg,
                    suggestedWeightKg = newWeight,
                    details = "Hit $minReps-$upperRep reps at $lastWeight kg",
                    detailsId = "Tercapai $minReps-$upperRep rep pada $lastWeight kg"
                )
            }
            // Working comfortably inside target rep range
            minReps >= lowerRep -> {
                val nextTargetRep = (avgReps + 1.0).toInt().coerceAtMost(upperRep)
                ProgressionAdvice.IncreaseReps(
                    targetReps = nextTargetRep,
                    suggestedWeightKg = lastWeight,
                    details = "Averaged ${avgReps.toInt()} reps. Next step: $nextTargetRep reps before increasing weight",
                    detailsId = "Rata-rata ${avgReps.toInt()} rep. Target berikutnya: $nextTargetRep rep sebelum menaikkan beban"
                )
            }
            // Below rep target
            else -> {
                ProgressionAdvice.MaintainOrConsolidate(
                    suggestedWeightKg = lastWeight,
                    details = "Completed $minReps reps (target: $lowerRep-$upperRep). Maintain load and recover",
                    detailsId = "Selesai $minReps rep (target: $lowerRep-$upperRep). Pertahankan beban dan pulihkan tenaga"
                )
            }
        }
    }
}
