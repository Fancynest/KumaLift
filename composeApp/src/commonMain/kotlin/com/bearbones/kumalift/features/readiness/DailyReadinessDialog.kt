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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.DailyReadinessCheckIn
import com.bearbones.kumalift.domain.model.ReadinessRecommendation

@Composable
fun DailyReadinessDialog(
    initialSleep: Int = 4,
    initialSoreness: Int = 4,
    initialEnergy: Int = 4,
    initialStress: Int = 4,
    initialNotes: String = "",
    onConfirm: (sleep: Int, soreness: Int, energy: Int, stress: Int, notes: String) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var sleep by remember { mutableStateOf(initialSleep) }
    var soreness by remember { mutableStateOf(initialSoreness) }
    var energy by remember { mutableStateOf(initialEnergy) }
    var stress by remember { mutableStateOf(initialStress) }
    var notes by remember { mutableStateOf(initialNotes) }

    val (percentage, recommendation) = remember(sleep, soreness, energy, stress) {
        DailyReadinessCheckIn.calculate(sleep, soreness, energy, stress)
    }

    val (statusColor, statusText) = when (recommendation) {
        ReadinessRecommendation.FULL_INTENSITY -> Pair(colors.recoveryFresh, s.readinessScoreFullIntensity)
        ReadinessRecommendation.MODERATE_LOAD -> Pair(colors.warning, s.readinessScoreModerate)
        ReadinessRecommendation.LIGHT_RECOVERY -> Pair(colors.error, s.readinessScoreLightRecovery)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentFill)
                    .clickable {
                        onConfirm(sleep, soreness, energy, stress, notes)
                        onDismiss()
                    }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.save,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccentFill
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = s.close,
                    style = MaterialTheme.typography.labelMedium.copy(color = colors.textSecondary)
                )
            }
        },
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
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
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = s.dailyReadinessTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = colors.textSecondary
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Real-time Readiness Score Gauge
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surfaceVariant.copy(alpha = 0.5f))
                        .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = s.todayReadinessScore,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor, PillShape)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                // 1. Sleep Quality
                ReadinessPillarRow(
                    label = s.sleepQualityLabel,
                    icon = Icons.Default.Nightlight,
                    value = sleep,
                    onValueChange = { sleep = it }
                )

                // 2. Muscle Soreness
                ReadinessPillarRow(
                    label = s.muscleSorenessLabel,
                    icon = Icons.Default.Healing,
                    value = soreness,
                    onValueChange = { soreness = it }
                )

                // 3. Energy Level
                ReadinessPillarRow(
                    label = s.energyLevelLabel,
                    icon = Icons.Default.Speed,
                    value = energy,
                    onValueChange = { energy = it }
                )

                // 4. Stress Level
                ReadinessPillarRow(
                    label = s.stressLevelLabel,
                    icon = Icons.Default.Psychology,
                    value = stress,
                    onValueChange = { stress = it }
                )

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(s.readinessNotesLabel) },
                    placeholder = { Text(s.readinessNotesPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accentFill
                    ),
                    maxLines = 3
                )
            }
        }
    )
}

@Composable
private fun ReadinessPillarRow(
    label: String,
    icon: ImageVector,
    value: Int,
    onValueChange: (Int) -> Unit
) {
    val colors = LocalKumaColors.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
            }
            Text(
                text = "$value / 5",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.accentText
                )
            )
        }

        // 1 - 5 Pill Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (level in 1..5) {
                val isSelected = level == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.accentFill else colors.surfaceVariant)
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accentFill else colors.border,
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable { onValueChange(level) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$level",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isSelected) colors.onAccentFill else colors.textPrimary
                        )
                    )
                }
            }
        }
    }
}
