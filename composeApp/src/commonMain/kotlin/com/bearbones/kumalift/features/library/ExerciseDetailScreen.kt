package com.bearbones.kumalift.features.library

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaCard
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.core.localization.localizedMuscleSubtitle
import com.bearbones.kumalift.domain.model.Exercise

enum class ExerciseDetailTab {
    SUMMARY,
    HOW_TO
}

enum class MetricSwitcherType {
    HEAVIEST_WEIGHT,
    ONE_REP_MAX,
    BEST_SET_VOLUME
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    onNavigateBack: () -> Unit,
    onAddExercise: (() -> Unit)? = null,
    onSaveNote: ((String) -> Unit)? = null
) {
    val s = LocalStrings.current
    var selectedTab by remember { mutableStateOf(ExerciseDetailTab.SUMMARY) }
    var selectedMetric by remember { mutableStateOf(MetricSwitcherType.HEAVIEST_WEIGHT) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var currentNote by remember { mutableStateOf(exercise.pinnedNote) }
    var showHelpDialog by remember { mutableStateOf<String?>(null) }
    var showTipDialog by remember { mutableStateOf(false) }

    val steps = remember(exercise) {
        exercise.getEffectiveSteps()
    }

    val colors = LocalKumaColors.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KumaColors.CanvasLowest)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar: Back button + Title (Share & More crossed out per red X)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(KumaColors.SurfaceDim)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
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
                    text = exercise.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        fontSize = 17.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )

                if (onAddExercise != null) {
                    Button(
                        onClick = onAddExercise,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentFill,
                            contentColor = colors.onAccentFill
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(s.add, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }

            // Top Tab Navigation: Summary | How to
            TabRow(
                selectedTabIndex = if (selectedTab == ExerciseDetailTab.SUMMARY) 0 else 1,
                containerColor = KumaColors.SurfaceDim,
                contentColor = colors.accentText,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[if (selectedTab == ExerciseDetailTab.SUMMARY) 0 else 1]),
                        color = colors.accentText,
                        height = 3.dp
                    )
                },
                divider = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(KumaColors.SurfaceContainer)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == ExerciseDetailTab.SUMMARY,
                    onClick = { selectedTab = ExerciseDetailTab.SUMMARY },
                    text = {
                        Text(
                            text = s.summaryTab,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ExerciseDetailTab.SUMMARY) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == ExerciseDetailTab.SUMMARY) colors.accentText else colors.textSecondary
                            )
                        )
                    }
                )
                Tab(
                    selected = selectedTab == ExerciseDetailTab.HOW_TO,
                    onClick = { selectedTab = ExerciseDetailTab.HOW_TO },
                    text = {
                        Text(
                            text = s.howToTab,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = if (selectedTab == ExerciseDetailTab.HOW_TO) FontWeight.Bold else FontWeight.Medium,
                                color = if (selectedTab == ExerciseDetailTab.HOW_TO) colors.accentText else colors.textSecondary
                            )
                        )
                    }
                )
            }

            // Scrollable Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp)
            ) {
                // Shared Visual Animation on Top (matching exercises-dataset GIF/video player)
                item {
                    ExerciseVisualAnimation(
                        exercise = exercise,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                when (selectedTab) {
                    ExerciseDetailTab.SUMMARY -> {
                        // Exercise Header & Target Muscles
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textPrimary
                                    )
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = s.targetMuscleLabel(exercise.localizedMuscleSubtitle(s)),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary,
                                        fontWeight = FontWeight.Medium
                                    )
                                )

                                val secondaryStr = if (exercise.secondaryMuscles.isNotEmpty()) {
                                    exercise.secondaryMuscles.joinToString(", ") { it.label(s) }
                                } else {
                                    s.stabilizerMusclesDefault
                                }

                                Text(
                                    text = s.secondaryMusclesLabel(secondaryStr),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                        }

                        // Guidance Tip Banner (Lightbulb)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(KumaColors.SurfaceDim)
                                    .clickable { showTipDialog = true }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lightbulb,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8), // Calm light blue bulb
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = s.howToLogEquipmentTitle(exercise.equipment.label.lowercase()),
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            color = colors.textPrimary,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }

                        // Analytics Chart Widget with Metric Toggle Pills
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                KumaCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        // Visual chart icon / placeholder
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                            contentDescription = null,
                                            tint = colors.textSecondary.copy(alpha = 0.4f),
                                            modifier = Modifier.size(44.dp)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = s.noWorkoutLogsYet,
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = colors.textSecondary
                                            )
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Toggle Pills: Heaviest Weight | One Rep Max | Best Set Volume
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf(
                                        MetricSwitcherType.HEAVIEST_WEIGHT to s.heaviestWeightTab,
                                        MetricSwitcherType.ONE_REP_MAX to s.oneRepMaxTab,
                                        MetricSwitcherType.BEST_SET_VOLUME to s.bestSetVolumeTab
                                    ).forEach { (metric, label) ->
                                        val isSelected = selectedMetric == metric
                                        Box(
                                            modifier = Modifier
                                                .clip(PillShape)
                                                .background(if (isSelected) colors.accentFill else KumaColors.SurfaceDim)
                                                .clickable { selectedMetric = metric }
                                                .padding(horizontal = 14.dp, vertical = 8.dp)
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
                            }
                        }

                        // Personal Records Section
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24), // Gold medal color
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = s.personalRecordsSection,
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold,
                                                color = colors.textPrimary
                                            )
                                        )
                                    }

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = s.prHelpContentDesc,
                                        tint = colors.textSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                showHelpDialog = s.prHelpDialog
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                val prWeight = exercise.currentPrWeightKg?.let { "$it kg" } ?: "—"
                                val pr1rm = exercise.currentPrWeightKg?.let { "${(it * 1.1).toInt()}.0 kg" } ?: "—"
                                val bestSetVol = exercise.currentPrWeightKg?.let { "${it * (exercise.currentPrReps ?: 1)} kg" } ?: "—"

                                PrMetricRow(label = s.heaviestWeightTab, value = prWeight)
                                Spacer(modifier = Modifier.height(10.dp))
                                PrMetricRow(label = s.oneRepMaxTab, value = pr1rm)
                                Spacer(modifier = Modifier.height(10.dp))
                                PrMetricRow(label = s.bestSetVolumeTab, value = bestSetVol)
                            }
                        }

                        // Strength Level Section
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.strengthLevelTitle,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )

                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                        contentDescription = s.strengthLevelHelpContentDesc,
                                        tint = colors.textSecondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable {
                                                showHelpDialog = s.strengthLevelHelpDialog
                                            }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                KumaCard(modifier = Modifier.fillMaxWidth()) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(KumaColors.SurfaceContainer)
                                                .border(2.dp, colors.accentText, CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "1/3",
                                                style = MaterialTheme.typography.labelSmall.copy(
                                                    fontWeight = FontWeight.Bold,
                                                    color = colors.accentText
                                                )
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = s.strengthLevelHint,
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    color = colors.textPrimary,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            )
                                        }

                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = colors.textSecondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ExerciseDetailTab.HOW_TO -> {
                        // Title
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 16.dp)
                            ) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        color = colors.textPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = s.stepByStepHeader,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = colors.textSecondary
                                    )
                                )
                            }
                        }

                        // Numbered Step-by-Step Instructions
                        itemsIndexed(steps) { index, stepText ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}.",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        fontSize = 16.sp
                                    ),
                                    modifier = Modifier.width(28.dp)
                                )

                                Text(
                                    text = stepText,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textPrimary.copy(alpha = 0.9f),
                                        fontSize = 15.sp,
                                        lineHeight = 22.sp
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Pinned Exercise Notes Section
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = s.pinnedNoteSection,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary
                                        )
                                    )

                                    Text(
                                        text = if (currentNote.isNotBlank()) s.edit else s.add,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = colors.accentText
                                        ),
                                        modifier = Modifier.clickable { showNoteDialog = true }
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                KumaCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showNoteDialog = true }
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PushPin,
                                            contentDescription = null,
                                            tint = if (currentNote.isNotBlank()) colors.accentText else colors.textSecondary.copy(alpha = 0.5f),
                                            modifier = Modifier.size(36.dp)
                                        )

                                        Spacer(modifier = Modifier.height(10.dp))

                                        Text(
                                            text = if (currentNote.isNotBlank()) {
                                                currentNote
                                            } else {
                                                s.pinnedNotePlaceholder
                                            },
                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                color = if (currentNote.isNotBlank()) colors.textPrimary else colors.textSecondary,
                                                fontSize = 14.sp
                                            ),
                                            lineHeight = 20.sp
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

    // Pinned Note Editor Dialog
    if (showNoteDialog) {
        var noteInput by remember { mutableStateOf(currentNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = {
                Text(text = s.pinnedNoteSection, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = s.pinnedNoteAutoHint(exercise.name),
                        style = MaterialTheme.typography.bodySmall.copy(color = colors.textSecondary)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(KumaColors.SurfaceContainer)
                            .padding(12.dp)
                    ) {
                        BasicTextField(
                            value = noteInput,
                            onValueChange = { noteInput = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.textPrimary),
                            cursorBrush = SolidColor(colors.accentText),
                            modifier = Modifier.fillMaxSize(),
                            decorationBox = { inner ->
                                if (noteInput.isBlank()) {
                                    Text(s.pinnedNoteInputPlaceholder, color = colors.textSecondary.copy(alpha = 0.5f))
                                }
                                inner()
                            }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleaned = noteInput.trim()
                        currentNote = cleaned
                        onSaveNote?.invoke(cleaned)
                        showNoteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(s.saveNoteButton, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNoteDialog = false }) {
                    Text(s.cancel, color = colors.textSecondary)
                }
            }
        )
    }

    // Help Dialog
    if (showHelpDialog != null) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = null },
            title = { Text(s.infoTitle, fontWeight = FontWeight.Bold) },
            text = { Text(showHelpDialog!!) },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = null },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(s.done, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Barbell Tip Dialog
    if (showTipDialog) {
        AlertDialog(
            onDismissRequest = { showTipDialog = false },
            title = { Text(s.howToLogDialogTitle(exercise.equipment.label), fontWeight = FontWeight.Bold) },
            text = {
                Text(s.howToLogDialogBody(exercise.equipment.label))
            },
            confirmButton = {
                Button(
                    onClick = { showTipDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accentFill,
                        contentColor = colors.onAccentFill
                    )
                ) {
                    Text(s.understandButton, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@Composable
private fun PrMetricRow(
    label: String,
    value: String
) {
    val colors = LocalKumaColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = colors.textSecondary
            )
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary
            )
        )
    }
}
