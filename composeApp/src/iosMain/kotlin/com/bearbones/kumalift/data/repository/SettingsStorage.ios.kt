package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bearbones.kumalift.data.local.DatabaseDriverFactory
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.database.KumaDatabase
import com.bearbones.kumalift.domain.model.UserSettings

@Composable
actual fun rememberDatabaseRepository(): KumaDatabaseRepository {
    return remember {
        val driver = DatabaseDriverFactory().createDriver()
        val database = KumaDatabase(driver)
        KumaDatabaseRepository(database)
    }
}

@Composable
actual fun rememberSettingsRepository(databaseRepo: KumaDatabaseRepository): SettingsRepository {
    return remember(databaseRepo) {
        val initial = databaseRepo.loadUserSettings() ?: UserSettings()
        SettingsRepository(
            initialSettings = initial,
            onSave = { updated -> databaseRepo.saveUserSettings(updated) }
        )
    }
}

@Composable
actual fun rememberExerciseRepository(databaseRepo: KumaDatabaseRepository): ExerciseRepository {
    return remember(databaseRepo) {
        val custom = databaseRepo.getAllCustomExercises()
        ExerciseRepository(
            initialExercises = custom + SeedData.defaultExercises,
            databaseRepo = databaseRepo
        )
    }
}

@Composable
actual fun rememberRoutineRepository(
    exerciseRepository: ExerciseRepository,
    databaseRepo: KumaDatabaseRepository
): RoutineRepository {
    return remember(exerciseRepository, databaseRepo) {
        val custom = databaseRepo.getAllCustomRoutines { id -> exerciseRepository.getExerciseById(id) }
        RoutineRepository(
            initialRoutines = custom + SeedData.defaultRoutines,
            databaseRepo = databaseRepo
        )
    }
}
