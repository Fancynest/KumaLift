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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.bearbones.kumalift.core.designsystem.KeepScreenOn
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.KumaSetCheckbox
import com.bearbones.kumalift.core.designsystem.PillShape
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
    val isEnglish = settings.appearance.language == AppLanguage.EN
    val isLb = settings.units.weightUnit == WeightUnit.LB
    val scope = rememberCoroutineScope()
    val restTimer = remember { RestTimer(scope) }
    val timerState by restTimer.state.collectAsState()

    var showRestTimerBar by remember { mutableStateOf(false) }
    var showConfirmFinishDialog by remember { mutableStateOf(false) }

    val workout = activeWorkout

    // T4: Wire keepScreenOn setting to real behavior
    KeepScreenOn(keepOn = workout != null && settings.logging.keepScreenOn)

    val wearableManager = com.bearbones.kumalift.core.wearable.rememberWearableManager()
    val wearableTelemetry by wearableManager.telemetry.collectAsState()

    val doFinishWorkout = {
        if (workout != null) {
            wearableManager.writeFinishedWorkout(
                workoutTitle = workout.title,
                startTimeEpochMs = workout.startTimeEpochMs,
                endTimeEpochMs = com.bearbones.kumalift.core.util.currentEpochMillis(),
                totalVolumeKg = workout.totalVolumeKg,
                activeCaloriesKcal = wearableTelemetry.activeCaloriesKcal
            )
            wearableManager.stopActiveWorkoutMonitoring()
            workoutRepository.finishWorkout(
                sessionRpe = 8.5,
                avgHeartRateBpm = wearableTelemetry.avgBpm,
                peakHeartRateBpm = wearableTelemetry.peakBpm,
                activeCaloriesKcal = wearableTelemetry.activeCaloriesKcal
            )
            onFinishWorkout()
        }
    }

    var elapsedSeconds by remember(workout?.id) {
        val initial = workout?.durationSeconds?.takeIf { it > 0 } ?: (34 * 60 + 18L)
        mutableStateOf(initial)
    }

    LaunchedEffect(workout?.id) {
        if (workout != null) {
            wearableManager.startActiveWorkoutMonitoring(workout.id)
        }
        while (true) {
            delay(1000)
            elapsedSeconds++
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
                    text = if (isEnglish) "No active session. Start a workout from Home or Routines!" else "Belum ada sesi aktif. Yuk mulai latihan dari Beranda atau Latihan!",
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
                                contentDescription = "Back",
                                tint = KumaColors.OnSurface
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = if (isEnglish) "Workout Log" else "Catatan Latihan",
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
                            text = if (isEnglish) "Finish" else "Selesai",
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
                                                text = if (isEnglish) "ACTIVE WORKOUT" else "SEDANG LATIHAN",
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
                                        // Live Heart Rate Pill if Wearable is connected
                                        if (wearableTelemetry.currentBpm != null) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(KumaColors.SurfaceContainerHigh)
                                                    .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(10.dp))
                                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Favorite,
                                                    contentDescription = null,
                                                    tint = KumaColors.ErrorRed,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text(
                                                    text = "${wearableTelemetry.currentBpm}",
                                                    style = MaterialTheme.typography.titleMedium.copy(
                                                        color = KumaColors.TextWhite,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    maxLines = 1,
                                                    softWrap = false
                                                )
                                                wearableTelemetry.currentZone?.let { zone ->
                                                    Text(
                                                        text = "Z${zone.zoneNumber}",
                                                        style = MaterialTheme.typography.labelSmall.copy(
                                                            color = LocalKumaColors.current.accentText,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    )
                                                }
                                            }
                                        }

                                        // Timer Box (Always single horizontal line with stopwatch icon)
                                        Row(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(KumaColors.SurfaceContainerHigh)
                                                .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(10.dp))
                                                .padding(horizontal = 12.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Timer,
                                                contentDescription = null,
                                                tint = LocalKumaColors.current.accentText,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = formattedTimer,
                                                style = MaterialTheme.typography.titleMedium.copy(
                                                    color = LocalKumaColors.current.textPrimary,
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
                                        text = if (isEnglish) "Completed Target" else "Target Selesai",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = KumaColors.TextSecondary
                                    )
                                    Text(
                                        text = if (isEnglish) "${workout.totalCompletedSets} of ${workout.totalSets} sets (${(workout.progressFraction * 100).toInt()}%)" else "${workout.totalCompletedSets} dari ${workout.totalSets} set (${(workout.progressFraction * 100).toInt()}%)",
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
                            isEnglish = isEnglish,
                            onToggleSet = { setId ->
                                workoutRepository.toggleSetCompletion(
                                    exerciseId = we.id,
                                    setId = setId,
                                    currentHeartRateBpm = wearableTelemetry.currentBpm
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
                                    text = if (isEnglish) "Add Exercise" else "Tambah Gerakan",
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
                                    text = if (isEnglish) "Finish Workout" else "Selesai Latihan",
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
                wearableTelemetry = wearableTelemetry,
                isEnglish = isEnglish,
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
                        text = if (isEnglish) "Finish Workout?" else "Selesaikan Latihan?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                },
                text = {
                    Text(
                        text = if (isEnglish)
                            "Are you sure you want to complete this workout session?"
                        else
                            "Apakah Anda yakin ingin menyelesaikan sesi latihan ini?",
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
                            text = if (isEnglish) "Finish" else "Selesaikan",
                            color = LocalKumaColors.current.accentText,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmFinishDialog = false }) {
                        Text(
                            text = if (isEnglish) "Cancel" else "Batal",
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
    isEnglish: Boolean = false,
    onToggleSet: (String) -> Unit,
    onUpdateSet: (String, Double, Int) -> Unit,
    onUpdateSetType: (String, SetType) -> Unit,
    onUpdateSetRpe: (String, Double?) -> Unit,
    onAddSet: () -> Unit
) {
    val exercise = workoutExercise.exercise

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
                        KumaBadge(text = "${exercise.equipment.code} · ${exercise.primaryMuscle.localizedName(isEnglish)}")
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Set Matrix Header with clean spacing
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SET",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.width(36.dp)
                )
                if (settings.logging.showPreviousColumn) {
                    Text(
                        text = if (isEnglish) "PREV" else "LALU",
                        style = MaterialTheme.typography.labelSmall,
                        color = KumaColors.TextMuted,
                        modifier = Modifier.weight(1.1f)
                    )
                }
                Text(
                    text = if (settings.units.weightUnit == WeightUnit.LB) "LB" else "KG",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.weight(1.2f)
                )
                Text(
                    text = "REPS",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
                    modifier = Modifier.weight(1.0f)
                )
                if (settings.logging.trackIntensity) {
                    Text(
                        text = if (settings.logging.intensityScale == IntensityScale.RIR) "RIR" else "RPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = KumaColors.TextMuted,
                        modifier = Modifier.weight(0.9f)
                    )
                }
                Text(
                    text = if (isEnglish) "CHECK" else "CEK",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted,
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

            // Add Set Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onAddSet),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEnglish) "Add Set" else "Tambah Set",
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
    exercise: Exercise,
    settings: UserSettings,
    onToggle: () -> Unit,
    onWeightChange: (Double) -> Unit,
    onRepsChange: (Int) -> Unit,
    onSetTypeChange: (SetType) -> Unit,
    onRpeChange: (Double?) -> Unit
) {
    val bg = if (set.isCompleted) KumaColors.SurfaceContainer.copy(alpha = 0.5f) else KumaColors.SurfaceContainer

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
            contentAlignment = Alignment.CenterStart
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
            Box(modifier = Modifier.weight(1.1f)) {
                val prevText = if (set.previousWeightKg != null && set.previousReps != null) {
                    val prevDisp = UnitConverters.toDisplayWeight(set.previousWeightKg, settings.units.weightUnit)
                    "$prevDisp × ${set.previousReps}"
                } else "-"
                Column {
                    Text(
                        text = prevText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 13.sp
                        )
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

        // Weight input box (respects active weight unit & equipment-based step)
        val unit = settings.units.weightUnit
        val displayWeight = UnitConverters.toDisplayWeight(set.weightKg, unit)
        val step = getWeightStep(exercise.equipment, unit, settings.logging)

        Box(
            modifier = Modifier
                .weight(1.2f)
                .padding(end = 6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable {
                    val newDisplay = (displayWeight + step)
                    val newStorageKg = UnitConverters.toStorageWeightKg(newDisplay, unit)
                    onWeightChange(newStorageKg)
                },
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

        // Reps input box (respects repStep setting)
        Box(
            modifier = Modifier
                .weight(1.0f)
                .padding(end = 6.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(KumaColors.SurfaceContainerHigh)
                .clickable {
                    val repStep = maxOf(1, settings.logging.repStep)
                    val nextReps = if (set.reps >= 30) 1 else (set.reps + repStep)
                    onRepsChange(nextReps)
                },
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
                    .padding(end = 6.dp)
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

        // Complete Checkbox (48x48 hit area)
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
    wearableTelemetry: com.bearbones.kumalift.core.wearable.LiveWearableTelemetry? = null,
    isEnglish: Boolean = false,
    addStepSec: Int = 30,
    subtractStepSec: Int = 15,
    onAdd: () -> Unit,
    onMinus: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
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
                text = if (isEnglish) "REST TIMER" else "WAKTU ISTIRAHAT",
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

        // Live HR recovery indicator
        if (wearableTelemetry?.currentBpm != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (wearableTelemetry.isHeartRateRestingReady) colors.accentText else KumaColors.ErrorRed,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${wearableTelemetry.currentBpm}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }
                Text(
                    text = if (wearableTelemetry.isHeartRateRestingReady) {
                        if (isEnglish) "READY" else "SIAP SET"
                    } else {
                        if (isEnglish) "PULIH" else "PULIH"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (wearableTelemetry.isHeartRateRestingReady) colors.accentText else colors.textSubtle
                    )
                )
            }
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
                    contentDescription = "Close",
                    tint = KumaColors.TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
