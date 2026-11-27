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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
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
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = s.close,
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
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = s.todayReadinessScore,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary
                                )
                            )
                            Text(
                                text = "$percentage%",
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = statusColor
                                )
                            )
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(PillShape)
                                .background(statusColor.copy(alpha = 0.15f))
                                .border(1.dp, statusColor, PillShape)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = statusText,
                                textAlign = TextAlign.Center,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = statusColor,
                                    fontSize = 11.sp
                                ),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // 1. Sleep Quality
                ReadinessPillarRow(
                    label = s.sleepQualityLabel,
                    icon = Icons.Default.Nightlight,
                    value = sleep,
                    onValueChange = { sleep = it },
                    minLabel = s.readinessSleepPoor,
                    midLabel = s.readinessLevelModerate,
                    maxLabel = s.readinessSleepGreat
                )

                // 2. Muscle Soreness
                ReadinessPillarRow(
                    label = s.muscleSorenessLabel,
                    icon = Icons.Default.Healing,
                    value = soreness,
                    onValueChange = { soreness = it },
                    minLabel = s.readinessSorenessSore,
                    midLabel = s.readinessLevelModerate,
                    maxLabel = s.readinessSorenessFresh
                )

                // 3. Energy Level
                ReadinessPillarRow(
                    label = s.energyLevelLabel,
                    icon = Icons.Default.Speed,
                    value = energy,
                    onValueChange = { energy = it },
                    minLabel = s.readinessLevelLow,
                    midLabel = s.readinessLevelModerate,
                    maxLabel = s.readinessLevelHigh
                )

                // 4. Stress Level
                ReadinessPillarRow(
                    label = s.stressLevelLabel,
                    icon = Icons.Default.Psychology,
                    value = stress,
                    onValueChange = { stress = it },
                    minLabel = s.readinessStressHigh,
                    midLabel = s.readinessLevelModerate,
                    maxLabel = s.readinessStressCalm
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
    onValueChange: (Int) -> Unit,
    minLabel: String,
    midLabel: String,
    maxLabel: String
) {
    val colors = LocalKumaColors.current

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
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
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
            }
            Text(
                text = "$value / 5",
                maxLines = 1,
                softWrap = false,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.accentText
                )
            )
        }

        // 1 - 5 Pill Selector with indicators for 1, 3, 5
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (level in 1..5) {
                val isSelected = level == value
                val indicator = when (level) {
                    1 -> minLabel
                    3 -> midLabel
                    5 -> maxLabel
                    else -> null
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) colors.accentFill else colors.surfaceVariant)
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) colors.accentFill else colors.border,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onValueChange(level) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$level",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                color = if (isSelected) colors.onAccentFill else colors.textPrimary,
                                fontSize = 14.sp
                            )
                        )
                    }

                    Text(
                        text = indicator ?: "",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = if (isSelected) colors.accentText else colors.textSecondary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 9.sp,
                            letterSpacing = 0.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
