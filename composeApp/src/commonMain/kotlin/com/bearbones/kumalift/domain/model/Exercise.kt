package com.bearbones.kumalift.domain.model

enum class MuscleGroup(val displayName: String) {
    CHEST("Chest"),
    BACK("Back"),
    LEGS("Legs"),
    SHOULDERS("Shoulders"),
    ARMS("Arms"),
    CORE("Core"),
    FULL_BODY("Full Body")
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
