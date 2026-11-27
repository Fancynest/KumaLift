package com.bearbones.kumalift.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaSessionBarChart
import com.bearbones.kumalift.core.designsystem.KumaSessionBarItem
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.buildDistinctChartDateLabels
import com.bearbones.kumalift.core.designsystem.formatCompactChartValue
import com.bearbones.kumalift.core.designsystem.formatIntegerChartValue
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.media.ProfileAvatarMedia
import com.bearbones.kumalift.core.media.ProfilePhotoCropDialog
import com.bearbones.kumalift.core.media.rememberProfileImagePicker
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AthleteSex
import com.bearbones.kumalift.domain.model.DateFormatPreference
import com.bearbones.kumalift.domain.model.ExperienceLevel
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit

enum class AnalyticsMetric {
    DURATION,
    VOLUME,
    REPS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeScreen(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    personalRecordRepository: PersonalRecordRepository,
    onNavigateToSettings: () -> Unit,
    onNavigateToProgress: () -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToRoutines: () -> Unit,
    onStartWorkout: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val completedWorkouts by workoutRepository.completedWorkouts.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val language = settings.appearance.language
    val dateFormat = settings.units.dateFormat

    var selectedMetric by rememberSaveable { mutableStateOf(AnalyticsMetric.VOLUME) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMeasuresDialog by remember { mutableStateOf(false) }
    var showScheduleModal by remember { mutableStateOf(false) }

    val athleteName = settings.profile.name.ifBlank { s.athleteDefault }
    val athleteUsername = settings.profile.username.ifBlank { s.athleteUsernameDefault }
    val workoutCount = completedWorkouts.size
    val allPrs by personalRecordRepository.personalRecords.collectAsState()
    val prCount = allPrs.size
    val isLb = settings.units.weightUnit == WeightUnit.LB
    val multiplier = if (isLb) 2.20462 else 1.0
    val unitSuffix = if (isLb) s.unitLb else s.unitKg
    val totalVolumeKg = completedWorkouts.sumOf { it.totalVolumeKg }
    val totalVolumeDisplay = totalVolumeKg * multiplier
    val totalVolumeText = if (totalVolumeDisplay >= 1000.0) {
        val thousands = ((totalVolumeDisplay / 100.0).toInt()) / 10.0
        "${thousands}k $unitSuffix"
    } else {
        "${totalVolumeDisplay.toInt()} $unitSuffix"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 90.dp)
        ) {
            // Top App Bar: Username + Actions (Edit, Share, Settings)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = athleteUsername,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary,
                            fontSize = 22.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { showEditProfileDialog = true }
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Edit profile button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KumaColors.SurfaceDim)
                                .clickable { showEditProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = s.editProfileContentDesc,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Settings gear button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(KumaColors.SurfaceDim)
                                .clickable(onClick = onNavigateToSettings),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = s.settingsContentDesc,
                                tint = colors.textPrimary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Athlete Profile Header: Avatar + Name + 3 Stats Counters
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(colors.surfaceVariant)
                                .border(2.dp, colors.accentText.copy(alpha = 0.5f), CircleShape)
                                .clickable { showEditProfileDialog = true },
                            contentAlignment = Alignment.Center
                        ) {
                            ProfileAvatarMedia(
                                avatarUri = settings.profile.avatarUri,
                                fallbackInitial = athleteName.take(1).ifBlank { "G" },
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Stats counters (Workouts, PRs, Total Volume)
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatCounter(
                                value = "$workoutCount",
                                label = s.tabWorkouts,
                                modifier = Modifier.weight(1f)
                            )
                            StatCounter(
                                value = "$prCount",
                                label = s.prRecordsStatLabel,
                                modifier = Modifier.weight(1f)
                            )
                            StatCounter(
                                value = if (totalVolumeDisplay >= 1000.0) {
                                    val thousands = ((totalVolumeDisplay / 100.0).toInt()) / 10.0
                                    "${thousands}k"
                                } else {
                                    "${totalVolumeDisplay.toInt()}"
                                },
                                label = s.volumeStatLabel(unitSuffix),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = athleteName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 18.sp
                        )
                    )

                    if (settings.profile.bio.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = settings.profile.bio,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary
                            )
                        )
                    }
                }
            }

            // Analytics Widget: Metric Toggle Pills + Interactive Chart Card
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    // Metric Pills: Duration | Volume | Reps
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricPill(
                            label = s.metricDurationLabel,
                            isSelected = selectedMetric == AnalyticsMetric.DURATION,
                            onClick = { selectedMetric = AnalyticsMetric.DURATION },
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = s.metricVolumeLabel,
                            isSelected = selectedMetric == AnalyticsMetric.VOLUME,
                            onClick = { selectedMetric = AnalyticsMetric.VOLUME },
                            modifier = Modifier.weight(1f)
                        )
                        MetricPill(
                            label = s.metricRepsLabel,
                            isSelected = selectedMetric == AnalyticsMetric.REPS,
                            onClick = { selectedMetric = AnalyticsMetric.REPS },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Analytics Chart Card
                    KumaCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = when (selectedMetric) {
                                            AnalyticsMetric.DURATION -> s.workoutDurationMetricLabel
                                            AnalyticsMetric.VOLUME -> s.totalVolumeLiftedMetricLabel
                                            AnalyticsMetric.REPS -> s.totalRepetitionsMetricLabel
                                        },
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when (selectedMetric) {
                                            AnalyticsMetric.DURATION -> s.profileDurationChartSubtitle
                                            AnalyticsMetric.VOLUME -> s.profileVolumeChartSubtitle
                                            AnalyticsMetric.REPS -> s.profileRepsChartSubtitle
                                        },
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = KumaColors.TextMuted
                                        )
                                    )
                                }

                                Text(
                                    text = if (completedWorkouts.isEmpty()) "—" else when (selectedMetric) {
                                        AnalyticsMetric.DURATION -> {
                                            val avgMins = completedWorkouts
                                                .map { (it.durationSeconds / 60).coerceAtLeast(1) }
                                                .average()
                                                .toInt()
                                            s.avgMinutesLabel(avgMins)
                                        }
                                        AnalyticsMetric.VOLUME -> totalVolumeText
                                        AnalyticsMetric.REPS -> {
                                            val totalReps = completedWorkouts.sumOf { w ->
                                                w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }
                                            }
                                            "${formatIntegerChartValue(totalReps.toDouble(), language)} ${s.repsAbbrev}"
                                        }
                                    },
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accentText
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            if (completedWorkouts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s.noWorkoutDataChartMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                val chartWorkouts = remember(completedWorkouts) {
                                    completedWorkouts.sortedBy { it.startTimeEpochMs }.takeLast(6)
                                }
                                var selectedBarIdx by remember(chartWorkouts, selectedMetric) {
                                    mutableStateOf((chartWorkouts.size - 1).coerceAtLeast(0))
                                }
                                val clampedIdx = selectedBarIdx.coerceIn(0, (chartWorkouts.size - 1).coerceAtLeast(0))

                                val barItems = remember(chartWorkouts, selectedMetric, multiplier, language, dateFormat, s.todayShortLabel, unitSuffix) {
                                    val xLabels = buildDistinctChartDateLabels(
                                        epochs = chartWorkouts.map { it.startTimeEpochMs },
                                        fallbacks = chartWorkouts.map { "" },
                                        pref = dateFormat,
                                        language = language,
                                        todayFallback = s.todayShortLabel
                                    )
                                    chartWorkouts.mapIndexed { idx, w ->
                                        val rawVal = when (selectedMetric) {
                                            AnalyticsMetric.DURATION -> (w.durationSeconds / 60).coerceAtLeast(1).toDouble()
                                            AnalyticsMetric.VOLUME -> w.totalVolumeKg * multiplier
                                            AnalyticsMetric.REPS -> w.exercises.sumOf { ex ->
                                                ex.sets.filter { it.isCompleted }.sumOf { it.reps }
                                            }.toDouble()
                                        }
                                        val unitLabel = when (selectedMetric) {
                                            AnalyticsMetric.DURATION -> s.minAbbrev
                                            AnalyticsMetric.VOLUME -> unitSuffix
                                            AnalyticsMetric.REPS -> s.repsAbbrev
                                        }
                                        val fullFormatted = formatIntegerChartValue(rawVal, language)
                                        KumaSessionBarItem(
                                            valueDisplay = rawVal,
                                            compactValueLabel = formatCompactChartValue(rawVal, language),
                                            xLabel = xLabels[idx],
                                            detailSummary = "${xLabels[idx]} · ${w.title} · $fullFormatted $unitLabel"
                                        )
                                    }
                                }
                                val avgVal = barItems.map { it.valueDisplay }.average()

                                KumaSessionBarChart(
                                    bars = barItems,
                                    avgDisplay = avgVal,
                                    selectedIndex = clampedIdx,
                                    onSelectBar = { selectedBarIdx = it },
                                    language = language
                                )

                                barItems.getOrNull(clampedIdx)?.let { activeItem ->
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = activeItem.detailSummary,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.textSecondary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Dashboard Grid (2x2 Cards: Statistics, Exercises, Measures, Calendar)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = s.dashboardSection,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Statistics Card
                        DashboardCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = s.statisticsCardTitle,
                            subtitle = s.statisticsCardSubtitle,
                            onClick = onNavigateToProgress
                        )

                        // Exercises Card
                        DashboardCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Default.FitnessCenter,
                            title = s.exercisesCardTitle,
                            subtitle = s.exercisesCardSubtitle,
                            onClick = onNavigateToLibrary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Measures Card
                        val bw = settings.profile.bodyweightKg
                        val ht = settings.profile.heightCm
                        val unitStr = if (settings.units.weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
                        val formattedBw = bw?.let {
                            NumberFormatUtils.formatDecimal(
                                UnitConverters.toDisplayWeight(it, settings.units.weightUnit),
                                settings.appearance.language,
                                1,
                                trimTrailingZeros = true
                            )
                        }
                        val measuresSubtitle = when {
                            formattedBw != null && ht != null -> "$formattedBw $unitStr · ${ht.toInt()} ${s.sizeUnitCm}"
                            formattedBw != null -> "$formattedBw $unitStr"
                            ht != null -> "${ht.toInt()} ${s.sizeUnitCm}"
                            else -> s.noMeasuresRecorded
                        }
                        DashboardCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Default.AccessibilityNew,
                            title = s.measuresCardTitle,
                            subtitle = measuresSubtitle,
                            onClick = { showMeasuresDialog = true }
                        )

                        // Calendar Card
                        DashboardCard(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            icon = Icons.Default.CalendarToday,
                            title = s.calendarCardTitle,
                            subtitle = s.trainingDaysSubtitle(settings.profile.trainingDaysPerWeek),
                            onClick = { showScheduleModal = true }
                        )
                    }
                }
            }

            // Workouts Section (Recent Completed Workouts or Empty State)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.allWorkoutsHeader,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                        )

                        Text(
                            text = s.viewAllButton,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText
                            ),
                            modifier = Modifier.clickable(onClick = onNavigateToRoutines)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (completedWorkouts.isEmpty()) {
                        // Empty workouts card matching reference screenshot
                        KumaCard(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = colors.textSecondary.copy(alpha = 0.5f),
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = s.noWorkoutsCompletedYet,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = onStartWorkout,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = colors.accentFill,
                                        contentColor = colors.onAccentFill
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s.startWorkoutButton,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // List completed workouts
                        completedWorkouts.take(5).forEach { workout ->
                            val durationMin = (workout.durationSeconds / 60).coerceAtLeast(1)
                            val vol = if (isLb) workout.totalVolumeKg * 2.20462 else workout.totalVolumeKg
                            val volFormatted = "${NumberFormatUtils.formatIntegerWithSeparators(vol.toLong(), settings.appearance.language)} $unitSuffix"

                            val timeZone = TimeZone.currentSystemDefault()
                            val workoutDate = Instant.fromEpochMilliseconds(workout.startTimeEpochMs)
                                .toLocalDateTime(timeZone)
                                .date
                            val todayDate = Clock.System.now().toLocalDateTime(timeZone).date
                            val dateText = if (workoutDate == todayDate) {
                                s.todayLabel
                            } else {
                                val dayOfWeekName = s.dayNamesFull.getOrElse(workoutDate.dayOfWeek.ordinal) { "" }
                                val monthName = s.monthNamesFull.getOrElse(workoutDate.monthNumber - 1) { "" }
                                if (workoutDate.year == todayDate.year) {
                                    "$dayOfWeekName, ${workoutDate.dayOfMonth} $monthName"
                                } else {
                                    "$dayOfWeekName, ${workoutDate.dayOfMonth} $monthName ${workoutDate.year}"
                                }
                            }
                            WorkoutSummaryCard(
                                title = workout.title,
                                date = dateText,
                                duration = "$durationMin ${s.minAbbrev}",
                                volume = volFormatted,
                                exerciseCount = workout.exercises.size
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
        }
    }

    // Edit Profile Dialog/Sheet matching profile.jpg
    if (showEditProfileDialog) {
        EditProfileModal(
            settingsRepository = settingsRepository,
            onDismiss = { showEditProfileDialog = false }
        )
    }

    // Measures Dialog
    if (showMeasuresDialog) {
        MeasuresModal(
            settingsRepository = settingsRepository,
            onDismiss = { showMeasuresDialog = false }
        )
    }

    if (showScheduleModal) {
        com.bearbones.kumalift.features.routines.WorkoutScheduleModal(
            settingsRepository = settingsRepository,
            onDismiss = { showScheduleModal = false }
        )
    }
}

@Composable
private fun StatCounter(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = value,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                fontSize = 20.sp
            )
        )
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textSecondary,
                fontSize = 12.sp
            )
        )
    }
}

@Composable
private fun MetricPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    val bgColor = if (isSelected) colors.accentFill else KumaColors.SurfaceDim
    val textColor = if (isSelected) colors.onAccentFill else colors.textSecondary

    Box(
        modifier = modifier
            .clip(PillShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            maxLines = 1,
            softWrap = false,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                color = textColor
            )
        )
    }
}

@Composable
private fun DashboardCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    KumaCard(
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = colors.accentText,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = subtitle,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun WorkoutSummaryCard(
    title: String,
    date: String,
    duration: String,
    volume: String,
    exerciseCount: Int
) {
    val colors = LocalKumaColors.current
    KumaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = date,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = LocalStrings.current.workoutSummaryMovements(exerciseCount),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary
                    )
                )
                Text(
                    text = "· $duration",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary
                    )
                )
                Text(
                    text = "· $volume",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.accentText,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileModal(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var nameInput by remember { mutableStateOf(settings.profile.name) }
    var usernameInput by remember { mutableStateOf(settings.profile.username) }
    var bioInput by remember { mutableStateOf(settings.profile.bio) }
    var selectedSex by remember { mutableStateOf(settings.profile.sex) }
    var birthdayInput by remember { mutableStateOf(settings.profile.birthday) }
    var avatarUriState by remember { mutableStateOf(settings.profile.avatarUri) }
    var pendingCropUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberProfileImagePicker { newPath ->
        pendingCropUri = newPath
    }

    if (pendingCropUri != null) {
        ProfilePhotoCropDialog(
            imageUri = pendingCropUri!!,
            onCancel = { pendingCropUri = null },
            onConfirm = { confirmedUri ->
                avatarUriState = confirmedUri
                pendingCropUri = null
            }
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KumaColors.SurfaceDim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .kumaKeyboardAvoidance()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            // Header: Close / Title / Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = s.close,
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDismiss)
                )

                Text(
                    text = s.editProfileTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                Button(
                    onClick = {
                        settingsRepository.update { current ->
                            current.copy(
                                profile = current.profile.copy(
                                    avatarUri = avatarUriState,
                                    name = nameInput.trim(),
                                    username = usernameInput.trim().lowercase().replace(" ", "_"),
                                    bio = bioInput.trim(),
                                    sex = selectedSex,
                                    birthday = birthdayInput.trim()
                                )
                            )
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(s.done, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Avatar + Change Picture
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(colors.surfaceVariant)
                        .border(2.dp, colors.accentText, CircleShape)
                        .clickable { imagePicker() },
                    contentAlignment = Alignment.Center
                ) {
                    ProfileAvatarMedia(
                        avatarUri = avatarUriState,
                        fallbackInitial = nameInput.take(1).ifBlank { "G" },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = s.changePictureButton,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.accentText,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.clickable { imagePicker() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Public profile data section
            Text(
                text = s.publicProfileSection,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = s.profileNameInputLabel,
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = s.profileNamePlaceholder
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = s.profileBioFieldPlaceholder,
                value = bioInput,
                onValueChange = { bioInput = it },
                placeholder = s.profileBioPlaceholder
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Private data section
            Text(
                text = s.privateProfileSection,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sex Selector (Male, Female, Unspecified)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.sexFieldLabel,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AthleteSex.MALE to s.sexMale,
                        AthleteSex.FEMALE to s.sexFemale
                    ).forEach { (sex, label) ->
                        val isSelected = selectedSex == sex
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) colors.accentFill else KumaColors.SurfaceContainer
                                )
                                .clickable { selectedSex = sex }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) colors.onAccentFill else colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ProfileField(
                label = s.birthdayFieldLabel,
                value = birthdayInput,
                onValueChange = { birthdayInput = it },
                placeholder = s.birthdayFieldPlaceholder
            )
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = LocalKumaColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textSecondary,
                fontWeight = FontWeight.SemiBold
            )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(KumaColors.SurfaceContainer)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = colors.textPrimary
                ),
                cursorBrush = SolidColor(colors.accentText),
                modifier = Modifier.fillMaxWidth(),
                decorationBox = { innerTextField ->
                    if (value.isBlank()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textSecondary.copy(alpha = 0.5f)
                            )
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MeasuresModal(
    settingsRepository: SettingsRepository,
    onDismiss: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var weightInput by remember(settings.profile.bodyweightKg, settings.units.weightUnit) {
        mutableStateOf(
            settings.profile.bodyweightKg?.let { kg ->
                val disp = UnitConverters.toDisplayWeight(kg, settings.units.weightUnit)
                if (disp == disp.toLong().toDouble()) disp.toLong().toString() else disp.toString()
            } ?: ""
        )
    }
    var heightInput by remember(settings.profile.heightCm) {
        mutableStateOf(settings.profile.heightCm?.toString() ?: "")
    }
    var hasValidationError by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KumaColors.SurfaceDim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .kumaKeyboardAvoidance()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = s.bodyMeasuresTitle,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            val weightUnitStr = if (settings.units.weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
            ProfileField(
                label = "${s.bodyWeightMeasureLabel} ($weightUnitStr)",
                value = weightInput,
                onValueChange = { weightInput = it },
                placeholder = "0.0"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = "${s.bodyHeightMeasureLabel} (${s.sizeUnitCm})",
                value = heightInput,
                onValueChange = {
                    heightInput = it
                    hasValidationError = false
                },
                placeholder = "0"
            )

            if (hasValidationError) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = s.enterValidWeightHint,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.error)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val trimmedW = weightInput.trim()
                    val trimmedH = heightInput.trim()
                    val parsedW = if (trimmedW.isEmpty()) {
                        null
                    } else {
                        NumberFormatUtils.parseDecimalInput(trimmedW)
                            ?.takeIf { it.isFinite() && it > 0.0 && it <= 1000.0 }
                            ?.let { UnitConverters.toStorageWeightKg(it, settings.units.weightUnit) }
                    }
                    val parsedH = if (trimmedH.isEmpty()) {
                        null
                    } else {
                        NumberFormatUtils.parseDecimalInput(trimmedH)
                            ?.takeIf { it.isFinite() && it in 50.0..280.0 }
                    }
                    if ((trimmedW.isNotEmpty() && parsedW == null) || (trimmedH.isNotEmpty() && parsedH == null)) {
                        hasValidationError = true
                    } else {
                        settingsRepository.update { current ->
                            current.copy(
                                profile = current.profile.copy(
                                    bodyweightKg = parsedW,
                                    heightCm = parsedH
                                )
                            )
                        }
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(s.saveMeasuresButton, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        }
    }
}
