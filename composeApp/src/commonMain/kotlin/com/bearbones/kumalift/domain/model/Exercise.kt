package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup {
    CHEST,
    BACK,
    LEGS,
    SHOULDERS,
    ARMS,
    CORE,
    CARDIO,
    NECK,
    FULL_BODY;
}

@Serializable
enum class Equipment(val code: String) {
    BARBELL("BB"),
    DUMBBELL("DB"),
    CABLE("CB"),
    BODYWEIGHT("BW"),
    MACHINE("MC"),
    SMITH_MACHINE("SM"),
    BAND("BD"),
    KETTLEBELL("KB"),
    OTHER("OT");
}

@Serializable
enum class MovementType {
    COMPOUND,
    ISOLATION;
}

@Serializable
enum class ExperienceLevel {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED;
}

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val subMuscle: String = "",
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val secondaryMuscleDetails: String = "",
    val equipment: Equipment,
    val equipmentDetail: String = "",
    val movementType: MovementType = MovementType.COMPOUND,
    val level: ExperienceLevel = ExperienceLevel.BEGINNER,
    val instructions: String = "",
    val instructionSteps: List<String> = emptyList(),
    val defaultIncrementKg: Double = 2.5,
    val currentPrWeightKg: Double? = null,
    val currentPrReps: Int? = null,
    val lastLoggedDate: String? = null,
    val gifUrl: String? = null,
    val thumbnailUrl: String? = null,
    val attribution: String = "© Gym visual",
    val pinnedNote: String = ""
) {
    fun getEffectiveSteps(
        s: com.bearbones.kumalift.core.localization.AppStrings? = null,
        defaultSteps: List<String> = emptyList()
    ): List<String> {
        if (s != null) {
            val localized = com.bearbones.kumalift.data.local.ExerciseDataset.getLocalizedSteps(id, s)
            if (!localized.isNullOrEmpty()) return localized
        }
        if (instructionSteps.isNotEmpty()) return instructionSteps
        if (instructions.isBlank()) {
            if (defaultSteps.isNotEmpty()) return defaultSteps
            if (s != null) {
                return listOf(s.defaultStep1, s.defaultStep2, s.defaultStep3, s.defaultStep4)
            }
            return listOf(
                "Posisikan tubuh dengan stabil dan nyaman.",
                "Lakukan gerakan secara terkontrol dengan menjaga postur yang benar.",
                "Rasakan kontraksi otot target pada puncak gerakan.",
                "Kembali perlahan ke posisi awal sambil mengatur pernapasan."
            )
        }
        val lines = instructions.split("\n").map { it.trim().removePrefix("-").trim() }.filter { it.isNotBlank() }
        if (lines.size > 1) return lines
        val sentences = instructions.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        return if (sentences.isNotEmpty()) sentences else listOf(instructions)
    }
}
