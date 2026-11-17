package com.bearbones.kumalift.core.sound

import com.bearbones.kumalift.domain.model.SoundProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SoundPolicyTests {

    @Test
    fun testRestEndForegroundAndInteractive() {
        val policy = SoundPolicy()
        // Foreground + interactive, KUMA, NORMAL, no DND -> plays in-app sound + light haptic, NO notification
        val decision = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )

        assertTrue(decision.playInAppSound)
        assertEquals(SoundEvent.REST_END, decision.soundEventToPlay)
        assertTrue(decision.vibrate)
        assertFalse(decision.postNotification)
        assertNull(decision.notificationChannelVariant)
    }

    @Test
    fun testRestEndForegroundSilentProfile() {
        val policy = SoundPolicy()
        // Foreground + interactive, SILENT profile -> no in-app sound, no vibrate, NO notification
        val decision = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.SILENT,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )

        assertFalse(decision.playInAppSound)
        assertFalse(decision.vibrate)
        assertFalse(decision.postNotification)
    }

    @Test
    fun testRestEndBackgroundPostsNotificationByProfile() {
        val policy = SoundPolicy()

        // 1. Kuma profile in background
        val dKuma = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )
        assertFalse(dKuma.playInAppSound)
        assertFalse(dKuma.vibrate)
        assertTrue(dKuma.postNotification)
        assertEquals(ChannelVariant.KUMA, dKuma.notificationChannelVariant)

        // 2. Vibrate-only profile in background
        val dVibrate = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.SILENT_VIBRATE,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 2000L
        )
        assertFalse(dVibrate.playInAppSound)
        assertFalse(dVibrate.vibrate)
        assertTrue(dVibrate.postNotification)
        assertEquals(ChannelVariant.VIBRATE, dVibrate.notificationChannelVariant)

        // 3. Silent profile in background
        val dSilent = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.SILENT,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 3000L
        )
        assertFalse(dSilent.playInAppSound)
        assertFalse(dSilent.vibrate)
        assertTrue(dSilent.postNotification)
        assertEquals(ChannelVariant.SILENT, dSilent.notificationChannelVariant)
    }

    @Test
    fun testRestEndBackgroundNotificationPermissionDenied() {
        val policy = SoundPolicy()
        // If notification permission denied in background -> nothing audible, show nothing
        val decision = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = false,
            currentTimeMs = 1000L
        )

        assertFalse(decision.playInAppSound)
        assertFalse(decision.vibrate)
        assertFalse(decision.postNotification)
        assertNull(decision.notificationChannelVariant)
    }

    @Test
    fun testRestEndBackgroundAlertDisabledInSettings() {
        val policy = SoundPolicy()
        val decision = policy.decide(
            event = SoundEvent.REST_END,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = false, // User disabled background rest alert
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )

        assertFalse(decision.postNotification)
    }

    @Test
    fun testTickRules() {
        val policy = SoundPolicy()

        // Valid tick: last 3s, countdownBeeps true, foreground, KUMA, NORMAL, not DND
        val valid = policy.decide(
            event = SoundEvent.TICK,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = true,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L,
            secondsRemaining = 3
        )
        assertTrue(valid.playInAppSound)
        assertEquals(SoundEvent.TICK, valid.soundEventToPlay)
        assertFalse(valid.vibrate)
        assertFalse(valid.postNotification)

        // Invalid: secondsRemaining > 3
        val invalidSeconds = policy.decide(
            event = SoundEvent.TICK,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = true,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 2000L,
            secondsRemaining = 4
        )
        assertFalse(invalidSeconds.playInAppSound)

        // Invalid: in background
        val inBg = policy.decide(
            event = SoundEvent.TICK,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = true,
            appInForeground = false,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 3000L,
            secondsRemaining = 2
        )
        assertFalse(inBg.playInAppSound)

        // Invalid: countdownBeeps disabled
        val beepsDisabled = policy.decide(
            event = SoundEvent.TICK,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 4000L,
            secondsRemaining = 1
        )
        assertFalse(beepsDisabled.playInAppSound)
    }

    @Test
    fun testInAppFeedbackEvents() {
        val policy = SoundPolicy()

        for (event in listOf(SoundEvent.SET_DONE, SoundEvent.PR, SoundEvent.WORKOUT_DONE)) {
            val d = policy.decide(
                event = event,
                soundProfile = SoundProfile.KUMA,
                soundAlerts = true,
                restTimerAlert = true,
                countdownBeeps = false,
                appInForeground = true,
                screenInteractive = true,
                ringerMode = RingerMode.NORMAL,
                doNotDisturbActive = false,
                notificationPermissionGranted = true,
                currentTimeMs = 1000L
            )
            assertTrue(d.playInAppSound, "Event $event should play sound")
            assertEquals(event, d.soundEventToPlay)
            assertTrue(d.vibrate, "Event $event should vibrate")
            assertFalse(d.postNotification)
        }

        // When soundAlerts is false -> sound disabled, but haptic remains active
        val noSound = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = false,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 3000L
        )
        assertFalse(noSound.playInAppSound)
        assertTrue(noSound.vibrate)
    }

    @Test
    fun testErrorEvent() {
        val policy = SoundPolicy()

        // ERROR with KUMA profile
        val dKuma = policy.decide(
            event = SoundEvent.ERROR,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )
        assertFalse(dKuma.playInAppSound)
        assertFalse(dKuma.vibrate)
        assertTrue(dKuma.postNotification)
        assertEquals(ChannelVariant.KUMA, dKuma.notificationChannelVariant)

        // ERROR with SILENT profile -> channel variant SILENT
        val dSilent = policy.decide(
            event = SoundEvent.ERROR,
            soundProfile = SoundProfile.SILENT,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 2000L
        )
        assertEquals(ChannelVariant.SILENT, dSilent.notificationChannelVariant)
    }

    @Test
    fun testReminderEvent() {
        val policy = SoundPolicy()
        val d = policy.decide(
            event = SoundEvent.REMINDER,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )
        assertFalse(d.playInAppSound)
        assertFalse(d.vibrate)
        assertTrue(d.postNotification)
        assertEquals(ChannelVariant.KUMA, d.notificationChannelVariant)
    }

    @Test
    fun testRingerModesAndDnd() {
        val policy = SoundPolicy()

        // RINGER_MODE VIBRATE: no sound, haptic allowed if profile != SILENT
        val dVibrate = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.VIBRATE,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )
        assertFalse(dVibrate.playInAppSound)
        assertTrue(dVibrate.vibrate)

        // RINGER_MODE SILENT: nothing
        val dSilent = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.SILENT,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 2000L
        )
        assertFalse(dSilent.playInAppSound)
        assertFalse(dSilent.vibrate)

        // DND active: nothing
        val dDnd = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = true,
            notificationPermissionGranted = true,
            currentTimeMs = 3000L
        )
        assertFalse(dDnd.playInAppSound)
        assertFalse(dDnd.vibrate)
    }

    @Test
    fun testDebounceRule() {
        val policy = SoundPolicy()

        // First trigger at t=1000ms plays
        val d1 = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1000L
        )
        assertTrue(d1.playInAppSound)

        // Second trigger at t=1800ms (< 1500ms later) is debounced
        val d2 = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 1800L
        )
        assertFalse(d2.playInAppSound)

        // Third trigger at t=2600ms (>= 1500ms after t=1000ms) plays
        val d3 = policy.decide(
            event = SoundEvent.SET_DONE,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = true,
            screenInteractive = true,
            ringerMode = RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true,
            currentTimeMs = 2600L
        )
        assertTrue(d3.playInAppSound)
    }
}
