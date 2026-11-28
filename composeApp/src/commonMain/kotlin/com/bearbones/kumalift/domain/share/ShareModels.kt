package com.bearbones.kumalift.domain.share

import com.bearbones.kumalift.domain.model.ShareItemOffset
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

const val SHARE_CANONICAL_WIDTH_DP = 360f
const val SHARE_CANONICAL_DENSITY = 3f
const val SHARE_EXPORT_WIDTH_PX = 1080
const val SHARE_MAX_HEIGHT_PX = 6000
const val MAX_WORKOUT_SHARE_STATS = 6

@Serializable
enum class ShareAspectRatio(
    val widthPx: Int,
    val heightPx: Int,
    val aspectLabel: String
) {
    SQUARE(1080, 1080, "1:1"),
    PORTRAIT(1080, 1350, "4:5"),
    STORY(1080, 1920, "9:16"),
    TALL_LIST(1080, 0, "FULL")
}

@Serializable
enum class ShareBackgroundMode {
    PHOTO,
    SOLID_COLOR,
    GRADIENT,
    TRANSPARENT
}

@Serializable
enum class ShareAccentPreset {
    LIME,
    WHITE,
    BLUE,
    AMBER,
    CORAL
}

@Serializable
enum class ShareCardTemplateStyle {
    HEVY_CLASSIC,
    BOLD_CENTER,
    MINIMAL_CORNER
}

@Serializable
enum class ProgramShareTemplateStyle {
    DARK_KUMA,
    CLEAN_LIGHT
}

enum class ShareSlotType {
    STAT_CHIP,
    TEXT,
    LOGO,
    EXERCISE_ROW
}

enum class ShareSlotAlignment {
    TOP_START,
    TOP_CENTER,
    TOP_END,
    CENTER_START,
    CENTER,
    CENTER_END,
    BOTTOM_START,
    BOTTOM_CENTER,
    BOTTOM_END
}

data class ShareTemplateSlot(
    val type: ShareSlotType,
    val slotId: String,
    val alignment: ShareSlotAlignment,
    val weight: Float = 1f
)

data class ShareTemplate(
    val id: String,
    val aspectRatio: ShareAspectRatio,
    val defaultBackgroundMode: ShareBackgroundMode,
    val accentPreset: ShareAccentPreset = ShareAccentPreset.LIME,
    val safeAreaTopDp: Float = 24f,
    val safeAreaBottomDp: Float = 24f,
    val safeAreaHorizontalDp: Float = 20f,
    val slots: List<ShareTemplateSlot>
)

data class ShareExportDimensions(
    val widthPx: Int,
    val heightPx: Int,
    val scaleFactor: Float,
    val isCapped: Boolean
)

/**
 * Deterministic pixel dimension capping for tall or scaled exports (default cap 1080x6000 px).
 */
fun calculateCappedExportDimensions(
    naturalWidthPx: Int,
    naturalHeightPx: Int,
    maxWidthPx: Int = SHARE_EXPORT_WIDTH_PX,
    maxHeightPx: Int = SHARE_MAX_HEIGHT_PX
): ShareExportDimensions {
    val safeW = naturalWidthPx.coerceAtLeast(1)
    val safeH = naturalHeightPx.coerceAtLeast(1)
    val widthScale = maxWidthPx.toFloat() / safeW.toFloat()
    val candidateH = (safeH * widthScale).roundToInt().coerceAtLeast(1)
    return if (candidateH > maxHeightPx) {
        val scale = maxHeightPx.toFloat() / safeH.toFloat()
        val cappedW = (safeW * scale).roundToInt().coerceIn(1, maxWidthPx)
        ShareExportDimensions(
            widthPx = cappedW,
            heightPx = maxHeightPx,
            scaleFactor = scale,
            isCapped = true
        )
    } else {
        ShareExportDimensions(
            widthPx = maxWidthPx,
            heightPx = candidateH,
            scaleFactor = widthScale,
            isCapped = false
        )
    }
}

/**
 * Builds deterministic PNG filename safe for cache & MediaStore exports.
 */
fun buildShareExportFileName(prefix: String, epochMs: Long): String {
    val cleanPrefix = prefix
        .trim()
        .lowercase()
        .replace(Regex("[^a-z0-9]+"), "_")
        .trim('_')
        .ifBlank { "kumalift_share" }
    val safeEpoch = epochMs.coerceAtLeast(0L)
    return "${cleanPrefix}_${safeEpoch}.png"
}

object BuiltInShareTemplates {
    fun workoutTemplate(
        style: ShareCardTemplateStyle,
        aspectRatio: ShareAspectRatio,
        backgroundMode: ShareBackgroundMode,
        accentPreset: ShareAccentPreset
    ): ShareTemplate {
        val safeTop = if (aspectRatio == ShareAspectRatio.STORY) 54f else 22f
        val safeBottom = if (aspectRatio == ShareAspectRatio.STORY) 54f else 22f
        val slots = when (style) {
            ShareCardTemplateStyle.HEVY_CLASSIC -> listOf(
                ShareTemplateSlot(ShareSlotType.STAT_CHIP, "stats_strip", ShareSlotAlignment.TOP_CENTER),
                ShareTemplateSlot(ShareSlotType.TEXT, "workout_title", ShareSlotAlignment.BOTTOM_START),
                ShareTemplateSlot(ShareSlotType.LOGO, "brand_mark", ShareSlotAlignment.BOTTOM_END)
            )
            ShareCardTemplateStyle.BOLD_CENTER -> listOf(
                ShareTemplateSlot(ShareSlotType.TEXT, "workout_title", ShareSlotAlignment.TOP_CENTER),
                ShareTemplateSlot(ShareSlotType.STAT_CHIP, "stats_grid", ShareSlotAlignment.CENTER),
                ShareTemplateSlot(ShareSlotType.LOGO, "brand_mark", ShareSlotAlignment.BOTTOM_CENTER)
            )
            ShareCardTemplateStyle.MINIMAL_CORNER -> listOf(
                ShareTemplateSlot(ShareSlotType.LOGO, "brand_mark", ShareSlotAlignment.TOP_START),
                ShareTemplateSlot(ShareSlotType.TEXT, "workout_title", ShareSlotAlignment.TOP_END),
                ShareTemplateSlot(ShareSlotType.STAT_CHIP, "stats_footer", ShareSlotAlignment.BOTTOM_CENTER)
            )
        }
        return ShareTemplate(
            id = "workout_${style.name.lowercase()}",
            aspectRatio = aspectRatio,
            defaultBackgroundMode = backgroundMode,
            accentPreset = accentPreset,
            safeAreaTopDp = safeTop,
            safeAreaBottomDp = safeBottom,
            safeAreaHorizontalDp = 20f,
            slots = slots
        )
    }

    fun programTemplate(
        style: ProgramShareTemplateStyle,
        aspectRatio: ShareAspectRatio,
        accentPreset: ShareAccentPreset
    ): ShareTemplate = ShareTemplate(
        id = "program_${style.name.lowercase()}",
        aspectRatio = aspectRatio,
        defaultBackgroundMode = ShareBackgroundMode.SOLID_COLOR,
        accentPreset = accentPreset,
        safeAreaTopDp = 20f,
        safeAreaBottomDp = 20f,
        safeAreaHorizontalDp = 18f,
        slots = listOf(
            ShareTemplateSlot(ShareSlotType.TEXT, "program_header", ShareSlotAlignment.TOP_START),
            ShareTemplateSlot(ShareSlotType.EXERCISE_ROW, "routine_columns", ShareSlotAlignment.CENTER),
            ShareTemplateSlot(ShareSlotType.LOGO, "brand_mark", ShareSlotAlignment.BOTTOM_END)
        )
    )
}

object ShareLayoutKeys {
    const val WORKOUT_TITLE = "__workout_title__"
    const val BRAND_MARK = "__brand_mark__"

    fun clampOffset(x: Float, y: Float): ShareItemOffset = ShareItemOffset(
        x = x.coerceIn(0f, 1f),
        y = y.coerceIn(0f, 1f)
    )

    fun defaultPositions(
        style: ShareCardTemplateStyle,
        visibleStatIds: List<String>
    ): Map<String, ShareItemOffset> {
        val result = linkedMapOf<String, ShareItemOffset>()
        when (style) {
            ShareCardTemplateStyle.HEVY_CLASSIC -> {
                visibleStatIds.forEachIndexed { idx, statId ->
                    val col = idx % 3
                    val row = idx / 3
                    val countInRow = minOf(3, visibleStatIds.size - row * 3).coerceAtLeast(1)
                    val x = when (countInRow) {
                        1 -> 0.50f
                        2 -> if (col == 0) 0.28f else 0.72f
                        else -> when (col) {
                            0 -> 0.16f
                            1 -> 0.50f
                            else -> 0.84f
                        }
                    }
                    val y = if (row == 0) 0.08f else 0.24f
                    result[statId] = ShareItemOffset(x, y)
                }
                result[WORKOUT_TITLE] = ShareItemOffset(0.02f, 0.96f)
                result[BRAND_MARK] = ShareItemOffset(0.98f, 0.96f)
            }
            ShareCardTemplateStyle.BOLD_CENTER -> {
                result[WORKOUT_TITLE] = ShareItemOffset(0.50f, 0.08f)
                visibleStatIds.forEachIndexed { idx, statId ->
                    val col = idx % 2
                    val row = idx / 2
                    val x = if (col == 0) 0.26f else 0.74f
                    val y = (0.36f + row * 0.20f).coerceAtMost(0.84f)
                    result[statId] = ShareItemOffset(x, y)
                }
                result[BRAND_MARK] = ShareItemOffset(0.50f, 0.97f)
            }
            ShareCardTemplateStyle.MINIMAL_CORNER -> {
                result[BRAND_MARK] = ShareItemOffset(0.02f, 0.04f)
                result[WORKOUT_TITLE] = ShareItemOffset(0.98f, 0.06f)
                visibleStatIds.forEachIndexed { idx, statId ->
                    val col = idx % 2
                    val row = idx / 2
                    val x = if (col == 0) 0.25f else 0.75f
                    val y = (0.76f + row * 0.10f).coerceAtMost(0.97f)
                    result[statId] = ShareItemOffset(x, y)
                }
            }
        }
        return result
    }
}
