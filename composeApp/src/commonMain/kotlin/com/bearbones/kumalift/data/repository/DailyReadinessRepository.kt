package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.logging.KumaLogger
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.DailyReadinessCheckIn
import com.bearbones.kumalift.domain.model.ReadinessRecommendation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class DailyReadinessRepository(
    private val databaseRepo: KumaDatabaseRepository? = null,
    initialCheckIns: List<DailyReadinessCheckIn> = emptyList()
) {
    private val _allCheckIns = MutableStateFlow(
        initialCheckIns.ifEmpty { databaseRepo?.getAllDailyReadiness() ?: emptyList() }
    )
    val allCheckIns: StateFlow<List<DailyReadinessCheckIn>> = _allCheckIns.asStateFlow()

    private val _todayCheckIn = MutableStateFlow<DailyReadinessCheckIn?>(
        _allCheckIns.value.firstOrNull { it.dateString == DailyReadinessCheckIn.todayDateString() }
    )
    val todayCheckIn: StateFlow<DailyReadinessCheckIn?> = _todayCheckIn.asStateFlow()

    fun recordCheckIn(
        sleep: Int,
        soreness: Int,
        energy: Int,
        stress: Int,
        notes: String = "",
        dateString: String = DailyReadinessCheckIn.todayDateString()
    ): DailyReadinessCheckIn {
        val (percentage, recommendation) = DailyReadinessCheckIn.calculate(sleep, soreness, energy, stress)
        val checkIn = DailyReadinessCheckIn(
            id = IdGenerator.nextId("readiness"),
            dateString = dateString,
            sleepScore = sleep.coerceIn(1, 5),
            sorenessScore = soreness.coerceIn(1, 5),
            energyScore = energy.coerceIn(1, 5),
            stressScore = stress.coerceIn(1, 5),
            overallPercentage = percentage,
            recommendation = recommendation,
            notes = notes,
            createdAtEpochMs = currentEpochMillis()
        )

        databaseRepo?.insertOrReplaceDailyReadiness(checkIn)
        _allCheckIns.update { list ->
            listOf(checkIn) + list.filterNot { it.dateString == dateString }
        }
        if (dateString == DailyReadinessCheckIn.todayDateString()) {
            _todayCheckIn.value = checkIn
        }
        KumaLogger.i("DailyReadinessRepository", "Recorded readiness check-in for $dateString: $percentage% ($recommendation)")
        return checkIn
    }

    fun getCheckInForDate(dateString: String): DailyReadinessCheckIn? {
        return _allCheckIns.value.firstOrNull { it.dateString == dateString }
    }

    fun deleteCheckIn(id: String) {
        databaseRepo?.deleteDailyReadinessById(id)
        _allCheckIns.update { list -> list.filterNot { it.id == id } }
        if (_todayCheckIn.value?.id == id) {
            _todayCheckIn.value = null
        }
    }
}
