package com.bearbones.kumalift.data.local

import com.bearbones.kumalift.domain.model.SoundProfile
import com.bearbones.kumalift.domain.model.SoundVolume
import com.bearbones.kumalift.domain.model.UserSettings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

object SettingsMigration {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Migrates legacy UserSettings JSON payloads to current schema:
     * - Maps legacy AlertSound (DEFAULT, SOFT, BEEP -> KUMA; NONE -> SILENT or SILENT_VIBRATE if restTimerVibrate is true)
     * - Safely falls back unknown enum values to valid defaults (KUMA / MEDIUM)
     * - Strips deprecated keys (restTimerSound, restTimerVibrate, monthlyReport, inlineTimer)
     */
    fun migrateJson(rawJson: String): String {
        return try {
            val rootElement = jsonParser.parseToJsonElement(rawJson).jsonObject
            val migratedRoot = buildJsonObject {
                rootElement.forEach { (key, element) ->
                    when (key) {
                        "notifications" -> {
                            if (element is JsonObject) {
                                put(key, migrateNotifications(element))
                            } else {
                                put(key, element)
                            }
                        }
                        "logging" -> {
                            if (element is JsonObject) {
                                put(key, migrateLogging(element))
                            } else {
                                put(key, element)
                            }
                        }
                        else -> put(key, element)
                    }
                }
            }
            migratedRoot.toString()
        } catch (_: Exception) {
            rawJson
        }
    }

    private fun migrateNotifications(notifs: JsonObject): JsonObject {
        return buildJsonObject {
            var soundProfileName: String? = null
            var soundVolumeName: String? = null

            // Check if legacy restTimerSound exists
            val legacySound = notifs["restTimerSound"]?.jsonPrimitive?.contentOrNull?.uppercase()
            val legacyVibrate = notifs["restTimerVibrate"]?.jsonPrimitive?.booleanOrNull ?: true

            if (legacySound != null) {
                soundProfileName = when (legacySound) {
                    "DEFAULT", "SOFT", "BEEP" -> SoundProfile.KUMA.name
                    "NONE" -> if (legacyVibrate) SoundProfile.SILENT_VIBRATE.name else SoundProfile.SILENT.name
                    else -> SoundProfile.KUMA.name // Safe fallback for unknown enum
                }
            }

            // If soundProfile already present, validate or fallback
            val existingProfile = notifs["soundProfile"]?.jsonPrimitive?.contentOrNull?.uppercase()
            if (existingProfile != null) {
                soundProfileName = try {
                    SoundProfile.valueOf(existingProfile).name
                } catch (_: Exception) {
                    SoundProfile.KUMA.name
                }
            }

            // Validate or default soundVolume
            val existingVolume = notifs["soundVolume"]?.jsonPrimitive?.contentOrNull?.uppercase()
            soundVolumeName = if (existingVolume != null) {
                try {
                    SoundVolume.valueOf(existingVolume).name
                } catch (_: Exception) {
                    SoundVolume.MEDIUM.name
                }
            } else {
                SoundVolume.MEDIUM.name
            }

            // Copy over other fields, omitting deprecated keys
            notifs.forEach { (key, value) ->
                when (key) {
                    "restTimerSound", "restTimerVibrate", "monthlyReport" -> {
                        // Omit deprecated keys
                    }
                    "soundProfile" -> {
                        // Handled below
                    }
                    "soundVolume" -> {
                        // Handled below
                    }
                    else -> put(key, value)
                }
            }

            put("soundProfile", JsonPrimitive(soundProfileName ?: SoundProfile.KUMA.name))
            put("soundVolume", JsonPrimitive(soundVolumeName))
        }
    }

    private fun migrateLogging(logging: JsonObject): JsonObject {
        return buildJsonObject {
            logging.forEach { (key, value) ->
                if (key != "inlineTimer") {
                    put(key, value)
                }
            }
        }
    }

    fun decodeUserSettings(rawJson: String): UserSettings? {
        return try {
            val migrated = migrateJson(rawJson)
            jsonParser.decodeFromString<UserSettings>(migrated)
        } catch (_: Exception) {
            null
        }
    }
}
