package com.bearbones.kumalift.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class KumaColors(
    val canvas: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val surfaceBright: Color,
    val border: Color,
    val borderStrong: Color,
    val scrim: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textSubtle: Color,
    val accentFill: Color,
    val accentFillPressed: Color,
    val onAccentFill: Color,
    val accentFillBorder: Color,
    val accentText: Color,
    val accentTextOnTint: Color,
    val accentChart: Color,
    val accentTint: Color,
    val error: Color,
    val onError: Color,
    val errorTint: Color,
    val chartSeriesMuted: Color,
    val chartGrid: Color,
    val chartThreshold: Color,
    val isDark: Boolean,
) {
    fun toHighContrast(): KumaColors {
        return copy(
            border = borderStrong,
            textSecondary = textPrimary.copy(alpha = 0.85f),
            textSubtle = textPrimary.copy(alpha = 0.70f)
        )
    }

    companion object {
        val current: KumaColors @Composable get() = LocalKumaColors.current

        // Dynamic theme-aware accessors for existing call-sites
        val CanvasLowest: Color @Composable get() = LocalKumaColors.current.canvas
        val SurfaceDim: Color @Composable get() = LocalKumaColors.current.surface
        val Surface: Color @Composable get() = LocalKumaColors.current.surface
        val SurfaceVariant: Color @Composable get() = LocalKumaColors.current.surfaceVariant
        val SurfaceBright: Color @Composable get() = LocalKumaColors.current.surfaceBright

        val SurfaceContainerLowest: Color @Composable get() = LocalKumaColors.current.canvas
        val SurfaceContainerLow: Color @Composable get() = LocalKumaColors.current.surface
        val SurfaceContainer: Color @Composable get() = LocalKumaColors.current.surfaceVariant
        val SurfaceContainerHigh: Color @Composable get() = LocalKumaColors.current.surfaceVariant
        val SurfaceContainerHighest: Color @Composable get() = LocalKumaColors.current.surfaceBright

        val ElectricLime: Color @Composable get() = LocalKumaColors.current.accentFill
        val ElectricLimeDim: Color @Composable get() = LocalKumaColors.current.accentFillPressed
        val OnElectricLime: Color @Composable get() = LocalKumaColors.current.onAccentFill
        val ElectricLimeContainer: Color @Composable get() = LocalKumaColors.current.accentTint
        val OnElectricLimeContainer: Color @Composable get() = LocalKumaColors.current.accentTextOnTint
        val ElectricLimeGlow: Color @Composable get() = LocalKumaColors.current.accentTint

        val TextWhite: Color @Composable get() = LocalKumaColors.current.textPrimary
        val OnSurface: Color @Composable get() = LocalKumaColors.current.textPrimary
        val OnSurfaceVariant: Color @Composable get() = LocalKumaColors.current.textSecondary
        val TextMuted: Color @Composable get() = LocalKumaColors.current.textSecondary
        val TextSecondary: Color @Composable get() = LocalKumaColors.current.textSecondary
        val TextTertiary: Color @Composable get() = LocalKumaColors.current.textSubtle

        val BorderDefault: Color @Composable get() = LocalKumaColors.current.border
        val BorderSubtle: Color @Composable get() = LocalKumaColors.current.border
        val BorderStrong: Color @Composable get() = LocalKumaColors.current.borderStrong
        val BorderFocused: Color @Composable get() = if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart

        val SecondarySlate: Color @Composable get() = LocalKumaColors.current.surfaceVariant
        val SecondaryContainer: Color @Composable get() = LocalKumaColors.current.surfaceBright
        val ErrorRed: Color @Composable get() = LocalKumaColors.current.error
        val ErrorContainer: Color @Composable get() = LocalKumaColors.current.errorTint
    }
}

val DarkColors = KumaColors(
    canvas = Color(0xFF0C0E12),
    surface = Color(0xFF1A1C20),
    surfaceVariant = Color(0xFF22252A),
    surfaceBright = Color(0xFF37393E),
    border = Color(0xFF2A2D35),
    borderStrong = Color(0xFF6B7280),
    scrim = Color(0x99000000),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF94A3B8),
    textSubtle = Color(0xFF64748B),
    accentFill = Color(0xFFC6FF3D),
    accentFillPressed = Color(0xFFB3EB2A),
    onAccentFill = Color(0xFF0C0E12),
    accentFillBorder = Color.Transparent,
    accentText = Color(0xFFC6FF3D),
    accentTextOnTint = Color(0xFFC6FF3D),
    accentChart = Color(0xFFC6FF3D),
    accentTint = Color(0x24C6FF3D),
    error = Color(0xFFFF5C5C),
    onError = Color(0xFF0C0E12),
    errorTint = Color(0x24FF5C5C),
    chartSeriesMuted = Color(0xFF37393E),
    chartGrid = Color(0xFF2A2D35),
    chartThreshold = Color(0xFF64748B),
    isDark = true,
)

val LightColors = KumaColors(
    canvas = Color(0xFFF4F5F2),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFECEEE8),
    surfaceBright = Color(0xFFE1E4DB),
    border = Color(0xFFD8DCD1),
    borderStrong = Color(0xFF8A9183),
    scrim = Color(0x660C0E12),
    textPrimary = Color(0xFF0C0E12),
    textSecondary = Color(0xFF475569),
    textSubtle = Color(0xFF64748B),
    accentFill = Color(0xFFC6FF3D),
    accentFillPressed = Color(0xFFB3EB2A),
    onAccentFill = Color(0xFF0C0E12),
    accentFillBorder = Color(0xFF9FD11F),
    accentText = Color(0xFF4D7C0F),
    accentTextOnTint = Color(0xFF3F6212),
    accentChart = Color(0xFF65A30D),
    accentTint = Color(0xFFEAF7C2),
    error = Color(0xFFD92D20),
    onError = Color(0xFFFFFFFF),
    errorTint = Color(0xFFFDE7E5),
    chartSeriesMuted = Color(0xFFD5D9CF),
    chartGrid = Color(0xFFE1E4DB),
    chartThreshold = Color(0xFF64748B),
    isDark = false,
)

val LocalKumaColors = staticCompositionLocalOf { DarkColors }
