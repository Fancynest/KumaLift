package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.core.util.currentEpochMillis
import com.bearbones.kumalift.domain.model.SoundVolume
import com.bearbones.kumalift.domain.model.UserSettings

object SoundManager {
    val policy = SoundPolicy()

    fun trigger(
        event: SoundEvent,
        settings: UserSettings,
        secondsRemaining: Int? = null
    ) {
        val appInForeground = PlatformSoundPlayer.isAppInForeground()
        val screenInteractive = PlatformSoundPlayer.isScreenInteractive()
        val ringerMode = PlatformSoundPlayer.getRingerMode()
        val dnd = PlatformSoundPlayer.isDoNotDisturbActive()
        val notifPerm = PlatformSoundPlayer.hasNotificationPermission()

        val decision = policy.decide(
            event = event,
            soundProfile = settings.notifications.soundProfile,
            soundAlerts = settings.logging.soundAlerts,
            restTimerAlert = settings.notifications.restTimerAlert,
            countdownBeeps = settings.notifications.countdownBeeps,
            appInForeground = appInForeground,
            screenInteractive = screenInteractive,
            ringerMode = ringerMode,
            doNotDisturbActive = dnd,
            notificationPermissionGranted = notifPerm,
            currentTimeMs = currentEpochMillis(),
            secondsRemaining = secondsRemaining
        )

        if (event == SoundEvent.REST_END) {
            val volumePercent = settings.notifications.soundVolume
            val volumeMultiplier = (volumePercent / 100f).coerceIn(0f, 1f)

            // Rest timer completion alert:
            // Plays if profile is KUMA, alert is enabled, and volume > 0.
            // Does NOT silence for ringerMode == VIBRATE or DND - athletes expect their rest timer
            // alarm to sound at their configured in-app volume.
            if (settings.notifications.soundProfile == com.bearbones.kumalift.domain.model.SoundProfile.KUMA &&
                settings.notifications.restTimerAlert &&
                volumeMultiplier > 0f
            ) {
                PlatformSoundPlayer.playRestTimerSound(
                    volumePercent,
                    settings.notifications.restTimerSoundSource,
                    settings.notifications.customRestTimerSoundPath
                )
            }

            // Haptic feedback
            if (settings.notifications.soundProfile != com.bearbones.kumalift.domain.model.SoundProfile.SILENT &&
                settings.notifications.restTimerAlert
            ) {
                PlatformSoundPlayer.playHaptic()
            }
            return
        }

        if (decision.playInAppSound && decision.soundEventToPlay != null) {
            PlatformSoundPlayer.playSound(decision.soundEventToPlay, settings.notifications.soundVolume)
        }
        if (decision.vibrate) {
            PlatformSoundPlayer.playHaptic()
        }
        if (decision.postNotification && decision.notificationChannelVariant != null) {
            PlatformSoundPlayer.postRestFinishedNotification(decision.notificationChannelVariant)
        }
    }

    fun previewSound(event: SoundEvent, volumePercent: Int) {
        PlatformSoundPlayer.playSound(event, volumePercent)
    }

    fun previewSound(event: SoundEvent, volume: SoundVolume) {
        PlatformSoundPlayer.playSound(event, volume.percent)
    }

    fun previewCustomSound(filePath: String, volumePercent: Int) {
        PlatformSoundPlayer.playCustomSound(filePath, volumePercent)
    }

    fun previewCustomSound(filePath: String, volume: SoundVolume) {
        PlatformSoundPlayer.playCustomSound(filePath, volume.percent)
    }

    fun previewSystemDefaultSound(volumePercent: Int) {
        PlatformSoundPlayer.playSystemDefaultSound(volumePercent)
    }

    fun previewSystemDefaultSound(volume: SoundVolume) {
        PlatformSoundPlayer.playSystemDefaultSound(volume.percent)
    }

    var onPlaybackStopped: (() -> Unit)? = null

    fun stopSound() {
        PlatformSoundPlayer.stopRestTimerSound()
        onPlaybackStopped?.invoke()
    }

    fun dismissRestFinishedNotification() {
        PlatformSoundPlayer.cancelRestFinishedNotification()
    }
}
