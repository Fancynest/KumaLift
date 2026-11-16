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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaPrimaryButton
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.TrainingGoal
import com.bearbones.kumalift.domain.model.TrainingProgram

@Composable
fun TrainingProgramSelectorModal(
    programs: List<TrainingProgram>,
    activeProgramId: String,
    routinesCountByProgram: Map<String, Int>,
    onSelectProgram: (TrainingProgram) -> Unit,
    onEditProgram: (TrainingProgram) -> Unit,
    onDeleteProgram: (TrainingProgram) -> Unit,
    onCreateNewProgram: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(20.dp),
                color = KumaColors.SurfaceDim,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.trainingProgramsTitle,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = KumaColors.TextWhite
                            )
                        )

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = s.cancel,
                                tint = KumaColors.TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Programs List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(programs, key = { it.id }) { program ->
                            val isActive = program.id == activeProgramId
                            val routineCount = routinesCountByProgram[program.id] ?: 0

                            ProgramItemCard(
                                program = program,
                                isActive = isActive,
                                routineCount = routineCount,
                                onSelect = { onSelectProgram(program) },
                                onEdit = { onEditProgram(program) },
                                onDelete = { onDeleteProgram(program) },
                                canDelete = programs.size > 1
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Button: Create New Program
                    KumaPrimaryButton(
                        text = s.newProgramButton,
                        onClick = onCreateNewProgram,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun ProgramItemCard(
    program: TrainingProgram,
    isActive: Boolean,
    routineCount: Int,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    val borderColor = if (isActive) colors.accentFill else KumaColors.BorderDefault
    val bgColor = if (isActive) KumaColors.SurfaceContainerHigh else KumaColors.SurfaceContainer

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isActive) 1.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onSelect() }
            .padding(14.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Title & Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = program.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = KumaColors.TextWhite
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (isActive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.accentFill)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = s.activeBadge,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.onAccentFill
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Metadata Chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Chip
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(KumaColors.SurfaceDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = goalDisplayName(program.category),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = KumaColors.TextSecondary
                        )
                    )
                }

                // Target Days
                val dayLabels = program.activeDays.mapNotNull { d ->
                    s.dayNamesShort.getOrNull(d - 1)
                }.joinToString(", ")
                val targetDaysSummary = buildString {
                    append(program.targetDaysPerWeek)
                    append(" ")
                    append(s.daysPerWeek)
                    if (dayLabels.isNotBlank()) {
                        append(" (")
                        append(dayLabels)
                        append(")")
                    }
                }
                Text(
                    text = targetDaysSummary,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    )
                )

                Text(
                    text = "•",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    )
                )

                // Routines Count
                Text(
                    text = s.routinesInProgramCount(routineCount),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = KumaColors.TextMuted
                    )
                )
            }

            if (program.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = program.description,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = KumaColors.TextSecondary
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isActive) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.accentFill.copy(alpha = 0.15f))
                            .border(1.dp, colors.accentFill, PillShape)
                            .clickable { onSelect() }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = s.useProgramButton,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.accentFill
                            )
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = s.edit,
                            tint = KumaColors.TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (canDelete) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = s.delete,
                                tint = KumaColors.ErrorRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateEditProgramModal(
    programToEdit: TrainingProgram? = null,
    onDismiss: () -> Unit,
    onSave: (name: String, description: String, category: TrainingGoal, activeDays: List<Int>) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var name by remember(programToEdit) { mutableStateOf(programToEdit?.name ?: "") }
    var description by remember(programToEdit) { mutableStateOf(programToEdit?.description ?: "") }
    var category by remember(programToEdit) { mutableStateOf(programToEdit?.category ?: TrainingGoal.HYPERTROPHY) }
    var selectedDays by remember(programToEdit) {
        mutableStateOf(programToEdit?.activeDays?.filter { it in 1..7 }?.distinct()?.take(3)?.ifEmpty { listOf(1, 3, 5) } ?: listOf(1, 3, 5))
    }
    var nameError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.75f)),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 680.dp),
                shape = RoundedCornerShape(20.dp),
                color = KumaColors.SurfaceDim,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                        .kumaKeyboardAvoidance()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Modal Title
                    Text(
                        text = if (programToEdit != null) s.editProgramTitle else s.createProgramTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = KumaColors.TextWhite
                        )
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    // Program Name Input
                    Text(
                        text = s.programNameLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = KumaColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            if (it.isNotBlank()) nameError = false
                        },
                        placeholder = { Text(s.programNamePlaceholder, color = KumaColors.TextMuted) },
                        isError = nameError,
                        supportingText = if (nameError) {
                            { Text(s.programNameRequiredError, color = KumaColors.ErrorRed) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentFill,
                            unfocusedBorderColor = KumaColors.BorderDefault,
                            focusedTextColor = KumaColors.TextWhite,
                            unfocusedTextColor = KumaColors.TextWhite,
                            focusedContainerColor = KumaColors.SurfaceContainer,
                            unfocusedContainerColor = KumaColors.SurfaceContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Description Input
                    Text(
                        text = s.programDescriptionLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = KumaColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        placeholder = { Text(s.programDescriptionPlaceholder, color = KumaColors.TextMuted) },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentFill,
                            unfocusedBorderColor = KumaColors.BorderDefault,
                            focusedTextColor = KumaColors.TextWhite,
                            unfocusedTextColor = KumaColors.TextWhite,
                            focusedContainerColor = KumaColors.SurfaceContainer,
                            unfocusedContainerColor = KumaColors.SurfaceContainer
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Category Selection Chips
                    Text(
                        text = s.programCategoryLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = KumaColors.TextSecondary
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TrainingGoal.entries.forEach { goal ->
                            val isSelected = goal == category
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.accentFill else KumaColors.BorderDefault,
                                        shape = PillShape
                                    )
                                    .clickable { category = goal }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = goalDisplayName(goal),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.onAccentFill else KumaColors.TextWhite
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Active Days Selector (Max 3 days)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.programActiveDaysLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = KumaColors.TextSecondary
                            )
                        )
                        Text(
                            text = "${selectedDays.size} / 3",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = if (selectedDays.size >= 3) colors.accentText else KumaColors.TextMuted
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = s.maxDaysNotice,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = KumaColors.TextMuted,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val allDays = (1..7).map { dayNum ->
                        dayNum to (s.dayNamesShort.getOrNull(dayNum - 1) ?: "D$dayNum")
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
                                        if (isSelected) {
                                            if (selectedDays.size > 1) {
                                                selectedDays = (selectedDays - dayNum).sorted()
                                            }
                                        } else {
                                            if (selectedDays.size < 3) {
                                                selectedDays = (selectedDays + dayNum).sorted()
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayLabel,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.onAccentFill else KumaColors.TextWhite,
                                        fontSize = 12.sp
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(26.dp))

                    // Actions (Cancel / Save)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = s.cancel,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = KumaColors.TextSecondary
                                )
                            )
                        }

                        Button(
                            onClick = {
                                if (name.isBlank()) {
                                    nameError = true
                                } else {
                                    onSave(name.trim(), description.trim(), category, selectedDays.sorted())
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentFill,
                                contentColor = colors.onAccentFill
                            )
                        ) {
                            Text(
                                text = s.save,
                                style = MaterialTheme.typography.labelLarge.copy(
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

@Composable
fun DeleteProgramConfirmationDialog(
    program: TrainingProgram,
    routineCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = KumaColors.SurfaceDim,
            border = androidx.compose.foundation.BorderStroke(1.dp, KumaColors.BorderDefault),
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(0.95f)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = s.deleteProgramTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = KumaColors.TextWhite
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = s.deleteProgramMessage(program.name, routineCount),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = KumaColors.TextSecondary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = s.cancel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = KumaColors.TextSecondary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KumaColors.ErrorRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = s.delete,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
            }
        }
    }
}

private fun goalDisplayName(goal: TrainingGoal): String {
    return when (goal) {
        TrainingGoal.HYPERTROPHY -> "Hypertrophy"
        TrainingGoal.STRENGTH -> "Strength"
        TrainingGoal.FAT_LOSS -> "Fat Loss"
        TrainingGoal.ENDURANCE -> "Endurance"
        TrainingGoal.GENERAL -> "General Fitness"
    }
}
