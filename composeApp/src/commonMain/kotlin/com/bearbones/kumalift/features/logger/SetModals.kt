package com.bearbones.kumalift.features.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.domain.model.WeightUnit
import kotlin.math.roundToInt

@Composable
fun WeightInputDialog(
    initialWeight: Double,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val focusManager = LocalFocusManager.current
    val initialStr = if (initialWeight > 0.0) {
        if (initialWeight == initialWeight.toLong().toDouble()) initialWeight.toLong().toString() else initialWeight.toString()
    } else ""
    var textValue by remember { mutableStateOf(initialStr) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPlateCalcModal by remember { mutableStateOf(false) }

    val handleConfirm = {
        val trimmed = textValue.trim()
        if (trimmed.isEmpty()) {
            onConfirm(0.0)
        } else {
            val parsed = NumberFormatUtils.parseDecimalInput(trimmed)
            if (parsed != null && parsed.isFinite() && parsed in 0.0..1500.0) {
                onConfirm(parsed)
            } else {
                errorMessage = s.enterValidWeightHint
            }
        }
    }

    if (showPlateCalcModal) {
        val currentDisplayWeight = NumberFormatUtils.parseDecimalInput(textValue) ?: initialWeight
        val initialKg = UnitConverters.toStorageWeightKg(currentDisplayWeight, unit)
        PlateCalculatorDialog(
            initialWeightKg = initialKg,
            unit = unit,
            onDismiss = { showPlateCalcModal = false },
            onConfirm = { calculatedWeightKg ->
                val converted = UnitConverters.toDisplayWeight(calculatedWeightKg, unit)
                val formatted = if (converted == converted.toLong().toDouble()) {
                    converted.toLong().toString()
                } else {
                    converted.toString()
                }
                textValue = formatted
                errorMessage = null
                showPlateCalcModal = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = "${s.editWeightTitle} (${if (unit == WeightUnit.LB) s.unitLb else s.unitKg})",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[,.]?\d*$"""))) {
                            textValue = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        handleConfirm()
                    }),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.ErrorRed))
                        } else {
                            Text(s.enterValidWeightHint, style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5.0, -1.0, 1.0, 5.0).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val current = NumberFormatUtils.parseDecimalInput(textValue) ?: 0.0
                                    val updated = maxOf(0.0, current + delta)
                                    textValue = if (updated == updated.toLong().toDouble()) updated.toLong().toString() else updated.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (delta > 0) "+$delta" else "$delta").removeSuffix(".0"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.accentFill.copy(alpha = 0.12f))
                        .clickable { showPlateCalcModal = true }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.plateCalculatorTitle,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.accentText
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = handleConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                )
            ) {
                Text(s.save, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
            }
        }
    )
}

@Composable
fun RepsInputDialog(
    initialReps: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val focusManager = LocalFocusManager.current
    var textValue by remember { mutableStateOf(if (initialReps > 0) initialReps.toString() else "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val handleConfirm = {
        val reps = textValue.trim().toIntOrNull()
        if (reps != null && reps in 1..999) {
            onConfirm(reps)
        } else {
            errorMessage = s.enterValidRepsHint
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = s.editRepsTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d+$"""))) {
                            textValue = input
                            errorMessage = null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        handleConfirm()
                    }),
                    singleLine = true,
                    isError = errorMessage != null,
                    supportingText = {
                        if (errorMessage != null) {
                            Text(errorMessage!!, style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.ErrorRed))
                        } else {
                            Text(s.enterValidRepsHint, style = MaterialTheme.typography.bodySmall.copy(color = KumaColors.TextMuted))
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5, -1, 1, 5).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val current = textValue.toIntOrNull() ?: 0
                                    val updated = maxOf(0, current + delta)
                                    textValue = updated.toString()
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = handleConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                )
            ) {
                Text(s.save, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RpePickerDialog(
    initialRpe: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    var selectedRpe by remember { mutableStateOf(initialRpe) }
    var textValue by remember {
        mutableStateOf(
            if (initialRpe != null) {
                if (initialRpe == initialRpe.toLong().toDouble()) initialRpe.toLong().toString() else initialRpe.toString()
            } else ""
        )
    }

    val rpePresets = listOf(
        10.0 to "0 RIR",
        9.5 to "0–1 RIR",
        9.0 to "1 RIR",
        8.5 to "1–2 RIR",
        8.0 to "2 RIR",
        7.5 to "2–3 RIR",
        7.0 to "3 RIR",
        6.5 to "3–4 RIR",
        6.0 to "4+ RIR"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Column {
                Text(
                    text = s.selectRpeTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = s.rpeScaleHint,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = colors.textSecondary
                    )
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Preset chips
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rpePresets.forEach { (rpeVal, rirLabel) ->
                        val isSelected = selectedRpe == rpeVal
                        val rpeDisplay = if (rpeVal == rpeVal.toLong().toDouble()) rpeVal.toLong().toString() else rpeVal.toString()
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                .clickable {
                                    selectedRpe = rpeVal
                                    textValue = rpeDisplay
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$rpeDisplay · $rirLabel",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Custom Decimal Input
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[,.]?\d*$"""))) {
                            textValue = input
                            val parsed = NumberFormatUtils.parseDecimalInput(input)
                            selectedRpe = if (parsed != null && parsed in 1.0..10.0) parsed else null
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.rpeColumnHeader, style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("8.0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onConfirm(null)
                    }
                ) {
                    Text(
                        text = s.clearValueButton,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(color = KumaColors.ErrorRed)
                    )
                }
                Button(
                    onClick = {
                        val parsed = NumberFormatUtils.parseDecimalInput(textValue) ?: selectedRpe
                        val finalRpe = parsed?.coerceIn(1.0, 10.0)
                        onConfirm(finalRpe)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(
                        text = s.save,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = s.cancel,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary)
                )
            }
        }
    )
}

@Composable
fun PrevInputDialog(
    initialWeightKg: Double?,
    initialReps: Int?,
    unit: WeightUnit,
    onDismiss: () -> Unit,
    onConfirm: (Double?, Int?) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    val initialDisplayWeight = initialWeightKg?.let { UnitConverters.toDisplayWeight(it, unit) }
    var weightText by remember {
        mutableStateOf(
            if (initialDisplayWeight != null && initialDisplayWeight > 0.0) {
                if (initialDisplayWeight == initialDisplayWeight.toLong().toDouble()) {
                    initialDisplayWeight.toLong().toString()
                } else initialDisplayWeight.toString()
            } else ""
        )
    }
    var repsText by remember {
        mutableStateOf(if (initialReps != null && initialReps > 0) initialReps.toString() else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = s.editPreviousTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                // Weight input
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d*[,.]?\d*$"""))) {
                            weightText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(
                            text = "${s.previousWeightLabel} (${if (unit == WeightUnit.LB) s.unitLb else s.unitKg})",
                            style = MaterialTheme.typography.labelSmall
                        )
                    },
                    placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-5.0, -1.0, 1.0, 5.0).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val current = NumberFormatUtils.parseDecimalInput(weightText) ?: 0.0
                                    val updated = maxOf(0.0, current + delta)
                                    weightText = if (updated == updated.toLong().toDouble()) updated.toLong().toString() else updated.toString()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = (if (delta > 0) "+$delta" else "$delta").removeSuffix(".0"),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Reps input
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { input ->
                        if (input.isEmpty() || input.matches(Regex("""^\d+$"""))) {
                            repsText = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.previousRepsLabel, style = MaterialTheme.typography.labelSmall) },
                    placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(-2, -1, 1, 2).forEach { delta ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(KumaColors.SurfaceContainer)
                                .clickable {
                                    val current = repsText.toIntOrNull() ?: 0
                                    val updated = maxOf(0, current + delta)
                                    repsText = updated.toString()
                                }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (delta > 0) "+$delta" else "$delta",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (delta > 0) colors.accentText else colors.textPrimary
                                )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = {
                        onConfirm(null, null)
                    }
                ) {
                    Text(
                        text = s.clearValueButton,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(color = KumaColors.ErrorRed)
                    )
                }
                Button(
                    onClick = {
                        val parsedWeight = NumberFormatUtils.parseDecimalInput(weightText)
                        val weightKg = parsedWeight?.let { UnitConverters.toStorageWeightKg(it, unit) }
                        val reps = repsText.toIntOrNull()
                        onConfirm(weightKg, reps)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(
                        text = s.save,
                        maxLines = 1,
                        softWrap = false,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = s.cancel,
                    maxLines = 1,
                    softWrap = false,
                    style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary)
                )
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomRestTimerDialog(
    initialSeconds: Int,
    title: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    val initialSec = initialSeconds.coerceAtLeast(15)
    var totalSeconds by remember { mutableStateOf(initialSec) }
    var minText by remember { mutableStateOf("${initialSec / 60}") }
    var secText by remember { mutableStateOf("${initialSec % 60}") }

    val presets = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Text(
                text = title ?: s.customRestTimerTitle,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Big preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(KumaColors.SurfaceContainer)
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = s.secondsRestLabel(totalSeconds),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.accentText
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    presets.forEach { secPreset ->
                        val isSelected = totalSeconds == secPreset
                        Box(
                            modifier = Modifier
                                .clip(PillShape)
                                .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                .clickable {
                                    totalSeconds = secPreset
                                    minText = "${secPreset / 60}"
                                    secText = "${secPreset % 60}"
                                }
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = s.secondsShort(secPreset),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Minutes & Seconds Inputs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = minText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d+$"""))) {
                                minText = input
                                val mins = input.toIntOrNull() ?: 0
                                val secs = secText.toIntOrNull() ?: 0
                                totalSeconds = (mins * 60 + secs).coerceAtLeast(5)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(s.customRestTimerMinutes, style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accentFill,
                            unfocusedBorderColor = KumaColors.BorderDefault
                        )
                    )

                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    )

                    OutlinedTextField(
                        value = secText,
                        onValueChange = { input ->
                            if (input.isEmpty() || input.matches(Regex("""^\d+$"""))) {
                                secText = input
                                val mins = minText.toIntOrNull() ?: 0
                                val secs = input.toIntOrNull() ?: 0
                                totalSeconds = (mins * 60 + secs).coerceAtLeast(5)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text(s.customRestTimerSeconds, style = MaterialTheme.typography.labelSmall) },
                        placeholder = { Text("0", style = MaterialTheme.typography.bodyLarge.copy(color = KumaColors.TextMuted)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colors.textPrimary,
                            unfocusedTextColor = colors.textPrimary,
                            focusedBorderColor = colors.accentFill,
                            unfocusedBorderColor = KumaColors.BorderDefault
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(totalSeconds.coerceIn(5, 1800))
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                )
            ) {
                Text(s.setTimerButton, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
            }
        }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseRestTimerDialog(
    exerciseName: String,
    initialExerciseSeconds: Int,
    sets: List<Triple<String, Int, Int>>, // id, setNumber, reps
    initialSetTimers: Map<String, Int?> = emptyMap(),
    onDismiss: () -> Unit,
    onConfirm: (exerciseSeconds: Int, perSetTimers: Map<String, Int?>) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current

    var selectedTab by remember { mutableStateOf(0) } // 0 = All Sets, 1 = Per Set
    val baseSec = initialExerciseSeconds.coerceAtLeast(15)
    var globalSeconds by remember { mutableStateOf(baseSec) }
    val setTimerMap = remember {
        mutableStateOf(
            sets.associate { (id, _, _) ->
                id to (initialSetTimers[id] ?: baseSec)
            }.toMutableMap()
        )
    }

    val presets = listOf(30, 45, 60, 90, 120, 150, 180, 240, 300)
    var activeEditingSetId by remember { mutableStateOf(sets.firstOrNull()?.first ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = KumaColors.SurfaceContainerLow,
        title = {
            Column {
                Text(
                    text = s.customRestTimerTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                Text(
                    text = exerciseName,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Segmented Tabs: [ Semua Set ] | [ Tiap Set / Reps ]
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .clip(RoundedCornerShape(10.dp))
                        .background(KumaColors.SurfaceContainer)
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) colors.accentFill else KumaColors.SurfaceContainer)
                            .clickable { selectedTab = 0 }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.restTimerAllSetsTab,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 0) colors.onAccentFill else colors.textPrimary
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) colors.accentFill else KumaColors.SurfaceContainer)
                            .clickable { selectedTab = 1 }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.restTimerPerSetTab,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == 1) colors.onAccentFill else colors.textPrimary
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (selectedTab == 0) {
                    // ALL SETS MODE
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(vertical = 14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = s.secondsRestLabel(globalSeconds),
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.accentText
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        presets.forEach { secPreset ->
                            val isSelected = globalSeconds == secPreset
                            Box(
                                modifier = Modifier
                                    .clip(PillShape)
                                    .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                    .clickable {
                                        globalSeconds = secPreset
                                        // update all per-set maps as well
                                        sets.forEach { (id, _, _) -> setTimerMap.value[id] = secPreset }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.secondsShort(secPreset),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) colors.onAccentFill else colors.textPrimary
                                    )
                                )
                            }
                        }
                    }
                } else {
                    // PER SET / REPS MODE
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        sets.forEach { (setId, setNum, reps) ->
                            val currentSec = setTimerMap.value[setId] ?: globalSeconds
                            val isEditingThis = activeEditingSetId == setId

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(KumaColors.SurfaceContainer)
                                    .border(
                                        width = if (isEditingThis) 1.5.dp else 0.dp,
                                        color = if (isEditingThis) colors.accentFill else KumaColors.BorderDefault,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { activeEditingSetId = setId }
                                    .padding(horizontal = 10.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(PillShape)
                                                .background(if (isEditingThis) colors.accentFill else KumaColors.SurfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$setNum",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isEditingThis) colors.onAccentFill else colors.textPrimary
                                                )
                                            )
                                        }
                                        Text(
                                            text = s.setRepsTimerLabel(setNum, reps),
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                                color = colors.textPrimary
                                            )
                                        )
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // -15s
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(KumaColors.SurfaceContainerHigh)
                                                .clickable {
                                                    val newSec = (currentSec - 15).coerceAtLeast(15)
                                                    val map = setTimerMap.value.toMutableMap()
                                                    map[setId] = newSec
                                                    setTimerMap.value = map
                                                    activeEditingSetId = setId
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }

                                        // Seconds Pill
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(if (isEditingThis) colors.accentFill.copy(alpha = 0.2f) else KumaColors.SurfaceContainerHigh)
                                                .padding(horizontal = 10.dp, vertical = 5.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Timer,
                                                    contentDescription = null,
                                                    tint = colors.accentText,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(
                                                    text = s.secondsShort(currentSec),
                                                    maxLines = 1,
                                                    softWrap = false,
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontWeight = FontWeight.ExtraBold,
                                                        color = colors.accentText
                                                    )
                                                )
                                            }
                                        }

                                        // +15s
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(KumaColors.SurfaceContainerHigh)
                                                .clickable {
                                                    val newSec = (currentSec + 15).coerceAtMost(600)
                                                    val map = setTimerMap.value.toMutableMap()
                                                    map[setId] = newSec
                                                    setTimerMap.value = map
                                                    activeEditingSetId = setId
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = null,
                                                tint = colors.textSecondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Presets bar for active editing set
                        if (activeEditingSetId.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                presets.take(6).forEach { secPreset ->
                                    val currentSec = setTimerMap.value[activeEditingSetId] ?: globalSeconds
                                    val isSelected = currentSec == secPreset
                                    Box(
                                        modifier = Modifier
                                            .clip(PillShape)
                                            .background(if (isSelected) colors.accentFill else KumaColors.SurfaceContainer)
                                            .clickable {
                                                val map = setTimerMap.value.toMutableMap()
                                                map[activeEditingSetId] = secPreset
                                                setTimerMap.value = map
                                            }
                                            .padding(horizontal = 9.dp, vertical = 5.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = s.secondsShort(secPreset),
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.onAccentFill else colors.textPrimary,
                                                fontSize = 11.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalMap = if (selectedTab == 0) {
                        sets.associate { (id, _, _) -> id to globalSeconds }
                    } else {
                        setTimerMap.value
                    }
                    onConfirm(globalSeconds.coerceIn(5, 1800), finalMap)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accentFill,
                    contentColor = colors.onAccentFill
                )
            ) {
                Text(s.setTimerButton, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(s.cancel, style = MaterialTheme.typography.labelLarge.copy(color = colors.textSecondary))
            }
        }
    )
}
