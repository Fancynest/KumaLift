package com.bearbones.kumalift.features.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaColors
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.domain.model.DefaultVolumeLandmarks
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.MuscleVolumeStats
import com.bearbones.kumalift.domain.model.MuscleVolumeStatus

enum class BodyView {
    FRONT,
    BACK
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AnatomyBodyHighlighter(
    statsMap: Map<MuscleGroup, MuscleVolumeStats>,
    modifier: Modifier = Modifier,
    initialView: BodyView = BodyView.FRONT,
    onMuscleSelected: ((MuscleGroup) -> Unit)? = null
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val muscleList = listOf(
        MuscleGroup.CHEST,
        MuscleGroup.BACK,
        MuscleGroup.SHOULDERS,
        MuscleGroup.ARMS,
        MuscleGroup.LEGS,
        MuscleGroup.CORE
    )

    var currentView by remember { mutableStateOf(initialView) }
    var selectedMuscle by remember { mutableStateOf(MuscleGroup.CHEST) }

    fun statusColor(status: MuscleVolumeStatus): Color = when (status) {
        MuscleVolumeStatus.RECOVERING -> colors.surfaceVariant.copy(alpha = 0.55f)
        MuscleVolumeStatus.MAINTENANCE -> colors.info
        MuscleVolumeStatus.OPTIMAL -> colors.recoveryFresh
        MuscleVolumeStatus.OVERREACHING -> colors.warning
        MuscleVolumeStatus.OVERTRAINED -> colors.error
    }

    val totalWeeklySets = statsMap.values.sumOf { it.weeklySets }
    val optimalCount = statsMap.values.count { it.status == MuscleVolumeStatus.OPTIMAL }
    val maintenanceCount = statsMap.values.count { it.status == MuscleVolumeStatus.MAINTENANCE }
    val underCount = statsMap.values.count { it.status == MuscleVolumeStatus.RECOVERING }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Weekly Hypertrophy KPI Summary Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.45f))
                .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "$totalWeeklySets ${s.weeklySetsCountLabel}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (optimalCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.recoveryFresh.copy(alpha = 0.15f))
                            .border(1.dp, colors.recoveryFresh.copy(alpha = 0.5f), PillShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$optimalCount ${s.volumeStatusOptimal}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.recoveryFresh,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
                if (maintenanceCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(colors.info.copy(alpha = 0.15f))
                            .border(1.dp, colors.info.copy(alpha = 0.5f), PillShape)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$maintenanceCount ${s.volumeStatusMaintenance}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = colors.info,
                                fontSize = 10.sp
                            )
                        )
                    }
                }
            }
        }

        // 2. Interactive SVG Anatomy Body Highlighter (from HichamELBSI/react-native-body-highlighter)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.25f))
                .border(1.dp, colors.border, RoundedCornerShape(16.dp))
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // View Switcher: Front View / Back View
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(colors.surfaceBright)
                    .border(1.dp, colors.border, PillShape)
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    BodyView.FRONT to "Front View",
                    BodyView.BACK to "Back View"
                ).forEach { (view, label) ->
                    val isViewSelected = currentView == view
                    Box(
                        modifier = Modifier
                            .clip(PillShape)
                            .background(if (isViewSelected) colors.surfaceVariant else Color.Transparent)
                            .clickable {
                                currentView = view
                                if (view == BodyView.BACK && selectedMuscle == MuscleGroup.CHEST) {
                                    selectedMuscle = MuscleGroup.BACK
                                    onMuscleSelected?.invoke(MuscleGroup.BACK)
                                } else if (view == BodyView.FRONT && selectedMuscle == MuscleGroup.BACK) {
                                    selectedMuscle = MuscleGroup.CHEST
                                    onMuscleSelected?.invoke(MuscleGroup.CHEST)
                                }
                            }
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isViewSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isViewSelected) colors.textPrimary else colors.textSecondary
                            )
                        )
                    }
                }
            }

            // Anatomical SVG Canvas
            AnatomySvgBodyCanvas(
                currentView = currentView,
                selectedMuscle = selectedMuscle,
                statsMap = statsMap,
                statusColor = ::statusColor
            )
        }

        // 3. Muscle Group Selector Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            muscleList.forEach { muscle ->
                val isSelected = muscle == selectedMuscle
                val stat = statsMap[muscle]
                val status = stat?.status ?: MuscleVolumeStatus.RECOVERING
                val chipColor = statusColor(status)

                Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isSelected) colors.surfaceBright else colors.surfaceVariant.copy(alpha = 0.6f))
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) colors.accentFill else colors.border,
                            shape = PillShape
                        )
                        .clickable {
                            selectedMuscle = muscle
                            onMuscleSelected?.invoke(muscle)
                            if (muscle == MuscleGroup.CHEST || muscle == MuscleGroup.CORE) {
                                currentView = BodyView.FRONT
                            } else if (muscle == MuscleGroup.BACK) {
                                currentView = BodyView.BACK
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(chipColor)
                    )
                    Text(
                        text = muscle.label(s),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    )
                }
            }
        }

        // 3. Featured Inspection Card for Selected Muscle
        val activeStat = statsMap[selectedMuscle] ?: MuscleVolumeStats(
            muscleGroup = selectedMuscle,
            weeklySets = 0,
            directSets = 0,
            indirectSets = 0,
            totalVolumeKg = 0.0,
            landmark = DefaultVolumeLandmarks.forMuscle(selectedMuscle),
            status = MuscleVolumeStatus.RECOVERING
        )

        MuscleVolumeDetailCard(
            stat = activeStat,
            statusColor = statusColor(activeStat.status)
        )

        // 4. Full Body Muscle Volume Matrix
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceVariant.copy(alpha = 0.35f))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = s.targetVolumeLandmarkLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            )

            muscleList.forEach { muscle ->
                val mStat = statsMap[muscle] ?: MuscleVolumeStats(
                    muscleGroup = muscle,
                    weeklySets = 0,
                    directSets = 0,
                    indirectSets = 0,
                    totalVolumeKg = 0.0,
                    landmark = DefaultVolumeLandmarks.forMuscle(muscle),
                    status = MuscleVolumeStatus.RECOVERING
                )
                val mColor = statusColor(mStat.status)
                val isSelected = muscle == selectedMuscle
                val maxVolume = maxOf(mStat.landmark.mrv, 20)
                val fillFraction = (mStat.weeklySets.toFloat() / maxVolume.toFloat()).coerceIn(0.04f, 1f)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) colors.surfaceBright else Color.Transparent)
                        .clickable {
                            selectedMuscle = muscle
                            onMuscleSelected?.invoke(muscle)
                        }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(mColor)
                    )

                    Text(
                        text = muscle.label(s),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = colors.textPrimary
                        ),
                        modifier = Modifier.width(76.dp)
                    )

                    // Compact Volume Progress Bar
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(PillShape)
                            .background(colors.surfaceBright)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillFraction)
                                .height(6.dp)
                                .clip(PillShape)
                                .background(mColor)
                        )
                    }

                    Text(
                        text = "${mStat.weeklySets} / ${mStat.landmark.mavMin}-${mStat.landmark.mavMax}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary,
                            fontSize = 11.sp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun MuscleVolumeDetailCard(
    stat: MuscleVolumeStats,
    statusColor: Color
) {
    val colors = LocalKumaColors.current
    val s = LocalStrings.current

    val statusText = when (stat.status) {
        MuscleVolumeStatus.RECOVERING -> s.volumeStatusRecovering
        MuscleVolumeStatus.MAINTENANCE -> s.volumeStatusMaintenance
        MuscleVolumeStatus.OPTIMAL -> s.volumeStatusOptimal
        MuscleVolumeStatus.OVERREACHING -> s.volumeStatusOverreaching
        MuscleVolumeStatus.OVERTRAINED -> s.volumeStatusOvertrained
    }

    val setsNeededForOptimal = maxOf(0, stat.landmark.mev - stat.weeklySets)
    val recommendationText = s.volumeRecommendationHint(stat.status.name, setsNeededForOptimal)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.6f))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header Row: Muscle Title + Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stat.muscleGroup.label(s),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                    )
                    Text(
                        text = "${stat.weeklySets} ${s.weeklySetsCountLabel} (${stat.directSets} ${s.directVsIndirectSetsLabel})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary
                        )
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor, PillShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    )
                }
            }

            // Landmark Progress Indicator (Israetel Hypertrophy Gauge: MEV -> MAV -> MRV)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = s.targetVolumeLandmarkLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                    )
                    Text(
                        text = s.landmarkValuesFormat(
                            stat.landmark.mev,
                            stat.landmark.mavMin,
                            stat.landmark.mavMax,
                            stat.landmark.mrv
                        ),
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = colors.textSecondary,
                            fontSize = 10.sp
                        )
                    )
                }

                // Volume Bar
                val maxScale = maxOf(stat.landmark.mrv + 4, stat.weeklySets + 2)
                val fillFraction = (stat.weeklySets.toFloat() / maxScale.toFloat()).coerceIn(0f, 1f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(PillShape)
                        .background(colors.surfaceBright)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fillFraction)
                            .height(8.dp)
                            .clip(PillShape)
                            .background(statusColor)
                    )
                }
            }

            // Recommendation Callout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surface.copy(alpha = 0.7f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = colors.accentText,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = recommendationText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = colors.textPrimary,
                        fontSize = 11.sp
                    )
                )
            }

            // Top Exercises Contributing
            if (stat.topExercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = s.topExercisesContributingLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textSecondary
                        )
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        stat.topExercises.take(3).forEach { (exName, count) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(colors.surfaceBright)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$exName ($count)",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 10.sp,
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
}

@Composable
private fun AnatomySvgBodyCanvas(
    currentView: BodyView,
    selectedMuscle: MuscleGroup,
    statsMap: Map<MuscleGroup, MuscleVolumeStats>,
    statusColor: (MuscleVolumeStatus) -> Color,
    modifier: Modifier = Modifier
) {
    val colors = LocalKumaColors.current

    val frontParsed = remember {
        val parser = PathParser()
        val outline = parser.parsePathString(BodyHighlighterSvgData.FRONT_OUTLINE).toPath()
        val parts = BodyHighlighterSvgData.FRONT_PARTS.map { part ->
            part to part.pathDataList.map { d ->
                parser.parsePathString(d).toPath()
            }
        }
        outline to parts
    }

    val backParsed = remember {
        val parser = PathParser()
        val outline = parser.parsePathString(BodyHighlighterSvgData.BACK_OUTLINE).toPath()
        val parts = BodyHighlighterSvgData.BACK_PARTS.map { part ->
            part to part.pathDataList.map { d ->
                parser.parsePathString(d).toPath()
            }
        }
        outline to parts
    }

    val (outlinePath, partsList) = if (currentView == BodyView.FRONT) frontParsed else backParsed

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(300.dp)
    ) {
        val scale = size.height / BodyHighlighterSvgData.VIEW_BOX_HEIGHT
        val baseOffsetX = (size.width - BodyHighlighterSvgData.VIEW_BOX_WIDTH * scale) / 2f
        val offsetX = if (currentView == BodyView.FRONT) {
            baseOffsetX
        } else {
            baseOffsetX - BodyHighlighterSvgData.BACK_OFFSET_X * scale
        }

        withTransform({
            translate(left = offsetX, top = 0f)
            scale(scale, scale, pivot = Offset.Zero)
        }) {
            // 1. Draw neutral anatomical body parts (head, hands, feet, etc.)
            partsList.forEach { (part, paths) ->
                if (part.muscleGroup == null) {
                    paths.forEach { path ->
                        drawPath(
                            path = path,
                            color = colors.surfaceVariant.copy(alpha = 0.65f),
                            style = Fill
                        )
                        drawPath(
                            path = path,
                            color = colors.border,
                            style = Stroke(width = 1f)
                        )
                    }
                }
            }

            // 2. Draw muscle parts with hypertrophy coloring
            partsList.forEach { (part, paths) ->
                val mg = part.muscleGroup ?: return@forEach
                val isSelected = mg == selectedMuscle
                val stat = statsMap[mg]
                val muscleStatusColor = statusColor(stat?.status ?: MuscleVolumeStatus.RECOVERING)

                val highlightColor = if (stat != null && stat.weeklySets > 0) {
                    muscleStatusColor
                } else {
                    colors.accentFill
                }

                val fillColor = when {
                    isSelected -> highlightColor.copy(alpha = 0.90f)
                    (stat?.weeklySets ?: 0) > 0 -> muscleStatusColor.copy(alpha = 0.40f)
                    else -> colors.surfaceVariant.copy(alpha = 0.85f)
                }

                val strokeColor = when {
                    isSelected -> if (colors.isDark) highlightColor else colors.accentText
                    (stat?.weeklySets ?: 0) > 0 -> muscleStatusColor.copy(alpha = 0.70f)
                    else -> colors.border
                }

                paths.forEach { path ->
                    drawPath(
                        path = path,
                        color = fillColor,
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = strokeColor,
                        style = Stroke(width = if (isSelected) 3.5f else 1.2f)
                    )
                }
            }

            // 3. Draw overall body silhouette outline
            drawPath(
                path = outlinePath,
                color = colors.borderStrong,
                style = Stroke(width = 2.5f)
            )
        }
    }
}
