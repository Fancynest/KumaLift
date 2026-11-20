package com.bearbones.kumalift.core.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import platform.UIKit.UIApplication

@Composable
actual fun KeepScreenOn(keepOn: Boolean) {
    DisposableEffect(keepOn) {
        UIApplication.sharedApplication.idleTimerDisabled = keepOn
        onDispose {
            UIApplication.sharedApplication.idleTimerDisabled = false
        }
    }
}
