package com.bearbones.kumalift.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaLogoMark
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.FirstDayOfWeek
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    routineRepository: com.bearbones.kumalift.data.repository.RoutineRepository? = null,
    personalRecordRepository: com.bearbones.kumalift.data.repository.PersonalRecordRepository? = null,
    onStartWorkout: () -> Unit,
    onStartFreestyle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val completedWorkouts by workoutRepository.completedWorkouts.collectAsState()
    val prList by (personalRecordRepository?.personalRecords ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val routines by (routineRepository?.routines ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    val scheduledRoutine = routines.firstOrNull()

    val currentHour = remember {
        try {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        } catch (e: Exception) {
            12
        }
    }
    val s = LocalStrings.current
    val timeGreeting = when (currentHour) {
        in 4..10 -> s.greetingMorning
        in 11..17 -> s.greetingAfternoon
        else -> s.greetingEvening
    }
    val athleteGreeting = settings.profile.username.ifBlank { settings.profile.name.ifBlank { s.athleteDefault } }
    val phaseText = when (settings.profile.goal) {
        TrainingGoal.HYPERTROPHY -> s.phaseHypertrophy
        TrainingGoal.STRENGTH -> s.phaseStrength
        TrainingGoal.GENERAL -> s.phaseGeneral
    }

    // Real dynamic stats calculated from stored database data
    val nowMs = remember { Clock.System.now().toEpochMilliseconds() }
    val oneWeekAgoMs = nowMs - 7L * 24 * 3600_000L
    val thisWeekWorkouts = remember(completedWorkouts) {
        completedWorkouts.filter { it.startTimeEpochMs >= oneWeekAgoMs }
    }
    val thisWeekVolumeKg = remember(thisWeekWorkouts) {
        thisWeekWorkouts.sumOf { it.totalVolumeKg }
    }
    val totalVolumeKg = remember(completedWorkouts) {
        completedWorkouts.sumOf { it.totalVolumeKg }
    }

    val isLb = settings.units.weightUnit == WeightUnit.LB
    val unitSuffix = if (isLb) s.unitLb else s.unitKg
    val multiplier = if (isLb) 2.20462 else 1.0

    val weekVolDisplay = thisWeekVolumeKg * multiplier
    val weekVolFormatted = if (weekVolDisplay >= 1000.0) {
        val thousands = ((weekVolDisplay / 100.0).toInt()) / 10.0
        "${thousands}k $unitSuffix"
    } else {
        "${weekVolDisplay.toInt()} $unitSuffix"
    }

    val totalVolDisplay = totalVolumeKg * multiplier
    val totalVolFormatted = if (totalVolDisplay >= 1000.0) {
        val thousands = ((totalVolDisplay / 100.0).toInt()) / 10.0
        "${thousands}k $unitSuffix"
    } else {
        "${totalVolDisplay.toInt()} $unitSuffix"
    }

    val targetGoalWorkouts = settings.profile.trainingDaysPerWeek
    val weekWorkoutsCount = thisWeekWorkouts.size
    val targetPct = if (targetGoalWorkouts > 0) minOf(100, (weekWorkoutsCount * 100) / targetGoalWorkouts) else 0

    val volumeBadge = s.volumeThisWeek(weekVolFormatted)
    val targetBadge = s.goalProgress(targetPct, weekWorkoutsCount, targetGoalWorkouts)
    val prBadge = s.prRecordsCount(prList.size)

    var backupBannerDismissed by remember { mutableStateOf(false) }
    val backupReminderDays = settings.dataBackup.backupReminderDays
    val lastBackupMs = settings.dataBackup.lastBackupEpochMs
    val isBackupDue = remember(backupReminderDays, lastBackupMs, backupBannerDismissed) {
        if (backupBannerDismissed || backupReminderDays <= 0) false
        else {
            val daysElapsed = if (lastBackupMs == null) 999L else (nowMs - lastBackupMs) / (24L * 3600_000L)
            daysElapsed >= backupReminderDays
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    // 1. Athlete Header & Dynamic Real-time Greeting
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = phaseText,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = LocalKumaColors.current.accentText,
                                letterSpacing = 1.sp
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "$timeGreeting, $athleteGreeting",
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 24.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Metric Chips in a horizontally scrollable LazyRow (zero squeezing)
                    LazyRow(
                        modifier = Modifier.isolateHorizontalScroll(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            KumaBadge(text = volumeBadge)
                        }
                        item {
                            KumaBadge(text = targetBadge)
                        }
                        item {
                            KumaBadge(text = prBadge)
                        }
                    }
                }

                // Backup Reminder Banner (data.backupReminderDays)
                if (isBackupDue) {
                    item {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainer,
                            borderColor = Color(0xFFF59E0B).copy(alpha = 0.6f),
                            cornerRadius = 14.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.backupReminderTitle,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B)
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = s.backupReminderBody,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = KumaColors.TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(KumaColors.SurfaceContainerHigh)
                                            .clickable { onOpenSettings() }
                                            .padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = s.backupButton,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = LocalKumaColors.current.accentText
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { backupBannerDismissed = true }
                                            .padding(horizontal = 8.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "✕",
                                            style = MaterialTheme.typography.labelSmall.copy(color = KumaColors.TextMuted)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Weekly Horizon (7-Day Tracker)
                if (settings.appearance.showWeeklyHorizon) {
                    item {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
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
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = LocalKumaColors.current.accentText,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = s.thisWeekLabel,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(KumaColors.ElectricLime)
                                            .border(
                                                1.dp,
                                                if (LocalKumaColors.current.isDark) Color.Transparent else LocalKumaColors.current.accentFillBorder,
                                                PillShape
                                            )
                                            .padding(horizontal = 12.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = s.workoutsTargetCount(weekWorkoutsCount, settings.profile.trainingDaysPerWeek),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = KumaColors.OnElectricLime,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // 7 Days Grid with firstDayOfWeek setting and honest dynamic completion tracking
                                val nowInstant = Clock.System.now()
                                val nowDt = nowInstant.toLocalDateTime(TimeZone.currentSystemDefault())
                                val todayDayOfWeek = nowDt.dayOfWeek.ordinal + 1 // 1 = Monday, ..., 7 = Sunday

                                val firstDay = settings.units.firstDayOfWeek
                                val dayIndices = when (firstDay) {
                                    FirstDayOfWeek.MONDAY -> listOf(1, 2, 3, 4, 5, 6, 7)
                                    FirstDayOfWeek.SUNDAY -> listOf(7, 1, 2, 3, 4, 5, 6)
                                    FirstDayOfWeek.SATURDAY -> listOf(6, 7, 1, 2, 3, 4, 5)
                                }
                                val dayLabels = when (firstDay) {
                                    FirstDayOfWeek.MONDAY -> s.daysMondayStart
                                    FirstDayOfWeek.SUNDAY -> s.daysSundayStart
                                    FirstDayOfWeek.SATURDAY -> s.daysSaturdayStart
                                }

                                val daysDone = dayIndices.map { dayNum ->
                                    thisWeekWorkouts.any { w ->
                                        val wDt = kotlinx.datetime.Instant.fromEpochMilliseconds(w.startTimeEpochMs).toLocalDateTime(TimeZone.currentSystemDefault())
                                        (wDt.dayOfWeek.ordinal + 1) == dayNum
                                    }
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    dayIndices.forEachIndexed { idx, dayNum ->
                                        val isDone = daysDone[idx]
                                        val isToday = dayNum == todayDayOfWeek
                                        DayCircle(
                                            day = dayLabels[idx],
                                            isDone = isDone,
                                            isActive = isToday && !isDone
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Today's Workout Card (Hero)
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                KumaBadge(text = s.todaysSchedule)
                                Text(
                                    text = scheduledRoutine?.targetRpeTier ?: s.readyToTrain,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = scheduledRoutine?.title ?: s.customRoutine,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = scheduledRoutine?.let {
                                    s.routineSubtitle(it.exerciseTemplates.size, it.estimatedDurationMinutes)
                                } ?: s.startEmptyOrPickHint,
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextSecondary
                            )

                            if (scheduledRoutine != null && scheduledRoutine.exerciseTemplates.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(16.dp))

                                // Exercise Preview Chips
                                LazyRow(
                                    modifier = Modifier.isolateHorizontalScroll(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(scheduledRoutine.exerciseTemplates.take(4)) { ex ->
                                        KumaBadge(text = ex.name)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            KumaPrimaryButton(
                                text = s.startWorkoutButton,
                                onClick = {
                                    if (scheduledRoutine != null) {
                                        workoutRepository.startWorkoutFromRoutine(scheduledRoutine)
                                    } else {
                                        workoutRepository.startEmptyWorkout(s.freestyleWorkoutName)
                                    }
                                    onStartWorkout()
                                }
                            )
                        }
                    }
                }

                // 4. Monthly Quick Glance Metrics (Spacious 3-column)
                if (settings.appearance.showMonthlyGlance) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = s.totalSessionsLabel,
                                value = "${completedWorkouts.size}",
                                subtext = s.totalSessionsSubtext
                            )
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = s.totalVolumeLabel,
                                value = totalVolFormatted,
                                subtext = s.totalVolumeSubtext
                            )
                            MetricTile(
                                modifier = Modifier.weight(1f),
                                label = s.recordsPrLabel,
                                value = "${prList.size}",
                                subtext = s.recordsPrSubtext,
                                isAccent = true
                            )
                        }
                    }
                }

                // 5. Recent Workouts & Freestyle Button
                if (settings.appearance.showRecentWorkouts) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = s.recentWorkoutsLabel,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            if (completedWorkouts.isEmpty()) {
                                KumaCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = KumaColors.SurfaceContainer,
                                    cornerRadius = 16.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.noRecentWorkouts,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = KumaColors.TextMuted,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                completedWorkouts.take(settings.appearance.recentWorkoutsCount).forEach { workout ->
                                val durationMin = workout.durationSeconds / 60
                                val durationText = "${durationMin} ${s.minAbbrev}"
                                val volDisplay = if (isLb) (workout.totalVolumeKg * 2.20462).toInt() else workout.totalVolumeKg.toInt()
                                val volText = "$volDisplay $unitSuffix"
                                val prCountInWorkout = workout.exercises.sumOf { ex -> ex.sets.count { it.isPr } }

                                KumaCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    backgroundColor = KumaColors.SurfaceContainer,
                                    cornerRadius = 16.dp
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(18.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = workout.title,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "$volText · $durationText",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = KumaColors.TextMuted
                                            )
                                        }
                                        if (prCountInWorkout > 0) {
                                            KumaBadge(
                                                text = "$prCountInWorkout PR",
                                                isPr = true
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Log Empty / Freestyle Action
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(KumaColors.SurfaceContainer)
                                .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(14.dp))
                                .clickable {
                                    workoutRepository.startEmptyWorkout(s.freestyleWorkoutName)
                                    onStartFreestyle()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s.freestyleWorkoutCardTitle,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = LocalKumaColors.current.textPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
            }
        }
    }
}

@Composable
fun DayCircle(
    day: String,
    isDone: Boolean = false,
    isRest: Boolean = false,
    isActive: Boolean = false
) {
    val colors = LocalKumaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = day,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isActive) colors.accentText else colors.textSubtle
        )
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> colors.accentFill
                        isActive -> colors.surfaceVariant
                        else -> colors.surfaceBright
                    }
                )
                .border(
                    width = if (isActive) 1.5.dp else 0.dp,
                    color = if (isActive) (if (colors.isDark) colors.accentFill else colors.accentChart) else Color.Transparent,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                isDone -> Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = colors.onAccentFill,
                    modifier = Modifier.size(18.dp)
                )
                isRest -> Icon(
                    imageVector = Icons.Default.Hotel,
                    contentDescription = null,
                    tint = colors.textSubtle,
                    modifier = Modifier.size(14.dp)
                )
                isActive -> Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(18.dp)
                )
                else -> Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceBright)
                )
            }
        }
    }
}

@Composable
fun MetricTile(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    val colors = LocalKumaColors.current
    KumaCard(
        modifier = modifier,
        backgroundColor = colors.surface,
        cornerRadius = 14.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = colors.textSubtle,
                    letterSpacing = 0.5.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = if (isAccent) colors.accentText else colors.textPrimary
                ),
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = if (isAccent) colors.accentText else colors.textSecondary
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
