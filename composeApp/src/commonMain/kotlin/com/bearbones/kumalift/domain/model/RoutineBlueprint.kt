package com.bearbones.kumalift.domain.model

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.label
import kotlinx.serialization.Serializable

@Serializable
enum class RpeTier(val code: String) {
    LIGHT("light"),
    MODERATE("moderate"),
    HEAVY("heavy"),
    MAXIMAL("maximal");

    companion object {
        fun fromValue(value: String): RpeTier {
            val v = value.trim().lowercase()
            return when {
                v == "light" || v.contains("6–7") || v.contains("6-7") || v.contains("ringan") -> LIGHT
                v == "moderate" || v.contains("7–8") || v.contains("7-8") || v.contains("sedang") -> MODERATE
                v == "heavy" || v.contains("8–9") || v.contains("8-9") || v.contains("berat") -> HEAVY
                v == "maximal" || v.contains("9–10") || v.contains("9-10") || v.contains("maksimal") -> MAXIMAL
                else -> HEAVY
            }
        }
    }
}

fun RpeTier.label(s: AppStrings): String = when (this) {
    RpeTier.LIGHT -> s.rpeTiers.getOrElse(0) { "Light, RPE 6–7" }
    RpeTier.MODERATE -> s.rpeTiers.getOrElse(1) { "Moderate, RPE 7–8" }
    RpeTier.HEAVY -> s.rpeTiers.getOrElse(2) { "Heavy, RPE 8–9" }
    RpeTier.MAXIMAL -> s.rpeTiers.getOrElse(3) { "Maximal, RPE 9–10" }
}

@Serializable
data class RoutineSetConfig(
    val setNumber: Int = 1,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val targetRpe: Double? = null,
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null,
    val previousPerformance: String? = null,
    val restTimerSeconds: Int? = null
)

@Serializable
data class RoutineExerciseConfig(
    val exercise: Exercise,
    val targetSets: Int = 3,
    val targetReps: Int = 8,
    val targetWeightKg: Double? = null,
    val restTimerSeconds: Int = 90,
    val supersetGroupId: String? = null,
    val setConfigs: List<RoutineSetConfig> = emptyList()
) {
    val effectiveSets: List<RoutineSetConfig>
        get() {
            if (setConfigs.isNotEmpty()) return setConfigs
            val count = maxOf(1, targetSets)
            return (1..count).map { idx ->
                RoutineSetConfig(
                    setNumber = idx,
                    targetReps = targetReps,
                    targetWeightKg = targetWeightKg,
                    targetRpe = null,
                    previousWeightKg = null,
                    previousReps = null,
                    previousPerformance = null,
                    restTimerSeconds = restTimerSeconds
                )
            }
        }
}

@Serializable
data class RoutineBlueprint(
    val id: String,
    val programId: String = DEFAULT_PROGRAM_ID,
    val title: String,
    val splitCategory: String, // "PPL Split", "Upper / Lower", "Full Body"
    val muscleFocus: String,   // "Chest · Triceps · Delts" or codes
    val estimatedDurationMinutes: Int = 60,
    val targetRpeTier: String = "heavy",
    val exerciseTemplates: List<Exercise> = emptyList(),
    val exerciseConfigs: List<RoutineExerciseConfig> = emptyList(),
    val plannedSetsPerExercise: Int = 3,
    val lastPerformedDaysAgo: String? = null,
    val scheduledDays: List<Int> = emptyList(), // 1 = Monday .. 7 = Sunday
    val presetKey: String? = null,
    val isUserRenamed: Boolean = false
) {
    /**
     * Resolves effective exercise configurations, ensuring backward compatibility with
     * older routines that only have `exerciseTemplates`.
     */
    val effectiveConfigs: List<RoutineExerciseConfig>
        get() {
            if (exerciseConfigs.isNotEmpty()) return exerciseConfigs
            return exerciseTemplates.map {
                RoutineExerciseConfig(
                    exercise = it,
                    targetSets = plannedSetsPerExercise,
                    targetReps = 8,
                    targetWeightKg = null,
                    restTimerSeconds = 90
                )
            }
        }
}

fun RoutineBlueprint.localizedTitle(s: AppStrings): String {
    if (isUserRenamed || presetKey == null) return title
    return when (presetKey) {
        "starter_push" -> s.routinePushDayTitle
        "starter_pull" -> s.routinePullDayTitle
        "starter_strength_a" -> s.routineFullBodyTitle
        "starter_general_full" -> s.routineUpperLowerTitle
        else -> title
    }
}

fun RoutineBlueprint.localizedRpeTier(s: AppStrings): String {
    return RpeTier.fromValue(targetRpeTier).label(s)
}

fun RoutineBlueprint.localizedSplitCategory(s: AppStrings): String {
    val trimmed = splitCategory.trim()
    val presets = s.presetRoutineCategories
    return when {
        trimmed.equals("PPL Split", ignoreCase = true) || trimmed.equals("Pola PPL", ignoreCase = true) -> presets.getOrElse(0) { "PPL Split" }
        trimmed.equals("Upper / Lower", ignoreCase = true) || trimmed.equals("Atas / Bawah", ignoreCase = true) -> presets.getOrElse(1) { "Upper / Lower" }
        trimmed.equals("Full Body", ignoreCase = true) || trimmed.equals("Seluruh Tubuh", ignoreCase = true) -> presets.getOrElse(2) { "Full Body" }
        trimmed.equals("Custom", ignoreCase = true) || trimmed.equals("Kustom", ignoreCase = true) -> s.custom
        else -> splitCategory
    }
}

fun RoutineBlueprint.localizedMuscleFocus(s: AppStrings): String {
    val configMuscles = effectiveConfigs.map { it.exercise.primaryMuscle.label(s) }.distinct()
    if (configMuscles.isNotEmpty()) {
        return configMuscles.joinToString(" · ")
    }

    val trimmed = muscleFocus.trim()
    return when {
        trimmed == "focus_push" ||
        trimmed.equals("Chest · Triceps · Delts", ignoreCase = true) ||
        trimmed.equals("Dada · Bahu · Triceps", ignoreCase = true) -> s.muscleFocusPush

        trimmed == "focus_pull" ||
        trimmed.equals("Back · Biceps", ignoreCase = true) ||
        trimmed.equals("Punggung · Bisep", ignoreCase = true) -> s.muscleFocusPull

        trimmed == "focus_compound" ||
        trimmed.equals("Heavy Compounds", ignoreCase = true) ||
        trimmed.equals("Gerakan Beban Majemuk", ignoreCase = true) -> s.muscleFocusCompound

        trimmed == "focus_conditioning" ||
        trimmed.equals("Conditioning & Hypertrophy", ignoreCase = true) ||
        trimmed.equals("Kondisi Fisik & Hipertrofi", ignoreCase = true) -> s.muscleFocusConditioning

        trimmed == "full_body" ||
        trimmed.equals("Full Body", ignoreCase = true) ||
        trimmed.equals("Seluruh Tubuh", ignoreCase = true) -> s.muscleFullBody

        else -> {
            val parts = trimmed.split(Regex("""[·,]+""")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isNotEmpty()) {
                val mapped = parts.map { part ->
                    val mg = MuscleGroup.values().firstOrNull { it.name.equals(part, ignoreCase = true) }
                    mg?.label(s) ?: part
                }
                mapped.joinToString(" · ")
            } else {
                trimmed.ifEmpty { s.muscleFullBody }
            }
        }
    }
}

