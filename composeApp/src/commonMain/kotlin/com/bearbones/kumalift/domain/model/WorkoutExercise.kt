package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class WorkoutExercise(
    val id: String,
    val exercise: Exercise,
    val sets: List<SetLog> = emptyList(),
    val notes: String = "",
    val restTimerSeconds: Int = 90,
    val supersetGroupId: String? = null
) {
    val completedSetsCount: Int
        get() = sets.count { it.isCompleted }

    val totalSetsCount: Int
        get() = sets.size

    val totalVolumeKg: Double
        get() = sets.filter { it.isCompleted && it.setType != SetType.WARMUP }.sumOf { it.volume }

    val topSet: SetLog?
        get() = sets.filter { it.isCompleted && it.setType != SetType.WARMUP }.maxByOrNull { it.weightKg }
}
