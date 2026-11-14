package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import com.bearbones.kumalift.data.local.KumaDatabaseRepository

/**
 * Returns the singleton KumaDatabaseRepository backed by local SQLite.
 */
@Composable
expect fun rememberDatabaseRepository(): KumaDatabaseRepository

/**
 * Returns a SettingsRepository populated with persisted user preferences from local SQLite database.
 */
@Composable
expect fun rememberSettingsRepository(databaseRepo: KumaDatabaseRepository): SettingsRepository

/**
 * Returns an ExerciseRepository populated with default and custom exercises from local SQLite database.
 */
@Composable
expect fun rememberExerciseRepository(databaseRepo: KumaDatabaseRepository): ExerciseRepository

/**
 * Returns a RoutineRepository populated with default and custom routines from local SQLite database.
 */
@Composable
expect fun rememberRoutineRepository(
    exerciseRepository: ExerciseRepository,
    databaseRepo: KumaDatabaseRepository
): RoutineRepository
