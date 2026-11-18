package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume

actual object PlatformSoundPlayer {
    actual fun playSound(event: SoundEvent, volumePercent: Int) {
        // Minimal iOS actual - sound not implemented on iOS in this milestone
    }

    actual fun playRestTimerSound(
        volumePercent: Int,
        source: RestTimerSoundSource,
        customPath: String?
    ) {
        // iOS actual stub
    }

    actual fun playCustomSound(filePath: String, volumePercent: Int) {
        // iOS actual stub
    }

    actual fun playSystemDefaultSound(volumePercent: Int) {
        // iOS actual stub
    }

    actual fun stopRestTimerSound() {
        // iOS actual stub
    }

    actual fun playHaptic() {
        // Minimal iOS actual
    }

    actual fun cancelRestFinishedNotification() {
        // Minimal iOS actual
    }

    actual fun postRestFinishedNotification(channelVariant: ChannelVariant) {
        // Minimal iOS actual
    }

    actual fun isAppInForeground(): Boolean = true

    actual fun isScreenInteractive(): Boolean = true

    actual fun getRingerMode(): RingerMode = RingerMode.NORMAL

    actual fun isDoNotDisturbActive(): Boolean = false

    actual fun hasNotificationPermission(): Boolean = true
}
