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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.RoutineBlueprint

@Composable
fun RoutinesScreen(
    routineRepository: RoutineRepository,
    workoutRepository: WorkoutRepository,
    onStartRoutine: () -> Unit
) {
    val routines by routineRepository.routines.collectAsState()
    var selectedCategory by remember { mutableStateOf("Semua Program") }

    val categories = listOf("Semua Program", "PPL Split", "Upper / Lower", "Full Body")

    val filteredRoutines = if (selectedCategory == "Semua Program") {
        routines
    } else {
        routines.filter { it.splitCategory == selectedCategory }
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
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Program & Pola Latihan",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )
            }

            LazyColumn(
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
                                Column {
                                    Text(
                                        text = "PROGRAM LATIHAN",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.ElectricLime,
                                            letterSpacing = 0.8.sp
                                        )
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "PPL Hypertrophy Phase 2",
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(PillShape)
                                        .background(KumaColors.SurfaceContainerHigh)
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "Minggu 3 dari 6",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = KumaColors.TextWhite,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Disiplin Program",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = KumaColors.TextMuted
                                )
                                Text(
                                    text = "75%",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = KumaColors.ElectricLime,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            LinearProgressIndicator(
                                progress = { 0.75f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(PillShape),
                                color = KumaColors.ElectricLime,
                                trackColor = KumaColors.SurfaceContainerHighest
                            )
                        }
                    }
                }

                // Category Filter Pills
                item {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 2.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = category == selectedCategory
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) KumaColors.ElectricLime else KumaColors.SurfaceContainer)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) KumaColors.ElectricLime else KumaColors.BorderDefault,
                                        shape = PillShape
                                    )
                                    .clickable { selectedCategory = category }
                                    .padding(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) KumaColors.OnElectricLime else KumaColors.OnSurface
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

@Composable
fun RoutineCardItem(
    routine: RoutineBlueprint,
    onStart: () -> Unit
) {
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
                Text(
                    text = routine.lastPerformedDaysAgo?.let { "Last: $it" } ?: "New",
                    style = MaterialTheme.typography.labelSmall,
                    color = KumaColors.TextMuted
                )
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
                text = "${routine.muscleFocus} · ${routine.estimatedDurationMinutes} min · ${routine.targetRpeTier}",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = KumaColors.TextSecondary,
                    fontSize = 13.sp
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Exercise Preview Tags
            LazyRow(
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
                    text = "Mulai Latihan Ini",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.OnElectricLime
                    )
                )
            }
        }
    }
}
