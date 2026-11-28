package com.bearbones.kumalift.features.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.ShareTokens
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.media.ShareImageExporter
import com.bearbones.kumalift.core.media.ShareSaveStatus
import com.bearbones.kumalift.core.media.openAppPermissionSettings
import com.bearbones.kumalift.core.media.rememberSharePhotoMediaLaunchers
import com.bearbones.kumalift.data.repository.PersonalRecordRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.data.repository.WorkoutRepository
import com.bearbones.kumalift.domain.model.ShareItemOffset
import com.bearbones.kumalift.domain.model.UserSettings
import com.bearbones.kumalift.domain.model.Workout
import com.bearbones.kumalift.domain.share.BuiltInShareTemplates
import com.bearbones.kumalift.domain.share.MAX_WORKOUT_SHARE_STATS
import com.bearbones.kumalift.domain.share.ShareAccentPreset
import com.bearbones.kumalift.domain.share.ShareAspectRatio
import com.bearbones.kumalift.domain.share.ShareBackgroundMode
import com.bearbones.kumalift.domain.share.ShareCardTemplateStyle
import com.bearbones.kumalift.domain.share.ShareLayoutKeys
import com.bearbones.kumalift.domain.share.WorkoutShareStatsBuilder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class ShareControlTab {
    DATA,
    BACKGROUND,
    STYLE
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutShareEditorDialog(
    workout: Workout,
    workoutRepository: WorkoutRepository?,
    settingsRepository: SettingsRepository?,
    personalRecordRepository: PersonalRecordRepository? = null,
    allowSessionSwitching: Boolean = false,
    onDismiss: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val scope = rememberCoroutineScope()
    val settings = settingsRepository?.settings?.collectAsState()?.value ?: UserSettings()
    val completedWorkouts = workoutRepository?.completedWorkouts?.collectAsState()?.value ?: emptyList()
    val allPersonalRecords = personalRecordRepository?.personalRecords?.collectAsState()?.value ?: emptyList()

    var activeWorkoutId by remember(workout.id) { mutableStateOf(workout.id) }
    val activeWorkout = remember(activeWorkoutId, workout, completedWorkouts) {
        completedWorkouts.firstOrNull { it.id == activeWorkoutId } ?: workout
    }
    var activeTab by remember { mutableStateOf(ShareControlTab.DATA) }

    var selectedPhotoPath by remember { mutableStateOf<String?>(null) }
    var decodedPhoto by remember { mutableStateOf<ImageBitmap?>(null) }
    var photoScale by remember { mutableStateOf(1f) }
    var photoOffsetX by remember { mutableStateOf(0f) }
    var photoOffsetY by remember { mutableStateOf(0f) }

    var aspectRatio by remember { mutableStateOf(ShareAspectRatio.PORTRAIT) }
    var templateStyle by remember { mutableStateOf(ShareCardTemplateStyle.HEVY_CLASSIC) }
    var noPhotoBackground by remember { mutableStateOf(ShareBackgroundMode.GRADIENT) }
    var accentPreset by remember { mutableStateOf(ShareAccentPreset.LIME) }
    var selectedStatIds by remember(settings.share.selectedWorkoutStatIds) {
        mutableStateOf(
            WorkoutShareStatsBuilder.sanitizeSelectedStatIds(settings.share.selectedWorkoutStatIds)
        )
    }
    var customItemPositions by remember(settings.share.customItemPositions) {
        mutableStateOf(settings.share.customItemPositions)
    }
    var customItemScales by remember(settings.share.customItemScales) {
        mutableStateOf(settings.share.customItemScales)
    }
    var showBrandWatermark by remember(settings.share.showBrandWatermark) {
        mutableStateOf(settings.share.showBrandWatermark)
    }
    var selectedItemKey by remember { mutableStateOf<String?>(null) }

    var isExporting by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }
    var showCameraDeniedDialog by remember { mutableStateOf(false) }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2300L)
            feedbackMessage = null
        }
    }

    val mediaLaunchers = rememberSharePhotoMediaLaunchers(
        onPhotoReady = { newPath ->
            val oldPath = selectedPhotoPath
            if (oldPath != null && oldPath != newPath) {
                ShareImageExporter.deleteTempSharePhoto(oldPath)
            }
            selectedPhotoPath = newPath
            photoScale = 1f
            photoOffsetX = 0f
            photoOffsetY = 0f
            feedbackMessage = null
        },
        onCameraPermissionDenied = { showCameraDeniedDialog = true }
    )

    LaunchedEffect(selectedPhotoPath) {
        val path = selectedPhotoPath
        decodedPhoto = if (!path.isNullOrBlank()) {
            ShareImageExporter.decodeSharePhotoBitmap(path)
        } else {
            null
        }
    }

    DisposableEffect(Unit) {
        onDispose { ShareImageExporter.deleteTempSharePhoto(selectedPhotoPath) }
    }

    val shareData = remember(activeWorkout, completedWorkouts, settings, allPersonalRecords, s) {
        WorkoutShareStatsBuilder.build(
            workout = activeWorkout,
            completedWorkouts = completedWorkouts,
            settings = settings,
            s = s,
            personalRecords = allPersonalRecords
        )
    }

    val visibleStats = remember(selectedStatIds, shareData.candidates) {
        val byId = shareData.candidates.associateBy { it.id }
        selectedStatIds.mapNotNull { byId[it] }.take(MAX_WORKOUT_SHARE_STATS)
    }

    val effectiveBgMode = if (decodedPhoto != null) ShareBackgroundMode.PHOTO else noPhotoBackground
    val template = remember(templateStyle, aspectRatio, effectiveBgMode, accentPreset) {
        BuiltInShareTemplates.workoutTemplate(
            style = templateStyle,
            aspectRatio = aspectRatio,
            backgroundMode = effectiveBgMode,
            accentPreset = accentPreset
        )
    }

    val athleteTag = remember(settings.profile.username, settings.profile.name) {
        val u = settings.profile.username.trim().trimStart('@')
        when {
            u.isNotBlank() -> "@$u"
            settings.profile.name.isNotBlank() -> settings.profile.name.trim()
            else -> ""
        }
    }

    val captureController = rememberShareCaptureController()

    fun persistShareSettings(
        nextStatIds: List<String> = selectedStatIds,
        nextPositions: Map<String, ShareItemOffset> = customItemPositions,
        nextScales: Map<String, Float> = customItemScales,
        nextWatermark: Boolean = showBrandWatermark
    ) {
        selectedStatIds = nextStatIds
        customItemPositions = nextPositions
        customItemScales = nextScales
        showBrandWatermark = nextWatermark
        settingsRepository?.update { cur ->
            cur.copy(
                share = cur.share.copy(
                    selectedWorkoutStatIds = nextStatIds,
                    customItemPositions = nextPositions,
                    customItemScales = nextScales,
                    showBrandWatermark = nextWatermark
                )
            )
        }
    }

    if (showCameraDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showCameraDeniedDialog = false },
            containerColor = KumaColors.SurfaceDim,
            title = {
                Text(
                    text = s.cameraPermissionDeniedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            },
            text = {
                Text(
                    text = s.cameraPermissionDeniedMessage,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraDeniedDialog = false
                        openAppPermissionSettings()
                    }
                ) {
                    Text(s.openSettingsAction, color = colors.accentText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraDeniedDialog = false }) {
                    Text(s.cancel, color = colors.textSecondary)
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KumaColors.CanvasLowest)
        ) {
            // 1. Minimal Huawei Health-style Top Bar: Back | Preview | Save | Share
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.CanvasLowest)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = s.back,
                        tint = colors.textPrimary
                    )
                }
                Text(
                    text = s.sharePreviewTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                IconButton(
                    enabled = !isExporting,
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val bmp = captureController.captureBitmap(transparentOnly = false)
                            val status = if (bmp != null) {
                                ShareImageExporter.saveBitmapToGallery(bmp, "kumalift_workout")
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
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = s.shareSaveToGalleryButton,
                        tint = colors.textPrimary
                    )
                }
                IconButton(
                    enabled = !isExporting,
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val bmp = captureController.captureBitmap(transparentOnly = false)
                            val ok = if (bmp != null) {
                                ShareImageExporter.shareBitmap(bmp, "kumalift_workout", s.shareWorkoutCardTitle)
                            } else {
                                false
                            }
                            if (!ok) {
                                feedbackMessage = s.shareFailedToast
                            }
                            isExporting = false
                        }
                    }
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = colors.accentText
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = s.shareExportToAppsButton,
                            tint = colors.accentText
                        )
                    }
                }
            }

            // Optional session selector strip when opened from Profile
            if (allowSessionSwitching && completedWorkouts.size > 1) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(completedWorkouts, key = { it.id }) { session ->
                        val isCur = session.id == activeWorkout.id
                        FilterChip(
                            selected = isCur,
                            onClick = { activeWorkoutId = session.id },
                            label = {
                                Text(
                                    text = session.title.ifBlank { s.workoutCompletedTitle },
                                    maxLines = 1,
                                    fontSize = 11.sp
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = colors.surfaceVariant,
                                labelColor = colors.textSecondary,
                                selectedContainerColor = colors.accentTint,
                                selectedLabelColor = colors.accentText
                            )
                        )
                    }
                }
            }

            // 2. Fixed Upper Live Preview Stage (with drag-and-drop items + empty-area photo pan/zoom)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, colors.border, RoundedCornerShape(18.dp))
                        .then(
                            if (decodedPhoto != null) {
                                Modifier.pointerInput(selectedPhotoPath) {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val nextScale = (photoScale * zoom).coerceIn(1f, 4f)
                                        photoScale = nextScale
                                        val maxPan = (nextScale - 1f) * 480f
                                        photoOffsetX = (photoOffsetX + pan.x * 1.6f).coerceIn(-maxPan, maxPan)
                                        photoOffsetY = (photoOffsetY + pan.y * 1.6f).coerceIn(-maxPan, maxPan)
                                    }
                                }
                            } else {
                                Modifier
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    SharePreviewStage(
                        aspectRatio = aspectRatio,
                        controller = captureController,
                        maxPreviewHeightDp = 330f
                    ) { isTransparentExport ->
                        WorkoutShareCardContent(
                            data = shareData,
                            visibleStats = visibleStats,
                            template = template,
                            templateStyle = templateStyle,
                            backgroundMode = effectiveBgMode,
                            accentPreset = accentPreset,
                            photoBitmap = decodedPhoto,
                            photoScale = photoScale,
                            photoOffsetX = photoOffsetX,
                            photoOffsetY = photoOffsetY,
                            athleteTag = athleteTag,
                            showBrandWatermark = showBrandWatermark,
                            itemPositions = customItemPositions,
                            itemScales = customItemScales,
                            activeDragKey = null,
                            selectedItemKey = selectedItemKey,
                            isTransparentExport = isTransparentExport,
                            isExportingRender = captureController.isExportingRender,
                            onSelectItem = { selectedItemKey = it },
                            onDragItem = { key, nextPos ->
                                customItemPositions = customItemPositions.toMutableMap().apply {
                                    put(key, nextPos)
                                }
                            },
                            onDragEnd = { key, finalPos ->
                                val nextMap = customItemPositions.toMutableMap().apply {
                                    put(key, finalPos)
                                }
                                persistShareSettings(nextPositions = nextMap)
                            }
                        )
                    }
                }

                // Inline auto-dismissing toast banner inside preview stage
                feedbackMessage?.let { msg ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 10.dp)
                            .clip(PillShape)
                            .background(KumaColors.SurfaceDim.copy(alpha = 0.94f))
                            .border(1.dp, colors.border, PillShape)
                            .clickable { feedbackMessage = null }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }
            }

            // Hint OR active item size/reset bar above sheet
            val activeKey = selectedItemKey
            if (activeKey != null) {
                val activeLabel = when (activeKey) {
                    ShareLayoutKeys.WORKOUT_TITLE -> s.shareItemTitleLabel
                    ShareLayoutKeys.BRAND_MARK -> s.shareItemWatermarkLabel
                    else -> shareData.candidates.firstOrNull { it.id == activeKey }?.label ?: activeKey
                }
                val currentScale = (customItemScales[activeKey] ?: 1f).coerceIn(0.6f, 2.0f)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$activeLabel · ${s.shareItemSizeLabel} ${(currentScale * 100).roundToInt()}%",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        ),
                        modifier = Modifier.width(126.dp)
                    )
                    Slider(
                        value = currentScale,
                        onValueChange = { nextScale ->
                            customItemScales = customItemScales.toMutableMap().apply {
                                put(activeKey, nextScale.coerceIn(0.6f, 2.0f))
                            }
                        },
                        onValueChangeFinished = {
                            persistShareSettings(nextScales = customItemScales)
                        },
                        valueRange = 0.6f..2.0f,
                        modifier = Modifier.weight(1f).height(26.dp),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.accentFill,
                            activeTrackColor = colors.accentFill,
                            inactiveTrackColor = colors.surfaceVariant
                        )
                    )
                    TextButton(
                        onClick = {
                            val nextScales = customItemScales.toMutableMap().apply { remove(activeKey) }
                            persistShareSettings(nextScales = nextScales)
                        }
                    ) {
                        Text("100%", color = colors.textSecondary, fontSize = 11.sp)
                    }
                    IconButton(
                        onClick = { selectedItemKey = null },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = s.done,
                            tint = colors.accentText,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.shareItemTapHint,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary, fontSize = 11.sp)
                    )
                    if (customItemPositions.isNotEmpty() || customItemScales.isNotEmpty()) {
                        TextButton(
                            onClick = {
                                selectedItemKey = null
                                persistShareSettings(nextPositions = emptyMap(), nextScales = emptyMap())
                                feedbackMessage = null
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = colors.accentText
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(s.shareResetLayoutAction, color = colors.accentText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. Lower Rounded Control Deck with 3 Huawei Health-style Sub-Tabs: Data | Background | Style
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = KumaColors.SurfaceDim,
                tonalElevation = 0.dp,
                border = BorderStroke(1.dp, colors.border)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    // 3 Sub-Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ShareControlTab.DATA to s.shareTabData,
                            ShareControlTab.BACKGROUND to s.shareTabBackground,
                            ShareControlTab.STYLE to s.shareTabStyle
                        ).forEach { (tab, label) ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(PillShape)
                                    .background(if (isSelected) colors.surfaceVariant else KumaColors.CanvasLowest)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) colors.accentText.copy(alpha = 0.5f) else colors.border,
                                        shape = PillShape
                                    )
                                    .clickable { activeTab = tab }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.accentText else colors.textSecondary
                                    )
                                )
                            }
                        }
                    }

                    // Tab Body (bounded height, scrollable internally without shifting Preview)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        when (activeTab) {
                            ShareControlTab.DATA -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionTitle(s.shareStatsSectionTitle)
                                    Text(
                                        text = s.shareStatsSelectedCount(selectedStatIds.size, MAX_WORKOUT_SHARE_STATS),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            color = colors.accentText,
                                            fontWeight = FontWeight.Bold
                                        )
                                    )
                                }

                                // 4-column circular icon grid (matches video's Workout data picker!)
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    maxItemsInEachRow = 4,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    val selectedSet = selectedStatIds.toSet()
                                    shareData.candidates.forEach { stat ->
                                        val isActive = selectedSet.contains(stat.id)
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable {
                                                    if (!isActive && selectedStatIds.size >= MAX_WORKOUT_SHARE_STATS) {
                                                        feedbackMessage = s.shareMaxStatsReachedNotice(MAX_WORKOUT_SHARE_STATS)
                                                    } else {
                                                        feedbackMessage = null
                                                        val nextIds = WorkoutShareStatsBuilder.toggleStatSelection(
                                                            currentIds = selectedStatIds,
                                                            targetId = stat.id
                                                        )
                                                        persistShareSettings(nextStatIds = nextIds)
                                                    }
                                                }
                                                .padding(vertical = 2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isActive) colors.accentFill else colors.surfaceVariant)
                                                    .border(
                                                        width = 1.dp,
                                                        color = if (isActive) colors.accentFill else colors.border,
                                                        shape = CircleShape
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = stat.iconKey.toVector(),
                                                    contentDescription = stat.label,
                                                    tint = if (isActive) colors.onAccentFill else colors.textSecondary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Text(
                                                text = stat.label,
                                                maxLines = 2,
                                                textAlign = TextAlign.Center,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontSize = 10.sp,
                                                    lineHeight = 12.sp,
                                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (isActive) colors.textPrimary else colors.textSecondary
                                                )
                                            )
                                        }
                                    }
                                }

                                // Brand watermark toggle + Transparent template download
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.shareBrandWatermarkToggle,
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textPrimary)
                                    )
                                    Switch(
                                        checked = showBrandWatermark,
                                        onCheckedChange = { persistShareSettings(nextWatermark = it) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = colors.onAccentFill,
                                            checkedTrackColor = colors.accentFill
                                        )
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (isExporting) return@OutlinedButton
                                        scope.launch {
                                            isExporting = true
                                            val bmp = captureController.captureBitmap(transparentOnly = true)
                                            val status = if (bmp != null) {
                                                ShareImageExporter.saveBitmapToGallery(bmp, "kumalift_template")
                                            } else {
                                                ShareSaveStatus.ERROR_WRITE
                                            }
                                            feedbackMessage = when (status) {
                                                ShareSaveStatus.SUCCESS -> s.shareTemplateSavedToast
                                                ShareSaveStatus.ERROR_UNSUPPORTED -> s.shareUnsupportedOsToast
                                                ShareSaveStatus.ERROR_WRITE -> s.shareFailedToast
                                            }
                                            isExporting = false
                                        }
                                    },
                                    enabled = !isExporting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, colors.accentText.copy(alpha = 0.55f))
                                ) {
                                    Icon(Icons.Default.LayersClear, contentDescription = null, modifier = Modifier.size(16.dp), tint = colors.accentText)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = s.shareDownloadBlankTemplateButton,
                                        color = colors.accentText,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            ShareControlTab.BACKGROUND -> {
                                // Visual tile grid like video's Background tab: Camera | Gallery | Solid | Gradient
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    BgOptionTile(
                                        label = s.takePhotoAction,
                                        icon = Icons.Default.CameraAlt,
                                        isSelected = false,
                                        modifier = Modifier.weight(1f),
                                        onClick = { mediaLaunchers.launchCamera() }
                                    )
                                    BgOptionTile(
                                        label = s.chooseFromGalleryAction,
                                        icon = Icons.Default.PhotoLibrary,
                                        isSelected = decodedPhoto != null,
                                        previewBitmap = decodedPhoto,
                                        modifier = Modifier.weight(1f),
                                        onClick = { mediaLaunchers.launchGallery() }
                                    )
                                    BgOptionTile(
                                        label = s.shareBackgroundSolid,
                                        icon = Icons.Default.LayersClear,
                                        isSelected = decodedPhoto == null && noPhotoBackground == ShareBackgroundMode.SOLID_COLOR,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            ShareImageExporter.deleteTempSharePhoto(selectedPhotoPath)
                                            selectedPhotoPath = null
                                            decodedPhoto = null
                                            noPhotoBackground = ShareBackgroundMode.SOLID_COLOR
                                        }
                                    )
                                    BgOptionTile(
                                        label = s.shareBackgroundGradient,
                                        icon = Icons.Default.Gradient,
                                        isSelected = decodedPhoto == null && noPhotoBackground == ShareBackgroundMode.GRADIENT,
                                        modifier = Modifier.weight(1f),
                                        useGradientPreview = true,
                                        onClick = {
                                            ShareImageExporter.deleteTempSharePhoto(selectedPhotoPath)
                                            selectedPhotoPath = null
                                            decodedPhoto = null
                                            noPhotoBackground = ShareBackgroundMode.GRADIENT
                                        }
                                    )
                                }

                                if (decodedPhoto != null) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = s.sharePanZoomHint,
                                            modifier = Modifier.weight(1f),
                                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                        )
                                        TextButton(
                                            onClick = {
                                                photoScale = 1f
                                                photoOffsetX = 0f
                                                photoOffsetY = 0f
                                            }
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp), tint = colors.accentText)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(s.shareResetPhotoPosition, color = colors.accentText, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }

                            ShareControlTab.STYLE -> {
                                SectionTitle(s.shareTemplateStyleSection)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    listOf(
                                        ShareCardTemplateStyle.HEVY_CLASSIC to s.shareTemplateHevyClassic,
                                        ShareCardTemplateStyle.BOLD_CENTER to s.shareTemplateBoldCenter,
                                        ShareCardTemplateStyle.MINIMAL_CORNER to s.shareTemplateMinimalCorner
                                    ).forEach { (styleOpt, label) ->
                                        val isSelected = templateStyle == styleOpt
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(if (isSelected) colors.accentTint else colors.surfaceVariant)
                                                .border(
                                                    width = if (isSelected) 1.5.dp else 1.dp,
                                                    color = if (isSelected) colors.accentText else colors.border,
                                                    shape = RoundedCornerShape(12.dp)
                                                )
                                                .clickable {
                                                    templateStyle = styleOpt
                                                    // Apply template's preset item placements smoothly
                                                    persistShareSettings(nextPositions = emptyMap())
                                                }
                                                .padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) colors.accentText else colors.textPrimary
                                                )
                                            )
                                        }
                                    }
                                }

                                SectionTitle(s.shareAspectRatioSection)
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        ShareAspectRatio.SQUARE to s.shareAspectSquare,
                                        ShareAspectRatio.PORTRAIT to s.shareAspectPortrait,
                                        ShareAspectRatio.STORY to s.shareAspectStory
                                    ).forEach { (ratio, label) ->
                                        ShareSelectorChip(
                                            selected = aspectRatio == ratio,
                                            label = label,
                                            onClick = { aspectRatio = ratio }
                                        )
                                    }
                                }

                                SectionTitle(s.shareAccentColorSection)
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    ShareAccentPreset.entries.forEach { preset ->
                                        val swatch = ShareTokens.resolveAccentColor(preset)
                                        val isSelected = accentPreset == preset
                                        Box(
                                            modifier = Modifier
                                                .size(30.dp)
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
                                                    modifier = Modifier.size(16.dp)
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
    }
}

@Composable
private fun BgOptionTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    previewBitmap: ImageBitmap? = null,
    useGradientPreview: Boolean = false,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.85f)
                .clip(RoundedCornerShape(12.dp))
                .then(
                    if (useGradientPreview) {
                        Modifier.background(
                            Brush.verticalGradient(listOf(colors.surfaceVariant, KumaColors.CanvasLowest))
                        )
                    } else {
                        Modifier.background(colors.surfaceVariant)
                    }
                )
                .border(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) colors.accentText else colors.border,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap,
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isSelected) colors.accentText else colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(5.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(colors.accentFill),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = colors.onAccentFill,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                color = if (isSelected) colors.accentText else colors.textSecondary,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}

@Composable
internal fun SectionTitle(text: String) {
    val colors = LocalKumaColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(
            color = colors.textPrimary,
            fontWeight = FontWeight.Bold
        )
    )
}

@Composable
internal fun ShareSelectorChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalKumaColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = colors.surfaceVariant,
            labelColor = colors.textPrimary,
            selectedContainerColor = colors.accentTint,
            selectedLabelColor = colors.accentText
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = colors.border,
            selectedBorderColor = colors.accentText
        )
    )
}
