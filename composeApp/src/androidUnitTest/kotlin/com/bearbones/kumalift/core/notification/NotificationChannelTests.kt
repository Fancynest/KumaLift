package com.bearbones.kumalift.core.notification

import com.bearbones.kumalift.core.sound.ChannelVariant
import com.bearbones.kumalift.core.sound.SoundPolicy
import com.bearbones.kumalift.domain.model.SoundProfile
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotificationChannelTests {

    @Test
    fun testChannelSelectionByProfile() {
        assertEquals("rest_timer_ongoing_v2", PlatformNotificationChannels.CHANNEL_REST_TIMER_ONGOING)
        assertEquals("rest_timer_alert_kuma_v2", PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_KUMA)
        assertEquals("rest_timer_alert_vibrate_v2", PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_VIBRATE)
        assertEquals("rest_timer_alert_silent_v2", PlatformNotificationChannels.CHANNEL_REST_TIMER_ALERT_SILENT)

        assertEquals("workout_reminder_kuma_v2", PlatformNotificationChannels.CHANNEL_WORKOUT_REMINDER_KUMA)
        assertEquals("workout_reminder_vibrate_v2", PlatformNotificationChannels.CHANNEL_WORKOUT_REMINDER_VIBRATE)
        assertEquals("workout_reminder_silent_v2", PlatformNotificationChannels.CHANNEL_WORKOUT_REMINDER_SILENT)

        assertEquals("backup_error_v2", PlatformNotificationChannels.CHANNEL_BACKUP_ERROR)

        val policy = SoundPolicy()

        // Rest timer channel variant mapping
        val dKuma = policy.decide(
            event = com.bearbones.kumalift.core.sound.SoundEvent.REST_END,
            soundProfile = SoundProfile.KUMA,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = com.bearbones.kumalift.core.sound.RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true
        )
        assertEquals(ChannelVariant.KUMA, dKuma.notificationChannelVariant)

        val dVibrate = policy.decide(
            event = com.bearbones.kumalift.core.sound.SoundEvent.REST_END,
            soundProfile = SoundProfile.SILENT_VIBRATE,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = com.bearbones.kumalift.core.sound.RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true
        )
        assertEquals(ChannelVariant.VIBRATE, dVibrate.notificationChannelVariant)

        val dSilent = policy.decide(
            event = com.bearbones.kumalift.core.sound.SoundEvent.REST_END,
            soundProfile = SoundProfile.SILENT,
            soundAlerts = true,
            restTimerAlert = true,
            countdownBeeps = false,
            appInForeground = false,
            screenInteractive = false,
            ringerMode = com.bearbones.kumalift.core.sound.RingerMode.NORMAL,
            doNotDisturbActive = false,
            notificationPermissionGranted = true
        )
        assertEquals(ChannelVariant.SILENT, dSilent.notificationChannelVariant)
    }

    @Test
    fun testAllRawAudioResourcesExist() {
        val candidateDirs = listOf(
            File("composeApp/src/androidMain/res/raw"),
            File("src/androidMain/res/raw"),
            File("../composeApp/src/androidMain/res/raw")
        )
        val rawDir = candidateDirs.firstOrNull { it.exists() && it.isDirectory }
        assertTrue(rawDir != null && rawDir.exists(), "Could not locate res/raw directory")

        val expectedAudioFiles = listOf(
            "kumalift_rest_end.ogg",
            "kumalift_tick.ogg",
            "kumalift_set_done.ogg",
            "kumalift_pr.ogg",
            "kumalift_workout_done.ogg",
            "kumalift_reminder.ogg",
            "kumalift_error.ogg"
        )

        for (filename in expectedAudioFiles) {
            val audioFile = File(rawDir, filename)
            assertTrue(
                audioFile.exists() && audioFile.length() > 0L,
                "Required audio asset '$filename' does not exist in res/raw or is empty"
            )
        }
    }
}
