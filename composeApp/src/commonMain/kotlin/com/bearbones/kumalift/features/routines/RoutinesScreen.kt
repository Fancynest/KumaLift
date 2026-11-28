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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaDeleteConfirmationDialog
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.media.QrPlanScannerDialog
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.SharedWorkoutPlanPackage
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram
import com.bearbones.kumalift.domain.model.WorkoutPlanShareCodec
import com.bearbones.kumalift.domain.model.localizedTitle
import com.bearbones.kumalift.domain.model.localizedMuscleFocus
import com.bearbones.kumalift.domain.model.localizedRpeTier
import com.bearbones.kumalift.domain.share.ProgramShareScope
import com.bearbones.kumalift.features.share.ProgramShareEditorDialog
import com.bearbones.kumalift.domain.model.localizedSplitCategory
import kotlinx.coroutines.delay
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private data class ProgramImageShareRequest(
    val programTitle: String,
    val routines: List<RoutineBlueprint>,
    val initialScope: ProgramShareScope,
    val initialRoutineId: String? = null
)

@Composable
fun RoutinesScreen(
    routineRepository: RoutineRepository,
    workoutRepository: WorkoutRepository,
    trainingProgramRepository: TrainingProgramRepository,
    exerciseRepository: ExerciseRepository = remember { ExerciseRepository() },
    settingsRepository: SettingsRepository? = null,
    listState: LazyListState = rememberLazyListState(),
    initialSharedPlanInput: String? = null,
    onConsumeSharedPlanInput: () -> Unit = {},
    onStartRoutine: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val routines by routineRepository.routines.collectAsState()
    val activeWorkout by workoutRepository.activeWorkout.collectAsState()
    val completedWorkouts by workoutRepository.completedWorkouts.collectAsState()
    val programs by trainingProgramRepository.programs.collectAsState()
    val activeProgramId by trainingProgramRepository.activeProgramId.collectAsState()
    val currentSettings = settingsRepository?.settings?.collectAsState()?.value
    val currentProfile = currentSettings?.profile ?: ProfileSettings()

    var planToShare by remember { mutableStateOf<SharedWorkoutPlanPackage?>(null) }
    var programImageToShare by remember { mutableStateOf<ProgramImageShareRequest?>(null) }
    var showQrScannerModal by remember { mutableStateOf(false) }
    var showImportPlanModal by remember { mutableStateOf(false) }
    var importPlanInitialText by remember { mutableStateOf<String?>(null) }
    var shareFeedbackBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialSharedPlanInput) {
        if (!initialSharedPlanInput.isNullOrBlank()) {
            importPlanInitialText = initialSharedPlanInput
            showImportPlanModal = true
            onConsumeSharedPlanInput()
        }
    }

    LaunchedEffect(shareFeedbackBanner) {
        if (shareFeedbackBanner != null) {
            delay(2800)
            shareFeedbackBanner = null
        }
    }

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
    var routineBeingEditedId by rememberSaveable { mutableStateOf<String?>(null) }
    val routineBeingEdited = remember(routineBeingEditedId, routines) {
        routineBeingEditedId?.let { editId -> routines.firstOrNull { it.id == editId } }
    }
    var pendingStartRoutine by remember { mutableStateOf<RoutineBlueprint?>(null) }

    // Training Programs Modal states
    var showProgramSelectorModal by remember { mutableStateOf(false) }
    var showCreateProgramModal by remember { mutableStateOf(false) }
    var programBeingEdited by remember { mutableStateOf<TrainingProgram?>(null) }
    var programToDelete by remember { mutableStateOf<TrainingProgram?>(null) }
    var routineToDelete by remember { mutableStateOf<RoutineBlueprint?>(null) }

    // Back handling
    KumaBackHandler(
        enabled = showCreateRoutineScreen ||
            selectedRoutineForDetail != null ||
            showProgramSelectorModal ||
            showCreateProgramModal ||
            programBeingEdited != null ||
            programToDelete != null ||
            routineToDelete != null ||
            planToShare != null ||
            programImageToShare != null ||
            showQrScannerModal ||
            showImportPlanModal
    ) {
        when {
            programImageToShare != null -> programImageToShare = null
            planToShare != null -> planToShare = null
            showQrScannerModal -> showQrScannerModal = false
            showImportPlanModal -> showImportPlanModal = false
            routineToDelete != null -> routineToDelete = null
            programToDelete != null -> programToDelete = null
            programBeingEdited != null -> programBeingEdited = null
            showCreateProgramModal -> showCreateProgramModal = false
            showProgramSelectorModal -> showProgramSelectorModal = false
            showCreateRoutineScreen -> {
                showCreateRoutineScreen = false
                routineBeingEditedId = null
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.workoutRoutinesTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )

                // Scan Barcode pill (replaces Import Plan button in top bar)
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(KumaColors.SurfaceContainer)
                        .border(1.dp, KumaColors.BorderDefault, PillShape)
                        .clickable { showQrScannerModal = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = s.scanBarcodeAction,
                            tint = colors.accentText,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = s.scanBarcodeAction,
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText
                            )
                        )
                    }
                }

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
                        maxLines = 1,
                        softWrap = false,
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = s.trainingProgramHeader,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
                                        maxLines = 1,
                                        softWrap = false,
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
                                        text = activeProgram.localizedTitle(s),
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
                                        text = "${activeProgram.category.label(s)} · ${activeProgram.targetDaysPerWeek} ${s.daysPerWeek}",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            color = KumaColors.TextSecondary
                                        )
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainer)
                                        .border(1.dp, KumaColors.BorderDefault, PillShape)
                                        .clickable {
                                            programImageToShare = ProgramImageShareRequest(
                                                programTitle = activeProgram.localizedTitle(s),
                                                routines = programRoutines,
                                                initialScope = ProgramShareScope.WHOLE_PROGRAM
                                            )
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = s.shareProgramImageAction,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(6.dp))

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

                            val animatedProgress = androidx.compose.animation.core.animateFloatAsState(
                                targetValue = progressFraction,
                                animationSpec = androidx.compose.animation.core.spring(
                                    stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow
                                )
                            ).value

                            LinearProgressIndicator(
                                progress = { animatedProgress },
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
                                text = s.createRoutineForProgramHint(activeProgram.localizedTitle(s)),
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
                                if (activeWorkout != null) {
                                    pendingStartRoutine = routine
                                } else {
                                    workoutRepository.startWorkoutFromRoutine(routine)
                                    onStartRoutine()
                                }
                            },
                            onShare = {
                                planToShare = WorkoutPlanShareCodec.fromRoutine(routine, s, currentProfile)
                            },
                            onDelete = { routineToDelete = routine }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
        }

        // Confirm replacing active workout
        pendingStartRoutine?.let { targetRoutine ->
            KumaDeleteConfirmationDialog(
                title = s.cancelWorkoutDialogTitle,
                message = s.cancelWorkoutDialogMessage,
                confirmText = s.startWorkoutButton,
                cancelText = s.cancel,
                onConfirm = {
                    pendingStartRoutine = null
                    workoutRepository.startWorkoutFromRoutine(targetRoutine)
                    onStartRoutine()
                },
                onDismiss = { pendingStartRoutine = null }
            )
        }

        // Routine Detail Screen Overlay
        if (selectedRoutineForDetail != null) {
            val routine = selectedRoutineForDetail!!
            RoutineDetailScreen(
                routine = routine,
                onNavigateBack = { selectedRoutineForDetail = null },
                onStartWorkout = { r ->
                    selectedRoutineForDetail = null
                    if (activeWorkout != null) {
                        pendingStartRoutine = r
                    } else {
                        workoutRepository.startWorkoutFromRoutine(r)
                        onStartRoutine()
                    }
                },
                onEditRoutine = { r ->
                    routineBeingEditedId = r.id
                    selectedRoutineForDetail = null
                    showCreateRoutineScreen = true
                },
                onDeleteRoutine = { r ->
                    routineToDelete = r
                    selectedRoutineForDetail = null
                },
                onShareRoutine = { r ->
                    planToShare = WorkoutPlanShareCodec.fromRoutine(r, s, currentProfile)
                }
            )
        }

        // Create / Edit Routine Screen Overlay
        if (showCreateRoutineScreen) {
            val userSettingsState = settingsRepository?.settings?.collectAsState()
            val currentWeightUnit = userSettingsState?.value?.units?.weightUnit ?: com.bearbones.kumalift.domain.model.WeightUnit.KG
            CreateRoutineScreen(
                exerciseRepository = exerciseRepository,
                routineToEdit = routineBeingEdited,
                currentProgramId = activeProgram.id,
                weightUnit = currentWeightUnit,
                onNavigateBack = {
                    showCreateRoutineScreen = false
                    routineBeingEditedId = null
                },
                onSaveRoutine = { updatedOrNewRoutine ->
                    if (routineBeingEdited != null) {
                        routineRepository.updateRoutine(updatedOrNewRoutine)
                    } else {
                        routineRepository.addRoutine(updatedOrNewRoutine)
                    }
                    showCreateRoutineScreen = false
                    routineBeingEditedId = null
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
                            selectedTrainingDays = prog.activeDays,
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
                onShareProgram = { prog ->
                    val progRoutines = routines.filter { it.programId == prog.id }
                    planToShare = WorkoutPlanShareCodec.fromProgram(prog, progRoutines, s, currentProfile)
                    showProgramSelectorModal = false
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
                onSave = { name, description, category ->
                    val created = trainingProgramRepository.createProgram(
                        name = name,
                        description = description,
                        category = category
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
                onSave = { name, description, category ->
                    val updated = prog.copy(
                        name = name,
                        description = description,
                        category = category
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

        // Modal 5: Delete Routine Confirmation
        routineToDelete?.let { targetRoutine ->
            KumaDeleteConfirmationDialog(
                title = s.deleteRoutineDialogTitle,
                message = s.deleteRoutineDialogMessage,
                confirmText = s.delete,
                cancelText = s.cancel,
                onConfirm = {
                    routineRepository.deleteRoutine(targetRoutine.id)
                    routineToDelete = null
                },
                onDismiss = { routineToDelete = null }
            )
        }

        // Modal 6: Share Plan Card & Actions
        planToShare?.let { pkg ->
            val currentUnit = currentSettings?.units?.weightUnit ?: com.bearbones.kumalift.domain.model.WeightUnit.KG
            val currentLang = currentSettings?.appearance?.language ?: com.bearbones.kumalift.domain.model.AppLanguage.SYSTEM
            ShareWorkoutPlanModal(
                pkg = pkg,
                weightUnit = currentUnit,
                language = currentLang,
                onShareAsImage = {
                    val isSingle = pkg.planType == com.bearbones.kumalift.domain.model.SharedPlanType.ROUTINE && pkg.routines.size == 1
                    val targetRoutineId = pkg.routines.firstOrNull()?.sourceRoutineId?.takeIf { it.isNotBlank() }
                    val sourceProgram = programs.firstOrNull { it.localizedTitle(s) == pkg.title || it.name == pkg.title }
                    val scopedRoutines = if (sourceProgram != null) {
                        routines.filter { it.programId == sourceProgram.id }.ifEmpty { programRoutines }
                    } else if (isSingle && targetRoutineId != null) {
                        routines.filter { it.id == targetRoutineId }.ifEmpty { programRoutines }
                    } else {
                        programRoutines
                    }
                    programImageToShare = ProgramImageShareRequest(
                        programTitle = pkg.title,
                        routines = scopedRoutines,
                        initialScope = if (isSingle) ProgramShareScope.SINGLE_ROUTINE else ProgramShareScope.WHOLE_PROGRAM,
                        initialRoutineId = targetRoutineId
                    )
                    planToShare = null
                },
                onDismiss = { planToShare = null },
                onStatusMessage = { msg -> shareFeedbackBanner = msg }
            )
        }

        programImageToShare?.let { req ->
            val currentUnit = currentSettings?.units?.weightUnit ?: com.bearbones.kumalift.domain.model.WeightUnit.KG
            val currentLang = currentSettings?.appearance?.language ?: com.bearbones.kumalift.domain.model.AppLanguage.SYSTEM
            val rawHandle = currentProfile.username.trim().trimStart('@')
            val athleteTag = when {
                rawHandle.isNotBlank() -> "@$rawHandle"
                currentProfile.name.isNotBlank() -> currentProfile.name.trim()
                else -> ""
            }
            ProgramShareEditorDialog(
                programTitle = req.programTitle,
                routines = req.routines,
                initialScope = req.initialScope,
                initialRoutineId = req.initialRoutineId,
                weightUnit = currentUnit,
                language = currentLang,
                athleteTag = athleteTag,
                onDismiss = { programImageToShare = null }
            )
        }

        // Modal 7: QR Barcode Scanner
        if (showQrScannerModal) {
            QrPlanScannerDialog(
                onCodeScanned = { scanned ->
                    showQrScannerModal = false
                    importPlanInitialText = scanned
                    showImportPlanModal = true
                },
                onDismiss = { showQrScannerModal = false }
            )
        }

        // Modal 8: Import Plan with Mandatory Pre-Import Preview
        if (showImportPlanModal) {
            ImportWorkoutPlanModal(
                initialRawInput = importPlanInitialText,
                exerciseRepository = exerciseRepository,
                routineRepository = routineRepository,
                trainingProgramRepository = trainingProgramRepository,
                settingsRepository = settingsRepository,
                onImported = {
                    showImportPlanModal = false
                    importPlanInitialText = null
                    shareFeedbackBanner = s.importPlanSuccessToast
                },
                onDismiss = {
                    showImportPlanModal = false
                    importPlanInitialText = null
                }
            )
        }

        shareFeedbackBanner?.let { bannerMsg ->
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = 100.dp)
                    .clip(PillShape)
                    .background(KumaColors.SurfaceContainerHigh)
                    .border(1.dp, colors.accentFill.copy(alpha = 0.5f), PillShape)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = bannerMsg,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.accentText,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
fun RoutineCardItem(
    routine: RoutineBlueprint,
    onClick: (() -> Unit)? = null,
    onStart: () -> Unit,
    onShare: (() -> Unit)? = null,
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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KumaBadge(text = routine.localizedSplitCategory(s))
                    routine.sharedByCreator?.takeIf { it.isNotBlank() }?.let { creator ->
                        KumaBadge(text = s.sharedByCreatorBadge(creator), isPr = false)
                    }
                    if (routine.scheduledDays.isNotEmpty()) {
                        val dayLabels = routine.scheduledDays.mapNotNull { s.dayNamesShort.getOrNull(it - 1) }.joinToString(", ")
                        Box(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .clip(PillShape)
                                .background(LocalKumaColors.current.accentTint)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = dayLabels,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = LocalKumaColors.current.accentText
                                )
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    routine.lastPerformedDaysAgo?.let { days ->
                        Text(
                            text = s.lastPerformedDaysAgo(days),
                            maxLines = 1,
                            softWrap = false,
                            style = MaterialTheme.typography.labelSmall,
                            color = KumaColors.TextMuted
                        )
                    }
                    if (onShare != null) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = s.sharePlanDesc,
                            tint = LocalKumaColors.current.accentText,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable(onClick = onShare)
                        )
                    }
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
                text = routine.localizedTitle(s),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${routine.localizedMuscleFocus(s)} · ${routine.estimatedDurationMinutes} ${s.minAbbrev} · ${routine.localizedRpeTier(s)}",
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


