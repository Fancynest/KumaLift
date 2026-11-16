package com.bearbones.kumalift.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import com.bearbones.kumalift.domain.model.localizedMuscleFocus
import com.bearbones.kumalift.domain.model.localizedRpeTier
import com.bearbones.kumalift.domain.model.localizedSplitCategory
import com.bearbones.kumalift.domain.model.localizedTitle
import com.bearbones.kumalift.features.library.ExerciseThumbnail

@Composable
fun RoutineDetailScreen(
    routine: RoutineBlueprint,
    onNavigateBack: () -> Unit,
    onStartWorkout: (RoutineBlueprint) -> Unit,
    onEditRoutine: (RoutineBlueprint) -> Unit,
    onDeleteRoutine: (RoutineBlueprint) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    val configs = routine.effectiveConfigs

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = s.back,
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = s.routineDetailTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Edit Routine Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.accentFill)
                            .clickable { onEditRoutine(routine) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = s.editRoutineButton,
                                tint = colors.onAccentFill,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = s.editRoutineButton,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.onAccentFill
                                )
                            )
                        }
                    }

                    // Delete Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .clickable { showDeleteDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = s.deleteRoutineContentDesc,
                            tint = colors.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Overview Card
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    KumaBadge(text = routine.localizedSplitCategory(s))
                                    if (routine.scheduledDays.isNotEmpty()) {
                                        val dayLabels = routine.scheduledDays.mapNotNull { s.dayNamesShort.getOrNull(it - 1) }.joinToString(", ")
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(LocalKumaColors.current.accentTint)
                                                .padding(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = dayLabels,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = LocalKumaColors.current.accentText
                                                )
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = routine.lastPerformedDaysAgo?.let { s.lastPerformedDaysAgo(it) } ?: s.newBadge,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Text(
                                text = routine.localizedTitle(s),
                                style = MaterialTheme.typography.headlineSmall.copy(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Text(
                                text = "${routine.localizedMuscleFocus(s)} · ${routine.estimatedDurationMinutes} ${s.minAbbrev} · ${routine.localizedRpeTier(s)}",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textSecondary,
                                    fontSize = 14.sp
                                )
                            )

                            Spacer(modifier = Modifier.height(20.dp))

                            // Start Workout Action Button
                            Button(
                                onClick = { onStartWorkout(routine) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = KumaColors.ElectricLime,
                                    contentColor = KumaColors.OnElectricLime
                                ),
                                shape = PillShape
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = s.startRoutineButton,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // Exercises Section Header
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.exercisesInRoutineHeader.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = s.exerciseCountLabel(configs.size),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Exercise items with full config (sets, reps, weight, rest)
                items(configs) { config ->
                    RoutineExerciseDetailCard(config = config)
                }

                item {
                    Spacer(modifier = Modifier.height(90.dp))
                }
            }
        }

        // Delete Confirmation Dialog
        if (showDeleteDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = {
                    Text(
                        text = s.deleteRoutineDialogTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = s.deleteRoutineDialogMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showDeleteDialog = false
                            onDeleteRoutine(routine)
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = s.delete,
                            color = colors.error,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(text = s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
                    }
                },
                containerColor = KumaColors.SurfaceContainerHigh,
                titleContentColor = KumaColors.TextWhite,
                textContentColor = KumaColors.TextSecondary
            )
        }
    }
}

@Composable
private fun RoutineExerciseDetailCard(config: RoutineExerciseConfig) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val ex = config.exercise

    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 14.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExerciseThumbnail(
                    exercise = ex,
                    size = 50.dp
                )

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = ex.name,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 15.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${ex.equipment.label(s)} · ${ex.primaryMuscle.label(s)}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = colors.textSecondary,
                            fontSize = 13.sp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Configuration Badges (Sets, Reps, Weight, Rest Time)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val effectiveSets = config.effectiveSets
                val repsLabel = if (config.setConfigs.isNotEmpty()) {
                    val repsList = config.setConfigs.map { it.targetReps }
                    if (repsList.distinct().size == 1) {
                        "${repsList.first()} ${s.repsAbbrev}"
                    } else {
                        "${repsList.joinToString("-")} ${s.repsAbbrev}"
                    }
                } else {
                    "${config.targetReps} ${s.repsAbbrev}"
                }
                ConfigBadge(
                    label = "${effectiveSets.size} ${s.setsAbbrev}",
                    containerColor = KumaColors.SurfaceContainer
                )
                ConfigBadge(
                    label = repsLabel,
                    containerColor = KumaColors.SurfaceContainer
                )
                if (config.targetWeightKg != null && config.targetWeightKg > 0) {
                    ConfigBadge(
                        label = "${config.targetWeightKg} ${s.unitKg}",
                        containerColor = KumaColors.SurfaceContainer
                    )
                } else {
                    ConfigBadge(
                        label = s.progressiveLabel,
                        containerColor = KumaColors.SurfaceContainer
                    )
                }
                ConfigBadge(
                    label = s.secondsRestLabel(config.restTimerSeconds),
                    containerColor = KumaColors.SurfaceContainer
                )
            }
        }
    }
}

@Composable
private fun ConfigBadge(
    label: String,
    containerColor: Color
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(containerColor)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                fontSize = 11.sp
            )
        )
    }
}
