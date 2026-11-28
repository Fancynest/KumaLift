package com.bearbones.kumalift.features.share

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bearbones.kumalift.core.designsystem.KumaLogoMark
import com.bearbones.kumalift.core.designsystem.KumaTheme
import com.bearbones.kumalift.core.designsystem.PillShape
import com.bearbones.kumalift.core.designsystem.ShareCardPalette
import com.bearbones.kumalift.core.designsystem.ShareTokens
import com.bearbones.kumalift.core.localization.LocalStrings
import com.bearbones.kumalift.domain.model.ShareItemOffset
import com.bearbones.kumalift.domain.share.ShareAccentPreset
import com.bearbones.kumalift.domain.share.ShareBackgroundMode
import com.bearbones.kumalift.domain.share.ShareCardTemplateStyle
import com.bearbones.kumalift.domain.share.ShareLayoutKeys
import com.bearbones.kumalift.domain.share.ShareStatIconKey
import com.bearbones.kumalift.domain.share.ShareTemplate
import com.bearbones.kumalift.domain.share.WorkoutShareData
import com.bearbones.kumalift.domain.share.WorkoutShareStatId
import com.bearbones.kumalift.domain.share.WorkoutShareStatItem
import kotlin.math.roundToInt

private data class PositionedShareElement(
    val key: String,
    val offset: ShareItemOffset,
    val scale: Float = 1f,
    val content: @Composable () -> Unit
)

@Composable
fun WorkoutShareCardContent(
    data: WorkoutShareData,
    visibleStats: List<WorkoutShareStatItem>,
    template: ShareTemplate,
    templateStyle: ShareCardTemplateStyle,
    backgroundMode: ShareBackgroundMode,
    accentPreset: ShareAccentPreset,
    photoBitmap: ImageBitmap?,
    photoScale: Float,
    photoOffsetX: Float,
    photoOffsetY: Float,
    athleteTag: String,
    showBrandWatermark: Boolean,
    itemPositions: Map<String, ShareItemOffset>,
    itemScales: Map<String, Float> = emptyMap(),
    activeDragKey: String?,
    selectedItemKey: String? = null,
    isTransparentExport: Boolean,
    isExportingRender: Boolean,
    onDragItem: ((key: String, newOffset: ShareItemOffset) -> Unit)? = null,
    onDragEnd: ((key: String, finalOffset: ShareItemOffset) -> Unit)? = null,
    onSelectItem: ((key: String?) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val palette = ShareTokens.darkWorkoutPalette(accentPreset)
    val effectiveMode = if (isTransparentExport) ShareBackgroundMode.TRANSPARENT else backgroundMode
    val hasPhoto = effectiveMode == ShareBackgroundMode.PHOTO && photoBitmap != null

    val bgModifier = when (effectiveMode) {
        ShareBackgroundMode.TRANSPARENT -> Modifier.background(Color.Transparent)
        ShareBackgroundMode.SOLID_COLOR -> Modifier.background(palette.background)
        ShareBackgroundMode.GRADIENT -> Modifier.background(
            Brush.verticalGradient(
                listOf(palette.backgroundSecondary, palette.background, palette.backgroundSecondary)
            )
        )
        ShareBackgroundMode.PHOTO -> Modifier.background(if (hasPhoto) palette.background else palette.backgroundSecondary)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .then(bgModifier)
    ) {
        if (hasPhoto) {
            Image(
                bitmap = photoBitmap!!,
                contentDescription = data.workoutTitle,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = photoScale
                        scaleY = photoScale
                        translationX = photoOffsetX
                        translationY = photoOffsetY
                    }
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.56f),
                                0.30f to Color.Black.copy(alpha = 0.08f),
                                0.70f to Color.Black.copy(alpha = 0.08f),
                                1.0f to Color.Black.copy(alpha = 0.66f)
                            )
                        )
                    )
            )
        }

        val showSubtleDropShadow = effectiveMode == ShareBackgroundMode.TRANSPARENT || hasPhoto
        val textShadow = if (showSubtleDropShadow) {
            Shadow(color = Color.Black.copy(alpha = 0.65f), blurRadius = 6f)
        } else null

        val defaults = ShareLayoutKeys.defaultPositions(
            style = templateStyle,
            visibleStatIds = visibleStats.map { it.id }
        )

        val elements = buildList {
            visibleStats.forEach { stat ->
                val pos = itemPositions[stat.id] ?: defaults[stat.id] ?: ShareItemOffset(0.5f, 0.25f)
                val sc = (itemScales[stat.id] ?: 1f).coerceIn(0.6f, 2.0f)
                add(
                    PositionedShareElement(
                        key = stat.id,
                        offset = pos,
                        scale = sc
                    ) {
                        ShareStatItemView(
                            stat = stat,
                            templateStyle = templateStyle,
                            palette = palette,
                            isTransparent = effectiveMode == ShareBackgroundMode.TRANSPARENT,
                            textShadow = textShadow
                        )
                    }
                )
            }

            add(
                PositionedShareElement(
                    key = ShareLayoutKeys.WORKOUT_TITLE,
                    offset = itemPositions[ShareLayoutKeys.WORKOUT_TITLE]
                        ?: defaults[ShareLayoutKeys.WORKOUT_TITLE]
                        ?: ShareItemOffset(0.02f, 0.96f),
                    scale = (itemScales[ShareLayoutKeys.WORKOUT_TITLE] ?: 1f).coerceIn(0.6f, 2.0f)
                ) {
                    WorkoutTitleSlot(
                        data = data,
                        athleteTag = athleteTag,
                        palette = palette,
                        textShadow = textShadow,
                        centered = templateStyle == ShareCardTemplateStyle.BOLD_CENTER
                    )
                }
            )

            if (showBrandWatermark) {
                add(
                    PositionedShareElement(
                        key = ShareLayoutKeys.BRAND_MARK,
                        offset = itemPositions[ShareLayoutKeys.BRAND_MARK]
                            ?: defaults[ShareLayoutKeys.BRAND_MARK]
                            ?: ShareItemOffset(0.98f, 0.96f),
                        scale = (itemScales[ShareLayoutKeys.BRAND_MARK] ?: 1f).coerceIn(0.6f, 2.0f)
                    ) {
                        KumaBrandWatermark(palette = palette, textShadow = textShadow)
                    }
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = template.safeAreaHorizontalDp.dp,
                    end = template.safeAreaHorizontalDp.dp,
                    top = template.safeAreaTopDp.dp,
                    bottom = template.safeAreaBottomDp.dp
                )
        ) {
            val density = LocalDensity.current
            val areaWidthPx = with(density) { maxWidth.toPx().coerceAtLeast(1f) }
            val areaHeightPx = with(density) { maxHeight.toPx().coerceAtLeast(1f) }

            Layout(
                modifier = Modifier.fillMaxSize(),
                content = {
                    elements.forEach { elem ->
                        val latestPos by rememberUpdatedState(elem.offset)
                        val latestSelected by rememberUpdatedState(selectedItemKey)
                        val latestDrag by rememberUpdatedState(onDragItem)
                        val latestDragEnd by rememberUpdatedState(onDragEnd)
                        val latestSelect by rememberUpdatedState(onSelectItem)
                        val isHighlighted = !isExportingRender && (activeDragKey == elem.key || selectedItemKey == elem.key)
                        val interactionModifier = if (!isExportingRender && onDragItem != null) {
                            Modifier
                                .pointerInput(elem.key) {
                                    detectTapGestures(
                                        onTap = {
                                            latestSelect?.invoke(if (latestSelected == elem.key) null else elem.key)
                                        }
                                    )
                                }
                                .pointerInput(elem.key, areaWidthPx, areaHeightPx) {
                                    detectDragGestures(
                                        onDragStart = { latestSelect?.invoke(elem.key) },
                                        onDragEnd = { latestDragEnd?.invoke(elem.key, latestPos) },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val cur = latestPos
                                            val next = ShareLayoutKeys.clampOffset(
                                                x = cur.x + dragAmount.x / (areaWidthPx * 0.72f).coerceAtLeast(1f),
                                                y = cur.y + dragAmount.y / (areaHeightPx * 0.78f).coerceAtLeast(1f)
                                            )
                                            latestDrag?.invoke(elem.key, next)
                                        }
                                    )
                                }
                        } else {
                            Modifier
                        }

                        Box(
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = elem.scale
                                    scaleY = elem.scale
                                }
                                .then(interactionModifier)
                                .then(
                                    if (isHighlighted) {
                                        Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.5.dp, palette.accent, RoundedCornerShape(12.dp))
                                            .background(palette.accent.copy(alpha = 0.10f))
                                    } else {
                                        Modifier
                                    }
                                )
                                .padding(4.dp)
                        ) {
                            elem.content()
                        }
                    }
                }
            ) { measurables, constraints ->
                val childConstraints = Constraints(
                    minWidth = 0,
                    maxWidth = constraints.maxWidth.coerceAtLeast(1),
                    minHeight = 0,
                    maxHeight = constraints.maxHeight.coerceAtLeast(1)
                )
                val placeables = measurables.map { it.measure(childConstraints) }
                val layoutW = constraints.maxWidth.coerceAtLeast(1)
                val layoutH = constraints.maxHeight.coerceAtLeast(1)
                layout(layoutW, layoutH) {
                    placeables.forEachIndexed { idx, placeable ->
                        val elem = elements[idx]
                        val off = elem.offset
                        val scaledW = (placeable.width * elem.scale).roundToInt()
                        val scaledH = (placeable.height * elem.scale).roundToInt()
                        val maxX = (layoutW - scaledW).coerceAtLeast(0)
                        val maxY = (layoutH - scaledH).coerceAtLeast(0)
                        val leftEdge = (off.x * maxX).roundToInt().coerceIn(0, maxX)
                        val topEdge = (off.y * maxY).roundToInt().coerceIn(0, maxY)
                        val px = leftEdge + ((scaledW - placeable.width) / 2f).roundToInt()
                        val py = topEdge + ((scaledH - placeable.height) / 2f).roundToInt()
                        placeable.placeRelative(px, py)
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareStatItemView(
    stat: WorkoutShareStatItem,
    templateStyle: ShareCardTemplateStyle,
    palette: ShareCardPalette,
    isTransparent: Boolean,
    textShadow: Shadow?
) {
    when (templateStyle) {
        ShareCardTemplateStyle.HEVY_CLASSIC -> {
            Column(
                modifier = Modifier.widthIn(max = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stat.label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = palette.textSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        shadow = textShadow
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = buildStatDisplayText(stat),
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = palette.textPrimary,
                            fontSize = if (stat.value.length > 10) 13.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            shadow = textShadow
                        )
                    )
                    if (stat.id == WorkoutShareStatId.PR_COUNT.storageKey) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        ShareCardTemplateStyle.BOLD_CENTER -> {
            Box(
                modifier = Modifier
                    .widthIn(min = 128.dp, max = 162.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isTransparent) Color.Black.copy(alpha = 0.48f) else palette.cardSurface)
                    .border(
                        width = 1.dp,
                        color = if (stat.id == WorkoutShareStatId.PR_COUNT.storageKey) {
                            palette.accent.copy(alpha = 0.75f)
                        } else {
                            palette.border.copy(alpha = 0.85f)
                        },
                        shape = RoundedCornerShape(14.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = stat.iconKey.toVector(),
                            contentDescription = null,
                            tint = palette.accent,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = stat.label.uppercase(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = palette.textSecondary,
                                letterSpacing = 0.6.sp
                            )
                        )
                    }
                    Text(
                        text = buildStatDisplayText(stat),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = if (stat.value.length > 14) 12.sp else 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = palette.textPrimary
                        )
                    )
                }
            }
        }
        ShareCardTemplateStyle.MINIMAL_CORNER -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .widthIn(max = 164.dp)
                    .clip(PillShape)
                    .background(if (isTransparent) Color.Black.copy(alpha = 0.50f) else palette.cardSurface)
                    .border(1.dp, palette.border.copy(alpha = 0.75f), PillShape)
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = stat.iconKey.toVector(),
                    contentDescription = null,
                    tint = palette.accent,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = buildStatDisplayText(stat),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = palette.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun WorkoutTitleSlot(
    data: WorkoutShareData,
    athleteTag: String,
    palette: ShareCardPalette,
    textShadow: Shadow?,
    centered: Boolean
) {
    Column(
        modifier = Modifier.widthIn(max = 220.dp),
        horizontalAlignment = if (centered) Alignment.CenterHorizontally else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (athleteTag.isNotBlank()) {
            Text(
                text = athleteTag,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium.copy(
                    color = palette.accent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    shadow = textShadow
                )
            )
        }
        Text(
            text = data.workoutTitle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.titleMedium.copy(
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                shadow = textShadow
            )
        )
        Text(
            text = data.formattedDate,
            maxLines = 1,
            style = MaterialTheme.typography.labelSmall.copy(
                color = palette.textSecondary,
                fontSize = 11.sp,
                shadow = textShadow
            )
        )
    }
}

@Composable
private fun KumaBrandWatermark(
    palette: ShareCardPalette,
    textShadow: Shadow?
) {
    val s = LocalStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .clip(PillShape)
            .background(Color.Black.copy(alpha = 0.28f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        KumaLogoMark(
            modifier = Modifier.size(18.dp),
            accentColor = palette.accent,
            darkColor = palette.onAccent
        )
        Text(
            text = s.appName,
            style = KumaTheme.typography.brandLogo.copy(
                fontSize = 14.sp,
                color = palette.textPrimary,
                shadow = textShadow
            )
        )
    }
}

private fun buildStatDisplayText(stat: WorkoutShareStatItem): String {
    return if (stat.unit.isNotBlank()) "${stat.value} ${stat.unit}" else stat.value
}

internal fun ShareStatIconKey.toVector() = when (this) {
    ShareStatIconKey.TIMER -> Icons.Default.Timer
    ShareStatIconKey.VOLUME -> Icons.Default.FitnessCenter
    ShareStatIconKey.TROPHY -> Icons.Default.EmojiEvents
    ShareStatIconKey.CHECK -> Icons.Default.CheckCircle
    ShareStatIconKey.EXERCISES -> Icons.Default.FitnessCenter
    ShareStatIconKey.REST -> Icons.Default.Speed
    ShareStatIconKey.CALORIES -> Icons.Default.LocalFireDepartment
    ShareStatIconKey.RPE -> Icons.Default.Bolt
    ShareStatIconKey.WORKOUT -> Icons.Default.FitnessCenter
    ShareStatIconKey.DATE -> Icons.Default.CalendarToday
    ShareStatIconKey.MUSCLE -> Icons.Default.FitnessCenter
    ShareStatIconKey.STREAK -> Icons.Default.Bolt
    ShareStatIconKey.TOP_SET -> Icons.Default.EmojiEvents
}
