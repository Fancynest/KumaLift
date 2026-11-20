package com.bearbones.kumalift.domain.model

object SmartExerciseAlternativeAdvisor {

    data class ScoredAlternative(
        val exercise: Exercise,
        val score: Int,
        val matchReasonCode: String // "same_pattern", "same_muscle", "secondary_support"
    )

    /**
     * Finds biomechanically viable alternatives for [targetExercise] from [allExercises],
     * optionally filtered by desired [equipmentFilter].
     */
    fun findAlternatives(
        targetExercise: Exercise,
        allExercises: List<Exercise>,
        equipmentFilter: Equipment? = null,
        limit: Int = 10
    ): List<ScoredAlternative> {
        val candidates = allExercises.filter { it.id != targetExercise.id }

        return candidates
            .filter { candidate ->
                if (equipmentFilter != null) candidate.equipment == equipmentFilter else true
            }
            .map { candidate ->
                var score = 0
                var reason = "secondary_support"

                // 1. Primary Muscle match (+50)
                if (candidate.primaryMuscle == targetExercise.primaryMuscle) {
                    score += 50
                    reason = "same_muscle"

                    // Sub-muscle match (+20)
                    if (targetExercise.subMuscle.isNotBlank() &&
                        candidate.subMuscle.equals(targetExercise.subMuscle, ignoreCase = true)
                    ) {
                        score += 20
                    }

                    // 2. Movement Type match (Compound vs Isolation: +25)
                    if (candidate.movementType == targetExercise.movementType) {
                        score += 25
                        reason = "same_pattern"
                    }
                } else if (candidate.secondaryMuscles.contains(targetExercise.primaryMuscle)) {
                    score += 25
                } else if (targetExercise.secondaryMuscles.contains(candidate.primaryMuscle)) {
                    score += 20
                }

                // Equipment variation bonus (+5)
                if (candidate.equipment != targetExercise.equipment) {
                    score += 5
                }

                ScoredAlternative(candidate, score, reason)
            }
            .filter { it.score >= 20 }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
