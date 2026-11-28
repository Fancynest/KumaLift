package com.bearbones.kumalift.core.designsystem

import androidx.compose.ui.graphics.Color
import com.bearbones.kumalift.domain.share.ShareAccentPreset

data class ShareCardPalette(
    val background: Color,
    val backgroundSecondary: Color,
    val cardSurface: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val accent: Color,
    val onAccent: Color,
    val isDark: Boolean
)

object ShareTokens {
    fun resolveAccentColor(preset: ShareAccentPreset): Color = when (preset) {
        ShareAccentPreset.LIME -> DarkColors.accentFill
        ShareAccentPreset.WHITE -> Color(0xFFFFFFFF)
        ShareAccentPreset.BLUE -> DarkColors.info
        ShareAccentPreset.AMBER -> DarkColors.warning
        ShareAccentPreset.CORAL -> DarkColors.error
    }

    fun resolveOnAccentColor(preset: ShareAccentPreset): Color = when (preset) {
        ShareAccentPreset.LIME,
        ShareAccentPreset.WHITE,
        ShareAccentPreset.BLUE,
        ShareAccentPreset.AMBER -> Color(0xFF0C0E12)
        ShareAccentPreset.CORAL -> Color(0xFFFFFFFF)
    }

    fun darkWorkoutPalette(accentPreset: ShareAccentPreset): ShareCardPalette = ShareCardPalette(
        background = DarkColors.canvas,
        backgroundSecondary = Color(0xFF161B24),
        cardSurface = DarkColors.surface.copy(alpha = 0.88f),
        border = DarkColors.border,
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD1D5DB),
        accent = resolveAccentColor(accentPreset),
        onAccent = resolveOnAccentColor(accentPreset),
        isDark = true
    )

    fun darkProgramPalette(accentPreset: ShareAccentPreset): ShareCardPalette = ShareCardPalette(
        background = DarkColors.canvas,
        backgroundSecondary = DarkColors.surface,
        cardSurface = DarkColors.surfaceVariant,
        border = DarkColors.border,
        textPrimary = DarkColors.textPrimary,
        textSecondary = DarkColors.textSecondary,
        accent = resolveAccentColor(accentPreset),
        onAccent = resolveOnAccentColor(accentPreset),
        isDark = true
    )

    fun cleanLightProgramPalette(accentPreset: ShareAccentPreset): ShareCardPalette {
        val accent = when (accentPreset) {
            ShareAccentPreset.LIME -> Color(0xFF2563EB)
            ShareAccentPreset.WHITE -> Color(0xFF111827)
            ShareAccentPreset.BLUE -> Color(0xFF2563EB)
            ShareAccentPreset.AMBER -> Color(0xFFD97706)
            ShareAccentPreset.CORAL -> Color(0xFFDC2626)
        }
        return ShareCardPalette(
            background = Color(0xFFFFFFFF),
            backgroundSecondary = Color(0xFFF8FAFC),
            cardSurface = Color(0xFFF3F4F6),
            border = Color(0xFFE5E7EB),
            textPrimary = Color(0xFF111827),
            textSecondary = Color(0xFF6B7280),
            accent = accent,
            onAccent = Color(0xFFFFFFFF),
            isDark = false
        )
    }
}
