# KumaLift – Fix Plan for the Coding Agent

Result of a static review of `github.com/Fancynest/KumaLift` (HEAD at commit `fe0682e`). The code was read, not executed. Verify each item against the current code before changing it, because line numbers may have shifted.

Reference documents already in the repo: `assets/md/KumaLift-Settings-Spec.md` and `assets/md/KumaLift-Design-Tokens.md`.

## Working Rules

1. Work in priority order (P0, then P1, then P2). One task per commit with a clear message.
2. After each task run `./gradlew :composeApp:allTests` and make sure the project still builds.
3. Do not invent data. Every number shown in the UI must come from real stored data or be an honest empty state.
4. Do not show success messages for actions that did not happen.
5. The app is **pure local**: no accounts, no server, no cloud sync, no remote analytics.
6. Weights are always stored in kg and lengths in cm. Convert only at the UI boundary.
7. Add or update tests for every behavior change.

---

## P0 – Critical (app is not usable as a tracker until these are done)

### T1. Persist all user data locally

**Problem:** `WorkoutRepository` (`_activeWorkout`, `_completedWorkouts`) and `PersonalRecordRepository` (`_prs`) live only in `MutableStateFlow`. Closing the app loses all history and PRs. `AndroidSettingsStore.save()` persists only profile fields, theme, and high contrast; all other settings groups (units, logging, equipment, notifications, calculations, security, health, data backup) are lost on restart. Only custom exercises and custom routines are saved (JSON files).

**Do:**
- Introduce a real local database in `commonMain` (Room KMP or SQLDelight; pick one and document the choice in the README). Tables at minimum: `workouts`, `workout_exercises`, `sets`, `personal_records`, `bodyweight_log`, `custom_exercises`, `routines`, `routine_exercises`.
- Persist the **active workout** after every change so an unfinished workout survives app kill and process death. On launch, offer to resume it (see `logging.staleWorkoutHours` in the Settings spec).
- Persist **every** field of `UserSettings` with a `settingsVersion` and migration support (multiplatform DataStore or multiplatform-settings).
- Repositories expose `Flow`s backed by the database; UI keeps collecting the same flows.
- Migrate existing `custom_routines.json` and custom exercise storage into the database on first launch so current user data is not lost.

**Acceptance:**
- Finish a workout, force-stop the app, reopen: the workout is in history and the PR is still there.
- Change every setting, force-stop, reopen: all values are retained.
- A test inserts and reads back a full workout with sets, PRs, and settings.

### T2. Remove dummy data and show honest empty states

**Problem:** mock numbers copied from the Stitch design are hardcoded in the UI, and sample data is created at startup.

Found (verify and search for more):
- `HomeScreen.kt`: `"18.4k kg"` / `"40.5k lb"`, `"Goal Reached: 80%"`, `"64.2k"`, `"Yesterday · 14,850 kg · 47 min"`.
- `MeScreen.kt`: `"141.5k lbs"` / `"64.2k kg"`, `"64.2k"`.
- `WorkoutSummaryScreen.kt`: `"47:20"`, `"14,850 kg"`, `"Barbell Bench Press: 118.0 kg"`.
- `ProgressAnalyticsScreen.kt`: `"118.0 kg"` and the chart data.
- `ExerciseDetailScreen.kt`: fallback `"118.0 kg"` when no PR exists.
- `SeedData.personalRecords` (fake PRs for bench, squat, deadlift, overhead press).
- `WorkoutRepository.init` calls `startSamplePushDayWorkout()`, so a fake workout in progress appears on first launch.
- `RoutineBlueprint.lastPerformedDaysAgo` values in `SeedData` ("4 days ago", "Last week").

**Do:**
- Compute every dashboard, summary, and progress value from stored data (volume, duration, sets, streak/adherence, PRs, charts). Respect `units.weight`.
- Add empty states for new users: no workouts yet, no PRs yet, no chart data.
- Remove `startSamplePushDayWorkout()` and `SeedData.personalRecords`. Starter routines may stay as templates, but "last performed" must come from real history.

**Acceptance:** a fresh install shows no fake numbers anywhere. After logging one workout, every value on Home, Summary, Progress, and Me reflects it exactly.

### T3. Implement real Export and Import (remove the fake)

**Problem:** in `SettingsScreen.kt`, `ExportImportDataPage` only sets a status message ("Data berhasil diekspor ke format JSON!" and "Data cadangan berhasil diverifikasi dan dimuat.") without doing anything. In a pure local app this misleads users into thinking their data is backed up.

**Do** (follow section 3.8 of the Settings spec):
- Export JSON (full fidelity with schema version), export CSV (zip), and create/restore backup (`.kumalift` file) using the system file picker via `expect/actual` (Android Storage Access Framework, iOS document picker).
- Restore with **Merge** or **Replace**, schema/version validation, and an automatic safety snapshot before Replace.
- Show real success and error states, with counts (for example "Exported 42 workouts, 318 sets").
- Add the backup reminder banner (`data.backupReminderDays`) and the "Last backup" read-only row.
- Remove the placeholder messages.

**Acceptance:** export, wipe all data, restore: the app is identical (workouts, PRs, routines, custom exercises, settings). Tested with an automated round-trip test.

### T4. Wire settings to real behavior

**Problem:** most settings are stored in the model but unused. Examples: `restTimer.start(90)` is hardcoded in `ActiveWorkoutLoggerScreen.kt`; weight unit only switches a few dummy strings; PR logic hardcodes Epley (`weightKg * (1.0 + reps / 30.0)`) and ignores `calc.oneRepMaxFormula`.

**Do:** for each key in `KumaLift-Settings-Spec.md`, either implement its behavior or remove the row from the UI until it works. At minimum, make these functional now: weight unit everywhere (display, input, stepper), first day of week, rest timer defaults/auto-start/adjust steps, weight steps per equipment, prefill source, RPE/RIR tracking, enabled set types, keep screen on, confirm finish, e1RM formula and max reps, PR metrics, volume inclusion rules, theme, language, haptics.
- Settings that change calculations must trigger recomputation of derived stats (section 2 of the Settings spec).

**Acceptance:** a test or manual checklist proves each implemented setting changes behavior and survives restart. No setting row exists that does nothing.

---

## P1 – Logic and correctness

### T5. Remove side effects from `StateFlow.update { }`

`WorkoutRepository.toggleSetCompletion` calls `prRepository.detectAndRecordPr(...)` and `onCompleted(...)` inside `_activeWorkout.update { }`; `SettingsRepository.update` and the exercise/routine repositories call `onSave` inside `update { }` too. `update` may re-run its lambda on contention, which can duplicate PR records and timer starts.

**Do:** compute the new state inside `update`, then run side effects (PR detection, timer callback, persistence) after it returns, once.

### T6. Unique, stable IDs

- Workout IDs are `w_<routine>_now` / `w_freestyle_now` / `w_active_push`, so all completed workouts collide. Use UUIDs (or time-based unique IDs).
- Set IDs are `set_<exerciseId>_<n>`; adding the same exercise twice, or deleting then adding a set, creates duplicates and `toggleSetCompletion` matches by `exercise.id`, so it can modify both entries. Identify workout exercises and sets by their own unique IDs everywhere (toggle, update, add).
- Custom exercise IDs use `size + 1`; switch to unique IDs.

**Acceptance:** tests where the same exercise is added twice and each set is toggled independently.

### T7. Rewrite PR detection

Current logic: a PR is any set with weight greater than the stored max for that exercise; `dateAchieved = "Today"` (a string); undoing a completed set does not revoke the PR; warm-up and drop sets count; `prTier` shows "3RM PR" but there is no per-rep-range tracking; e1RM ignores the formula setting.

**Do:**
- Store PRs in the database with a real timestamp and the set that produced them.
- Track PRs per rep range (1RM, 3RM, 5RM, and so on) and by estimated 1RM, controlled by `calc.prMetrics` and `calc.repRangePrs`.
- Use `OneRepMaxCalculator.calculate(...)` with the configured formula and `maxRepsForEstimate`.
- Exclude warm-up sets (and drop/failure per settings) from PR eligibility.
- Compute PRs from history so that un-checking or editing a set recomputes them correctly.
- Show dates formatted according to the date format setting.

### T8. Better defaults and real timestamps

- New sets default to `60.0 kg` for every exercise in `startWorkoutFromRoutine`, including bodyweight exercises. Use the prefill source setting (last session, best set, or none); bodyweight exercises default to 0 added weight.
- `Workout.startTimeEpochMs` is `0L` for new workouts. Record the real start time; derive duration from it.
- Remove dead code like `if (setIndex == 1) SetType.NORMAL else SetType.NORMAL`.

### T9. Remove JVM-only APIs from `commonMain`

`System.currentTimeMillis()` is used in `WorkoutRepository.kt`, `ActiveWorkoutLoggerScreen.kt` (2 places), and `CreateRoutineScreen.kt`. It does not exist on Kotlin/Native, so the iOS build will fail on macOS even though Android builds on Windows (iOS targets are ignored there).

**Do:** use `kotlinx-datetime` (`Clock.System.now().toEpochMilliseconds()`), or an `expect/actual`. Also search `commonMain` for other `java.*` / `android.*` usages.

### T10. Back navigation

No `BackHandler` exists anywhere. Navigation is a manual `Screen` enum, so the Android back button likely exits the app from Active Logger, Summary, Settings, and sub-pages.

**Do:** handle back at the root (Android `BackHandler` through an `expect/actual` wrapper or a proper back stack): back from sub-pages returns to the previous screen, back in the logger asks for confirmation (respecting `logging.confirmFinish`), and back on the Home tab exits. Also test predictive back on Android 14+.

---

## P2 – Platform, release, and quality

### T11. iOS media

Exercise thumbnails and GIFs are loaded only on Android (Coil 2 in `androidMain`, assets in `androidMain/assets`). The iOS `actual` implementations are icon placeholders. Either implement iOS media (for example Coil 3 multiplatform with Compose resources) or explicitly document Android-first status in the README and hide iOS from the build until ready.

### T12. Media license and APK size

- Exercise media (`1,324` GIFs, about 154 MB in `androidMain/assets`) is © Gym visual. The README already states that a public or commercial release requires an independent license from Gym visual. Keep that notice and do not ship publicly without the license.
- Ship only a licensed subset, or move the media to on-demand delivery (Play Asset Delivery). Add a note on how to swap in a different media source without code changes (the `gifUrl` / `thumbnailUrl` fields already abstract this).

### T13. Manifest and permissions

- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` and `SCHEDULE_EXACT_ALARM` are restricted by Google Play policy for most apps. Prefer `WorkManager` or inexact alarms for reminders, as in the Settings spec; remove these permissions unless a justified exception applies.
- Health Connect permissions and Bluetooth LE heart-rate monitoring (not in the specs) require a privacy policy and Play declarations. Keep them behind opt-in settings and only request permissions at the moment the user enables the feature.
- Review `android:largeHeap="true"` (hides memory problems), `android:allowBackup="true"` (decide deliberately, since all data is local and the app has its own backup), and `isMinifyEnabled = false` for release (enable R8 with keep rules and test).

### T14. Exercise dataset storage

`ExerciseDataset.kt` is about 23,900 lines of generated Kotlin in ten `chunkN()` functions. This slows compilation and risks the JVM method size limit as it grows. Move the data to a JSON resource (or the database) loaded lazily in the background, keep the generator script (`generate_dataset.ps1`) in sync, and keep `ExerciseDatasetTests` working.

### T15. Tests and docs

- Current tests are few (about 14). Add tests for: PR detection (including undo), `WorkoutRepository` (unique IDs, toggle, add set, finish), unit conversion round trips, settings persistence, export/import round trip, recompute triggers.
- Update the README so claims match reality (for example, "Unit tests (1RM, Volume, PR Detection)" must be true).
- Optional: update Kotlin, Compose Multiplatform, and AGP (currently Kotlin 2.0.21, CMP 1.7.1, AGP 8.7.3) after the P0 and P1 work is stable.

---

## Definition of Done

- A fresh install shows no fake data; every number is computed from stored data.
- Data and settings survive app restart, process death, and a backup/restore cycle.
- Back navigation works as expected on Android.
- The project compiles for Android and, on macOS or CI, for iOS (even if iOS media is a documented placeholder).
- `./gradlew :composeApp:allTests` passes, with new tests covering the changes above.
