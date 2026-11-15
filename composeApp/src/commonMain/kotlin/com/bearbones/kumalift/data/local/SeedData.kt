package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.PersonalRecord
import com.bearbones.kumalift.domain.model.RoutineBlueprint

object SeedData {

    /**
     * Sourced 100% exclusively from exercises-dataset/data/exercises.json (1,324 exercises).
     * No data from database-latihan-fitnes.md is used.
     */
    val defaultExercises: List<Exercise> = ExerciseDataset.allExercises

    private fun ex(idOrQuery: String): Exercise {
        return defaultExercises.firstOrNull { it.id == idOrQuery }
            ?: defaultExercises.firstOrNull {
                val clean = idOrQuery.removePrefix("ex_").replace("_", " ")
                it.name.contains(clean, ignoreCase = true)
            }
            ?: defaultExercises.first()
    }

    val defaultRoutines: List<RoutineBlueprint> = emptyList()

    // Real empty PRs on fresh installation (no invented/mock data per project rules)
    val personalRecords: List<PersonalRecord> = emptyList()
}
