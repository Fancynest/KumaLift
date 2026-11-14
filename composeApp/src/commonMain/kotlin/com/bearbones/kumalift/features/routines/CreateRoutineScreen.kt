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
import com.bearbones.kumalift.core.media.ExerciseThumbnailMedia
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineScreen(
    exerciseRepository: ExerciseRepository,
    isEnglish: Boolean = false,
    onNavigateBack: () -> Unit,
    onSaveRoutine: (RoutineBlueprint) -> Unit
) {
    val colors = LocalKumaColors.current

    var routineTitle by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("PPL Split") }
    var customCategory by remember { mutableStateOf("") }
    var selectedDurationMinutes by remember { mutableStateOf(60) }
    var selectedRpeTier by remember { mutableStateOf("Berat, RPE 8–9") }
    val selectedExercises = remember { mutableStateListOf<Exercise>() }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }

    val presetCategories = listOf("PPL Split", "Upper / Lower", "Full Body", "Custom")
    val durationOptions = listOf(30, 45, 60, 75, 90)
    val rpeOptions = listOf(
        "Ringan, RPE 6–7",
        "Sedang, RPE 7–8",
        "Berat, RPE 8–9",
        "Maksimal, RPE 9–10"
    )

    val finalCategory = if (selectedCategory == "Custom") {
        customCategory.trim().ifBlank { "Custom" }
    } else {
        selectedCategory
    }

    val autoMuscleFocus = remember(selectedExercises.toList(), isEnglish) {
        val muscles = selectedExercises.map { it.primaryMuscle.localizedName(isEnglish) }.distinct()
        if (muscles.isEmpty()) (if (isEnglish) "Full Body" else "Seluruh Tubuh") else muscles.joinToString(" · ")
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
                            contentDescription = "Kembali",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Text(
                        text = "Buat Pola Latihan",
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
                        text = "Simpan",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // Form Body (Vertical Scroll)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section 1: Routine Name
                Column {
                    Text(
                        text = "NAMA POLA LATIHAN *",
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
                                        text = "misal: Push Day A, Kaki Fokus Quad",
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
                            text = "Nama pola latihan wajib diisi",
                            color = Color(0xFFFF453A),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                // Section 2: Split Category
                Column {
                    Text(
                        text = "KATEGORI PROGRAM / SPLIT",
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

                    if (selectedCategory == "Custom") {
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
                                            text = "Nama kategori custom (misal: Bro Split, Calisthenics)",
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
                        text = "ESTIMASI DURASI",
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
                                    text = "$mins mnt",
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
                        text = "TARGET INTENSITAS (RPE)",
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
                            text = "FOKUS OTOT",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        Text(
                            text = "Otomatis",
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
                            text = "GERAKAN LATIHAN (${selectedExercises.size}) *",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )

                        Text(
                            text = "Tambah",
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
                                    text = "Belum ada gerakan latihan",
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
                                        text = "Tambah Gerakan",
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
                                text = "Minimal tambahkan 1 gerakan latihan ke pola ini",
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
                                    isEnglish = isEnglish,
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
                                    text = if (isEnglish) "Add Another Exercise" else "Tambah Gerakan Lainnya",
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
            }
        }

        // Exercise Picker Sheet
        if (showExercisePicker) {
            ExercisePickerModal(
                exerciseRepository = exerciseRepository,
                selectedIds = selectedExercises.map { it.id }.toSet(),
                isEnglish = isEnglish,
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
    isEnglish: Boolean = false,
    onRemove: () -> Unit
) {
    val colors = LocalKumaColors.current
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
                    text = "${exercise.primaryMuscle.localizedName(isEnglish)} · ${exercise.equipment.localizedName(isEnglish)}",
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
                    contentDescription = if (isEnglish) "Remove" else "Hapus",
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
    isEnglish: Boolean = false,
    onAddExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val allExercises by exerciseRepository.exercises.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }

    val muscleOptions = listOf(
        null to (if (isEnglish) "All" else "Semua"),
        MuscleGroup.CHEST to MuscleGroup.CHEST.localizedName(isEnglish),
        MuscleGroup.BACK to MuscleGroup.BACK.localizedName(isEnglish),
        MuscleGroup.LEGS to MuscleGroup.LEGS.localizedName(isEnglish),
        MuscleGroup.SHOULDERS to MuscleGroup.SHOULDERS.localizedName(isEnglish),
        MuscleGroup.ARMS to MuscleGroup.ARMS.localizedName(isEnglish),
        MuscleGroup.CORE to MuscleGroup.CORE.localizedName(isEnglish),
        MuscleGroup.CARDIO to MuscleGroup.CARDIO.localizedName(isEnglish)
    )

    val filteredList = remember(searchQuery, selectedMuscle, allExercises) {
        if (searchQuery.isBlank() && selectedMuscle == null) {
            allExercises
        } else {
            allExercises.filter { ex ->
                val matchQuery = searchQuery.isBlank() ||
                        ex.name.contains(searchQuery, ignoreCase = true) ||
                        ex.equipment.displayName.contains(searchQuery, ignoreCase = true)
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
                    text = "Pilih Gerakan Latihan",
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
                    Text("Selesai", fontWeight = FontWeight.Bold)
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
                        contentDescription = "Search",
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
                                    text = "Cari nama gerakan, alat, atau otot...",
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
                            contentDescription = "Clear",
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
                text = "${filteredList.size} Gerakan ditemukan",
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
                                    text = "${ex.primaryMuscle.displayName} · ${ex.equipment.displayName}",
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
                                            contentDescription = "Added",
                                            tint = colors.accentText,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Ada",
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
                                        text = "Tambah",
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
