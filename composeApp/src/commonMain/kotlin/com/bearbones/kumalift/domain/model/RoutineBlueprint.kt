package com.bearbones.kumalift.domain.model

data class RoutineBlueprint(
    val id: String,
    val title: String,
    val splitCategory: String, // "PPL Split", "Upper / Lower", "Full Body"
    val muscleFocus: String,   // "Chest · Triceps · Delts"
    val estimatedDurationMinutes: Int = 60,
    val targetRpeTier: String = "Heavy, RPE 8–9",
    val exerciseTemplates: List<Exercise> = emptyList(),
    val plannedSetsPerExercise: Int = 3,
    val lastPerformedDaysAgo: String? = null
)
