package com.bearbones.kumalift.features.readiness

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.DailyReadinessRepository
import com.bearbones.kumalift.domain.model.ReadinessRecommendation

@Composable
fun DailyReadinessCard(
    readinessRepository: DailyReadinessRepository,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val todayCheckIn by readinessRepository.todayCheckIn.collectAsState()

    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        DailyReadinessDialog(
            initialSleep = todayCheckIn?.sleepScore ?: 4,
            initialSoreness = todayCheckIn?.sorenessScore ?: 4,
            initialEnergy = todayCheckIn?.energyScore ?: 4,
            initialStress = todayCheckIn?.stressScore ?: 4,
            initialNotes = todayCheckIn?.notes ?: "",
            onConfirm = { sleep, soreness, energy, stress, notes ->
                readinessRepository.recordCheckIn(sleep, soreness, energy, stress, notes)
            },
            onDismiss = { showDialog = false }
        )
    }

    val current = todayCheckIn
    val (statusColor, statusText) = if (current != null) {
        when (current.recommendation) {
            ReadinessRecommendation.FULL_INTENSITY -> Pair(colors.recoveryFresh, s.readinessScoreFullIntensity)
            ReadinessRecommendation.MODERATE_LOAD -> Pair(colors.warning, s.readinessScoreModerate)
            ReadinessRecommendation.LIGHT_RECOVERY -> Pair(colors.error, s.readinessScoreLightRecovery)
        }
    } else {
        Pair(colors.textSecondary, "")
    }

    KumaCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 18.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
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
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = s.dailyReadinessTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                if (current != null) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(statusColor.copy(alpha = 0.15f))
                            .border(1.dp, statusColor, PillShape)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${current.overallPercentage}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.accentFill)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.checkInTodayAction,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.onAccentFill
                            )
                        )
                    }
                }
            }

            if (current != null) {
                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )

                // 4 Pillars mini indicators
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniPillarMetric(label = s.sleepQualityLabel, value = current.sleepScore, icon = Icons.Default.Nightlight)
                    MiniPillarMetric(label = s.muscleSorenessLabel, value = current.sorenessScore, icon = Icons.Default.Healing)
                    MiniPillarMetric(label = s.energyLevelLabel, value = current.energyScore, icon = Icons.Default.Speed)
                    MiniPillarMetric(label = s.stressLevelLabel, value = current.stressScore, icon = Icons.Default.Psychology)
                }
            } else {
                Text(
                    text = s.dailyReadinessCardSubtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textSecondary
                    )
                )
            }
        }
    }
}

@Composable
private fun MiniPillarMetric(
    label: String,
    value: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val colors = LocalKumaColors.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.accentText,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$value/5",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                fontSize = 11.sp
            )
        )
    }
}
