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
    CREATE_PROGRAM,
    CREATE_EXERCISE,
    SAVE_EXERCISE_NOTE,
    SCHEDULE_WORKOUT,
    CANCEL_SCHEDULE,
    DELETE_SCHEDULE,
    START_MESOCYCLE,
    LOG_BODYWEIGHT,
    LOG_READINESS,
    UPDATE_TRAINING_GOAL,
    ADD_EXERCISE_TO_ACTIVE_WORKOUT,
    UPDATE_AI_PREFERENCES
}

@Serializable
enum class AiExerciseDiffStatus {
    ADDED,
    MODIFIED,
    REPLACED,
    REMOVED,
    UNCHANGED
}

@Serializable
data class AiSetPreview(
    val setNumber: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val restSeconds: Int = 75
)

@Serializable
data class AiExercisePreviewItem(
    val exerciseId: String = "",
    val name: String,
    val primaryMuscleCode: String = "",
    val equipmentCode: String = "",
    val sets: Int,
    val reps: Int,
    val weightKg: Double? = null,
    val restSeconds: Int = 75,
    val setDetails: List<AiSetPreview> = emptyList()
)

@Serializable
data class AiExerciseDiffEntry(
    val status: AiExerciseDiffStatus,
    val before: AiExercisePreviewItem? = null,
    val after: AiExercisePreviewItem? = null
)

@Serializable
data class AiFieldChangePreview(
    val fieldKey: String,
    val beforeValue: String? = null,
    val afterValue: String,
    val beforeWeightKg: Double? = null,
    val afterWeightKg: Double? = null
)

@Serializable
data class AiOperationPreviewDetail(
    val routineId: String? = null,
    val routineName: String? = null,
    val beforeRoutineName: String? = null,
    val splitCategory: String? = null,
    val beforeSplitCategory: String? = null,
    val durationMinutes: Int? = null,
    val beforeDurationMinutes: Int? = null,
    val scheduledDays: List<Int> = emptyList(),
    val beforeScheduledDays: List<Int> = emptyList(),
    val exerciseDiffs: List<AiExerciseDiffEntry> = emptyList(),
    val updatedExercises: List<AiExercisePreviewItem> = emptyList(),
    val fieldChanges: List<AiFieldChangePreview> = emptyList()
)

@Serializable
data class AiProposedOperation(
    val id: String,
    val actionType: AiActionType,
    val operationKind: AiOperationKind,
    val title: String,
    val subtitle: String = "",
    val payloadJson: String = "{}",
    val previewDetail: AiOperationPreviewDetail? = null
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
data class AiWebSource(
    val title: String,
    val url: String
)

const val DEFAULT_CHAT_THREAD_ID = "thread_default"

@Serializable
data class AiChatThread(
    val id: String,
    val title: String,
    val contextSummary: String = "",
    val messageCount: Int = 0,
    val createdAtEpochMs: Long = 0L,
    val updatedAtEpochMs: Long = 0L
)

@Serializable
data class AiChatMessage(
    val id: String,
    val role: AiChatRole,
    val text: String,
    val threadId: String = DEFAULT_CHAT_THREAD_ID,
    val imagePath: String? = null,
    val imagePaths: List<String> = emptyList(),
    val stepLogs: List<String> = emptyList(),
    val proposal: AiActionProposal? = null,
    val nutritionCard: AiNutritionCardData? = null,
    val isError: Boolean = false,
    val createdAtEpochMs: Long = 0L
) {
    val effectiveImagePaths: List<String>
        get() = if (imagePaths.isNotEmpty()) imagePaths else listOfNotNull(imagePath)
}

fun AiChatMessage.copyableContent(): String {
    if (text.isNotBlank()) return text
    return buildList {
        nutritionCard?.let { n ->
            add(
                buildString {
                    append("${n.foodName} (${n.caloriesKcal} kcal | P ${n.proteinGrams}g | C ${n.carbsGrams}g | F ${n.fatGrams}g)")
                    if (n.summary.isNotBlank()) append("\n${n.summary}")
                }
            )
        }
        proposal?.let { p ->
            val opsText = p.operations.joinToString("\n") { op ->
                if (op.subtitle.isNotBlank()) "${op.title} — ${op.subtitle}" else op.title
            }
            if (opsText.isNotBlank()) add(opsText)
        }
    }.joinToString("\n\n")
}
