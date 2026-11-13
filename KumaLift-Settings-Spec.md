# KumaLift – Settings Specification

Complete specification of the **Settings** feature. All items below are in scope for implementation now (no phasing).

## 0. Constraints and Scope

- **Pure local app.** No server, no accounts, no login, no cloud sync, no remote analytics, no crash-reporting SDK. All data stays on the device.
- This spec **supersedes** the cloud-sync lines in the PRD (section 5.1 "Cloud sync" and any "cloud telemetry sync" wording on the Profile tab). Backup and restore are handled through local files instead (section 8).
- Platform: Compose Multiplatform. Settings logic lives in `commonMain`. Items marked **Android** or **iOS** need platform-specific implementations via `expect/actual` or an interface.
- Brand: the app is named **KumaLift**. The Stitch mockups say "FORGE"; use KumaLift in all UI text.
- Design: follow the PRD design system (dark by default, lime `#C6FF3D` accent reserved for primary actions and active states, Inter, 12–16px radii, minimum 48px tap targets). Destructive actions use a dedicated error color `#FF5C5C` (not lime).

## 1. Settings Screen Structure

Entry point: **Profile tab**. The Profile screen shows the athlete header and a list of settings groups. Each group opens its own sub-screen.

| # | Group | Route |
|---|---|---|
| 1 | Athlete Profile | `/settings/profile` |
| 2 | Units and Formats | `/settings/units` |
| 3 | Workout Logging | `/settings/logging` |
| 4 | Equipment | `/settings/equipment` |
| 5 | Timer and Notifications | `/settings/notifications` |
| 6 | Calculations and Analytics | `/settings/calculations` |
| 7 | Appearance and Home | `/settings/appearance` |
| 8 | Data and Backup | `/settings/data` |
| 9 | Privacy and Security | `/settings/security` |
| 10 | Health Integration | `/settings/health` |
| 11 | About and Help | `/settings/about` |

UX rules:
- **Changes apply immediately.** No Save button. Show a brief confirmation only for destructive or slow actions.
- Row types: switch, single-choice (bottom sheet), multi-choice (bottom sheet), numeric stepper, time picker, text input, action button, read-only info.
- Each row has a title, an optional one-line description, and the current value on the right.
- Include a **search field** at the top of the Profile settings list that filters rows across all groups by title and description.
- Group destructive actions in a separate "Danger Zone" section at the bottom of Data and Backup.
- A "Reset to default" action exists per group (resets that group only) and globally (section 11).

## 2. Data Model and Storage

- Define `UserSettings` as one immutable Kotlin data class in `domain/model`, composed of nested groups (`ProfileSettings`, `UnitSettings`, `LoggingSettings`, and so on), each with a **default value for every field**.
- Expose through a `SettingsRepository` with `val settings: Flow<UserSettings>` and `suspend fun update(transform: (UserSettings) -> UserSettings)`.
- Persist with **multiplatform DataStore (Preferences)** or `multiplatform-settings`. Do not put settings in the main workout database. Exception: **body weight history** is a log, so it belongs in the database (section 3 of the profile group explains the link).
- Store a `settingsVersion: Int` and write migrations when keys change.
- Every screen reads settings through the `Flow` so changes propagate instantly.
- **Canonical storage units:** weights are always stored in the database as **kilograms (Double)** and lengths as **centimeters**. Conversion to lb/in happens only at the UI boundary. Display values rounded to the relevant step (see 3.x), never round the stored value.

### Settings that trigger recomputation

Changing these must invalidate and recompute derived statistics (estimated 1RM, PRs, volume, charts), not just refresh the UI:

| Setting key | What recomputes |
|---|---|
| `calc.oneRepMaxFormula` | All estimated 1RM values, 1RM charts, e1RM-based PRs |
| `calc.maxRepsForEstimate` | e1RM eligibility of every set |
| `calc.prMetrics`, `calc.repRangePrs` | PR detection over full history |
| `calc.includeWarmupInVolume`, `calc.includeDropSetsInVolume`, `calc.includeFailureSetsInVolume` | Volume totals and weekly tonnage |
| `calc.bodyweightVolumeMode` | Volume for bodyweight exercises |
| `profile.bodyweightKg` history | Bodyweight-based volume and relative strength |

Recompute off the main thread, show a non-blocking progress indicator, and make it cancelable/restartable if the user changes the setting again.

## 3. Settings Reference

Notation: **Key** is the storage key. **Default** is the value used on first launch. **Type** is the value type or enum.

### 3.1 Athlete Profile

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `profile.name` | Name | String | empty | Optional. Used in the Home greeting. |
| `profile.avatarUri` | Profile photo | String? | null | Optional. Copy the picked image into app-private storage and store that path, not the gallery URI. |
| `profile.bodyweightKg` | Body weight | Double? | null | Optional. Editing it adds a dated entry to the **body weight log** table (date, kg). The setting shows the latest entry. Also offer "View history". |
| `profile.heightCm` | Height | Double? | null | Optional. Displayed in cm or ft/in per unit settings. |
| `profile.birthYear` | Birth year | Int? | null | Optional. Validate 1900 to current year. |
| `profile.sex` | Sex | Enum: `MALE, FEMALE, UNSPECIFIED` | `UNSPECIFIED` | Optional. Only used for strength-standard comparisons. |
| `profile.experienceLevel` | Experience level | Enum: `BEGINNER, INTERMEDIATE, ADVANCED` | `INTERMEDIATE` | Used for suggestions and default progression steps. |
| `profile.goal` | Training goal | Enum: `STRENGTH, HYPERTROPHY, GENERAL` | `HYPERTROPHY` | Used for default rest times and RPE targets in suggestions. |
| `profile.trainingDaysPerWeek` | Training days per week | Int 1..7 | 4 | Used for the adherence target on Home. |

### 3.2 Units and Formats

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `units.weight` | Weight unit | Enum: `KG, LB` | `KG` | Global. Switching converts all displayed weights, including history. Stored data never changes. |
| `units.length` | Body measurement unit | Enum: `CM, IN` | `CM` | Height and body measurements. |
| `format.firstDayOfWeek` | First day of week | Enum: `MONDAY, SUNDAY, SATURDAY` | `MONDAY` | Affects the 7-day tracker, weekly tonnage buckets, and streak logic. |
| `format.timeFormat` | Time format | Enum: `SYSTEM, H12, H24` | `SYSTEM` | |
| `format.dateFormat` | Date format | Enum: `SYSTEM, DMY, MDY, YMD` | `SYSTEM` | |

### 3.3 Workout Logging

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `logging.restTimer.defaultSec` | Default rest time | Int seconds, 15..600, step 5 | 90 | |
| `logging.restTimer.separateByType` | Separate rest for compound and isolation | Boolean | false | When on, the two rows below appear. |
| `logging.restTimer.compoundSec` | Compound rest time | Int seconds | 150 | Used when the exercise is marked compound. |
| `logging.restTimer.isolationSec` | Isolation rest time | Int seconds | 75 | |
| `logging.restTimer.autoStart` | Auto-start rest timer | Boolean | true | Starts when a set is checked. |
| `logging.restTimer.addStepSec` | Rest timer "add" step | Enum: `15, 30` | 30 | The "+" pill on the timer. |
| `logging.restTimer.subtractStepSec` | Rest timer "subtract" step | Enum: `15, 30` | 15 | The "−" pill on the timer. |
| `logging.workoutTimerStart` | Workout timer starts | Enum: `ON_START, ON_FIRST_SET` | `ON_START` | |
| `logging.weightStepKg.barbell` | Weight step: barbell | Double | 2.5 | Stored in kg. Show and edit in the selected unit. Offer presets (1.25, 2.5, 5) plus custom. |
| `logging.weightStepKg.dumbbell` | Weight step: dumbbell | Double | 2.0 | |
| `logging.weightStepKg.cable` | Weight step: cable | Double | 2.5 | |
| `logging.weightStepKg.machine` | Weight step: machine | Double | 2.5 | |
| `logging.weightStepKg.bodyweight` | Weight step: bodyweight / added | Double | 0.5 | |
| `logging.repStep` | Rep step | Int | 1 | |
| `logging.defaultSetCount` | Default sets when adding an exercise | Int 1..10 | 3 | |
| `logging.prefillSource` | Pre-fill new sets from | Enum: `LAST_SESSION, BEST_SET, NONE` | `LAST_SESSION` | |
| `logging.showPreviousColumn` | Show "Previous" column | Boolean | true | |
| `logging.trackIntensity` | Track intensity (RPE/RIR) | Boolean | true | When off, hide the RPE column and RPE fields in summaries. |
| `logging.intensityScale` | Intensity scale | Enum: `RPE, RIR` | `RPE` | RPE uses 6 to 10 in 0.5 steps; RIR uses 0 to 5 in whole steps. Store RPE internally and convert RIR as `10 - RIR`. |
| `logging.enabledSetTypes` | Available set types | Multi-select: `WARMUP, WORKING, DROP, FAILURE` | all | `WORKING` cannot be disabled. |
| `logging.compactSetRows` | Compact set rows | Boolean | false | Reduces row height while keeping 48px tap targets for the check and steppers. |
| `logging.keepScreenOn` | Keep screen on during workout | Boolean | true | |
| `logging.confirmFinish` | Confirm before finishing workout | Boolean | true | |
| `logging.staleWorkoutHours` | Prompt about unfinished workout after | Int hours, 0 = off | 6 | On app launch, if an active workout is older than this, ask to resume, finish, or discard. |

### 3.4 Equipment

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `equipment.bars` | Bars | List of `{id, name, weightKg}` | Olympic bar 20, Women's bar 15, EZ bar 10 | Add, edit, delete. Exactly one is marked default. |
| `equipment.defaultBarId` | Default barbell | Reference to a bar | Olympic bar | Used by the plate calculator. |
| `equipment.platesKg` | Plates (kg) | List of `{weightKg, enabled}` | 25, 20, 15, 10, 5, 2.5, 1.25, 0.5 all enabled | Inventory of available plates when the unit is kg. |
| `equipment.platesLb` | Plates (lb) | List of `{weightLb, enabled}` | 45, 35, 25, 10, 5, 2.5 all enabled | Inventory when the unit is lb. Kept separate to avoid awkward conversions. |
| `equipment.showPlateCalculator` | Show plate calculator in logger | Boolean | true | Tapping the weight field on a barbell exercise shows plates per side. |
| `equipment.plateCalculatorRoundingKg` | Round to nearest loadable weight | Boolean | true | If the entered weight cannot be loaded exactly, suggest the nearest loadable one. |

Plate calculator behavior: given target weight, bar weight and enabled plates, compute plates per side using a greedy algorithm over the enabled plates (largest first). Show a remainder warning if the target is not exactly loadable.

### 3.5 Timer and Notifications

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `notif.restTimerAlert` | Rest timer alert | Boolean | true | Local notification when the timer ends while the app is in the background. |
| `notif.restTimerSound` | Alert sound | Enum: `DEFAULT, SOFT, BEEP, NONE` | `DEFAULT` | Bundle the sounds as resources. |
| `notif.restTimerVibrate` | Vibrate | Boolean | true | |
| `notif.countdownBeeps` | Beep during last 3 seconds | Boolean | false | |
| `notif.ongoingWorkout` | Persistent workout notification | Boolean | true | Ongoing notification showing workout time and rest countdown. **Android:** implement as a foreground service. |
| `notif.workoutReminders` | Workout reminders | Boolean | false | |
| `notif.reminderDays` | Reminder days | Multi-select weekdays | training days | Visible only when reminders are on. |
| `notif.reminderTime` | Reminder time | Time | 17:00 | |
| `notif.streakReminder` | Streak reminder | Boolean | false | Fires only if the user has an active streak and has not trained that day. |
| `notif.streakReminderTime` | Streak reminder time | Time | 20:00 | |
| `notif.quietHoursEnabled` | Quiet hours | Boolean | false | Suppresses scheduled reminders in the window. |
| `notif.quietHoursStart` / `notif.quietHoursEnd` | Quiet hours window | Time | 22:00 / 06:00 | Window may cross midnight. |

Permission handling:
- **Android 13+:** request `POST_NOTIFICATIONS` when the user enables any notification setting. If denied, switch the setting back off and show a row linking to system settings.
- **Android 12+ exact alarms:** prefer inexact scheduling (`WorkManager` or inexact alarms) for reminders to avoid the exact-alarm permission. Reminders are allowed to be a few minutes off.
- **iOS:** request notification authorization on first enable; use local notifications only.
- A "Notification permission" status row should always show the current state with a button to open system settings.

### 3.6 Calculations and Analytics

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `calc.oneRepMaxFormula` | Estimated 1RM formula | Enum: `EPLEY, BRZYCKI, LOMBARDI, OCONNER` | `EPLEY` | Show the formula in the row description. |
| `calc.maxRepsForEstimate` | Max reps used for 1RM estimate | Int 5..15 | 10 | Sets above this are excluded from e1RM because accuracy drops. |
| `calc.prMetrics` | Personal record types | Multi-select: `WEIGHT, E1RM, SET_VOLUME` | `WEIGHT, E1RM` | |
| `calc.repRangePrs` | Track PRs per rep range | Boolean | true | Enables entries like "3RM PR", "5RM PR". |
| `calc.includeWarmupInVolume` | Count warm-up sets in volume | Boolean | false | |
| `calc.includeDropSetsInVolume` | Count drop sets in volume | Boolean | true | |
| `calc.includeFailureSetsInVolume` | Count failure sets in volume | Boolean | true | |
| `calc.bodyweightVolumeMode` | Bodyweight exercise volume | Enum: `ADDED_ONLY, BODYWEIGHT_PLUS_ADDED` | `ADDED_ONLY` | `BODYWEIGHT_PLUS_ADDED` requires a body weight value; if missing, prompt the user to enter it. |
| `calc.weeklyVolumeTargetKg` | Weekly volume target | Double? (kg) | null | Optional. Draws the maintenance threshold line on the weekly tonnage chart. |

Formulas (for the agent to implement and unit test):
- Epley: `1RM = w × (1 + r / 30)`
- Brzycki: `1RM = w × 36 / (37 − r)`
- Lombardi: `1RM = w × r^0.10`
- O'Conner: `1RM = w × (1 + r / 40)`
- For `r = 1`, estimated 1RM equals the lifted weight in every formula.
- Volume per set: `weight × reps` (with the bodyweight mode above for bodyweight exercises).

### 3.7 Appearance and Home

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `appearance.theme` | Theme | Enum: `DARK, LIGHT, SYSTEM` | `DARK` | The light theme needs its own token set that keeps the lime accent readable (use a darker lime variant for text on light surfaces). |
| `appearance.highContrast` | High contrast | Boolean | false | Stronger borders and text contrast. |
| `appearance.language` | Language | Enum: `SYSTEM, ID, EN` | `SYSTEM` | Use Compose Multiplatform resources for strings (`composeResources`). Switching must apply without restarting the app where the platform allows it. |
| `appearance.textScale` | Text size | Enum: `SYSTEM, SMALL, DEFAULT, LARGE, XLARGE` | `SYSTEM` | |
| `appearance.reduceMotion` | Reduce motion | Boolean | false | Disables non-essential animations and chart draw-in effects. |
| `appearance.haptics` | Haptic feedback | Boolean | true | On set check, timer end, PR. |
| `appearance.leftHandedMode` | Left-handed mode | Boolean | false | Mirrors the set-complete column, floating buttons, and primary bottom actions to the left. |
| `home.showWeeklyHorizon` | Show weekly tracker | Boolean | true | |
| `home.showMonthlyGlance` | Show monthly stats | Boolean | true | |
| `home.showRecentWorkouts` | Show recent workouts | Boolean | true | |
| `home.recentWorkoutsCount` | Recent workouts shown | Int 3..10 | 5 | |

### 3.8 Data and Backup (local only)

| Key / Action | Title | Type | Default | Notes |
|---|---|---|---|---|
| Action | Export CSV | Action | | Opens the system file picker (Android: Storage Access Framework; iOS: document picker/share sheet). Options: all time or date range; include notes toggle. Exports workouts, sets, exercises, routines, and body weight as separate CSVs bundled in one zip. |
| Action | Export JSON | Action | | Full-fidelity export with schema version. |
| Action | Create backup | Action | | Writes one `.kumalift` file (zip containing JSON data and settings). |
| Action | Restore from backup | Action | | File picker. Validate schema version and checksum. Offer **Merge** or **Replace**. Always create an automatic safety snapshot before Replace. |
| Action | Import from other apps | Action | | Supports Strong CSV, Hevy CSV, and a generic CSV with a column-mapping step. Show a dry-run preview with counts and detected duplicates before committing. |
| `data.autoBackupEnabled` | Automatic backup | Boolean | false | **Android only** (needs a user-chosen folder). On iOS hide this row and rely on manual backup and the share sheet. |
| `data.autoBackupFrequency` | Backup frequency | Enum: `DAILY, WEEKLY, MONTHLY` | `WEEKLY` | Implemented with `WorkManager`. |
| `data.autoBackupFolderUri` | Backup folder | String? | null | Persisted SAF tree URI with persisted permission. |
| `data.autoBackupKeepLast` | Backups to keep | Int 1..20 | 5 | Delete older backups in that folder created by the app. |
| `data.backupReminderDays` | Remind me to back up after | Int days, 0 = off | 30 | Shows a dismissible banner on Home if the last backup is older than this. Important because all data is local. |
| Read-only | Last backup | Date/time | | |
| Read-only | Storage used | Size | | Database size, number of workouts, number of logged sets. |

**Danger Zone** (error color, each requires confirmation):
- **Clear workout history:** deletes workouts and sets, keeps routines, custom exercises, and settings. Suggest creating a backup first.
- **Reset all settings:** restores defaults for every group. Does not touch workout data.
- **Delete all data:** deletes everything and returns to onboarding. Two-step confirmation: dialog, then type `DELETE`. Offer "Create backup first" as the primary option in the dialog.

### 3.9 Privacy and Security

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `security.appLock` | App lock | Enum: `NONE, BIOMETRIC, PIN` | `NONE` | `BIOMETRIC` uses system biometric prompt with device-credential fallback. `PIN` is a 4 to 6 digit app PIN stored hashed with a salt in secure storage (Android Keystore / iOS Keychain). |
| `security.lockTimeout` | Lock after | Enum: `IMMEDIATE, 1_MIN, 5_MIN, 15_MIN` | `IMMEDIATE` | Applies when the app returns from background. |
| `security.hideInAppSwitcher` | Hide content in app switcher | Boolean | false | **Android:** `FLAG_SECURE` (also blocks screenshots). **iOS:** blur overlay when the app goes inactive. |
| Info | Data and privacy | Read-only page | | Static text: all data stays on the device, no account, no tracking, no data is sent anywhere. |
| Info | Permissions | Read-only list | | Status of notification and other permissions, each with a button to open system settings. |
| `debug.localLogging` | Local diagnostic log | Boolean | false | Writes a rotating log file on device (size cap, for example 1 MB). |
| Action | Export diagnostic log | Action | | Share the log file manually, for support or bug reports. Visible only when logging has produced a file. |

### 3.10 Health Integration (on-device)

Runs entirely through the OS health APIs, so no server is involved. Entire group is hidden if the platform API is unavailable.

| Key | Title | Type | Default | Notes |
|---|---|---|---|---|
| `health.enabled` | Connect to Health app | Boolean | false | **Android:** Health Connect. **iOS:** Apple Health (HealthKit). Request permissions only when turned on. |
| `health.writeWorkouts` | Save workouts to Health | Boolean | true when connected | Writes each finished session (start, end, type strength training). |
| `health.writeBodyWeight` | Save body weight to Health | Boolean | false | |
| `health.readBodyWeight` | Read body weight from Health | Boolean | false | Imports new entries into the body weight log without duplicating existing ones. |
| Info | Permission status | Read-only | | With a button to open the system permission screen. |

### 3.11 About and Help

| Item | Type | Notes |
|---|---|---|
| App version and build number | Read-only | |
| What's new | Static page | Changelog bundled in the app. |
| Send feedback or report a bug | Action | Opens the email composer via intent (`mailto:`). Optional checkbox "Include app version and device info" (no workout data). |
| Rate the app | Action | Opens the store listing. |
| Open-source licenses | Static page | Generate from dependencies (for example the AboutLibraries plugin). |
| Privacy policy and terms | Static page | Bundled text. Required for store publishing. |
| Help and FAQ | Static page | Bundled. Cover units, backup and restore, and how e1RM is estimated. |
| Replay onboarding | Action | |
| Reset all settings | Action | Same behavior as in the Danger Zone; here for discoverability. |

## 4. Implementation Notes for the Agent

1. Build `UserSettings`, defaults, `SettingsRepository`, and the storage layer first, with unit tests for defaults and migrations.
2. Build the unit conversion helpers (kg/lb, cm/in/ft-in) with tests. Verify round trips do not drift stored values.
3. Implement the calculation functions (e1RM formulas, volume, PR detection) as pure functions in `core/util` with tests, then wire the recompute triggers from section 2.
4. Build the settings UI group by group, reusing shared row components (switch row, choice row, stepper row, time row, action row) from `core/designsystem`.
5. Add platform pieces behind interfaces: notifications and foreground service, file pickers, biometric/PIN, secure-window flag, health APIs, folder-based auto backup.
6. Every setting must be wired to behavior. Do not ship a row that changes nothing.
7. Test the cross-cutting cases: switching units mid-history, changing the e1RM formula with existing data, restoring a backup with Merge and Replace, denying notification permission, and process death during an active workout.

## 5. Explicitly Out of Scope

- Accounts, sign-in, sign-out, delete account.
- Cloud sync and any server-backed feature.
- Remote analytics, crash-reporting SDKs, and server push notifications.
- Social features and sharing to a backend (image/file sharing through the OS share sheet is fine).
- Watch companion apps (Wear OS / watchOS).
