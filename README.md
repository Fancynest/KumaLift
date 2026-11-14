# KumaLift 🐻⚡

**Minimalist High-Precision Strength Telemetry & Training Logger**

Built with **Compose Multiplatform (CMP)** targeting **Android** and **iOS** from a shared Kotlin codebase.

---

## 🎨 Design System: "Iron & Electric Minimalist Athletic"
- **Backgrounds:** `#0C0E12` (Lowest), `#111317` (Surface)
- **Cards & Surfaces:** `#1A1C20`, `#1E2024`, `#282A2E`
- **Primary Accent:** `#C6FF3D` (Electric Lime - strictly for completions, PRs, active telemetry, and CTAs)
- **Typography:** Inter with Tabular Numerals (`tnum`) for zero jitter during active sets and timers
- **Touch Ergonomics:** 48px+ touch targets, bottom-weighted controls for one-handed gym usage

---

## 💾 Local Storage Architecture: SQLDelight
KumaLift is **100% pure local** with zero remote servers, cloud dependencies, or remote analytics:
- **Engine:** [SQLDelight 2.0.2](https://cashapp.github.io/sqldelight/) generates type-safe multiplatform Kotlin APIs directly from SQLite schema (`KumaDatabase.sq`).
- **Choice Justification:** SQLDelight provides native SQLite performance across Android (`AndroidSqliteDriver`), iOS (`NativeSqliteDriver`), and JVM unit tests (`JdbcSqliteDriver`) without annotation processor friction or runtime reflection.
- **Tables:** `workouts`, `workout_exercises`, `sets`, `personal_records`, `bodyweight_log`, `custom_exercises`, `routines`, `routine_exercises`, `active_workout_state`, and `user_settings_storage`.
- **Durability:** The active workout is persisted after every completed set or weight change. In-progress sessions survive app termination, background OS kill, and reboot.
- **Settings & Migrations:** Full `UserSettings` hierarchy serialized with version tracking (`settingsVersion`) and automatic first-launch migration from legacy files.

---

## 📁 Project Structure

```text
KumaLift/
├── composeApp/
│   └── src/
│       ├── commonMain/kotlin/com/bearbones/kumalift/
│       │   ├── app/              # App(), Navigation Shell, Bottom Tabs
│       │   ├── core/
│       │   │   ├── designsystem/ # Color, Type, Shape, Theme, Reusable Components
│       │   │   ├── timer/        # RestTimer + Expect/Actual feedback
│       │   │   └── util/         # 1RM (Epley & Brzycki), Volume & Distribution
│       │   ├── data/
│       │   │   ├── local/        # Pre-seeded exercises, routines, PRs
│       │   │   └── repository/   # WorkoutRepository, ExerciseRepository, etc.
│       │   ├── domain/model/     # Workout, Exercise, SetLog, PersonalRecord, Routine
│       │   └── features/
│       │       ├── home/         # Home Dashboard & 7-day Weekly Horizon
│       │       ├── logger/       # Active Workout Logger & Persistent Rest Timer
│       │       ├── routines/     # Mesocycle & Routine Blueprints
│       │       ├── library/      # Exercise Library & Muscle Filters
│       │       ├── summary/      # Post-session debrief & Volume Breakdown
│       │       └── progress/     # 1RM Trajectory & Volume Charts
│       ├── commonTest/           # Unit tests (1RM, Volume, PR Detection)
│       ├── androidMain/          # Android MainActivity, Manifest
│       └── iosMain/              # iOS MainViewController
├── iosApp/                       # iOS Xcode SwiftUI wrapper
└── gradle/libs.versions.toml     # Version catalog
```

---

## 🚀 Running the Project

### Android
```bash
./gradlew :composeApp:installDebug
```
or open in Android Studio and select the **composeApp** run configuration.

### Tests
```bash
./gradlew :composeApp:testDebugUnitTest
```
Runs 100% of the unit test suite covering:
- Database CRUD persistence for workouts, sets, PRs, and user settings (`LocalDatabasePersistenceTests`).
- 1RM calculators (Epley, Brzycki, Lombardi, O'Conner) and PR detection rules (`KumaLiftTests`).
- Unique ID collision prevention and independent duplicate exercise execution (`WorkoutLogicTests`).
- Settings wiring, weight unit conversion (KG/LB), adherence horizon (`SettingsTests`).
- Full export/import round-trip validation and snapshot safety (`DataBackupManagerTests`).
- Exercise dataset catalog integrity (`ExerciseDatasetTests`).

### iOS (on macOS)
Open `iosApp/` in Xcode or run via CI/CD runner.

---

## 📱 Platform Media Architecture (Android-First & iOS)

- **Android:** Exercise thumbnails and animation GIFs are rendered locally with hardware acceleration via Coil (`androidMain/assets`).
- **iOS:** iOS target builds use lightweight vector icon placeholders (`ExerciseMedia.ios.kt`) keeping iOS builds fast and decoupled from binary asset bundles. Full cross-platform asset streaming via Coil 3 or Compose Resources can be enabled when provisioning an iOS production bundle.

---

## 📜 Media Attribution, Licensing & Asset Swapping

This project utilizes the fitness exercises dataset and media from:
**https://github.com/hasaneyldrm/exercises-dataset** (1,324 exercises with animation GIFs, 180×180 thumbnails, muscle group metadata, and step-by-step instructions).

- **Media Copyright:** The exercise thumbnail images and animation GIFs are property of **Gym visual** and used with attribution:
  > **© Gym visual — https://gymvisual.com/**
- **Licensing Requirement:** As per Gym visual's terms, any public, commercial, or production release of this application requires obtaining an independent license directly from **Gym visual** (https://gymvisual.com/content/3-terms-and-conditions-of-use). Do not distribute or publish binaries containing Gym visual copyrighted media without an active license.
- **Dataset (non-media):** The text dataset (names, categories, body parts, equipment, targets, and multilingual instructions) is distributed under the MIT License.
- **Swapping Media Sources / On-Demand Asset Delivery:**
  The `gifUrl` and `thumbnailUrl` properties in `Exercise.kt` cleanly abstract media locations. To swap in custom media (e.g., self-hosted CDN, Play Asset Delivery, or vector animations) without touching business logic:
  1. Set the desired URI scheme in `Exercise.thumbnailUrl` / `Exercise.gifUrl` (e.g. `https://cdn.example.com/exercises/...`).
  2. Implement a custom image loader in `core/media/ExerciseMedia.*.kt`.
