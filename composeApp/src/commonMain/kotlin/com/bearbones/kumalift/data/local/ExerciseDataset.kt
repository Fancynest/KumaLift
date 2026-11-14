package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Exercise
import kotlinx.serialization.json.Json

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
}
