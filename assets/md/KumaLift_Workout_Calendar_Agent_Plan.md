# KumaLift --- Workout Calendar Implementation Plan

> **Task type:** Incremental feature implementation\
> **Target placement:** Home screen\
> **Scope:** Workout history + date-specific workout scheduling\
> **Platforms:** Kotlin Multiplatform (Android and iOS)\
> **Important:** Work incrementally. Preserve the existing architecture
> and design system. Do not perform unrelated refactors.

## 1. Objective

Implement a Workout Calendar experience inside KumaLift's existing Home
screen. The calendar must let users:

1.  View completed workout history by date.
2.  View and manage workouts planned for specific dates.
3.  Select a date and inspect its activity.
4.  Start a scheduled routine through the existing workout logger.
5.  See useful monthly training statistics.

The calendar is an addition to the existing Home screen, **not a new
bottom-navigation destination**.

## 2. Repository facts to verify and respect

The supplied repository snapshot contains these relevant components:

  ----------------------------------------------------------------------------------------------
  Existing component                            Observed responsibility / relevant behavior
  --------------------------------------------- ------------------------------------------------
  `features/home/HomeScreen.kt`                 Home dashboard; collects completed workouts and
                                                routines; contains the existing Weekly Horizon.

  `domain/model/Workout.kt`                     Workout domain model.

  `domain/model/RoutineBlueprint.kt`            Routine model; `scheduledDays` is a list where 1
                                                = Monday through 7 = Sunday.

  `features/routines/WorkoutScheduleModal.kt`   Existing schedule modal for weekly routine-day
                                                selection. Do not silently repurpose it as a
                                                date-specific event editor.

  `data/repository/WorkoutRepository.kt`        Exposes
                                                `completedWorkouts: StateFlow<List<Workout>>`;
                                                has `startWorkoutFromRoutine(...)` and
                                                `finishWorkout(...)`.

  `data/local/KumaDatabaseRepository.kt`        Loads and persists workout and routine data.

  `commonMain/sqldelight/.../KumaDatabase.sq`   SQLDelight schema and queries. Existing
                                                `workouts` table includes `startTimeEpochMs`,
                                                `endTimeEpochMs`, `durationSeconds`, and
                                                `isFinished`.

  `data/export/DataBackupManager.kt`            JSON backup payload currently includes workouts,
                                                PRs, routines, exercises, training programs, and
                                                settings.

  `core/localization/AppStrings.kt`,            Shared localization contract and
  `EnStrings.kt`, `IdStrings.kt`                English/Indonesian implementations.

  `app/App.kt`                                  App composition / dependency wiring.

  `build.gradle.kts`                            SQLDelight database config has
                                                `verifyMigrations.set(true)`.
  ----------------------------------------------------------------------------------------------

### Existing scheduling distinction

Do not conflate these existing concepts:

-   `RoutineBlueprint.scheduledDays`: recurring weekday assignment on a
    routine.
-   `TrainingProgram.activeDays`: recurring weekday assignment on a
    program.
-   `UserSettings.profile.selectedTrainingDays`: user training-day
    preference.
-   **New date-specific schedule event:** a single planned workout
    assigned to a concrete calendar date.

The current Home screen's Weekly Horizon derives upcoming items from
recurring routine/program weekdays. This feature must add date-specific
scheduling without changing the meaning of those existing fields.

### Verify before implementation

The source snapshot is the baseline, but inspect the current working
tree before changing files. Confirm the current APIs, model properties,
dependency injection, database version/migration layout, backup
import/export path, and test conventions. Do not assume this plan
overrides changes made since the supplied ZIP.

## 3. UX requirements

### 3.1 Home screen placement

Add a **Workout Calendar preview section** to `HomeScreen.kt`. Keep the
existing Home screen navigation model; do not add a new
bottom-navigation tab.

Suggested Home composition:

1.  Existing Home header and greeting.
2.  Existing Weekly Horizon (preserve its behavior).
3.  **New Workout Calendar section.**
4.  Existing workout / recovery / statistics sections in their current
    order unless a small layout adjustment is necessary.

The calendar should fit KumaLift's existing visual language and use its
design tokens, color providers, typography, card components, spacing,
and localization. Do not introduce arbitrary hardcoded colors or a
separate visual system.

### 3.2 Monthly calendar

The calendar section should provide:

-   Month and year heading.
-   Previous-month and next-month controls.
-   Correct month grid, including leading/trailing days as appropriate
    to the current design.
-   Weekday labels respecting the user's configured first day of week,
    if the existing setting is available.
-   A clear selected-date state.
-   A clear current-date state.
-   Per-date activity indicators.
-   A legend or accessible status distinction.

Required date states:

  -----------------------------------------------------------------------
  State                               Meaning
  ----------------------------------- -----------------------------------
  Completed                           At least one completed workout
                                      exists on that local calendar date.

  Scheduled                           At least one active date-specific
                                      schedule exists on that date.

  Rest day                            Optional explicit rest-day event,
                                      if implemented; must not be
                                      inferred from an empty date.

  Missed                              An eligible planned event is in the
                                      past and was not completed or
                                      cancelled. Do not infer missed from
                                      a day with no workout.

  Today                               Visual marker for the current local
                                      date; can coexist with activity
                                      state.
  -----------------------------------------------------------------------

If a date contains both a completed workout and a schedule, represent
both facts without hiding either.

### 3.3 Selected date detail

When a user selects a date, show its details within the Home screen (for
example, a detail card below the month grid). Do not navigate to a new
top-level screen for this initial scope.

**Completed workout details** should use existing workout data, where
available:

-   Workout title and split type.
-   Start and end time.
-   Duration.
-   Total volume and set counts.
-   Exercise list and set details.
-   Session RPE and notes when present.

**Scheduled event details** should show:

-   Routine title / snapshot title.
-   Planned date.
-   Routine details available from the linked routine or stored
    snapshot.
-   Start action.
-   Reschedule action.
-   Cancel action.

**Empty date state** should offer:

-   Add workout schedule.
-   Optionally set rest day, only if explicit rest-day events are
    implemented.
-   Start freestyle workout, if the existing Home callback can be used
    without disrupting navigation.

Support multiple completed workouts and multiple schedule events on one
date. Do not assume one workout per day.

### 3.4 Add / edit / reschedule flow

Recommended flow:

1.  User selects a date.
2.  User taps **Add Workout Schedule**.
3.  User chooses an available routine.
4.  User confirms the date and optional note.
5.  Persist the date-specific event.
6.  Update the calendar and selected-date detail reactively.

Reschedule should update the event's date, not mutate
`RoutineBlueprint.scheduledDays`.

Cancel should change the event state or otherwise preserve an
auditable/correct history according to the existing app's data
conventions. Avoid destructive deletion if it would make statistics or
backup semantics ambiguous.

## 4. Architecture proposal

Use the existing package conventions and dependency injection patterns.
The following is a suggested separation of responsibilities, not a
requirement to create every file exactly as named:

``` text
features/
  home/
    HomeScreen.kt
    components/
      ... existing components ...
  calendar/
    WorkoutCalendarSection.kt
    WorkoutCalendarState.kt
    WorkoutCalendarLogic.kt
    WorkoutDayDetail.kt
    WorkoutScheduleDialog.kt
    components/
      CalendarMonthHeader.kt
      CalendarDayCell.kt
      CalendarLegend.kt
      DailyActivityCard.kt
      MonthlyStatistics.kt

domain/
  model/
    WorkoutScheduleEvent.kt

data/
  repository/
    WorkoutScheduleRepository.kt
  local/
    KumaDatabaseRepository.kt
```

Responsibilities:

-   **WorkoutCalendarSection:** UI container embedded in Home.
-   **WorkoutCalendarState / state holder:** selected month, selected
    date, calendar data, loading/error state as appropriate.
-   **WorkoutCalendarLogic:** pure date-grid and status derivation
    logic.
-   **WorkoutDayDetail:** selected-date activity presentation.
-   **WorkoutScheduleDialog:** create/edit/reschedule interaction.
-   **WorkoutScheduleRepository:** date-specific schedule operations and
    reactive data exposure.
-   **WorkoutScheduleEvent:** domain representation of a date-specific
    schedule.

Prefer pure shared logic in `commonMain` and test it in `commonTest`.
Follow the project's existing architecture rather than introducing a new
state-management framework.

## 5. Database design

### 5.1 Add date-specific schedule persistence

A dedicated SQLDelight table is recommended because recurring weekday
assignments cannot represent one-off date-specific plans.

Proposed conceptual schema (adapt naming, SQLDelight syntax, and
foreign-key conventions to the actual project):

``` sql
CREATE TABLE workout_schedule_events (
    id TEXT NOT NULL PRIMARY KEY,
    routineId TEXT,
    scheduledDate TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'PLANNED',
    linkedWorkoutId TEXT,
    routineTitleSnapshot TEXT NOT NULL,
    splitTypeSnapshot TEXT,
    notes TEXT,
    createdAtEpochMs INTEGER NOT NULL,
    updatedAtEpochMs INTEGER NOT NULL,

    FOREIGN KEY (routineId)
        REFERENCES routines(id)
        ON DELETE SET NULL,

    FOREIGN KEY (linkedWorkoutId)
        REFERENCES workouts(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_workout_schedule_events_date
ON workout_schedule_events(scheduledDate);
```

This is a proposal, not a command to paste verbatim. Confirm SQLDelight
syntax and existing schema conventions first.

### 5.2 Date representation

Use a canonical local calendar date string, such as `2026-11-24`, for
`scheduledDate`. The event represents a calendar day, not an instant in
UTC.

-   Use `kotlinx.datetime.LocalDate` for date calculations.
-   Convert workout epoch timestamps to local date using
    `TimeZone.currentSystemDefault()`.
-   Avoid converting a date-only value through UTC timestamps.
-   Handle month boundaries, leap years, year transitions, and
    first-day-of-week correctly.

### 5.3 Event status

Use a constrained status model, for example:

-   `PLANNED`
-   `COMPLETED`
-   `CANCELLED`

A `MISSED` state may be derived for display from a past planned event,
or persisted only if there is a clear product reason. Do not mark future
events missed. Do not treat cancelled events as missed.

A separate explicit `REST_DAY` event is optional and should not be
required for the first useful release.

### 5.4 Routine deletion and snapshots

A schedule should remain understandable if its source routine is later
deleted. Preserve a routine title snapshot (and any other minimal
display data needed) when the event is created. If the routine foreign
key becomes null, the calendar must still render the event safely.

### 5.5 Workout linkage

Use `linkedWorkoutId` to link a scheduled event to the exact workout
created from it. Do not match only by date or title: multiple sessions
can occur on one day.

When a scheduled workout is started, carry the schedule event ID through
the workout lifecycle using an explicit, reliable mechanism. On
successful completion, link the finished workout to that exact event and
update its status to `COMPLETED`.

If a user starts a scheduled routine but cancels the workout, the event
should remain planned unless product behavior explicitly says otherwise.

### 5.6 Migration

SQLDelight is configured with `verifyMigrations.set(true)`. Therefore:

-   Add the schema change using the project's expected SQLDelight
    migration workflow.
-   Inspect whether migration files or generated schema snapshots are
    expected in the current branch.
-   Do not edit only the `.sq` schema and leave migration verification
    broken.
-   Verify fresh database creation and upgrade from the previous schema.
-   Do not reset or wipe existing user data.

## 6. Repository and workout logger integration

### 6.1 Preserve existing workout history source

The existing `WorkoutRepository` exposes
`completedWorkouts: StateFlow<List<Workout>>`, and the database has a
completed-workout query. Reuse the existing completed workout model and
source where appropriate.

Calendar history must include only finished workouts
(`isFinished == true`). An active or abandoned workout must not be
counted as completed history.

Do not create a second workout-history table or duplicate workout
records for calendar purposes.

### 6.2 Scheduled workout start

The existing repository has `startWorkoutFromRoutine(...)`. Extend the
flow incrementally so an optional schedule event identifier can be
associated with the active session.

Possible approaches include: - Passing an optional schedule event ID
into the start operation and persisting it with active workout state;
or - Using another existing session metadata mechanism, if one is
already present and reliable.

Inspect active workout persistence and restoration before choosing. The
association must survive app process death if the active workout itself
is restored.

### 6.3 Completion

The existing `finishWorkout(...)` persists a completed workout and
updates the in-memory completed list. Extend the completion flow so it
can also mark the associated schedule event completed and set
`linkedWorkoutId`.

Keep the workout save and event-link update consistent. If the current
persistence layer cannot provide a transaction, design safe
retry/idempotency behavior so a partial failure does not permanently
lose the link.

### 6.4 Repository API

Design repository methods consistent with existing code, such as:

``` kotlin
fun observeScheduleEvents(
    startDate: LocalDate,
    endDate: LocalDate
): Flow<List<WorkoutScheduleEvent>>

suspend fun createScheduleEvent(...): Result<WorkoutScheduleEvent>

suspend fun rescheduleEvent(
    eventId: String,
    newDate: LocalDate
): Result<Unit>

suspend fun cancelEvent(eventId: String): Result<Unit>

suspend fun completeEvent(
    eventId: String,
    workoutId: String
): Result<Unit>
```

These signatures are illustrative. Match the repository's current
synchronous/suspending and Flow conventions rather than forcing these
exact signatures.

## 7. Calendar calculations and statistics

Keep calendar calculations out of composable bodies where practical. Use
deterministic, testable functions.

### Required calculations

-   Month grid cells and adjacent-month padding.
-   Weekday ordering based on first-day-of-week preference.
-   Mapping completed workout timestamps to local dates.
-   Grouping multiple workouts and schedule events by date.
-   Combined day status when a date has both planned and completed
    activity.
-   Monthly totals.

### Suggested monthly statistics

-   **Workout days:** number of unique local dates containing at least
    one completed workout.
-   **Total sessions:** number of completed workout sessions.
-   **Total duration:** sum of completed session durations.
-   **Total volume:** sum of completed workout volume, respecting the
    app's existing unit-display rules.
-   **Scheduled events:** count of active planned events in the selected
    month.
-   **Completion rate:** completed eligible scheduled events divided by
    eligible scheduled events.

Completion rate must exclude future and cancelled events. Clearly define
whether a completed event is counted by event status or linked workout,
and keep the rule consistent.

Do not assume the sample UI figures in planning discussions are real
data.

### Important date correctness

The current Home screen contains weekday-based Weekly Horizon logic and
time-window calculations. Do not reuse a weekday-only comparison to
identify a calendar date.

Two Mondays in different weeks are different dates. Group history by the
full local date (`year-month-day`), not weekday ordinal.

Avoid capturing `Clock.System.now()` in a `remember` block with no
refresh strategy if that would leave the UI stuck on yesterday after the
app remains open across midnight. Follow the app's
lifecycle/time-refresh conventions or provide a small, testable
current-date state mechanism.

## 8. Localization and design system

Add all user-facing strings through the existing localization contract:

-   `AppStrings.kt`
-   `EnStrings.kt`
-   `IdStrings.kt`

At minimum, cover:

-   Workout calendar
-   Previous month / next month
-   Today
-   Completed
-   Scheduled
-   Rest day (if supported)
-   Missed (if shown)
-   Add workout schedule
-   Select routine
-   Reschedule
-   Cancel schedule
-   Monthly overview
-   Workout days
-   Total sessions
-   Total duration
-   Total volume
-   No activity on this date
-   Confirm / save / error / empty states

Requirements:

-   No hardcoded user-facing strings in composables.
-   English and Indonesian must both compile and display correctly.
-   Use existing KumaLift colors, typography, cards, spacing, and button
    components.
-   Preserve the current dark / Electric Lime identity.
-   Use accessible touch targets and clear selected/today/status states.
-   Respect weight-unit settings when displaying volume.
-   Avoid adding dependencies unless necessary and justified.

## 9. Backup and restore

`DataBackupManager.kt` currently serializes `KumaBackupData` with a
schema version and includes workouts, personal records, routines,
exercises, training programs, and settings.

If date-specific schedule events are added, include them in backup and
restore:

-   Add a serializable schedule-event representation to
    `KumaBackupData`.
-   Export all relevant schedule events.
-   Restore them in the correct foreign-key-safe order.
-   Support older backup files that do not contain the new field by
    giving it a backward-compatible default such as an empty list.
-   Preserve the existing merge/replace import semantics.
-   Ensure linked workout references remain valid after import.
-   Update import result counts/messages if useful and consistent with
    the current UI.
-   Add tests for old backup compatibility and new schedule-event round
    trips.

Do not change the backup schema version policy without inspecting the
existing versioning strategy.

## 10. Implementation phases

Work in small, reviewable increments. Do not attempt all phases in one
giant change.

### Phase 0 --- Re-inspection and implementation design

-   Inspect current working tree and relevant files.
-   Confirm current `Workout`, `RoutineBlueprint`, `WorkoutRepository`,
    database repository, backup model, navigation, and localization
    APIs.
-   Identify SQLDelight migration workflow and test conventions.
-   Present a concise implementation summary if the repository differs
    materially from this plan.
-   Do not edit unrelated code.

### Phase 1 --- Calendar foundation (history-first)

-   Implement pure calendar date/grid logic.
-   Add the monthly calendar preview to Home.
-   Add month navigation and selected-date state.
-   Display completed workouts by local date.
-   Add selected-date completed workout details.
-   Add monthly statistics based on existing completed workout data.
-   Add English and Indonesian strings.
-   Do not add date-specific schedule persistence in this phase unless
    needed for a small compile-safe interface.

**Phase 1 acceptance:** existing workout history appears on the correct
local dates; calendar navigation works; Home remains functional; no
existing workout data is changed.

### Phase 2 --- Date-specific scheduling

-   Add `WorkoutScheduleEvent` domain model.
-   Add SQLDelight schema and migration.
-   Add schedule queries and repository operations.
-   Add schedule creation, rescheduling, and cancellation UI.
-   Show scheduled events in the calendar and selected-date detail.
-   Preserve recurring `scheduledDays` and `activeDays` behavior
    unchanged.

**Phase 2 acceptance:** date-specific events persist across app
restarts; changing an event date does not change recurring routine
weekdays; cancelled events do not count as active plans.

### Phase 3 --- Workout logger linkage and backup

-   Carry event identity from scheduled start into active workout state.
-   Link the exact finished workout to its schedule event.
-   Handle cancellation and failure paths safely.
-   Add backup/export/import support and backward compatibility.

**Phase 3 acceptance:** completing a scheduled workout updates the
correct event; unrelated events on the same date remain untouched; old
backups still import.

### Phase 4 --- Polish and advanced insights

-   Improve loading, empty, and error states.
-   Validate Android and iOS UI behavior.
-   Add completion-rate and consistency insights only after core event
    semantics are stable.
-   Consider explicit rest-day events only if they add meaningful value.

## 11. Testing plan

Add tests following existing project conventions. Prefer shared tests
for platform-independent date and domain logic.

### Calendar logic

-   February in a non-leap year.
-   February in a leap year.
-   28-, 30-, and 31-day months.
-   Month transition across December / January.
-   First day of week: Monday and Sunday (and other supported values).
-   Selected date in previous/next month padding.
-   Today marker and month navigation.
-   Multiple completed workouts on one date.
-   Workout timestamps near local midnight and timezone boundaries.

### History

-   Finished workouts appear on correct local dates.
-   Unfinished workout is excluded.
-   Multiple sessions on one date are all shown.
-   Monthly workout-day count uses unique dates.
-   Monthly session count counts sessions, not days.
-   Duration and volume aggregate correctly.

### Scheduling

-   Create schedule event.
-   Reschedule event.
-   Cancel event.
-   Multiple events on one date.
-   Deleted routine leaves a renderable snapshot.
-   Future events are not marked missed.
-   Cancelled events are not marked missed or included as active
    scheduled events.
-   Recurring weekday schedules remain unchanged.

### Workout linkage

-   Starting a scheduled workout preserves event ID.
-   Restored active workout retains event association.
-   Finishing workout completes the exact event.
-   Cancelling active workout leaves event planned.
-   Two scheduled events on one date are not cross-linked.
-   Repeated completion/link operation is safe.

### Persistence and backup

-   Fresh database creation.
-   Upgrade migration from the previous schema.
-   Schedule persistence after restart.
-   Backup and restore round trip.
-   Import of older backup without schedule events.
-   Merge and replace modes.
-   Linked workout references remain valid.

### Platform and localization

-   Android build and relevant tests.
-   iOS / shared compilation and relevant tests.
-   English and Indonesian string coverage.
-   No hardcoded visible strings.

## 12. Definition of Done

-   [ ] Calendar is embedded in Home; no new bottom-navigation tab.
-   [ ] Existing Weekly Horizon and recurring routine schedule behavior
    remain intact.
-   [ ] Month grid and month navigation work correctly.
-   [ ] First-day-of-week preference is respected.
-   [ ] Completed workout history is grouped by local calendar date.
-   [ ] Multiple workouts per date are supported.
-   [ ] Selected-date details display existing workout data.
-   [ ] Users can add a date-specific workout schedule.
-   [ ] Users can reschedule and cancel date-specific events.
-   [ ] Date-specific events do not mutate recurring routine weekdays.
-   [ ] Scheduled workout can be started using the existing workout
    logger.
-   [ ] Completion links the exact workout to the exact schedule event.
-   [ ] Schedule events persist across restarts.
-   [ ] SQLDelight migration verification passes.
-   [ ] Backup and restore include schedule events and remain
    backward-compatible.
-   [ ] English and Indonesian localization are complete.
-   [ ] Android and iOS builds pass.
-   [ ] Relevant unit and integration tests pass.
-   [ ] No unrelated refactors or destructive data changes are
    introduced.

## 13. Agent operating instructions

1.  **Inspect before editing.** The supplied ZIP is a baseline snapshot;
    the current working tree is authoritative.
2.  Do not assume file paths or APIs still match. Search the project and
    adapt to current conventions.
3.  Do not rewrite the Home screen or WorkoutRepository wholesale.
4.  Do not add a bottom-navigation destination for Calendar.
5.  Do not treat `scheduledDays` or `activeDays` as date-specific
    schedule records.
6.  Do not duplicate workout history in a new table.
7.  Do not infer a missed workout from an empty calendar date.
8.  Do not use weekday-only matching for date history.
9.  Do not hardcode user-facing strings or design colors.
10. Do not remove or reset existing user data.
11. Run relevant tests and builds after each phase; report exact
    commands and results.
12. If a required API, migration convention, or product behavior is
    ambiguous, inspect the repository first. Ask only if a safe
    implementation decision cannot be derived.
13. Keep changes scoped to this feature. Report changed files, migration
    details, test results, known limitations, and any follow-up work.
14. Implement one phase at a time and keep the project buildable between
    phases.

**Initial execution request:** Start with Phase 0 inspection. Then
implement Phase 1 only unless explicitly instructed to continue. Do not
silently proceed into database scheduling work before the history-first
calendar is reviewed.
