package com.bearbones.kumalift.domain.share

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutHealthRecord
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

enum class WorkoutShareStatId(val storageKey: String) {
    DURATION("DURATION"),
    VOLUME("VOLUME"),
    PR_COUNT("PR_COUNT"),
    SETS_COMPLETED("SETS_COMPLETED"),
    EXERCISES_DONE("EXERCISES_DONE"),
    AVG_REST("AVG_REST"),
    EST_CALORIES("EST_CALORIES"),
    SESSION_RPE("SESSION_RPE"),
    WORKOUT_TYPE("WORKOUT_TYPE"),
    WORKOUT_DATE("WORKOUT_DATE"),
    MUSCLE_GROUPS("MUSCLE_GROUPS"),
    WEEKLY_STREAK("WEEKLY_STREAK"),
    TOP_SET("TOP_SET"),
    PR_HIGHLIGHTS("PR_HIGHLIGHTS");

    companion object {
        fun fromKey(key: String): WorkoutShareStatId? = entries.firstOrNull { it.storageKey == key }
    }
}

enum class ShareStatIconKey {
    TIMER,
    VOLUME,
    TROPHY,
    CHECK,
    EXERCISES,
    REST,
    CALORIES,
    RPE,
    WORKOUT,
    DATE,
    MUSCLE,
    STREAK,
    TOP_SET
}

data class WorkoutShareStatItem(
    val id: String,
    val label: String,
    val value: String,
    val unit: String,
    val iconKey: ShareStatIconKey
)

data class WorkoutShareData(
    val workoutTitle: String,
    val workoutSplitType: String,
    val formattedDate: String,
    val candidates: List<WorkoutShareStatItem>
)

object WorkoutShareStatsBuilder {

    fun sanitizeSelectedStatIds(
        rawIds: List<String>,
        maxCount: Int = MAX_WORKOUT_SHARE_STATS
    ): List<String> {
        val valid = rawIds
            .mapNotNull { WorkoutShareStatId.fromKey(it)?.storageKey }
            .distinct()
            .take(maxCount)
        return valid.ifEmpty {
            listOf(
                WorkoutShareStatId.DURATION.storageKey,
                WorkoutShareStatId.VOLUME.storageKey,
                WorkoutShareStatId.PR_COUNT.storageKey
            )
        }
    }

    fun toggleStatSelection(
        currentIds: List<String>,
        targetId: String,
        maxCount: Int = MAX_WORKOUT_SHARE_STATS
    ): List<String> {
        val clean = sanitizeSelectedStatIds(currentIds, maxCount)
        return if (clean.contains(targetId)) {
            if (clean.size <= 1) clean else clean.filterNot { it == targetId }
        } else if (clean.size < maxCount && WorkoutShareStatId.fromKey(targetId) != null) {
            clean + targetId
        } else {
            clean
        }
    }

    fun moveStatSelection(
        currentIds: List<String>,
        targetId: String,
        delta: Int
    ): List<String> {
        val list = currentIds.toMutableList()
        val idx = list.indexOf(targetId)
        if (idx == -1) return currentIds
        val targetIdx = (idx + delta).coerceIn(0, list.lastIndex)
        if (targetIdx == idx) return currentIds
        val item = list.removeAt(idx)
        list.add(targetIdx, item)
        return list
    }

    fun build(
        workout: Workout,
        completedWorkouts: List<Workout>,
        settings: UserSettings,
        s: AppStrings,
        timeZone: TimeZone = TimeZone.currentSystemDefault(),
        personalRecords: List<PersonalRecord> = emptyList()
    ): WorkoutShareData {
        val language = settings.appearance.language
        val weightUnit = settings.units.weightUnit
        val isLb = weightUnit == WeightUnit.LB
        val unitStr = if (isLb) s.unitLb else s.unitKg

        val durationSec = workout.durationSeconds.coerceAtLeast(0L)
        val durMins = durationSec / 60
        val durSecs = durationSec % 60
        val durationValue = "${durMins}:${if (durSecs < 10) "0$durSecs" else "$durSecs"}"

        val volumeDisplay = UnitConverters.toDisplayWeight(workout.totalVolumeKg, weightUnit).toLong()
        val volumeValue = NumberFormatUtils.formatIntegerWithSeparators(volumeDisplay, language)

        val completedSets = workout.totalCompletedSets
        val totalSets = workout.totalSets
        val setsValue = "$completedSets / $totalSets"

        val prSourceSetIds = personalRecords.mapNotNull { it.sourceSetId }.toSet()
        val prSets = workout.exercises.flatMap { ex ->
            ex.sets.filter { it.isPr || prSourceSetIds.contains(it.id) }.map { set -> ex to set }
        }

        val configuredRestSeconds = workout.exercises
            .flatMap { ex -> ex.sets.mapNotNull { it.restTimerSeconds ?: ex.restTimerSeconds } }
            .filter { it > 0 }
        val avgRestValue = if (configuredRestSeconds.isNotEmpty()) {
            s.secondsShort(configuredRestSeconds.average().toInt())
        } else {
            "—"
        }

        val calories = workout.activeCaloriesKcal ?: settings.profile.bodyweightKg?.takeIf { it > 0.0 }?.let { bwKg ->
            WorkoutHealthRecord.estimateCalories(
                durationMinutes = durationSec / 60.0,
                bodyWeightKg = bwKg
            )
        }
        val caloriesValue = calories?.let { NumberFormatUtils.formatIntegerWithSeparators(it.toLong(), language) } ?: "—"
        val caloriesUnit = if (calories != null) s.aiNutritionCaloriesLabel else ""

        val rpeValue = workout.sessionRpe?.let {
            NumberFormatUtils.formatDecimal(it, language, decimals = 1, trimTrailingZeros = true)
        } ?: "—"

        val formattedDate = formatWorkoutDate(
            epochMs = workout.startTimeEpochMs,
            pref = settings.units.dateFormat,
            language = language,
            timeZone = timeZone
        )

        val totalVol = workout.exercises.sumOf { it.totalVolumeKg }
        val muscleSummary = if (totalVol > 0.0) {
            workout.exercises
                .groupBy { it.exercise.primaryMuscle }
                .entries
                .sortedByDescending { (_, list) -> list.sumOf { it.totalVolumeKg } }
                .take(3)
                .joinToString(" · ") { (muscle, _) -> muscle.label(s) }
        } else {
            workout.exercises
                .map { it.exercise.primaryMuscle.label(s) }
                .distinct()
                .take(3)
                .joinToString(" · ")
                .ifBlank { "—" }
        }

        val streakWeeks = computeWeeklyStreak(
            workouts = (completedWorkouts + workout).distinctBy { it.id },
            referenceEpochMs = workout.startTimeEpochMs,
            firstDayOfWeek = settings.units.firstDayOfWeek,
            timeZone = timeZone
        )

        val topSetEntry = workout.exercises
            .mapNotNull { we -> we.topSet?.let { top -> we to top } }
            .maxWithOrNull(compareBy({ it.second.weightKg }, { it.second.reps }))
        val topSetText = topSetEntry?.let { (we, set) ->
            val wDisp = NumberFormatUtils.formatDecimal(
                UnitConverters.toDisplayWeight(set.weightKg, weightUnit),
                language,
                decimals = 1,
                trimTrailingZeros = true
            )
            "${we.exercise.name} $wDisp$unitStr × ${set.reps}"
        } ?: "—"

        val prHighlightText = if (prSets.isNotEmpty()) {
            prSets.take(2).joinToString(", ") { (we, set) ->
                val wDisp = NumberFormatUtils.formatDecimal(
                    UnitConverters.toDisplayWeight(set.weightKg, weightUnit),
                    language,
                    decimals = 1,
                    trimTrailingZeros = true
                )
                "${we.exercise.name} $wDisp$unitStr×${set.reps}"
            }
        } else {
            "—"
        }

        val candidates = listOf(
            WorkoutShareStatItem(
                id = WorkoutShareStatId.DURATION.storageKey,
                label = s.shareStatDurationLabel,
                value = durationValue,
                unit = "",
                iconKey = ShareStatIconKey.TIMER
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.VOLUME.storageKey,
                label = s.shareStatVolumeLabel,
                value = volumeValue,
                unit = unitStr,
                iconKey = ShareStatIconKey.VOLUME
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.PR_COUNT.storageKey,
                label = s.shareStatPrCountLabel,
                value = prSets.size.toString(),
                unit = "",
                iconKey = ShareStatIconKey.TROPHY
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.SETS_COMPLETED.storageKey,
                label = s.shareStatSetsLabel,
                value = setsValue,
                unit = "",
                iconKey = ShareStatIconKey.CHECK
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.EXERCISES_DONE.storageKey,
                label = s.shareStatExercisesLabel,
                value = workout.exercises.size.toString(),
                unit = "",
                iconKey = ShareStatIconKey.EXERCISES
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.AVG_REST.storageKey,
                label = s.shareStatAvgRestLabel,
                value = avgRestValue,
                unit = "",
                iconKey = ShareStatIconKey.REST
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.EST_CALORIES.storageKey,
                label = s.shareStatCaloriesLabel,
                value = caloriesValue,
                unit = caloriesUnit,
                iconKey = ShareStatIconKey.CALORIES
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.SESSION_RPE.storageKey,
                label = s.shareStatSessionRpeLabel,
                value = rpeValue,
                unit = "",
                iconKey = ShareStatIconKey.RPE
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.WORKOUT_TYPE.storageKey,
                label = s.shareStatWorkoutTypeLabel,
                value = workout.splitType.ifBlank { workout.title },
                unit = "",
                iconKey = ShareStatIconKey.WORKOUT
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.WORKOUT_DATE.storageKey,
                label = s.shareStatDateLabel,
                value = formattedDate,
                unit = "",
                iconKey = ShareStatIconKey.DATE
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.MUSCLE_GROUPS.storageKey,
                label = s.shareStatMusclesLabel,
                value = muscleSummary,
                unit = "",
                iconKey = ShareStatIconKey.MUSCLE
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.WEEKLY_STREAK.storageKey,
                label = s.shareStatWeeklyStreakLabel,
                value = s.shareStreakWeeksValue(streakWeeks),
                unit = "",
                iconKey = ShareStatIconKey.STREAK
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.TOP_SET.storageKey,
                label = s.shareStatTopSetLabel,
                value = topSetText,
                unit = "",
                iconKey = ShareStatIconKey.TOP_SET
            ),
            WorkoutShareStatItem(
                id = WorkoutShareStatId.PR_HIGHLIGHTS.storageKey,
                label = s.shareStatPrHighlightsLabel,
                value = prHighlightText,
                unit = "",
                iconKey = ShareStatIconKey.TROPHY
            )
        )

        return WorkoutShareData(
            workoutTitle = workout.title.ifBlank { s.workoutCompletedTitle },
            workoutSplitType = workout.splitType,
            formattedDate = formattedDate,
            candidates = candidates
        )
    }

    fun formatWorkoutDate(
        epochMs: Long,
        pref: DateFormatPreference,
        language: AppLanguage,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): String {
        val dt = Instant.fromEpochMilliseconds(epochMs.coerceAtLeast(0L)).toLocalDateTime(timeZone).date
        val d = dt.dayOfMonth.toString().padStart(2, '0')
        val m = dt.monthNumber.toString().padStart(2, '0')
        val y = dt.year
        val resolved = if (pref == DateFormatPreference.SYSTEM) {
            if (language == AppLanguage.ID) DateFormatPreference.DMY else DateFormatPreference.MDY
        } else {
            pref
        }
        return when (resolved) {
            DateFormatPreference.MDY -> "$m/$d/$y"
            DateFormatPreference.YMD -> "$y-$m-$d"
            DateFormatPreference.DMY, DateFormatPreference.SYSTEM -> "$d/$m/$y"
        }
    }

    fun computeWeeklyStreak(
        workouts: List<Workout>,
        referenceEpochMs: Long,
        firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Int {
        if (workouts.isEmpty()) return 0
        val startDow = when (firstDayOfWeek) {
            FirstDayOfWeek.MONDAY -> DayOfWeek.MONDAY
            FirstDayOfWeek.SUNDAY -> DayOfWeek.SUNDAY
            FirstDayOfWeek.SATURDAY -> DayOfWeek.SATURDAY
        }
        fun weekStart(date: LocalDate): LocalDate {
            val offset = (date.dayOfWeek.ordinal - startDow.ordinal + 7) % 7
            return date.minus(DatePeriod(days = offset))
        }
        val activeWeeks = workouts
            .map { Instant.fromEpochMilliseconds(it.startTimeEpochMs).toLocalDateTime(timeZone).date }
            .map(::weekStart)
            .toSet()
        var cursor = weekStart(Instant.fromEpochMilliseconds(referenceEpochMs).toLocalDateTime(timeZone).date)
        var streak = 0
        while (activeWeeks.contains(cursor)) {
            streak++
            cursor = cursor.minus(DatePeriod(days = 7))
        }
        return streak
    }
}
