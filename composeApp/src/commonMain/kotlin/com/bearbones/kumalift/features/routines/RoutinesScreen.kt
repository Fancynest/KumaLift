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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun RoutinesScreen(
    routineRepository: RoutineRepository,
    workoutRepository: WorkoutRepository,
    trainingProgramRepository: TrainingProgramRepository,
    exerciseRepository: ExerciseRepository = remember { ExerciseRepository() },
    settingsRepository: SettingsRepository? = null,
    listState: LazyListState = rememberLazyListState(),
    onStartRoutine: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val routines by routineRepository.routines.collectAsState()
    val completedWorkouts by workoutRepository.completedWorkouts.collectAsState()
    val programs by trainingProgramRepository.programs.collectAsState()
    val activeProgramId by trainingProgramRepository.activeProgramId.collectAsState()

    val activeProgram = remember(programs, activeProgramId) {
        programs.find { it.id == activeProgramId } ?: programs.firstOrNull() ?: TrainingProgram.DEFAULT_HYPERTROPHY
    }

    // Routines belonging strictly to the active program
    val programRoutines = remember(routines, activeProgram.id) {
        routines.filter { it.programId == activeProgram.id }
    }

    // Mapping routines count per program for selector modal
    val routinesCountByProgram = remember(routines, programs) {
        programs.associate { p ->
            p.id to routines.count { it.programId == p.id }
        }
    }

    // Weekly adherence computation
    val oneWeekAgoMs = remember { currentEpochMillis() - 7L * 24 * 3600_000L }
    val thisWeekWorkouts = remember(completedWorkouts, oneWeekAgoMs) {
        completedWorkouts.filter { it.startTimeEpochMs >= oneWeekAgoMs }
    }
    val completedDaysCount = remember(thisWeekWorkouts) {
        thisWeekWorkouts.map { w ->
            Instant.fromEpochMilliseconds(w.startTimeEpochMs)
                .toLocalDateTime(TimeZone.currentSystemDefault()).date
        }.distinct().size
    }
    val targetDays = activeProgram.targetDaysPerWeek.coerceAtLeast(1)
    val adherencePct = minOf(100, (completedDaysCount * 100) / targetDays)
    val progressFraction = (completedDaysCount.toFloat() / targetDays.toFloat()).coerceIn(0f, 1f)

    val allCategoryLabel = s.allProgramsCategory
    var selectedCategory by rememberSaveable(s.allProgramsCategory, activeProgram.id) { mutableStateOf(allCategoryLabel) }
    var showCreateRoutineScreen by rememberSaveable { mutableStateOf(false) }
    var selectedRoutineForDetail by remember { mutableStateOf<RoutineBlueprint?>(null) }
    var routineBeingEdited by remember { mutableStateOf<RoutineBlueprint?>(null) }

    // Training Programs Modal states
    var showProgramSelectorModal by remember { mutableStateOf(false) }
    var showCreateProgramModal by remember { mutableStateOf(false) }
    var programBeingEdited by remember { mutableStateOf<TrainingProgram?>(null) }
    var programToDelete by remember { mutableStateOf<TrainingProgram?>(null) }

    // Back handling
    KumaBackHandler(
        enabled = showCreateRoutineScreen ||
            selectedRoutineForDetail != null ||
            showProgramSelectorModal ||
            showCreateProgramModal ||
            programBeingEdited != null ||
            programToDelete != null
    ) {
        when {
            programToDelete != null -> programToDelete = null
            programBeingEdited != null -> programBeingEdited = null
            showCreateProgramModal -> showCreateProgramModal = false
            showProgramSelectorModal -> showProgramSelectorModal = false
            showCreateRoutineScreen -> {
                showCreateRoutineScreen = false
                routineBeingEdited = null
            }
            selectedRoutineForDetail != null -> selectedRoutineForDetail = null
        }
    }

    val categories = remember(programRoutines, allCategoryLabel) {
        listOf(allCategoryLabel) + programRoutines.map { it.splitCategory }.distinct()
    }

    val filteredRoutines = remember(selectedCategory, programRoutines, allCategoryLabel) {
        if (selectedCategory == allCategoryLabel) {
            programRoutines
        } else {
            programRoutines.filter { it.splitCategory == selectedCategory }
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

                // Button "Buat Routine"
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(colors.accentFill)
                        .clickable { showCreateRoutineScreen = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.createRoutineButton,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.onAccentFill
                        )
                    )
                }
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                }

                // 1. Dynamic Training Program Card
                item {
                    KumaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showProgramSelectorModal = true },
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 18.dp
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            // Subheader row: Label & target count
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.trainingProgramHeader,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.accentFill,
                                        letterSpacing = 1.sp
                                    )
                                )

                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainer)
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = s.adherenceProgress(completedDaysCount, targetDays),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = KumaColors.TextSecondary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Program Name & Switch pill
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = activeProgram.name,
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = KumaColors.TextWhite
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${formatGoalName(activeProgram.category)} · ${activeProgram.targetDaysPerWeek} ${s.daysPerWeek}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = KumaColors.TextSecondary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainer)
                                        .border(1.dp, KumaColors.BorderDefault, PillShape)
                                        .clickable { showProgramSelectorModal = true }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = s.switchProgramButton,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accentFill
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Weekly Adherence Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.weeklyAdherenceLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.TextSecondary
                                    )
                                )
                                Text(
                                    text = "$adherencePct%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (adherencePct > 0) colors.accentFill else KumaColors.TextMuted
                                    )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            LinearProgressIndicator(
                                progress = { progressFraction },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(PillShape),
                                color = colors.accentFill,
                                trackColor = KumaColors.SurfaceContainer
                            )
                        }
                    }
                }

                // 2. Category Filter Pills (only if program has routines)
                if (programRoutines.isNotEmpty()) {
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
                                        .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) (if (colors.isDark) Color.Transparent else colors.accentFillBorder) else KumaColors.BorderDefault,
                                            shape = PillShape
                                        )
                                        .clickable { selectedCategory = category }
                                        .padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Text(
                                        text = category,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. Routine Cards or Empty State
                if (programRoutines.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 36.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = KumaColors.TextMuted,
                                modifier = Modifier.size(52.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = s.noRoutinesInProgramYet,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = s.createRoutineForProgramHint(activeProgram.name),
                                style = MaterialTheme.typography.bodyMedium,
                                color = KumaColors.TextSecondary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            KumaPrimaryButton(
                                text = s.createRoutineButton,
                                onClick = { showCreateRoutineScreen = true },
                                modifier = Modifier.fillMaxWidth(0.7f)
                            )
                        }
                    }
                } else if (filteredRoutines.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp, horizontal = 20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = s.noRoutinesMessage,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    items(filteredRoutines, key = { it.id }) { routine ->
                        RoutineCardItem(
                            routine = routine,
                            onClick = { selectedRoutineForDetail = routine },
                            onStart = {
                                workoutRepository.startWorkoutFromRoutine(routine)
                                onStartRoutine()
                            },
                            onDelete = { routineRepository.deleteRoutine(routine.id) }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Routine Detail Screen Overlay
        if (selectedRoutineForDetail != null) {
            val routine = selectedRoutineForDetail!!
            RoutineDetailScreen(
                routine = routine,
                onNavigateBack = { selectedRoutineForDetail = null },
                onStartWorkout = { r ->
                    workoutRepository.startWorkoutFromRoutine(r)
                    selectedRoutineForDetail = null
                    onStartRoutine()
                },
                onEditRoutine = { r ->
                    routineBeingEdited = r
                    selectedRoutineForDetail = null
                    showCreateRoutineScreen = true
                },
                onDeleteRoutine = { r ->
                    routineRepository.deleteRoutine(r.id)
                    selectedRoutineForDetail = null
                }
            )
        }

        // Create / Edit Routine Screen Overlay
        if (showCreateRoutineScreen) {
            CreateRoutineScreen(
                exerciseRepository = exerciseRepository,
                routineToEdit = routineBeingEdited,
                currentProgramId = activeProgram.id,
                onNavigateBack = {
                    showCreateRoutineScreen = false
                    routineBeingEdited = null
                },
                onSaveRoutine = { updatedOrNewRoutine ->
                    if (routineBeingEdited != null) {
                        routineRepository.updateRoutine(updatedOrNewRoutine)
                    } else {
                        routineRepository.addRoutine(updatedOrNewRoutine)
                    }
                    showCreateRoutineScreen = false
                    routineBeingEdited = null
                }
            )
        }

        // Modal 1: Training Programs Selector
        if (showProgramSelectorModal) {
            TrainingProgramSelectorModal(
                programs = programs,
                activeProgramId = activeProgram.id,
                routinesCountByProgram = routinesCountByProgram,
                onSelectProgram = { prog ->
                    trainingProgramRepository.setActiveProgramId(prog.id)
                    settingsRepository?.updateProfile {
                        it.copy(
                            activeProgramId = prog.id,
                            trainingDaysPerWeek = prog.targetDaysPerWeek,
                            goal = prog.category
                        )
                    }
                    showProgramSelectorModal = false
                },
                onEditProgram = { prog ->
                    programBeingEdited = prog
                    showProgramSelectorModal = false
                },
                onDeleteProgram = { prog ->
                    programToDelete = prog
                },
                onCreateNewProgram = {
                    showCreateProgramModal = true
                    showProgramSelectorModal = false
                },
                onDismiss = { showProgramSelectorModal = false }
            )
        }

        // Modal 2: Create New Program
        if (showCreateProgramModal) {
            CreateEditProgramModal(
                programToEdit = null,
                onDismiss = { showCreateProgramModal = false },
                onSave = { name, description, category, targetDays ->
                    val created = trainingProgramRepository.createProgram(
                        name = name,
                        description = description,
                        category = category,
                        targetDaysPerWeek = targetDays
                    )
                    trainingProgramRepository.setActiveProgramId(created.id)
                    settingsRepository?.updateProfile {
                        it.copy(
                            activeProgramId = created.id,
                            trainingDaysPerWeek = created.targetDaysPerWeek,
                            goal = created.category
                        )
                    }
                    showCreateProgramModal = false
                }
            )
        }

        // Modal 3: Edit Existing Program
        if (programBeingEdited != null) {
            val prog = programBeingEdited!!
            CreateEditProgramModal(
                programToEdit = prog,
                onDismiss = { programBeingEdited = null },
                onSave = { name, description, category, targetDays ->
                    val updated = prog.copy(
                        name = name,
                        description = description,
                        category = category,
                        targetDaysPerWeek = targetDays
                    )
                    trainingProgramRepository.updateProgram(updated)
                    if (activeProgram.id == updated.id) {
                        settingsRepository?.updateProfile {
                            it.copy(
                                trainingDaysPerWeek = updated.targetDaysPerWeek,
                                goal = updated.category
                            )
                        }
                    }
                    programBeingEdited = null
                }
            )
        }

        // Modal 4: Delete Program Confirmation
        if (programToDelete != null) {
            val prog = programToDelete!!
            val count = routinesCountByProgram[prog.id] ?: 0
            DeleteProgramConfirmationDialog(
                program = prog,
                routineCount = count,
                onConfirm = {
                    trainingProgramRepository.deleteProgram(prog.id, routineRepository)
                    programToDelete = null
                },
                onDismiss = { programToDelete = null }
            )
        }
    }
}

@Composable
fun RoutineCardItem(
    routine: RoutineBlueprint,
    onClick: (() -> Unit)? = null,
    onStart: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val s = LocalStrings.current
    KumaCard(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
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

private fun formatGoalName(goal: TrainingGoal): String {
    return when (goal) {
        TrainingGoal.HYPERTROPHY -> "Hypertrophy"
        TrainingGoal.STRENGTH -> "Strength"
        TrainingGoal.FAT_LOSS -> "Fat Loss"
        TrainingGoal.ENDURANCE -> "Endurance"
        TrainingGoal.GENERAL -> "General Fitness"
    }
}
