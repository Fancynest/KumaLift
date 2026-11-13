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
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
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
                    text = "Rangkuman Latihan",
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
                                        text = "LATIHAN TUNTAS",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.OnElectricLime,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Text(
                                    text = "RPE 8.5",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = "Push Day Selesai",
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 28.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "Hari ini · 11:30 – 12:17",
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
                                label = "DURASI",
                                value = "47:20",
                                subtext = "Target: +4 mnt",
                                icon = Icons.Default.Timer
                            )
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "TOTAL BEBAN",
                                value = "14,850 kg",
                                subtext = "+8% dr sesi lalu",
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
                                label = "SET TUNTAS",
                                value = "16 / 16",
                                subtext = "100% disiplin",
                                icon = Icons.Default.CheckCircle
                            )
                            SummaryMetricCard(
                                modifier = Modifier.weight(1f),
                                label = "RATA-RATA REHAT",
                                value = "88 dtk",
                                subtext = "Est. 420 kkal",
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
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "REKOR BARU (PR)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                KumaBadge(
                                    text = "+3.0 kg",
                                    isPr = true
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Barbell Bench Press: 118.0 kg",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "3RM PR",
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextMuted
                            )
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
                                text = "Fokus Otot yang Dilatih",
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
                                    text = "Dada: 52%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                Text(
                                    text = "Bahu: 28%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                                Text(
                                    text = "Trisep: 20%",
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
                        text = "SIMPAN & AKHIRI SESI",
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
                    tint = if (isAccent) LocalKumaColors.current.accentText else KumaColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isAccent) LocalKumaColors.current.accentText else LocalKumaColors.current.textPrimary
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtext,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    color = if (isAccent) LocalKumaColors.current.accentText else KumaColors.TextMuted
                )
            )
        }
    }
}
