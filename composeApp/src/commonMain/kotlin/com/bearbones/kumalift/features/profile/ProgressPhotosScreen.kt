package com.bearbones.kumalift.features.profile

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bearbones.kumalift.core.designsystem.KumaBadge
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.KumaDeleteConfirmationDialog
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.kumaKeyboardAvoidance
import com.bearbones.kumalift.core.localization.AppStrings
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.media.ProgressPhotoImage
import com.bearbones.kumalift.core.media.deleteProgressPhotoFile
import com.bearbones.kumalift.core.media.openAppPermissionSettings
import com.bearbones.kumalift.core.media.rememberProgressPhotoMediaLaunchers
import com.bearbones.kumalift.core.media.shareProgressPhotoWithSummary
import com.bearbones.kumalift.core.navigation.KumaBackHandler
import com.bearbones.kumalift.core.util.IdGenerator
import com.bearbones.kumalift.core.util.NumberFormatUtils
import com.bearbones.kumalift.core.util.UnitConverters
import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.data.repository.ProgressPhotoRepository
import com.bearbones.kumalift.data.repository.SettingsRepository
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.FitnessHistoryOrganizer
import com.bearbones.kumalift.domain.model.ProgressPhotoEntry
import com.bearbones.kumalift.domain.model.WeightUnit
import com.bearbones.kumalift.domain.model.groupProgressPhotosByMonth
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class ProgressPhotosViewTab {
    TIMELINE,
    COMPARE
}

@Composable
fun ProgressPhotosScreen(
    progressPhotoRepository: ProgressPhotoRepository,
    settingsRepository: SettingsRepository,
    onNavigateBack: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val photos by progressPhotoRepository.photos.collectAsState()
    val settings by settingsRepository.settings.collectAsState()

    val weightUnit = settings.units.weightUnit
    val language = settings.appearance.language

    var activeTab by remember { mutableStateOf(ProgressPhotosViewTab.TIMELINE) }
    var pendingNewPhotoPath by remember { mutableStateOf<String?>(null) }
    var selectedDetailPhoto by remember { mutableStateOf<ProgressPhotoEntry?>(null) }
    var photoPendingDelete by remember { mutableStateOf<ProgressPhotoEntry?>(null) }
    var showCameraPermissionDialog by remember { mutableStateOf(false) }

    val mediaLaunchers = rememberProgressPhotoMediaLaunchers(
        onPhotoReady = { savedPath -> pendingNewPhotoPath = savedPath },
        onCameraPermissionDenied = { showCameraPermissionDialog = true }
    )

    KumaBackHandler(enabled = true) {
        when {
            photoPendingDelete != null -> photoPendingDelete = null
            selectedDetailPhoto != null -> selectedDetailPhoto = null
            pendingNewPhotoPath != null -> {
                deleteProgressPhotoFile(pendingNewPhotoPath)
                pendingNewPhotoPath = null
            }
            showCameraPermissionDialog -> showCameraPermissionDialog = false
            else -> onNavigateBack()
        }
    }

    val monthlyGroups = remember(photos) { groupProgressPhotosByMonth(photos) }
    val expandedMonths = remember { mutableStateMapOf<String, Boolean>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
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
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = s.progressPhotosTitle,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Privacy Indicator Banner
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(KumaColors.SurfaceContainerLow)
                            .border(1.dp, KumaColors.BorderDefault, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = colors.accentText,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = s.progressPhotosPrivacyBanner,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = colors.textSecondary,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }

                // Capture & Gallery Actions
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { mediaLaunchers.launchCamera() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.accentFill,
                                contentColor = colors.onAccentFill
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.takePhotoAction,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        OutlinedButton(
                            onClick = { mediaLaunchers.launchGallery() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = s.chooseFromGalleryAction,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // View Switcher (Timeline / Before & After)
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PillShape)
                            .background(KumaColors.SurfaceContainer)
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        ProgressTabPill(
                            title = s.timelinePhotosTab,
                            selected = activeTab == ProgressPhotosViewTab.TIMELINE,
                            onClick = { activeTab = ProgressPhotosViewTab.TIMELINE },
                            modifier = Modifier.weight(1f)
                        )
                        ProgressTabPill(
                            title = s.comparePhotosTab,
                            selected = activeTab == ProgressPhotosViewTab.COMPARE,
                            onClick = { activeTab = ProgressPhotosViewTab.COMPARE },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (activeTab == ProgressPhotosViewTab.TIMELINE) {
                    if (photos.isEmpty()) {
                        item {
                            KumaCard(
                                modifier = Modifier.fillMaxWidth(),
                                backgroundColor = KumaColors.SurfaceContainerLow,
                                cornerRadius = 18.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        tint = colors.accentText,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(
                                        text = s.progressPhotosEmptyTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        ),
                                        textAlign = TextAlign.Center
                                    )
                                    Text(
                                        text = s.progressPhotosEmptyBody,
                                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    } else {
                        items(monthlyGroups, key = { it.key }) { group ->
                            val isExpanded = expandedMonths[group.key] ?: true
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(KumaColors.SurfaceContainer)
                                        .clickable { expandedMonths[group.key] = !isExpanded }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = FitnessHistoryOrganizer.formatMonthYearHeader(group.year, group.monthNumber, s),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        )
                                        Text(
                                            text = s.progressPhotosDashboardSubtitle(group.photos.size),
                                            style = MaterialTheme.typography.labelSmall.copy(color = colors.textSecondary)
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = colors.textSecondary
                                    )
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        group.photos.chunked(2).forEach { pair ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                pair.forEach { entry ->
                                                    ProgressPhotoGridCard(
                                                        entry = entry,
                                                        weightUnit = weightUnit,
                                                        language = language,
                                                        s = s,
                                                        onClick = { selectedDetailPhoto = entry },
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                                if (pair.size == 1) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    item {
                        ProgressPhotoComparisonSection(
                            photos = photos,
                            weightUnit = weightUnit,
                            language = language,
                            s = s
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(60.dp)) }
            }
        }
    }

    pendingNewPhotoPath?.let { path ->
        AddProgressPhotoDetailsDialog(
            imagePath = path,
            defaultBodyweightKg = settings.profile.bodyweightKg,
            weightUnit = weightUnit,
            language = language,
            onDismiss = {
                deleteProgressPhotoFile(path)
                pendingNewPhotoPath = null
            },
            onSave = { entry ->
                progressPhotoRepository.addPhoto(entry)
                pendingNewPhotoPath = null
            }
        )
    }

    selectedDetailPhoto?.let { detail ->
        ProgressPhotoDetailDialog(
            entry = detail,
            weightUnit = weightUnit,
            language = language,
            onDismiss = { selectedDetailPhoto = null },
            onDeleteRequest = { photoPendingDelete = detail }
        )
    }

    photoPendingDelete?.let { target ->
        KumaDeleteConfirmationDialog(
            title = s.progressPhotoDeleteTitle,
            message = s.progressPhotoDeleteMessage,
            confirmText = s.delete,
            cancelText = s.cancel,
            onConfirm = {
                progressPhotoRepository.deletePhoto(target.id)
                photoPendingDelete = null
                if (selectedDetailPhoto?.id == target.id) {
                    selectedDetailPhoto = null
                }
            },
            onDismiss = { photoPendingDelete = null }
        )
    }

    if (showCameraPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionDialog = false },
            containerColor = KumaColors.SurfaceDim,
            title = {
                Text(
                    text = s.cameraPermissionDeniedTitle,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = s.cameraPermissionDeniedMessage,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        openAppPermissionSettings()
                    }
                ) {
                    Text(s.openSettingsAction, color = colors.accentText, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCameraPermissionDialog = false
                        mediaLaunchers.launchGallery()
                    }
                ) {
                    Text(s.chooseFromGalleryAction, color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProgressTabPill(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    Box(
        modifier = modifier
            .clip(PillShape)
            .background(if (selected) colors.accentFill else KumaColors.SurfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = if (selected) colors.onAccentFill else colors.textSecondary
            )
        )
    }
}

@Composable
private fun ProgressPhotoGridCard(
    entry: ProgressPhotoEntry,
    weightUnit: WeightUnit,
    language: AppLanguage,
    s: AppStrings,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    KumaCard(
        modifier = modifier.clickable(onClick = onClick),
        backgroundColor = KumaColors.SurfaceContainer,
        cornerRadius = 16.dp
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.8f)
            ) {
                ProgressPhotoImage(
                    imagePath = entry.imagePath,
                    contentDescription = entry.note.ifBlank { s.progressPhotosTitle },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(KumaColors.CanvasLowest.copy(alpha = 0.72f))
                        .padding(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = s.privateOnlyBadge,
                        tint = colors.accentText,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = FitnessHistoryOrganizer.formatFullDateWithYear(entry.capturedDateEpochMs, "", s),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
                entry.bodyweightKg?.let { kg ->
                    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
                    val formatted = NumberFormatUtils.formatDecimal(
                        UnitConverters.toDisplayWeight(kg, weightUnit),
                        language,
                        decimals = 1,
                        trimTrailingZeros = true
                    )
                    Text(
                        text = "$formatted $unitLabel",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.accentText,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressPhotoComparisonSection(
    photos: List<ProgressPhotoEntry>,
    weightUnit: WeightUnit,
    language: AppLanguage,
    s: AppStrings
) {
    val colors = LocalKumaColors.current
    if (photos.size < 2) {
        KumaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = KumaColors.SurfaceContainerLow,
            cornerRadius = 18.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Compare,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = s.progressPhotoNeedTwoCompare,
                    style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary),
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val ascending = remember(photos) { photos.sortedBy { it.capturedDateEpochMs } }
    var beforeId by remember(ascending) { mutableStateOf(ascending.first().id) }
    var afterId by remember(ascending) { mutableStateOf(ascending.last().id) }

    val beforePhoto = ascending.firstOrNull { it.id == beforeId } ?: ascending.first()
    val afterPhoto = ascending.firstOrNull { it.id == afterId } ?: ascending.last()
    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        KumaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = KumaColors.SurfaceContainer,
            cornerRadius = 18.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ComparisonSideCard(
                        badgeText = s.progressPhotoBeforeLabel,
                        entry = beforePhoto,
                        weightUnit = weightUnit,
                        language = language,
                        s = s,
                        modifier = Modifier.weight(1f)
                    )
                    ComparisonSideCard(
                        badgeText = s.progressPhotoAfterLabel,
                        entry = afterPhoto,
                        weightUnit = weightUnit,
                        language = language,
                        s = s,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Elapsed Days & Weight Delta Summary
                val elapsedDays = (abs(afterPhoto.capturedDateEpochMs - beforePhoto.capturedDateEpochMs) / 86_400_000L).toInt()
                val bwBefore = beforePhoto.bodyweightKg
                val bwAfter = afterPhoto.bodyweightKg
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    KumaBadge(text = s.daysElapsedLabel(elapsedDays), isPr = false)
                    if (bwBefore != null && bwAfter != null) {
                        val dispDelta = UnitConverters.toDisplayWeight(bwAfter - bwBefore, weightUnit)
                        val sign = if (dispDelta > 0.0) "+" else ""
                        val formattedDelta = NumberFormatUtils.formatDecimal(dispDelta, language, decimals = 1, trimTrailingZeros = true)
                        KumaBadge(text = "${s.weightDeltaLabel}: $sign$formattedDelta $unitLabel", isPr = true)
                    }
                }
            }
        }

        // Photo pickers for Before and After
        Text(
            text = s.progressPhotoSelectOlderLabel,
            style = MaterialTheme.typography.labelMedium.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ascending.forEach { item ->
                KumaProgressFilterChip(
                    selected = item.id == beforePhoto.id,
                    onClick = { beforeId = item.id },
                    labelText = FitnessHistoryOrganizer.formatFullDateWithYear(item.capturedDateEpochMs, "", s),
                    fontSize = 11.sp
                )
            }
        }

        Text(
            text = s.progressPhotoSelectNewerLabel,
            style = MaterialTheme.typography.labelMedium.copy(color = colors.textPrimary, fontWeight = FontWeight.Bold)
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            ascending.asReversed().forEach { item ->
                KumaProgressFilterChip(
                    selected = item.id == afterPhoto.id,
                    onClick = { afterId = item.id },
                    labelText = FitnessHistoryOrganizer.formatFullDateWithYear(item.capturedDateEpochMs, "", s),
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun ComparisonSideCard(
    badgeText: String,
    entry: ProgressPhotoEntry,
    weightUnit: WeightUnit,
    language: AppLanguage,
    s: AppStrings,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(KumaColors.SurfaceDim)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        KumaBadge(text = badgeText, isPr = badgeText == s.progressPhotoAfterLabel)
        ProgressPhotoImage(
            imagePath = entry.imagePath,
            contentDescription = badgeText,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.75f)
                .clip(RoundedCornerShape(10.dp)),
            contentScale = ContentScale.Crop
        )
        Text(
            text = FitnessHistoryOrganizer.formatFullDateWithYear(entry.capturedDateEpochMs, "", s),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.labelSmall.copy(
                color = colors.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            )
        )
        entry.bodyweightKg?.let { kg ->
            val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
            val formatted = NumberFormatUtils.formatDecimal(
                UnitConverters.toDisplayWeight(kg, weightUnit),
                language,
                decimals = 1,
                trimTrailingZeros = true
            )
            Text(
                text = "$formatted $unitLabel",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = colors.accentText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddProgressPhotoDetailsDialog(
    imagePath: String,
    defaultBodyweightKg: Double?,
    weightUnit: WeightUnit,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onSave: (ProgressPhotoEntry) -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg

    var daysAgoText by remember { mutableStateOf("0") }
    var weightText by remember(defaultBodyweightKg, weightUnit) {
        mutableStateOf(
            defaultBodyweightKg?.let { kg ->
                NumberFormatUtils.formatDecimal(
                    UnitConverters.toDisplayWeight(kg, weightUnit),
                    language,
                    decimals = 1,
                    trimTrailingZeros = true
                )
            }.orEmpty()
        )
    }
    var waistText by remember { mutableStateOf("") }
    var chestText by remember { mutableStateOf("") }
    var armsText by remember { mutableStateOf("") }
    var noteText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .kumaKeyboardAvoidance()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = s.addProgressPhotoDetailsTitle,
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

                ProgressPhotoImage(
                    imagePath = imagePath,
                    contentDescription = s.addProgressPhotoDetailsTitle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    contentScale = ContentScale.Crop
                )

                Text(
                    text = s.progressPhotoDateOffsetLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = colors.textPrimary,
                        fontWeight = FontWeight.Bold
                    )
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(0, 7, 30, 90).forEach { presetDays ->
                        val labelText = if (presetDays == 0) {
                            s.progressPhotoDateTodayOption
                        } else {
                            s.daysElapsedLabel(presetDays)
                        }
                        KumaProgressFilterChip(
                            selected = daysAgoText.toIntOrNull() == presetDays,
                            onClick = { daysAgoText = presetDays.toString() },
                            labelText = labelText
                        )
                    }
                }

                NumericMetricInputField(
                    label = s.progressPhotoDatePickDaysAgoHint,
                    value = daysAgoText,
                    onValueChange = { daysAgoText = it.filter { ch -> ch.isDigit() } },
                    keyboardType = KeyboardType.Number
                )

                NumericMetricInputField(
                    label = "${s.bodyWeightMeasureLabel} ($unitLabel)",
                    value = weightText,
                    onValueChange = { weightText = it },
                    keyboardType = KeyboardType.Decimal
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NumericMetricInputField(
                        label = "${s.waistMeasurementLabel} (${s.sizeUnitCm})",
                        value = waistText,
                        onValueChange = { waistText = it },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )
                    NumericMetricInputField(
                        label = "${s.chestMeasurementLabel} (${s.sizeUnitCm})",
                        value = chestText,
                        onValueChange = { chestText = it },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )
                    NumericMetricInputField(
                        label = "${s.armsMeasurementLabel} (${s.sizeUnitCm})",
                        value = armsText,
                        onValueChange = { armsText = it },
                        modifier = Modifier.weight(1f),
                        keyboardType = KeyboardType.Decimal
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it.take(300) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(s.readinessNotesLabel) },
                    placeholder = { Text(s.progressPhotoNotesHint) },
                    shape = RoundedCornerShape(12.dp),
                    colors = progressDialogInputColors()
                )

                Button(
                    onClick = {
                        val now = currentEpochMillis()
                        val daysAgo = (daysAgoText.toIntOrNull() ?: 0).coerceIn(0, 3650)
                        val capturedAt = (now - daysAgo * 86_400_000L).coerceAtLeast(1L)
                        val parsedBwKg = NumberFormatUtils.parseDecimalInput(weightText)
                            ?.takeIf { it.isFinite() && it in 10.0..1000.0 }
                            ?.let { UnitConverters.toStorageWeightKg(it, weightUnit) }
                        val entry = ProgressPhotoEntry(
                            id = IdGenerator.progressPhotoId(),
                            imagePath = imagePath,
                            capturedDateEpochMs = capturedAt,
                            bodyweightKg = parsedBwKg,
                            waistCm = NumberFormatUtils.parseDecimalInput(waistText)?.takeIf { it in 20.0..300.0 },
                            chestCm = NumberFormatUtils.parseDecimalInput(chestText)?.takeIf { it in 20.0..300.0 },
                            armsCm = NumberFormatUtils.parseDecimalInput(armsText)?.takeIf { it in 10.0..150.0 },
                            note = noteText.trim(),
                            isPrivate = true,
                            createdAtEpochMs = now
                        )
                        onSave(entry)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(
                        text = s.progressPhotoSaveAction,
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
    }
}

@Composable
private fun KumaProgressFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    labelText: String,
    fontSize: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified
) {
    val colors = LocalKumaColors.current
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = labelText,
                fontSize = fontSize,
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

@Composable
private fun progressDialogInputColors() = LocalKumaColors.current.let { colors ->
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        focusedContainerColor = colors.surfaceVariant,
        unfocusedContainerColor = colors.surfaceVariant,
        focusedBorderColor = colors.accentFill,
        unfocusedBorderColor = colors.border,
        focusedLabelColor = colors.accentText,
        unfocusedLabelColor = colors.textSecondary,
        focusedPlaceholderColor = colors.textSubtle,
        unfocusedPlaceholderColor = colors.textSubtle,
        cursorColor = colors.accentText
    )
}

@Composable
private fun NumericMetricInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    keyboardType: KeyboardType = KeyboardType.Decimal
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        shape = RoundedCornerShape(12.dp),
        colors = progressDialogInputColors()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgressPhotoDetailDialog(
    entry: ProgressPhotoEntry,
    weightUnit: WeightUnit,
    language: AppLanguage,
    onDismiss: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val s = LocalStrings.current
    val colors = LocalKumaColors.current
    val unitLabel = if (weightUnit == WeightUnit.LB) s.unitLb else s.unitKg
    val fullDate = FitnessHistoryOrganizer.formatFullDateWithYear(entry.capturedDateEpochMs, "", s, includeDayOfWeek = true)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp),
            shape = RoundedCornerShape(22.dp),
            color = KumaColors.SurfaceDim,
            tonalElevation = 0.dp,
            border = BorderStroke(1.dp, colors.border)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = fullDate,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = colors.accentText,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = s.privateOnlyBadge,
                                style = MaterialTheme.typography.labelSmall.copy(color = colors.accentText)
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = s.close,
                            tint = colors.textSecondary
                        )
                    }
                }

                ProgressPhotoImage(
                    imagePath = entry.imagePath,
                    contentDescription = fullDate,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.8f)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Fit
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    entry.bodyweightKg?.let { kg ->
                        val disp = NumberFormatUtils.formatDecimal(
                            UnitConverters.toDisplayWeight(kg, weightUnit),
                            language,
                            decimals = 1,
                            trimTrailingZeros = true
                        )
                        KumaBadge(text = "${s.bodyWeightMeasureLabel}: $disp $unitLabel", isPr = true)
                    }
                    entry.waistCm?.let { v ->
                        val d = NumberFormatUtils.formatDecimal(v, language, decimals = 1, trimTrailingZeros = true)
                        KumaBadge(text = "${s.waistMeasurementLabel}: $d ${s.sizeUnitCm}", isPr = false)
                    }
                    entry.chestCm?.let { v ->
                        val d = NumberFormatUtils.formatDecimal(v, language, decimals = 1, trimTrailingZeros = true)
                        KumaBadge(text = "${s.chestMeasurementLabel}: $d ${s.sizeUnitCm}", isPr = false)
                    }
                    entry.armsCm?.let { v ->
                        val d = NumberFormatUtils.formatDecimal(v, language, decimals = 1, trimTrailingZeros = true)
                        KumaBadge(text = "${s.armsMeasurementLabel}: $d ${s.sizeUnitCm}", isPr = false)
                    }
                }

                if (entry.note.isNotBlank()) {
                    Text(
                        text = entry.note,
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDeleteRequest,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            tint = colors.error,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.delete, color = colors.error, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            val summary = buildString {
                                append("$fullDate — ${s.appName}")
                                entry.bodyweightKg?.let { kg ->
                                    val disp = NumberFormatUtils.formatDecimal(
                                        UnitConverters.toDisplayWeight(kg, weightUnit),
                                        language,
                                        decimals = 1,
                                        trimTrailingZeros = true
                                    )
                                    append("\n${s.bodyWeightMeasureLabel}: $disp $unitLabel")
                                }
                                if (entry.note.isNotBlank()) {
                                    append("\n${entry.note}")
                                }
                            }
                            shareProgressPhotoWithSummary(entry.imagePath, summary, s.progressPhotoShareAction)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(s.progressPhotoShareAction, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
