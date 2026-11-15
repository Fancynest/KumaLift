package com.bearbones.kumalift.features.routines

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.localization.localizedMuscleSubtitle
import com.bearbones.kumalift.core.media.ExerciseThumbnailMedia
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineScreen(
    exerciseRepository: ExerciseRepository,
    onNavigateBack: () -> Unit,
    onSaveRoutine: (RoutineBlueprint) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    var routineTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("PPL Split") }
    var customCategory by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableStateOf(60) }
    var selectedRpeTier by remember { mutableStateOf(s.rpeTiers.getOrElse(2) { "RPE 8–9" }) }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    val presetCategories = s.presetRoutineCategories
    val durationOptions = listOf(30, 45, 60, 75, 90)
    val rpeOptions = s.rpeTiers

    val finalCategory = if (selectedCategory == s.presetRoutineCategories.last()) {
        customCategory.trim().ifBlank { s.custom }
    } else {
        selectedCategory
    }

    val autoMuscleFocus = remember(selectedExercises.toList(), s) {
        val muscles = selectedExercises.map { it.primaryMuscle.label(s) }.distinct()
        if (muscles.isEmpty()) s.muscleFullBody else muscles.joinToString(" · ")
    }

    val isTitleValid = routineTitle.trim().isNotEmpty()
    val isExercisesValid = selectedExercises.isNotEmpty()
    val isFormValid = isTitleValid && isExercisesValid

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Screen Top Header
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
                        text = s.createRoutineTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (isFormValid) {
                            val newRoutine = RoutineBlueprint(
                                id = com.bearbones.kumalift.core.util.IdGenerator.routineId(routineTitle.trim()),
                                title = routineTitle.trim(),
                                splitCategory = finalCategory,
                                muscleFocus = autoMuscleFocus,
                                estimatedDurationMinutes = selectedDurationMinutes,
                                targetRpeTier = selectedRpeTier,
                                exerciseTemplates = selectedExercises.toList(),
                                plannedSetsPerExercise = 3
                            )
                            onSaveRoutine(newRoutine)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    ),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = s.save,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Form Body (Vertical Scroll)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .kumaKeyboardAvoidance()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Routine Name
                Column {
                    Text(
                        text = s.routineNameLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KumaColors.SurfaceContainer)
                            .border(
                                width = 1.dp,
                                color = if (hasAttemptedSubmit && !isTitleValid) Color(0xFFFF453A) else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        BasicTextField(
                            value = routineTitle,
                            onValueChange = { routineTitle = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            ),
                            cursorBrush = SolidColor(colors.accentText),
                            modifier = Modifier.fillMaxWidth(),
                            decorationBox = { innerTextField ->
                                if (routineTitle.isEmpty()) {
                                    Text(
                                        text = s.routineNamePlaceholder,
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            color = colors.textSecondary.copy(alpha = 0.45f)
                                        )
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    if (hasAttemptedSubmit && !isTitleValid) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = s.routineNameRequiredError,
                            color = Color(0xFFFF453A),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Section 2: Split Category
                Column {
                    Text(
                        text = s.routineCategoryLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        presetCategories.forEach { category ->
                            val isSelected = selectedCategory == category
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onAccentFill else colors.textSecondary
                                    )
                                )
                            }
                        }
                    }

                    if (selectedCategory == presetCategories.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(KumaColors.SurfaceContainer)
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            BasicTextField(
                                value = customCategory,
                                onValueChange = { customCategory = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colors.textPrimary
                                ),
                                cursorBrush = SolidColor(colors.accentText),
                                modifier = Modifier.fillMaxWidth(),
                                decorationBox = { innerTextField ->
                                    if (customCategory.isEmpty()) {
                                        Text(
                                            text = s.customCategoryPlaceholder,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = colors.textSecondary.copy(alpha = 0.45f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                // Section 3: Target Duration
                Column {
                    Text(
                        text = s.estimatedDurationLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        durationOptions.forEach { mins ->
                            val isSelected = selectedDurationMinutes == mins
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable { selectedDurationMinutes = mins }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$mins ${s.minAbbrev}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onAccentFill else colors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: Target Intensity (RPE)
                Column {
                    Text(
                        text = s.targetIntensityLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        rpeOptions.forEach { rpe ->
                            val isSelected = selectedRpeTier == rpe
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable { selectedRpeTier = rpe }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = rpe,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onAccentFill else colors.textSecondary
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 5: Muscle Focus
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.muscleFocusLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = s.autoLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = autoMuscleFocus,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Section 6: Exercises List
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${s.exercisesInRoutineHeader} (${selectedExercises.size}) *",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )

                        Text(
                            text = s.add,
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold
                            ),
                            modifier = Modifier.clickable { showExercisePicker = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (selectedExercises.isEmpty()) {
                        KumaCard(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = KumaColors.SurfaceContainerLow,
                            cornerRadius = 14.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = s.noExercisesInRoutineYet,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.accentFill)
                                        .clickable { showExercisePicker = true }
                                        .padding(horizontal = 18.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = s.addExerciseToRoutineButton,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            color = colors.onAccentFill,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }

                        if (hasAttemptedSubmit && !isExercisesValid) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = s.exercisesInRoutineRequiredError,
                                color = Color(0xFFFF453A),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedExercises.forEachIndexed { index, exercise ->
                                SelectedExerciseRow(
                                    index = index + 1,
                                    exercise = exercise,
                                    onRemove = { selectedExercises.removeAt(index) }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KumaColors.SurfaceContainerLow)
                                    .border(1.dp, colors.accentText.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                    .clickable { showExercisePicker = true }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.addAnotherExerciseButton,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = colors.accentText,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // Exercise Picker Sheet
        if (showExercisePicker) {
            ExercisePickerModal(
                exerciseRepository = exerciseRepository,
                selectedIds = selectedExercises.map { it.id }.toSet(),
                onAddExercise = { ex ->
                    if (!selectedExercises.any { it.id == ex.id }) {
                        selectedExercises.add(ex)
                    }
                },
                onDismiss = { showExercisePicker = false }
            )
        }
    }
}

@Composable
private fun SelectedExerciseRow(
    index: Int,
    exercise: Exercise,
    onRemove: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    KumaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(colors.accentFill),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$index",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.onAccentFill,
                        fontWeight = FontWeight.ExtraBold
                    )
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
            ) {
                ExerciseThumbnailMedia(
                    assetPath = exercise.thumbnailUrl,
                    contentDescription = exercise.name,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title & Muscle
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    ),
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = exercise.localizedMuscleSubtitle(s),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    ),
                    maxLines = 1
                )
            }

            // Remove button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = s.remove,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerModal(
    exerciseRepository: ExerciseRepository,
    selectedIds: Set<String>,
    onAddExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allExercises by exerciseRepository.exercises.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

    val muscleOptions = listOf(
        null to s.all,
        MuscleGroup.CHEST to MuscleGroup.CHEST.label(s),
        MuscleGroup.BACK to MuscleGroup.BACK.label(s),
        MuscleGroup.LEGS to MuscleGroup.LEGS.label(s),
        MuscleGroup.SHOULDERS to MuscleGroup.SHOULDERS.label(s),
        MuscleGroup.ARMS to MuscleGroup.ARMS.label(s),
        MuscleGroup.CORE to MuscleGroup.CORE.label(s),
        MuscleGroup.CARDIO to MuscleGroup.CARDIO.label(s)
    )

    val filteredList = remember(searchQuery, selectedMuscle, allExercises, s) {
        if (searchQuery.isBlank() && selectedMuscle == null) {
            allExercises
        } else {
            allExercises.filter { ex ->
                val matchQuery = searchQuery.isBlank() ||
                        ex.name.contains(searchQuery, ignoreCase = true) ||
                        ex.equipment.label(s).contains(searchQuery, ignoreCase = true) ||
                        ex.primaryMuscle.label(s).contains(searchQuery, ignoreCase = true)
                val matchMuscle = selectedMuscle == null || ex.primaryMuscle == selectedMuscle
                matchQuery && matchMuscle
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = KumaColors.SurfaceDim
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .kumaKeyboardAvoidance()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header: Title & Done
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = s.exercisePickerTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(s.done, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = s.search,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.accentText),
                        modifier = Modifier.weight(1f),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = s.exercisePickerSearchPlaceholder,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Muscle filter pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(muscleOptions) { (muscle, label) ->
                    val isSelected = selectedMuscle == muscle
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                            .clickable { selectedMuscle = muscle }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) colors.onAccentFill else colors.textSecondary
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = s.exercisesFoundCount(filteredList.size),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Exercises list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filteredList,
                    key = { it.id },
                    contentType = { "exercise_picker_row" }
                ) { ex ->
                    val isAlreadyAdded = selectedIds.contains(ex.id)
                    KumaCard(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = KumaColors.SurfaceContainerLow,
                        cornerRadius = 10.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White)
                            ) {
                                ExerciseThumbnailMedia(
                                    assetPath = ex.thumbnailUrl,
                                    contentDescription = ex.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ex.name,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary
                                    ),
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = ex.localizedMuscleSubtitle(s),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = colors.textSecondary
                                    ),
                                    maxLines = 1
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            if (isAlreadyAdded) {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.accentFill.copy(alpha = 0.2f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = s.exercisePickerAddedBadge,
                                            tint = colors.accentText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = s.exercisePickerAddedBadge,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                color = colors.accentText,
                                                fontWeight = FontWeight.Bold
                                            )
                                        )
                                    }
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(colors.accentFill)
                                        .clickable { onAddExercise(ex) }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = s.add,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.onAccentFill,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
