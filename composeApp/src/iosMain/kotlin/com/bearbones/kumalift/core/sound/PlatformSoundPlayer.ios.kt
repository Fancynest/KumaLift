package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume
import platform.AudioToolbox.AudioServicesPlayAlertSound
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState

actual object PlatformSoundPlayer {
    actual fun playSound(event: SoundEvent, volumePercent: Int) {
        if (volumePercent <= 0) return
        AudioServicesPlayAlertSound(1007.toUInt())
    }

    actual fun playRestTimerSound(
        volumePercent: Int,
        source: RestTimerSoundSource,
        customPath: String?
    ) {
        if (volumePercent <= 0) return
        AudioServicesPlayAlertSound(1007.toUInt())
    }

    actual fun playCustomSound(filePath: String, volumePercent: Int) {
        if (volumePercent <= 0) return
        AudioServicesPlayAlertSound(1007.toUInt())
    }

    actual fun playSystemDefaultSound(volumePercent: Int) {
        if (volumePercent <= 0) return
        AudioServicesPlayAlertSound(1007.toUInt())
    }

    actual fun stopRestTimerSound() {
        // iOS system alert sounds auto-stop upon playback completion
    }

    actual fun playHaptic() {
        try {
            val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
            generator.prepare()
            generator.impactOccurred()
        } catch (_: Exception) {
            AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        }
    }

    actual fun cancelRestFinishedNotification() {
        // Notification management handled via UNUserNotificationCenter in main queue
    }

    actual fun postRestFinishedNotification(channelVariant: ChannelVariant) {
        // Notification management handled via UNUserNotificationCenter in main queue
    }

    actual fun isAppInForeground(): Boolean {
        return UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
    }

    actual fun isScreenInteractive(): Boolean = true

    actual fun getRingerMode(): RingerMode = RingerMode.NORMAL

    actual fun isDoNotDisturbActive(): Boolean = false

    actual fun hasNotificationPermission(): Boolean = true
}
