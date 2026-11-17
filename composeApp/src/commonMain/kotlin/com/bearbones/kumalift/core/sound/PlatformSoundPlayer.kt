package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.SoundVolume

expect object PlatformSoundPlayer {
    fun playSound(event: SoundEvent, volume: SoundVolume)
    fun playHaptic()
    fun cancelRestFinishedNotification()
    fun postRestFinishedNotification(channelVariant: ChannelVariant)
    fun isAppInForeground(): Boolean
    fun isScreenInteractive(): Boolean
    fun getRingerMode(): RingerMode
    fun isDoNotDisturbActive(): Boolean
    fun hasNotificationPermission(): Boolean
}
