package com.bearbones.kumalift.features.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import com.bearbones.kumalift.domain.model.localizedTitle
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun WorkoutCalendarSection(
    completedWorkouts: List<Workout>,
    firstDayOfWeek: FirstDayOfWeek,
    weightUnit: WeightUnit,
    scheduledEvents: List<WorkoutScheduleEvent> = emptyList(),
    routines: List<RoutineBlueprint> = emptyList(),
    onScheduleRoutine: ((routine: RoutineBlueprint, date: LocalDate, notes: String?) -> Unit)? = null,
    onRescheduleEvent: ((eventId: String, newDate: LocalDate) -> Unit)? = null,
    onCancelEvent: ((eventId: String) -> Unit)? = null,
    onRestoreEvent: ((eventId: String) -> Unit)? = null,
    onDeleteEvent: ((eventId: String) -> Unit)? = null,
    onStartScheduledWorkout: ((eventId: String, routineId: String) -> Unit)? = null,
    onStartRoutine: ((routine: RoutineBlueprint) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val nowInstant = remember { Clock.System.now() }
    val timeZone = remember { TimeZone.currentSystemDefault() }
    val today = remember(nowInstant, timeZone) {
        nowInstant.toLocalDateTime(timeZone).date
    }

    var displayedYear by remember(today) { mutableIntStateOf(today.year) }
    var displayedMonth by remember(today) { mutableIntStateOf(today.monthNumber) }
    var selectedDate by remember(today) { mutableStateOf(today) }

    var showScheduleDialog by remember { mutableStateOf(false) }
    var editingScheduleEvent by remember { mutableStateOf<WorkoutScheduleEvent?>(null) }

    val workoutsByDate = remember(completedWorkouts, timeZone) {
        WorkoutCalendarLogic.groupWorkoutsByLocalDate(completedWorkouts, timeZone)
    }

    val scheduledByDate = remember(scheduledEvents) {
        WorkoutCalendarLogic.groupScheduleEventsByLocalDate(scheduledEvents)
    }

    val routinesByDayOfWeek = remember(routines) {
        val map = mutableMapOf<Int, MutableList<RoutineBlueprint>>()
        for (r in routines) {
            for (day in r.scheduledDays) {
                map.getOrPut(day) { mutableListOf() }.add(r)
            }
        }
        map
    }

    val weeks = remember(
        displayedYear, displayedMonth, firstDayOfWeek, today, selectedDate,
        workoutsByDate, scheduledByDate, routinesByDayOfWeek
    ) {
        WorkoutCalendarLogic.buildMonthGrid(
            year = displayedYear,
            month = displayedMonth,
            firstDayOfWeek = firstDayOfWeek,
            today = today,
            selectedDate = selectedDate,
            workoutsByDate = workoutsByDate,
            scheduledEventsByDate = scheduledByDate,
            routinesByDayOfWeek = routinesByDayOfWeek
        )
    }

    val monthlyStats = remember(displayedYear, displayedMonth, workoutsByDate) {
        WorkoutCalendarLogic.calculateMonthlyStats(displayedYear, displayedMonth, workoutsByDate)
    }

    val dayHeaders = remember(firstDayOfWeek, s) {
        when (firstDayOfWeek) {
            FirstDayOfWeek.MONDAY -> s.daysMondayStart
            FirstDayOfWeek.SUNDAY -> s.daysSundayStart
            FirstDayOfWeek.SATURDAY -> s.daysSaturdayStart
        }
    }

    val monthName = s.monthNamesFull.getOrElse(displayedMonth - 1) { "" }
    val isViewingCurrentMonth = (displayedYear == today.year && displayedMonth == today.monthNumber)
    val isSelectedToday = (selectedDate == today)

    val isLb = weightUnit == WeightUnit.LB
    val unitSuffix = if (isLb) s.unitLb else s.unitKg
    val volMultiplier = if (isLb) 2.20462 else 1.0

    KumaCard(
        modifier = modifier.fillMaxWidth(),
        backgroundColor = colors.surfaceVariant,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Calendar Title and Month Navigation Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = s.workoutCalendarTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                if (!isViewingCurrentMonth || !isSelectedToday) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.accentFill.copy(alpha = 0.15f))
                            .clickable {
                                displayedYear = today.year
                                displayedMonth = today.monthNumber
                                selectedDate = today
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.todayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                fontSize = 11.sp
                            )
                        )
                    }
                }
            }

            // Month Selector Bar (< Month Year >)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        val (prevY, prevM) = WorkoutCalendarLogic.previousMonth(displayedYear, displayedMonth)
                        displayedYear = prevY
                        displayedMonth = prevM
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = s.prevMonth,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Text(
                    text = "$monthName $displayedYear",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                IconButton(
                    onClick = {
                        val (nextY, nextM) = WorkoutCalendarLogic.nextMonth(displayedYear, displayedMonth)
                        displayedYear = nextY
                        displayedMonth = nextM
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = s.nextMonth,
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // 2. Weekday Column Headers
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { header ->
                    Text(
                        text = header.take(3),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // 3. Month Day Grid (7 columns)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { cell ->
                            CalendarDayCell(
                                cell = cell,
                                onClick = {
                                    selectedDate = cell.date
                                    if (cell.date.monthNumber != displayedMonth) {
                                        displayedYear = cell.date.year
                                        displayedMonth = cell.date.monthNumber
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.recoveryFresh)
                    )
                    Text(
                        text = s.completedStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(colors.accentText)
                    )
                    Text(
                        text = s.scheduledStatus,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .border(1.dp, colors.accentText, CircleShape)
                    )
                    Text(
                        text = s.todayLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }

            // 4. Monthly Statistics Strip
            MonthlyStatisticsCard(
                stats = monthlyStats,
                isLb = isLb,
                unitSuffix = unitSuffix,
                volMultiplier = volMultiplier,
                strings = s
            )

            // 5. Selected Date Details
            val selectedWorkouts = workoutsByDate[selectedDate] ?: emptyList()
            val selectedEvents = scheduledByDate[selectedDate] ?: emptyList()
            val selectedDayOfWeekIso = selectedDate.dayOfWeek.ordinal + 1
            val selectedRecurringRoutines = routinesByDayOfWeek[selectedDayOfWeekIso] ?: emptyList()

            SelectedDayDetailCard(
                date = selectedDate,
                workouts = selectedWorkouts,
                scheduledEvents = selectedEvents,
                recurringRoutines = selectedRecurringRoutines,
                isToday = (selectedDate == today),
                isLb = isLb,
                unitSuffix = unitSuffix,
                timeZone = timeZone,
                strings = s,
                onScheduleClick = {
                    editingScheduleEvent = null
                    showScheduleDialog = true
                },
                onEditEvent = { event ->
                    editingScheduleEvent = event
                    showScheduleDialog = true
                },
                onStartScheduledWorkout = onStartScheduledWorkout,
                onStartRoutine = onStartRoutine
            )
        }
    }

    if (showScheduleDialog) {
        WorkoutScheduleDialog(
            initialDate = selectedDate,
            existingEvent = editingScheduleEvent,
            availableRoutines = routines,
            onDismiss = {
                showScheduleDialog = false
                editingScheduleEvent = null
            },
            onSaveSchedule = { routine, targetDate, notes ->
                onScheduleRoutine?.invoke(routine, targetDate, notes)
            },
            onReschedule = { eventId, newDate ->
                onRescheduleEvent?.invoke(eventId, newDate)
            },
            onCancelSchedule = { eventId ->
                onCancelEvent?.invoke(eventId)
            },
            onRestoreSchedule = { eventId ->
                onRestoreEvent?.invoke(eventId)
            },
            onDeleteSchedule = { eventId ->
                onDeleteEvent?.invoke(eventId)
            }
        )
    }
}

@Composable
private fun CalendarDayCell(
    cell: CalendarDayCellData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current

    val backgroundColor = when {
        cell.isSelected -> colors.accentFill
        else -> Color.Transparent
    }

    val contentColor = when {
        cell.isSelected -> colors.onAccentFill
        !cell.isCurrentMonth -> colors.textSecondary.copy(alpha = 0.35f)
        cell.isToday -> colors.accentText
        else -> colors.textPrimary
    }

    val borderModifier = when {
        cell.isSelected -> Modifier
        cell.isToday -> Modifier.border(1.5.dp, colors.accentText, RoundedCornerShape(10.dp))
        else -> Modifier
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(10.dp))
            .then(borderModifier)
            .background(backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${cell.date.dayOfMonth}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (cell.isSelected || cell.isToday) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp,
                    color = contentColor
                )
            )

            // Indicators
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (cell.hasCompletedWorkout) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (cell.isSelected) colors.onAccentFill else colors.recoveryFresh)
                    )
                }
                if (cell.hasScheduledWorkout || cell.hasRecurringRoutine) {
                    Box(
                        modifier = Modifier
                            .size(4.dp)
                            .clip(CircleShape)
                            .background(if (cell.isSelected) colors.onAccentFill else colors.accentText)
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyStatisticsCard(
    stats: MonthlyWorkoutStats,
    isLb: Boolean,
    unitSuffix: String,
    volMultiplier: Double,
    strings: AppStrings
) {
    val colors = LocalKumaColors.current

    val totalVolDisplay = stats.totalVolumeKg * volMultiplier
    val totalVolFormatted = if (totalVolDisplay >= 1000.0) {
        val thousands = ((totalVolDisplay / 100.0).toInt()) / 10.0
        "${thousands}k $unitSuffix"
    } else {
        "${totalVolDisplay.toInt()} $unitSuffix"
    }

    val totalMin = stats.totalDurationSeconds / 60
    val totalHours = totalMin / 60
    val remMin = totalMin % 60
    val durationFormatted = if (totalHours > 0) "${totalHours}h ${remMin}m" else "${totalMin}m"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(
            value = "${stats.uniqueWorkoutDays}",
            label = strings.workoutDaysLabel
        )
        StatItem(
            value = "${stats.totalSessions}",
            label = strings.totalSessionsLabel
        )
        StatItem(
            value = durationFormatted,
            label = strings.totalDurationLabel
        )
        StatItem(
            value = totalVolFormatted,
            label = strings.totalVolumeLabel
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    val colors = LocalKumaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.accentText
            )
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = colors.textSecondary
            )
        )
    }
}

@Composable
private fun SelectedDayDetailCard(
    date: LocalDate,
    workouts: List<Workout>,
    scheduledEvents: List<WorkoutScheduleEvent>,
    recurringRoutines: List<RoutineBlueprint> = emptyList(),
    isToday: Boolean,
    isLb: Boolean,
    unitSuffix: String,
    timeZone: TimeZone,
    strings: AppStrings,
    onScheduleClick: () -> Unit,
    onEditEvent: (WorkoutScheduleEvent) -> Unit,
    onStartScheduledWorkout: ((eventId: String, routineId: String) -> Unit)?,
    onStartRoutine: ((routine: RoutineBlueprint) -> Unit)? = null
) {
    val colors = LocalKumaColors.current

    val dayOfWeekName = strings.dayNamesFull.getOrElse(date.dayOfWeek.ordinal) { "" }
    val monthName = strings.monthNamesFull.getOrElse(date.monthNumber - 1) { "" }
    val dateHeader = "$dayOfWeekName, ${date.dayOfMonth} $monthName ${date.year}"

    val isEmpty = workouts.isEmpty() && scheduledEvents.isEmpty() && recurringRoutines.isEmpty()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Header + Today chip + Add Schedule action
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isToday) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.accentTint)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.todayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }

            if (!isEmpty) {
                IconButton(
                    onClick = onScheduleClick,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = strings.addScheduleWorkout,
                        tint = colors.accentText,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Scheduled Events for this date
        if (scheduledEvents.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                scheduledEvents.forEach { event ->
                    val statusColor = when {
                        event.isCompleted -> colors.recoveryFresh
                        event.isCancelled -> colors.textSecondary.copy(alpha = 0.5f)
                        else -> colors.accentText
                    }
                    val statusText = when {
                        event.isCompleted -> strings.completedStatus
                        event.isCancelled -> strings.cancelledStatus
                        else -> strings.scheduledStatus
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = event.routineTitleSnapshot,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (event.isCancelled) colors.textSecondary else colors.textPrimary
                                    )
                                )
                                event.splitTypeSnapshot?.let { KumaBadge(text = it) }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(statusColor.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = statusText,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = statusColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            event.notes?.let { note ->
                                Text(
                                    text = note,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.textSecondary,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (event.isPlanned && event.routineId != null && onStartScheduledWorkout != null) {
                                IconButton(
                                    onClick = { onStartScheduledWorkout(event.id, event.routineId) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = strings.startScheduledWorkout,
                                        tint = colors.accentFill,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onEditEvent(event) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = strings.rescheduleWorkout,
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Recurring Routines for this day of the week (when no explicit scheduled events exist)
        if (recurringRoutines.isNotEmpty() && scheduledEvents.none { it.isPlanned }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                recurringRoutines.forEach { routine ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = routine.localizedTitle(strings),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                routine.splitCategory.takeIf { it.isNotBlank() }?.let { KumaBadge(text = it) }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.accentTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = strings.scheduledStatus,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accentText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        )
                                    )
                                }
                            }

                            val exerciseCount = routine.effectiveConfigs.size
                            Text(
                                text = strings.routineSubtitle(exerciseCount, routine.estimatedDurationMinutes),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }

                        if (onStartRoutine != null) {
                            IconButton(
                                onClick = { onStartRoutine(routine) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = strings.startScheduledWorkout,
                                    tint = colors.accentFill,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Empty state / Rest Day
        if (isEmpty) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = strings.restDayTitle,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textSecondary,
                                fontSize = 10.sp
                            )
                        )
                    }
                    Text(
                        text = strings.noActivityOnDate,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(colors.accentTint)
                        .clickable(onClick = onScheduleClick)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = strings.addScheduleWorkout,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.accentText,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        } else if (workouts.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                workouts.forEach { w ->
                    val startDt = Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(timeZone)
                    val hour = startDt.hour.toString().padStart(2, '0')
                    val min = startDt.minute.toString().padStart(2, '0')
                    val startTimeFormatted = "$hour:$min"

                    val durMin = w.durationSeconds / 60
                    val volDisplay = if (isLb) (w.totalVolumeKg * 2.20462).toInt() else w.totalVolumeKg.toInt()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = w.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    )
                                )
                                KumaBadge(text = w.splitType)
                            }

                            Text(
                                text = "$startTimeFormatted · ${durMin} ${strings.minAbbrev} · $volDisplay $unitSuffix · ${w.totalCompletedSets} ${strings.setsAbbrev}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary
                                )
                            )

                            if (w.exercises.isNotEmpty()) {
                                Text(
                                    text = w.exercises.joinToString(", ") { it.exercise.name },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.textSecondary.copy(alpha = 0.8f),
                                        fontSize = 10.sp
                                    ),
                                    maxLines = 1
                                )
                            }
                        }

                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = strings.completedStatus,
                            tint = colors.recoveryFresh,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
