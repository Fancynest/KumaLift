package com.bearbones.kumalift.features.calendar

import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.Workout
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class CalendarDayCellData(
    val date: LocalDate,
    val isCurrentMonth: Boolean,
    val isToday: Boolean,
    val isSelected: Boolean,
    val workouts: List<Workout> = emptyList()
) {
    val hasCompletedWorkout: Boolean get() = workouts.isNotEmpty()
    val completedCount: Int get() = workouts.size
}

data class MonthlyWorkoutStats(
    val uniqueWorkoutDays: Int,
    val totalSessions: Int,
    val totalDurationSeconds: Long,
    val totalVolumeKg: Double
)

object WorkoutCalendarLogic {

    fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }

    fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            2 -> if (isLeapYear(year)) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
    }

    fun previousMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 1) Pair(year - 1, 12) else Pair(year, month - 1)
    }

    fun nextMonth(year: Int, month: Int): Pair<Int, Int> {
        return if (month == 12) Pair(year + 1, 1) else Pair(year, month + 1)
    }

    fun groupWorkoutsByLocalDate(
        workouts: List<Workout>,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Map<LocalDate, List<Workout>> {
        val finished = workouts.filter { it.isFinished }
        val map = mutableMapOf<LocalDate, MutableList<Workout>>()
        for (w in finished) {
            val localDate = Instant.fromEpochMilliseconds(w.startTimeEpochMs)
                .toLocalDateTime(timeZone)
                .date
            map.getOrPut(localDate) { mutableListOf() }.add(w)
        }
        return map
    }

    fun buildMonthGrid(
        year: Int,
        month: Int,
        firstDayOfWeek: FirstDayOfWeek,
        today: LocalDate,
        selectedDate: LocalDate,
        workoutsByDate: Map<LocalDate, List<Workout>>
    ): List<List<CalendarDayCellData>> {
        val firstDayOfMonth = LocalDate(year, month, 1)
        val firstDayIso = firstDayOfMonth.dayOfWeek.ordinal + 1 // 1 = Mon, ..., 7 = Sun

        val leadingDaysCount = when (firstDayOfWeek) {
            FirstDayOfWeek.MONDAY -> (firstDayIso - 1) % 7
            FirstDayOfWeek.SUNDAY -> firstDayIso % 7
            FirstDayOfWeek.SATURDAY -> (firstDayIso + 1) % 7
        }

        val cells = mutableListOf<CalendarDayCellData>()

        // Leading days from previous month
        if (leadingDaysCount > 0) {
            val (prevYear, prevMonth) = previousMonth(year, month)
            val prevMonthDaysCount = daysInMonth(prevYear, prevMonth)
            val startDay = prevMonthDaysCount - leadingDaysCount + 1
            for (day in startDay..prevMonthDaysCount) {
                val date = LocalDate(prevYear, prevMonth, day)
                cells.add(
                    CalendarDayCellData(
                        date = date,
                        isCurrentMonth = false,
                        isToday = (date == today),
                        isSelected = (date == selectedDate),
                        workouts = workoutsByDate[date] ?: emptyList()
                    )
                )
            }
        }

        // Current month days
        val currentMonthDaysCount = daysInMonth(year, month)
        for (day in 1..currentMonthDaysCount) {
            val date = LocalDate(year, month, day)
            cells.add(
                CalendarDayCellData(
                    date = date,
                    isCurrentMonth = true,
                    isToday = (date == today),
                    isSelected = (date == selectedDate),
                    workouts = workoutsByDate[date] ?: emptyList()
                )
            )
        }

        // Trailing days from next month to complete the week grid
        val remainder = cells.size % 7
        if (remainder != 0) {
            val trailingDaysCount = 7 - remainder
            val (nextYear, nextMonth) = nextMonth(year, month)
            for (day in 1..trailingDaysCount) {
                val date = LocalDate(nextYear, nextMonth, day)
                cells.add(
                    CalendarDayCellData(
                        date = date,
                        isCurrentMonth = false,
                        isToday = (date == today),
                        isSelected = (date == selectedDate),
                        workouts = workoutsByDate[date] ?: emptyList()
                    )
                )
            }
        }

        return cells.chunked(7)
    }

    fun calculateMonthlyStats(
        year: Int,
        month: Int,
        workoutsByDate: Map<LocalDate, List<Workout>>
    ): MonthlyWorkoutStats {
        val monthDates = workoutsByDate.filter { (date, workouts) ->
            date.year == year && date.monthNumber == month && workouts.isNotEmpty()
        }
        val uniqueDays = monthDates.size
        val allWorkouts = monthDates.values.flatten()
        val totalSessions = allWorkouts.size
        val totalDurationSeconds = allWorkouts.sumOf { it.durationSeconds }
        val totalVolumeKg = allWorkouts.sumOf { it.totalVolumeKg }

        return MonthlyWorkoutStats(
            uniqueWorkoutDays = uniqueDays,
            totalSessions = totalSessions,
            totalDurationSeconds = totalDurationSeconds,
            totalVolumeKg = totalVolumeKg
        )
    }
}
