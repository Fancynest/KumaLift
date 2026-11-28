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

    @Test
    fun testWorkoutLinkageOnFinishCompletesExactEvent() {
        val scheduleRepo = WorkoutScheduleRepository(databaseRepo = null)
        val prRepo = PersonalRecordRepository(initialPrs = emptyList())
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            scheduleRepository = scheduleRepo
        )

        val nov24 = LocalDate(2026, 11, 24)
        val event1 = scheduleRepo.scheduleRoutine(
            routineId = "rt_squat",
            routineTitle = "Leg Day Heavy",
            scheduledDate = nov24
        )
        val event2 = scheduleRepo.scheduleRoutine(
            routineId = "rt_core",
            routineTitle = "Core Session",
            scheduledDate = nov24
        )

        val routine = com.bearbones.kumalift.domain.model.RoutineBlueprint(
            id = "rt_squat",
            title = "Leg Day Heavy",
            splitCategory = "Legs",
            muscleFocus = "Quadriceps · Hamstrings"
        )

        // Start workout linked to event1
        workoutRepo.startWorkoutFromRoutine(routine, scheduledEventId = event1.id)
        val active = workoutRepo.activeWorkout.value
        assertNotNull(active)
        assertEquals(event1.id, active.scheduledEventId)

        // Finish workout
        val finished = workoutRepo.finishWorkout()
        assertNotNull(finished)

        // Verify event1 is completed and linked to finished.id
        val ev1After = scheduleRepo.events.value.first { it.id == event1.id }
        assertTrue(ev1After.isCompleted)
        assertEquals(finished.id, ev1After.linkedWorkoutId)

        // Verify event2 on the SAME date remains PLANNED and UNTOUCHED
        val ev2After = scheduleRepo.events.value.first { it.id == event2.id }
        assertTrue(ev2After.isPlanned)
        assertEquals(null, ev2After.linkedWorkoutId)
    }

    @Test
    fun testCancelWorkoutLeavesScheduledEventPlanned() {
        val scheduleRepo = WorkoutScheduleRepository(databaseRepo = null)
        val prRepo = PersonalRecordRepository(initialPrs = emptyList())
        val workoutRepo = WorkoutRepository(
            prRepository = prRepo,
            scheduleRepository = scheduleRepo
        )

        val date = LocalDate(2026, 11, 25)
        val event = scheduleRepo.scheduleRoutine(
            routineId = "rt_bench",
            routineTitle = "Push Day",
            scheduledDate = date
        )

        val routine = com.bearbones.kumalift.domain.model.RoutineBlueprint(
            id = "rt_bench",
            title = "Push Day",
            splitCategory = "Push",
            muscleFocus = "Chest · Triceps · Delts"
        )

        workoutRepo.startWorkoutFromRoutine(routine, scheduledEventId = event.id)
        workoutRepo.cancelWorkout()

        // Event must remain PLANNED
        val evAfter = scheduleRepo.events.value.first { it.id == event.id }
        assertTrue(evAfter.isPlanned)
        assertEquals(null, evAfter.linkedWorkoutId)
    }

    @Test
    fun testUpdateEventNotesUpdatesAndClearsNotes() {
        val repo = WorkoutScheduleRepository(databaseRepo = null)
        val date = LocalDate(2026, 11, 28)
        val event = repo.scheduleRoutine(
            routineId = "rt_notes",
            routineTitle = "Upper Day",
            scheduledDate = date,
            notes = "Initial note"
        )
        assertEquals("Initial note", repo.getEventsForDate(date).first().notes)

        repo.updateEventNotes(event.id, "  Focus on pause reps  ")
        assertEquals("Focus on pause reps", repo.getEventsForDate(date).first().notes)

        repo.updateEventNotes(event.id, "   ")
        assertEquals(null, repo.getEventsForDate(date).first().notes)
    }
}
