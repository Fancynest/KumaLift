package com.bearbones.kumalift.features.logger

import kotlinx.datetime.Clock
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.KeepScreenOn
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaSetCheckbox
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.localization.localizedMuscleSubtitle
import com.bearbones.kumalift.core.timer.RestTimer
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.IntensityScale
import com.bearbones.kumalift.domain.model.LoggingSettings
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.SetLog
import com.bearbones.kumalift.domain.model.SetType
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.WorkoutExercise
import kotlin.math.roundToInt

@Composable
fun ActiveWorkoutLoggerScreen(
    workoutRepository: WorkoutRepository,
    settingsRepository: SettingsRepository? = null,
    onNavigateBack: () -> Unit,
    onFinishWorkout: () -> Unit,
    onAddExerciseClick: () -> Unit
) {
    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val settings = settingsRepository?.settings?.collectAsState()?.value ?: UserSettings()
    val s = LocalStrings.current
    val isLb = settings.units.weightUnit == WeightUnit.LB
    val scope = rememberCoroutineScope()
    val restTimer = remember { RestTimer(scope) }
    val timerState by restTimer.state.collectAsState()

    var showRestTimerBar by remember { mutableStateOf(false) }
    var showConfirmFinishDialog by remember { mutableStateOf(false) }
    var showLeaveLoggerDialog by remember { mutableStateOf(false) }

    // T10: Handle back navigation in active workout logger
    KumaBackHandler(enabled = true) {
        if (showConfirmFinishDialog) {
            showConfirmFinishDialog = false
        } else if (showLeaveLoggerDialog) {
            showLeaveLoggerDialog = false
        } else if (settings.logging.confirmFinish) {
            showLeaveLoggerDialog = true
        } else {
            onNavigateBack()
        }
    }

    val workout = activeWorkout

    // T4: Wire keepScreenOn setting to real behavior
    KeepScreenOn(keepOn = workout != null && settings.logging.keepScreenOn)

    val doFinishWorkout = {
        if (workout != null) {
            workoutRepository.finishWorkout(
                sessionRpe = 8.5
            )
            onFinishWorkout()
        }
    }

    val computeElapsed = {
        if (workout == null || workout.startTimeEpochMs <= 0L) 0L
        else {
            val now = Clock.System.now().toEpochMilliseconds()
            if (workout.isPaused && workout.pausedAtEpochMs != null) {
                maxOf(0L, (workout.pausedAtEpochMs - workout.startTimeEpochMs - workout.totalPausedDurationMs) / 1000L)
            } else {
                maxOf(0L, (now - workout.startTimeEpochMs - workout.totalPausedDurationMs) / 1000L)
            }
        }
    }

    var elapsedSeconds by remember(workout?.id, workout?.startTimeEpochMs) {
        mutableStateOf(computeElapsed())
    }

    LaunchedEffect(workout?.id, workout?.startTimeEpochMs, workout?.isPaused, workout?.pausedAtEpochMs) {
        while (workout != null && workout.startTimeEpochMs > 0L) {
            elapsedSeconds = computeElapsed()
            if (workout.isPaused) break
            delay(1000)
        }
    }

    val formattedTimer = remember(elapsedSeconds) {
        val hours = elapsedSeconds / 3600
        val minutes = (elapsedSeconds % 3600) / 60
        val seconds = elapsedSeconds % 60
        if (hours > 0) {
            "${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        } else {
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        if (workout == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = s.noActiveSessionMessage,
                    style = MaterialTheme.typography.bodyLarge,
                    color = KumaColors.TextMuted
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top App Bar with statusBarsPadding
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(KumaColors.SurfaceDim)
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(KumaColors.SurfaceContainer)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = s.back,
                                tint = KumaColors.OnSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = s.workoutLogTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = KumaColors.TextWhite
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Selesai Button in Top Bar
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.ElectricLime)
                            .clickable {
                                if (settings.logging.confirmFinish) {
                                    showConfirmFinishDialog = true
                                } else {
                                    doFinishWorkout()
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.finish,
                            style = MaterialTheme.typography.labelLarge.copy(
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
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(6.dp))
                        // Top Telemetry Header Card
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
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = s.activeWorkoutBadge,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    color = LocalKumaColors.current.accentText,
                                                    letterSpacing = 0.8.sp
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = workout.title,
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = LocalKumaColors.current.textPrimary
                                            ),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Timer Box (Interactive pause/resume with accurate wall-clock timer)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(if (workout.isPaused) KumaColors.SurfaceContainerHighest else KumaColors.SurfaceContainerHigh)
                                                .border(
                                                    1.dp,
                                                    if (workout.isPaused) Color(0xFFF59E0B) else KumaColors.BorderDefault,
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable {
                                                    if (workout.isPaused) {
                                                        workoutRepository.resumeWorkout()
                                                    } else {
                                                        workoutRepository.pauseWorkout()
                                                    }
                                                }
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = if (workout.isPaused) Icons.Default.PlayArrow else Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = if (workout.isPaused) Color(0xFFF59E0B) else LocalKumaColors.current.accentText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = formattedTimer,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = if (workout.isPaused) Color(0xFFF59E0B) else LocalKumaColors.current.textPrimary,
                                                    fontWeight = FontWeight.Bold
                                                ),
                                                maxLines = 1,
                                                softWrap = false
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(18.dp))

                                // Progress Bar
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = s.completedTargetLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KumaColors.TextSecondary
                                    )
                                    Text(
                                        text = s.completedSetsProgress(
                                            workout.totalCompletedSets,
                                            workout.totalSets,
                                            (workout.progressFraction * 100).toInt()
                                        ),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = LocalKumaColors.current.accentText
                                        )
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { workout.progressFraction },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(PillShape),
                                    color = LocalKumaColors.current.accentChart,
                                    trackColor = KumaColors.SurfaceContainerHighest
                                )
                            }
                        }
                    }

                    // Exercise Cards List
                    items(workout.exercises, key = { it.id }) { we ->
                        ExerciseLoggingCard(
                            workoutExercise = we,
                            settings = settings,
                            onToggleSet = { setId ->
                                workoutRepository.toggleSetCompletion(
                                    exerciseId = we.id,
                                    setId = setId
                                ) { completed ->
                                    if (completed && settings.logging.restTimerAutoStart) {
                                        showRestTimerBar = true
                                        val durationSec = if (settings.logging.restTimerSeparateByType) {
                                            if (we.exercise.movementType == MovementType.COMPOUND) {
                                                settings.logging.restTimerCompoundSec
                                            } else {
                                                settings.logging.restTimerIsolationSec
                                            }
                                        } else {
                                            settings.logging.restTimerDefaultSec
                                        }
                                        restTimer.start(durationSec)
                                    }
                                }
                            },
                            onUpdateSet = { setId, weight, reps ->
                                workoutRepository.updateSet(we.id, setId, weight, reps)
                            },
                            onUpdateSetType = { setId, setType ->
                                workoutRepository.updateSetType(we.id, setId, setType)
                            },
                            onUpdateSetRpe = { setId, rpe ->
                                workoutRepository.updateSetRpe(we.id, setId, rpe)
                            },
                            onAddSet = {
                                workoutRepository.addSetToExercise(we.id)
                            },
                            onRemoveLastSet = {
                                val lastSet = we.sets.lastOrNull()
                                if (lastSet != null) {
                                    workoutRepository.removeSetFromExercise(we.id, lastSet.id)
                                }
                            },
                            onRemoveExercise = {
                                workoutRepository.removeExerciseFromActiveWorkout(we.id)
                            }
                        )
                    }

                    // Bottom Action Buttons
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KumaColors.SurfaceContainer)
                                    .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(14.dp))
                                    .clickable(onClick = onAddExerciseClick),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.addExerciseButton,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = LocalKumaColors.current.textPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(KumaColors.ElectricLime)
                                    .clickable {
                                        if (settings.logging.confirmFinish) {
                                            showConfirmFinishDialog = true
                                        } else {
                                            doFinishWorkout()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.finishWorkoutButton,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = KumaColors.OnElectricLime,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(120.dp))
                    }
                }
            }
        }

        // Persistent Floating Rest Timer Pill
        AnimatedVisibility(
            visible = showRestTimerBar && timerState.remainingSeconds > 0,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 28.dp)
        ) {
            FloatingRestTimerCard(
                timerState = timerState,
                addStepSec = settings.logging.restTimerAddStepSec,
                subtractStepSec = settings.logging.restTimerSubtractStepSec,
                onAdd = { restTimer.addSeconds(settings.logging.restTimerAddStepSec) },
                onMinus = { restTimer.addSeconds(-settings.logging.restTimerSubtractStepSec) },
                onDismiss = {
                    showRestTimerBar = false
                    restTimer.reset()
                }
            )
        }

        // T4: Wire confirmFinish setting to confirmation dialog
        if (showConfirmFinishDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmFinishDialog = false },
                title = {
                    Text(
                        text = s.finishWorkoutModalTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = s.finishWorkoutModalMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showConfirmFinishDialog = false
                            doFinishWorkout()
                        }
                    ) {
                        Text(
                            text = s.finish,
                            color = LocalKumaColors.current.accentText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmFinishDialog = false }) {
                        Text(
                            text = s.cancel,
                            color = KumaColors.TextMuted
                        )
                    }
                },
                containerColor = KumaColors.SurfaceContainerHigh,
                titleContentColor = KumaColors.TextWhite,
                textContentColor = KumaColors.TextSecondary
            )
        }

        if (showLeaveLoggerDialog) {
            AlertDialog(
                onDismissRequest = { showLeaveLoggerDialog = false },
                title = {
                    Text(
                        text = s.minimizeWorkoutModalTitle,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = s.minimizeWorkoutModalMessage,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showLeaveLoggerDialog = false
                            onNavigateBack()
                        }
                    ) {
                        Text(
                            text = s.minimizeButton,
                            color = LocalKumaColors.current.accentText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLeaveLoggerDialog = false }) {
                        Text(
                            text = s.stayButton,
                            color = KumaColors.TextMuted
                        )
                    }
                },
                containerColor = KumaColors.SurfaceContainerHigh,
                titleContentColor = KumaColors.TextWhite,
                textContentColor = KumaColors.TextSecondary
            )
        }
    }
}

private fun getWeightStep(
    equipment: Equipment,
    unit: WeightUnit,
    loggingSettings: LoggingSettings
): Double {
    return if (unit == WeightUnit.LB) {
        when (equipment) {
            Equipment.BARBELL, Equipment.MACHINE -> 5.0
            Equipment.DUMBBELL, Equipment.CABLE -> 2.5
            Equipment.BODYWEIGHT -> 1.0
            else -> 5.0
        }
    } else {
        when (equipment) {
            Equipment.BARBELL -> loggingSettings.weightStepKgBarbell
            Equipment.DUMBBELL -> loggingSettings.weightStepKgDumbbell
            Equipment.CABLE -> loggingSettings.weightStepKgCable
            Equipment.MACHINE -> loggingSettings.weightStepKgMachine
            Equipment.BODYWEIGHT -> loggingSettings.weightStepKgBodyweight
            else -> 2.5
        }
    }
}

@Composable
fun ExerciseLoggingCard(
    workoutExercise: WorkoutExercise,
    settings: UserSettings,
    onToggleSet: (String) -> Unit,
    onUpdateSet: (String, Double, Int) -> Unit,
    onUpdateSetType: (String, SetType) -> Unit,
    onUpdateSetRpe: (String, Double?) -> Unit,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit = {},
    onRemoveExercise: () -> Unit = {}
) {
    val exercise = workoutExercise.exercise
    val s = LocalStrings.current
    var showMenu by remember { mutableStateOf(false) }

    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 18.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        KumaBadge(text = "${exercise.equipment.label(s)} · ${exercise.primaryMuscle.label(s)}")
                        if (exercise.currentPrWeightKg != null) {
                            KumaBadge(
                                text = "PR: ${UnitConverters.formatWeight(exercise.currentPrWeightKg, settings.units.weightUnit)}",
                                isPr = true
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = KumaColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(s.removeExerciseAction, color = KumaColors.ErrorRed) },
                            onClick = {
                                showMenu = false
                                onRemoveExercise()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = KumaColors.ErrorRed
                                )
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Set Matrix Header with clean spacing and precise alignment
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.setColumnHeader,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KumaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
                if (settings.logging.showPreviousColumn) {
                    Text(
                        text = s.prevColumnHeader,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f)
                    )
                }
                Text(
                    text = if (settings.units.weightUnit == WeightUnit.LB) s.unitLb.uppercase() else s.unitKg.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KumaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.2f).padding(horizontal = 4.dp)
                )
                Text(
                    text = s.repsColumnHeader,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KumaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1.0f).padding(horizontal = 4.dp)
                )
                if (settings.logging.trackIntensity) {
                    Text(
                        text = if (settings.logging.intensityScale == IntensityScale.RIR) "RIR" else "RPE",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.9f).padding(horizontal = 4.dp)
                    )
                }
                Text(
                    text = s.checkColumnHeader,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = KumaColors.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Set Rows with comfortable touch targets
            workoutExercise.sets.forEach { set ->
                SetRowItem(
                    set = set,
                    exercise = exercise,
                    settings = settings,
                    onToggle = { onToggleSet(set.id) },
                    onWeightChange = { newWeight -> onUpdateSet(set.id, newWeight, set.reps) },
                    onRepsChange = { newReps -> onUpdateSet(set.id, set.weightKg, newReps) },
                    onSetTypeChange = { newType -> onUpdateSetType(set.id, newType) },
                    onRpeChange = { newRpe -> onUpdateSetRpe(set.id, newRpe) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add / Remove Set Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .clickable(onClick = onAddSet),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.addSetButton,
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = KumaColors.OnSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                if (workoutExercise.sets.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .clickable(onClick = onRemoveLastSet),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = s.removeLastSetAction,
                            tint = KumaColors.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SetRowItem(
    set: SetLog,
    exercise: Exercise,
    settings: UserSettings,
    onToggle: () -> Unit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRpeChange: (Double?) -> Unit
) {
    val bg = if (set.isCompleted) KumaColors.SurfaceContainer.copy(alpha = 0.5f) else KumaColors.SurfaceContainer
    val unit = settings.units.weightUnit
    val displayWeight = UnitConverters.toDisplayWeight(set.weightKg, unit)

    var showWeightDialog by remember { mutableStateOf(false) }
    var showRepsDialog by remember { mutableStateOf(false) }

    if (showWeightDialog) {
        WeightInputDialog(
            initialWeight = displayWeight,
            unit = unit,
            onDismiss = { showWeightDialog = false },
            onConfirm = { newWeight ->
                val newStorageKg = UnitConverters.toStorageWeightKg(newWeight, unit)
                onWeightChange(newStorageKg)
                showWeightDialog = false
            }
        )
    }

    if (showRepsDialog) {
        RepsInputDialog(
            initialReps = set.reps,
            onDismiss = { showRepsDialog = false },
            onConfirm = { newReps ->
                onRepsChange(newReps)
                showRepsDialog = false
            }
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set number / type badge (clicking cycles enabled set types)
        val enabledTypes = settings.logging.enabledSetTypes.toList().ifEmpty { listOf(SetType.NORMAL) }
        val label = when (set.setType) {
            SetType.WARMUP -> "W"
            SetType.DROP -> "D"
            SetType.FAILURE -> "F"
            SetType.NORMAL -> "${set.setNumber}"
        }
        val typeColor = when (set.setType) {
            SetType.WARMUP -> Color(0xFFF59E0B)
            SetType.DROP -> Color(0xFFEC4899)
            SetType.FAILURE -> Color(0xFFEF4444)
            SetType.NORMAL -> if (set.isCompleted) {
                if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentText else LocalKumaColors.current.accentTextOnTint
            } else LocalKumaColors.current.textPrimary
        }

        Box(
            modifier = Modifier
                .width(36.dp)
                .clickable {
                    val currentIndex = enabledTypes.indexOf(set.setType)
                    val nextType = enabledTypes[(if (currentIndex >= 0) currentIndex + 1 else 0) % enabledTypes.size]
                    onSetTypeChange(nextType)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = typeColor
                )
            )
        }

        // Previous & Set Heart Rate (respects showPreviousColumn)
        if (settings.logging.showPreviousColumn) {
            Box(
                modifier = Modifier.weight(1.1f),
                contentAlignment = Alignment.Center
            ) {
                val prevText = if (set.previousWeightKg != null && set.previousReps != null) {
                    val prevDisp = UnitConverters.toDisplayWeight(set.previousWeightKg, settings.units.weightUnit)
                    "$prevDisp × ${set.previousReps}"
                } else "-"
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 13.sp
                        ),
                        textAlign = TextAlign.Center
                    )
                    if (set.heartRateBpm != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = KumaColors.ErrorRed,
                                modifier = Modifier.size(10.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "${set.heartRateBpm}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = LocalKumaColors.current.accentText
                                )
                            )
                        }
                    }
                }
            }
        }

        // Weight input box (opens custom decimal weight dialog)
        Box(
            modifier = Modifier
                .weight(1.2f)
                .padding(horizontal = 4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable { showWeightDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$displayWeight",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = KumaColors.TextWhite
                )
            )
        }

        // Reps input box (opens custom reps dialog)
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(horizontal = 4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable { showRepsDialog = true },
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

        // Intensity column (RPE or RIR if trackIntensity is enabled)
        if (settings.logging.trackIntensity) {
            val isRir = settings.logging.intensityScale == IntensityScale.RIR
            val intensityText = if (isRir) {
                if (set.rpe == null) "—" else "${maxOf(0, (10 - set.rpe.roundToInt()))}"
            } else {
                if (set.rpe == null) "—" else "${set.rpe}"
            }

            Box(
                modifier = Modifier
                    .weight(0.9f)
                    .padding(horizontal = 4.dp)
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KumaColors.SurfaceContainerHigh)
                    .clickable {
                        val newRpe = if (isRir) {
                            when (set.rpe) {
                                null -> 10.0 // 0 RIR
                                10.0 -> 9.0  // 1 RIR
                                9.0 -> 8.0   // 2 RIR
                                8.0 -> 7.0   // 3 RIR
                                else -> null
                            }
                        } else {
                            when (set.rpe) {
                                null -> 7.0
                                7.0 -> 7.5
                                7.5 -> 8.0
                                8.0 -> 8.5
                                8.5 -> 9.0
                                9.0 -> 9.5
                                9.5 -> 10.0
                                else -> null
                            }
                        }
                        onRpeChange(newRpe)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = intensityText,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (intensityText == "—") KumaColors.TextMuted else LocalKumaColors.current.accentText
                    )
                )
            }
        }

        // Complete Checkbox (48x48 hit area, centered with check column)
        Box(
            modifier = Modifier.width(48.dp),
            contentAlignment = Alignment.Center
        ) {
            KumaSetCheckbox(
                isCompleted = set.isCompleted,
                onToggle = onToggle
            )
        }
    }
}

@Composable
fun WeightInputDialog(
    initialWeight: Double,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val initialStr = if (initialWeight > 0.0) {
        if (initialWeight == initialWeight.toLong().toDouble()) initialWeight.toLong().toString() else initialWeight.toString()
    } else ""
    var textValue by remember { mutableStateOf(initialStr) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = "${s.editWeightTitle} (${if (unit == WeightUnit.LB) s.unitLb else s.unitKg})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d*$"""))) {
                            textValue = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0.0", color = KumaColors.TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = KumaColors.ErrorRed)
                        } else {
                            Text(s.enterValidWeightHint, color = KumaColors.TextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5.0, -1.0, 1.0, 5.0).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val curr = textValue.toDoubleOrNull() ?: 0.0
                                    val next = maxOf(0.0, curr + delta)
                                    textValue = if (next == next.toLong().toDouble()) next.toLong().toString() else next.toString()
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (delta > 0) "+$delta" else "$delta").removeSuffix(".0"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toDoubleOrNull()
                    if (parsed == null || parsed < 0.0 || parsed > 999.9) {
                        errorMessage = s.enterValidWeightHint
                    } else {
                        onConfirm(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(s.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun RepsInputDialog(
    initialReps: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    var textValue by remember { mutableStateOf(if (initialReps > 0) initialReps.toString() else "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = s.editRepsTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.all { it.isDigit() }) {
                            textValue = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("10", color = KumaColors.TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, color = KumaColors.ErrorRed)
                        } else {
                            Text(s.enterValidRepsHint, color = KumaColors.TextMuted)
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5, -1, 1, 5).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val curr = textValue.toIntOrNull() ?: 0
                                    val next = maxOf(1, curr + delta)
                                    textValue = next.toString()
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (delta > 0) "+$delta" else "$delta"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textSecondary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsed = textValue.toIntOrNull()
                    if (parsed == null || parsed < 1 || parsed > 999) {
                        errorMessage = s.enterValidRepsHint
                    } else {
                        onConfirm(parsed)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(s.save, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun FloatingRestTimerCard(
    timerState: com.bearbones.kumalift.core.timer.RestTimerState,
    addStepSec: Int = 30,
    subtractStepSec: Int = 15,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    Row(
        modifier = Modifier
            .clip(PillShape)
            .background(colors.surfaceBright)
            .border(
                1.5.dp,
                if (colors.isDark) colors.accentFill else colors.accentChart,
                PillShape
            )
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { timerState.progressFraction },
                modifier = Modifier.size(32.dp),
                color = colors.accentChart,
                trackColor = colors.surfaceVariant
            )
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = colors.accentText,
                modifier = Modifier.size(16.dp)
            )
        }

        Column {
            Text(
                text = s.restTimerHeader,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    color = colors.textSubtle,
                    letterSpacing = 0.5.sp
                )
            )
            Text(
                text = timerState.formattedTime,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.accentText
                )
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onMinus)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "-${subtractStepSec}s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.surfaceVariant)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "+${addStepSec}s",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.accentText,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = s.close,
                    tint = KumaColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
