package com.bearbones.kumalift.features.mesocycle

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.bearbones.kumalift.data.repository.MesocycleRepository
import com.bearbones.kumalift.domain.model.MesocycleBlock
import com.bearbones.kumalift.domain.model.MesocyclePhase

@Composable
fun MesocycleSection(
    mesocycleRepository: MesocycleRepository,
    modifier: Modifier = Modifier
) {
    val activeBlock by mesocycleRepository.activeMesocycle.collectAsState()
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    var showCreateDialog by remember { mutableStateOf(false) }

    if (showCreateDialog) {
        NewMesocycleDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, totalWeeks, deloadWeek ->
                mesocycleRepository.startNewMesocycle(
                    name = name,
                    totalWeeks = totalWeeks,
                    deloadWeek = deloadWeek
                )
                showCreateDialog = false
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = colors.accentFill,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = s.mesocycleTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.accentFill.copy(alpha = 0.15f))
                    .clickable { showCreateDialog = true }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = colors.accentText,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = s.startNewMesocycleAction,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.accentText
                        )
                    )
                }
            }
        }

        val block = activeBlock
        if (block != null) {
            ActiveMesocycleCard(
                block = block,
                onAdvanceWeek = { mesocycleRepository.advanceWeek() },
                onToggleDeload = { mesocycleRepository.toggleDeload() },
                onCompleteBlock = { mesocycleRepository.completeMesocycle(block.id) }
            )
        } else {
            EmptyMesocycleCard(
                onStartNewBlock = { showCreateDialog = true }
            )
        }
    }
}

@Composable
private fun ActiveMesocycleCard(
    block: MesocycleBlock,
    onAdvanceWeek: () -> Unit,
    onToggleDeload: () -> Unit,
    onCompleteBlock: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val phaseColor = when (block.phase) {
        MesocyclePhase.INTRODUCTORY -> colors.info
        MesocyclePhase.ACCUMULATION -> colors.accentFill
        MesocyclePhase.PEAK_OVERLOAD -> colors.warning
        MesocyclePhase.DELOAD -> colors.recoveryFresh
    }

    val phaseLabel = when (block.phase) {
        MesocyclePhase.INTRODUCTORY -> s.phaseIntroductory
        MesocyclePhase.ACCUMULATION -> s.phaseAccumulation
        MesocyclePhase.PEAK_OVERLOAD -> s.phasePeakOverload
        MesocyclePhase.DELOAD -> s.phaseDeload
    }

    KumaCard(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Block Name & Phase Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = block.name,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = s.mesocycleWeekFormat(block.currentWeek, block.totalWeeks),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(phaseColor.copy(alpha = 0.15f))
                        .border(1.dp, phaseColor.copy(alpha = 0.4f), PillShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = phaseLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = phaseColor
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Week Dots Timeline
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (w in 1..block.totalWeeks) {
                    val isPast = w < block.currentWeek
                    val isCurrent = w == block.currentWeek
                    val isDeloadWk = w == block.deloadWeek

                    val dotColor = when {
                        isCurrent -> phaseColor
                        isPast -> colors.accentFill.copy(alpha = 0.6f)
                        isDeloadWk -> colors.recoveryFresh.copy(alpha = 0.5f)
                        else -> colors.surfaceVariant
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (isCurrent) 22.dp else 16.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .then(
                                    if (isCurrent) Modifier.border(2.dp, colors.textPrimary, CircleShape) else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPast) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = KumaColors.CanvasLowest,
                                    modifier = Modifier.size(10.dp)
                                )
                            } else if (isDeloadWk) {
                                Icon(
                                    imageVector = Icons.Default.Eco,
                                    contentDescription = null,
                                    tint = if (isCurrent) colors.onAccentFill else colors.recoveryFresh,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }
                        Text(
                            text = "W$w",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) colors.textPrimary else colors.textSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Advance Week
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.accentFill)
                        .clickable(onClick = onAdvanceWeek)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = null,
                            tint = colors.onAccentFill,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (block.currentWeek < block.totalWeeks) s.advanceWeekAction else s.completeMesocycleAction,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.onAccentFill
                            )
                        )
                    }
                }

                // Toggle Deload
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (block.isDeloadActive) colors.recoveryFresh else colors.surfaceVariant)
                        .clickable(onClick = onToggleDeload)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Eco,
                            contentDescription = null,
                            tint = if (block.isDeloadActive) colors.onRecoveryFresh else colors.recoveryFresh,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = s.toggleDeloadAction,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = if (block.isDeloadActive) colors.onRecoveryFresh else colors.textPrimary
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyMesocycleCard(
    onStartNewBlock: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    KumaCard(
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = s.noActiveMesocycle,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary
                )
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentFill)
                    .clickable(onClick = onStartNewBlock)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.startNewMesocycleAction,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccentFill
                    )
                )
            }
        }
    }
}
