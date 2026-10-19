# Caliper OS — Complete Build Fix & Automated GitHub APK Pipeline

This plan resolves the remaining Android compilation and Gradle configuration issues that prevented the APK from building after the previous interrupted session, and sets up an automated GitHub build pipeline so your APK is generated cleanly without manual coding or setup.

## User Review & Critical Decisions

> [!IMPORTANT]
> Because you requested an automatic diagnosis and fix without needing to inspect technical build logs yourself, we will apply all required code, Gradle, and GitHub Actions fixes automatically once you click **Proceed**.

- **Confirmed Approach**: Automatically diagnose and fix all Kotlin compilation mismatches, remove unused cloud plugins that fail when `google-services.json` is absent, auto-generate the signing keystore from `debug.keystore.base64` during Gradle builds, and add a GitHub Actions workflow so GitHub builds a ready-to-download APK on every push.

---

## 1. Overview & Core Concept

- **What It Does**: Restores a 100% clean, zero-error Android APK build for **Caliper** (Precision Academic Telemetry, Schedule & Attendance Management System) both inside AI Studio and on GitHub.
- **Target Audience / Persona**: Students tracking daily lecture schedules, 75% attendance safe zones, and Continuous Internal Assessment (CIA) telemetry who want a reliable, one-click installable APK from GitHub.
- **Key Value**: Eliminates build failures caused by interrupted code edits, missing `google-services.json` plugin checks, and missing decoded keystore files on fresh GitHub clones.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Today Telemetry Feed**: Displays the `SlimVernierPrecisionBar` semester attendance gauge, date stepper, one-tap batch attendance (`ALL PRESENT` / `EXTRA SLOT`), active lecture cards with `PRESENT` / `ABSENT` / `OFF` tactile pills, and a collapsible completed lectures tray.
  2. **Safe Zone Radar**: Displays per-subject attendance rings, 75% target vernier bars, bunk buffers / recovery calculators, and register reconciliation dialogs.
  3. **Telemetry Audit & CIA**: Displays the smooth bezier attendance trajectory wave chart, CIA score breakdown (`/50`), academic milestone badges, interactive monthly audit calendar, and medical leave logs.
  4. **Settings & Hardware Engine**: Controls living fluid touch physics, haptic clicks, acoustic confirmation tones, 24-hour clock format, weekly timetable matrix, holiday windows, and local JSON vault backup/restore.
- **Visual Identity & Theme**:
  - *Aesthetic Direction*: **Precision Caliper OS** with Deep Obsidian Glass surfaces and ColorOS living fluid touch physics.
  - *Color Palette & Mood*: True OLED Pitch Black (`#000000`) canvas, Deep Obsidian Glass (`#0E121B` at 78% alpha) cards with 1dp top-left specular borders, Electric Emerald (`#00E676`) safe-zone indicators, Crimson Coral (`#FF5252`) bunk alerts, Amber Gold (`#FFD740`) warnings, and Cyan (`#00B0FF`) lecture accents.
  - *Typography & Hierarchy*: **Space Grotesk** for bold headers and subject titles paired with **JetBrains Mono** (`tnum`) for tabular attendance percentages, room numbers, and timestamps.
  - *Interactive Feedback & Motion*: Spring-physics touch scaling (`0.96f` press compression with `DampingRatioMediumBouncy`) paired with crisp hardware haptic ticks.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Self-Healing Keystore Decoding in Gradle**
  - *Chosen Approach*: Keep the existing signing configuration intact for upgrade compatibility, and add an automatic Gradle initialization step that decodes `debug.keystore.base64` into `debug.keystore` whenever `debug.keystore` is missing (such as on a fresh GitHub clone or CI runner).
  - *Why*: GitHub repositories only track `debug.keystore.base64` by default; when GitHub tries to build the APK, Gradle fails if `debug.keystore` has not been decoded yet.
  - *Alternatives Considered*: Requiring manual terminal commands before building (rejected because it breaks automated GitHub builds).
- **Decision 2: Remove Unused Google Services Plugin Requirement**
  - *Chosen Approach*: Remove the unused `google-services` plugin and unused Firebase BOM from the app build configuration since Caliper uses local Room SQLite + DataStore persistence and local JSON vault backups.
  - *Why*: Applying the Google Services plugin without a `google-services.json` file causes release/CI tasks on GitHub to fail or emit fatal configuration errors.
  - *Alternatives Considered*: Committing a dummy `google-services.json` file (rejected as unnecessary clutter for an offline-first Room database app).
- **Decision 3: Automated GitHub Actions APK Build Workflow**
  - *Chosen Approach*: Add a ready-to-run GitHub Actions workflow that sets up JDK 17 and the Android SDK, decodes the keystore, runs the Gradle APK assembly, and uploads the signed APK as a downloadable artifact on GitHub.
  - *Why*: Ensures that whenever code is pushed to GitHub, an installable APK is automatically built and available in the GitHub Actions tab without needing any coding knowledge.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        GITHUB & GRADLE BUILD PIPELINE                   │
│  [GitHub Actions / Gradle Init] ──> Auto-Decode debug.keystore.base64   │
│                                 ──> KSP Room Compiler + Compose Build   │
│                                 ──> Signed Installable APK Artifact     │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     CALIPER OS APPLICATION ARCHITECTURE                 │
│                                                                         │
│  ┌──────────────────────┐    ┌──────────────────────┐                   │
│  │   Room SQLite DB     │    │ Preferences DataStore│                   │
│  │ (Subjects, Slots,    │    │ (Haptics, Audio,     │                   │
│  │  Logs, Assessments,  │    │  24h Clock, Theme,   │                   │
│  │  Holidays, Leaves)   │    │  Semester Window)    │                   │
│  └──────────┬───────────┘    └──────────┬───────────┘                   │
│             └─────────────┬─────────────┘                               │
│                           ▼                                             │
│             ┌───────────────────────────┐                               │
│             │     CampusRepository      │                               │
│             │ (IERT CSE Sem-2 Seeding,  │                               │
│             │  Attendance & CIA Queries)│                               │
│             └─────────────┬─────────────┘                               │
│                           ▼                                             │
│             ┌───────────────────────────┐                               │
│             │       MainViewModel       │                               │
│             │ (Reactive StateFlows,     │                               │
│             │  75% Buffer Math, Streak, │                               │
│             │  Auto-Vault JSON Backup)  │                               │
│             └─────────────┬─────────────┘                               │
│                           ▼                                             │
│  ┌────────────────────────┴────────────────────────┐                    │
│  │           MainScaffold (3-Tab Glass Dock)       │                    │
│  ├───► TodayScreen (Hero Vernier + Active Slots)   │                    │
│  ├───► SafeZoneScreen (Subject Rings + Reconcile)  │                    │
│  ├───► AuditScreen (Wave Chart + CIA + Calendar)   │                    │
│  └───► SettingsScreen (Haptics + Timetable Matrix) │                    │
└─────────────────────────────────────────────────────────────────────────┘
```

- **Data Model & State**:
  - **Entities**: `SubjectEntity`, `TimetableSlotEntity`, `AttendanceLogEntity`, `AssessmentEntity`, `DailyDayStatusEntity`, `MedicalLeaveEntity`, `HolidayRangeEntity`, `CustomActivityEntity`, `TestRecordEntity`, and `QuickNoteEntity` aligned with computed convenience properties so all UI screens and dialogs compile cleanly.
  - **Reactive State**: `MainViewModel` combines Room `Flow` streams into `currentDaySlots`, `subjectStats`, `globalSummary`, `collegeStreak`, and `ciaBreakdown`.
- **Interactive Component & State Mapping**:
  - **Verification**: Full end-to-end compilation check via `compile_applet` to ensure all Kotlin source files, KSP Room annotations, resources, and Gradle scripts build with zero errors.
