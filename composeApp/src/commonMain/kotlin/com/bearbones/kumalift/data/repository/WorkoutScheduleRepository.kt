package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.local.KumaDatabaseRepository
import com.bearbones.kumalift.domain.model.ScheduleStatus
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class WorkoutScheduleRepository(
    private val databaseRepo: KumaDatabaseRepository? = null
) {
    private val _events = MutableStateFlow<List<WorkoutScheduleEvent>>(emptyList())
    val events: StateFlow<List<WorkoutScheduleEvent>> = _events.asStateFlow()

    init {
        val loaded = databaseRepo?.getAllScheduleEvents() ?: emptyList()
        _events.value = loaded
    }

    fun setEventsForTesting(testEvents: List<WorkoutScheduleEvent>) {
        _events.value = testEvents
    }

    fun scheduleRoutine(
        routineId: String?,
        routineTitle: String,
        splitType: String? = null,
        scheduledDate: LocalDate,
        notes: String? = null
    ): WorkoutScheduleEvent {
        val now = currentEpochMillis()
        val event = WorkoutScheduleEvent(
            id = IdGenerator.nextId("sched"),
            routineId = routineId,
            scheduledDate = scheduledDate.toString(),
            status = ScheduleStatus.PLANNED,
            linkedWorkoutId = null,
            routineTitleSnapshot = routineTitle,
            splitTypeSnapshot = splitType,
            notes = notes?.trim()?.ifBlank { null },
            createdAtEpochMs = now,
            updatedAtEpochMs = now
        )
        databaseRepo?.insertScheduleEvent(event)
        _events.update { current -> current + event }
        return event
    }

    fun rescheduleEvent(eventId: String, newDate: LocalDate) {
        val now = currentEpochMillis()
        val dateStr = newDate.toString()
        databaseRepo?.updateScheduleEventDate(eventId, dateStr, now)
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) ev.copy(scheduledDate = dateStr, updatedAtEpochMs = now) else ev
            }
        }
    }

    fun cancelEvent(eventId: String) {
        val now = currentEpochMillis()
        databaseRepo?.updateScheduleEventStatus(eventId, ScheduleStatus.CANCELLED, null, now)
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) ev.copy(status = ScheduleStatus.CANCELLED, updatedAtEpochMs = now) else ev
            }
        }
    }

    fun uncancelEvent(eventId: String) {
        val now = currentEpochMillis()
        databaseRepo?.updateScheduleEventStatus(eventId, ScheduleStatus.PLANNED, null, now)
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) ev.copy(status = ScheduleStatus.PLANNED, updatedAtEpochMs = now) else ev
            }
        }
    }

    fun completeEvent(eventId: String, linkedWorkoutId: String) {
        val now = currentEpochMillis()
        databaseRepo?.updateScheduleEventStatus(eventId, ScheduleStatus.COMPLETED, linkedWorkoutId, now)
        _events.update { list ->
            list.map { ev ->
                if (ev.id == eventId) {
                    ev.copy(
                        status = ScheduleStatus.COMPLETED,
                        linkedWorkoutId = linkedWorkoutId,
                        updatedAtEpochMs = now
                    )
                } else ev
            }
        }
    }

    fun deleteEvent(eventId: String) {
        databaseRepo?.deleteScheduleEvent(eventId)
        _events.update { list -> list.filterNot { it.id == eventId } }
    }

    fun getEventsForDate(date: LocalDate): List<WorkoutScheduleEvent> {
        val dateStr = date.toString()
        return _events.value.filter { it.scheduledDate == dateStr }
    }

    fun getEventsForMonth(year: Int, month: Int): List<WorkoutScheduleEvent> {
        return _events.value.filter { ev ->
            val ld = ev.localDate ?: return@filter false
            ld.year == year && ld.monthNumber == month
        }
    }
}
