package com.bearbones.kumalift.features.logger

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.SmartExerciseAlternativeAdvisor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmartAlternativeDialog(
    currentExercise: Exercise,
    allExercises: List<Exercise>,
    onSelectAlternative: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var selectedEquipmentFilter by remember { mutableStateOf<Equipment?>(null) }

    val alternatives = remember(currentExercise, allExercises, selectedEquipmentFilter) {
        SmartExerciseAlternativeAdvisor.findAlternatives(
            targetExercise = currentExercise,
            allExercises = allExercises,
            equipmentFilter = selectedEquipmentFilter,
            limit = 12
        )
    }

    val equipmentFilters = listOf(
        null,
        Equipment.DUMBBELL,
        Equipment.BARBELL,
        Equipment.CABLE,
        Equipment.MACHINE,
        Equipment.BODYWEIGHT,
        Equipment.SMITH_MACHINE
    )

    AlertDialog(
        onDismissRequest = onDismiss,
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
        },
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = s.smartAlternativesTitle,
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
                Text(
                    text = "${s.smartAlternativesSubtitle}: ${currentExercise.name}",
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Equipment Filter Chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    equipmentFilters.forEach { equip ->
                        val isSelected = equip == selectedEquipmentFilter
                        val label = equip?.code ?: s.allEquipmentLabel
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isSelected) colors.surfaceBright else colors.surfaceVariant)
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) colors.accentFill else colors.border,
                                    shape = PillShape
                                )
                                .clickable { selectedEquipmentFilter = equip }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) colors.accentText else colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Alternatives List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(alternatives, key = { it.exercise.id }) { item ->
                        val (reasonColor, reasonText) = when (item.matchReasonCode) {
                            "same_pattern" -> Pair(colors.recoveryFresh, s.matchReasonSamePattern)
                            "same_muscle" -> Pair(colors.info, s.matchReasonSameMuscle)
                            else -> Pair(colors.warning, s.matchReasonSecondary)
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceVariant)
                                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectAlternative(item.exercise)
                                    onDismiss()
                                }
                                .padding(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.exercise.name,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Reason Pill
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(reasonColor.copy(alpha = 0.15f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = reasonText,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = reasonColor,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }

                                        // Equipment Code Badge
                                        Text(
                                            text = "• ${item.exercise.equipment.code}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        )

                                        // Movement Type Badge
                                        Text(
                                            text = "• ${item.exercise.movementType.name}",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = colors.textSecondary,
                                                fontSize = 10.sp
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Select / Swap Button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.accentFill)
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = s.swapExerciseButton,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.onAccentFill,
                                            fontSize = 11.sp
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
