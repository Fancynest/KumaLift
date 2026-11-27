package com.bearbones.kumalift.features.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaDeleteConfirmationDialog
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.WorkoutScheduleEvent
import com.bearbones.kumalift.domain.model.localizeSplitCategory
import com.bearbones.kumalift.domain.model.localizedSplitCategory
import com.bearbones.kumalift.domain.model.localizedTitle
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutScheduleDialog(
    initialDate: LocalDate,
    existingEvent: WorkoutScheduleEvent? = null,
    availableRoutines: List<RoutineBlueprint>,
    onDismiss: () -> Unit,
    onSaveSchedule: (routine: RoutineBlueprint, targetDate: LocalDate, notes: String?) -> Unit,
    onReschedule: ((eventId: String, newDate: LocalDate) -> Unit)? = null,
    onCancelSchedule: ((eventId: String) -> Unit)? = null,
    onRestoreSchedule: ((eventId: String) -> Unit)? = null,
    onDeleteSchedule: ((eventId: String) -> Unit)? = null
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var targetDate by remember { mutableStateOf(initialDate) }
    var selectedRoutineId by remember {
        mutableStateOf(existingEvent?.routineId ?: availableRoutines.firstOrNull()?.id ?: "")
    }
    var notes by remember { mutableStateOf(existingEvent?.notes ?: "") }

    val selectedRoutine = availableRoutines.firstOrNull { it.id == selectedRoutineId }
    val dayOfWeekName = s.dayNamesFull.getOrElse(targetDate.dayOfWeek.ordinal) { "" }
    val monthName = s.monthNamesFull.getOrElse(targetDate.monthNumber - 1) { "" }
    val dateDisplay = "$dayOfWeekName, ${targetDate.dayOfMonth} $monthName ${targetDate.year}"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingEvent != null) s.rescheduleWorkout else s.scheduleRoutineTitle,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = s.cancel,
                        tint = colors.textSecondary
                    )
                }
            }

            // Target Date Display & Day Adjuster
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = s.scheduleDateLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = dateDisplay,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.accentText
                        )
                    )
                }
                if (existingEvent == null || (onReschedule != null && existingEvent.isPlanned)) {
                    IconButton(
                        onClick = { targetDate = targetDate.minus(DatePeriod(days = 1)) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                    }
                    IconButton(
                        onClick = { targetDate = targetDate.plus(DatePeriod(days = 1)) },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = colors.textPrimary
                        )
                    }
                }
            }

            // Routine Selection (if creating fresh)
            if (existingEvent == null) {
                Text(
                    text = s.selectRoutineToSchedule,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )

                if (availableRoutines.isEmpty()) {
                    Text(
                        text = s.noRoutinesAvailable,
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableRoutines, key = { it.id }) { routine ->
                            val isSelected = (routine.id == selectedRoutineId)
                            val itemBorder = if (isSelected) colors.accentFill else colors.border
                            val itemBg = if (isSelected) colors.accentTint else colors.surfaceVariant

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(itemBg)
                                    .border(1.dp, itemBorder, RoundedCornerShape(12.dp))
                                    .clickable { selectedRoutineId = routine.id }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = routine.localizedTitle(s),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                    )
                                    KumaBadge(text = routine.localizedSplitCategory(s))
                                }

                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(colors.accentFill),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = colors.onAccentFill,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Showing existing routine info snapshot
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceVariant)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = s.routineNameLabel,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )
                    Text(
                        text = existingEvent.routineTitleSnapshot,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    existingEvent.splitTypeSnapshot?.let { split ->
                        KumaBadge(text = localizeSplitCategory(split, s))
                    }
                }
            }

            // Optional Notes Field
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(s.scheduleNotesPlaceholder) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accentFill,
                    unfocusedBorderColor = colors.border,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary
                ),
                maxLines = 2,
                shape = RoundedCornerShape(12.dp)
            )

            // Action Buttons
            if (existingEvent == null) {
                Button(
                    onClick = {
                        selectedRoutine?.let { routine ->
                            onSaveSchedule(routine, targetDate, notes)
                            onDismiss()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                    enabled = selectedRoutine != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    ),
                    shape = PillShape
                ) {
                    Text(
                        text = s.addScheduleWorkout,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            } else {
                // Existing Event controls: Reschedule / Cancel / Delete
                if (existingEvent.isPlanned && onReschedule != null && targetDate != initialDate) {
                    Button(
                        onClick = {
                            onReschedule(existingEvent.id, targetDate)
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        ),
                        shape = PillShape
                    ) {
                        Text(
                            text = s.save,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existingEvent.isCancelled) {
                        Button(
                            onClick = {
                                onRestoreSchedule?.invoke(existingEvent.id)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentFill,
                                contentColor = colors.onAccentFill
                            ),
                            shape = PillShape
                        ) {
                            Text(
                                text = s.restoreSchedule,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else if (existingEvent.isPlanned) {
                        Button(
                            onClick = {
                                onCancelSchedule?.invoke(existingEvent.id)
                                onDismiss()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.surfaceBright,
                                contentColor = colors.textPrimary
                            ),
                            shape = PillShape
                        ) {
                            Text(
                                text = s.cancelSchedule,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    onDeleteSchedule?.let { onDelete ->
                        var showDeleteConfirm by remember { mutableStateOf(false) }
                        if (showDeleteConfirm) {
                            KumaDeleteConfirmationDialog(
                                title = s.deleteScheduleConfirmTitle,
                                message = s.deleteScheduleConfirmMessage,
                                confirmText = s.deleteSchedule,
                                cancelText = s.cancel,
                                onConfirm = {
                                    showDeleteConfirm = false
                                    onDelete(existingEvent.id)
                                    onDismiss()
                                },
                                onDismiss = { showDeleteConfirm = false }
                            )
                        }
                        Button(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier
                                .weight(0.75f)
                                .heightIn(min = 48.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.errorTint,
                                contentColor = colors.error
                            ),
                            shape = PillShape
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = s.deleteSchedule,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.size(4.dp))
                            Text(
                                text = s.deleteSchedule,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
