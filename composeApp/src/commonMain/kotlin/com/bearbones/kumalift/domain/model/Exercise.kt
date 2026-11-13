package com.bearbones.kumalift.domain.model

enum class MuscleGroup(val displayName: String) {
    CHEST("Dada"),
    BACK("Punggung"),
    LEGS("Kaki"),
    SHOULDERS("Bahu"),
    ARMS("Lengan"),
    CORE("Core"),
    FULL_BODY("Seluruh Tubuh")
}

enum class Equipment(val code: String, val displayName: String) {
    BARBELL("BB", "Barbell"),
    DUMBBELL("DB", "Dumbbell"),
    CABLE("CB", "Cable"),
    BODYWEIGHT("BW", "Bodyweight"),
    MACHINE("MC", "Machine"),
    SMITH_MACHINE("SM", "Smith Machine"),
    BAND("BD", "Resistance Band"),
    KETTLEBELL("KB", "Kettlebell"),
    OTHER("OT", "Lainnya")
}

enum class MovementType(val displayName: String) {
    COMPOUND("Compound"),
    ISOLATION("Isolasi")
}

enum class ExperienceLevel(val displayName: String) {
    BEGINNER("Pemula"),
    INTERMEDIATE("Menengah"),
    ADVANCED("Lanjut")
}

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
}
