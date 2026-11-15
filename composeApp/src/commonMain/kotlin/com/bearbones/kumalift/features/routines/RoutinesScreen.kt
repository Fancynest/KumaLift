package com.bearbones.kumalift.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.RoutineBlueprint

@Composable
fun RoutinesScreen(
    routineRepository: RoutineRepository,
    workoutRepository: WorkoutRepository,
    exerciseRepository: ExerciseRepository = remember { ExerciseRepository() },
    settingsRepository: SettingsRepository? = null,
    listState: LazyListState = rememberLazyListState(),
    onStartRoutine: () -> Unit
) {
    val s = LocalStrings.current
    val settings = settingsRepository?.settings?.collectAsState()?.value
    val routines by routineRepository.routines.collectAsState()
    val completedWorkouts by workoutRepository.completedWorkouts.collectAsState()
    val allCategoryLabel = s.allProgramsCategory
    var selectedCategory by rememberSaveable(s.allProgramsCategory) { mutableStateOf(allCategoryLabel) }
    var showCreateRoutineScreen by remember { mutableStateOf(false) }

    // T10: Handle back when create routine modal is open
    KumaBackHandler(enabled = showCreateRoutineScreen) {
        showCreateRoutineScreen = false
    }

    val targetDays = settings?.profile?.trainingDaysPerWeek ?: 4
    val nowMs = remember { kotlinx.datetime.Clock.System.now().toEpochMilliseconds() }
    val oneWeekAgoMs = nowMs - 7L * 24 * 3600_000L
    val workoutsThisWeek = remember(completedWorkouts) {
        completedWorkouts.count { it.startTimeEpochMs >= oneWeekAgoMs }
    }
    val adherenceFrac = if (targetDays > 0) (workoutsThisWeek.toFloat() / targetDays).coerceIn(0f, 1f) else 0f
    val adherencePct = (adherenceFrac * 100).toInt()
    val goalName = when (settings?.profile?.goal) {
        com.bearbones.kumalift.domain.model.TrainingGoal.HYPERTROPHY -> s.programHypertrophy
        com.bearbones.kumalift.domain.model.TrainingGoal.STRENGTH -> s.programStrength
        com.bearbones.kumalift.domain.model.TrainingGoal.GENERAL, null -> s.programGeneral
    }

    val categories = remember(routines, allCategoryLabel) {
        listOf(allCategoryLabel) + routines.map { it.splitCategory }.distinct()
    }

    val filteredRoutines = remember(selectedCategory, routines, allCategoryLabel) {
        if (selectedCategory == allCategoryLabel) {
            routines
        } else {
            routines.filter { it.splitCategory == selectedCategory }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Top Header with statusBarsPadding
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.workoutRoutinesTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )

                // Button "Buat Pola"
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(LocalKumaColors.current.accentFill)
                        .clickable { showCreateRoutineScreen = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.createRoutineButton,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = LocalKumaColors.current.onAccentFill
                        )
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    // Mesocycle Progress Banner
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
                                Text(
                                    text = s.trainingProgramHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        letterSpacing = 0.8.sp
                                    )
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainerHigh)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = s.adherenceProgress(workoutsThisWeek, targetDays),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.TextWhite,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = goalName,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = s.weeklyAdherenceLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                                Text(
                                    text = "$adherencePct%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = LocalKumaColors.current.accentText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { adherenceFrac },
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

                // Category Filter Pills
                item {
                    LazyRow(
                        modifier = Modifier.isolateHorizontalScroll(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) LocalKumaColors.current.accentFill else KumaColors.SurfaceContainer)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) (if (LocalKumaColors.current.isDark) Color.Transparent else LocalKumaColors.current.accentFillBorder) else KumaColors.BorderDefault,
                                        shape = PillShape
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) LocalKumaColors.current.onAccentFill else LocalKumaColors.current.textPrimary
                                    )
                                )
                            }
                        }
                    }
                }

                // Routine Cards
                items(filteredRoutines, key = { it.id }) { routine ->
                    RoutineCardItem(
                        routine = routine,
                        onStart = {
                            workoutRepository.startWorkoutFromRoutine(routine)
                            onStartRoutine()
                        },
                        onDelete = if (routine.id.startsWith("custom_")) {
                            { routineRepository.deleteRoutine(routine.id) }
                        } else null
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        if (showCreateRoutineScreen) {
            CreateRoutineScreen(
                exerciseRepository = exerciseRepository,
                onNavigateBack = { showCreateRoutineScreen = false },
                onSaveRoutine = { newRoutine ->
                    routineRepository.addRoutine(newRoutine)
                    showCreateRoutineScreen = false
                }
            )
        }
    }
}

@Composable
fun RoutineCardItem(
    routine: RoutineBlueprint,
    onStart: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val s = LocalStrings.current
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
                KumaBadge(text = routine.splitCategory)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = routine.lastPerformedDaysAgo?.let { s.lastPerformedDaysAgo(it) } ?: s.newBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = KumaColors.TextMuted
                    )
                    if (onDelete != null) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = s.deleteRoutineContentDesc,
                            tint = KumaColors.TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(onClick = onDelete)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = routine.title,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${routine.muscleFocus} · ${routine.estimatedDurationMinutes} ${s.minAbbrev} · ${routine.targetRpeTier}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = KumaColors.TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise Preview Tags
            LazyRow(
                modifier = Modifier.isolateHorizontalScroll(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(routine.exerciseTemplates) { ex ->
                    KumaBadge(text = ex.name)
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Start Routine Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(PillShape)
                    .background(KumaColors.ElectricLime)
                    .clickable(onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = s.startRoutineButton,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.OnElectricLime
                    )
                )
            }
        }
    }
}
