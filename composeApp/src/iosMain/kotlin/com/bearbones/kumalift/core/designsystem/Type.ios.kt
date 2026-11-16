package com.bearbones.kumalift.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import kumalift.composeapp.generated.resources.Res
import kumalift.composeapp.generated.resources.barlow_bold
import kumalift.composeapp.generated.resources.barlow_condensed_bold
import kumalift.composeapp.generated.resources.barlow_condensed_extrabold
import kumalift.composeapp.generated.resources.barlow_condensed_medium
import kumalift.composeapp.generated.resources.barlow_condensed_regular
import kumalift.composeapp.generated.resources.barlow_condensed_semibold
import kumalift.composeapp.generated.resources.barlow_medium
import kumalift.composeapp.generated.resources.barlow_regular
import kumalift.composeapp.generated.resources.barlow_semibold
import org.jetbrains.compose.resources.Font

@Composable
actual fun rememberBarlowCondensedFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.barlow_condensed_regular, FontWeight.Normal),
        Font(Res.font.barlow_condensed_medium, FontWeight.Medium),
        Font(Res.font.barlow_condensed_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_condensed_bold, FontWeight.Bold),
        Font(Res.font.barlow_condensed_extrabold, FontWeight.ExtraBold)
    )
}

@Composable
actual fun rememberBarlowFontFamily(): FontFamily {
    return FontFamily(
        Font(Res.font.barlow_regular, FontWeight.Normal),
        Font(Res.font.barlow_medium, FontWeight.Medium),
        Font(Res.font.barlow_semibold, FontWeight.SemiBold),
        Font(Res.font.barlow_bold, FontWeight.Bold)
    )
}
