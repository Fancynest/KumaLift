package com.bearbones.kumalift.features.logger

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaSetCheckbox
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.timer.RestTimer
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.WorkoutExercise

@Composable
fun ActiveWorkoutLoggerScreen(
    workoutRepository: WorkoutRepository,
    onNavigateBack: () -> Unit,
    onFinishWorkout: () -> Unit,
    onAddExerciseClick: () -> Unit
) {
    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val scope = rememberCoroutineScope()
    val restTimer = remember { RestTimer(scope) }
    val timerState by restTimer.state.collectAsState()

    var showRestTimerBar by remember { mutableStateOf(false) }

    val workout = activeWorkout

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        if (workout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "No active workout. Start one from Home or Routines!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = KumaColors.TextMuted
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(KumaColors.SurfaceDim)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = KumaColors.OnSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Active Workout",
                            style = MaterialTheme.typography.headlineSmall,
                            color = KumaColors.TextWhite
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(KumaColors.ElectricLime),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "KL",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = KumaColors.OnElectricLime
                            )
                        )
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(4.dp))
                        // Top Telemetry Header Card
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(KumaColors.ElectricLime)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "ACTIVE TELEMETRY",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = KumaColors.ElectricLime
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = workout.title,
                                            style = MaterialTheme.typography.headlineMedium
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Timer Box
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(KumaColors.SurfaceContainerHigh)
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = KumaColors.TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "34:18",
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = KumaColors.TextWhite,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            )
                                        }

                                        // Finish Button
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(KumaColors.ElectricLime)
                                                .clickable {
                                                    workoutRepository.finishWorkout()
                                                    onFinishWorkout()
                                                }
                                                .padding(horizontal = 14.dp, vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(
                                                    text = "Finish",
                                                    style = MaterialTheme.typography.labelLarge.copy(
                                                        fontWeight = FontWeight.Bold,
                                                        color = KumaColors.OnElectricLime
                                                    )
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Flag,
                                                    contentDescription = null,
                                                    tint = KumaColors.OnElectricLime,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Progress Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Progress Overview",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KumaColors.TextSecondary
                                    )
                                    Text(
                                        text = "${workout.totalCompletedSets} of ${workout.totalSets} sets (${(workout.progressFraction * 100).toInt()}%)",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = KumaColors.ElectricLime
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { workout.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(PillShape),
                                    color = KumaColors.ElectricLime,
                                    trackColor = KumaColors.SurfaceContainerHighest
                                )
                            }
                        }
                    }

                    // Exercise Cards List
                    items(workout.exercises, key = { it.id }) { we ->
                        ExerciseLoggingCard(
                            workoutExercise = we,
                            onToggleSet = { setId ->
                                workoutRepository.toggleSetCompletion(we.exercise.id, setId) { completed ->
                                    if (completed) {
                                        showRestTimerBar = true
                                        restTimer.start(90)
                                    }
                                }
                            },
                            onUpdateSet = { setId, weight, reps ->
                                workoutRepository.updateSet(we.exercise.id, setId, weight, reps)
                            },
                            onAddSet = {
                                workoutRepository.addSetToExercise(we.exercise.id)
                            }
                        )
                    }

                    // Bottom Action Buttons
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KumaColors.SurfaceContainer)
                                    .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(12.dp))
                                    .clickable(onClick = onAddExerciseClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = KumaColors.ElectricLime,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Add Exercise",
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            color = KumaColors.TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // Persistent Floating Rest Timer Pill
        AnimatedVisibility(
            visible = showRestTimerBar && timerState.remainingSeconds > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            FloatingRestTimerCard(
                timerState = timerState,
                onAdd30s = { restTimer.addSeconds(30) },
                onMinus15s = { restTimer.addSeconds(-15) },
                onDismiss = {
                    showRestTimerBar = false
                    restTimer.reset()
                }
            )
        }
    }
}

@Composable
fun ExerciseLoggingCard(
    workoutExercise: WorkoutExercise,
    onToggleSet: (String) -> Unit,
    onUpdateSet: (String, Double, Int) -> Unit,
    onAddSet: () -> Unit
) {
    val exercise = workoutExercise.exercise

    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        KumaBadge(text = "${exercise.equipment.code} · ${exercise.primaryMuscle.displayName}")
                        if (exercise.currentPrWeightKg != null) {
                            KumaBadge(
                                text = "PR: ${exercise.currentPrWeightKg} kg",
                                isPr = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(KumaColors.SurfaceContainer)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = KumaColors.TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Set Matrix Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text = "PREVIOUS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.weight(1.4f)
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "LOG",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Set Rows
            workoutExercise.sets.forEach { set ->
                SetRowItem(
                    set = set,
                    onToggle = { onToggleSet(set.id) },
                    onWeightChange = { newWeight -> onUpdateSet(set.id, newWeight, set.reps) },
                    onRepsChange = { newReps -> onUpdateSet(set.id, set.weightKg, newReps) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // + Add Set Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onAddSet),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = KumaColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Add Set",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = KumaColors.OnSurface,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
fun SetRowItem(
    set: SetLog,
    onToggle: () -> Unit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit
) {
    val bg = if (set.isCompleted) KumaColors.SurfaceContainer.copy(alpha = 0.5f) else KumaColors.SurfaceContainer

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number / type
        Box(modifier = Modifier.width(36.dp)) {
            Text(
                text = "${set.setNumber}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (set.isCompleted) KumaColors.ElectricLime else KumaColors.TextWhite
                )
            )
        }

        // Previous
        Box(modifier = Modifier.weight(1.2f)) {
            val prevText = if (set.previousWeightKg != null && set.previousReps != null) {
                "${set.previousWeightKg} × ${set.previousReps}"
            } else "-"
            Text(
                text = prevText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = KumaColors.TextMuted,
                    fontSize = 13.sp
                )
            )
        }

        // KG input box with tap increment
        Box(
            modifier = Modifier
                .weight(1.4f)
                .padding(end = 4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable { onWeightChange(set.weightKg + 2.5) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.weightKg}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.TextWhite
                )
            )
        }

        // Reps input box with tap increment
        Box(
            modifier = Modifier
                .weight(1.2f)
                .padding(end = 4.dp)
                .height(38.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable { onRepsChange((set.reps + 1) % 30) },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${set.reps}",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.TextWhite
                )
            )
        }

        // Complete Checkbox
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            KumaSetCheckbox(
                isCompleted = set.isCompleted,
                onToggle = onToggle
            )
        }
    }
}

@Composable
fun FloatingRestTimerCard(
    timerState: com.bearbones.kumalift.core.timer.RestTimerState,
    onAdd30s: () -> Unit,
    onMinus15s: () -> Unit,
    onDismiss: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(KumaColors.SurfaceContainerHigh)
            .border(1.5.dp, KumaColors.ElectricLime, PillShape)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { timerState.progressFraction },
                modifier = Modifier.size(28.dp),
                color = KumaColors.ElectricLime,
                trackColor = KumaColors.SurfaceContainer
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = KumaColors.ElectricLime,
                modifier = Modifier.size(14.dp)
            )
        }

        Column {
            Text(
                text = "REST INTERVAL",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    color = KumaColors.TextMuted
                )
            )
            Text(
                text = timerState.formattedTime,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = KumaColors.ElectricLime
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onMinus15s)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "-15s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.OnSurface,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onAdd30s)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+30s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.ElectricLime,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = KumaColors.TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
