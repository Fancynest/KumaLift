package com.bearbones.kumalift.data.repository

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bearbones.kumalift.core.backup.AutoBackupScheduler
import com.bearbones.kumalift.data.local.AndroidDatabaseProvider
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.data.local.SeedData

@Composable
actual fun rememberDatabaseRepository(): KumaDatabaseRepository {
    val context = LocalContext.current.applicationContext
    return remember<KumaDatabaseRepository>(context) {
        val repo = AndroidDatabaseProvider.getRepository(context)
        repo
    }
}

@Composable
actual fun rememberSettingsRepository(databaseRepo: KumaDatabaseRepository): SettingsRepository {
    val context = LocalContext.current.applicationContext
    return remember<SettingsRepository>(context, databaseRepo) {
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

        val repo = SettingsRepository(
            initialSettings = initial,
            onSave = { updated ->
                databaseRepo.saveUserSettings(updated)
                AutoBackupScheduler.scheduleOrUpdate(context, updated)
                com.bearbones.kumalift.core.timer.RestTimerCoordinator.userSettingsProvider = { updated }
            }
        )
        com.bearbones.kumalift.core.timer.RestTimerCoordinator.userSettingsProvider = { repo.settings.value }
        repo
    }
}

@Composable
actual fun rememberExerciseRepository(databaseRepo: KumaDatabaseRepository): ExerciseRepository {
    val context = LocalContext.current.applicationContext
    return remember<ExerciseRepository>(context, databaseRepo) {
        val migPrefs = context.getSharedPreferences("kumalift_legacy_migrations", Context.MODE_PRIVATE)
        val dbCustom = databaseRepo.getAllCustomExercises()
        val custom = if (dbCustom.isNotEmpty() || migPrefs.getBoolean("migrated_exercises_v1", false)) {
            if (!migPrefs.getBoolean("migrated_exercises_v1", false)) {
                migPrefs.edit().putBoolean("migrated_exercises_v1", true).apply()
            }
            dbCustom
        } else {
            // First run migration from legacy json
            val legacyStore = AndroidExerciseStore(context)
            val migrated = legacyStore.loadCustomExercises()
            migrated.forEach { databaseRepo.saveCustomExercise(it) }
            migPrefs.edit().putBoolean("migrated_exercises_v1", true).apply()
            migrated
        }

        val repo = ExerciseRepository(
            initialExercises = custom + SeedData.defaultExercises,
            databaseRepo = databaseRepo
        )
        repo
    }
}

@Composable
actual fun rememberRoutineRepository(
    exerciseRepository: ExerciseRepository,
    databaseRepo: KumaDatabaseRepository
): RoutineRepository {
    val context = LocalContext.current.applicationContext
    return remember<RoutineRepository>(context, exerciseRepository, databaseRepo) {
        val migPrefs = context.getSharedPreferences("kumalift_legacy_migrations", Context.MODE_PRIVATE)
        val dbCustom = databaseRepo.getAllCustomRoutines { id -> exerciseRepository.getExerciseById(id) }
        val custom = if (dbCustom.isNotEmpty() || migPrefs.getBoolean("migrated_routines_v1", false)) {
            if (!migPrefs.getBoolean("migrated_routines_v1", false)) {
                migPrefs.edit().putBoolean("migrated_routines_v1", true).apply()
            }
            dbCustom
        } else {
            // First run migration from legacy json
            val legacyStore = AndroidRoutineStore(context, exerciseRepository)
            val migrated = legacyStore.loadCustomRoutines()
            migrated.forEach { databaseRepo.saveCustomRoutine(it) }
            migPrefs.edit().putBoolean("migrated_routines_v1", true).apply()
            migrated
        }

        val repo = RoutineRepository(
            initialRoutines = custom.filterNot { it.id.startsWith("rt_") },
            databaseRepo = databaseRepo
        )
        repo
    }
}

@Composable
actual fun rememberTrainingProgramRepository(
    databaseRepo: KumaDatabaseRepository,
    settingsRepository: SettingsRepository
): TrainingProgramRepository {
    return remember<TrainingProgramRepository>(databaseRepo, settingsRepository) {
        var dbPrograms = databaseRepo.getAllTrainingPrograms()
        if (dbPrograms.isEmpty()) {
            databaseRepo.saveTrainingProgram(com.bearbones.kumalift.domain.model.TrainingProgram.DEFAULT_HYPERTROPHY)
            dbPrograms = databaseRepo.getAllTrainingPrograms()
        }
        val activeId = settingsRepository.settings.value.profile.activeProgramId
        val repo = TrainingProgramRepository(
            initialPrograms = dbPrograms,
            initialActiveProgramId = activeId,
            databaseRepo = databaseRepo,
            onActiveProgramChanged = { newId ->
                settingsRepository.updateProfile { it.copy(activeProgramId = newId) }
            }
        )
        repo
    }
}
