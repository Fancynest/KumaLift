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

    // E4: Semantic role tokens
    // Set type tokens
    val setTypeWarmup: Color,
    val onSetTypeWarmup: Color,
    val setTypeDrop: Color,
    val onSetTypeDrop: Color,
    val setTypeFailure: Color,
    val onSetTypeFailure: Color,

    // Muscle recovery tokens
    val recoveryFresh: Color,
    val onRecoveryFresh: Color,
    val recoveryTired: Color,
    val onRecoveryTired: Color,
    val recoveryExhausted: Color,
    val onRecoveryExhausted: Color,

    // Olympic barbell plate tokens
    val plateRed: Color,
    val onPlateRed: Color,
    val plateBlue: Color,
    val onPlateBlue: Color,
    val plateYellow: Color,
    val onPlateYellow: Color,
    val plateGreen: Color,
    val onPlateGreen: Color,
    val plateWhite: Color,
    val onPlateWhite: Color,
    val plateGray: Color,
    val onPlateGray: Color,
    val plateBlack: Color,
    val onPlateBlack: Color,
    val plateBarbell: Color,
    val plateSleeve: Color,
    val plateTrack: Color,

    // Additional semantic tokens
    val warning: Color,
    val onWarning: Color,
    val warningTint: Color,
    val supersetBadge: Color,
    val onSupersetBadge: Color,
    val supersetBadgeTint: Color,
    val info: Color,
    val onInfo: Color,
    val gold: Color,
    val onGold: Color,
    val dayActiveBg: Color,
    val dayActiveText: Color,
    val mediaPlaceholder: Color,
    val mediaPlaceholderIcon: Color,

    // Anatomy muscle groups
    val muscleChest: Color,
    val muscleBack: Color,
    val muscleLegs: Color,
    val muscleShoulders: Color,
    val muscleArms: Color,
    val muscleCore: Color,
    val muscleCardio: Color,
    val muscleNeck: Color,
    val muscleOther: Color,
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
        val ElectricLimeText: Color @Composable get() = LocalKumaColors.current.accentText
        val ElectricLimeTextOnTint: Color @Composable get() = LocalKumaColors.current.accentTextOnTint
        val ElectricLimeBorder: Color @Composable get() = if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart
        val AccentText: Color @Composable get() = LocalKumaColors.current.accentText
        val AccentTextOnTint: Color @Composable get() = LocalKumaColors.current.accentTextOnTint
        val AccentChart: Color @Composable get() = LocalKumaColors.current.accentChart
        val AccentFill: Color @Composable get() = LocalKumaColors.current.accentFill
        val OnAccentFill: Color @Composable get() = LocalKumaColors.current.onAccentFill
        val AccentFillBorder: Color @Composable get() = LocalKumaColors.current.accentFillBorder
        val ChartSeriesPrimary: Color @Composable get() = if (LocalKumaColors.current.isDark) LocalKumaColors.current.accentFill else LocalKumaColors.current.accentChart

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

        // E4: Semantic role tokens
        val SetTypeWarmup: Color @Composable get() = LocalKumaColors.current.setTypeWarmup
        val OnSetTypeWarmup: Color @Composable get() = LocalKumaColors.current.onSetTypeWarmup
        val SetTypeDrop: Color @Composable get() = LocalKumaColors.current.setTypeDrop
        val OnSetTypeDrop: Color @Composable get() = LocalKumaColors.current.onSetTypeDrop
        val SetTypeFailure: Color @Composable get() = LocalKumaColors.current.setTypeFailure
        val OnSetTypeFailure: Color @Composable get() = LocalKumaColors.current.onSetTypeFailure

        val RecoveryFresh: Color @Composable get() = LocalKumaColors.current.recoveryFresh
        val OnRecoveryFresh: Color @Composable get() = LocalKumaColors.current.onRecoveryFresh
        val RecoveryTired: Color @Composable get() = LocalKumaColors.current.recoveryTired
        val OnRecoveryTired: Color @Composable get() = LocalKumaColors.current.onRecoveryTired
        val RecoveryExhausted: Color @Composable get() = LocalKumaColors.current.recoveryExhausted
        val OnRecoveryExhausted: Color @Composable get() = LocalKumaColors.current.onRecoveryExhausted

        val PlateRed: Color @Composable get() = LocalKumaColors.current.plateRed
        val OnPlateRed: Color @Composable get() = LocalKumaColors.current.onPlateRed
        val PlateBlue: Color @Composable get() = LocalKumaColors.current.plateBlue
        val OnPlateBlue: Color @Composable get() = LocalKumaColors.current.onPlateBlue
        val PlateYellow: Color @Composable get() = LocalKumaColors.current.plateYellow
        val OnPlateYellow: Color @Composable get() = LocalKumaColors.current.onPlateYellow
        val PlateGreen: Color @Composable get() = LocalKumaColors.current.plateGreen
        val OnPlateGreen: Color @Composable get() = LocalKumaColors.current.onPlateGreen
        val PlateWhite: Color @Composable get() = LocalKumaColors.current.plateWhite
        val OnPlateWhite: Color @Composable get() = LocalKumaColors.current.onPlateWhite
        val PlateGray: Color @Composable get() = LocalKumaColors.current.plateGray
        val OnPlateGray: Color @Composable get() = LocalKumaColors.current.onPlateGray
        val PlateBlack: Color @Composable get() = LocalKumaColors.current.plateBlack
        val OnPlateBlack: Color @Composable get() = LocalKumaColors.current.onPlateBlack
        val PlateBarbell: Color @Composable get() = LocalKumaColors.current.plateBarbell
        val PlateSleeve: Color @Composable get() = LocalKumaColors.current.plateSleeve
        val PlateTrack: Color @Composable get() = LocalKumaColors.current.plateTrack

        val Warning: Color @Composable get() = LocalKumaColors.current.warning
        val OnWarning: Color @Composable get() = LocalKumaColors.current.onWarning
        val WarningTint: Color @Composable get() = LocalKumaColors.current.warningTint

        val SupersetBadge: Color @Composable get() = LocalKumaColors.current.supersetBadge
        val OnSupersetBadge: Color @Composable get() = LocalKumaColors.current.onSupersetBadge
        val SupersetBadgeTint: Color @Composable get() = LocalKumaColors.current.supersetBadgeTint

        val Info: Color @Composable get() = LocalKumaColors.current.info
        val OnInfo: Color @Composable get() = LocalKumaColors.current.onInfo
        val Gold: Color @Composable get() = LocalKumaColors.current.gold
        val OnGold: Color @Composable get() = LocalKumaColors.current.onGold

        val DayActiveBg: Color @Composable get() = LocalKumaColors.current.dayActiveBg
        val DayActiveText: Color @Composable get() = LocalKumaColors.current.dayActiveText

        val MediaPlaceholder: Color @Composable get() = LocalKumaColors.current.mediaPlaceholder
        val MediaPlaceholderIcon: Color @Composable get() = LocalKumaColors.current.mediaPlaceholderIcon
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

    // E4: Semantic role tokens
    setTypeWarmup = Color(0xFFF59E0B),
    onSetTypeWarmup = Color(0xFF0C0E12),
    setTypeDrop = Color(0xFF60A5FA),
    onSetTypeDrop = Color(0xFF0C0E12),
    setTypeFailure = Color(0xFFFF5C5C),
    onSetTypeFailure = Color(0xFF0C0E12),

    recoveryFresh = Color(0xFF34D399),
    onRecoveryFresh = Color(0xFF0C0E12),
    recoveryTired = Color(0xFFF59E0B),
    onRecoveryTired = Color(0xFF0C0E12),
    recoveryExhausted = Color(0xFFFF5C5C),
    onRecoveryExhausted = Color(0xFF0C0E12),

    plateRed = Color(0xFFDC2626),
    onPlateRed = Color(0xFFFFFFFF),
    plateBlue = Color(0xFF2563EB),
    onPlateBlue = Color(0xFFFFFFFF),
    plateYellow = Color(0xFFEAB308),
    onPlateYellow = Color(0xFF0C0E12),
    plateGreen = Color(0xFF15803D),
    onPlateGreen = Color(0xFFFFFFFF),
    plateWhite = Color(0xFFFFFFFF),
    onPlateWhite = Color(0xFF0C0E12),
    plateGray = Color(0xFF9CA3AF),
    onPlateGray = Color(0xFF0C0E12),
    plateBlack = Color(0xFF1F2937),
    onPlateBlack = Color(0xFFFFFFFF),
    plateBarbell = Color(0xFF5A6065),
    plateSleeve = Color(0xFF374151),
    plateTrack = Color(0xFF141416),

    warning = Color(0xFFF59E0B),
    onWarning = Color(0xFF0C0E12),
    warningTint = Color(0x24F59E0B),
    supersetBadge = Color(0xFF06B6D4),
    onSupersetBadge = Color(0xFF0C0E12),
    supersetBadgeTint = Color(0x2406B6D4),
    info = Color(0xFF38BDF8),
    onInfo = Color(0xFF0C0E12),
    gold = Color(0xFFFBBF24),
    onGold = Color(0xFF0C0E12),
    dayActiveBg = Color(0xFF1A2C18),
    dayActiveText = Color(0xFFC6FF3D),
    mediaPlaceholder = Color(0xFF161A22),
    mediaPlaceholderIcon = Color(0xFF64748B),

    muscleChest = Color(0xFFFF6B6B),
    muscleBack = Color(0xFF4ECDC4),
    muscleLegs = Color(0xFFFFE66D),
    muscleShoulders = Color(0xFF6BCB77),
    muscleArms = Color(0xFF4D96FF),
    muscleCore = Color(0xFFFF9F43),
    muscleCardio = Color(0xFFFF5252),
    muscleNeck = Color(0xFFE056FD),
    muscleOther = Color(0xFFBDBDBD),
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

    // E4: Semantic role tokens
    setTypeWarmup = Color(0xFFB45309),
    onSetTypeWarmup = Color(0xFFFFFFFF),
    setTypeDrop = Color(0xFF1D4ED8),
    onSetTypeDrop = Color(0xFFFFFFFF),
    setTypeFailure = Color(0xFFB91C1C),
    onSetTypeFailure = Color(0xFFFFFFFF),

    recoveryFresh = Color(0xFF047857),
    onRecoveryFresh = Color(0xFFFFFFFF),
    recoveryTired = Color(0xFFB45309),
    onRecoveryTired = Color(0xFFFFFFFF),
    recoveryExhausted = Color(0xFFB91C1C),
    onRecoveryExhausted = Color(0xFFFFFFFF),

    plateRed = Color(0xFFDC2626),
    onPlateRed = Color(0xFFFFFFFF),
    plateBlue = Color(0xFF2563EB),
    onPlateBlue = Color(0xFFFFFFFF),
    plateYellow = Color(0xFFEAB308),
    onPlateYellow = Color(0xFF0C0E12),
    plateGreen = Color(0xFF15803D),
    onPlateGreen = Color(0xFFFFFFFF),
    plateWhite = Color(0xFFFFFFFF),
    onPlateWhite = Color(0xFF0C0E12),
    plateGray = Color(0xFF4B5563),
    onPlateGray = Color(0xFFFFFFFF),
    plateBlack = Color(0xFF1F2937),
    onPlateBlack = Color(0xFFFFFFFF),
    plateBarbell = Color(0xFF64748B),
    plateSleeve = Color(0xFF94A3B8),
    plateTrack = Color(0xFFE2E8F0),

    warning = Color(0xFFB45309),
    onWarning = Color(0xFFFFFFFF),
    warningTint = Color(0xFFFEF3C7),
    supersetBadge = Color(0xFF0E7490),
    onSupersetBadge = Color(0xFFFFFFFF),
    supersetBadgeTint = Color(0xFFCFFAFE),
    info = Color(0xFF0284C7),
    onInfo = Color(0xFFFFFFFF),
    gold = Color(0xFFB45309),
    onGold = Color(0xFFFFFFFF),
    dayActiveBg = Color(0xFFEAF7C2),
    dayActiveText = Color(0xFF3F6212),
    mediaPlaceholder = Color(0xFFF1F5F9),
    mediaPlaceholderIcon = Color(0xFF94A3B8),

    muscleChest = Color(0xFFDC2626),
    muscleBack = Color(0xFF0D9488),
    muscleLegs = Color(0xFFD97706),
    muscleShoulders = Color(0xFF16A34A),
    muscleArms = Color(0xFF2563EB),
    muscleCore = Color(0xFFEA580C),
    muscleCardio = Color(0xFFE11D48),
    muscleNeck = Color(0xFF9333EA),
    muscleOther = Color(0xFF64748B),
)

val LocalKumaColors = staticCompositionLocalOf { DarkColors }
