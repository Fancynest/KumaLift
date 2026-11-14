package com.bearbones.kumalift.features.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup

/**
 * Single source of truth for all exercise catalog filters (Requirement 9.A).
 *
 * Implements:
 * - Single state holder for query, muscle, equipment, and movement type.
 * - Robust Saver compatible with enums and nullable enums (Requirement 9.D).
 * - Safe fallback for unrecognized or deprecated enum strings to prevent crashes.
 */
class LibraryFilterState(
    initialSearchQuery: String = "",
    initialMuscle: MuscleGroup? = null,
    initialEquipment: Equipment? = null,
    initialMovementType: MovementType? = null
) {
    var searchQuery: String by mutableStateOf(initialSearchQuery)
    var selectedMuscle: MuscleGroup? by mutableStateOf(initialMuscle)
    var selectedEquipment: Equipment? by mutableStateOf(initialEquipment)
    var selectedMovementType: MovementType? by mutableStateOf(initialMovementType)

    val isActive: Boolean
        get() = searchQuery.isNotBlank() ||
            selectedMuscle != null ||
            selectedEquipment != null ||
            selectedMovementType != null

    fun reset() {
        searchQuery = ""
        selectedMuscle = null
        selectedEquipment = null
        selectedMovementType = null
    }

    fun matches(exercise: Exercise): Boolean {
        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() ||
            exercise.name.contains(query, ignoreCase = true) ||
            exercise.primaryMuscle.displayName.contains(query, ignoreCase = true) ||
            exercise.subMuscle.contains(query, ignoreCase = true) ||
            exercise.equipment.displayName.contains(query, ignoreCase = true) ||
            exercise.equipment.code.contains(query, ignoreCase = true) ||
            exercise.movementType.displayName.contains(query, ignoreCase = true)

        val matchesMuscle = selectedMuscle == null || exercise.primaryMuscle == selectedMuscle
        val matchesEquipment = selectedEquipment == null || exercise.equipment == selectedEquipment
        val matchesMovementType = selectedMovementType == null || exercise.movementType == selectedMovementType

        return matchesQuery && matchesMuscle && matchesEquipment && matchesMovementType
    }

    companion object {
        val Saver: Saver<LibraryFilterState, Any> = listSaver(
            save = { state ->
                listOf(
                    state.searchQuery,
                    state.selectedMuscle?.name,
                    state.selectedEquipment?.name,
                    state.selectedMovementType?.name
                )
            },
            restore = { list ->
                val query = list.getOrNull(0) as? String ?: ""
                val muscleName = list.getOrNull(1) as? String
                val equipmentName = list.getOrNull(2) as? String
                val movementTypeName = list.getOrNull(3) as? String

                // Requirement 9.D: Safely restore enums by name, guarding against missing/deprecated entries
                val muscle = muscleName?.let { name ->
                    runCatching { MuscleGroup.valueOf(name) }.getOrNull()
                }
                val equipment = equipmentName?.let { name ->
                    runCatching { Equipment.valueOf(name) }.getOrNull()
                }
                val movementType = movementTypeName?.let { name ->
                    runCatching { MovementType.valueOf(name) }.getOrNull()
                }

                LibraryFilterState(
                    initialSearchQuery = query,
                    initialMuscle = muscle,
                    initialEquipment = equipment,
                    initialMovementType = movementType
                )
            }
        )
    }
}

/**
 * Creates and remembers a [LibraryFilterState] with process-recreation survival via [rememberSaveable].
 */
@Composable
fun rememberLibraryFilterState(): LibraryFilterState {
    return rememberSaveable(saver = LibraryFilterState.Saver) {
        LibraryFilterState()
    }
}
