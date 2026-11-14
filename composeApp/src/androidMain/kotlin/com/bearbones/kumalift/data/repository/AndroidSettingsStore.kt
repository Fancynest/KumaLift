package com.bearbones.kumalift.data.repository

import android.content.Context
import com.bearbones.kumalift.domain.model.AppearanceSettings
import com.bearbones.kumalift.domain.model.AppLanguage
import com.bearbones.kumalift.domain.model.AppTheme
import com.bearbones.kumalift.domain.model.AthleteSex
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.UserSettings
import java.io.File

class AndroidSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("kumalift_user_settings", Context.MODE_PRIVATE)

    fun load(): UserSettings {
        var avatarUri = prefs.getString("profile_avatar_uri", null)
        if (avatarUri != null) {
            val file = File(avatarUri.removePrefix("file://"))
            if (!file.exists() || file.length() == 0L) {
                avatarUri = null
            }
        }
        // Fallback: If preferences don't have the path or were lost, discover any local avatar file in filesDir
        if (avatarUri == null) {
            val files = context.filesDir.listFiles { _, name -> name.startsWith("profile_avatar") }
            val latest = files?.maxByOrNull { it.lastModified() }
            if (latest != null && latest.exists() && latest.length() > 0L) {
                avatarUri = latest.absolutePath
            }
        }

        val name = prefs.getString("profile_name", "") ?: ""
        val username = prefs.getString("profile_username", "") ?: ""
        val bio = prefs.getString("profile_bio", "") ?: ""
        val link = prefs.getString("profile_link", "") ?: ""
        val birthday = prefs.getString("profile_birthday", "") ?: ""
        val sexName = prefs.getString("profile_sex", AthleteSex.UNSPECIFIED.name) ?: AthleteSex.UNSPECIFIED.name
        val sex = try {
            AthleteSex.valueOf(sexName)
        } catch (_: Exception) {
            AthleteSex.UNSPECIFIED
        }
        val bw = if (prefs.contains("profile_bw")) prefs.getFloat("profile_bw", 0f).toDouble() else null
        val ht = if (prefs.contains("profile_ht")) prefs.getFloat("profile_ht", 0f).toDouble() else null

        val themeName = prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        val theme = try {
            AppTheme.valueOf(themeName)
        } catch (_: Exception) {
            AppTheme.SYSTEM
        }
        val highContrast = prefs.getBoolean("app_high_contrast", false)

        val langName = prefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        val language = try {
            AppLanguage.valueOf(langName)
        } catch (_: Exception) {
            AppLanguage.SYSTEM
        }

        return UserSettings(
            profile = ProfileSettings(
                name = name,
                username = username,
                bio = bio,
                link = link,
                birthday = birthday,
                avatarUri = avatarUri,
                sex = sex,
                bodyweightKg = bw,
                heightCm = ht
            ),
            appearance = AppearanceSettings(
                theme = theme,
                highContrast = highContrast,
                language = language
            )
        )
    }

    fun save(settings: UserSettings) {
        prefs.edit().apply {
            putString("profile_name", settings.profile.name)
            putString("profile_username", settings.profile.username)
            putString("profile_bio", settings.profile.bio)
            putString("profile_link", settings.profile.link)
            putString("profile_birthday", settings.profile.birthday)
            putString("profile_avatar_uri", settings.profile.avatarUri)
            putString("profile_sex", settings.profile.sex.name)
            if (settings.profile.bodyweightKg != null) {
                putFloat("profile_bw", settings.profile.bodyweightKg.toFloat())
            } else {
                remove("profile_bw")
            }
            if (settings.profile.heightCm != null) {
                putFloat("profile_ht", settings.profile.heightCm.toFloat())
            } else {
                remove("profile_ht")
            }
            putString("app_theme", settings.appearance.theme.name)
            putBoolean("app_high_contrast", settings.appearance.highContrast)
            putString("app_language", settings.appearance.language.name)
            apply()
        }
    }
}
