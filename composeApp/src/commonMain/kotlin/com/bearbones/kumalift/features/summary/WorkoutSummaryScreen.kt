package com.bearbones.kumalift.features.summary

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.domain.model.Workout

@Composable
fun WorkoutSummaryScreen(
    workout: Workout? = null,
    onSaveAndClose: () -> Unit
) {
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
                    text = "Workout Summary",
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
                    // Session Header Card
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
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.ElectricLime)
                                        .padding(horizontal = 12.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "SESSION LOGGED",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.OnElectricLime,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "RPE 8.5 · Peak Drive",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Push Day Complete",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Today · 11:30 AM – 12:17 PM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextMuted
                            )
                        }
                    }
                }

                // Core 2x2 Grid Metrics
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "DURATION",
                                value = "47:20",
                                subtext = "Pace: +4m vs target",
                                icon = Icons.Default.Timer
                            )
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "TOTAL VOLUME",
                                value = "14,850 kg",
                                subtext = "+8% vs prev split",
                                icon = Icons.Default.FitnessCenter,
                                isAccent = true
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "SETS COMPLETED",
                                value = "16 / 16",
                                subtext = "100% adherence",
                                icon = Icons.Default.CheckCircle
                            )
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "AVG REST INTERVAL",
                                value = "88 sec",
                                subtext = "420 kcal estimated",
                                icon = Icons.Default.Speed
                            )
                        }
                    }
                }

                // PR Showcase Card
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainer,
                        cornerRadius = 16.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KumaColors.ElectricLimeGlow),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = null,
                                    tint = KumaColors.ElectricLime,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column {
                                Text(
                                    text = "NEW PR ACHIEVED",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.ElectricLime,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Barbell Bench Press: 118.0 kg",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "3RM PR (+3.0 kg breakthrough)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = KumaColors.TextMuted
                                )
                            }
                        }
                    }
                }

                // Volume Distribution by Muscle Group
                item {
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "Volume Distribution",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(14.dp))

                            // Segmented bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(PillShape)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(0.52f)
                                        .fillMaxSize()
                                        .background(KumaColors.ElectricLime)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.28f)
                                        .fillMaxSize()
                                        .background(KumaColors.SurfaceContainerHigh)
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(0.20f)
                                        .fillMaxSize()
                                        .background(KumaColors.SecondaryContainer)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Chest: 52%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Shoulders: 28%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                                Text(
                                    text = "Triceps: 20%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }
                        }
                    }
                }

                // Save Action
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    KumaPrimaryButton(
                        text = "SAVE & FINISH WORKOUT",
                        onClick = onSaveAndClose
                    )
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }
    }
}

@Composable
fun SummaryMetricCard(
    label: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isAccent: Boolean = false
) {
    KumaCard(
        modifier = modifier,
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        color = KumaColors.TextMuted
                    )
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isAccent) KumaColors.ElectricLime else KumaColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAccent) KumaColors.ElectricLime else KumaColors.TextWhite
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = if (isAccent) KumaColors.ElectricLime else KumaColors.TextMuted
                )
            )
        }
    }
}
