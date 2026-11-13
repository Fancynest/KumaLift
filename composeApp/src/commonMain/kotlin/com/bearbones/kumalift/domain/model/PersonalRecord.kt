package com.bearbones.kumalift.domain.model

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val estimated1RmKg: Double,
    val dateAchieved: String,
    val deltaWeightKg: Double = 0.0,
    val prTier: String = "3RM PR"
)
