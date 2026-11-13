package com.bearbones.kumalift

import androidx.compose.ui.graphics.Color
import com.bearbones.kumalift.core.designsystem.DarkColors
import com.bearbones.kumalift.core.designsystem.LightColors
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertTrue

class ColorContrastTests {

    private fun linearize(channel: Float): Double {
        return if (channel <= 0.04045f) {
            channel / 12.92
        } else {
            ((channel + 0.055) / 1.055).toDouble().pow(2.4)
        }
    }

    private fun relativeLuminance(color: Color): Double {
        val r = linearize(color.red)
        val g = linearize(color.green)
        val b = linearize(color.blue)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    private fun contrastRatio(color1: Color, color2: Color): Double {
        val l1 = relativeLuminance(color1)
        val l2 = relativeLuminance(color2)
        val lighter = max(l1, l2)
        val darker = min(l1, l2)
        return (lighter + 0.05) / (darker + 0.05)
    }

    @Test
    fun testDarkThemeContrastRatios() {
        // onAccentFill (#0C0E12) on accentFill (#C6FF3D) > 15:1 (WCAG spec says 16.3:1)
        val onAccentContrast = contrastRatio(DarkColors.onAccentFill, DarkColors.accentFill)
        assertTrue(onAccentContrast >= 15.0, "onAccentFill on accentFill should be >= 15:1, got $onAccentContrast")

        // textPrimary on canvas > 15:1
        val textPrimaryCanvas = contrastRatio(DarkColors.textPrimary, DarkColors.canvas)
        assertTrue(textPrimaryCanvas >= 15.0, "textPrimary on canvas should be >= 15:1, got $textPrimaryCanvas")

        // textSecondary on surface >= 4.5:1 (WCAG AA body text)
        val textSecondarySurface = contrastRatio(DarkColors.textSecondary, DarkColors.surface)
        assertTrue(textSecondarySurface >= 4.5, "textSecondary on surface should be >= 4.5:1, got $textSecondarySurface")

        // error on surface >= 4.5:1
        val errorSurface = contrastRatio(DarkColors.error, DarkColors.surface)
        assertTrue(errorSurface >= 4.5, "error on surface should be >= 4.5:1, got $errorSurface")
    }

    @Test
    fun testLightThemeContrastRatios() {
        // textPrimary (#0C0E12) on canvas (#F4F5F2) >= 15:1 (WCAG target 17.7:1)
        val textPrimaryCanvas = contrastRatio(LightColors.textPrimary, LightColors.canvas)
        assertTrue(textPrimaryCanvas >= 15.0, "textPrimary on canvas should be >= 15:1, got $textPrimaryCanvas")

        // textSecondary (#475569) on surface (#FFFFFF) >= 4.5:1 (WCAG target 7.6:1)
        val textSecondarySurface = contrastRatio(LightColors.textSecondary, LightColors.surface)
        assertTrue(textSecondarySurface >= 4.5, "textSecondary on surface should be >= 4.5:1, got $textSecondarySurface")

        // accentText (#4D7C0F) on surface (#FFFFFF) >= 4.5:1 (WCAG target 5.0:1)
        val accentTextSurface = contrastRatio(LightColors.accentText, LightColors.surface)
        assertTrue(accentTextSurface >= 4.5, "accentText on surface should be >= 4.5:1, got $accentTextSurface")

        // accentTextOnTint (#3F6212) on accentTint (#EAF7C2) >= 4.5:1 (WCAG target 6.3:1)
        val accentTextOnTint = contrastRatio(LightColors.accentTextOnTint, LightColors.accentTint)
        assertTrue(accentTextOnTint >= 4.5, "accentTextOnTint on tint should be >= 4.5:1, got $accentTextOnTint")

        // borderStrong (#8A9183) on surface (#FFFFFF) >= 3.0:1 (WCAG UI boundary 3:1)
        val borderStrongSurface = contrastRatio(LightColors.borderStrong, LightColors.surface)
        assertTrue(borderStrongSurface >= 3.0, "borderStrong on surface should be >= 3.0:1, got $borderStrongSurface")

        // error (#D92D20) on surface (#FFFFFF) >= 4.5:1
        val errorSurface = contrastRatio(LightColors.error, LightColors.surface)
        assertTrue(errorSurface >= 4.5, "error on surface should be >= 4.5:1, got $errorSurface")
    }
}
