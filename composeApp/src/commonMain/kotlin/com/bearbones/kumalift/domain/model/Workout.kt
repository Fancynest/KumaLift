package com.bearbones.kumalift.domain.model

data class Workout(
    val id: String,
    val title: String,
    val splitType: String = "Freestyle",
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long? = null,
    val durationSeconds: Long = 0,
    val exercises: List<WorkoutExercise> = emptyList(),
    val sessionRpe: Double? = null,
    val notes: String = "",
    val isFinished: Boolean = false,
    val avgHeartRateBpm: Int? = null,
    val peakHeartRateBpm: Int? = null,
    val activeCaloriesKcal: Double? = null
) {
    val totalVolumeKg: Double
        get() = exercises.sumOf { it.totalVolumeKg }

    val totalCompletedSets: Int
        get() = exercises.sumOf { it.completedSetsCount }

    val totalSets: Int
        get() = exercises.sumOf { it.totalSetsCount }

    val progressFraction: Float
        get() = if (totalSets > 0) totalCompletedSets.toFloat() / totalSets else 0f
}
