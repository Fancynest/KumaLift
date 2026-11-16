package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.IdStrings
import com.bearbones.kumalift.domain.model.Exercise
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ExercisesIdPayload(
    val source: String = "machine-translated",
    val exercises: Map<String, List<String>> = emptyMap()
)

object ExerciseDataset {
    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val allExercises: List<Exercise> by lazy {
        try {
            val json = readExercisesJson().trim().removePrefix("\uFEFF")
            jsonParser.decodeFromString<List<Exercise>>(json)
        } catch (e: Throwable) {
            println("ERROR loading exercises.json: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    val allExerciseIds: Set<String> by lazy {
        allExercises.map { it.id }.toSet()
    }

    private val indonesianInstructions: Map<String, List<String>> by lazy {
        try {
            val json = readExercisesIdJson().trim().removePrefix("\uFEFF")
            if (json.isBlank() || json == "{}") emptyMap()
            else {
                val payload = jsonParser.decodeFromString<ExercisesIdPayload>(json)
                payload.exercises
            }
        } catch (e: Throwable) {
            println("ERROR loading exercises_id.json: ${e.message}")
            e.printStackTrace()
            emptyMap()
        }
    }

    fun getIndonesianSteps(id: String): List<String>? = indonesianInstructions[id]

    fun getLocalizedSteps(id: String, s: AppStrings): List<String>? {
        if (s is IdStrings) {
            val idSteps = getIndonesianSteps(id)
            if (!idSteps.isNullOrEmpty()) return idSteps
        }
        return null
    }

    val indonesianInstructionsPayload: ExercisesIdPayload? by lazy {
        try {
            val json = readExercisesIdJson().trim().removePrefix("\uFEFF")
            if (json.isBlank() || json == "{}") null
            else jsonParser.decodeFromString<ExercisesIdPayload>(json)
        } catch (e: Throwable) {
            null
        }
    }
}
