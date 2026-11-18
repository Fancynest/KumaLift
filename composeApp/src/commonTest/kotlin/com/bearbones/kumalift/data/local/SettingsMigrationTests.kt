package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.SoundProfile
import com.bearbones.kumalift.domain.model.SoundVolume
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SettingsMigrationTests {

    @Test
    fun testLegacyAlertSoundDefaultMapsToKuma() {
        val legacyJson = """
            {
                "notifications": {
                    "restTimerSound": "DEFAULT",
                    "restTimerVibrate": true,
                    "monthlyReport": true
                },
                "logging": {
                    "inlineTimer": true,
                    "soundAlerts": true
                }
            }
        """.trimIndent()

        val settings = SettingsMigration.decodeUserSettings(legacyJson)
        assertNotNull(settings)
        assertEquals(SoundProfile.KUMA, settings.notifications.soundProfile)
        assertEquals(50, settings.notifications.soundVolume)
        assertTrue(settings.logging.soundAlerts)

        val migratedJson = SettingsMigration.migrateJson(legacyJson)
        assertFalse(migratedJson.contains("restTimerSound"))
        assertFalse(migratedJson.contains("restTimerVibrate"))
        assertFalse(migratedJson.contains("monthlyReport"))
        assertFalse(migratedJson.contains("inlineTimer"))
    }

    @Test
    fun testLegacyAlertSoundSoftAndBeepMapToKuma() {
        for (legacySound in listOf("SOFT", "BEEP")) {
            val json = """
                {
                    "notifications": {
                        "restTimerSound": "$legacySound",
                        "restTimerVibrate": true
                    }
                }
            """.trimIndent()
            val settings = SettingsMigration.decodeUserSettings(json)
            assertNotNull(settings)
            assertEquals(SoundProfile.KUMA, settings.notifications.soundProfile)
        }
    }

    @Test
    fun testLegacyAlertSoundNoneWithVibrate() {
        // restTimerSound = NONE and restTimerVibrate = true -> SILENT_VIBRATE
        val json = """
            {
                "notifications": {
                    "restTimerSound": "NONE",
                    "restTimerVibrate": true
                }
            }
        """.trimIndent()
        val settings = SettingsMigration.decodeUserSettings(json)
        assertNotNull(settings)
        assertEquals(SoundProfile.SILENT_VIBRATE, settings.notifications.soundProfile)
    }

    @Test
    fun testLegacyAlertSoundNoneWithoutVibrate() {
        // restTimerSound = NONE and restTimerVibrate = false -> SILENT
        val json = """
            {
                "notifications": {
                    "restTimerSound": "NONE",
                    "restTimerVibrate": false
                }
            }
        """.trimIndent()
        val settings = SettingsMigration.decodeUserSettings(json)
        assertNotNull(settings)
        assertEquals(SoundProfile.SILENT, settings.notifications.soundProfile)
    }

    @Test
    fun testUnknownEnumSafeFallback() {
        // Unknown sound enum string does not crash decoding and falls back to KUMA
        val json = """
            {
                "notifications": {
                    "restTimerSound": "NON_EXISTENT_EXPLOSION_SOUND",
                    "soundProfile": "UNKNOWN_PROFILE",
                    "soundVolume": "EXTREME_LOUD"
                }
            }
        """.trimIndent()
        val settings = SettingsMigration.decodeUserSettings(json)
        assertNotNull(settings)
        assertEquals(SoundProfile.KUMA, settings.notifications.soundProfile)
        assertEquals(50, settings.notifications.soundVolume)
    }

    @Test
    fun testMissingSoundVolumeDefaultsToMedium() {
        val json = """
            {
                "notifications": {
                    "soundProfile": "KUMA"
                }
            }
        """.trimIndent()
        val settings = SettingsMigration.decodeUserSettings(json)
        assertNotNull(settings)
        assertEquals(50, settings.notifications.soundVolume)
    }
}
