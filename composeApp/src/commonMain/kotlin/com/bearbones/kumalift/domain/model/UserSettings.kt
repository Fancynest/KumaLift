package com.bearbones.kumalift.domain.model

enum class AthleteSex {
    UNSPECIFIED,
    MALE,
    FEMALE
}

enum class TrainingGoal {
    HYPERTROPHY,
    STRENGTH,
    GENERAL
}

enum class WeightUnit {
    KG,
    LB
}

enum class DistanceUnit {
    KILOMETERS,
    MILES
}

enum class BodyMeasurementUnit {
    CM,
    IN
}

enum class LengthUnit {
    CM,
    IN
}

enum class FirstDayOfWeek {
    MONDAY,
    SUNDAY,
    SATURDAY
}

enum class TimeFormatPreference {
    SYSTEM,
    H12,
    H24
}

enum class DateFormatPreference {
    SYSTEM,
    DMY,
    MDY,
    YMD
}

enum class WorkoutTimerStart {
    ON_START,
    ON_FIRST_SET
}

enum class PrefillSource {
    LAST_SESSION,
    BEST_SET,
    NONE
}

enum class IntensityScale {
    RPE,
    RIR
}

data class BarEquipment(
    val id: String,
    val name: String,
    val weightKg: Double
)

data class PlateItem(
    val weight: Double,
    val enabled: Boolean
)

enum class AlertSound {
    DEFAULT,
    SOFT,
    BEEP,
    NONE
}

enum class OneRepMaxFormula {
    EPLEY,
    BRZYCKI,
    LOMBARDI,
    OCONNER
}

enum class PrMetricType {
    WEIGHT,
    E1RM,
    SET_VOLUME
}

enum class BodyweightVolumeMode {
    ADDED_ONLY,
    BODYWEIGHT_PLUS_ADDED
}

enum class AppTheme {
    DARK,
    LIGHT,
    SYSTEM
}

enum class AppLanguage {
    SYSTEM,
    ID,
    EN
}

enum class AppTextScale {
    SYSTEM,
    SMALL,
    DEFAULT,
    LARGE,
    XLARGE
}

enum class BackupFrequency {
    DAILY,
    WEEKLY,
    MONTHLY
}

enum class AppLockType {
    NONE,
    BIOMETRIC,
    PIN
}

enum class LockTimeout {
    IMMEDIATE,
    ONE_MIN,
    FIVE_MIN,
    FIFTEEN_MIN
}

data class ProfileSettings(
    val name: String = "",
    val username: String = "",
    val bio: String = "",
    val link: String = "",
    val birthday: String = "",
    val avatarUri: String? = null,
    val bodyweightKg: Double? = null,
    val heightCm: Double? = null,
    val birthYear: Int? = null,
    val sex: AthleteSex = AthleteSex.UNSPECIFIED,
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val goal: TrainingGoal = TrainingGoal.HYPERTROPHY,
    val trainingDaysPerWeek: Int = 4
)

data class UnitSettings(
    val weightUnit: WeightUnit = WeightUnit.KG,
    val distanceUnit: DistanceUnit = DistanceUnit.KILOMETERS,
    val bodyMeasurementUnit: BodyMeasurementUnit = BodyMeasurementUnit.CM,
    val lengthUnit: LengthUnit = LengthUnit.CM,
    val firstDayOfWeek: FirstDayOfWeek = FirstDayOfWeek.MONDAY,
    val timeFormat: TimeFormatPreference = TimeFormatPreference.SYSTEM,
    val dateFormat: DateFormatPreference = DateFormatPreference.SYSTEM
)

data class LoggingSettings(
    val restTimerDefaultSec: Int = 90,
    val restTimerSeparateByType: Boolean = false,
    val restTimerCompoundSec: Int = 150,
    val restTimerIsolationSec: Int = 75,
    val restTimerAutoStart: Boolean = true,
    val restTimerAddStepSec: Int = 30,
    val restTimerSubtractStepSec: Int = 15,
    val workoutTimerStart: WorkoutTimerStart = WorkoutTimerStart.ON_START,
    val weightStepKgBarbell: Double = 2.5,
    val weightStepKgDumbbell: Double = 2.0,
    val weightStepKgCable: Double = 2.5,
    val weightStepKgMachine: Double = 2.5,
    val weightStepKgBodyweight: Double = 0.5,
    val repStep: Int = 1,
    val defaultSetCount: Int = 3,
    val prefillSource: PrefillSource = PrefillSource.LAST_SESSION,
    val showPreviousColumn: Boolean = true,
    val trackIntensity: Boolean = true,
    val intensityScale: IntensityScale = IntensityScale.RPE,
    val enabledSetTypes: Set<SetType> = SetType.entries.toSet(),
    val compactSetRows: Boolean = false,
    val keepScreenOn: Boolean = true,
    val confirmFinish: Boolean = true,
    val staleWorkoutHours: Int = 6,
    val soundAlerts: Boolean = true,
    val plateCalculatorEnabled: Boolean = true,
    val smartSupersetScrolling: Boolean = false,
    val inlineTimer: Boolean = true
)

data class EquipmentSettings(
    val bars: List<BarEquipment> = listOf(
        BarEquipment("olympic", "Olympic Bar", 20.0),
        BarEquipment("women", "Women's Bar", 15.0),
        BarEquipment("ez", "EZ Bar", 10.0)
    ),
    val defaultBarId: String = "olympic",
    val platesKg: List<PlateItem> = listOf(
        PlateItem(25.0, true),
        PlateItem(20.0, true),
        PlateItem(15.0, true),
        PlateItem(10.0, true),
        PlateItem(5.0, true),
        PlateItem(2.5, true),
        PlateItem(1.25, true),
        PlateItem(0.5, true)
    ),
    val platesLb: List<PlateItem> = listOf(
        PlateItem(45.0, true),
        PlateItem(35.0, true),
        PlateItem(25.0, true),
        PlateItem(10.0, true),
        PlateItem(5.0, true),
        PlateItem(2.5, true)
    ),
    val showPlateCalculator: Boolean = true,
    val plateCalculatorRoundingKg: Boolean = true
)

data class NotificationSettings(
    val restTimerAlert: Boolean = true,
    val restTimerSound: AlertSound = AlertSound.DEFAULT,
    val restTimerVibrate: Boolean = true,
    val countdownBeeps: Boolean = false,
    val ongoingWorkout: Boolean = true,
    val workoutReminders: Boolean = false,
    val reminderDays: Set<Int> = setOf(1, 2, 4, 5), // Mon, Tue, Thu, Fri
    val reminderTime: String = "17:00",
    val streakReminder: Boolean = false,
    val streakReminderTime: String = "20:00",
    val quietHoursEnabled: Boolean = false,
    val quietHoursStart: String = "22:00",
    val quietHoursEnd: String = "06:00",
    val monthlyReport: Boolean = true
)

data class CalculationSettings(
    val oneRepMaxFormula: OneRepMaxFormula = OneRepMaxFormula.EPLEY,
    val maxRepsForEstimate: Int = 10,
    val prMetrics: Set<PrMetricType> = setOf(PrMetricType.WEIGHT, PrMetricType.E1RM),
    val repRangePrs: Boolean = true,
    val includeWarmupInVolume: Boolean = false,
    val includeDropSetsInVolume: Boolean = true,
    val includeFailureSetsInVolume: Boolean = true,
    val bodyweightVolumeMode: BodyweightVolumeMode = BodyweightVolumeMode.ADDED_ONLY,
    val weeklyVolumeTargetKg: Double? = null
)

data class AppearanceSettings(
    val theme: AppTheme = AppTheme.DARK,
    val highContrast: Boolean = false,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val textScale: AppTextScale = AppTextScale.SYSTEM,
    val reduceMotion: Boolean = false,
    val haptics: Boolean = true,
    val leftHandedMode: Boolean = false,
    val showWeeklyHorizon: Boolean = true,
    val showMonthlyGlance: Boolean = true,
    val showRecentWorkouts: Boolean = true,
    val recentWorkoutsCount: Int = 5
)

data class DataBackupSettings(
    val autoBackupEnabled: Boolean = false,
    val autoBackupFrequency: BackupFrequency = BackupFrequency.WEEKLY,
    val autoBackupFolderUri: String? = null,
    val autoBackupKeepLast: Int = 5,
    val backupReminderDays: Int = 30,
    val lastBackupEpochMs: Long? = null
)

data class SecuritySettings(
    val appLock: AppLockType = AppLockType.NONE,
    val pinHash: String? = null,
    val lockTimeout: LockTimeout = LockTimeout.IMMEDIATE,
    val hideInAppSwitcher: Boolean = false,
    val localLogging: Boolean = false
)

data class HealthSettings(
    val healthEnabled: Boolean = false,
    val writeWorkouts: Boolean = true,
    val writeBodyWeight: Boolean = false,
    val readBodyWeight: Boolean = false
)

data class UserSettings(
    val settingsVersion: Int = 1,
    val profile: ProfileSettings = ProfileSettings(),
    val units: UnitSettings = UnitSettings(),
    val logging: LoggingSettings = LoggingSettings(),
    val equipment: EquipmentSettings = EquipmentSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val calculations: CalculationSettings = CalculationSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val dataBackup: DataBackupSettings = DataBackupSettings(),
    val security: SecuritySettings = SecuritySettings(),
    val health: HealthSettings = HealthSettings()
)
