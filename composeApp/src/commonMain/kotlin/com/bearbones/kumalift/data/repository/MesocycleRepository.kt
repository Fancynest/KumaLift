package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.MesocycleBlock
import com.bearbones.kumalift.domain.model.MesocyclePhase
import com.bearbones.kumalift.domain.model.MesocycleStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MesocycleRepository(
    private val databaseRepo: KumaDatabaseRepository? = null
) {
    private val _activeMesocycle = MutableStateFlow<MesocycleBlock?>(null)
    val activeMesocycle: StateFlow<MesocycleBlock?> = _activeMesocycle.asStateFlow()

    private val _allMesocycles = MutableStateFlow<List<MesocycleBlock>>(emptyList())
    val allMesocycles: StateFlow<List<MesocycleBlock>> = _allMesocycles.asStateFlow()

    init {
        restoreFromDatabase()
    }

    private fun restoreFromDatabase() {
        if (databaseRepo == null) return
        try {
            val list = databaseRepo.getAllMesocycles()
            _allMesocycles.value = list
            _activeMesocycle.value = list.firstOrNull { it.status == MesocycleStatus.ACTIVE }
            KumaLogger.i("MesocycleRepository", "Restored ${list.size} mesocycles from DB, active=${_activeMesocycle.value?.name}")
        } catch (t: Throwable) {
            KumaLogger.w("MesocycleRepository", "Failed restoring mesocycles: ${t.message}")
        }
    }

    fun startNewMesocycle(
        name: String,
        totalWeeks: Int = 5,
        programId: String? = null,
        deloadWeek: Int = totalWeeks,
        notes: String = ""
    ): MesocycleBlock {
        val now = currentEpochMillis()
        // If an existing mesocycle is active, complete it first
        val currentActive = _activeMesocycle.value
        if (currentActive != null && currentActive.status == MesocycleStatus.ACTIVE) {
            completeMesocycle(currentActive.id)
        }

        val safeTotalWeeks = totalWeeks.coerceIn(2, 16)
        val safeDeloadWeek = deloadWeek.coerceIn(2, safeTotalWeeks)

        val newBlock = MesocycleBlock(
            id = IdGenerator.nextId("meso"),
            name = name.ifBlank { "Mesocycle ${_allMesocycles.value.size + 1}" },
            programId = programId,
            totalWeeks = safeTotalWeeks,
            currentWeek = 1,
            deloadWeek = safeDeloadWeek,
            isDeloadActive = false,
            startDateEpochMs = now,
            endDateEpochMs = null,
            status = MesocycleStatus.ACTIVE,
            notes = notes,
            createdAtEpochMs = now
        )

        _allMesocycles.update { listOf(newBlock) + it }
        _activeMesocycle.value = newBlock

        databaseRepo?.insertOrReplaceMesocycle(newBlock)
        KumaLogger.i("MesocycleRepository", "Started new mesocycle: ${newBlock.name} ($safeTotalWeeks wks)")
        return newBlock
    }

    fun advanceWeek(): MesocycleBlock? {
        val current = _activeMesocycle.value ?: return null
        if (current.currentWeek < current.totalWeeks) {
            val nextWeek = current.currentWeek + 1
            val isDeloadNow = nextWeek >= current.deloadWeek
            val updated = current.copy(
                currentWeek = nextWeek,
                isDeloadActive = isDeloadNow
            )
            updateBlock(updated)
            databaseRepo?.updateMesocycleWeek(current.id, nextWeek, isDeloadNow)
            KumaLogger.i("MesocycleRepository", "Advanced mesocycle to week $nextWeek (deload=$isDeloadNow)")
            return updated
        } else {
            // Completed all weeks in mesocycle
            return completeMesocycle(current.id)
        }
    }

    fun toggleDeload(forceActive: Boolean? = null): MesocycleBlock? {
        val current = _activeMesocycle.value ?: return null
        val targetActive = forceActive ?: !current.isDeloadActive
        val updated = current.copy(isDeloadActive = targetActive)
        updateBlock(updated)
        databaseRepo?.updateMesocycleWeek(current.id, current.currentWeek, targetActive)
        KumaLogger.i("MesocycleRepository", "Toggled deload active=$targetActive for ${current.name}")
        return updated
    }

    fun completeMesocycle(id: String): MesocycleBlock? {
        val now = currentEpochMillis()
        val target = _allMesocycles.value.firstOrNull { it.id == id } ?: return null
        val completed = target.copy(
            status = MesocycleStatus.COMPLETED,
            endDateEpochMs = now,
            isDeloadActive = false
        )
        updateBlock(completed)
        if (_activeMesocycle.value?.id == id) {
            _activeMesocycle.value = null
        }
        databaseRepo?.updateMesocycleStatus(id, MesocycleStatus.COMPLETED, now)
        KumaLogger.i("MesocycleRepository", "Completed mesocycle ${target.name}")
        return completed
    }

    fun deleteMesocycle(id: String) {
        _allMesocycles.update { it.filterNot { b -> b.id == id } }
        if (_activeMesocycle.value?.id == id) {
            _activeMesocycle.value = null
        }
        databaseRepo?.deleteMesocycle(id)
        KumaLogger.i("MesocycleRepository", "Deleted mesocycle id=$id")
    }

    fun isDeloadActive(): Boolean {
        return _activeMesocycle.value?.phase == MesocyclePhase.DELOAD
    }

    private fun updateBlock(block: MesocycleBlock) {
        _allMesocycles.update { list ->
            list.map { if (it.id == block.id) block else it }
        }
        if (_activeMesocycle.value?.id == block.id) {
            _activeMesocycle.value = if (block.status == MesocycleStatus.ACTIVE) block else null
        }
    }

    fun clearAll() {
        _activeMesocycle.value = null
        _allMesocycles.value = emptyList()
        databaseRepo?.clearAllMesocycles()
    }
}
