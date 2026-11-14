package com.bearbones.kumalift.features.progress

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.PersonalRecord

@Composable
fun ProgressAnalyticsScreen(
    personalRecordRepository: PersonalRecordRepository,
    settingsRepository: SettingsRepository? = null,
    workoutRepository: com.bearbones.kumalift.data.repository.WorkoutRepository? = null,
    listState: LazyListState = rememberLazyListState()
) {
    val settings = settingsRepository?.settings?.collectAsState()?.value
    val isEnglish = resolveIsEnglish(settings?.appearance?.language ?: AppLanguage.SYSTEM)
    val isLb = settings?.units?.weightUnit == com.bearbones.kumalift.domain.model.WeightUnit.LB
    val mult = if (isLb) 2.20462 else 1.0
    val unitStr = if (isLb) "lb" else "kg"
    val prs by personalRecordRepository.personalRecords.collectAsState()
    val completedWorkouts by (workoutRepository?.completedWorkouts ?: kotlinx.coroutines.flow.MutableStateFlow(emptyList())).collectAsState()
    var selectedTimeframe by rememberSaveable { mutableStateOf("3M") }
    val timeframes = listOf("1M", "3M", "6M", "1Y", "ALL")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Top Header with statusBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isEnglish) "Progress & Analytics" else "Perkembangan Latihan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Timeframe Segmented Control
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        timeframes.forEach { tf ->
                            val isSelected = tf == selectedTimeframe
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer)
                                    .border(
                                        width = if (isSelected) 1.dp else 0.dp,
                                        color = if (isSelected) (if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart) else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTimeframe = tf },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LocalKumaColors.current.accentText else KumaColors.TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // 1. Estimated 1RM Trajectory Card
                item {
                    val topPr = prs.maxByOrNull { it.weightKg }
                    if (topPr == null) {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isEnglish) "ESTIMATED ONE REP MAX (1RM)" else "ESTIMASI BEBAN MAKSIMAL (1RM)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isEnglish) "No PR records logged yet.\nComplete workouts to unlock 1RM estimates and progress charts."
                                    else "Belum ada rekor PR tercatat.\nSelesaikan latihan untuk melihat estimasi 1RM dan grafik perkembangan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KumaColors.TextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else {
                        val exercisePrs = prs.filter { it.exerciseId == topPr.exerciseId }.sortedBy { it.dateAchieved }
                        val prWeightVal = (topPr.weightKg * mult).let { if (it % 1.0 == 0.0) "${it.toInt()} $unitStr" else "$it $unitStr" }

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
                                    Column {
                                        Text(
                                            text = if (isEnglish) "ESTIMATED ONE REP MAX (1RM)" else "ESTIMASI BEBAN MAKSIMAL (1RM)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = LocalKumaColors.current.accentText,
                                                letterSpacing = 0.8.sp
                                            )
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = topPr.exerciseName,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(LocalKumaColors.current.accentTint)
                                            .border(
                                                1.dp,
                                                if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentFillBorder,
                                                PillShape
                                            )
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                                contentDescription = null,
                                                tint = LocalKumaColors.current.accentTextOnTint,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = topPr.prTier.ifBlank { "PR" },
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = LocalKumaColors.current.accentTextOnTint,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = prWeightVal,
                                    style = MaterialTheme.typography.headlineLarge.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 32.sp,
                                        color = KumaColors.TextWhite
                                    )
                                )

                                Spacer(modifier = Modifier.height(20.dp))

                                val maxPrWeight = (exercisePrs.maxOfOrNull { it.weightKg } ?: 1.0).coerceAtLeast(1.0)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(95.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KumaColors.SurfaceContainer)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    exercisePrs.takeLast(6).forEachIndexed { idx, p ->
                                        val isPeak = idx == exercisePrs.takeLast(6).size - 1
                                        val hf = (p.weightKg / maxPrWeight).toFloat().coerceIn(0.2f, 1f)
                                        val dispVal = (p.weightKg * mult).let { if (it % 1.0 == 0.0) "${it.toInt()}" else "$it" }
                                        ChartPoint(
                                            label = "P${idx + 1}",
                                            heightFraction = hf,
                                            value = dispVal,
                                            isPeak = isPeak
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. Weekly Tonnage & Volume
                item {
                    if (completedWorkouts.isEmpty()) {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 18.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isEnglish) "Weekly Volume & Tonnage" else "Total Beban per Minggu",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isEnglish) "No workout volume history yet.\nLog your first session to track your weekly tonnage."
                                    else "Belum ada riwayat beban latihan.\nCatat sesi pertamamu untuk melacak tonase mingguan.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KumaColors.TextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    } else {
                        val recentWorkouts = completedWorkouts.takeLast(6)
                        val maxVol = (recentWorkouts.maxOfOrNull { it.totalVolumeKg } ?: 1.0).coerceAtLeast(1.0)
                        val avgVol = (recentWorkouts.map { it.totalVolumeKg }.average() * mult).toInt()

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
                                    Text(
                                        text = if (isEnglish) "Weekly Volume & Tonnage" else "Total Beban per Minggu",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                    Text(
                                        text = "${if (isEnglish) "Avg" else "Rata-rata"} $avgVol $unitStr",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = LocalKumaColors.current.accentText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    recentWorkouts.forEachIndexed { idx, w ->
                                        val hf = (w.totalVolumeKg / maxVol).toFloat().coerceIn(0.15f, 1f)
                                        VolumeBar(
                                            week = "S${idx + 1}",
                                            heightFraction = hf,
                                            isCurrent = idx == recentWorkouts.size - 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Hall of Fame (Personal Records)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = if (isEnglish) "Personal Records (PR)" else "Rekor Angkatan Terbaik (PR)",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        if (prs.isEmpty()) {
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
                                        text = if (isEnglish) "No personal records recorded yet." else "Belum ada rekor angkatan terbaik (PR) yang tercatat.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = KumaColors.TextMuted,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            prs.forEach { pr ->
                                PersonalRecordItem(pr = pr)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun ChartPoint(label: String, heightFraction: Float, value: String, isPeak: Boolean = false) {
    val colors = LocalKumaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isPeak) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.accentFill)
                    .border(
                        1.dp,
                        if (colors.isDark) Color.Transparent else colors.accentFillBorder,
                        PillShape
                    )
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccentFill
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .size(if (isPeak) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(if (isPeak) (if (colors.isDark) colors.accentFill else colors.accentChart) else colors.textSubtle)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isPeak) colors.accentText else colors.textSubtle,
                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
fun VolumeBar(week: String, heightFraction: Float, isCurrent: Boolean = false) {
    val colors = LocalKumaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height((90 * heightFraction).dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (isCurrent) (if (colors.isDark) colors.accentFill else colors.accentChart) else colors.surfaceBright)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = week,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isCurrent) colors.accentText else colors.textSubtle,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
fun PersonalRecordItem(pr: PersonalRecord) {
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
                    text = pr.exerciseName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${pr.weightKg} kg × ${pr.reps} reps · Est 1RM: ${pr.estimated1RmKg} kg",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = KumaColors.TextMuted,
                        fontSize = 13.sp
                    )
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                KumaBadge(
                    text = pr.prTier,
                    isPr = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = pr.dateAchieved,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted,
                        fontSize = 11.sp
                    )
                )
            }
        }
    }
}
