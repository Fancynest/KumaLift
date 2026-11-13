package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberSettingsRepository(): SettingsRepository {
    return remember { SettingsRepository() }
}

@Composable
actual fun rememberExerciseRepository(): ExerciseRepository {
    return remember { ExerciseRepository() }
}

@Composable
actual fun rememberRoutineRepository(exerciseRepository: ExerciseRepository): RoutineRepository {
    return remember { RoutineRepository() }
}
