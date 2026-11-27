package com.bearbones.kumalift.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import com.bearbones.kumalift.domain.model.AppTheme

object KumaTheme {
    val colors: KumaColors
        @Composable
        get() = LocalKumaColors.current

    val typography: KumaCustomTypography
        @Composable
        get() = LocalKumaCustomTypography.current
}

private fun KumaColors.toMaterialColorScheme() = if (isDark) {
    darkColorScheme(
        primary = accentFill,
        onPrimary = onAccentFill,
        primaryContainer = accentTint,
        onPrimaryContainer = accentText,
        secondary = surfaceVariant,
        onSecondary = textPrimary,
        secondaryContainer = surfaceBright,
        onSecondaryContainer = textSecondary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = border,
        error = error,
        onError = onError
    )
} else {
    lightColorScheme(
        primary = accentText,
        onPrimary = onError,
        primaryContainer = accentTint,
        onPrimaryContainer = accentTextOnTint,
        secondary = surfaceVariant,
        onSecondary = textPrimary,
        secondaryContainer = surfaceBright,
        onSecondaryContainer = textSecondary,
        background = canvas,
        onBackground = textPrimary,
        surface = surface,
        onSurface = textPrimary,
        surfaceVariant = surfaceVariant,
        onSurfaceVariant = textSecondary,
        outline = border,
        error = error,
        onError = onError
    )
}

@Composable
fun KumaLiftTheme(
    theme: AppTheme = AppTheme.SYSTEM,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val isDark = when (theme) {
        AppTheme.DARK -> true
        AppTheme.LIGHT -> false
        AppTheme.SYSTEM -> isSystemInDarkTheme()
    }

    val baseColors = if (isDark) DarkColors else LightColors
    val resolvedColors = if (highContrast) baseColors.toHighContrast() else baseColors

    val barlowCondensed = rememberBarlowCondensedFontFamily()
    val barlow = rememberBarlowFontFamily()
    val typography = remember(barlowCondensed, barlow) {
        createKumaTypography(barlowCondensed, barlow)
    }
    val customTypography = remember(barlowCondensed, barlow) {
        createKumaCustomTypography(barlowCondensed, barlow)
    }

    CompositionLocalProvider(
        LocalKumaColors provides resolvedColors,
        LocalKumaCustomTypography provides customTypography
    ) {
        MaterialTheme(
            colorScheme = resolvedColors.toMaterialColorScheme(),
            typography = typography,
            shapes = KumaShapes
        ) {
            androidx.compose.material3.ProvideTextStyle(value = typography.bodyMedium) {
                content()
            }
        }
    }
}
