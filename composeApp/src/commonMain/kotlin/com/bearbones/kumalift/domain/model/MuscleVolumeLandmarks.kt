package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleVolumeStatus {
    RECOVERING,    // 0 sets this week (resting / ready for session)
    MAINTENANCE,   // 1 .. MEV-1 sets (maintaining, below progressive overload threshold)
    OPTIMAL,       // MEV .. MAV_MAX sets (optimal adaptive hypertrophy stimulus)
    OVERREACHING,  // MAV_MAX+1 .. MRV sets (high volume accumulation / functional overreaching)
    OVERTRAINED    // > MRV sets (exceeds recovery capacity, high systemic fatigue, deload advised)
}

@Serializable
data class MuscleVolumeLandmark(
    val mev: Int,      // Minimum Effective Volume (sets/week)
    val mavMin: Int,   // Minimum Adaptive Volume (sets/week)
    val mavMax: Int,   // Maximum Adaptive Volume (sets/week)
    val mrv: Int       // Maximum Recoverable Volume (sets/week)
) {
    fun evaluateStatus(weeklySets: Int): MuscleVolumeStatus {
        return when {
            weeklySets <= 0 -> MuscleVolumeStatus.RECOVERING
            weeklySets < mev -> MuscleVolumeStatus.MAINTENANCE
            weeklySets <= mavMax -> MuscleVolumeStatus.OPTIMAL
            weeklySets <= mrv -> MuscleVolumeStatus.OVERREACHING
            else -> MuscleVolumeStatus.OVERTRAINED
        }
    }
}

object DefaultVolumeLandmarks {
    val landmarks: Map<MuscleGroup, MuscleVolumeLandmark> = mapOf(
        MuscleGroup.CHEST to MuscleVolumeLandmark(mev = 8, mavMin = 12, mavMax = 20, mrv = 22),
        MuscleGroup.BACK to MuscleVolumeLandmark(mev = 10, mavMin = 14, mavMax = 22, mrv = 25),
        MuscleGroup.SHOULDERS to MuscleVolumeLandmark(mev = 8, mavMin = 12, mavMax = 20, mrv = 24),
        MuscleGroup.ARMS to MuscleVolumeLandmark(mev = 6, mavMin = 10, mavMax = 18, mrv = 20),
        MuscleGroup.LEGS to MuscleVolumeLandmark(mev = 8, mavMin = 12, mavMax = 22, mrv = 26),
        MuscleGroup.CORE to MuscleVolumeLandmark(mev = 4, mavMin = 8, mavMax = 16, mrv = 20),
        MuscleGroup.NECK to MuscleVolumeLandmark(mev = 2, mavMin = 4, mavMax = 10, mrv = 14),
        MuscleGroup.CARDIO to MuscleVolumeLandmark(mev = 3, mavMin = 5, mavMax = 10, mrv = 15),
        MuscleGroup.FULL_BODY to MuscleVolumeLandmark(mev = 12, mavMin = 18, mavMax = 28, mrv = 32)
    )

    fun forMuscle(muscle: MuscleGroup): MuscleVolumeLandmark {
        return landmarks[muscle] ?: MuscleVolumeLandmark(mev = 8, mavMin = 12, mavMax = 20, mrv = 22)
    }
}

@Serializable
data class MuscleVolumeStats(
    val muscleGroup: MuscleGroup,
    val weeklySets: Int,
    val directSets: Int,
    val indirectSets: Int,
    val totalVolumeKg: Double,
    val landmark: MuscleVolumeLandmark,
    val status: MuscleVolumeStatus,
    val topExercises: List<Pair<String, Int>> = emptyList()
)
