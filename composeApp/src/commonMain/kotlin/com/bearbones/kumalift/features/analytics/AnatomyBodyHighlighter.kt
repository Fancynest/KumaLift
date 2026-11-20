package com.bearbones.kumalift.features.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.LocalKumaColors
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.core.localization.label
import com.bearbones.kumalift.domain.model.MuscleGroup
import com.bearbones.kumalift.domain.model.MuscleVolumeLandmark
import com.bearbones.kumalift.domain.model.MuscleVolumeStats
import com.bearbones.kumalift.domain.model.MuscleVolumeStatus

enum class BodyView {
    FRONT,
    BACK
}

private data class MuscleRegion(
    val muscleGroup: MuscleGroup,
    val bounds: Rect,
    val buildPath: (scaleX: Float, scaleY: Float) -> Path
)

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

    var currentView by remember { mutableStateOf(initialView) }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(MuscleGroup.CHEST) }

    fun statusColor(status: MuscleVolumeStatus): Color = when (status) {
        MuscleVolumeStatus.RECOVERING -> colors.surfaceVariant.copy(alpha = 0.45f)
        MuscleVolumeStatus.MAINTENANCE -> colors.info
        MuscleVolumeStatus.OPTIMAL -> colors.recoveryFresh
        MuscleVolumeStatus.OVERREACHING -> colors.warning
        MuscleVolumeStatus.OVERTRAINED -> colors.error
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Front / Back View Switcher
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(colors.surfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            BodyView.entries.forEach { view ->
                val isSelected = view == currentView
                val label = if (view == BodyView.FRONT) s.bodyViewFront else s.bodyViewBack
                Box(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(if (isSelected) colors.surfaceBright else Color.Transparent)
                        .clickable { currentView = view }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) colors.accentText else colors.textSecondary
                        )
                    )
                }
            }
        }

        // SVG Canvas Anatomical Body Model
        val canvasWidth = 220.dp
        val canvasHeight = 330.dp

        Box(
            modifier = Modifier
                .size(width = canvasWidth, height = canvasHeight)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.surface.copy(alpha = 0.5f))
                .border(1.dp, colors.border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .size(width = 200.dp, height = 310.dp)
                    .pointerInput(currentView) {
                        detectTapGestures { offset ->
                            val scaleX = size.width / 160f
                            val scaleY = size.height / 300f
                            val normalizedX = offset.x / scaleX
                            val normalizedY = offset.y / scaleY

                            val regions = if (currentView == BodyView.FRONT) frontRegions else backRegions
                            val hit = regions.firstOrNull { it.bounds.contains(Offset(normalizedX, normalizedY)) }
                            if (hit != null) {
                                selectedMuscle = hit.muscleGroup
                                onMuscleSelected?.invoke(hit.muscleGroup)
                            }
                        }
                    }
            ) {
                val scaleX = size.width / 160f
                val scaleY = size.height / 300f

                // Draw Head & Body Silhouette Contour
                drawBodySilhouette(scaleX, scaleY, colors.surfaceVariant.copy(alpha = 0.25f), colors.borderStrong)

                // Draw Muscle Regions
                val regions = if (currentView == BodyView.FRONT) frontRegions else backRegions
                for (region in regions) {
                    val stat = statsMap[region.muscleGroup]
                    val status = stat?.status ?: MuscleVolumeStatus.RECOVERING
                    val fillColor = statusColor(status)
                    val isSelected = region.muscleGroup == selectedMuscle

                    val path = region.buildPath(scaleX, scaleY)
                    drawPath(
                        path = path,
                        color = fillColor,
                        style = Fill
                    )
                    drawPath(
                        path = path,
                        color = if (isSelected) colors.accentFill else colors.canvas,
                        style = Stroke(width = if (isSelected) 2.5f * scaleX else 1.2f * scaleX)
                    )
                }
            }
        }

        // Quick Muscle Selector Chips
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                MuscleGroup.CHEST,
                MuscleGroup.BACK,
                MuscleGroup.SHOULDERS,
                MuscleGroup.ARMS,
                MuscleGroup.LEGS,
                MuscleGroup.CORE
            ).forEach { muscle ->
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
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp),
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
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) colors.textPrimary else colors.textSecondary
                        )
                    )
                }
            }
        }

        // Interactive Inspection Card for Selected Muscle
        selectedMuscle?.let { muscle ->
            val stat = statsMap[muscle] ?: MuscleVolumeStats(
                muscleGroup = muscle,
                weeklySets = 0,
                directSets = 0,
                indirectSets = 0,
                totalVolumeKg = 0.0,
                landmark = com.bearbones.kumalift.domain.model.DefaultVolumeLandmarks.forMuscle(muscle),
                status = MuscleVolumeStatus.RECOVERING
            )

            MuscleVolumeDetailCard(
                stat = stat,
                statusColor = statusColor(stat.status)
            )
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

// -------------------------------------------------------------
// Vector Silhouette and Anatomical Path Definitions (160 x 300)
// -------------------------------------------------------------

private fun DrawScope.drawBodySilhouette(scaleX: Float, scaleY: Float, contourColor: Color, borderColor: Color) {
    // Head Oval
    drawOval(
        color = contourColor,
        topLeft = Offset((80f - 14f) * scaleX, (22f - 15f) * scaleY),
        size = Size(28f * scaleX, 30f * scaleY),
        style = Fill
    )
    drawOval(
        color = borderColor,
        topLeft = Offset((80f - 14f) * scaleX, (22f - 15f) * scaleY),
        size = Size(28f * scaleX, 30f * scaleY),
        style = Stroke(width = 1f * scaleX)
    )

    // Body Frame Contour Outline
    val silhouettePath = Path().apply {
        moveTo(70f * scaleX, 36f * scaleY)
        lineTo(50f * scaleX, 48f * scaleY)
        lineTo(30f * scaleX, 74f * scaleY)
        lineTo(24f * scaleX, 144f * scaleY)
        lineTo(38f * scaleX, 146f * scaleY)
        lineTo(48f * scaleX, 106f * scaleY)
        lineTo(54f * scaleX, 144f * scaleY)
        lineTo(52f * scaleX, 218f * scaleY)
        lineTo(54f * scaleX, 282f * scaleY)
        lineTo(72f * scaleX, 282f * scaleY)
        lineTo(76f * scaleX, 216f * scaleY)
        lineTo(80f * scaleX, 156f * scaleY)
        lineTo(84f * scaleX, 216f * scaleY)
        lineTo(88f * scaleX, 282f * scaleY)
        lineTo(106f * scaleX, 282f * scaleY)
        lineTo(108f * scaleX, 218f * scaleY)
        lineTo(106f * scaleX, 144f * scaleY)
        lineTo(112f * scaleX, 106f * scaleY)
        lineTo(122f * scaleX, 146f * scaleY)
        lineTo(136f * scaleX, 144f * scaleY)
        lineTo(130f * scaleX, 74f * scaleY)
        lineTo(110f * scaleX, 48f * scaleY)
        lineTo(90f * scaleX, 36f * scaleY)
        close()
    }
    drawPath(path = silhouettePath, color = contourColor, style = Fill)
    drawPath(path = silhouettePath, color = borderColor, style = Stroke(width = 1f * scaleX))
}

// Front Anatomical Muscle Regions
private val frontRegions = listOf(
    // Chest Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.CHEST,
        bounds = Rect(48f, 52f, 79f, 85f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(52f * sx, 54f * sy)
                lineTo(78f * sx, 54f * sy)
                lineTo(78f * sx, 84f * sy)
                lineTo(54f * sx, 80f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.CHEST,
        bounds = Rect(81f, 52f, 112f, 85f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(82f * sx, 54f * sy)
                lineTo(108f * sx, 54f * sy)
                lineTo(106f * sx, 80f * sy)
                lineTo(82f * sx, 84f * sy)
                close()
            }
        }
    ),
    // Shoulders Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.SHOULDERS,
        bounds = Rect(38f, 48f, 54f, 78f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(50f * sx, 48f * sy)
                lineTo(38f * sx, 64f * sy)
                lineTo(44f * sx, 76f * sy)
                lineTo(52f * sx, 66f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.SHOULDERS,
        bounds = Rect(106f, 48f, 122f, 78f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(110f * sx, 48f * sy)
                lineTo(108f * sx, 66f * sy)
                lineTo(116f * sx, 76f * sy)
                lineTo(122f * sx, 64f * sy)
                close()
            }
        }
    ),
    // Biceps Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(32f, 76f, 48f, 108f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(44f * sx, 76f * sy)
                lineTo(48f * sx, 92f * sy)
                lineTo(44f * sx, 106f * sy)
                lineTo(34f * sx, 98f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(112f, 76f, 128f, 108f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(116f * sx, 76f * sy)
                lineTo(126f * sx, 98f * sy)
                lineTo(116f * sx, 106f * sy)
                lineTo(112f * sx, 92f * sy)
                close()
            }
        }
    ),
    // Forearms Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(24f, 108f, 44f, 146f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(42f * sx, 108f * sy)
                lineTo(38f * sx, 144f * sy)
                lineTo(26f * sx, 140f * sy)
                lineTo(32f * sx, 106f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(116f, 108f, 136f, 146f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(118f * sx, 108f * sy)
                lineTo(128f * sx, 106f * sy)
                lineTo(134f * sx, 140f * sy)
                lineTo(122f * sx, 144f * sy)
                close()
            }
        }
    ),
    // Core (Abs & Obliques)
    MuscleRegion(
        muscleGroup = MuscleGroup.CORE,
        bounds = Rect(58f, 86f, 102f, 146f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(60f * sx, 86f * sy)
                lineTo(100f * sx, 86f * sy)
                lineTo(94f * sx, 144f * sy)
                lineTo(66f * sx, 144f * sy)
                close()
            }
        }
    ),
    // Quads Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(52f, 148f, 78f, 218f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(54f * sx, 148f * sy)
                lineTo(77f * sx, 148f * sy)
                lineTo(74f * sx, 216f * sy)
                lineTo(54f * sx, 214f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(82f, 148f, 108f, 218f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(83f * sx, 148f * sy)
                lineTo(106f * sx, 148f * sy)
                lineTo(106f * sx, 214f * sy)
                lineTo(86f * sx, 216f * sy)
                close()
            }
        }
    ),
    // Calves Front Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(52f, 220f, 74f, 282f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(54f * sx, 220f * sy)
                lineTo(72f * sx, 220f * sy)
                lineTo(68f * sx, 280f * sy)
                lineTo(56f * sx, 278f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(86f, 220f, 108f, 282f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(88f * sx, 220f * sy)
                lineTo(106f * sx, 220f * sy)
                lineTo(104f * sx, 278f * sy)
                lineTo(92f * sx, 280f * sy)
                close()
            }
        }
    )
)

// Back Anatomical Muscle Regions
private val backRegions = listOf(
    // Upper Trapezius
    MuscleRegion(
        muscleGroup = MuscleGroup.BACK,
        bounds = Rect(56f, 36f, 104f, 62f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(72f * sx, 36f * sy)
                lineTo(88f * sx, 36f * sy)
                lineTo(104f * sx, 48f * sy)
                lineTo(80f * sx, 62f * sy)
                lineTo(56f * sx, 48f * sy)
                close()
            }
        }
    ),
    // Rear Deltoids
    MuscleRegion(
        muscleGroup = MuscleGroup.SHOULDERS,
        bounds = Rect(38f, 48f, 54f, 76f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(50f * sx, 48f * sy)
                lineTo(38f * sx, 64f * sy)
                lineTo(44f * sx, 74f * sy)
                lineTo(52f * sx, 66f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.SHOULDERS,
        bounds = Rect(106f, 48f, 122f, 76f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(110f * sx, 48f * sy)
                lineTo(108f * sx, 66f * sy)
                lineTo(116f * sx, 74f * sy)
                lineTo(122f * sx, 64f * sy)
                close()
            }
        }
    ),
    // Lats (Latissimus Dorsi) & Mid Back
    MuscleRegion(
        muscleGroup = MuscleGroup.BACK,
        bounds = Rect(52f, 64f, 108f, 138f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(54f * sx, 66f * sy)
                lineTo(80f * sx, 64f * sy)
                lineTo(106f * sx, 66f * sy)
                lineTo(98f * sx, 136f * sy)
                lineTo(62f * sx, 136f * sy)
                close()
            }
        }
    ),
    // Triceps Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(32f, 74f, 48f, 108f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(44f * sx, 74f * sy)
                lineTo(48f * sx, 92f * sy)
                lineTo(44f * sx, 106f * sy)
                lineTo(34f * sx, 98f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(112f, 74f, 128f, 108f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(116f * sx, 74f * sy)
                lineTo(126f * sx, 98f * sy)
                lineTo(116f * sx, 106f * sy)
                lineTo(112f * sx, 92f * sy)
                close()
            }
        }
    ),
    // Forearms Back Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(24f, 108f, 44f, 146f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(42f * sx, 108f * sy)
                lineTo(38f * sx, 144f * sy)
                lineTo(26f * sx, 140f * sy)
                lineTo(32f * sx, 106f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.ARMS,
        bounds = Rect(116f, 108f, 136f, 146f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(118f * sx, 108f * sy)
                lineTo(128f * sx, 106f * sy)
                lineTo(134f * sx, 140f * sy)
                lineTo(122f * sx, 144f * sy)
                close()
            }
        }
    ),
    // Glutes Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(54f, 140f, 78f, 178f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(56f * sx, 140f * sy)
                lineTo(78f * sx, 140f * sy)
                lineTo(76f * sx, 176f * sy)
                lineTo(54f * sx, 172f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(82f, 140f, 106f, 178f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(82f * sx, 140f * sy)
                lineTo(104f * sx, 140f * sy)
                lineTo(106f * sx, 172f * sy)
                lineTo(84f * sx, 176f * sy)
                close()
            }
        }
    ),
    // Hamstrings Left & Right
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(54f, 180f, 76f, 226f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(56f * sx, 180f * sy)
                lineTo(76f * sx, 180f * sy)
                lineTo(73f * sx, 224f * sy)
                lineTo(55f * sx, 222f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(84f, 180f, 106f, 226f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(84f * sx, 180f * sy)
                lineTo(104f * sx, 180f * sy)
                lineTo(105f * sx, 222f * sy)
                lineTo(87f * sx, 224f * sy)
                close()
            }
        }
    ),
    // Calves Back (Gastrocnemius / Soleus)
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(52f, 228f, 74f, 282f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(54f * sx, 228f * sy)
                lineTo(72f * sx, 228f * sy)
                lineTo(68f * sx, 280f * sy)
                lineTo(56f * sx, 278f * sy)
                close()
            }
        }
    ),
    MuscleRegion(
        muscleGroup = MuscleGroup.LEGS,
        bounds = Rect(86f, 228f, 108f, 282f),
        buildPath = { sx, sy ->
            Path().apply {
                moveTo(88f * sx, 228f * sy)
                lineTo(106f * sx, 228f * sy)
                lineTo(104f * sx, 278f * sy)
                lineTo(92f * sx, 280f * sy)
                close()
            }
        }
    )
)
