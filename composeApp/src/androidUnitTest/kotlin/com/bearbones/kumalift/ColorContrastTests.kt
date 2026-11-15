package com.bearbones.kumalift

import androidx.compose.ui.graphics.Color
import com.bearbones.kumalift.core.designsystem.DarkColors
import com.bearbones.kumalift.core.designsystem.LightColors
import java.io.File
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
        val colors = DarkColors

        // Core UI tokens
        val onAccentContrast = contrastRatio(colors.onAccentFill, colors.accentFill)
        assertTrue(onAccentContrast >= 15.0, "onAccentFill on accentFill should be >= 15:1, got $onAccentContrast")

        val textPrimaryCanvas = contrastRatio(colors.textPrimary, colors.canvas)
        assertTrue(textPrimaryCanvas >= 15.0, "textPrimary on canvas should be >= 15:1, got $textPrimaryCanvas")

        val textSecondarySurface = contrastRatio(colors.textSecondary, colors.surface)
        assertTrue(textSecondarySurface >= 4.5, "textSecondary on surface should be >= 4.5:1, got $textSecondarySurface")

        val errorSurface = contrastRatio(colors.error, colors.surface)
        assertTrue(errorSurface >= 4.5, "error on surface should be >= 4.5:1, got $errorSurface")

        // E4: SetType badge tokens (must meet WCAG AA >= 4.5:1)
        val warmupBadge = contrastRatio(colors.onSetTypeWarmup, colors.setTypeWarmup)
        assertTrue(warmupBadge >= 4.5, "onSetTypeWarmup on setTypeWarmup should be >= 4.5:1, got $warmupBadge")

        val dropBadge = contrastRatio(colors.onSetTypeDrop, colors.setTypeDrop)
        assertTrue(dropBadge >= 4.5, "onSetTypeDrop on setTypeDrop should be >= 4.5:1, got $dropBadge")

        val failureBadge = contrastRatio(colors.onSetTypeFailure, colors.setTypeFailure)
        assertTrue(failureBadge >= 4.5, "onSetTypeFailure on setTypeFailure should be >= 4.5:1, got $failureBadge")

        // E4: Muscle recovery tokens
        val freshRecovery = contrastRatio(colors.onRecoveryFresh, colors.recoveryFresh)
        assertTrue(freshRecovery >= 4.5, "onRecoveryFresh on recoveryFresh should be >= 4.5:1, got $freshRecovery")

        val tiredRecovery = contrastRatio(colors.onRecoveryTired, colors.recoveryTired)
        assertTrue(tiredRecovery >= 4.5, "onRecoveryTired on recoveryTired should be >= 4.5:1, got $tiredRecovery")

        val exhaustedRecovery = contrastRatio(colors.onRecoveryExhausted, colors.recoveryExhausted)
        assertTrue(exhaustedRecovery >= 4.5, "onRecoveryExhausted on recoveryExhausted should be >= 4.5:1, got $exhaustedRecovery")

        // E4: Olympic barbell plate tokens
        val plateRed = contrastRatio(colors.onPlateRed, colors.plateRed)
        assertTrue(plateRed >= 4.5, "onPlateRed on plateRed should be >= 4.5:1, got $plateRed")

        val plateBlue = contrastRatio(colors.onPlateBlue, colors.plateBlue)
        assertTrue(plateBlue >= 4.5, "onPlateBlue on plateBlue should be >= 4.5:1, got $plateBlue")

        val plateYellow = contrastRatio(colors.onPlateYellow, colors.plateYellow)
        assertTrue(plateYellow >= 4.5, "onPlateYellow on plateYellow should be >= 4.5:1, got $plateYellow")

        val plateGreen = contrastRatio(colors.onPlateGreen, colors.plateGreen)
        assertTrue(plateGreen >= 4.5, "onPlateGreen on plateGreen should be >= 4.5:1, got $plateGreen")

        val plateWhite = contrastRatio(colors.onPlateWhite, colors.plateWhite)
        assertTrue(plateWhite >= 15.0, "onPlateWhite on plateWhite should be >= 15:1, got $plateWhite")

        val plateGray = contrastRatio(colors.onPlateGray, colors.plateGray)
        assertTrue(plateGray >= 4.5, "onPlateGray on plateGray should be >= 4.5:1, got $plateGray")

        val plateBlack = contrastRatio(colors.onPlateBlack, colors.plateBlack)
        assertTrue(plateBlack >= 10.0, "onPlateBlack on plateBlack should be >= 10:1, got $plateBlack")

        // Additional semantic tokens
        val warningContrast = contrastRatio(colors.onWarning, colors.warning)
        assertTrue(warningContrast >= 4.5, "onWarning on warning should be >= 4.5:1, got $warningContrast")

        val supersetContrast = contrastRatio(colors.onSupersetBadge, colors.supersetBadge)
        assertTrue(supersetContrast >= 4.5, "onSupersetBadge on supersetBadge should be >= 4.5:1, got $supersetContrast")

        val dayActiveContrast = contrastRatio(colors.dayActiveText, colors.dayActiveBg)
        assertTrue(dayActiveContrast >= 4.5, "dayActiveText on dayActiveBg should be >= 4.5:1, got $dayActiveContrast")
    }

    @Test
    fun testLightThemeContrastRatios() {
        val colors = LightColors

        // Core UI tokens
        val textPrimaryCanvas = contrastRatio(colors.textPrimary, colors.canvas)
        assertTrue(textPrimaryCanvas >= 15.0, "textPrimary on canvas should be >= 15:1, got $textPrimaryCanvas")

        val textSecondarySurface = contrastRatio(colors.textSecondary, colors.surface)
        assertTrue(textSecondarySurface >= 4.5, "textSecondary on surface should be >= 4.5:1, got $textSecondarySurface")

        val accentTextSurface = contrastRatio(colors.accentText, colors.surface)
        assertTrue(accentTextSurface >= 4.5, "accentText on surface should be >= 4.5:1, got $accentTextSurface")

        val accentTextOnTint = contrastRatio(colors.accentTextOnTint, colors.accentTint)
        assertTrue(accentTextOnTint >= 4.5, "accentTextOnTint on tint should be >= 4.5:1, got $accentTextOnTint")

        val borderStrongSurface = contrastRatio(colors.borderStrong, colors.surface)
        assertTrue(borderStrongSurface >= 3.0, "borderStrong on surface should be >= 3.0:1, got $borderStrongSurface")

        val errorSurface = contrastRatio(colors.error, colors.surface)
        assertTrue(errorSurface >= 4.5, "error on surface should be >= 4.5:1, got $errorSurface")

        // E4: SetType badge tokens (must meet WCAG AA >= 4.5:1)
        val warmupBadge = contrastRatio(colors.onSetTypeWarmup, colors.setTypeWarmup)
        assertTrue(warmupBadge >= 4.5, "onSetTypeWarmup on setTypeWarmup should be >= 4.5:1, got $warmupBadge")

        val dropBadge = contrastRatio(colors.onSetTypeDrop, colors.setTypeDrop)
        assertTrue(dropBadge >= 4.5, "onSetTypeDrop on setTypeDrop should be >= 4.5:1, got $dropBadge")

        val failureBadge = contrastRatio(colors.onSetTypeFailure, colors.setTypeFailure)
        assertTrue(failureBadge >= 4.5, "onSetTypeFailure on setTypeFailure should be >= 4.5:1, got $failureBadge")

        // E4: Muscle recovery tokens
        val freshRecovery = contrastRatio(colors.onRecoveryFresh, colors.recoveryFresh)
        assertTrue(freshRecovery >= 4.5, "onRecoveryFresh on recoveryFresh should be >= 4.5:1, got $freshRecovery")

        val tiredRecovery = contrastRatio(colors.onRecoveryTired, colors.recoveryTired)
        assertTrue(tiredRecovery >= 4.5, "onRecoveryTired on recoveryTired should be >= 4.5:1, got $tiredRecovery")

        val exhaustedRecovery = contrastRatio(colors.onRecoveryExhausted, colors.recoveryExhausted)
        assertTrue(exhaustedRecovery >= 4.5, "onRecoveryExhausted on recoveryExhausted should be >= 4.5:1, got $exhaustedRecovery")

        // E4: Olympic barbell plate tokens
        val plateRed = contrastRatio(colors.onPlateRed, colors.plateRed)
        assertTrue(plateRed >= 4.5, "onPlateRed on plateRed should be >= 4.5:1, got $plateRed")

        val plateBlue = contrastRatio(colors.onPlateBlue, colors.plateBlue)
        assertTrue(plateBlue >= 4.5, "onPlateBlue on plateBlue should be >= 4.5:1, got $plateBlue")

        val plateYellow = contrastRatio(colors.onPlateYellow, colors.plateYellow)
        assertTrue(plateYellow >= 4.5, "onPlateYellow on plateYellow should be >= 4.5:1, got $plateYellow")

        val plateGreen = contrastRatio(colors.onPlateGreen, colors.plateGreen)
        assertTrue(plateGreen >= 4.5, "onPlateGreen on plateGreen should be >= 4.5:1, got $plateGreen")

        val plateWhite = contrastRatio(colors.onPlateWhite, colors.plateWhite)
        assertTrue(plateWhite >= 15.0, "onPlateWhite on plateWhite should be >= 15:1, got $plateWhite")

        val plateGray = contrastRatio(colors.onPlateGray, colors.plateGray)
        assertTrue(plateGray >= 4.5, "onPlateGray on plateGray should be >= 4.5:1, got $plateGray")

        val plateBlack = contrastRatio(colors.onPlateBlack, colors.plateBlack)
        assertTrue(plateBlack >= 10.0, "onPlateBlack on plateBlack should be >= 10:1, got $plateBlack")

        // Additional semantic tokens
        val warningContrast = contrastRatio(colors.onWarning, colors.warning)
        assertTrue(warningContrast >= 4.5, "onWarning on warning should be >= 4.5:1, got $warningContrast")

        val supersetContrast = contrastRatio(colors.onSupersetBadge, colors.supersetBadge)
        assertTrue(supersetContrast >= 4.5, "onSupersetBadge on supersetBadge should be >= 4.5:1, got $supersetContrast")

        val dayActiveContrast = contrastRatio(colors.dayActiveText, colors.dayActiveBg)
        assertTrue(dayActiveContrast >= 4.5, "dayActiveText on dayActiveBg should be >= 4.5:1, got $dayActiveContrast")
    }

    @Test
    fun testZeroHardcodedColorHexOutsideDesignSystem() {
        val baseDirs = listOf(
            File("composeApp/src/commonMain/kotlin/com/bearbones/kumalift"),
            File("composeApp/src/androidMain/kotlin/com/bearbones/kumalift"),
            File("composeApp/src/iosMain/kotlin/com/bearbones/kumalift"),
            File("src/commonMain/kotlin/com/bearbones/kumalift"),
            File("src/androidMain/kotlin/com/bearbones/kumalift"),
            File("src/iosMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/commonMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/androidMain/kotlin/com/bearbones/kumalift"),
            File("../composeApp/src/iosMain/kotlin/com/bearbones/kumalift")
        )
        val validDirs = baseDirs.filter { it.exists() && it.isDirectory }.distinctBy { it.canonicalPath }
        assertTrue(validDirs.isNotEmpty(), "Could not find source directories in candidate paths")

        val hexColorRegex = Regex("""Color\(0x""")
        val violations = mutableListOf<String>()

        val ktFiles = validDirs.flatMap { it.walkTopDown() }.filter { file ->
            val path = file.absolutePath.replace('\\', '/')
            file.extension == "kt" &&
                    !path.contains("/core/designsystem/") &&
                    !path.contains("/core/designsystem") &&
                    !path.contains("/test/") &&
                    !path.contains("/androidTest/") &&
                    !path.contains("/commonTest/")
        }.distinctBy { it.canonicalPath }

        for (file in ktFiles) {
            file.useLines { lines ->
                lines.forEachIndexed { index, line ->
                    val trimmed = line.trim()
                    if (!trimmed.startsWith("//") && !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                        if (hexColorRegex.containsMatchIn(line)) {
                            violations.add("${file.name}:${index + 1}: $line")
                        }
                    }
                }
            }
        }

        assertTrue(
            violations.isEmpty(),
            "Found ${violations.size} hardcoded 'Color(0x' outside core/designsystem:\n" +
                    violations.joinToString("\n")
        )
    }
}
