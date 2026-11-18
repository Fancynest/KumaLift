package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume

expect object PlatformSoundPlayer {
    fun playSound(event: SoundEvent, volume: SoundVolume)
    fun playRestTimerSound(
        volume: SoundVolume,
        source: RestTimerSoundSource,
        customPath: String?
    )
    fun playCustomSound(filePath: String, volume: SoundVolume)
    fun playSystemDefaultSound(volume: SoundVolume)
    fun stopRestTimerSound()
    fun playHaptic()
    fun cancelRestFinishedNotification()
    fun postRestFinishedNotification(channelVariant: ChannelVariant)
    fun isAppInForeground(): Boolean
    fun isScreenInteractive(): Boolean
    fun getRingerMode(): RingerMode
    fun isDoNotDisturbActive(): Boolean
    fun hasNotificationPermission(): Boolean
}
