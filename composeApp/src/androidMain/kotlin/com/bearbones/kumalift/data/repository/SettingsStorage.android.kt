package com.bearbones.kumalift.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.bearbones.kumalift.data.local.SeedData

@Composable
actual fun rememberSettingsRepository(): SettingsRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val store = AndroidSettingsStore(context)
        SettingsRepository(
            initialSettings = store.load(),
            onSave = { updated -> store.save(updated) }
        )
    }
}

@Composable
actual fun rememberExerciseRepository(): ExerciseRepository {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        val store = AndroidExerciseStore(context)
        val savedCustom = store.loadCustomExercises()
        ExerciseRepository(
            initialExercises = savedCustom + SeedData.defaultExercises,
            onSaveCustom = { list -> store.saveCustomExercises(list) }
        )
    }
}

@Composable
actual fun rememberRoutineRepository(exerciseRepository: ExerciseRepository): RoutineRepository {
    val context = LocalContext.current.applicationContext
    return remember(context, exerciseRepository) {
        val store = AndroidRoutineStore(context, exerciseRepository)
        val savedCustom = store.loadCustomRoutines()
        RoutineRepository(
            initialRoutines = savedCustom + SeedData.defaultRoutines,
            onSave = { list -> store.saveCustomRoutines(list) }
        )
    }
}
