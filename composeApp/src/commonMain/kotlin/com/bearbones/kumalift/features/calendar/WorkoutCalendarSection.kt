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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FitnessCenter
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
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.Workout
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

    val workoutsByDate = remember(completedWorkouts, timeZone) {
        WorkoutCalendarLogic.groupWorkoutsByLocalDate(completedWorkouts, timeZone)
    }

    val weeks = remember(displayedYear, displayedMonth, firstDayOfWeek, today, selectedDate, workoutsByDate) {
        WorkoutCalendarLogic.buildMonthGrid(
            year = displayedYear,
            month = displayedMonth,
            firstDayOfWeek = firstDayOfWeek,
            today = today,
            selectedDate = selectedDate,
            workoutsByDate = workoutsByDate
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
        backgroundColor = KumaColors.SurfaceContainerLow,
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
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = s.todayLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.accentText
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
                    style = MaterialTheme.typography.titleSmall.copy(
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

            // 2. Weekday Header (M T W T F S S)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dayHeaders.forEach { dayName ->
                    Text(
                        text = dayName,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary.copy(alpha = 0.7f)
                        ),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // 3. Month Days Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
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
                                    if (!cell.isCurrentMonth) {
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
                            .background(KumaColors.ElectricLime)
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
            SelectedDayDetailCard(
                date = selectedDate,
                workouts = selectedWorkouts,
                isToday = (selectedDate == today),
                isLb = isLb,
                unitSuffix = unitSuffix,
                timeZone = timeZone,
                strings = s
            )
        }
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

            if (cell.hasCompletedWorkout) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (cell.isSelected) colors.onAccentFill else KumaColors.ElectricLime)
                )
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
    isToday: Boolean,
    isLb: Boolean,
    unitSuffix: String,
    timeZone: TimeZone,
    strings: AppStrings
) {
    val colors = LocalKumaColors.current

    // Day of week name (1=Monday..7=Sunday)
    val dayOfWeekName = strings.dayNamesFull.getOrElse(date.dayOfWeek.ordinal) { "" }
    val monthName = strings.monthNamesFull.getOrElse(date.monthNumber - 1) { "" }
    val dateHeader = "$dayOfWeekName, ${date.dayOfMonth} $monthName ${date.year}"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surfaceVariant)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dateHeader,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
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

        if (workouts.isEmpty()) {
            Text(
                text = strings.noActivityOnDate,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary
                ),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        } else {
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
                            .background(KumaColors.SurfaceDim)
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
                            tint = KumaColors.ElectricLime,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
