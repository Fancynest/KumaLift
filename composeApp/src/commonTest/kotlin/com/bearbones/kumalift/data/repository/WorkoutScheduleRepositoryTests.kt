package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.domain.model.ScheduleStatus
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WorkoutScheduleRepositoryTests {

    @Test
    fun testScheduleRoutineInMemory() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val date = LocalDate(2026, 11, 24)

        val event = repo.scheduleRoutine(
            routineId = "routine_push",
            routineTitle = "Push Day",
            splitType = "Upper",
            scheduledDate = date,
            notes = "Focus on bench press"
        )

        assertNotNull(event.id)
        assertEquals("routine_push", event.routineId)
        assertEquals("2026-11-24", event.scheduledDate)
        assertEquals(ScheduleStatus.PLANNED, event.status)
        assertEquals("Push Day", event.routineTitleSnapshot)
        assertEquals("Upper", event.splitTypeSnapshot)
        assertEquals("Focus on bench press", event.notes)
        assertTrue(event.isPlanned)
        assertFalse(event.isCompleted)
        assertFalse(event.isCancelled)

        val eventsForDate = repo.getEventsForDate(date)
        assertEquals(1, eventsForDate.size)
        assertEquals(event.id, eventsForDate.first().id)
    }

    @Test
    fun testRescheduleEvent() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val nov24 = LocalDate(2026, 11, 24)
        val nov26 = LocalDate(2026, 11, 26)

        val event = repo.scheduleRoutine(
            routineId = "routine_legs",
            routineTitle = "Leg Day",
            scheduledDate = nov24
        )

        assertEquals(1, repo.getEventsForDate(nov24).size)
        assertEquals(0, repo.getEventsForDate(nov26).size)

        repo.rescheduleEvent(event.id, nov26)

        assertEquals(0, repo.getEventsForDate(nov24).size)
        val rescheduled = repo.getEventsForDate(nov26)
        assertEquals(1, rescheduled.size)
        assertEquals("2026-11-26", rescheduled.first().scheduledDate)
    }

    @Test
    fun testCancelAndRestoreEvent() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val date = LocalDate(2026, 11, 25)

        val event = repo.scheduleRoutine(
            routineId = "routine_pull",
            routineTitle = "Pull Day",
            scheduledDate = date
        )

        assertTrue(repo.getEventsForDate(date).first().isPlanned)

        repo.cancelEvent(event.id)
        val cancelled = repo.getEventsForDate(date).first()
        assertTrue(cancelled.isCancelled)
        assertFalse(cancelled.isPlanned)

        repo.uncancelEvent(event.id)
        val restored = repo.getEventsForDate(date).first()
        assertTrue(restored.isPlanned)
        assertFalse(restored.isCancelled)
    }

    @Test
    fun testCompleteEvent() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val date = LocalDate(2026, 11, 25)

        val event = repo.scheduleRoutine(
            routineId = "routine_arms",
            routineTitle = "Arm Day",
            scheduledDate = date
        )

        repo.completeEvent(event.id, linkedWorkoutId = "workout_123")

        val completed = repo.getEventsForDate(date).first()
        assertTrue(completed.isCompleted)
        assertEquals("workout_123", completed.linkedWorkoutId)
    }

    @Test
    fun testDeleteEvent() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val date = LocalDate(2026, 11, 25)

        val event = repo.scheduleRoutine(
            routineId = "routine_cardio",
            routineTitle = "Cardio",
            scheduledDate = date
        )

        assertEquals(1, repo.events.value.size)
        repo.deleteEvent(event.id)
        assertEquals(0, repo.events.value.size)
    }
}
