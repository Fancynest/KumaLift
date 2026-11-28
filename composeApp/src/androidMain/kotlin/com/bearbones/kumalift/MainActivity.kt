package com.bearbones.kumalift

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.bearbones.kumalift.app.App
import com.bearbones.kumalift.core.timer.AndroidAppProvider
import com.bearbones.kumalift.domain.model.WorkoutPlanDeepLinkBus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidAppProvider.appContext = applicationContext
        handleIncomingPlanIntent(intent)
        window.setBackgroundDrawableResource(android.R.color.black)
        enableEdgeToEdge()
        configureHighRefreshRate(window)
        setContent {
            App()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingPlanIntent(intent)
    }

    private fun handleIncomingPlanIntent(incoming: Intent?) {
        val uriData = incoming?.data?.toString()
        if (!uriData.isNullOrBlank()) {
            WorkoutPlanDeepLinkBus.submit(uriData)
            return
        }
        val sharedText = incoming?.getStringExtra(Intent.EXTRA_TEXT)
        if (!sharedText.isNullOrBlank()) {
            WorkoutPlanDeepLinkBus.submit(sharedText)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        configureHighRefreshRate(window)
    }

    private fun configureHighRefreshRate(win: Window) {
        try {
            // Android 11+ (API 30+): Pick the display mode with maximum refresh rate (90Hz / 120Hz / 144Hz)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val display = win.context.display
                val modes = display?.supportedModes
                if (!modes.isNullOrEmpty()) {
                    val currentMode = display.mode
                    val highestMode = modes
                        .filter { it.physicalWidth == currentMode.physicalWidth && it.physicalHeight == currentMode.physicalHeight }
                        .maxByOrNull { it.refreshRate }
                        ?: modes.maxByOrNull { it.refreshRate }

                    highestMode?.let { mode ->
                        val params = win.attributes
                        params.preferredDisplayModeId = mode.modeId
                        if (mode.refreshRate > 60f) {
                            params.preferredRefreshRate = mode.refreshRate
                        }
                        win.attributes = params
                    }
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Android 6.0 - 10 (API 23 - 29)
                @Suppress("DEPRECATION")
                val modes = win.windowManager?.defaultDisplay?.supportedModes
                val maxRate = modes?.maxOfOrNull { it.refreshRate } ?: 60f
                if (maxRate > 60f) {
                    val params = win.attributes
                    params.preferredRefreshRate = maxRate
                    win.attributes = params
                }
            }
        } catch (_: Throwable) {
            // Graceful fallback: maintain system default
        }
    }
}
