# KumaLift QA & Logic Stabilization Checklist

This document tracks all QA scenarios and logic verification for KumaLift v2.0 (Pure Local Offline Training), per `docs/KumaLift-Stabilization-Plan.md` Task D.

---

## 1. Test Scenarios Matrix

| ID | Scenario | Verification Method | Status | Notes / Fix Applied |
|---|---|---|---|---|
| **QA-01** | **Fresh Install & Zero Fake Data**<br>No fake/mock workouts, clean history, correct empty states across Home, Progress, Me, and Exercises. | Unit tests (`LogicStabilizationTests.kt`), Emulator verification | **PASS** | Verified on fresh database: 0 completed workouts, 0 mock PRs, clean empty states. |
| **QA-02** | **Start Workout from Routine**<br>Launch workout from blueprint (e.g., Pull Day A), sets prefilled correctly. | Integration tests, UI walkthrough on Pixel 7 Pro | **PASS** | Workout created with correct exercise templates and planned sets. |
| **QA-03** | **Start Freestyle Workout**<br>Launch blank workout, add exercises from library, adjust targets. | Integration tests, UI walkthrough | **PASS** | Exercises added dynamically from catalog. |
| **QA-04** | **Duplicate Exercise in One Workout**<br>Add the same exercise twice (e.g. Bench Press early and late in session). | `testStartWorkoutFromRoutineFreestyleAndDuplicateExercise` | **PASS** | Separate `WorkoutExercise` IDs prevent set collision and handle matrix updates independently. |
| **QA-05** | **Set Logging (Weight, Reps, RPE, Set Type)**<br>Update values, cycle set types (Normal, Warmup, Drop, Failure), set RPE. | Unit tests, UI matrix | **PASS** | Bounded stepper controls; canonical storage in kg. |
| **QA-06** | **Set Completion & Un-completion (Toggle)**<br>Check set off; uncheck set off. | `testCompleteUncompleteSetAndPrRevocation` | **PASS** | Toggle updates completion flag, saves to SQLDelight DB, and recalculates PRs. |
| **QA-07** | **Add & Remove Sets**<br>Add new set row; remove last set. | `testRemoveSetRestoresPreviousPr`, UI remove set button | **PASS** | Added delete set button in `ExerciseLoggingCard`. Revokes and restores previous PRs from history. |
| **QA-08** | **Remove Exercise from Active Workout**<br>Remove an entire exercise card from the active session. | `testRemoveExerciseRevokesItsPrs`, UI dropdown menu | **PASS** | Added overflow dropdown with `removeExerciseAction`. Revokes PRs generated during active session and restores previous PRs. |
| **QA-09** | **Rest Timer Execution & Backgrounding**<br>Timer auto-starts on set completion, adjusts (+15s), survives backgrounding without drift. | `testRestTimerWallClockAccuracy`, Android implementation | **PASS** | Uses wall-clock `targetEndEpochMs` calculation. Immune to coroutine throttle. Posts completion notification and haptic vibration. |
| **QA-10** | **Finish Workout & Summary Integrity**<br>Finish workout; verify duration, volume, sets, and PRs match exactly. | `testFinishWorkoutSummaryValuesMatchLoggedSets`, `App.kt` | **PASS** | Fixed summary null reference by falling back to `completedWorkouts.firstOrNull()`. Summary displays exact volume, duration, and PRs. |
| **QA-11** | **History & Dashboards Synchronized**<br>Home, Progress Analytics, and Me screens immediately reflect newly completed workout. | UI verification, StateFlow reactive bindings | **PASS** | `completedWorkouts` and `personalRecords` StateFlows update Home badges, calendar adherence, Progress charts, and Me profile metrics. |
| **QA-12** | **PR Detection & Rep-Range PRs**<br>Detect initial PR, heavier weight PR, and higher rep PR; warmup/drop sets excluded. | `WorkoutLogicTests.kt`, `LogicStabilizationTests.kt` | **PASS** | Correctly evaluates 1RM, e1RM, and rep-range PRs according to user `CalculationSettings`. |
| **QA-13** | **PR Revocation on Set Undo or Deletion**<br>Undo or delete a PR-bearing set; verify PR is revoked and preceding best PR is restored. | `testCompleteUncompleteSetAndPrRevocation`, `testRemoveSetRestoresPreviousPr` | **PASS** | `revokePrBySetId` recalibrates exercise PRs across entire historical session log. |
| **QA-14** | **Unit Switching (KG <-> LB)**<br>Switch between kg and lb in Settings; verify all screens format accurately without storage drift. | `testUnitsSwitchKgToLbWithoutValueDrift` | **PASS** | Database stores canonical kg; conversions happen at presentation layer via `UnitConverters`. |
| **QA-15** | **Mid-Workout Process Recreation & App Kill**<br>Kill process mid-workout; reopen app and verify active workout and navigation overlay are restored. | `LocalDatabasePersistenceTests.kt`, `testNavigatorSaverRestoresActiveLoggerAndSummary` | **PASS** | Active workout JSON persisted in SQLDelight table `ActiveWorkout`; `AppNavigator.Saver` restores `Overlay.ActiveLogger`. |
| **QA-16** | **Backup, Wipe & Restore**<br>Export JSON backup, wipe all data, restore via REPLACE or MERGE mode. | `DataBackupTests.kt`, `LocalDatabasePersistenceTests.kt` | **PASS** | Full fidelity schema v1 backup restores workouts, routines, exercises, PRs, and settings. Safety snapshot taken prior to replace. |
| **QA-17** | **Settings Persistence & Behavior**<br>Verify all rows in Settings persist across restarts and drive real app behavior. | `SettingsStorageTests.kt`, UI inspection | **PASS** | Theme, language, units, rest timers, screen on, diagnostic logs, and notifications all persist to device storage. |
| **QA-18** | **Structured Local Diagnostic Logging**<br>Toggle `localLogging`, export log file, clear buffer; verify unexpected events are logged. | `testDiagnosticLoggingBufferAndToggle`, `KumaLogger.kt` | **PASS** | Bounded in-memory rotating buffer (up to 1,000 entries) with zero remote telemetry. Exportable via `ExportImportDataPage`. |
| **QA-19** | **State Ownership & Lifecycle Hoisting**<br>Ensure navigation tab switching and detail modals do not lose scroll position, filters, or form state. | `AppNavigatorTests.kt`, `LibraryFilterStateTests.kt` | **PASS** | Hoisted state holders, `rememberSaveable` across library filters, detail view IDs, timeframe pickers, and routine categories. |
| **QA-20** | **Localization & Language Guard**<br>Ensure zero untranslated or mixed-language text strings across Indonesian and English. | `LanguageGuardTests.kt` | **PASS** | Automated reflection test validates completeness and absence of English fallback leaks. |

---

## 2. Automated Test Suite Summary

- **Total Test Files**: 8 test suites (`LogicStabilizationTests`, `LanguageGuardTests`, `SpritePlayerTests`, `LibraryFilterStateTests`, `ControllableGifPlayerTests`, `AppNavigatorTests`, `LocalDatabasePersistenceTests`, `WorkoutLogicTests`)
- **Total Test Cases**: 60+ unit & integration tests
- **Command**: `./gradlew :composeApp:testDebugUnitTest`
- **Result**: `BUILD SUCCESSFUL` (all tests passing)
