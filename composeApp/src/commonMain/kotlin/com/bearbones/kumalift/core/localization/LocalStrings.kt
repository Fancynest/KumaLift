package com.bearbones.kumalift.core.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import com.bearbones.kumalift.core.util.resolveIsEnglish
import com.bearbones.kumalift.domain.model.AppLanguage

/**
 * CompositionLocal providing [AppStrings] down the composable tree.
 * Defaults to [EnStrings].
 */
val LocalStrings = staticCompositionLocalOf<AppStrings> { EnStrings }

/**
 * Convenience accessor for composables: [appStrings].
 */
val appStrings: AppStrings
    @Composable
    @ReadOnlyComposable
    get() = LocalStrings.current

/**
 * Resolve [AppStrings] outside composition from [AppLanguage].
 */
fun resolveAppStrings(language: AppLanguage): AppStrings =
    if (resolveIsEnglish(language)) EnStrings else IdStrings
