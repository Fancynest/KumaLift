package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.util.OneRepMaxCalculator
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
sealed class ProgressionAdvice {
    abstract val messageId: String
    abstract val messageEn: String
    abstract val suggestedWeightKg: Double?
    abstract val targetReps: Int?
    abstract val previousE1RmKg: Double?
    abstract val projectedE1RmKg: Double?

    @Serializable
    data class IncreaseWeight(
        val deltaKg: Double,
        override val suggestedWeightKg: Double,
        override val targetReps: Int,
        val details: String,
        val detailsId: String = details,
        override val previousE1RmKg: Double? = null,
        override val projectedE1RmKg: Double? = null,
        override val messageId: String = "Siap naik beban! Tambah +$deltaKg kg (target repetisi tercapai di sesi sebelumnya)",
        override val messageEn: String = "Progression ready! Add +$deltaKg kg (target reps reached in previous session)"
    ) : ProgressionAdvice()

    @Serializable
    data class IncreaseReps(
        override val targetReps: Int,
        override val suggestedWeightKg: Double,
        val details: String,
        val detailsId: String = details,
        override val previousE1RmKg: Double? = null,
        override val projectedE1RmKg: Double? = null,
        override val messageId: String = "Pertahankan beban $suggestedWeightKg kg, fokus tambah repetisi (target: $targetReps rep)",
        override val messageEn: String = "Maintain $suggestedWeightKg kg, aim for target $targetReps reps"
    ) : ProgressionAdvice()

    @Serializable
    data class MaintainOrConsolidate(
        override val suggestedWeightKg: Double,
        override val targetReps: Int? = null,
        val details: String,
        val detailsId: String = details,
        override val previousE1RmKg: Double? = null,
        override val projectedE1RmKg: Double? = null,
        override val messageId: String = "Konsolidasi beban $suggestedWeightKg kg. Prioritaskan teknik dan kontrol gerakan",
        override val messageEn: String = "Consolidate at $suggestedWeightKg kg. Prioritize form and tempo control"
    ) : ProgressionAdvice()

    @Serializable
    data class Deload(
        override val suggestedWeightKg: Double,
        override val targetReps: Int? = null,
        val details: String,
        val detailsId: String = details,
        override val previousE1RmKg: Double? = null,
        override val projectedE1RmKg: Double? = null,
        override val messageId: String = "Saran deload: kurangi beban ke $suggestedWeightKg kg untuk memulihkan kelelahan sistemik",
        override val messageEn: String = "Deload recommended: reduce load to $suggestedWeightKg kg to recover systemic fatigue"
    ) : ProgressionAdvice()

    @Serializable
    data class Baseline(
        override val suggestedWeightKg: Double,
        override val targetReps: Int? = null,
        override val previousE1RmKg: Double? = null,
        override val projectedE1RmKg: Double? = null,
        override val messageId: String = "Sesi awal. Catat repetisi dan beban untuk membangun baseline progresi",
        override val messageEn: String = "Initial session. Log sets to build your progressive overload baseline"
    ) : ProgressionAdvice()
}

object ProgressionAdvisor {

    /**
     * Analyzes performance of previous session and provides actionable progressive overload advice,
     * incorporating RPE auto-regulation and estimated 1RM projections.
     */
    fun advise(
        exercise: Exercise,
        previousSets: List<SetLog>,
        currentWeightKg: Double,
        trainingGoal: TrainingGoal = TrainingGoal.HYPERTROPHY,
        oneRmFormula: OneRepMaxFormula = OneRepMaxFormula.EPLEY
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

        // RPE tracking & auto-regulation
        val rpeLoggedSets = completedWorkingSets.mapNotNull { it.rpe }
        val avgRpe = if (rpeLoggedSets.isNotEmpty()) rpeLoggedSets.average() else null

        val maxRepsForE1Rm = maxOf(10, upperRep)

        // 1RM calculations
        val bestPreviousSet = completedWorkingSets.maxByOrNull {
            OneRepMaxCalculator.calculate(it.weightKg, it.reps, oneRmFormula, maxReps = maxRepsForE1Rm)
        }
        val previousE1Rm = if (bestPreviousSet != null && bestPreviousSet.weightKg > 0.0 && bestPreviousSet.reps > 0) {
            val raw = OneRepMaxCalculator.calculate(
                bestPreviousSet.weightKg,
                bestPreviousSet.reps,
                oneRmFormula,
                maxReps = maxRepsForE1Rm
            )
            if (raw.isFinite() && raw > 0.0) (raw * 10.0).roundToInt() / 10.0 else null
        } else null

        // Standard increments based on movement & muscle
        val baseIncrement = when {
            exercise.movementType == MovementType.COMPOUND &&
                    (exercise.primaryMuscle == MuscleGroup.LEGS || exercise.primaryMuscle == MuscleGroup.BACK) -> 5.0
            exercise.movementType == MovementType.COMPOUND -> 2.5
            else -> exercise.defaultIncrementKg.takeIf { it > 0.0 } ?: 1.25
        }

        // Case 1: Excessive fatigue / failure with high RPE -> Recommend Deload or Load Reduction
        if (avgRpe != null && avgRpe >= 9.8 && minReps < lowerRep) {
            val deloadWeight = ((lastWeight * 0.90) * 2.0).roundToInt() / 2.0
            return ProgressionAdvice.Deload(
                suggestedWeightKg = deloadWeight.coerceAtLeast(0.0),
                targetReps = lowerRep,
                details = "High fatigue (RPE ${((avgRpe * 10).roundToInt() / 10.0)}, min $minReps reps). -10% load recommended",
                detailsId = "Kelelahan tinggi (RPE ${((avgRpe * 10).roundToInt() / 10.0)}, min $minReps rep). Rekomendasi -10% beban",
                previousE1RmKg = previousE1Rm,
                projectedE1RmKg = OneRepMaxCalculator.calculateEstimated1Rm(deloadWeight, lowerRep, oneRmFormula)
            )
        }

        // Case 2: Target Rep Upper Limit Reached -> Ready for Weight Increase (Double Progression milestone!)
        if (minReps >= upperRep || avgReps >= upperRep) {
            val deltaKg = if (avgRpe != null && avgRpe <= 7.0 && exercise.movementType == MovementType.COMPOUND) {
                baseIncrement * 1.5
            } else {
                baseIncrement
            }
            val newWeight = lastWeight + deltaKg
            val projectedE1Rm = OneRepMaxCalculator.calculateEstimated1Rm(newWeight, lowerRep, oneRmFormula)

            val rpeNote = if (avgRpe != null) " (RPE ${((avgRpe * 10).roundToInt() / 10.0)})" else ""
            return ProgressionAdvice.IncreaseWeight(
                deltaKg = deltaKg,
                suggestedWeightKg = newWeight,
                targetReps = lowerRep,
                details = "Hit $minReps-$upperRep reps at $lastWeight kg$rpeNote",
                detailsId = "Tercapai $minReps-$upperRep rep pada $lastWeight kg$rpeNote",
                previousE1RmKg = previousE1Rm,
                projectedE1RmKg = projectedE1Rm
            )
        }

        // Case 3: Inside Target Rep Range -> Progress Reps
        if (minReps >= lowerRep) {
            if (avgRpe != null && avgRpe >= 9.5) {
                return ProgressionAdvice.MaintainOrConsolidate(
                    suggestedWeightKg = lastWeight,
                    targetReps = minReps,
                    details = "Working in range ($minReps reps) at near-limit RPE ${((avgRpe * 10).roundToInt() / 10.0)}. Consolidate form before adding reps",
                    detailsId = "Masuk rentang ($minReps rep) dengan RPE batas ${((avgRpe * 10).roundToInt() / 10.0)}. Konsolidasi teknik sebelum tambah rep",
                    previousE1RmKg = previousE1Rm,
                    projectedE1RmKg = previousE1Rm
                )
            }

            val nextTargetRep = (avgReps + 1.0).roundToInt().coerceIn(lowerRep, upperRep)
            val projectedRaw = OneRepMaxCalculator.calculate(
                lastWeight,
                nextTargetRep,
                oneRmFormula,
                maxReps = maxRepsForE1Rm
            )
            val projectedE1Rm = if (projectedRaw.isFinite() && projectedRaw > 0.0) {
                val rounded = (projectedRaw * 10.0).roundToInt() / 10.0
                if (previousE1Rm != null) maxOf(rounded, previousE1Rm) else rounded
            } else {
                previousE1Rm
            }
            return ProgressionAdvice.IncreaseReps(
                targetReps = nextTargetRep,
                suggestedWeightKg = lastWeight,
                details = "Averaged ${((avgReps * 10).roundToInt() / 10.0)} reps. Next step: target $nextTargetRep reps before increasing weight",
                detailsId = "Rata-rata ${((avgReps * 10).roundToInt() / 10.0)} rep. Target berikutnya: $nextTargetRep rep sebelum menaikkan beban",
                previousE1RmKg = previousE1Rm,
                projectedE1RmKg = projectedE1Rm
            )
        }

        // Case 4: Below lower rep target
        return ProgressionAdvice.MaintainOrConsolidate(
            suggestedWeightKg = lastWeight,
            targetReps = lowerRep,
            details = "Completed $minReps reps (target: $lowerRep-$upperRep). Maintain load, focus on execution",
            detailsId = "Selesai $minReps rep (target: $lowerRep-$upperRep). Pertahankan beban, fokus pada eksekusi",
            previousE1RmKg = previousE1Rm,
            projectedE1RmKg = previousE1Rm
        )
    }
}
