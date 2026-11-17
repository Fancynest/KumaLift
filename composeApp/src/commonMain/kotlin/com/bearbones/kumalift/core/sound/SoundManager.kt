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

    fun previewSound(event: SoundEvent, volume: SoundVolume) {
        PlatformSoundPlayer.playSound(event, volume)
    }

    fun dismissRestFinishedNotification() {
        PlatformSoundPlayer.cancelRestFinishedNotification()
    }
}
