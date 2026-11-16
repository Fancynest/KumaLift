package com.bearbones.kumalift.core.designsystem

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
expect fun rememberBarlowCondensedFontFamily(): FontFamily

@Composable
expect fun rememberBarlowFontFamily(): FontFamily

fun createKumaTypography(
    barlowCondensed: FontFamily,
    barlow: FontFamily
): Typography = Typography(
    // Display / Hero (Barlow Condensed ExtraBold 800)
    displayLarge = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    displaySmall = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),

    // Headings & Page Titles (Barlow Condensed Bold 700)
    headlineLarge = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.1).sp
    ),

    // Section Titles, Exercise Names, Workout Titles (Barlow Condensed Bold 700 / SemiBold 600)
    titleLarge = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    titleSmall = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body Text (Barlow Regular 400)
    bodyLarge = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),

    // Buttons / CTAs (Barlow Condensed Bold 700)
    labelLarge = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),

    // Labels, Badges, Tags, Form Field Labels (Barlow SemiBold 600)
    labelMedium = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Secondary Metadata, Navigation, Captions (Barlow Medium 500)
    labelSmall = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.5.sp
    )
)

data class KumaCustomTypography(
    val displayHero: TextStyle,
    val brandLogo: TextStyle,
    val exerciseName: TextStyle,
    val workoutTitle: TextStyle,
    val statNumberHero: TextStyle,
    val statNumberLarge: TextStyle,
    val statNumberMedium: TextStyle,
    val statNumberSmall: TextStyle,
    val statUnit: TextStyle,
    val ctaButton: TextStyle,
    val tagLabel: TextStyle,
    val navLabel: TextStyle,
    val timerNumber: TextStyle
)

fun createKumaCustomTypography(
    barlowCondensed: FontFamily,
    barlow: FontFamily
): KumaCustomTypography = KumaCustomTypography(
    displayHero = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 40.sp,
        lineHeight = 46.sp,
        letterSpacing = (-0.5).sp
    ),
    brandLogo = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp
    ),
    exerciseName = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    ),
    workoutTitle = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    statNumberHero = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-0.5).sp
    ),
    statNumberLarge = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 32.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    statNumberMedium = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.3).sp
    ),
    statNumberSmall = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.2).sp
    ),
    statUnit = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    ),
    ctaButton = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.5.sp
    ),
    tagLabel = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.3.sp
    ),
    navLabel = TextStyle(
        fontFamily = barlow,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp
    ),
    timerNumber = TextStyle(
        fontFamily = barlowCondensed,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    )
)

val LocalKumaCustomTypography = staticCompositionLocalOf {
    createKumaCustomTypography(FontFamily.Default, FontFamily.Default)
}

val KumaTypography: Typography = createKumaTypography(
    barlowCondensed = FontFamily.Default,
    barlow = FontFamily.Default
)
