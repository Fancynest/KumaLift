package com.bearbones.kumalift.features.routines

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.bearbones.kumalift.domain.model.RoutineExerciseConfig
import androidx.compose.ui.text.style.TextOverflow
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
import com.bearbones.kumalift.core.designsystem.KumaDeleteConfirmationDialog
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.localization.localizedMuscleSubtitle
import com.bearbones.kumalift.core.media.ExerciseThumbnailMedia
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.domain.model.Exercise
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.RpeTier
import com.bearbones.kumalift.domain.model.localizedTitle
import com.bearbones.kumalift.domain.model.localizedRpeTier
import com.bearbones.kumalift.domain.model.localizedSplitCategory
import com.bearbones.kumalift.domain.model.RoutineSetConfig
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.features.logger.CustomRestTimerDialog
import com.bearbones.kumalift.features.logger.ExerciseRestTimerDialog
import com.bearbones.kumalift.features.logger.PrevInputDialog
import com.bearbones.kumalift.features.logger.RepsInputDialog
import com.bearbones.kumalift.features.logger.RpePickerDialog
import com.bearbones.kumalift.features.logger.WeightInputDialog
import androidx.compose.ui.text.style.TextAlign

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateRoutineScreen(
    exerciseRepository: ExerciseRepository,
    routineToEdit: RoutineBlueprint? = null,
    currentProgramId: String = com.bearbones.kumalift.domain.model.DEFAULT_PROGRAM_ID,
    weightUnit: WeightUnit = WeightUnit.KG,
    onNavigateBack: () -> Unit,
    onSaveRoutine: (RoutineBlueprint) -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    var routineTitle by remember(routineToEdit) { mutableStateOf(routineToEdit?.localizedTitle(s) ?: "") }
    var selectedCategory by remember(routineToEdit) {
        mutableStateOf(routineToEdit?.localizedSplitCategory(s) ?: s.presetRoutineCategories.firstOrNull() ?: "")
    }
    var customCategory by remember(routineToEdit) { mutableStateOf("") }
    var selectedDays by remember(routineToEdit) {
        mutableStateOf(routineToEdit?.scheduledDays ?: emptyList())
    }
    var selectedDurationMinutes by remember(routineToEdit) { mutableStateOf(routineToEdit?.estimatedDurationMinutes ?: 60) }
    var selectedRpeTier by remember(routineToEdit) { mutableStateOf(routineToEdit?.localizedRpeTier(s) ?: s.rpeTiers.getOrElse(2) { "Heavy, RPE 8–9" }) }
    val selectedConfigs = remember(routineToEdit) {
        mutableStateListOf<RoutineExerciseConfig>().apply {
            if (routineToEdit != null) {
                addAll(routineToEdit.effectiveConfigs)
            }
        }
    }
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = remember(density) { with(density) { 110.dp.toPx() } }

    val presetCategories = s.presetRoutineCategories
    val durationOptions = listOf(30, 45, 60, 75, 90)
    val rpeOptions = s.rpeTiers

    val finalCategory = if (selectedCategory == s.presetRoutineCategories.last()) {
        customCategory.trim().ifBlank { s.custom }
    } else {
        selectedCategory
    }

    val autoMuscleFocus = remember(selectedConfigs.toList(), s) {
        val muscles = selectedConfigs.map { it.exercise.primaryMuscle.label(s) }.distinct()
        if (muscles.isEmpty()) s.muscleFullBody else muscles.joinToString(" · ")
    }

    val isTitleValid = routineTitle.trim().isNotEmpty()
    val isExercisesValid = selectedConfigs.isNotEmpty()
    val isFormValid = isTitleValid && isExercisesValid
    val screenTitle = if (routineToEdit != null) s.editRoutineTitle else s.createRoutineTitle

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
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                        text = screenTitle,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                }

                Button(
                    onClick = {
                        hasAttemptedSubmit = true
                        if (isFormValid) {
                            val routineId = routineToEdit?.id ?: com.bearbones.kumalift.core.util.IdGenerator.routineId(routineTitle.trim())
                            val muscleCodes = selectedConfigs.map { it.exercise.primaryMuscle.name }.distinct().joinToString(",")
                            val rpeTierCode = RpeTier.fromValue(selectedRpeTier).code
                            val isRenamed = if (routineToEdit != null) {
                                if (routineToEdit.presetKey != null) {
                                    routineTitle.trim() != routineToEdit.localizedTitle(s) || routineToEdit.isUserRenamed
                                } else {
                                    routineToEdit.isUserRenamed
                                }
                            } else {
                                false
                            }
                            val newRoutine = RoutineBlueprint(
                                id = routineId,
                                programId = routineToEdit?.programId ?: currentProgramId,
                                title = routineTitle.trim(),
                                splitCategory = finalCategory,
                                muscleFocus = muscleCodes.ifEmpty { "FULL_BODY" },
                                estimatedDurationMinutes = selectedDurationMinutes,
                                targetRpeTier = rpeTierCode,
                                exerciseTemplates = selectedConfigs.map { it.exercise },
                                exerciseConfigs = selectedConfigs.toList(),
                                plannedSetsPerExercise = selectedConfigs.firstOrNull()?.targetSets ?: 3,
                                scheduledDays = selectedDays.sorted(),
                                presetKey = routineToEdit?.presetKey,
                                isUserRenamed = isRenamed
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
                        maxLines = 1,
                        softWrap = false,
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
                                color = if (hasAttemptedSubmit && !isTitleValid) colors.error else Color.Transparent,
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
                            color = colors.error,
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

                // Section 3: Active Days (Jadwal Hari Aktif)
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.routineActiveDaysLabel,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp
                            )
                        )
                        if (selectedDays.isNotEmpty()) {
                            val countText = buildString {
                                append(selectedDays.size)
                                append(" ")
                                append(s.daysPerWeek)
                            }
                            Text(
                                text = countText,
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.accentText
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = s.routineActiveDaysSubtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 11.sp
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val allDays = (1..7).map { dayNum ->
                        dayNum to (s.dayNamesShort.getOrNull(dayNum - 1) ?: "")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        allDays.forEach { (dayNum, dayLabel) ->
                            val isSelected = selectedDays.contains(dayNum)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.accentFill else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        selectedDays = if (isSelected) {
                                            (selectedDays - dayNum).sorted()
                                        } else {
                                            (selectedDays + dayNum).sorted()
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabel,
                                    maxLines = 1,
                                    softWrap = false,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.onAccentFill else KumaColors.TextWhite,
                                        fontSize = 12.sp,
                                        letterSpacing = 0.sp
                                    )
                                )
                            }
                        }
                    }
                }

                // Section 4: Target Duration
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
                                    maxLines = 1,
                                    softWrap = false,
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
                            text = "${s.exercisesInRoutineHeader} (${selectedConfigs.size}) *",
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

                    if (selectedConfigs.isEmpty()) {
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
                                color = colors.error,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedConfigs.forEachIndexed { index, config ->
                                val isThisDragging = draggingIndex == index
                                SelectedExerciseRow(
                                    index = index + 1,
                                    config = config,
                                    isDragging = isThisDragging,
                                    dragOffsetY = if (isThisDragging) dragOffsetY else 0f,
                                    onDragStart = {
                                        draggingIndex = index
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { delta ->
                                        dragOffsetY += delta
                                        val currentIdx = draggingIndex ?: return@SelectedExerciseRow
                                        val threshold = itemHeightPx * 0.7f
                                        if (dragOffsetY > threshold && currentIdx < selectedConfigs.size - 1) {
                                            val targetIdx = currentIdx + 1
                                            val item = selectedConfigs.removeAt(currentIdx)
                                            selectedConfigs.add(targetIdx, item)
                                            draggingIndex = targetIdx
                                            dragOffsetY -= itemHeightPx
                                        } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                            val targetIdx = currentIdx - 1
                                            val item = selectedConfigs.removeAt(currentIdx)
                                            selectedConfigs.add(targetIdx, item)
                                            draggingIndex = targetIdx
                                            dragOffsetY += itemHeightPx
                                        }
                                    },
                                    onDragEnd = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingIndex = null
                                        dragOffsetY = 0f
                                    },
                                    unit = weightUnit,
                                    onConfigChange = { updatedConfig ->
                                        selectedConfigs[index] = updatedConfig
                                    },
                                    onSupersetChange = { newSs ->
                                        selectedConfigs[index] = config.copy(supersetGroupId = newSs)
                                    },
                                    onRemove = {
                                        if (draggingIndex == index) {
                                            draggingIndex = null
                                            dragOffsetY = 0f
                                        }
                                        selectedConfigs.removeAt(index)
                                    }
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

                Spacer(modifier = Modifier.height(120.dp))
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }

        // Exercise Picker Sheet
        if (showExercisePicker) {
            ExercisePickerModal(
                exerciseRepository = exerciseRepository,
                selectedIds = selectedConfigs.map { it.exercise.id }.toSet(),
                onAddExercise = { ex ->
                    if (!selectedConfigs.any { it.exercise.id == ex.id }) {
                        val initialSets = (1..3).map { setNum ->
                            RoutineSetConfig(
                                setNumber = setNum,
                                targetReps = 8
                            )
                        }
                        selectedConfigs.add(
                            RoutineExerciseConfig(
                                exercise = ex,
                                targetSets = 3,
                                targetReps = 8,
                                restTimerSeconds = 90,
                                setConfigs = initialSets
                            )
                        )
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
    config: RoutineExerciseConfig,
    isDragging: Boolean,
    dragOffsetY: Float,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    unit: WeightUnit = WeightUnit.KG,
    onConfigChange: (RoutineExerciseConfig) -> Unit,
    onSupersetChange: (String?) -> Unit,
    onRemove: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val exercise = config.exercise
    val effectiveSets = config.effectiveSets

    var activeDialogSetIndex by remember { mutableStateOf<Int?>(null) }
    var showWeightDialogForSet by remember { mutableStateOf(false) }
    var showRepsDialogForSet by remember { mutableStateOf(false) }
    var showRpeDialogForSet by remember { mutableStateOf(false) }
    var showPrevDialogForSet by remember { mutableStateOf(false) }
    var showSetRestTimerDialog by remember { mutableStateOf(false) }
    var showCustomRestTimerDialog by remember { mutableStateOf(false) }

    // Weight Dialog
    if (showWeightDialogForSet && activeDialogSetIndex != null && activeDialogSetIndex!! < effectiveSets.size) {
        val targetSet = effectiveSets[activeDialogSetIndex!!]
        val displayWeight = targetSet.targetWeightKg?.let { UnitConverters.toDisplayWeight(it, unit) } ?: 0.0
        WeightInputDialog(
            initialWeight = displayWeight,
            unit = unit,
            onDismiss = {
                showWeightDialogForSet = false
                activeDialogSetIndex = null
            },
            onConfirm = { newWeight ->
                val newStorageKg = UnitConverters.toStorageWeightKg(newWeight, unit)
                val sets = effectiveSets.toMutableList()
                sets[activeDialogSetIndex!!] = sets[activeDialogSetIndex!!].copy(
                    targetWeightKg = if (newStorageKg > 0.0) newStorageKg else null
                )
                onConfigChange(config.copy(targetSets = sets.size, setConfigs = sets))
                showWeightDialogForSet = false
                activeDialogSetIndex = null
            }
        )
    }

    // Reps Dialog
    if (showRepsDialogForSet && activeDialogSetIndex != null && activeDialogSetIndex!! < effectiveSets.size) {
        val targetSet = effectiveSets[activeDialogSetIndex!!]
        RepsInputDialog(
            initialReps = targetSet.targetReps,
            onDismiss = {
                showRepsDialogForSet = false
                activeDialogSetIndex = null
            },
            onConfirm = { newReps ->
                val sets = effectiveSets.toMutableList()
                sets[activeDialogSetIndex!!] = sets[activeDialogSetIndex!!].copy(targetReps = newReps)
                onConfigChange(config.copy(targetSets = sets.size, setConfigs = sets))
                showRepsDialogForSet = false
                activeDialogSetIndex = null
            }
        )
    }

    // RPE Dialog
    if (showRpeDialogForSet && activeDialogSetIndex != null && activeDialogSetIndex!! < effectiveSets.size) {
        val targetSet = effectiveSets[activeDialogSetIndex!!]
        RpePickerDialog(
            initialRpe = targetSet.targetRpe,
            onDismiss = {
                showRpeDialogForSet = false
                activeDialogSetIndex = null
            },
            onConfirm = { newRpe ->
                val sets = effectiveSets.toMutableList()
                sets[activeDialogSetIndex!!] = sets[activeDialogSetIndex!!].copy(targetRpe = newRpe)
                onConfigChange(config.copy(targetSets = sets.size, setConfigs = sets))
                showRpeDialogForSet = false
                activeDialogSetIndex = null
            }
        )
    }

    // Previous Dialog
    if (showPrevDialogForSet && activeDialogSetIndex != null && activeDialogSetIndex!! < effectiveSets.size) {
        val targetSet = effectiveSets[activeDialogSetIndex!!]
        PrevInputDialog(
            initialWeightKg = targetSet.previousWeightKg,
            initialReps = targetSet.previousReps,
            unit = unit,
            onDismiss = {
                showPrevDialogForSet = false
                activeDialogSetIndex = null
            },
            onConfirm = { prevWeightKg, prevReps ->
                val sets = effectiveSets.toMutableList()
                sets[activeDialogSetIndex!!] = sets[activeDialogSetIndex!!].copy(
                    previousWeightKg = prevWeightKg,
                    previousReps = prevReps
                )
                onConfigChange(config.copy(targetSets = sets.size, setConfigs = sets))
                showPrevDialogForSet = false
                activeDialogSetIndex = null
            }
        )
    }

    // Per-Set Custom Rest Timer Dialog
    if (showSetRestTimerDialog && activeDialogSetIndex != null && activeDialogSetIndex!! < effectiveSets.size) {
        val targetSet = effectiveSets[activeDialogSetIndex!!]
        CustomRestTimerDialog(
            initialSeconds = targetSet.restTimerSeconds ?: config.restTimerSeconds,
            title = s.setRestTimerTitleWithNumber(targetSet.setNumber),
            onDismiss = {
                showSetRestTimerDialog = false
                activeDialogSetIndex = null
            },
            onConfirm = { newSec ->
                val updated = effectiveSets.toMutableList()
                updated[activeDialogSetIndex!!] = targetSet.copy(restTimerSeconds = newSec)
                onConfigChange(config.copy(setConfigs = updated))
                showSetRestTimerDialog = false
                activeDialogSetIndex = null
            }
        )
    }

    // Custom Rest Timer Dialog (All sets & per-set tabs)
    if (showCustomRestTimerDialog) {
        ExerciseRestTimerDialog(
            exerciseName = config.exercise.name,
            initialExerciseSeconds = config.restTimerSeconds,
            sets = effectiveSets.map { Triple(it.setNumber.toString(), it.setNumber, it.targetReps) },
            initialSetTimers = effectiveSets.associate { it.setNumber.toString() to (it.restTimerSeconds ?: config.restTimerSeconds) },
            onDismiss = { showCustomRestTimerDialog = false },
            onConfirm = { exerciseSec, perSetTimers ->
                val updatedSets = effectiveSets.map { sc ->
                    val setSec = perSetTimers[sc.setNumber.toString()]
                    sc.copy(restTimerSeconds = setSec)
                }
                onConfigChange(
                    config.copy(
                        restTimerSeconds = exerciseSec,
                        setConfigs = updatedSets
                    )
                )
                showCustomRestTimerDialog = false
            }
        )
    }

    val cardBorder = when {
        isDragging -> Modifier.border(2.dp, colors.accentFill, RoundedCornerShape(14.dp))
        config.supersetGroupId != null -> Modifier.border(1.5.dp, colors.supersetBadge, RoundedCornerShape(14.dp))
        else -> Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging) 10f else 0f)
            .graphicsLayer {
                if (isDragging) {
                    translationY = dragOffsetY
                    scaleX = 1.03f
                    scaleY = 1.03f
                    shadowElevation = 16.dp.toPx()
                    shape = RoundedCornerShape(14.dp)
                    clip = false
                }
            }
            .then(cardBorder)
            .animateContentSize()
    ) {
        KumaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = if (isDragging) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainerLow,
            cornerRadius = 14.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Exercise Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Drag handle for smooth touch drag reordering
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDragging) colors.accentFill.copy(alpha = 0.2f) else Color.Transparent)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = { onDragStart() },
                                    onVerticalDrag = { change, amount ->
                                        change.consume()
                                        onDrag(amount)
                                    },
                                    onDragEnd = { onDragEnd() },
                                    onDragCancel = { onDragCancel() }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapVert,
                            contentDescription = s.dragToReorder,
                            tint = if (isDragging) colors.accentFill else colors.textSecondary.copy(alpha = 0.7f),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    // Index number badge
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
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White)
                    ) {
                        ExerciseThumbnailMedia(
                            assetPath = exercise.thumbnailUrl,
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Title, Muscle
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                fontSize = 15.sp,
                                lineHeight = 18.sp
                            ),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = exercise.localizedMuscleSubtitle(s),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Action buttons: Superset pill + Remove button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Superset Pill Toggle
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(
                                    if (config.supersetGroupId != null) colors.supersetBadgeTint
                                    else KumaColors.SurfaceContainer
                                )
                                .clickable {
                                    val nextSs = when (config.supersetGroupId) {
                                        null -> "A"
                                        "A" -> "B"
                                        "B" -> null
                                        else -> null
                                    }
                                    onSupersetChange(nextSs)
                                }
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (config.supersetGroupId != null) "SS ${config.supersetGroupId}" else "+ SS",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (config.supersetGroupId != null) colors.supersetBadge else KumaColors.TextMuted
                                )
                            )
                        }

                        // Remove button
                        var showRemoveExerciseConfirm by remember { mutableStateOf(false) }
                        if (showRemoveExerciseConfirm) {
                            KumaDeleteConfirmationDialog(
                                title = s.removeRoutineExerciseConfirmTitle,
                                message = s.removeRoutineExerciseConfirmMessage(exercise.name),
                                confirmText = s.delete,
                                cancelText = s.cancel,
                                onConfirm = {
                                    showRemoveExerciseConfirm = false
                                    onRemove()
                                },
                                onDismiss = { showRemoveExerciseConfirm = false }
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(KumaColors.SurfaceContainer)
                                .clickable { showRemoveExerciseConfirm = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = s.remove,
                                tint = colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Sets Table Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.setColumnHeader,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = s.prevColumnHeader,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp)
                    )
                    Text(
                        text = if (unit == WeightUnit.LB) s.unitLb.uppercase() else s.unitKg.uppercase(),
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.1f).padding(horizontal = 2.dp)
                    )
                    Text(
                        text = s.repsColumnHeader,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1.0f).padding(horizontal = 2.dp)
                    )
                    Text(
                        text = s.rpeColumnHeader,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 0.sp),
                        color = KumaColors.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(0.9f).padding(horizontal = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(32.dp))
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Set Rows
                effectiveSets.forEachIndexed { sIndex, setCfg ->
                    val prevDisplay = if (setCfg.previousWeightKg != null && setCfg.previousReps != null) {
                        val dispW = UnitConverters.toDisplayWeight(setCfg.previousWeightKg, unit)
                        "$dispW × ${setCfg.previousReps}"
                    } else setCfg.previousPerformance ?: "—"

                    val weightDisplay = if (setCfg.targetWeightKg != null && setCfg.targetWeightKg > 0.0) {
                        UnitConverters.toDisplayWeight(setCfg.targetWeightKg, unit).toString().removeSuffix(".0")
                    } else "—"

                    val rpeDisplay = if (setCfg.targetRpe != null) {
                        if (setCfg.targetRpe == setCfg.targetRpe.toLong().toDouble()) setCfg.targetRpe.toLong().toString() else "${setCfg.targetRpe}"
                    } else "—"

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Set Number (clickable for custom rest timer on this set)
                        Column(
                            modifier = Modifier
                                .width(32.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    activeDialogSetIndex = sIndex
                                    showSetRestTimerDialog = true
                                },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "${setCfg.setNumber}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                ),
                                textAlign = TextAlign.Center
                            )
                            if (setCfg.restTimerSeconds != null) {
                                Text(
                                    text = s.timerBadgeSeconds(setCfg.restTimerSeconds),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.warning
                                    )
                                )
                            }
                        }

                        // PREV Cell
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .padding(horizontal = 2.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    activeDialogSetIndex = sIndex
                                    showPrevDialogForSet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = prevDisplay,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (prevDisplay == "—") FontWeight.Normal else FontWeight.SemiBold,
                                    color = if (prevDisplay == "—") KumaColors.TextMuted else colors.textPrimary
                                ),
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // KG Cell
                        Box(
                            modifier = Modifier
                                .weight(1.1f)
                                .padding(horizontal = 2.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    activeDialogSetIndex = sIndex
                                    showWeightDialogForSet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = weightDisplay,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (weightDisplay == "—") KumaColors.TextMuted else colors.textPrimary
                                )
                            )
                        }

                        // REPS Cell
                        Box(
                            modifier = Modifier
                                .weight(1.0f)
                                .padding(horizontal = 2.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    activeDialogSetIndex = sIndex
                                    showRepsDialogForSet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${setCfg.targetReps}",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                        }

                        // RPE Cell
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(horizontal = 2.dp)
                                .height(38.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    activeDialogSetIndex = sIndex
                                    showRpeDialogForSet = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = rpeDisplay,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (rpeDisplay == "—") KumaColors.TextMuted else colors.accentText
                                )
                            )
                        }

                        // Delete Set button
                        if (effectiveSets.size > 1) {
                            var pendingDeleteSetIndex by remember { mutableStateOf<Int?>(null) }
                            pendingDeleteSetIndex?.let { setIdxToRemove ->
                                KumaDeleteConfirmationDialog(
                                    title = s.deleteRoutineSetConfirmTitle,
                                    message = s.deleteRoutineSetConfirmMessage(setIdxToRemove + 1),
                                    confirmText = s.delete,
                                    cancelText = s.cancel,
                                    onConfirm = {
                                        val currentSets = effectiveSets.toMutableList()
                                        if (setIdxToRemove in currentSets.indices) {
                                            currentSets.removeAt(setIdxToRemove)
                                            val renumbered = currentSets.mapIndexed { i, sCfg -> sCfg.copy(setNumber = i + 1) }
                                            onConfigChange(config.copy(targetSets = renumbered.size, setConfigs = renumbered))
                                        }
                                        pendingDeleteSetIndex = null
                                    },
                                    onDismiss = { pendingDeleteSetIndex = null }
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .clickable { pendingDeleteSetIndex = sIndex },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = s.deleteSetAction,
                                    tint = colors.textSecondary.copy(alpha = 0.7f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(32.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom actions: Add Set & Custom Rest Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // + Add Set Button
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(8.dp))
                            .background(KumaColors.SurfaceContainer)
                            .border(1.dp, colors.accentFill.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .clickable {
                                val lastSet = effectiveSets.lastOrNull()
                                val nextNum = effectiveSets.size + 1
                                val newSet = RoutineSetConfig(
                                    setNumber = nextNum,
                                    targetReps = lastSet?.targetReps ?: 8,
                                    targetWeightKg = lastSet?.targetWeightKg,
                                    targetRpe = lastSet?.targetRpe,
                                    previousWeightKg = lastSet?.previousWeightKg,
                                    previousReps = lastSet?.previousReps
                                )
                                val updatedSets = effectiveSets + newSet
                                onConfigChange(config.copy(targetSets = updatedSets.size, setConfigs = updatedSets))
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = s.addSetButton,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.accentText
                                )
                            )
                        }
                    }

                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.weight(1f))

                    // Custom Rest Timer Button
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(KumaColors.SurfaceContainer)
                            .clickable { showCustomRestTimerDialog = true }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = s.secondsRestLabel(config.restTimerSeconds),
                                maxLines = 1,
                                softWrap = false,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExercisePickerModal(
    exerciseRepository: ExerciseRepository,
    selectedIds: Set<String>,
    onAddExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
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

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = KumaColors.CanvasLowest
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .kumaKeyboardAvoidance()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
            ) {
                // Header: Title & Done
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                Text(
                    text = s.exercisePickerTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
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
                    Text(s.done, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
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
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = 15.sp,
                                        lineHeight = 18.sp
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
}
