package com.bearbones.kumalift.features.calendar

import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkoutCalendarLogicTests {

    @Test
    fun testLeapYear() {
        assertTrue(WorkoutCalendarLogic.isLeapYear(2024))
        assertTrue(WorkoutCalendarLogic.isLeapYear(2000))
        assertFalse(WorkoutCalendarLogic.isLeapYear(2026))
        assertFalse(WorkoutCalendarLogic.isLeapYear(2100))
        assertFalse(WorkoutCalendarLogic.isLeapYear(1900))
    }

    @Test
    fun testDaysInMonth() {
        assertEquals(31, WorkoutCalendarLogic.daysInMonth(2026, 1))
        assertEquals(28, WorkoutCalendarLogic.daysInMonth(2026, 2))
        assertEquals(29, WorkoutCalendarLogic.daysInMonth(2024, 2))
        assertEquals(30, WorkoutCalendarLogic.daysInMonth(2026, 4))
        assertEquals(31, WorkoutCalendarLogic.daysInMonth(2026, 12))
    }

    @Test
    fun testMonthTransitions() {
        assertEquals(Pair(2025, 12), WorkoutCalendarLogic.previousMonth(2026, 1))
        assertEquals(Pair(2026, 5), WorkoutCalendarLogic.previousMonth(2026, 6))
        assertEquals(Pair(2027, 1), WorkoutCalendarLogic.nextMonth(2026, 12))
        assertEquals(Pair(2026, 7), WorkoutCalendarLogic.nextMonth(2026, 6))
    }

    @Test
    fun testBuildMonthGridNovember2026MondayStart() {
        // Nov 1, 2026 is Sunday (ISO 7)
        // With MONDAY start: Mon..Sat are leading days (6 days: Oct 26, 27, 28, 29, 30, 31)
        // Nov has 30 days.
        // Total so far = 6 + 30 = 36. Remainder = 36 % 7 = 1.
        // Trailing days needed = 6 (Dec 1, 2, 3, 4, 5, 6).
        // Total cells = 42 (6 weeks).
        val today = LocalDate(2026, 11, 20)
        val selected = LocalDate(2026, 11, 18)
        val grid = WorkoutCalendarLogic.buildMonthGrid(
            year = 2026,
            month = 11,
            firstDayOfWeek = FirstDayOfWeek.MONDAY,
            today = today,
            selectedDate = selected,
            workoutsByDate = emptyMap()
        )

        assertEquals(6, grid.size)
        val firstRow = grid[0]
        assertEquals(LocalDate(2026, 10, 26), firstRow[0].date)
        assertFalse(firstRow[0].isCurrentMonth)
        assertEquals(LocalDate(2026, 11, 1), firstRow[6].date)
        assertTrue(firstRow[6].isCurrentMonth)

        // Find today cell
        val todayCell = grid.flatten().first { it.date == today }
        assertTrue(todayCell.isToday)
        assertFalse(todayCell.isSelected)

        // Find selected cell
        val selectedCell = grid.flatten().first { it.date == selected }
        assertFalse(selectedCell.isToday)
        assertTrue(selectedCell.isSelected)

        // Last cell
        val lastCell = grid.last().last()
        assertEquals(LocalDate(2026, 12, 6), lastCell.date)
        assertFalse(lastCell.isCurrentMonth)
    }

    @Test
    fun testBuildMonthGridSundayStart() {
        // Nov 1, 2026 is Sunday. With SUNDAY start, Nov 1 is the very first cell!
        val today = LocalDate(2026, 11, 20)
        val grid = WorkoutCalendarLogic.buildMonthGrid(
            year = 2026,
            month = 11,
            firstDayOfWeek = FirstDayOfWeek.SUNDAY,
            today = today,
            selectedDate = today,
            workoutsByDate = emptyMap()
        )

        val firstRow = grid[0]
        assertEquals(LocalDate(2026, 11, 1), firstRow[0].date)
        assertTrue(firstRow[0].isCurrentMonth)
    }

    @Test
    fun testGroupWorkoutsByLocalDateAndFilterUnfinished() {
        val tz = TimeZone.of("UTC")
        // 2026-11-18 10:00:00 UTC -> 1794996000000L
        val epoch1 = 1794996000000L // Nov 18, 2026
        val epoch2 = epoch1 + 3600_000L // Nov 18, 2026 (second session)
        val epoch3 = epoch1 + 86400_000L // Nov 19, 2026

        val finished1 = Workout(
            id = "w1",
            title = "Legs & Core",
            startTimeEpochMs = epoch1,
            durationSeconds = 3600,
            isFinished = true
        )
        val finished2 = Workout(
            id = "w2",
            title = "Evening Mobility",
            startTimeEpochMs = epoch2,
            durationSeconds = 1800,
            isFinished = true
        )
        val unfinished = Workout(
            id = "w3",
            title = "Active Workout",
            startTimeEpochMs = epoch2 + 1000L,
            isFinished = false
        )
        val finishedNextDay = Workout(
            id = "w4",
            title = "Upper Body",
            startTimeEpochMs = epoch3,
            durationSeconds = 4000,
            isFinished = true
        )

        val map = WorkoutCalendarLogic.groupWorkoutsByLocalDate(
            listOf(finished1, finished2, unfinished, finishedNextDay),
            tz
        )

        val nov18 = LocalDate(2026, 11, 18)
        val nov19 = LocalDate(2026, 11, 19)

        assertEquals(2, map.size)
        assertEquals(2, map[nov18]?.size)
        assertEquals(listOf("w1", "w2"), map[nov18]?.map { it.id })
        assertEquals(1, map[nov19]?.size)
        assertEquals("w4", map[nov19]?.first()?.id)
    }

    @Test
    fun testCalculateMonthlyStats() {
        val nov18 = LocalDate(2026, 11, 18)
        val nov19 = LocalDate(2026, 11, 19)
        val dec1 = LocalDate(2026, 12, 1)

        val ex1 = WorkoutExercise(
            id = "e1",
            exercise = Exercise(
                id = "ex_squat",
                name = "Squat",
                primaryMuscle = MuscleGroup.LEGS,
                equipment = Equipment.BARBELL
            ),
            sets = listOf(
                SetLog(id = "s1", setNumber = 1, weightKg = 100.0, reps = 10, isCompleted = true),
                SetLog(id = "s2", setNumber = 2, weightKg = 100.0, reps = 10, isCompleted = true)
            )
        )
        val w1 = Workout(
            id = "w1",
            title = "Squat Day",
            startTimeEpochMs = 1000L,
            durationSeconds = 3000L,
            exercises = listOf(ex1),
            isFinished = true
        )
        val w2 = Workout(
            id = "w2",
            title = "Evening Stretch",
            startTimeEpochMs = 2000L,
            durationSeconds = 1200L,
            isFinished = true
        )
        val w3 = Workout(
            id = "w3",
            title = "Upper Body",
            startTimeEpochMs = 3000L,
            durationSeconds = 3600L,
            isFinished = true
        )
        val wDec = Workout(
            id = "w4",
            title = "Dec Workout",
            startTimeEpochMs = 4000L,
            durationSeconds = 2000L,
            isFinished = true
        )

        val grouped = mapOf(
            nov18 to listOf(w1, w2),
            nov19 to listOf(w3),
            dec1 to listOf(wDec)
        )

        val statsNov = WorkoutCalendarLogic.calculateMonthlyStats(2026, 11, grouped)

        assertEquals(2, statsNov.uniqueWorkoutDays, "Unique workout days in Nov should be 2")
        assertEquals(3, statsNov.totalSessions, "Total sessions in Nov should be 3")
        assertEquals(7800L, statsNov.totalDurationSeconds, "Duration should sum Nov sessions")
        assertEquals(2000.0, statsNov.totalVolumeKg, "Volume should sum Nov workouts")
    }
}
