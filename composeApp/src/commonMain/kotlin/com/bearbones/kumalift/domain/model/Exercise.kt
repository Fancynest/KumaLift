package com.bearbones.kumalift.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class MuscleGroup(val displayName: String) {
    CHEST("Dada"),
    BACK("Punggung"),
    LEGS("Kaki"),
    SHOULDERS("Bahu"),
    ARMS("Lengan"),
    CORE("Core"),
    CARDIO("Kardio"),
    NECK("Leher"),
    FULL_BODY("Seluruh Tubuh");

    fun localizedName(isEnglish: Boolean): String = when (this) {
        CHEST -> if (isEnglish) "Chest" else "Dada"
        BACK -> if (isEnglish) "Back" else "Punggung"
        LEGS -> if (isEnglish) "Legs" else "Kaki"
        SHOULDERS -> if (isEnglish) "Shoulders" else "Bahu"
        ARMS -> if (isEnglish) "Arms" else "Lengan"
        CORE -> if (isEnglish) "Core" else "Perut"
        CARDIO -> if (isEnglish) "Cardio" else "Kardio"
        NECK -> if (isEnglish) "Neck" else "Leher"
        FULL_BODY -> if (isEnglish) "Full Body" else "Seluruh Tubuh"
    }
}

@Serializable
enum class Equipment(val code: String, val displayName: String) {
    BARBELL("BB", "Barbell"),
    DUMBBELL("DB", "Dumbbell"),
    CABLE("CB", "Cable"),
    BODYWEIGHT("BW", "Bodyweight"),
    MACHINE("MC", "Machine"),
    SMITH_MACHINE("SM", "Smith Machine"),
    BAND("BD", "Resistance Band"),
    KETTLEBELL("KB", "Kettlebell"),
    OTHER("OT", "Lainnya");

    fun localizedName(isEnglish: Boolean): String = when (this) {
        BARBELL -> "Barbell"
        DUMBBELL -> "Dumbbell"
        CABLE -> "Cable"
        BODYWEIGHT -> if (isEnglish) "Bodyweight" else "Berat Tubuh"
        MACHINE -> if (isEnglish) "Machine" else "Mesin"
        SMITH_MACHINE -> "Smith Machine"
        BAND -> if (isEnglish) "Resistance Band" else "Karet Resistensi"
        KETTLEBELL -> "Kettlebell"
        OTHER -> if (isEnglish) "Other" else "Lainnya"
    }
}

@Serializable
enum class MovementType(val displayName: String) {
    COMPOUND("Compound"),
    ISOLATION("Isolasi");

    fun localizedName(isEnglish: Boolean): String = when (this) {
        COMPOUND -> "Compound"
        ISOLATION -> if (isEnglish) "Isolation" else "Isolasi"
    }
}

@Serializable
enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Pemula"),
    INTERMEDIATE("Menengah"),
    ADVANCED("Lanjut")
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
    fun getEffectiveSteps(): List<String> {
        if (instructionSteps.isNotEmpty()) return instructionSteps
        if (instructions.isBlank()) return listOf(
            "Posisikan tubuh dengan stabil dan nyaman.",
            "Lakukan gerakan secara terkontrol dengan menjaga postur yang benar.",
            "Rasakan kontraksi otot target pada puncak gerakan.",
            "Kembali perlahan ke posisi awal sambil mengatur pernapasan."
        )
        val lines = instructions.split("\n").map { it.trim().removePrefix("-").trim() }.filter { it.isNotBlank() }
        if (lines.size > 1) return lines
        val sentences = instructions.split(Regex("(?<=[.!?])\\s+")).map { it.trim() }.filter { it.isNotBlank() }
        return if (sentences.isNotEmpty()) sentences else listOf(instructions)
    }

    fun localizedSubMuscle(isEnglish: Boolean): String {
        val clean = subMuscle.trim().lowercase()
        if (clean.isBlank()) return ""
        return when (clean) {
            "abs" -> if (isEnglish) "Abs" else "Otot Perut"
            "quads" -> if (isEnglish) "Quads" else "Paha Depan"
            "hamstrings" -> if (isEnglish) "Hamstrings" else "Paha Belakang"
            "glutes" -> if (isEnglish) "Glutes" else "Bokong"
            "calves" -> if (isEnglish) "Calves" else "Betis"
            "biceps" -> if (isEnglish) "Biceps" else "Bisep"
            "triceps" -> if (isEnglish) "Triceps" else "Trisep"
            "forearms" -> if (isEnglish) "Forearms" else "Lengan Bawah"
            "lats" -> if (isEnglish) "Lats" else "Punggung Sayap"
            "traps" -> if (isEnglish) "Traps" else "Trapezius"
            "upper back" -> if (isEnglish) "Upper Back" else "Punggung Atas"
            "pectorals" -> if (isEnglish) "Pectorals" else "Dada"
            "delts" -> if (isEnglish) "Delts" else "Bahu"
            "abductors" -> if (isEnglish) "Abductors" else "Abduktor"
            "adductors" -> if (isEnglish) "Adductors" else "Adduktor"
            "spine" -> if (isEnglish) "Spine" else "Punggung Bawah"
            "cardiovascular system" -> if (isEnglish) "Cardio" else "Kardiovaskular"
            "serratus anterior" -> if (isEnglish) "Serratus" else "Serratus"
            "levator scapulae" -> if (isEnglish) "Levator Scapulae" else "Belikat"
            else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    fun localizedMuscleSubtitle(isEnglish: Boolean): String {
        val primary = primaryMuscle.localizedName(isEnglish)
        val sub = localizedSubMuscle(isEnglish)
        return if (sub.isNotBlank() && !sub.equals(primary, ignoreCase = true)) {
            "$primary · $sub"
        } else {
            primary
        }
    }
}
