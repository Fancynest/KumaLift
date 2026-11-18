package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.RestTimerSoundSource
import com.bearbones.kumalift.domain.model.SoundVolume

expect object PlatformSoundPlayer {
    fun playSound(event: SoundEvent, volumePercent: Int)
    fun playRestTimerSound(
        volumePercent: Int,
        source: RestTimerSoundSource,
        customPath: String?
    )
    fun playCustomSound(filePath: String, volumePercent: Int)
    fun playSystemDefaultSound(volumePercent: Int)
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

fun PlatformSoundPlayer.playSound(event: SoundEvent, volume: SoundVolume) = playSound(event, volume.percent)
fun PlatformSoundPlayer.playRestTimerSound(volume: SoundVolume, source: RestTimerSoundSource, customPath: String?) = playRestTimerSound(volume.percent, source, customPath)
fun PlatformSoundPlayer.playCustomSound(filePath: String, volume: SoundVolume) = playCustomSound(filePath, volume.percent)
fun PlatformSoundPlayer.playSystemDefaultSound(volume: SoundVolume) = playSystemDefaultSound(volume.percent)
