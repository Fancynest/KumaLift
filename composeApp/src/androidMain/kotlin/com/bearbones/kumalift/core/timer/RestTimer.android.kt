package com.bearbones.kumalift.core.timer

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.bearbones.kumalift.core.notification.WorkoutReminderReceiver

object AndroidAppProvider {
    @Volatile
    var appContext: Context? = null
}

actual fun playTimerCompletionFeedback() {
    val context = AndroidAppProvider.appContext ?: return
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        if (vibrator?.hasVibrator() == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 300), -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(500)
            }
        }

        WorkoutReminderReceiver.showReminderNotification(
            context = context,
            title = "Rest Finished!",
            message = "Your rest timer is done. Ready for the next set!",
            channelName = "Rest Timer",
            channelDesc = "Notifications for workout rest timer completion"
        )
    } catch (_: Throwable) {
        // Safe fallback if permissions or vibration unavailable
    }
}
