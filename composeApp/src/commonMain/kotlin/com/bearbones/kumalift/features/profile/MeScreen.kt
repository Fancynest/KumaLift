package com.bearbones.kumalift.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
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
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.media.ProfileAvatarMedia
import com.bearbones.kumalift.core.media.ProfilePhotoCropDialog
import com.bearbones.kumalift.core.media.rememberProfileImagePicker
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AthleteSex
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
    val isEnglish = resolveIsEnglish(settings.appearance.language)

    var selectedMetric by remember { mutableStateOf(AnalyticsMetric.VOLUME) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMeasuresDialog by remember { mutableStateOf(false) }

    val athleteName = settings.profile.name.ifBlank { if (isEnglish) "Athlete" else "Atlet" }
    val athleteUsername = settings.profile.username.ifBlank { if (isEnglish) "athlete" else "atlet" }
    val workoutCount = completedWorkouts.size
    val allPrs by personalRecordRepository.personalRecords.collectAsState()
    val prCount = allPrs.size
    val isLb = settings.units.weightUnit == WeightUnit.LB
    val multiplier = if (isLb) 2.20462 else 1.0
    val unitSuffix = if (isLb) "lb" else "kg"
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
                                contentDescription = "Edit Profile",
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
                                contentDescription = "Settings",
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
                                .background(Color(0xFF111317))
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
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            StatCounter(
                                value = "$workoutCount",
                                label = if (isEnglish) "Workouts" else "Latihan"
                            )
                            StatCounter(
                                value = "$prCount",
                                label = if (isEnglish) "PR Records" else "Rekor PR"
                            )
                            StatCounter(
                                value = if (totalVolumeDisplay >= 1000.0) {
                                    val thousands = ((totalVolumeDisplay / 100.0).toInt()) / 10.0
                                    "${thousands}k"
                                } else {
                                    "${totalVolumeDisplay.toInt()}"
                                },
                                label = if (isEnglish) "Volume ($unitSuffix)" else "Volume ($unitSuffix)"
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
                            label = if (isEnglish) "Duration" else "Durasi",
                            isSelected = selectedMetric == AnalyticsMetric.DURATION,
                            onClick = { selectedMetric = AnalyticsMetric.DURATION }
                        )
                        MetricPill(
                            label = if (isEnglish) "Volume" else "Volume",
                            isSelected = selectedMetric == AnalyticsMetric.VOLUME,
                            onClick = { selectedMetric = AnalyticsMetric.VOLUME }
                        )
                        MetricPill(
                            label = if (isEnglish) "Reps" else "Repetisi",
                            isSelected = selectedMetric == AnalyticsMetric.REPS,
                            onClick = { selectedMetric = AnalyticsMetric.REPS }
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
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = when (selectedMetric) {
                                        AnalyticsMetric.DURATION -> if (isEnglish) "Workout Duration" else "Durasi Latihan"
                                        AnalyticsMetric.VOLUME -> if (isEnglish) "Total Volume Lifted" else "Total Beban Diangkat"
                                        AnalyticsMetric.REPS -> if (isEnglish) "Total Repetitions" else "Total Repetisi"
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textSecondary
                                    )
                                )

                                Text(
                                    text = if (completedWorkouts.isEmpty()) "—" else when (selectedMetric) {
                                        AnalyticsMetric.DURATION -> {
                                            val avgMins = (completedWorkouts.map { it.durationSeconds }.average() / 60).toInt()
                                            "$avgMins ${if (isEnglish) "min avg" else "mnt rata-rata"}"
                                        }
                                        AnalyticsMetric.VOLUME -> totalVolumeText
                                        AnalyticsMetric.REPS -> {
                                            val totalReps = completedWorkouts.sumOf { w ->
                                                w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }
                                            }
                                            "$totalReps reps"
                                        }
                                    },
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accentText
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            if (completedWorkouts.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(110.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (isEnglish) "No workout data yet. Complete a workout to see your progress chart!" else "Belum ada riwayat latihan. Selesaikan latihan untuk melihat grafik perkembangan!",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colors.textSecondary,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            } else {
                                val chartWorkouts = completedWorkouts.takeLast(6)
                                val barData = chartWorkouts.map { w ->
                                    when (selectedMetric) {
                                        AnalyticsMetric.DURATION -> (w.durationSeconds / 60).toFloat()
                                        AnalyticsMetric.VOLUME -> (w.totalVolumeKg * multiplier).toFloat()
                                        AnalyticsMetric.REPS -> w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }.toFloat()
                                    }
                                }
                                val barLabels = chartWorkouts.mapIndexed { idx, _ -> "S${idx + 1}" }
                                val maxVal = (barData.maxOrNull() ?: 1f).coerceAtLeast(1f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    barData.forEachIndexed { index, value ->
                                        val isCurrent = index == barData.size - 1
                                        val heightFraction = (value / maxVal).coerceIn(0.15f, 1f)

                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .width(26.dp)
                                                    .height((90 * heightFraction).dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isCurrent) colors.accentText else KumaColors.SurfaceContainer
                                                    )
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Text(
                                                text = barLabels[index],
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 11.sp,
                                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isCurrent) colors.accentText else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
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
                        text = if (isEnglish) "Dashboard" else "Dasbor",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Statistics Card
                        DashboardCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.AutoMirrored.Filled.ShowChart,
                            title = if (isEnglish) "Statistics" else "Statistik",
                            subtitle = if (isEnglish) "1RM & Volume" else "1RM & Beban",
                            onClick = onNavigateToProgress
                        )

                        // Exercises Card
                        DashboardCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            title = if (isEnglish) "Exercises" else "Katalog",
                            subtitle = if (isEnglish) "1,324 Movements" else "1.324 Gerakan",
                            onClick = onNavigateToLibrary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Measures Card
                        val bw = settings.profile.bodyweightKg ?: 75.0
                        val ht = settings.profile.heightCm ?: 175.0
                        DashboardCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.AccessibilityNew,
                            title = if (isEnglish) "Measures" else "Ukuran Tubuh",
                            subtitle = "$bw kg · ${ht.toInt()} cm",
                            onClick = { showMeasuresDialog = true }
                        )

                        // Calendar Card
                        DashboardCard(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.CalendarToday,
                            title = if (isEnglish) "Calendar" else "Kalender",
                            subtitle = if (isEnglish) "${settings.profile.trainingDaysPerWeek}d / week" else "${settings.profile.trainingDaysPerWeek} hari/minggu",
                            onClick = onNavigateToRoutines
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
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEnglish) "Workouts" else "Riwayat Latihan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                        )

                        Text(
                            text = if (isEnglish) "View All" else "Lihat Semua",
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
                                    text = if (isEnglish) "No workouts yet" else "Belum ada sesi latihan selesai",
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
                                        text = if (isEnglish) "Start Workout" else "Mulai Latihan",
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    } else {
                        // List completed workouts
                        completedWorkouts.take(5).forEach { workout ->
                            WorkoutSummaryCard(
                                title = workout.title,
                                date = "Hari ini",
                                duration = "${workout.durationSeconds / 60} mnt",
                                volume = "14.200 kg",
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
}

@Composable
private fun StatCounter(
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
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary,
                fontSize = 20.sp
            )
        )
        Text(
            text = label,
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
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    val bgColor = if (isSelected) colors.accentFill else KumaColors.SurfaceDim
    val textColor = if (isSelected) colors.onAccentFill else colors.textSecondary

    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
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

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = subtitle,
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = date,
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
                    text = "$exerciseCount Gerakan",
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
                    contentDescription = "Tutup",
                    tint = colors.textPrimary,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable(onClick = onDismiss)
                )

                Text(
                    text = "Edit Profile",
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
                    Text("Done", fontWeight = FontWeight.Bold)
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
                        .background(Color(0xFF111317))
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
                    text = "Change Picture",
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
                text = "Public profile data",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = "Name",
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = "Nama Lengkap"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = "Bio",
                value = bioInput,
                onValueChange = { bioInput = it },
                placeholder = "Deskripsikan diri Anda"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Private data section
            Text(
                text = "Private data",
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
                    text = "Sex",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = colors.textPrimary
                    )
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        AthleteSex.MALE to "Male",
                        AthleteSex.FEMALE to "Female"
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
                label = "Birthday",
                value = birthdayInput,
                onValueChange = { birthdayInput = it },
                placeholder = "misal: 15 Januari 2000"
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var weightInput by remember {
        mutableStateOf(settings.profile.bodyweightKg?.toString() ?: "75.0")
    }
    var heightInput by remember {
        mutableStateOf(settings.profile.heightCm?.toString() ?: "175.0")
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KumaColors.SurfaceDim
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                text = "Ukuran Tubuh (Measures)",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            ProfileField(
                label = "Berat Badan (${if (settings.units.weightUnit == WeightUnit.LB) "lbs" else "kg"})",
                value = weightInput,
                onValueChange = { weightInput = it },
                placeholder = "75.0"
            )

            Spacer(modifier = Modifier.height(12.dp))

            ProfileField(
                label = "Tinggi Badan (cm)",
                value = heightInput,
                onValueChange = { heightInput = it },
                placeholder = "175.0"
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    val w = weightInput.toDoubleOrNull()
                    val h = heightInput.toDoubleOrNull()
                    settingsRepository.update { current ->
                        current.copy(
                            profile = current.profile.copy(
                                bodyweightKg = w ?: current.profile.bodyweightKg,
                                heightCm = h ?: current.profile.heightCm
                            )
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Simpan Ukuran Tubuh", fontWeight = FontWeight.Bold)
            }
        }
    }
}
