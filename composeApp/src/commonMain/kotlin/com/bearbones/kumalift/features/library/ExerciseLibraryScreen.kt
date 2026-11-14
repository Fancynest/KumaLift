package com.bearbones.kumalift.features.library

import androidx.compose.animation.Crossfade
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.Equipment
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup

private enum class LibraryViewState {
    CATALOG,
    DETAIL,
    CREATE
}

@Composable
fun ExerciseLibraryScreen(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository? = null,
    settingsRepository: SettingsRepository? = null,
    filterState: LibraryFilterState = rememberLibraryFilterState(),
    listState: LazyListState = rememberLazyListState(),
    onExerciseSelected: ((Exercise) -> Unit)? = null
) {
    var selectedExerciseForDetail by remember { mutableStateOf<Exercise?>(null) }
    var isCreatingExercise by remember { mutableStateOf(false) }
    val allExercises by exerciseRepository.exercises.collectAsState()
    val isEnglish = resolveIsEnglish(settingsRepository?.settings?.collectAsState()?.value?.appearance?.language ?: AppLanguage.SYSTEM)

    // T10: Sub-navigation back handlers
    KumaBackHandler(enabled = selectedExerciseForDetail != null) {
        selectedExerciseForDetail = null
    }
    KumaBackHandler(enabled = isCreatingExercise && selectedExerciseForDetail == null) {
        isCreatingExercise = false
    }

    Crossfade(
        targetState = when {
            selectedExerciseForDetail != null -> LibraryViewState.DETAIL
            isCreatingExercise -> LibraryViewState.CREATE
            else -> LibraryViewState.CATALOG
        }
    ) { state ->
        when (state) {
            LibraryViewState.DETAIL -> {
                val detailedExercise = selectedExerciseForDetail
                if (detailedExercise != null) {
                    // Keep detailed exercise updated with latest repository state (e.g. pinned notes)
                    val liveExercise = allExercises.find { it.id == detailedExercise.id } ?: detailedExercise
                    ExerciseDetailScreen(
                        exercise = liveExercise,
                        isEnglish = isEnglish,
                        onNavigateBack = { selectedExerciseForDetail = null },
                        onAddExercise = if (workoutRepository?.activeWorkout?.collectAsState()?.value != null) {
                            {
                                workoutRepository.addExerciseToActiveWorkout(liveExercise)
                                onExerciseSelected?.invoke(liveExercise)
                            }
                        } else null,
                        onSaveNote = { note ->
                            exerciseRepository.updatePinnedNote(liveExercise.id, note)
                        }
                    )
                }
            }
            LibraryViewState.CREATE -> {
                CreateExerciseScreen(
                    onNavigateBack = { isCreatingExercise = false },
                    onSaveExercise = { name, primaryMuscle, equipment, secondaryMuscles, movementType ->
                        val created = exerciseRepository.addCustomExercise(
                            name = name,
                            primaryMuscle = primaryMuscle,
                            equipment = equipment,
                            secondaryMuscles = secondaryMuscles,
                            movementType = movementType
                        )
                        isCreatingExercise = false
                        selectedExerciseForDetail = created
                    }
                )
            }
            LibraryViewState.CATALOG -> {
                ExerciseCatalogList(
                    exerciseRepository = exerciseRepository,
                    workoutRepository = workoutRepository,
                    filterState = filterState,
                    listState = listState,
                    isEnglish = isEnglish,
                    onOpenDetail = { selectedExerciseForDetail = it },
                    onOpenCreate = { isCreatingExercise = true },
                    onAddExercise = { exercise ->
                        workoutRepository?.addExerciseToActiveWorkout(exercise)
                        onExerciseSelected?.invoke(exercise)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExerciseCatalogList(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository?,
    filterState: LibraryFilterState,
    listState: LazyListState,
    isEnglish: Boolean = false,
    onOpenDetail: (Exercise) -> Unit,
    onOpenCreate: () -> Unit,
    onAddExercise: (Exercise) -> Unit
) {
    val allExercises by exerciseRepository.exercises.collectAsState()
    val activeWorkout = workoutRepository?.activeWorkout?.collectAsState()?.value
    val colors = LocalKumaColors.current

    val filteredExercises = remember(
        allExercises,
        filterState.searchQuery,
        filterState.selectedMuscle,
        filterState.selectedEquipment,
        filterState.selectedMovementType
    ) {
        if (!filterState.isActive) {
            allExercises
        } else {
            allExercises.filter { filterState.matches(it) }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar: "Exercises", "Create"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isEnglish) "Exercises" else "Katalog Gerakan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = colors.textPrimary,
                        fontSize = 22.sp
                    )
                )

                Text(
                    text = if (isEnglish) "Create" else "Buat Gerakan",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.accentText
                    ),
                    modifier = Modifier.clickable { onOpenCreate() }
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar matching screenshot
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KumaColors.SurfaceContainer)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (filterState.searchQuery.isEmpty()) {
                            Text(
                                text = if (isEnglish) "Search exercise..." else "Cari nama gerakan...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                                color = colors.textSecondary.copy(alpha = 0.6f)
                            )
                        }
                        BasicTextField(
                            value = filterState.searchQuery,
                            onValueChange = { filterState.searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            ),
                            cursorBrush = SolidColor(colors.accentText),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (filterState.searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { filterState.searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Row 1: Target Otot
                LazyRow(
                    modifier = Modifier.isolateHorizontalScroll(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterPill(
                            label = if (isEnglish) "All Muscles" else "Semua Otot",
                            isSelected = filterState.selectedMuscle == null,
                            onClick = { filterState.selectedMuscle = null }
                        )
                    }

                    items(MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }) { muscle ->
                        FilterPill(
                            label = muscle.localizedName(isEnglish),
                            isSelected = filterState.selectedMuscle == muscle,
                            onClick = {
                                filterState.selectedMuscle = if (filterState.selectedMuscle == muscle) null else muscle
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Row 2: Alat / Equipment
                LazyRow(
                    modifier = Modifier.isolateHorizontalScroll(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterPill(
                            label = if (isEnglish) "All Equipment" else "Semua Alat",
                            isSelected = filterState.selectedEquipment == null,
                            onClick = { filterState.selectedEquipment = null }
                        )
                    }

                    items(Equipment.entries) { eq ->
                        FilterPill(
                            label = eq.localizedName(isEnglish),
                            isSelected = filterState.selectedEquipment == eq,
                            onClick = {
                                filterState.selectedEquipment = if (filterState.selectedEquipment == eq) null else eq
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Filter Row 3: Tipe Gerakan
                LazyRow(
                    modifier = Modifier.isolateHorizontalScroll(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        FilterPill(
                            label = if (isEnglish) "All Types" else "Semua Tipe",
                            isSelected = filterState.selectedMovementType == null,
                            onClick = { filterState.selectedMovementType = null }
                        )
                    }

                    items(MovementType.entries) { mt ->
                        FilterPill(
                            label = mt.localizedName(isEnglish),
                            isSelected = filterState.selectedMovementType == mt,
                            onClick = {
                                filterState.selectedMovementType = if (filterState.selectedMovementType == mt) null else mt
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section Header: "Exercises (count)" + Reset Filter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Exercises (${filteredExercises.size})" else "Daftar Gerakan (${filteredExercises.size})",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            fontSize = 16.sp
                        )
                    )

                    if (filterState.isActive) {
                        Text(
                            text = if (isEnglish) "Reset Filters" else "Atur Ulang",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentText,
                                fontSize = 12.sp
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { filterState.reset() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Exercise Rows List
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 90.dp)
                ) {
                    items(
                        items = filteredExercises,
                        key = { it.id },
                        contentType = { "exercise_row" }
                    ) { exercise ->
                        ExerciseRowItem(
                            exercise = exercise,
                            hasActiveWorkout = activeWorkout != null,
                            isEnglish = isEnglish,
                            onItemClick = { onOpenDetail(exercise) },
                            onAddClick = { onAddExercise(exercise) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExerciseRowItem(
    exercise: Exercise,
    hasActiveWorkout: Boolean,
    isEnglish: Boolean = false,
    onItemClick: () -> Unit,
    onAddClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(KumaColors.SurfaceDim)
            .clickable(onClick = onItemClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular thumbnail with anatomical illustration (180x180 dataset style)
        ExerciseThumbnail(
            exercise = exercise,
            size = 48.dp
        )

        Spacer(modifier = Modifier.width(14.dp))

        // Title & Muscle Subtitle with end padding to prevent overlap with buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        ) {
            Text(
                text = exercise.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    fontSize = 15.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = exercise.localizedMuscleSubtitle(isEnglish),
                style = MaterialTheme.typography.bodySmall.copy(
                    color = colors.textSecondary,
                    fontSize = 13.sp
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Action: "Tambah" / "Add" if active workout is running, else chevron right
        if (hasActiveWorkout) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.accentFill)
                    .clickable(onClick = onAddClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isEnglish) "Add" else "Tambah",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.onAccentFill
                    )
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.textSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colors.onAccentFill else colors.textPrimary
            )
        )
    }
}
