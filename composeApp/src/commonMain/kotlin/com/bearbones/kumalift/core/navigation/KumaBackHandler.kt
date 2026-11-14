package com.bearbones.kumalift.core.navigation

import androidx.compose.runtime.Composable

@Composable
expect fun KumaBackHandler(enabled: Boolean = true, onBack: () -> Unit)
