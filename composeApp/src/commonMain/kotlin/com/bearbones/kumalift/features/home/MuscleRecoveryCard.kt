package com.bearbones.kumalift.features.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.MuscleRecoveryState
import com.bearbones.kumalift.domain.model.MuscleRecoveryStatus
import com.bearbones.kumalift.domain.model.MuscleRecoveryTracker
import com.bearbones.kumalift.domain.model.Workout

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuscleRecoveryCard(
    completedWorkouts: List<Workout>,
    modifier: Modifier = Modifier
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    val recoveryStates = remember(completedWorkouts) {
        MuscleRecoveryTracker.calculateRecovery(completedWorkouts)
    }

    var showDetailDialog by remember { mutableStateOf(false) }

    if (showDetailDialog) {
        MuscleRecoveryDetailDialog(
            recoveryStates = recoveryStates,
            onDismiss = { showDetailDialog = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(KumaColors.SurfaceContainerLow)
            .clickable { showDetailDialog = true }
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = s.muscleRecoveryTitle,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = KumaColors.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Text(
                    text = "72h Window",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted,
                        fontSize = 11.sp
                    )
                )
            }

            // Muscle Readiness Chips in FlowRow
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                recoveryStates.take(6).forEach { state ->
                    val (statusColor, statusText) = when (state.status) {
                        MuscleRecoveryStatus.FRESH -> Pair(Color(0xFF10B981), s.recoveryFresh)
                        MuscleRecoveryStatus.RECOVERING -> Pair(Color(0xFFF59E0B), s.recoveryRecovering)
                        MuscleRecoveryStatus.FATIGUED -> Pair(Color(0xFFEF4444), s.recoveryFatigued)
                    }

                    Row(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(KumaColors.SurfaceContainer)
                            .border(1.dp, statusColor.copy(alpha = 0.3f), PillShape)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Text(
                            text = state.muscleGroup.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                        )
                        Text(
                            text = "${state.readinessPercentage}%",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MuscleRecoveryDetailDialog(
    recoveryStates: List<MuscleRecoveryState>,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Column {
                Text(
                    text = s.muscleRecoveryTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = "Estimasi Kesiapan Otot Berdasarkan Riwayat Latihan",
                    style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                recoveryStates.forEach { state ->
                    val (statusColor, statusText) = when (state.status) {
                        MuscleRecoveryStatus.FRESH -> Pair(Color(0xFF10B981), s.recoveryFresh)
                        MuscleRecoveryStatus.RECOVERING -> Pair(Color(0xFFF59E0B), s.recoveryRecovering)
                        MuscleRecoveryStatus.FATIGUED -> Pair(Color(0xFFEF4444), s.recoveryFatigued)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.muscleGroup.name.lowercase().replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Text(
                                    text = "$statusText · ${state.readinessPercentage}%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Progress bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(PillShape)
                                .background(KumaColors.SurfaceContainerHigh)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(state.readinessPercentage / 100f)
                                    .height(6.dp)
                                    .clip(PillShape)
                                    .background(statusColor)
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (state.hoursSinceLastTrained != null) {
                                    "${state.hoursSinceLastTrained}j lalu (${state.setsLast72h} set)"
                                } else {
                                    "Belum dilatih dalam 72j"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = KumaColors.TextMuted,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // Explicit Scientific Disclaimer per requirements
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainerHigh)
                        .padding(12.dp)
                ) {
                    Text(
                        text = s.recoveryDisclaimer,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = s.close,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.accentText
                    )
                )
            }
        }
    )
}
