package com.bearbones.kumalift.features.logger

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val initialStr = if (initialWeight > 0.0) {
        if (initialWeight == initialWeight.toLong().toDouble()) initialWeight.toLong().toString() else initialWeight.toString()
    } else ""
    var textValue by remember { mutableStateOf(initialStr) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showPlateCalcModal by remember { mutableStateOf(false) }

    if (showPlateCalcModal) {
        PlateCalculatorDialog(
            initialWeightKg = textValue.toDoubleOrNull() ?: initialWeight,
            unit = unit,
            onDismiss = { showPlateCalcModal = false },
            onConfirm = { calculatedWeight ->
                textValue = calculatedWeight.toString().removeSuffix(".0")
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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
                onClick = {
                    val weight = NumberFormatUtils.parseDecimalInput(textValue) ?: 0.0
                    onConfirm(weight)
                },
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
    var textValue by remember { mutableStateOf(if (initialReps > 0) initialReps.toString() else "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                onClick = {
                    val reps = textValue.toIntOrNull() ?: 0
                    onConfirm(reps)
                },
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
                    Text(s.clearValueButton, style = MaterialTheme.typography.labelLarge.copy(color = KumaColors.ErrorRed))
                }
                Button(
                    onClick = {
                        val parsed = textValue.toDoubleOrNull() ?: selectedRpe
                        val finalRpe = parsed?.coerceIn(1.0, 10.0)
                        onConfirm(finalRpe)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(s.save, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
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
                    Text(s.clearValueButton, style = MaterialTheme.typography.labelLarge.copy(color = KumaColors.ErrorRed))
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
                    Text(s.save, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                }
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
fun CustomRestTimerDialog(
    initialSeconds: Int,
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
                text = s.customRestTimerTitle,
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
