package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleRecoveryStatus {
    FRESH,       // >72h or 0 recent volume
    RECOVERING,  // 24h - 48h, moderate volume
    FATIGUED     // <24h or high recent volume
}

@Serializable
data class MuscleRecoveryState(
    val muscleGroup: MuscleGroup,
    val readinessPercentage: Int, // 0 - 100%
    val status: MuscleRecoveryStatus,
    val setsLast72h: Int,
    val hoursSinceLastTrained: Long?
)

object MuscleRecoveryTracker {

    const val RECOVERY_WINDOW_HOURS = 72L

    /**
     * Calculates the estimated muscle readiness for all muscle groups based on [completedWorkouts].
     */
    fun calculateRecovery(
        completedWorkouts: List<Workout>,
        currentEpochMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    ): List<MuscleRecoveryState> {
        val windowMs = RECOVERY_WINDOW_HOURS * 3600_000L
        val minTimeMs = currentEpochMs - windowMs

        val recentWorkouts = completedWorkouts.filter { (it.endTimeEpochMs ?: it.startTimeEpochMs) >= minTimeMs }

        // Map of MuscleGroup -> (recentSetsCount, mostRecentTimestamp)
        val muscleStats = mutableMapOf<MuscleGroup, Pair<Int, Long>>()

        for (workout in recentWorkouts) {
            val workoutTime = workout.endTimeEpochMs ?: workout.startTimeEpochMs
            for (exerciseLog in workout.exercises) {
                val workingSetsCount = exerciseLog.sets.count { it.isCompleted && it.setType != SetType.WARMUP }
                if (workingSetsCount == 0) continue

                // Primary muscle gets 100% set count
                val primary = exerciseLog.exercise.primaryMuscle
                val currentPrimary = muscleStats[primary] ?: (0 to 0L)
                muscleStats[primary] = Pair(
                    currentPrimary.first + workingSetsCount,
                    maxOf(currentPrimary.second, workoutTime)
                )

                // Secondary muscles get 50% set stimulus
                for (secondary in exerciseLog.exercise.secondaryMuscles) {
                    val currentSecondary = muscleStats[secondary] ?: (0 to 0L)
                    muscleStats[secondary] = Pair(
                        currentSecondary.first + (workingSetsCount / 2).coerceAtLeast(1),
                        maxOf(currentSecondary.second, workoutTime)
                    )
                }
            }
        }

        return MuscleGroup.entries.map { muscle ->
            val stat = muscleStats[muscle]
            if (stat == null || stat.first == 0) {
                // No stimulus in last 72 hours -> Fresh
                MuscleRecoveryState(
                    muscleGroup = muscle,
                    readinessPercentage = 100,
                    status = MuscleRecoveryStatus.FRESH,
                    setsLast72h = 0,
                    hoursSinceLastTrained = null
                )
            } else {
                val sets = stat.first
                val lastTrainedMs = stat.second
                val hoursElapsed = ((currentEpochMs - lastTrainedMs) / 3600_000L).coerceAtLeast(0L)

                // Progressive recovery estimation based on elapsed hours and accumulated volume
                // Baseline: 0h = ~30%, 24h = ~60%, 48h = ~85%, 72h = 100%
                // Heavy volume (>12 sets) slows recovery down slightly
                val volumePenalty = (sets / 6).coerceAtMost(3) * 5 // up to -15%
                val timeFraction = (hoursElapsed.toFloat() / RECOVERY_WINDOW_HOURS.toFloat()).coerceIn(0f, 1f)
                val baseReadiness = (35f + 65f * timeFraction).toInt()
                val netReadiness = (baseReadiness - volumePenalty).coerceIn(25, 100)

                val status = when {
                    netReadiness >= 85 -> MuscleRecoveryStatus.FRESH
                    netReadiness >= 60 -> MuscleRecoveryStatus.RECOVERING
                    else -> MuscleRecoveryStatus.FATIGUED
                }

                MuscleRecoveryState(
                    muscleGroup = muscle,
                    readinessPercentage = netReadiness,
                    status = status,
                    setsLast72h = sets,
                    hoursSinceLastTrained = hoursElapsed
                )
            }
        }.sortedWith(compareBy({ it.readinessPercentage }, { it.muscleGroup.name }))
    }
}
