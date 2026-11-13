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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.domain.model.PersonalRecord

@Composable
fun ProgressAnalyticsScreen(
    personalRecordRepository: PersonalRecordRepository
) {
    val prs by personalRecordRepository.personalRecords.collectAsState()
    var selectedTimeframe by remember { mutableStateOf("3M") }
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
                    text = "Progress & Telemetry",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )
            }

            LazyColumn(
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
                                        color = if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedTimeframe = tf },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tf,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // 1. Estimated 1RM Trajectory Card
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
                                Column {
                                    Text(
                                        text = "ESTIMATED 1RM TRAJECTORY",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.ElectricLime,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Barbell Bench Press",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.ElectricLimeGlow)
                                        .border(1.dp, KumaColors.ElectricLime, PillShape)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                            contentDescription = null,
                                            tint = KumaColors.ElectricLime,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "+7.5 kg (+6.8%)",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = KumaColors.ElectricLime,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "118.0 kg",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 32.sp,
                                    color = KumaColors.TextWhite
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Milestone Graph Representation
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
                                ChartPoint(label = "W1", heightFraction = 0.45f, value = "105")
                                ChartPoint(label = "W2", heightFraction = 0.55f, value = "107.5")
                                ChartPoint(label = "W3", heightFraction = 0.65f, value = "110")
                                ChartPoint(label = "W4", heightFraction = 0.75f, value = "112.5")
                                ChartPoint(label = "W5", heightFraction = 0.82f, value = "115")
                                ChartPoint(label = "W6", heightFraction = 1.0f, value = "118", isPeak = true)
                            }
                        }
                    }
                }

                // 2. Weekly Tonnage & Volume
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
                                Text(
                                    text = "Weekly Tonnage",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Avg 54,200 kg (+14%)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(18.dp))

                            // 6-week Volume Bar Chart
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                VolumeBar(week = "W1", heightFraction = 0.60f)
                                VolumeBar(week = "W2", heightFraction = 0.70f)
                                VolumeBar(week = "W3", heightFraction = 0.65f)
                                VolumeBar(week = "W4", heightFraction = 0.85f)
                                VolumeBar(week = "W5", heightFraction = 0.80f)
                                VolumeBar(week = "W6", heightFraction = 1.0f, isCurrent = true)
                            }
                        }
                    }
                }

                // 3. Hall of Fame (Personal Records)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = KumaColors.ElectricLime,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Hall of Fame (Personal Records)",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }

                        prs.forEach { pr ->
                            PersonalRecordItem(pr = pr)
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
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        if (isPeak) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(KumaColors.ElectricLime)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.OnElectricLime
                    )
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
        Box(
            modifier = Modifier
                .size(if (isPeak) 12.dp else 8.dp)
                .clip(CircleShape)
                .background(if (isPeak) KumaColors.ElectricLime else KumaColors.TextMuted)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isPeak) KumaColors.ElectricLime else KumaColors.TextMuted,
                fontWeight = if (isPeak) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
fun VolumeBar(week: String, heightFraction: Float, isCurrent: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height((90 * heightFraction).dp)
                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .background(if (isCurrent) KumaColors.ElectricLime else KumaColors.SurfaceContainerHigh)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = week,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                color = if (isCurrent) KumaColors.ElectricLime else KumaColors.TextMuted,
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
