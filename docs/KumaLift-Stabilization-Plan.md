# KumaLift – Stabilization Plan (after first on-device test)

Context: the app was installed on a real phone. Reported problems: (1) many bugs when switching between menus, (2) inconsistent language, (3) messy logic, (4) exercise GIFs look very pixelated. This plan is based on a static review of commit `8207791` plus an experiment on the real GIF assets. Verify each finding against the code before changing it.

## Working Rules

1. One task per commit. After each task run `./gradlew :composeApp:testDebugUnitTest` and install on a device to verify.
2. Do not add features while this plan is open. Only fix, simplify, and test.
3. No fake data, no success messages for actions that did not happen.
4. Every task ends with the acceptance checks listed in it. Do not mark done without running them on a device.
5. Pure local app: no server, no accounts, no analytics.

---

## A. Navigation rewrite (P0)

### Findings (`app/App.kt`)

- There are **two sources of truth** for the current tab: `screenStack.last()` and `pagerState.currentPage`. They are kept in sync by two `LaunchedEffect`s that write to each other:
  - `LaunchedEffect(pagerState.currentPage)` rewrites `screenStack` when the page changes.
  - `LaunchedEffect(currentScreen)` calls `pagerState.scrollToPage(...)` when the stack changes.
- `navigateTo(tab)` calls `animateScrollToPage(target)` **and** mutates `screenStack`. While the animation passes through intermediate pages (for example Home to Me passes Routines, Exercises, Progress), `currentPage` changes, the first effect writes the intermediate tab into the stack, and the second effect calls `scrollToPage(intermediate)`, which cancels or fights the running animation. Result: flicker, wrong landing tab, bottom bar highlighting the wrong item.
- The tab pages are inside a `HorizontalPager` while several pages contain horizontally scrollable rows (filter chips). Swipe gestures conflict.
- Back handling is split across `App.kt` and individual screens.

### Target design

1. Replace the dual state with one small, **pure Kotlin navigator** in `commonMain` (no Compose dependencies) so it can be unit tested:
   ```kotlin
   enum class Tab { HOME, ROUTINES, LIBRARY, PROGRESS, ME }
   sealed interface Overlay { object ActiveLogger; object Summary; object Settings }

   class AppNavigator {
       var selectedTab by mutableStateOf(Tab.HOME)      // single source of truth for tabs
       val overlays = mutableStateListOf<Overlay>()     // full-screen screens above the tabs
       fun selectTab(tab: Tab) { overlays.clear(); selectedTab = tab }   // define exactly what tab taps do
       fun push(overlay: Overlay)
       fun back(): Boolean   // pops overlay; else if tab != HOME -> HOME; else returns false (let the system exit)
   }
   ```
2. Remove the `HorizontalPager`. Render the selected tab with a plain `when (selectedTab)` (optionally a short `Crossfade`), wrapped in `rememberSaveableStateHolder()` so each tab keeps its scroll position and filters (keep the hoisted `LazyListState` and `LibraryFilterState`). Disable swipe between tabs entirely. Bottom bar taps are the only way to switch tabs.
3. The bottom bar highlights `navigator.selectedTab` directly. There are **no** `LaunchedEffect`s that sync navigation state.
4. Overlay screens (Active Logger, Summary, Settings) are drawn above the tab content; the bottom bar is hidden while an overlay is open (as now), and the active workout mini-bar behavior stays.
5. One root back handler: `KumaBackHandler(enabled = navigator.overlays.isNotEmpty() || navigator.selectedTab != Tab.HOME) { navigator.back() }`. Screens with their own sub-navigation (Library detail/create, Settings sub-pages) register their own handler, which takes priority because it is deeper in the composition. Document this order in a comment.
6. All cross-screen jumps (Me to Progress, Home to Exercises, "start workout") call `navigator.selectTab(...)` or `navigator.push(...)`; no screen mutates a stack directly.
7. Define and test these flows:
   - Start workout from any tab: Active Logger opens; Back returns to the same tab (not Home).
   - Finish workout: Summary opens; Done goes to Home.
   - Open Settings from Home or Me: Back returns to where it was opened.
   - Tapping the current tab again scrolls its list to top (optional) and never resets filters.
   - Rapid tapping across tabs never leaves the highlight on a different tab than the content.

### Acceptance

- Unit tests for `AppNavigator` covering every flow above (including `back()` at Home returning `false`).
- On device: tap Home, Me, Exercises, Progress, Routines in quick succession 20 times; content and highlight always match. Open an exercise detail, go to another tab and back: filters and scroll are preserved.

---

## B. Language consistency (P0)

### Findings

- Two parallel label systems exist for enums: `displayName` (hardcoded, partly Indonesian, partly English, for example `CHEST("Dada")`, `OTHER("Lainnya")`) and `localizedName(isEnglish)`. Several UI call sites use `displayName` directly, so labels depend on the screen: `CreateExerciseScreen`, `CreateRoutineScreen`, `ExerciseDetailScreen` (dialog text is hardcoded Indonesian: "Cara Mencatat Beban ..."), `LibraryFilterState` search.
- `WorkoutRepository` builds display strings (`secondaryMuscleDetails`, `equipmentDetail`) using `displayName` and stores them in models, so they are frozen in one language.
- Hundreds of inline `if (isEnglish) "..." else "..."` ternaries, plus a few remaining literals (`MeScreen` "Done" / "Simpan Ukuran Tubuh", `ExerciseDetailScreen`, `CreateRoutineScreen`, `ProfilePhotoCropDialog`).
- Strings outside Compose (Android notification text, `DataBackupManager` messages) are hardcoded in one language.
- Exercise names and instructions come from the dataset (English). This is acceptable for names, but must be a conscious, consistent rule.

### Target design

1. Introduce a type-safe string layer in `commonMain`:
   ```kotlin
   interface AppStrings {
       val tabHome: String
       val tabExercises: String
       // ... every user-visible string
       fun setsCompleted(done: Int, total: Int): String
   }
   object IdStrings : AppStrings { ... }
   object EnStrings : AppStrings { ... }
   val LocalStrings = staticCompositionLocalOf<AppStrings> { EnStrings }
   ```
   Provide it at the app root from `appearance.language` (via the existing `LanguageResolver`). Because both languages implement one interface, a missing translation is a **compile error**. Switching language updates the UI instantly.
2. Enum labels: remove `displayName` from `MuscleGroup`, `Equipment`, `MovementType` (keep stable `code`/`name` for storage and export). Add `fun MuscleGroup.label(s: AppStrings)` etc. Nothing in the UI may call `.displayName` or `.name` for display.
3. Do not store localized strings in models or the database. Store enums/keys; localize at display time. Fix `WorkoutRepository` (`secondaryMuscleDetails`, `equipmentDetail`) accordingly. Same for sub-muscle labels derived from the dataset `subMuscle` code: map code to a label in `AppStrings`.
4. Replace **every** `isEnglish` ternary and string literal in UI code with `LocalStrings.current`. Include `contentDescription`s, dialogs, snackbars, validation errors, empty states, chart labels, units text, settings rows, and the export/import messages. Pass `AppStrings` (or the language) explicitly to non-UI code that produces text: notifications (Android), `DataBackupManager`.
5. Dates, numbers and units: use one formatting path that respects language and the unit/date settings; no hardcoded "kg", "lb", month names, or "Today"/"Hari ini" literals in screens.
6. Dataset text: exercise names stay as in the dataset (document this in the README). For instructions, check whether the upstream dataset provides Indonesian; if yes, include it and select by language; if not, keep English and make sure labels around it are localized.
7. Add a **guard test** (JVM unit test, scans `commonMain` sources) that fails if it finds `Text("` followed by a letter, `isEnglish)` ternaries, or `.displayName` in UI packages. This prevents regressions.

### Acceptance

- Switch language Indonesian to English and back: no screen shows mixed languages (check Home, Exercises list and detail, Create Exercise, Routines, Create Routine, Logger, Summary, Progress, Me, every Settings page, dialogs, notifications, export messages).
- Guard test passes. `rg "isEnglish"` returns nothing in feature code.

---

## C. GIF quality: pre-process the assets (P0)

### Findings

- The bundled GIFs are **180x180 px, 256 colors, dithered**. They are shown at 180dp, which is about 470 to 540 physical pixels on a typical phone, so they are upscaled about 2.6 to 3 times. No rendering setting can add detail that does not exist; dithering noise also becomes visible as "pixels" when scaled.
- Experiment on a real frame (`3/4 Sit-up`): plain bicubic 3x (what the app does) shows jagged edges and dither noise. A neural super-resolution upscale (FSRCNN x3 or EDSR x3) gives clearly cleaner edges and smoother shading. FSRCNN takes about 0.04 s per frame on CPU (a full run of about 1,324 GIFs x 13 frames takes roughly 15 to 30 minutes). EDSR is sharper but about 20 s per frame on CPU, so use it only with a GPU or for a subset.
- Result files in the test: about 100 to 150 KB per exercise as one WebP sprite sheet (3x, quality 88), compared with about 95 KB per original GIF. Expect roughly 150 to 190 MB for the full set at quality 88, and about 110 MB at quality 78 to 80.

### Do

1. Add `tools/upscale_gifs.py` (provided with this plan) to the repo. It converts every GIF into a **sprite sheet WebP** (frames in a grid, white background) using FSRCNN x3, and writes `manifest.json` with frame size, grid layout and per-frame delays. Run:
   ```
   pip install opencv-contrib-python numpy pillow
   python tools/upscale_gifs.py --src composeApp/src/androidMain/assets/videos \
          --out composeApp/src/androidMain/assets/anim --scale 3 --quality 80 --resume
   ```
   Test first with `--limit 5`. Keep the original GIFs out of the shipped assets once the new files work (move them to an untracked folder; do not ship both).
2. Replace the GIF parsing path in `core/media` with a **sprite loader**: read `anim/<stem>.webp` and `manifest.json`, keep the sprite as **one** `ImageBitmap` (do not slice into separate bitmaps), and draw frame *i* with `drawImage(image, srcOffset, srcSize, dstOffset, dstSize, filterQuality = High)`. Keep `ControllableGifPlayer`'s public API, the pause/resume (time-based) behavior, the speed setting and the crossfade. Keep the old GIF parser only as a fallback if a sprite is missing.
3. **Memory:** one sprite is about 2160x1620x4 = 14 MB in memory. Limit the in-memory cache to 3 sprites (or about 48 MB), not 20. Decode off the main thread, and cancel loading when leaving composition. The sprite must stay under 4096 px on its longest side (the script's 4-column grid keeps it at 2160 px).
4. Keep the display size at 180dp. With 540 px frames this is about 1:1 on a 3x-density phone, so no visible upscaling remains.
5. Update `gifUrl`/asset resolution: map each exercise's existing `gifUrl` stem to `anim/<stem>.webp`. Update the README (asset pipeline, how to regenerate, size).
6. **License:** upscaled frames are derivative works of media that is (c) Gym visual. The existing note stays valid: a public or commercial release requires a license from the rights holder. Do not commit the upscaled files to a public repo if the repo is public.
7. If APK size becomes a problem, ship only a subset of the most common exercises and load the rest on demand (Play Asset Delivery), or lower `--quality`.

### Acceptance

- Side by side on the phone (old GIF player vs new sprite player) for 5 exercises: edges are smooth, no visible dithering or blocky pixels at 180dp.
- Pause stops on the exact current frame, play continues from it, and the speed setting still works.
- Opening 20 exercises in a row does not increase memory beyond the cache limit and causes no jank.

---

## D. Logic stabilization (P1)

The reported "messy logic" is not specific yet. Do this systematically:

1. Write `docs/QA-CHECKLIST.md` and run it **on a device** after tasks A to C. For each scenario, record pass/fail and fix failures:
   - Fresh install: no fake data anywhere; empty states are correct.
   - Start a workout from a routine, from freestyle, and with an exercise added from the library; log sets (weight, reps, RPE), complete and un-complete sets; add/remove sets and exercises; duplicate exercise in one workout.
   - Rest timer: starts on set completion per settings, adjusts, survives backgrounding, notification appears.
   - Finish workout: Summary values (duration, volume, sets, PRs) match what was logged; history entry exists; Home, Progress and Me update.
   - PRs: first-time set, weight PR, rep-range PR, undo a completed set (PR is revoked and the previous PR is restored).
   - Units: switch kg to lb and back; every screen shows consistent values; stored values do not drift.
   - Kill the app mid-workout: resume restores the exact state.
   - Backup, wipe, restore: everything returns.
   - Settings: every row changes behavior or is removed; values persist after restart.
2. For every failure, add a regression test at the repository or navigator level before fixing it.
3. Add structured **local** logging for unexpected states (invalid IDs, missing exercises, empty history when expected) behind the diagnostic-log setting, so crashes and oddities on the phone can be exported and shared.
4. Review state ownership: one owner per piece of state, UI state hoisted out of composables that can leave the composition (Crossfade, conditional branches). Search for `remember { mutableStateOf` in screens that are swapped by navigation and fix as done for the library filters.

---

## Order of Work

1. A (navigation), because several other bugs are symptoms of broken navigation state.
2. B (language), mostly mechanical, large diff; do it before more screens change.
3. C (GIF pipeline), independent; can run in parallel with B.
4. D (QA and logic), last, on the cleaned-up base.

## Definition of Done

- No navigation glitches in the rapid-switching test; back behaves as specified.
- No mixed-language screens; guard test in place.
- Exercise animations look smooth and sharp at 180dp on a real phone.
- QA checklist fully passes on a device, with regression tests for each bug found.
- `./gradlew :composeApp:testDebugUnitTest` passes.
