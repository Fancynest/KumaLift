package com.bearbones.kumalift.data.repository

import com.bearbones.kumalift.domain.model.AppearanceSettings
import com.bearbones.kumalift.domain.model.CalculationSettings
import com.bearbones.kumalift.domain.model.DataBackupSettings
import com.bearbones.kumalift.domain.model.EquipmentSettings
import com.bearbones.kumalift.domain.model.HealthSettings
import com.bearbones.kumalift.domain.model.LoggingSettings
import com.bearbones.kumalift.domain.model.NotificationSettings
import com.bearbones.kumalift.domain.model.ProfileSettings
import com.bearbones.kumalift.domain.model.SecuritySettings
import com.bearbones.kumalift.domain.model.UnitSettings
import com.bearbones.kumalift.domain.model.UserSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class SettingsRepository(
    initialSettings: UserSettings = UserSettings(),
    private val onSave: ((UserSettings) -> Unit)? = null
) {
    private val _settings = MutableStateFlow(initialSettings)
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    fun update(transform: (UserSettings) -> UserSettings) {
        _settings.update { current ->
            transform(current)
        }
        onSave?.invoke(_settings.value)
    }

    fun resetAll() {
        update { UserSettings() }
    }

    fun resetProfile() {
        update { it.copy(profile = ProfileSettings()) }
    }

    fun resetUnits() {
        update { it.copy(units = UnitSettings()) }
    }

    fun resetLogging() {
        update { it.copy(logging = LoggingSettings()) }
    }

    fun resetEquipment() {
        update { it.copy(equipment = EquipmentSettings()) }
    }

    fun resetNotifications() {
        update { it.copy(notifications = NotificationSettings()) }
    }

    fun resetCalculations() {
        update { it.copy(calculations = CalculationSettings()) }
    }

    fun resetAppearance() {
        update { it.copy(appearance = AppearanceSettings()) }
    }

    fun resetDataBackup() {
        update { it.copy(dataBackup = DataBackupSettings()) }
    }

    fun resetSecurity() {
        update { it.copy(security = SecuritySettings()) }
    }

    fun resetHealth() {
        update { it.copy(health = HealthSettings()) }
    }
}
