package com.bearbones.kumalift.domain.ai

import kotlinx.serialization.Serializable

@Serializable
enum class AiChatRole {
    USER,
    ASSISTANT
}

@Serializable
enum class AiProposalStatus {
    PENDING,
    APPLIED,
    CANCELLED,
    FAILED
}

@Serializable
enum class AiOperationKind {
    ADD,
    UPDATE,
    DELETE
}

@Serializable
enum class AiActionType {
    CREATE_ROUTINE,
    UPDATE_ROUTINE,
    DELETE_ROUTINE,
    SCHEDULE_WORKOUT,
    CANCEL_SCHEDULE,
    DELETE_SCHEDULE,
    START_MESOCYCLE,
    LOG_BODYWEIGHT,
    UPDATE_TRAINING_GOAL,
    ADD_EXERCISE_TO_ACTIVE_WORKOUT,
    UPDATE_AI_PREFERENCES
}

@Serializable
data class AiProposedOperation(
    val id: String,
    val actionType: AiActionType,
    val operationKind: AiOperationKind,
    val title: String,
    val subtitle: String = "",
    val payloadJson: String = "{}"
)

@Serializable
data class AiActionProposal(
    val id: String,
    val summary: String = "",
    val status: AiProposalStatus = AiProposalStatus.PENDING,
    val operations: List<AiProposedOperation> = emptyList(),
    val resultNote: String? = null
)

@Serializable
data class AiNutritionCardData(
    val foodName: String,
    val caloriesKcal: Int,
    val proteinGrams: Int,
    val carbsGrams: Int,
    val fatGrams: Int,
    val summary: String
)

@Serializable
data class AiChatMessage(
    val id: String,
    val role: AiChatRole,
    val text: String,
    val imagePath: String? = null,
    val stepLogs: List<String> = emptyList(),
    val proposal: AiActionProposal? = null,
    val nutritionCard: AiNutritionCardData? = null,
    val isError: Boolean = false,
    val createdAtEpochMs: Long = 0L
)
