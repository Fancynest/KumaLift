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
import androidx.compose.runtime.remember
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
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun HomeScreen(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository,
    onStartWorkout: () -> Unit,
    onStartFreestyle: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val settings by settingsRepository.settings.collectAsState()

    val currentHour = remember {
        try {
            Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).hour
        } catch (e: Exception) {
            12
        }
    }
    val isEnglish = settings.appearance.language == AppLanguage.EN
    val timeGreeting = when (currentHour) {
        in 4..10 -> if (isEnglish) "Good morning" else "Selamat pagi"
        in 11..14 -> if (isEnglish) "Good afternoon" else "Selamat siang"
        in 15..18 -> if (isEnglish) "Good afternoon" else "Selamat sore"
        else -> if (isEnglish) "Good evening" else "Selamat malam"
    }
    val athleteGreeting = settings.profile.username.ifBlank { settings.profile.name.ifBlank { "Alex" } }
    val phaseText = when (settings.profile.goal) {
        TrainingGoal.HYPERTROPHY -> if (isEnglish) "WEEK 6 · HYPERTROPHY PHASE" else "MINGGU 6 · FASE TAMBAH OTOT"
        TrainingGoal.STRENGTH -> if (isEnglish) "WEEK 6 · STRENGTH PHASE" else "MINGGU 6 · FASE KEKUATAN"
        TrainingGoal.GENERAL -> if (isEnglish) "WEEK 6 · GENERAL FITNESS" else "MINGGU 6 · KEBUGARAN UMUM"
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
                    val volumeVal = if (settings.units.weightUnit == WeightUnit.LB) "40.5k lb" else "18.4k kg"
                    val volumeBadge = if (isEnglish) "$volumeVal Volume This Week" else "$volumeVal Beban Minggu Ini"
                    val streakBadge = if (isEnglish) "4-Day Streak" else "Streak 4 Hari"
                    val targetBadge = if (isEnglish) "Goal Reached: 80%" else "Target Tercapai: 80%"

                    LazyRow(
                        modifier = Modifier.isolateHorizontalScroll(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        item {
                            KumaBadge(text = streakBadge)
                        }
                        item {
                            KumaBadge(text = volumeBadge)
                        }
                        item {
                            KumaBadge(text = targetBadge)
                        }
                    }
                }

                // 2. Weekly Horizon (7-Day Tracker)
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
                                        text = if (isEnglish) "This Week" else "Minggu Ini",
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
                                        text = "4/${settings.profile.trainingDaysPerWeek} ${if (isEnglish) "Workouts" else "Sesi Latihan"}",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.OnElectricLime,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // 7 Days Grid with comfortable breathing room
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                DayCircle(if (isEnglish) "M" else "S", isDone = true)
                                DayCircle(if (isEnglish) "T" else "S", isDone = true)
                                DayCircle(if (isEnglish) "W" else "R", isRest = true)
                                DayCircle(if (isEnglish) "T" else "K", isDone = true)
                                DayCircle(if (isEnglish) "F" else "J", isActive = true)
                                DayCircle(if (isEnglish) "S" else "S")
                                DayCircle(if (isEnglish) "S" else "M")
                            }
                        }
                    }
                }

                // 2.5. Daily Recovery & Readiness Score Card (from Wearables & Health Connect)
                item {
                    val colors = LocalKumaColors.current
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
                                        imageVector = Icons.Default.Favorite,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = if (isEnglish) "Body Readiness" else "Kesiapan Tubuh Hari Ini",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.accentTint)
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "88% · OPTIMAL" else "88% · PRIMA",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accentText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (isEnglish) "Heart & muscle recovery optimal (RHR: 56 BPM). Ready for heavy lifting or a PR attempt today." else "Pemulihan jantung & otot optimal (RHR: 56 BPM). Siap mendorong beban berat atau mencoba PR hari ini.",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = KumaColors.TextSecondary,
                                    lineHeight = 18.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentFill)
                                    )
                                    Text(
                                        text = if (isEnglish) "Synced via Health Connect" else "Sinkron via Health Connect",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accentText,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                                Text(
                                    text = if (isEnglish) "Sleep 7h 42m" else "Tidur 7j 42m",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
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
                                KumaBadge(text = if (isEnglish) "TODAY'S SCHEDULE" else "JADWAL HARI INI")
                                Text(
                                    text = if (isEnglish) "Heavy · RPE 8–9" else "Beban Berat · RPE 8–9",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Pull Day A",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = if (isEnglish) "4 exercises · 16 sets · ~55 min" else "4 gerakan · 16 set · ~55 mnt",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Exercise Preview Chips
                            LazyRow(
                                modifier = Modifier.isolateHorizontalScroll(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                item { KumaBadge(text = "Deadlift") }
                                item { KumaBadge(text = "Barbell Row") }
                                item { KumaBadge(text = "Lat Pulldown") }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            KumaPrimaryButton(
                                text = if (isEnglish) "START WORKOUT" else "MULAI LATIHAN",
                                onClick = onStartWorkout
                            )
                        }
                    }
                }

                // 4. Monthly Quick Glance Metrics (Spacious 3-column)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = if (isEnglish) "TOTAL SESSIONS" else "TOTAL SESI",
                            value = "18",
                            subtext = if (isEnglish) "+3 vs last mo" else "+3 dr bln lalu"
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = if (isEnglish) "TOTAL VOLUME" else "TOTAL BEBAN",
                            value = "64.2k",
                            subtext = if (isEnglish) "+12% tonnage" else "+12% tonase"
                        )
                        MetricTile(
                            modifier = Modifier.weight(1f),
                            label = if (isEnglish) "RECORDS (PR)" else "REKOR (PR)",
                            value = "3",
                            subtext = if (isEnglish) "New records" else "Rekor baru",
                            isAccent = true
                        )
                    }
                }

                // 5. Recent Workouts & Freestyle Button
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isEnglish) "Recent Workouts" else "Riwayat Latihan",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        // Recent Workout Item
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
                                Column {
                                    Text(
                                        text = "Push Day A",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (isEnglish) "Yesterday · 14,850 kg · 47 min" else "Kemarin · 14.850 kg · 47 mnt",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KumaColors.TextMuted
                                    )
                                }
                                KumaBadge(
                                    text = "3RM PR",
                                    isPr = true
                                )
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
                                    workoutRepository.startEmptyWorkout(if (isEnglish) "Freestyle Workout" else "Latihan Bebas")
                                    onStartFreestyle()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isEnglish) "Freestyle Workout" else "Latihan Bebas",
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
