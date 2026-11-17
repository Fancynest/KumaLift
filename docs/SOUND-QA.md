# KumaLift Sound Identity & Audio Policy QA Checklist

This document details the verification scenarios and results for the KumaLift sound identity, audio playback engine, and notification channels.

## Core Audio Identity Standards
- **Aesthetic**: Calm, warm, low-mid, masculine, premium. Never startling or harsh.
- **Rules**: Quiet by default, no beeps, bells, buzzers, alarms, or looping sounds.
- **Audio Assets**:
  - `kumalift_rest_end.ogg` (1.0s, -20 LUFS)
  - `kumalift_tick.ogg` (0.12s, -18 dBFS peak)
  - `kumalift_set_done.ogg` (0.15s, -12 dBFS peak)
  - `kumalift_pr.ogg` (1.4s, -20 LUFS)
  - `kumalift_workout_done.ogg` (1.8s, -20 LUFS)
  - `kumalift_reminder.ogg` (1.2s, -20 LUFS)
  - `kumalift_error.ogg` (0.5s, -23 LUFS)

---

## Device & Platform Test Scenarios

### 1. Ringer Modes
- [x] **Normal Mode**:
  - In-app sounds play at chosen volume (`LOW`, `MEDIUM`, or `NORMAL`).
  - Haptic feedback fires cleanly for set completion, PR, and rest end.
- [x] **Vibrate Mode**:
  - All in-app sounds are suppressed.
  - Light haptic feedback still functions if `soundProfile != SILENT`.
- [x] **Silent Mode**:
  - All in-app audio and vibration are completely suppressed.

### 2. Do Not Disturb (DND)
- [x] **DND Active**:
  - Device interruption filter suppresses all in-app sounds and in-app vibrations.
  - No unexpected audio interruptions.

### 3. Audio Ducking with Background Music
- [x] **Music Playing in Another App (Spotify / YouTube Music)**:
  - Audio focus requested with `AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK`.
  - External music ducks (volume temporarily lowers) for the duration of KumaLift audio asset + 100ms.
  - External music is never paused, stopped, or killed.
  - Focus is released immediately after playback.

### 4. Bluetooth Headsets & Accessories
- [x] **A2DP / Handsfree Bluetooth Connected**:
  - Audio routes cleanly to connected audio device via `USAGE_ASSISTANCE_SONIFICATION`.
  - No clipping or routing errors.

### 5. App Lifecycle & Foreground / Background State
- [x] **Rest Timer in Foreground (Screen Active)**:
  - When timer reaches 0: Exactly one in-app `rest_end` sound plays.
  - Light haptic fires.
  - Zero notifications are posted to the notification shade (no double alert).
- [x] **Rest Timer in Background / Screen Off**:
  - When timer reaches 0: A notification is posted to the appropriate channel variant (`rest_timer_alert_kuma_v2`, `_vibrate_v2`, or `_silent_v2`).
  - No in-app `SoundPool` sound is triggered.
- [x] **Foreground Notification Dismissal**:
  - If a "rest finished" notification was posted while backgrounded, returning the app to the foreground immediately dismisses the notification.
  - Skipping or restarting the timer also immediately dismisses any pending alert notification.

### 6. Notification Permissions
- [x] **Permission Granted**:
  - Ongoing countdown timer shows with native chronometer (when `ongoingWorkout` is enabled).
  - Background rest alerts are posted.
- [x] **Permission Denied**:
  - In background: Nothing audible plays, no notification posted.
  - App logs once and fails gracefully with zero crashes.

### 7. Notification Channel Localization & Re-creation
- [x] **Versioned Channels (`_v2`)**:
  - Legacy channels (`kuma_rest_timer_ongoing`, `kuma_rest_timer_alert`, `workout_reminders`, `kumalift_backup_alerts`) deleted on upgrade.
  - Channels `rest_timer_alert_kuma_v2`, `workout_reminder_kuma_v2`, `backup_error_v2` created with `USAGE_NOTIFICATION_EVENT`.
- [x] **Language Switching (Indonesian / English)**:
  - Changing language in Settings immediately updates channel names and descriptions in system settings.

### 8. Rapid Set Completions & Debounce
- [x] **Rapid Clicking (Tapping check buttons quickly)**:
  - 1500ms debounce window prevents sound stacking or double hits.
  - Sounds never overlap or loop.

### 9. SoundPool Preloading Resilience
- [x] **Cold Start Trigger**:
  - Assets are preloaded asynchronously on app startup.
  - If a sound event is triggered before the sample finishes loading, it is silently dropped without lagging the UI or throwing an exception.
