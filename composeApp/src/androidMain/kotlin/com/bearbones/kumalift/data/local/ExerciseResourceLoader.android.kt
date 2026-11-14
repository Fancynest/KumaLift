package com.bearbones.kumalift.data.local

actual fun readExercisesJson(): String {
    val stream = Thread.currentThread().contextClassLoader?.getResourceAsStream("exercises.json")
        ?: ExerciseDataset::class.java.classLoader?.getResourceAsStream("exercises.json")
        ?: ExerciseDataset::class.java.getResourceAsStream("/exercises.json")
        ?: throw IllegalStateException("exercises.json resource not found in classpath")
    return stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
}
