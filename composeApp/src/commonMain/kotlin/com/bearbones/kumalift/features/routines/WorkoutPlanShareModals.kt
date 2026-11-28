package com.bearbones.kumalift.features.routines

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.data.rememberDataImportLauncher
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.data.repository.ExerciseRepository
import com.bearbones.kumalift.data.repository.RoutineRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.TrainingProgramRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.ImportedPlanResult
import com.bearbones.kumalift.domain.model.SharedWorkoutPlanPackage
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.WorkoutPlanShareCodec
import com.bearbones.kumalift.domain.model.WorkoutPlanShareImporter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShareWorkoutPlanModal(
    pkg: SharedWorkoutPlanPackage,
    weightUnit: WeightUnit = WeightUnit.KG,
    language: AppLanguage = AppLanguage.SYSTEM,
    onShareAsImage: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    onStatusMessage: (String) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val qrPayload = remember(pkg) { WorkoutPlanShareCodec.encodeQrCompactPlanCode(pkg) }
    var showGeneratedBarcode by remember(pkg) { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.sharePlanModalTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
                            tint = colors.textSecondary
                        )
                    }
                }

                // Authentic KumaLift Preview Card
                KumaCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.accentFill.copy(alpha = 0.35f), RoundedCornerShape(16.dp)),
                    backgroundColor = KumaColors.SurfaceContainer,
                    cornerRadius = 16.dp
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            KumaBadge(text = s.kumasSharedBrandingTag, isPr = true)
                            KumaBadge(text = pkg.goal.label(s), isPr = false)
                        }

                        Text(
                            text = pkg.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color = colors.textPrimary
                            )
                        )

                        if (pkg.creatorDisplayLabel.isNotBlank()) {
                            Text(
                                text = s.sharedByCreatorBadge(pkg.creatorDisplayLabel),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }

                        val totalExercises = pkg.routines.sumOf { it.exercises.size }
                        Text(
                            text = s.importPreviewMetaSummary(pkg.routines.size, totalExercises, pkg.targetDaysPerWeek),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                        )

                        SharedPlanStructureBody(
                            pkg = pkg,
                            weightUnit = weightUnit,
                            language = language,
                            maxHeightDp = 240
                        )
                    }
                }

                // Actions: Image PNG + Generate Barcode (Copy Code / Summary / Share removed)
                if (onShareAsImage != null) {
                    Button(
                        onClick = onShareAsImage,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.shareProgramImageAction,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showGeneratedBarcode = !showGeneratedBarcode },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCode2,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.accentText
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.generateBarcodeAction,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }

                if (showGeneratedBarcode) {
                    com.bearbones.kumalift.features.share.ProgramQrBarcodeSection(
                        qrPayload = qrPayload,
                        summaryLabel = pkg.title,
                        filePrefix = "kumalift_plan_qr",
                        onFeedback = onStatusMessage
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImportWorkoutPlanModal(
    initialRawInput: String? = null,
    exerciseRepository: ExerciseRepository,
    routineRepository: RoutineRepository,
    trainingProgramRepository: TrainingProgramRepository,
    settingsRepository: SettingsRepository?,
    onImported: (ImportedPlanResult) -> Unit,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val settingsState = settingsRepository?.settings?.collectAsState()
    val weightUnit = settingsState?.value?.units?.weightUnit ?: WeightUnit.KG
    val language = settingsState?.value?.appearance?.language ?: AppLanguage.SYSTEM

    var rawInput by remember(initialRawInput) { mutableStateOf(initialRawInput.orEmpty()) }
    val decodedPkg = remember(rawInput) { WorkoutPlanShareCodec.decodePlanInput(rawInput) }

    val importFileLauncher = rememberDataImportLauncher { content ->
        if (!content.isNullOrBlank()) {
            rawInput = content
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumaKeyboardAvoidance()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.importPlanModalTitle,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
                            tint = colors.textSecondary
                        )
                    }
                }

                OutlinedTextField(
                    value = rawInput,
                    onValueChange = { rawInput = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 84.dp, max = 140.dp),
                    placeholder = {
                        Text(
                            text = s.importPlanPasteHint,
                            color = colors.textSecondary
                        )
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accentFill,
                        unfocusedBorderColor = KumaColors.BorderDefault,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary
                    )
                )

                OutlinedButton(
                    onClick = { importFileLauncher() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FileOpen,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = s.importPlanFromFileAction,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (rawInput.isNotBlank() && decodedPkg == null) {
                    Text(
                        text = s.importPlanInvalidPayload,
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.error)
                    )
                }

                if (decodedPkg != null) {
                    KumaCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, colors.accentFill.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                        backgroundColor = KumaColors.SurfaceContainer,
                        cornerRadius = 16.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = s.importPreviewHeaderTitle,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentText,
                                    fontWeight = FontWeight.Bold
                                )
                            )

                            Text(
                                text = decodedPkg.title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.textPrimary
                                )
                            )

                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                KumaBadge(text = decodedPkg.goal.label(s), isPr = false)
                                if (decodedPkg.creatorDisplayLabel.isNotBlank()) {
                                    KumaBadge(
                                        text = s.sharedByCreatorBadge(decodedPkg.creatorDisplayLabel),
                                        isPr = true
                                    )
                                }
                            }

                            if (decodedPkg.description.isNotBlank()) {
                                Text(
                                    text = decodedPkg.description,
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }

                            val totalEx = decodedPkg.routines.sumOf { it.exercises.size }
                            Text(
                                text = s.importPreviewMetaSummary(decodedPkg.routines.size, totalEx, decodedPkg.targetDaysPerWeek),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )

                            SharedPlanStructureBody(
                                pkg = decodedPkg,
                                weightUnit = weightUnit,
                                language = language,
                                maxHeightDp = 280
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Verified,
                                    contentDescription = null,
                                    tint = colors.accentText,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = s.sharedPlanCopyNote,
                                    style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val result = WorkoutPlanShareImporter.importAsPersonalCopy(
                                pkg = decodedPkg,
                                exerciseRepository = exerciseRepository,
                                routineRepository = routineRepository,
                                trainingProgramRepository = trainingProgramRepository,
                                settingsRepository = settingsRepository
                            )
                            onImported(result)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        )
                    ) {
                        Text(
                            text = s.importSavePersonalCopyButton,
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SharedPlanStructureBody(
    pkg: SharedWorkoutPlanPackage,
    weightUnit: WeightUnit,
    language: AppLanguage,
    maxHeightDp: Int
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = maxHeightDp.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        pkg.routines.forEach { routine ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(KumaColors.SurfaceDim)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = routine.title,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "~${routine.estimatedDurationMinutes} ${s.minAbbrev}",
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                    )
                }

                if (routine.scheduledDays.isNotEmpty()) {
                    val days = routine.scheduledDays.mapNotNull { d -> s.dayNamesShort.getOrNull(d - 1) }.joinToString(" · ")
                    if (days.isNotBlank()) {
                        Text(
                            text = days,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.accentText,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                routine.exercises.forEach { ex ->
                    val loadSuffix = ex.targetWeightKg?.takeIf { it > 0.0 }?.let { kg ->
                        val disp = NumberFormatUtils.formatDecimal(
                            UnitConverters.toDisplayWeight(kg, weightUnit),
                            language,
                            decimals = 1,
                            trimTrailingZeros = true
                        )
                        " · $disp $unitLabel"
                    }.orEmpty()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ex.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = colors.textPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            )
                            Text(
                                text = "${ex.primaryMuscle.label(s)} · ${s.setsRepsPreviewChip(ex.targetSets, ex.targetReps)}$loadSuffix",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.textSecondary,
                                    fontSize = 11.sp
                                )
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = s.aiPreviewRestSeconds(ex.restTimerSeconds),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontSize = 11.sp
                            )
                        )
                    }
                }

                if (routine.instructions.isNotBlank()) {
                    Text(
                        text = routine.instructions,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
