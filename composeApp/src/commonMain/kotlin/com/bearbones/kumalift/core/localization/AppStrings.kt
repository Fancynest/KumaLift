package com.bearbones.kumalift.core.localization

/**
 * Type-safe string contract for KumaLift UI and user-facing messages.
 * Missing translations in either English or Indonesian will trigger compile errors.
 */
interface AppStrings {
    // Common Actions & Words
    val appName: String
    val back: String
    val save: String
    val cancel: String
    val close: String
    val done: String
    val edit: String
    val delete: String
    val remove: String
    val add: String
    val search: String
    val finish: String
    val all: String
    val yes: String
    val no: String
    val custom: String
    val never: String
    val recorded: String
    val athleteDefault: String
    val athleteUsernameDefault: String
    val unitKg: String
    val unitLb: String
    val minAbbrev: String
    val daysAbbrev: String
    val repsAbbrev: String
    val setsAbbrev: String

    // Bottom Navigation & Mini-bar
    val tabHome: String
    val tabWorkouts: String
    val tabExercises: String
    val tabProgress: String
    val tabProfile: String
    val workoutInProgress: String
    val resume: String

    // Home Screen
    val greetingMorning: String
    val greetingAfternoon: String
    val greetingEvening: String
    val phaseHypertrophy: String
    val phaseStrength: String
    val phaseGeneral: String
    val phaseEndurance: String
    val phaseFatLoss: String
    val programEndurance: String
    val programFatLoss: String
    val goalEndurance: String
    val goalFatLoss: String
    val routineDetailTitle: String
    val editRoutineButton: String
    val editRoutineTitle: String
    val moveUpAction: String
    val moveDownAction: String
    val pauseWorkoutButton: String
    val resumeWorkoutButton: String
    val cancelWorkoutButton: String
    val cancelWorkoutDialogTitle: String
    val cancelWorkoutDialogMessage: String
    val trainingScheduleTitle: String
    val trainingGoalTitle: String
    val exerciseNotesLabel: String
    val exerciseNotesPlaceholder: String
    fun volumeThisWeek(vol: String): String
    fun goalProgress(pct: Int, current: Int, target: Int): String
    fun prRecordsCount(count: Int): String
    val backupReminderTitle: String
    val backupReminderBody: String
    val backupButton: String
    val thisWeekLabel: String
    fun workoutsTargetCount(current: Int, target: Int): String
    val daysMondayStart: List<String>
    val daysSundayStart: List<String>
    val daysSaturdayStart: List<String>
    val todaysSchedule: String
    val readyToTrain: String
    val customRoutine: String
    fun routineSubtitle(exercisesCount: Int, mins: Int): String
    val startEmptyOrPickHint: String
    val startWorkoutButton: String
    val freestyleWorkoutName: String
    val totalSessionsLabel: String
    val totalSessionsSubtext: String
    val totalVolumeLabel: String
    val totalVolumeSubtext: String
    val recordsPrLabel: String
    val recordsPrSubtext: String
    val recentWorkoutsLabel: String
    val noRecentWorkouts: String
    val freestyleWorkoutCardTitle: String
    val freestyleWorkoutCardSubtitle: String

    // Exercise Catalog & Detail
    val exercisesTitle: String
    val createExerciseButton: String
    val searchExerciseHint: String
    val allMusclesFilter: String
    val allEquipmentFilter: String
    val allTypesFilter: String
    fun exercisesCountHeader(count: Int): String
    val resetFilters: String
    val addExerciseToWorkout: String
    val summaryTab: String
    val howToTab: String
    fun targetMuscleLabel(muscle: String): String
    fun secondaryMusclesLabel(muscles: String): String
    val stabilizerMusclesDefault: String
    fun howToLogEquipmentTitle(equipment: String): String
    val noWorkoutLogsYet: String
    val heaviestWeightTab: String
    val oneRepMaxTab: String
    val bestSetVolumeTab: String
    val personalRecordsSection: String
    val pinnedNoteSection: String
    val pinnedNotePlaceholder: String
    val saveNoteButton: String
    val instructionsHeader: String
    fun howToLogDialogTitle(equipment: String): String
    fun howToLogDialogBody(equipment: String): String
    val strengthLevelTitle: String
    val strengthLevelHelpContentDesc: String
    val strengthLevelHelpDialog: String
    val strengthLevelHint: String
    val stepByStepHeader: String
    fun pinnedNoteAutoHint(exerciseName: String): String
    val pinnedNoteInputPlaceholder: String
    val prHelpContentDesc: String
    val prHelpDialog: String
    val infoTitle: String
    val understandButton: String
    val defaultStep1: String
    val defaultStep2: String
    val defaultStep3: String
    val defaultStep4: String

    // Create Exercise Form
    val createExerciseTitle: String
    val exerciseNameLabel: String
    val exerciseNamePlaceholder: String
    val exerciseNameRequiredError: String
    val equipmentLabel: String
    val equipmentSub: String
    val primaryMuscleLabel: String
    val primaryMuscleSub: String
    val secondaryMusclesLabel: String
    val secondaryMusclesSub: String
    val movementTypeLabel: String
    val movementTypeSub: String
    val movementCompoundDesc: String
    val movementIsolationDesc: String

    // Routines Screen & Create Routine
    val workoutRoutinesTitle: String
    val createRoutineButton: String
    val trainingProgramHeader: String
    val weeklyAdherenceLabel: String
    val allProgramsCategory: String
    val programHypertrophy: String
    val programStrength: String
    val programGeneral: String
    fun adherenceProgress(current: Int, target: Int): String
    val startRoutineButton: String
    val newBadge: String
    fun lastPerformedDaysAgo(days: String): String
    val deleteRoutineContentDesc: String
    val deleteRoutineDialogTitle: String
    val deleteRoutineDialogMessage: String
    val createRoutineTitle: String
    val routineNameLabel: String
    val routineNamePlaceholder: String
    val routineNameRequiredError: String
    val routineCategoryLabel: String
    val targetRpeTierLabel: String
    val exercisesInRoutineHeader: String
    val addExerciseToRoutineButton: String
    val addAnotherExerciseButton: String
    val noExercisesInRoutineYet: String
    val reorderExercisesHint: String
    val customCategoryPlaceholder: String
    val targetRpeSub: String
    val deleteRoutineExerciseContentDesc: String
    val exercisePickerTitle: String
    val exercisePickerSearchPlaceholder: String
    val exercisePickerAddedBadge: String
    val estimatedDurationLabel: String
    val targetIntensityLabel: String
    val muscleFocusLabel: String
    val autoLabel: String
    val exercisesInRoutineRequiredError: String
    fun exercisesFoundCount(count: Int): String
    val rpeTiers: List<String>
    val presetRoutineCategories: List<String>
    val routineActiveDaysLabel: String
    val routineActiveDaysSubtitle: String

    // Multi Training Programs
    val trainingProgramsTitle: String
    val newProgramButton: String
    val createProgramTitle: String
    val editProgramTitle: String
    val programNameLabel: String
    val programNamePlaceholder: String
    val programNameRequiredError: String
    val programDescriptionLabel: String
    val programDescriptionPlaceholder: String
    val programCategoryLabel: String
    val programTargetDaysLabel: String
    val daysPerWeek: String
    val activeBadge: String
    val useProgramButton: String
    val switchProgramHint: String
    val switchProgramButton: String
    val activeProgramCardSubtitle: String
    val deleteProgramTitle: String
    fun deleteProgramMessage(name: String, routineCount: Int): String
    val cannotDeleteOnlyProgram: String
    fun routinesInProgramCount(count: Int): String
    val noRoutinesInProgramYet: String
    fun createRoutineForProgramHint(name: String): String
    val programActiveDaysLabel: String
    val maxDaysNotice: String
    val minDaysNotice: String
    val restDayTitle: String
    val restDayAllProgramsMessage: String
    fun restDayProgramMessage(name: String): String

    // Active Workout Logger
    val workoutLogTitle: String
    val finishWorkoutButton: String
    val activeWorkoutBadge: String
    val completedTargetLabel: String
    fun completedSetsProgress(completed: Int, total: Int, pct: Int): String
    val addExerciseButton: String
    val finishWorkoutModalTitle: String
    val finishWorkoutModalMessage: String
    val minimizeWorkoutModalTitle: String
    val minimizeWorkoutModalMessage: String
    val stayButton: String
    val minimizeButton: String
    val noActiveSessionMessage: String
    val setColumnHeader: String
    val prevColumnHeader: String
    fun weightColumnHeader(unit: String): String
    val repsColumnHeader: String
    val rpeColumnHeader: String
    val checkColumnHeader: String
    val addSetButton: String
    val restTimerHeader: String
    val skipRestButton: String
    val replaceExerciseAction: String
    val removeExerciseAction: String

    // Workout Summary
    val workoutSummaryHeader: String
    val workoutCompletedTitle: String
    val workoutCompletedSubtitle: String
    val saveAndCloseButton: String
    val workoutDurationStat: String
    val activeSessionSubtext: String
    val totalVolumeStat: String
    val totalTonnageSubtext: String
    val totalSetsStat: String
    fun completedPercent(pct: Int): String
    val targetRestStat: String
    fun secondsShort(sec: Int): String
    val perSetIntervalSubtext: String
    val prsAchievedStat: String
    val newRecordPrBadge: String
    val trainedMuscleFocusHeader: String
    val noCompletedVolumeRecord: String
    val saveAndFinishSessionButton: String
    val todaySummaryPrefix: String
    val muscleDistributionHeader: String

    // Progress & Analytics
    val progressAndAnalyticsTitle: String
    val estimatedOneRepMaxHeader: String
    val noPrsLoggedYet: String
    val weeklyVolumeTonnageHeader: String
    val noVolumeHistoryYet: String
    val personalRecordsHeader: String
    val noPersonalRecordsYet: String
    val metricDurationLabel: String
    val metricVolumeLabel: String
    val metricRepsLabel: String
    val avgPrefix: String
    fun avgMinutesLabel(mins: Int): String
    val prRecordsStatLabel: String
    fun volumeStatLabel(unit: String): String
    val workoutDurationMetricLabel: String
    val totalVolumeLiftedMetricLabel: String
    val totalRepetitionsMetricLabel: String
    val noWorkoutDataChartMessage: String

    // Profile (Me) Screen
    val profileTitle: String
    val dashboardSection: String
    val statisticsCardTitle: String
    val statisticsCardSubtitle: String
    val exercisesCardTitle: String
    val exercisesCardSubtitle: String
    val measuresCardTitle: String
    val measuresCardSubtitle: String
    val calendarCardTitle: String
    fun trainingDaysSubtitle(days: Int): String
    val allWorkoutsHeader: String
    val viewAllButton: String
    val noWorkoutsCompletedYet: String
    val editProfileTitle: String
    val profileNameInputLabel: String
    val profileNamePlaceholder: String
    val usernameInputLabel: String
    val targetDaysInputLabel: String
    val trainingGoalLabel: String
    val bodyMeasuresTitle: String
    val bodyWeightMeasureLabel: String
    val bodyFatMeasureLabel: String
    val chestMeasureLabel: String
    val waistMeasureLabel: String
    val armsMeasureLabel: String
    val thighsMeasureLabel: String
    val saveMeasuresButton: String
    val adjustPhotoTitle: String
    val adjustPhotoDescription: String
    val zoomLabel: String
    val usePhotoButton: String
    val changePictureButton: String
    val publicProfileSection: String
    val profileBioFieldPlaceholder: String
    val profileBioPlaceholder: String
    val privateProfileSection: String
    val sexFieldLabel: String
    val sexMale: String
    val sexFemale: String
    val birthdayFieldLabel: String
    val birthdayFieldPlaceholder: String
    val bodyHeightMeasureLabel: String
    fun workoutSummaryMovements(count: Int): String
    val updateButton: String

    // Settings Screens
    val settingsTitle: String
    val accountGroupTitle: String
    val preferencesGroupTitle: String
    val systemGroupTitle: String
    val dangerZoneGroupTitle: String
    val profileSettingsRow: String
    val accountSettingsRow: String
    val notificationsSettingsRow: String
    val workoutsSettingsRow: String
    val unitsSettingsRow: String
    val languageSettingsRow: String
    val themeSettingsRow: String
    val exportImportSettingsRow: String
    val aboutLicensesSettingsRow: String

    // Settings - Account
    val memberStatusLabel: String
    val memberFreeOffline: String
    val offlineModeLabel: String
    val offlineModeDescription: String
    val userIdLabel: String
    val deviceIdLabel: String

    // Settings - Notifications
    val dailyWorkoutReminderTitle: String
    val dailyWorkoutReminderSubtitle: String
    val notificationPermissionRequiredTitle: String
    val notificationPermissionRequiredBody: String
    val openNotificationSettingsButton: String
    val reminderTimeLabel: String
    val activeDaysLabel: String
    val reminderDaysCount: String

    // Settings - Workouts & Logging
    val warmupsOptionTitle: String
    val warmupsOptionSubtitle: String
    val dropSetsOptionTitle: String
    val dropSetsOptionSubtitle: String
    val rpeOptionTitle: String
    val rpeOptionSubtitle: String
    val autoRestTimerTitle: String
    val autoRestTimerSubtitle: String
    val defaultRestTimeLabel: String
    val soundVibrationTitle: String
    val soundVibrationSubtitle: String
    val confirmFinishTitle: String
    val confirmFinishSubtitle: String
    val staleWorkoutHoursLabel: String
    val staleWorkoutHoursSubtitle: String

    // Settings - Units
    val weightUnitTitle: String
    val distanceUnitTitle: String
    val sizeUnitTitle: String
    val firstDayOfWeekTitle: String
    val dateFormatTitle: String

    // Settings - Language & Theme
    val selectLanguageTitle: String
    val systemDefaultLanguage: String
    val indonesianLanguage: String
    val englishLanguage: String
    val appearanceThemeTitle: String
    val themeSystem: String
    val themeDark: String
    val themeLight: String
    val highContrastTitle: String
    val highContrastSubtitle: String

    // Settings - Export & Import
    val jsonBackupHeader: String
    val exportJsonBackupTitle: String
    val exportJsonBackupSubtitle: String
    val restoreBackupTitle: String
    val restoreBackupSubtitle: String
    val restoreSafetySnapshotTitle: String
    val restoreSafetySnapshotSubtitle: String
    val csvExportHeader: String
    val exportCsvTitle: String
    val exportCsvSubtitle: String
    val storageStatusHeader: String
    val storageUsedTitle: String
    fun storageUsedSubtitle(workouts: Int, sets: Int): String
    val lastBackupTitle: String
    val clearHistoryTitle: String
    val clearHistorySubtitle: String
    val deleteAllDataTitle: String
    val deleteAllDataSubtitle: String
    val restoreModeDialogTitle: String
    val restoreModeDialogMessage: String
    val mergeOptionButton: String
    val replaceOptionButton: String
    val clearHistoryConfirmTitle: String
    val clearHistoryConfirmMessage: String
    val deleteAllDataConfirmTitle: String
    val deleteAllDataConfirmMessage: String

    // Settings - About
    val aboutTitle: String
    val mediaAttributionHeader: String
    val openSourceLicensesHeader: String
    val exerciseMediaAttributionTitle: String
    val exerciseMediaAttributionBody: String

    // Backup & Restore Messages
    fun backupInvalidJson(error: String): String
    fun backupNewerVersion(version: Int): String
    fun backupReplaceSuccess(exercises: Int, routines: Int, prs: Int, workouts: Int): String
    fun backupMergeSuccess(exercises: Int, routines: Int, prs: Int, workouts: Int): String
    val backupNoDataToExport: String
    val snapshotRestoredSuccess: String
    val noSnapshotAvailable: String
    val snapshotSavedSuccess: String

    // Notifications (Android OS)
    val notifChannelName: String
    val notifChannelDescription: String
    val notifReminderTitle: String
    val notifReminderBody: String
    val notificationVariations: List<Pair<String, String>>

    // Empty states & Dialogs
    val noRoutinesMessage: String
    val createFirstRoutineHint: String
    val noMeasuresRecorded: String
    val editWeightTitle: String
    val editRepsTitle: String
    val enterValidWeightHint: String
    val enterValidRepsHint: String

    // Enums - Muscle Groups
    val muscleChest: String
    val muscleBack: String
    val muscleLegs: String
    val muscleShoulders: String
    val muscleArms: String
    val muscleCore: String
    val muscleCardio: String
    val muscleNeck: String
    val muscleFullBody: String

    // Enums - Equipment
    val equipBarbell: String
    val equipDumbbell: String
    val equipCable: String
    val equipBodyweight: String
    val equipMachine: String
    val equipSmithMachine: String
    val equipBand: String
    val equipKettlebell: String
    val equipOther: String

    // Enums - Movement Types
    val movementCompound: String
    val movementIsolation: String

    // Enums - Experience Levels
    val levelBeginner: String
    val levelIntermediate: String
    val levelAdvanced: String

    // Sub-Muscles mapping (19 items from dataset)
    fun subMuscleLabel(code: String): String

    // Enums - Set Types
    val setTypeWarmup: String
    val setTypeNormal: String
    val setTypeDrop: String
    val setTypeFailure: String

    // Settings - Additional Strings
    val generalSectionTitle: String
    val monthlyReportTitle: String
    val monthlyReportSubtitle: String
    val selectCustomReminderTimeTitle: String
    val keepAwakeTitle: String
    val keepAwakeSubtitle: String
    val plateCalculatorTitle: String
    val plateCalculatorSubtitle: String
    val smartSupersetTitle: String
    val smartSupersetSubtitle: String
    val inlineTimerTitle: String
    val inlineTimerSubtitle: String
    val calculationsHeader: String
    val estimated1RmFormulaTitle: String
    val maxRepsFor1RmTitle: String
    val countWarmupInVolumeTitle: String
    val countWarmupInVolumeSubtitle: String
    val selectUnitsTitle: String
    val distanceUnitMiles: String
    val distanceUnitKm: String
    val sizeUnitIn: String
    val sizeUnitCm: String
    val selectThemeTitle: String
    val systemLanguageTitle: String
    val systemLanguageSubtitle: String
    val indonesianLanguageSubtitle: String
    val englishLanguageSubtitle: String
    val restoreDataDialogTitle: String
    val restoreDataDialogBody: String
    val replaceFullButton: String
    val mergeButton: String
    val deleteHistoryButton: String
    val deleteEverythingButton: String
    val clearHistoryDialogBody: String
    val deleteAllDataDialogBody: String
    val backupRestoreLocalHeader: String
    val exportBackupJsonTitle: String
    val exportBackupJsonSubtitle: String
    val restoreFromBackupTitle: String
    val restoreFromBackupSubtitle: String
    val restoreSafetySnapshotSub: String
    val exportWorkoutsCsvTitle: String
    val exportWorkoutsCsvSubtitle: String
    val clearWorkoutHistoryTitle: String
    val clearWorkoutHistorySubtitle: String
    val deleteAllDataActionTitle: String
    val deleteAllDataActionSubtitle: String

    // File Picker & Backup Extra
    val backupFileSavedSuccess: String
    fun failedToWriteBackupFile(error: String): String
    val backupFileSavingCancelled: String
    fun failedToOpenFilePicker(error: String): String
    val importCancelledOrNoFile: String
    val databaseRepoUnavailable: String
    val failedToRestoreBackup: String
    val failedToMergeBackup: String
    val workoutHistoryClearedSuccess: String
    val allDataDeletedSuccess: String

    // Diagnostic Logging & Set Management
    val diagnosticLogHeader: String
    val localLoggingTitle: String
    val localLoggingSubtitle: String
    val exportDiagnosticLogTitle: String
    val exportDiagnosticLogSubtitle: String
    val clearDiagnosticLogTitle: String
    val clearDiagnosticLogSubtitle: String
    val diagnosticLogCleared: String
    val noDiagnosticLogsYet: String
    val removeLastSetAction: String

    // Advanced Features: Plate Calc, Warm-up, Progression, Recovery, Superset, Onboarding, Backup
    val barWeight: String
    val totalWeight: String
    val loadEachSide: String
    val warmupSetsTitle: String
    val addWarmupSetsAction: String
    val warmupSetGenerated: String
    val progressionReady: String
    val applyProgressionAction: String
    val muscleRecoveryTitle: String
    val recoveryFresh: String
    val recoveryRecovering: String
    val recoveryFatigued: String
    val recoveryDisclaimer: String
    val supersetLabel: String
    val onboardingSkip: String
    val onboardingStepGoal: String
    val onboardingStepDays: String
    val onboardingStepExp: String
    val onboardingFinish: String

    // E1 & E2 Backup & Encryption
    val autoBackupHeader: String
    val autoBackupToFolderTitle: String
    val autoBackupToFolderSubtitle: String
    val backupNowChooseLocation: String
    val backupNowChooseLocationSubtitle: String
    val restoreBackupChooseLocation: String
    val restoreBackupChooseLocationSubtitle: String
    val autoBackupFrequencyLabel: String
    val backupFrequencyDaily: String
    val backupFrequencyWeekly: String
    val backupFrequencyMonthly: String
    val autoBackupKeepLastLabel: String
    val autoBackupFolderLabel: String
    val autoBackupSelectFolder: String
    val autoBackupNoFolderSelected: String
    fun autoBackupStatusOk(timestamp: String): String
    fun autoBackupStatusFailed(error: String): String
    val autoBackupNeedsAttention: String
    val autoBackupFailureNotificationTitle: String
    val autoBackupFailureNotificationBody: String
    val autoBackupRunNow: String
    val manualExportGoogleDriveNote: String
    val autoBackupFolderInaccessible: String
    val autoBackupFolderNotConfigured: String
    val encryptBackupsTitle: String
    val encryptBackupsSubtitle: String
    val setBackupPasswordTitle: String
    val changeBackupPasswordTitle: String
    val backupPasswordWarning: String
    val passwordFieldLabel: String
    val confirmPasswordFieldLabel: String
    val passwordMismatchError: String
    val enterPasswordPrompt: String
    val decryptionFailedError: String

    // E3 Language Cleanup Additions
    val dayNamesFull: List<String>
    val dayNamesShort: List<String>
    val onboardingGoalDescription: String
    val onboardingDaysDescription: String
    val onboardingPickTrainingDays: String
    val onboardingExpDescription: String
    fun onboardingNextStep(step: Int): String
    val routinePushDayTitle: String
    val routinePullDayTitle: String
    val routineLegDayTitle: String
    val routineFullBodyTitle: String
    val routineUpperLowerTitle: String

    val scheduleFrequencyDescription: String
    val scheduleQuickPatterns: String
    fun schedulePickDaysHeader(count: Int): String
    val scheduleSummaryHeader: String
    fun scheduleDaysPerWeekSummary(count: Int): String

    val setTypeWarmupBadge: String
    val setTypeDropBadge: String
    val setTypeFailureBadge: String
    fun setTypeNormalBadge(setNumber: Int): String
    val setTypeWarmupMenuLabel: String
    val setTypeDropMenuLabel: String
    val setTypeFailureMenuLabel: String
    fun setTypeNormalMenuLabel(setNumber: Int): String

    val bareBarbellLabel: String
    fun supersetBadge(id: String): String
    val supersetGroupAction: String
    val supersetUngroupAction: String
    val dragToReorder: String
    fun exerciseCountLabel(count: Int): String
    val progressiveLabel: String
    fun secondsRestLabel(seconds: Int): String

    val restTimerNotificationTitle: String
    val restTimerNotificationText: String
    val restTimerChannelName: String
    val restTimerChannelDescription: String
    val restTimerRestingSubtitle: String
    val restTimerActionAdd30: String
    val restTimerActionSkip: String

    val recoveryWindow72h: String
    val recoveryDialogSubtitle: String
    fun recoveryStatusFormat(status: String, pct: Int): String
    fun recoveryTrainedAgo(hours: Int, sets: Int): String
    val recoveryNotTrainedIn72h: String

    val editProfileContentDesc: String
    val settingsContentDesc: String
    val targetLabel: String
    val hoursLabel: String
    val minutesLabel: String
    fun diagnosticLogEntriesCount(count: Int): String
    fun prCountLabel(count: Int): String
    val prBadge: String

    val muscleFocusPush: String
    val muscleFocusPull: String
    val muscleFocusCompound: String
    val muscleFocusConditioning: String

    val progressionHypertrophy: String
    val guidelineHypertrophy: String
    val progressionStrength: String
    val guidelineStrength: String
    val progressionEndurance: String
    val guidelineEndurance: String
    val progressionFatLoss: String
    val guidelineFatLoss: String
    val progressionGeneral: String
    val guidelineGeneral: String

    val customRestTimerTitle: String
    val setTimerButton: String
    val editPreviousTitle: String
    val previousWeightLabel: String
    val previousRepsLabel: String
    val selectRpeTitle: String
    val rpeScaleHint: String
    val clearValueButton: String
    val customRestTimerMinutes: String
    val customRestTimerSeconds: String
    val deleteSetAction: String
}
