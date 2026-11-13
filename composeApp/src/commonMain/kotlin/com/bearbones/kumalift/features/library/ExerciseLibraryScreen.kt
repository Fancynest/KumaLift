package com.bearbones.kumalift.features.library

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
        val matchesQuery = searchQuery.isBlank() || exercise.name.contains(searchQuery, ignoreCase = true)
        val matchesMuscle = selectedMuscle == null || exercise.primaryMuscle == selectedMuscle
        matchesQuery && matchesMuscle
    }

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
                    .height(60.dp)
                    .background(KumaColors.SurfaceDim)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Exercise Library",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )

                Text(
                    text = "${filteredExercises.size} Indexed",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.ElectricLime
                    )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = "Search movement or equipment...",
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
                        .clip(RoundedCornerShape(12.dp))
                        .background(KumaColors.SurfaceContainer),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = KumaColors.ElectricLime,
                        unfocusedBorderColor = KumaColors.BorderDefault,
                        focusedTextColor = KumaColors.TextWhite,
                        unfocusedTextColor = KumaColors.TextWhite
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Muscle Group Filter Chips
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
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
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "All",
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
                                .padding(horizontal = 14.dp, vertical = 6.dp)
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

                Spacer(modifier = Modifier.height(14.dp))

                // Exercise List
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
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
                        Spacer(modifier = Modifier.height(80.dp))
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
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = KumaColors.SurfaceContainerLow
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KumaBadge(text = exercise.equipment.code)
                    Text(
                        text = exercise.primaryMuscle.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = KumaColors.TextMuted
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                if (exercise.currentPrWeightKg != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "PR: ${exercise.currentPrWeightKg} kg × ${exercise.currentPrReps ?: 1}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = KumaColors.ElectricLime
                        )
                    )
                }
            }

            IconButton(
                onClick = onAdd,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(KumaColors.SurfaceContainer)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add",
                    tint = KumaColors.ElectricLime
                )
            }
        }
    }
}
