package com.bearbones.kumalift.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val KumaDarkColorScheme = darkColorScheme(
    primary = KumaColors.ElectricLime,
    onPrimary = KumaColors.OnElectricLime,
    primaryContainer = KumaColors.ElectricLimeContainer,
    onPrimaryContainer = KumaColors.OnElectricLimeContainer,
    secondary = KumaColors.SecondarySlate,
    onSecondary = KumaColors.TextWhite,
    secondaryContainer = KumaColors.SecondaryContainer,
    onSecondaryContainer = KumaColors.OnSurface,
    background = KumaColors.CanvasLowest,
    onBackground = KumaColors.OnSurface,
    surface = KumaColors.Surface,
    onSurface = KumaColors.OnSurface,
    surfaceVariant = KumaColors.SurfaceContainerHigh,
    onSurfaceVariant = KumaColors.OnSurfaceVariant,
    outline = KumaColors.BorderDefault,
    error = KumaColors.ErrorRed,
    onError = KumaColors.TextWhite
)

@Composable
fun KumaLiftTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KumaDarkColorScheme,
        typography = KumaTypography,
        shapes = KumaShapes,
        content = content
    )
}
