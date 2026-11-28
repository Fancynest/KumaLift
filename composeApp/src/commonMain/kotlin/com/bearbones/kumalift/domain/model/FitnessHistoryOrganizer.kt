package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.localization.AppStrings
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToLong

data class PrWeightGroup(
    val weightKg: Double,
    val records: List<PersonalRecord>
)

data class PrExerciseGroup(
    val exerciseId: String,
    val exerciseName: String,
    val bestOneRmKg: Double,
    val bestWeightKg: Double,
    val totalAchievements: Int,
    val latestEpochMs: Long,
    val weightGroups: List<PrWeightGroup>
)

data class MonthlyWorkoutGroup(
    val year: Int,
    val monthNumber: Int,
    val totalVolumeKg: Double,
    val totalDurationSeconds: Long,
    val workouts: List<Workout>
) {
    val key: String get() = "${year}_${monthNumber.toString().padStart(2, '0')}"
}

/**
 * Pure presentation organizer for PR records & workout history.
 * Never mutates, deletes, or merges underlying PR/workout entities.
 */
object FitnessHistoryOrganizer {

    fun organizePersonalRecords(prs: List<PersonalRecord>): List<PrExerciseGroup> {
        if (prs.isEmpty()) return emptyList()
        return prs
            .groupBy { it.exerciseId.ifBlank { it.exerciseName } }
            .values
            .map { exercisePrs ->
                val newest = exercisePrs.maxWithOrNull(
                    compareBy<PersonalRecord> { it.dateAchievedEpochMs }
                        .thenBy { if (it.estimated1RmKg > 0.0) it.estimated1RmKg else it.weightKg }
                ) ?: exercisePrs.first()

                val weightGroups = exercisePrs
                    .groupBy { ((it.weightKg * 100.0).roundToLong() / 100.0) }
                    .entries
                    .sortedByDescending { it.key }
                    .map { (bucketWeightKg, bucketRecords) ->
                        val chronological = bucketRecords.sortedWith(
                            compareByDescending<PersonalRecord> { it.dateAchievedEpochMs }
                                .thenByDescending { it.reps }
                        )
                        PrWeightGroup(
                            weightKg = bucketWeightKg,
                            records = chronological
                        )
                    }

                PrExerciseGroup(
                    exerciseId = newest.exerciseId,
                    exerciseName = newest.exerciseName,
                    bestOneRmKg = exercisePrs.maxOfOrNull {
                        if (it.estimated1RmKg > 0.0) it.estimated1RmKg else it.weightKg
                    } ?: 0.0,
                    bestWeightKg = exercisePrs.maxOfOrNull { it.weightKg } ?: 0.0,
                    totalAchievements = exercisePrs.size,
                    latestEpochMs = exercisePrs.maxOfOrNull { it.dateAchievedEpochMs } ?: 0L,
                    weightGroups = weightGroups
                )
            }
            .sortedWith(
                compareByDescending<PrExerciseGroup> { it.latestEpochMs }
                    .thenByDescending { it.bestOneRmKg }
                    .thenByDescending { it.bestWeightKg }
            )
    }

    /**
     * Filters completed workout sessions to the most recent 1-month window for Home display,
     * sorted newest-to-oldest. Older workouts remain completely untouched in repositories and DB.
     */
    fun filterRecentWorkoutsLastMonth(
        workouts: List<Workout>,
        nowInstant: Instant = Clock.System.now(),
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<Workout> {
        if (workouts.isEmpty()) return emptyList()
        val cutoffDate = nowInstant.toLocalDateTime(timeZone).date.minus(DatePeriod(months = 1))
        val cutoffEpochMs = cutoffDate.atStartOfDayIn(timeZone).toEpochMilliseconds()
        return workouts
            .filter { it.startTimeEpochMs >= cutoffEpochMs }
            .sortedByDescending { it.startTimeEpochMs }
    }

    fun groupWorkoutsByMonth(
        workouts: List<Workout>,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): List<MonthlyWorkoutGroup> {
        if (workouts.isEmpty()) return emptyList()
        return workouts
            .sortedByDescending { it.startTimeEpochMs }
            .groupBy { w ->
                val dt = Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(timeZone)
                dt.year to dt.monthNumber
            }
            .entries
            .map { (ym, list) ->
                MonthlyWorkoutGroup(
                    year = ym.first,
                    monthNumber = ym.second,
                    totalVolumeKg = list.sumOf { it.totalVolumeKg },
                    totalDurationSeconds = list.sumOf { it.durationSeconds },
                    workouts = list
                )
            }
            .sortedWith(
                compareByDescending<MonthlyWorkoutGroup> { it.year }
                    .thenByDescending { it.monthNumber }
            )
    }

    fun formatFullDateWithYear(
        epochMs: Long,
        fallback: String,
        s: AppStrings,
        includeDayOfWeek: Boolean = false,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        if (epochMs <= 0L) return fallback
        val date = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(timeZone).date
        val monthName = s.monthNamesFull.getOrElse(date.monthNumber - 1) { "${date.monthNumber}" }
        return if (includeDayOfWeek) {
            val dayName = s.dayNamesFull.getOrElse(date.dayOfWeek.ordinal) { "" }
            if (dayName.isNotBlank()) "$dayName, ${date.dayOfMonth} $monthName ${date.year}"
            else "${date.dayOfMonth} $monthName ${date.year}"
        } else {
            "${date.dayOfMonth} $monthName ${date.year}"
        }
    }

    fun formatMonthYearHeader(year: Int, monthNumber: Int, s: AppStrings): String {
        val monthName = s.monthNamesFull.getOrElse(monthNumber - 1) { monthNumber.toString() }
        return "$monthName $year"
    }
}
