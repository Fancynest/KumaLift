package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bearbones.kumalift.data.local.DatabaseDriverFactory
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.local.SeedData
import com.bearbones.kumalift.database.KumaDatabase

@Composable
actual fun rememberDatabaseRepository(): KumaDatabaseRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val driver = DatabaseDriverFactory(context).createDriver()
        val database = KumaDatabase(driver)
        KumaDatabaseRepository(database)
    }
}

@Composable
actual fun rememberSettingsRepository(databaseRepo: KumaDatabaseRepository): SettingsRepository {
    val context = LocalContext.current.applicationContext
    return remember(context, databaseRepo) {
        // Load from SQLDelight database first
        val savedDbSettings = databaseRepo.loadUserSettings()
        val initial = if (savedDbSettings != null) {
            savedDbSettings
        } else {
            // First run migration from AndroidSettingsStore
            val legacyStore = AndroidSettingsStore(context)
            val migrated = legacyStore.load()
            databaseRepo.saveUserSettings(migrated)
            migrated
        }

        SettingsRepository(
            initialSettings = initial,
            onSave = { updated ->
                databaseRepo.saveUserSettings(updated)
            }
        )
    }
}

@Composable
actual fun rememberExerciseRepository(databaseRepo: KumaDatabaseRepository): ExerciseRepository {
    val context = LocalContext.current.applicationContext
    return remember(context, databaseRepo) {
        val dbCustom = databaseRepo.getAllCustomExercises()
        val custom = if (dbCustom.isNotEmpty()) {
            dbCustom
        } else {
            // First run migration from legacy json
            val legacyStore = AndroidExerciseStore(context)
            val migrated = legacyStore.loadCustomExercises()
            migrated.forEach { databaseRepo.saveCustomExercise(it) }
            migrated
        }

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
    val context = LocalContext.current.applicationContext
    return remember(context, exerciseRepository, databaseRepo) {
        val dbCustom = databaseRepo.getAllCustomRoutines { id -> exerciseRepository.getExerciseById(id) }
        val custom = if (dbCustom.isNotEmpty()) {
            dbCustom
        } else {
            // First run migration from legacy json
            val legacyStore = AndroidRoutineStore(context, exerciseRepository)
            val migrated = legacyStore.loadCustomRoutines()
            migrated.forEach { databaseRepo.saveCustomRoutine(it) }
            migrated
        }

        RoutineRepository(
            initialRoutines = custom + SeedData.defaultRoutines,
            databaseRepo = databaseRepo
        )
    }
}
