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
    MACHINE("MC", "Machine")
}

data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscle: MuscleGroup,
    val secondaryMuscles: List<MuscleGroup> = emptyList(),
    val equipment: Equipment,
    val defaultIncrementKg: Double = 2.5,
    val currentPrWeightKg: Double? = null,
    val currentPrReps: Int? = null,
    val lastLoggedDate: String? = null
)
