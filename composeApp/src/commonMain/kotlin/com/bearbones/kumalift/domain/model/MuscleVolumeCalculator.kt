package com.bearbones.kumalift.domain.model

object MuscleVolumeCalculator {

    const val DAYS_WINDOW_MS = 7 * 86_400_000L

    fun calculateWeeklyVolume(
        completedWorkouts: List<Workout>,
        currentEpochMs: Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds(),
        windowMs: Long = DAYS_WINDOW_MS
    ): Map<MuscleGroup, MuscleVolumeStats> {
        val minTimeMs = currentEpochMs - windowMs
        val recentWorkouts = completedWorkouts.filter { (it.endTimeEpochMs ?: it.startTimeEpochMs) >= minTimeMs }

        val directSetsMap = mutableMapOf<MuscleGroup, Int>()
        val indirectSetsMap = mutableMapOf<MuscleGroup, Int>()
        val volumeKgMap = mutableMapOf<MuscleGroup, Double>()
        val exercisesMap = mutableMapOf<MuscleGroup, MutableMap<String, Int>>()

        for (workout in recentWorkouts) {
            for (we in workout.exercises) {
                val completedWorkingSets = we.sets.filter { it.isCompleted && it.setType != SetType.WARMUP }
                val count = completedWorkingSets.size
                if (count == 0) continue

                val exerciseVol = completedWorkingSets.sumOf { it.weightKg * it.reps }
                val primary = we.exercise.primaryMuscle

                // Primary muscle gets direct working set credit
                directSetsMap[primary] = (directSetsMap[primary] ?: 0) + count
                volumeKgMap[primary] = (volumeKgMap[primary] ?: 0.0) + exerciseVol
                val exMap = exercisesMap.getOrPut(primary) { mutableMapOf() }
                exMap[we.exercise.name] = (exMap[we.exercise.name] ?: 0) + count

                // Secondary muscles get indirect set stimulus (half rounded up)
                for (sec in we.exercise.secondaryMuscles) {
                    val halfCount = maxOf(1, count / 2)
                    indirectSetsMap[sec] = (indirectSetsMap[sec] ?: 0) + halfCount
                    volumeKgMap[sec] = (volumeKgMap[sec] ?: 0.0) + (exerciseVol * 0.5)
                    val secExMap = exercisesMap.getOrPut(sec) { mutableMapOf() }
                    secExMap[we.exercise.name] = (secExMap[we.exercise.name] ?: 0) + halfCount
                }
            }
        }

        return MuscleGroup.entries.associateWith { muscle ->
            val direct = directSetsMap[muscle] ?: 0
            val indirect = indirectSetsMap[muscle] ?: 0
            val totalSets = direct + indirect
            val volKg = volumeKgMap[muscle] ?: 0.0
            val landmark = DefaultVolumeLandmarks.forMuscle(muscle)
            val status = landmark.evaluateStatus(totalSets)
            val topExercises = exercisesMap[muscle]
                ?.entries
                ?.sortedByDescending { it.value }
                ?.take(4)
                ?.map { Pair(it.key, it.value) }
                ?: emptyList()

            MuscleVolumeStats(
                muscleGroup = muscle,
                weeklySets = totalSets,
                directSets = direct,
                indirectSets = indirect,
                totalVolumeKg = volKg,
                landmark = landmark,
                status = status,
                topExercises = topExercises
            )
        }
    }
}
