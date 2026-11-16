package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.DEFAULT_PROGRAM_ID
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class TrainingProgramRepository(
    initialPrograms: List<TrainingProgram> = listOf(TrainingProgram.DEFAULT_HYPERTROPHY),
    initialActiveProgramId: String = DEFAULT_PROGRAM_ID,
    private val databaseRepo: KumaDatabaseRepository? = null,
    private val onProgramsChanged: ((List<TrainingProgram>) -> Unit)? = null,
    private val onActiveProgramChanged: ((String) -> Unit)? = null
) {
    private val _programs = MutableStateFlow(
        if (initialPrograms.isEmpty()) listOf(TrainingProgram.DEFAULT_HYPERTROPHY) else initialPrograms
    )
    val programs: StateFlow<List<TrainingProgram>> = _programs.asStateFlow()

    private val _activeProgramId = MutableStateFlow(
        if (_programs.value.any { it.id == initialActiveProgramId }) {
            initialActiveProgramId
        } else {
            _programs.value.firstOrNull()?.id ?: DEFAULT_PROGRAM_ID
        }
    )
    val activeProgramId: StateFlow<String> = _activeProgramId.asStateFlow()

    val activeProgram: TrainingProgram
        get() = _programs.value.find { it.id == _activeProgramId.value }
            ?: _programs.value.firstOrNull()
            ?: TrainingProgram.DEFAULT_HYPERTROPHY

    fun setActiveProgramId(id: String) {
        if (_programs.value.any { it.id == id }) {
            _activeProgramId.value = id
            onActiveProgramChanged?.invoke(id)
        }
    }

    fun createProgram(
        name: String,
        description: String = "",
        category: TrainingGoal = TrainingGoal.HYPERTROPHY,
        activeDays: List<Int> = listOf(1, 3, 5),
        targetDaysPerWeek: Int = activeDays.size
    ): TrainingProgram {
        val cleanName = name.trim().ifBlank { "Training Program" }
        val id = IdGenerator.nextId("prog")
        val cleanDays = activeDays.filter { it in 1..7 }.distinct().take(3).ifEmpty { listOf(1, 3, 5) }.sorted()
        val newProgram = TrainingProgram(
            id = id,
            name = cleanName,
            description = description.trim(),
            category = category,
            targetDaysPerWeek = cleanDays.size,
            activeDays = cleanDays,
            isDefault = false,
            createdAtEpochMs = currentEpochMillis()
        )

        _programs.update { it + newProgram }
        databaseRepo?.saveTrainingProgram(newProgram)
        onProgramsChanged?.invoke(_programs.value)
        return newProgram
    }

    fun updateProgram(program: TrainingProgram) {
        val cleanDays = program.activeDays.filter { it in 1..7 }.distinct().take(3).ifEmpty { listOf(1, 3, 5) }.sorted()
        val normalized = program.copy(
            activeDays = cleanDays,
            targetDaysPerWeek = cleanDays.size
        )
        _programs.update { list ->
            list.map { if (it.id == normalized.id) normalized else it }
        }
        databaseRepo?.saveTrainingProgram(normalized)
        onProgramsChanged?.invoke(_programs.value)
    }

    fun deleteProgram(programId: String, routineRepo: RoutineRepository? = null): Boolean {
        if (_programs.value.size <= 1) {
            return false // Cannot delete the only remaining program
        }

        _programs.update { list ->
            list.filterNot { it.id == programId }
        }

        // Delete associated routines
        routineRepo?.deleteRoutinesForProgram(programId)
        databaseRepo?.deleteTrainingProgram(programId)

        // Switch active program if the deleted one was active
        if (_activeProgramId.value == programId) {
            val fallback = _programs.value.firstOrNull()?.id ?: DEFAULT_PROGRAM_ID
            _activeProgramId.value = fallback
            onActiveProgramChanged?.invoke(fallback)
        }

        onProgramsChanged?.invoke(_programs.value)
        return true
    }

    fun getProgramById(id: String): TrainingProgram? {
        return _programs.value.find { it.id == id }
    }
}
