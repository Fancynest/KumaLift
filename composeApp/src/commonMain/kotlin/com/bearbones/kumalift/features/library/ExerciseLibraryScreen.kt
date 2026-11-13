package com.bearbones.kumalift.features.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.isolateHorizontalScroll
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MovementType
import com.bearbones.kumalift.domain.model.MuscleGroup

@Composable
fun ExerciseLibraryScreen(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository? = null,
    onExerciseSelected: ((Exercise) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var selectedSubMuscle by remember { mutableStateOf<String?>(null) }
    var selectedType by remember { mutableStateOf<MovementType?>(null) }

    // Single-card accordion state: only one exercise expanded at a time
    var expandedExerciseId by remember { mutableStateOf<String?>(null) }

    val allExercises by exerciseRepository.exercises.collectAsState()

    // Available sub-muscles for the currently selected muscle group
    val availableSubMuscles = remember(selectedMuscle, allExercises) {
        if (selectedMuscle == null) emptyList()
        else allExercises
            .filter { it.primaryMuscle == selectedMuscle && it.subMuscle.isNotBlank() }
            .map { it.subMuscle }
            .distinct()
    }

    val filteredExercises = allExercises.filter { exercise ->
        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() ||
            exercise.name.contains(query, ignoreCase = true) ||
            exercise.primaryMuscle.displayName.contains(query, ignoreCase = true) ||
            exercise.subMuscle.contains(query, ignoreCase = true) ||
            exercise.equipment.displayName.contains(query, ignoreCase = true) ||
            exercise.equipment.code.contains(query, ignoreCase = true) ||
            exercise.equipmentDetail.contains(query, ignoreCase = true) ||
            exercise.secondaryMuscleDetails.contains(query, ignoreCase = true) ||
            exercise.secondaryMuscles.any { it.displayName.contains(query, ignoreCase = true) }

        val matchesMuscle = selectedMuscle == null || exercise.primaryMuscle == selectedMuscle
        val matchesSubMuscle = selectedSubMuscle == null || exercise.subMuscle == selectedSubMuscle
        val matchesType = selectedType == null || exercise.movementType == selectedType

        matchesQuery && matchesMuscle && matchesSubMuscle && matchesType
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Katalog Gerakan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )

                Text(
                    text = "${filteredExercises.size} Gerakan",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.ElectricLime,
                        fontWeight = FontWeight.Bold
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Compact Search Bar (Sleek 44dp height)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(KumaColors.SurfaceContainer)
                        .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = KumaColors.TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Cari nama gerakan, alat, atau otot...",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.5.sp),
                                color = KumaColors.TextMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.5.sp,
                                color = KumaColors.TextWhite
                            ),
                            cursorBrush = SolidColor(KumaColors.ElectricLime),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (searchQuery.isNotEmpty()) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = KumaColors.TextMuted,
                            modifier = Modifier
                                .size(18.dp)
                                .clickable { searchQuery = "" }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary Muscle Group Filter Chips
                LazyRow(
                    modifier = Modifier.isolateHorizontalScroll(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 2.dp)
                ) {
                    item {
                        val isAllSelected = selectedMuscle == null
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isAllSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isAllSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                    PillShape
                                )
                                .clickable {
                                    selectedMuscle = null
                                    selectedSubMuscle = null
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Semua Otot",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isAllSelected) KumaColors.OnElectricLime else KumaColors.OnSurface
                                )
                            )
                        }
                    }

                    items(MuscleGroup.entries.filter { it != MuscleGroup.FULL_BODY }) { muscle ->
                        val isSelected = selectedMuscle == muscle
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                .border(
                                    1.dp,
                                    if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                    PillShape
                                )
                                .clickable {
                                    selectedMuscle = muscle
                                    selectedSubMuscle = null
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = muscle.displayName,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) KumaColors.OnElectricLime else KumaColors.OnSurface
                                )
                            )
                        }
                    }
                }

                // Sub-Muscle Filter Chips (Appears when specific muscle is selected)
                AnimatedVisibility(visible = availableSubMuscles.isNotEmpty()) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            modifier = Modifier.isolateHorizontalScroll(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                val isAllSub = selectedSubMuscle == null
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (isAllSub) KumaColors.SurfaceContainerHighest else KumaColors.SurfaceContainerLow)
                                        .border(
                                            1.dp,
                                            if (isAllSub) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                            PillShape
                                        )
                                        .clickable { selectedSubMuscle = null }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = "Semua Bagian",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isAllSub) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isAllSub) KumaColors.ElectricLime else KumaColors.TextMuted
                                        )
                                    )
                                }
                            }

                            items(availableSubMuscles) { sub ->
                                val isSelected = selectedSubMuscle == sub
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(if (isSelected) KumaColors.SurfaceContainerHighest else KumaColors.SurfaceContainerLow)
                                        .border(
                                            1.dp,
                                            if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                            PillShape
                                        )
                                        .clickable { selectedSubMuscle = sub }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text(
                                        text = sub,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextMuted
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Movement Type Filter (Semua, Compound, Isolasi)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TypeFilterChip(
                        label = "Semua Tipe",
                        isSelected = selectedType == null,
                        onClick = { selectedType = null }
                    )
                    TypeFilterChip(
                        label = "Compound",
                        isSelected = selectedType == MovementType.COMPOUND,
                        onClick = { selectedType = MovementType.COMPOUND }
                    )
                    TypeFilterChip(
                        label = "Isolasi",
                        isSelected = selectedType == MovementType.ISOLATION,
                        onClick = { selectedType = MovementType.ISOLATION }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Exercise List with single-card accordion
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredExercises, key = { it.id }) { exercise ->
                        val isExpanded = expandedExerciseId == exercise.id
                        ExerciseListItem(
                            exercise = exercise,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedExerciseId = if (isExpanded) null else exercise.id
                            },
                            onAdd = {
                                if (workoutRepository != null) {
                                    workoutRepository.addExerciseToActiveWorkout(exercise)
                                }
                                onExerciseSelected?.invoke(exercise)
                            }
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TypeFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer)
            .border(
                1.dp,
                if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) KumaColors.ElectricLime else KumaColors.TextMuted
            )
        )
    }
}

@Composable
fun ExerciseListItem(
    exercise: Exercise,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onAdd: () -> Unit
) {
    KumaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .animateContentSize(),
        backgroundColor = if (isExpanded) KumaColors.SurfaceContainer else KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Header: Badges & Add Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Badges: Alat · Tipe · Level
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KumaBadge(text = exercise.equipment.code)
                    KumaBadge(text = exercise.movementType.displayName)
                    if (isExpanded) {
                        KumaBadge(text = exercise.level.displayName)
                    }
                }

                // Add Button (+ Tambah)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainerHigh)
                        .clickable(onClick = onAdd)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Tambah",
                            tint = KumaColors.ElectricLime,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Tambah",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = KumaColors.ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle: Otot Utama & Sub-Otot
            val subOtotInfo = if (exercise.subMuscle.isNotBlank()) " · ${exercise.subMuscle}" else ""
            Text(
                text = "${exercise.primaryMuscle.displayName}$subOtotInfo",
                style = MaterialTheme.typography.labelSmall,
                color = KumaColors.TextMuted
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Nama Latihan & Expand Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextWhite,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Tutup" else "Detail",
                    tint = if (isExpanded) KumaColors.ElectricLime else KumaColors.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Expanded Details Section (Only visible for the clicked item)
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(KumaColors.BorderDefault.copy(alpha = 0.5f))
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    // Detail Alat & Level
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Alat: ${exercise.equipmentDetail.ifBlank { exercise.equipment.displayName }}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                        Text(
                            text = "Level: ${exercise.level.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextMuted,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Otot Sekunder (Bantu)
                    if (exercise.secondaryMuscleDetails.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Otot bantu: ${exercise.secondaryMuscleDetails}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextSecondary,
                                fontSize = 12.sp
                            )
                        )
                    }

                    // Panduan Teknik Gerakan
                    if (exercise.instructions.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Panduan Teknik:",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = KumaColors.ElectricLime
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = exercise.instructions,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = KumaColors.TextWhite.copy(alpha = 0.88f),
                                fontSize = 12.sp,
                                lineHeight = 17.sp
                            )
                        )
                    }

                    // Rekor Angkatan (PR) jika ada
                    if (exercise.currentPrWeightKg != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Rekor Pribadi (PR): ${exercise.currentPrWeightKg} kg × ${exercise.currentPrReps ?: 1}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = KumaColors.ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}
