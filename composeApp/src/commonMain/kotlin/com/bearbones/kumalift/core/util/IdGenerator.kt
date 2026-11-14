package com.bearbones.kumalift.core.util

import kotlinx.datetime.Clock
import kotlin.random.Random

object IdGenerator {
    fun nextId(prefix: String = ""): String {
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val randomPart = Random.nextInt(10000, 99999)
        return if (prefix.isBlank()) "${timestamp}_$randomPart" else "${prefix}_${timestamp}_$randomPart"
    }

    fun workoutId(routineId: String? = null): String {
        return nextId(if (routineId != null) "w_$routineId" else "w")
    }

    fun workoutExerciseId(exerciseId: String): String {
        return nextId("we_$exerciseId")
    }

    fun setId(exerciseId: String, setNumber: Int): String {
        return nextId("set_${exerciseId}_${setNumber}")
    }

    fun customExerciseId(name: String): String {
        val cleanName = name.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16)
        return nextId("custom_$cleanName")
    }

    fun routineId(title: String): String {
        val cleanTitle = title.lowercase().replace(Regex("[^a-z0-9]"), "_").take(16)
        return nextId("custom_$cleanTitle")
    }

    fun prId(exerciseId: String): String {
        return nextId("pr_$exerciseId")
    }
}
