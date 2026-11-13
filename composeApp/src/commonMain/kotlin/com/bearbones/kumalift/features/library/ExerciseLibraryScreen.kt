package com.bearbones.kumalift.features.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup

@Composable
fun ExerciseLibraryScreen(
    exerciseRepository: ExerciseRepository,
    workoutRepository: WorkoutRepository? = null,
    onExerciseSelected: ((Exercise) -> Unit)? = null
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

    val allExercises by exerciseRepository.exercises.collectAsState()

    val filteredExercises = allExercises.filter { exercise ->
        val query = searchQuery.trim()
        val matchesQuery = query.isBlank() ||
            exercise.name.contains(query, ignoreCase = true) ||
            exercise.primaryMuscle.displayName.contains(query, ignoreCase = true) ||
            exercise.equipment.displayName.contains(query, ignoreCase = true) ||
            exercise.equipment.code.contains(query, ignoreCase = true) ||
            exercise.secondaryMuscles.any { it.displayName.contains(query, ignoreCase = true) }
        val matchesMuscle = selectedMuscle == null || exercise.primaryMuscle == selectedMuscle
        matchesQuery && matchesMuscle
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
                        color = KumaColors.ElectricLime
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(14.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Cari nama latihan, barbel, dumbbell...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = KumaColors.TextMuted
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = KumaColors.TextMuted
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(KumaColors.SurfaceContainer),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KumaColors.ElectricLime,
                        unfocusedBorderColor = KumaColors.BorderDefault,
                        focusedTextColor = KumaColors.TextWhite,
                        unfocusedTextColor = KumaColors.TextWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Muscle Group Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
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
                                .clickable { selectedMuscle = null }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Semua",
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
                                .clickable { selectedMuscle = muscle }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
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

                Spacer(modifier = Modifier.height(16.dp))

                // Exercise List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredExercises, key = { it.id }) { exercise ->
                        ExerciseListItem(
                            exercise = exercise,
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
fun ExerciseListItem(
    exercise: Exercise,
    onAdd: () -> Unit
) {
    KumaCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAdd),
        backgroundColor = KumaColors.SurfaceContainerLow,
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    KumaBadge(text = exercise.equipment.code)
                    Text(
                        text = "${exercise.equipment.displayName} · ${exercise.primaryMuscle.displayName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = KumaColors.TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextWhite
                    )
                )
                if (exercise.secondaryMuscles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Sekunder: ${exercise.secondaryMuscles.joinToString { it.displayName }}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 11.sp
                        )
                    )
                }
                if (exercise.currentPrWeightKg != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "PR: ${exercise.currentPrWeightKg} kg × ${exercise.currentPrReps ?: 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = KumaColors.ElectricLime,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(KumaColors.SurfaceContainer)
                    .clickable(onClick = onAdd)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        modifier = Modifier.size(16.dp)
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
    }
}
