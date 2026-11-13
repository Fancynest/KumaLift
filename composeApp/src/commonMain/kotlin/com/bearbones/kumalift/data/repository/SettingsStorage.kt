package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable

/**
 * Returns a SettingsRepository populated with persisted user preferences on platforms that support it.
 */
@Composable
expect fun rememberSettingsRepository(): SettingsRepository

/**
 * Returns an ExerciseRepository populated with default and custom exercises on platforms that support it.
 */
@Composable
expect fun rememberExerciseRepository(): ExerciseRepository

/**
 * Returns a RoutineRepository populated with default and custom routines on platforms that support it.
 */
@Composable
expect fun rememberRoutineRepository(exerciseRepository: ExerciseRepository): RoutineRepository
