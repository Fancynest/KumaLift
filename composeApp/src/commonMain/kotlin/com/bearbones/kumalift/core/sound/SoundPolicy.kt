package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.SoundProfile

enum class SoundEvent {
    REST_END,
    TICK,
    SET_DONE,
    PR,
    WORKOUT_DONE,
    REMINDER,
    ERROR
}

enum class RingerMode {
    NORMAL,
    VIBRATE,
    SILENT
}

enum class ChannelVariant {
    KUMA,
    VIBRATE,
    SILENT
}

data class SoundDecision(
    val playInAppSound: Boolean,
    val soundEventToPlay: SoundEvent? = null,
    val postNotification: Boolean = false,
    val notificationChannelVariant: ChannelVariant? = null,
    val vibrate: Boolean = false
)

/**
 * Pure Kotlin decision engine for sound and notification dispatch.
 * Zero Android dependencies.
 */
class SoundPolicy(
    private val defaultDebounceMs: Long = 1500L
) {
    private val lastPlayedEpochMs = mutableMapOf<SoundEvent, Long>()

    fun resetDebounce() {
        lastPlayedEpochMs.clear()
    }

    fun getDebounceWindowMs(event: SoundEvent): Long {
        return if (event == SoundEvent.TICK) 800L else defaultDebounceMs
    }

    /**
     * Evaluates playback and notification policy against current device & app state.
     */
    fun decide(
        event: SoundEvent,
        soundProfile: SoundProfile,
        soundAlerts: Boolean,
        restTimerAlert: Boolean,
        countdownBeeps: Boolean,
        appInForeground: Boolean,
        screenInteractive: Boolean,
        ringerMode: RingerMode,
        doNotDisturbActive: Boolean,
        notificationPermissionGranted: Boolean,
        currentTimeMs: Long = 0L,
        secondsRemaining: Int? = null,
        debounceOverrideMs: Long? = null
    ): SoundDecision {
        val isForegroundAndInteractive = appInForeground && screenInteractive

        var shouldPlaySound = false
        var shouldVibrate = false
        var shouldPostNotif = false
        var notifVariant: ChannelVariant? = null

        when (event) {
            SoundEvent.REST_END -> {
                if (isForegroundAndInteractive) {
                    // Rule 1: In foreground and interactive -> in-app sound + light haptic, NO sounding notification
                    if (restTimerAlert && soundProfile == SoundProfile.KUMA) {
                        shouldPlaySound = true
                    }
                    if (restTimerAlert && soundProfile != SoundProfile.SILENT) {
                        shouldVibrate = true
                    }
                    shouldPostNotif = false
                    notifVariant = null
                } else {
                    // Rule 2: In background or screen off -> notification only using profile channel, no in-app sound
                    shouldPlaySound = false
                    shouldVibrate = false
                    if (restTimerAlert && notificationPermissionGranted) {
                        shouldPostNotif = true
                        notifVariant = when (soundProfile) {
                            SoundProfile.KUMA -> ChannelVariant.KUMA
                            SoundProfile.SILENT_VIBRATE -> ChannelVariant.VIBRATE
                            SoundProfile.SILENT -> ChannelVariant.SILENT
                        }
                    } else {
                        shouldPostNotif = false
                        notifVariant = null
                    }
                }
            }

            SoundEvent.TICK -> {
                // Rule 3: TICK only in last 3s, only countdownBeeps true, only foreground + interactive,
                // only profile == KUMA and ringer NORMAL, not DND. Never in background.
                val inLast3Seconds = secondsRemaining == null || secondsRemaining in 1..3
                if (inLast3Seconds && countdownBeeps && isForegroundAndInteractive &&
                    soundProfile == SoundProfile.KUMA && ringerMode == RingerMode.NORMAL && !doNotDisturbActive
                ) {
                    shouldPlaySound = true
                }
                shouldVibrate = false
                shouldPostNotif = false
            }

            SoundEvent.SET_DONE, SoundEvent.PR, SoundEvent.WORKOUT_DONE -> {
                // Rule 4: In-app feedback: only soundAlerts true, profile == KUMA, foreground + interactive, ringer NORMAL, not DND.
                // Haptic (light) only if profile != SILENT and not (ringer SILENT) and not DND.
                if (isForegroundAndInteractive) {
                    if (soundAlerts && soundProfile == SoundProfile.KUMA && ringerMode == RingerMode.NORMAL && !doNotDisturbActive) {
                        shouldPlaySound = true
                    }
                    if (soundProfile != SoundProfile.SILENT && ringerMode != RingerMode.SILENT && !doNotDisturbActive) {
                        shouldVibrate = true
                    }
                }
                shouldPostNotif = false
            }

            SoundEvent.ERROR -> {
                // Rule 5: ERROR: notification only, no vibration, uses error sound only if profile == KUMA.
                shouldPlaySound = false
                shouldVibrate = false
                if (notificationPermissionGranted) {
                    shouldPostNotif = true
                    notifVariant = if (soundProfile == SoundProfile.KUMA) ChannelVariant.KUMA else ChannelVariant.SILENT
                }
            }

            SoundEvent.REMINDER -> {
                // Rule 6: REMINDER: notification using reminder channel variant for profile.
                shouldPlaySound = false
                shouldVibrate = false
                if (notificationPermissionGranted) {
                    shouldPostNotif = true
                    notifVariant = when (soundProfile) {
                        SoundProfile.KUMA -> ChannelVariant.KUMA
                        SoundProfile.SILENT_VIBRATE -> ChannelVariant.VIBRATE
                        SoundProfile.SILENT -> ChannelVariant.SILENT
                    }
                }
            }
        }

        // Global override: RingerMode SILENT or DND active suppresses in-app feedback clicks and ticks,
        // while REST_END timer alarm obeys its own SoundProfile.
        if (event != SoundEvent.REST_END) {
            if (ringerMode == RingerMode.SILENT || doNotDisturbActive) {
                shouldPlaySound = false
                shouldVibrate = false
            } else if (ringerMode == RingerMode.VIBRATE) {
                shouldPlaySound = false
            }
        }

        // Rule 8: Debounce in-app sound (1500ms default, never plays twice within debounce window)
        var finalPlayInAppSound = false
        var eventToPlay: SoundEvent? = null
        if (shouldPlaySound) {
            val debounceWindow = debounceOverrideMs ?: getDebounceWindowMs(event)
            val lastPlayed = lastPlayedEpochMs[event]
            if (lastPlayed == null || (currentTimeMs - lastPlayed >= debounceWindow)) {
                finalPlayInAppSound = true
                eventToPlay = event
                lastPlayedEpochMs[event] = currentTimeMs
            }
        }

        return SoundDecision(
            playInAppSound = finalPlayInAppSound,
            soundEventToPlay = eventToPlay,
            postNotification = shouldPostNotif,
            notificationChannelVariant = notifVariant,
            vibrate = shouldVibrate
        )
    }
}
