package com.bearbones.kumalift.features.share

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.ShareTokens
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.WorkoutPlanShareCodec
import com.bearbones.kumalift.domain.model.localizedTitle
import com.bearbones.kumalift.core.media.ShareImageExporter
import com.bearbones.kumalift.core.media.ShareSaveStatus
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.RoutineBlueprint
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.share.ProgramShareBuilder
import com.bearbones.kumalift.domain.share.ProgramShareScope
import com.bearbones.kumalift.domain.share.ProgramShareTemplateStyle
import com.bearbones.kumalift.domain.share.ShareAccentPreset
import com.bearbones.kumalift.domain.share.ShareAspectRatio
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramShareEditorDialog(
    programTitle: String,
    routines: List<RoutineBlueprint>,
    initialScope: ProgramShareScope = ProgramShareScope.WHOLE_PROGRAM,
    initialRoutineId: String? = null,
    weightUnit: WeightUnit = WeightUnit.KG,
    language: AppLanguage = AppLanguage.SYSTEM,
    athleteTag: String = "",
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val scope = rememberCoroutineScope()

    var shareScope by remember(initialScope) { mutableStateOf(initialScope) }
    var selectedRoutineId by remember(initialRoutineId, routines) {
        mutableStateOf(initialRoutineId ?: routines.firstOrNull()?.id)
    }
    var selectedExerciseKeys by remember(routines) {
        mutableStateOf(ProgramShareBuilder.allExerciseKeysForRoutines(routines))
    }
    var aspectRatio by remember { mutableStateOf(ShareAspectRatio.TALL_LIST) }
    var templateStyle by remember { mutableStateOf(ProgramShareTemplateStyle.DARK_KUMA) }
    var accentPreset by remember { mutableStateOf(ShareAccentPreset.LIME) }
    var showTargets by remember { mutableStateOf(false) }
    var showMuscleEquipment by remember { mutableStateOf(false) }

    var thumbnailBitmaps by remember { mutableStateOf<Map<String, ImageBitmap>>(emptyMap()) }
    var isExporting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val allAssetPaths = remember(routines) {
        routines.flatMap { r -> r.effectiveConfigs.mapNotNull { it.exercise.thumbnailUrl } }.distinct()
    }
    LaunchedEffect(allAssetPaths) {
        thumbnailBitmaps = ShareImageExporter.loadExerciseThumbnailBitmaps(allAssetPaths)
    }

    val renderData = remember(
        programTitle,
        routines,
        shareScope,
        selectedRoutineId,
        selectedExerciseKeys,
        showTargets,
        weightUnit,
        language,
        s
    ) {
        ProgramShareBuilder.build(
            programTitle = programTitle,
            routines = routines,
            scope = shareScope,
            selectedRoutineId = selectedRoutineId,
            selectedExerciseKeys = selectedExerciseKeys,
            showTargets = showTargets,
            weightUnit = weightUnit,
            language = language,
            s = s
        )
    }

    val scopedPlanPackage = remember(
        programTitle,
        routines,
        shareScope,
        selectedRoutineId,
        selectedExerciseKeys,
        athleteTag,
        s
    ) {
        ProgramShareBuilder.buildScopedPlanPackage(
            programTitle = programTitle,
            routines = routines,
            scope = shareScope,
            selectedRoutineId = selectedRoutineId,
            selectedExerciseKeys = selectedExerciseKeys,
            creatorTag = athleteTag,
            s = s
        )
    }

    val liveQrPayload = remember(scopedPlanPackage) {
        WorkoutPlanShareCodec.encodeQrCompactPlanCode(scopedPlanPackage)
    }

    val captureController = rememberShareCaptureController()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .heightIn(max = 760.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = s.shareProgramImageTitle,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = colors.textPrimary
                        )
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = s.close, tint = colors.textSecondary)
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live Scrollable Preview Stage (export renders full height regardless of scroll)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                            .background(KumaColors.CanvasLowest)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (renderData.columns.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = s.shareSelectedExercisesEmpty,
                                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                                )
                            }
                        } else {
                            SharePreviewStage(
                                aspectRatio = aspectRatio,
                                controller = captureController
                            ) { _ ->
                                ProgramShareCardContent(
                                    data = renderData,
                                    aspectRatio = aspectRatio,
                                    templateStyle = templateStyle,
                                    accentPreset = accentPreset,
                                    showMuscleEquipment = showMuscleEquipment,
                                    thumbnailBitmaps = thumbnailBitmaps,
                                    athleteTag = athleteTag
                                )
                            }
                        }
                    }

                    if (captureController.lastCalculatedDimensions.isCapped && renderData.columns.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.warningTint)
                                .padding(10.dp)
                        ) {
                            Text(
                                text = s.shareTallBudgetWarning,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.warning,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }

                    // Dynamic Program & Schedule Barcode directly below image preview & above What to Include
                    if (renderData.columns.isNotEmpty()) {
                        ProgramQrBarcodeSection(
                            qrPayload = liveQrPayload,
                            summaryLabel = "${renderData.headerTitle} · ${renderData.subtitle}",
                            filePrefix = "kumalift_program_qr",
                            onFeedback = { feedbackMessage = it }
                        )
                    }

                    // 1. Scope Selector
                    SectionTitle(s.shareScopeSection)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShareSelectorChip(
                            selected = shareScope == ProgramShareScope.WHOLE_PROGRAM,
                            label = s.shareScopeWholeProgram,
                            onClick = { shareScope = ProgramShareScope.WHOLE_PROGRAM }
                        )
                        ShareSelectorChip(
                            selected = shareScope == ProgramShareScope.SINGLE_ROUTINE,
                            label = s.shareScopeSingleRoutine,
                            onClick = { shareScope = ProgramShareScope.SINGLE_ROUTINE }
                        )
                        ShareSelectorChip(
                            selected = shareScope == ProgramShareScope.CUSTOM_SELECTION,
                            label = s.shareScopeCustomSelection,
                            onClick = { shareScope = ProgramShareScope.CUSTOM_SELECTION }
                        )
                    }

                    if (shareScope == ProgramShareScope.SINGLE_ROUTINE && routines.isNotEmpty()) {
                        SectionTitle(s.shareSelectRoutineLabel)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            routines.forEach { r ->
                                ShareSelectorChip(
                                    selected = r.id == selectedRoutineId,
                                    label = r.localizedTitle(s),
                                    onClick = { selectedRoutineId = r.id }
                                )
                            }
                        }
                    }

                    if (shareScope == ProgramShareScope.CUSTOM_SELECTION && routines.isNotEmpty()) {
                        Text(
                            text = s.shareSelectExercisesHint,
                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            routines.forEach { routine ->
                                val configs = routine.effectiveConfigs
                                if (configs.isNotEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(KumaColors.SurfaceContainerLow)
                                            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = routine.localizedTitle(s),
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.accentText
                                            )
                                        )
                                        configs.forEachIndexed { idx, cfg ->
                                            val itemKey = ProgramShareBuilder.buildExerciseItemKey(routine.id, idx, cfg.exercise.id)
                                            val checked = selectedExerciseKeys.contains(itemKey)
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        selectedExerciseKeys = if (checked) {
                                                            selectedExerciseKeys - itemKey
                                                        } else {
                                                            selectedExerciseKeys + itemKey
                                                        }
                                                    }
                                                    .padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Checkbox(
                                                    checked = checked,
                                                    onCheckedChange = { next ->
                                                        selectedExerciseKeys = if (next) {
                                                            selectedExerciseKeys + itemKey
                                                        } else {
                                                            selectedExerciseKeys - itemKey
                                                        }
                                                    },
                                                    colors = CheckboxDefaults.colors(
                                                        checkedColor = colors.accentFill,
                                                        checkmarkColor = colors.onAccentFill,
                                                        uncheckedColor = colors.textSecondary
                                                    )
                                                )
                                                Text(
                                                    text = cfg.exercise.name,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = colors.textPrimary,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 2. Aspect Ratio & Template Style
                    SectionTitle(s.shareAspectRatioSection)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            ShareAspectRatio.TALL_LIST to s.shareAspectTallList,
                            ShareAspectRatio.SQUARE to s.shareAspectSquare,
                            ShareAspectRatio.PORTRAIT to s.shareAspectPortrait
                        ).forEach { (ratio, label) ->
                            ShareSelectorChip(
                                selected = aspectRatio == ratio,
                                label = label,
                                onClick = { aspectRatio = ratio }
                            )
                        }
                    }

                    SectionTitle(s.shareTemplateStyleSection)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ShareSelectorChip(
                            selected = templateStyle == ProgramShareTemplateStyle.DARK_KUMA,
                            label = s.shareTemplateDarkKuma,
                            onClick = { templateStyle = ProgramShareTemplateStyle.DARK_KUMA }
                        )
                        ShareSelectorChip(
                            selected = templateStyle == ProgramShareTemplateStyle.CLEAN_LIGHT,
                            label = s.shareTemplateCleanLight,
                            onClick = { templateStyle = ProgramShareTemplateStyle.CLEAN_LIGHT }
                        )
                    }

                    SectionTitle(s.shareAccentColorSection)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ShareAccentPreset.entries.forEach { preset ->
                            val swatch = ShareTokens.resolveAccentColor(preset)
                            val isSelected = accentPreset == preset
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(swatch)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) colors.textPrimary else colors.border,
                                        shape = CircleShape
                                    )
                                    .clickable { accentPreset = preset },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ShareTokens.resolveOnAccentColor(preset),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 3. Optional Row Toggles
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.shareShowMuscleEquipmentToggle,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary)
                        )
                        Switch(
                            checked = showMuscleEquipment,
                            onCheckedChange = { showMuscleEquipment = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccentFill,
                                checkedTrackColor = colors.accentFill
                            )
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = s.shareShowWeightRpeToggle,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary)
                        )
                        Switch(
                            checked = showTargets,
                            onCheckedChange = { showTargets = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAccentFill,
                                checkedTrackColor = colors.accentFill
                            )
                        )
                    }

                    feedbackMessage?.let { msg ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(colors.accentTint)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = msg,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = colors.accentText,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(colors.border))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isExporting || renderData.columns.isEmpty()) return@Button
                            scope.launch {
                                isExporting = true
                                val bmp = captureController.captureBitmap(transparentOnly = false)
                                val status = if (bmp != null) {
                                    ShareImageExporter.saveBitmapToGallery(bmp, "kumalift_program")
                                } else {
                                    ShareSaveStatus.ERROR_WRITE
                                }
                                feedbackMessage = when (status) {
                                    ShareSaveStatus.SUCCESS -> s.shareSavedToGalleryToast
                                    ShareSaveStatus.ERROR_UNSUPPORTED -> s.shareUnsupportedOsToast
                                    ShareSaveStatus.ERROR_WRITE -> s.shareFailedToast
                                }
                                isExporting = false
                            }
                        },
                        enabled = !isExporting && renderData.columns.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        )
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.shareDownloadPngButton,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
