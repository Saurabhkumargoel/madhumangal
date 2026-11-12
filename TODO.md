# 📋 Madhumangal - Engineering Roadmap & Architecture TODO

This roadmap outlines high-impact technical initiatives, architecture improvements for multi-developer collaboration, and zero-cost project management practices.

---

## 🏗️ 1. Multi-Developer Project Architecture (Target: Next Major Build)

As multiple developers contribute to Madhumangal simultaneously, moving away from a monolithic single `:app` module is critical to prevent merge conflicts, enable parallel Gradle compilation, and establish clear domain boundaries.

### A. Modularization Plan (Multi-Module Gradle Structure)
Split the monolithic `:app` into decoupled feature and core modules:

```text
madhumangal/
├── core/
│   ├── common/         # Base models, Result monads, Logger, Coroutine dispatchers
│   ├── database/       # Room database, Entities, DAOs, Migrations
│   ├── audio/          # AudioRecordingService, PCM resampler, WAV writer, Buffer manager
│   └── stt/            # Vosk engine, Android SpeechRecognizer, Offline model loader
├── feature/
│   ├── assistant/      # Madhu conversational UI, Soundwave visualizer, Reminder NLP
│   ├── recorder/       # Live speech-to-text recording view, chunked audio playback
│   ├── notes/          # Task dashboard, category grouping, markdown exports
│   └── settings/       # Tier visibility filters (Smart/Advanced/Dev), SAF export
└── app/                # Application entry-point, Navigation host, Dependency wiring
```

- **Benefits:**
  - **Zero Git Merge Collisions:** Audio engineers work in `:core:audio`, UI devs work in `:feature:assistant`, and storage devs work in `:core:database`.
  - **Faster Incremental Builds:** Gradle only rebuilds modified modules rather than the whole app.
  - **Isolated Unit Testing:** Each module can be tested in isolation without spinning up full Compose UI tests.

### B. Dependency Centralization (Version Catalog)
- Migrate all dependencies in `build.gradle.kts` to `gradle/libs.versions.toml`.
- Guarantees consistent Compose, Kotlin, Coroutines, and Vosk SDK versions across all developers' workstations.

### C. Lightweight Dependency Injection
- Introduce lightweight dependency injection (Hilt or Koin) to decouple `MainViewModel` from direct instantiation of databases, audio managers, and Vosk transcribers.

---

## 👥 2. Zero-Cost Team Management & Collaboration Workflow

To coordinate multiple engineers without expensive enterprise tools (like paid Jira, Linear, or Bitbucket), leverage GitHub's built-in, completely free tools:

### A. Free GitHub Projects (Kanban Board)
- **Setup:** Create a free GitHub Project board in the `Saurabhkumargoel/madhumangal` repository.
- **Workflow Columns:**
  1. `Backlog` - Raw feature requests and ideas.
  2. `Todo` - Refined tasks ready for implementation.
  3. `In Progress` - Actively being worked on by an assigned developer.
  4. `In Review (PR)` - Pull Request submitted with automated test checks.
  5. `Done` - Merged into `main` and verified.
- **Issue Templates:** Standardized Markdown templates for `.github/ISSUE_TEMPLATE/` (`bug_report.md`, `feature_request.md`).

### B. Zero-Cost Automated CI/CD (GitHub Actions)
- Set up `.github/workflows/android_ci.yml` using GitHub's free monthly CI runner minutes.
- Triggers on every Pull Request to `main`:
  - Runs `./gradlew testDebugUnitTest` to guarantee 100% test pass rate.
  - Runs `./gradlew assembleDebug` to verify compilation.
  - Uploads the debug APK as an artifact for team testers to download and sideload immediately.

### C. Branch Protection & Review Discipline
- Protect `main` branch: require 1 peer review or green CI status before merging.
- Standardized branch naming convention (enforced in `.agrules`):
  - `feature/<short-description>`
  - `bug/<short-description>`
  - `hotfix/<short-description>`

---

## 🚀 3. Product & Feature Backlog

- [ ] **Bilingual Offline STT:** Support real-time code-switching between Hindi and English without manual model swapping.
- [ ] **Speaker Diarization:** Identify different speakers in continuous meetings (Speaker 1, Speaker 2).
- [ ] **Google Drive Backup via SAF:** Optional user-controlled cloud sync using Android Storage Access Framework without third-party backend servers.
- [ ] **Lock Screen Widget / Quick Settings Tile:** Instant 1-tap activation of Madhu voice assistant directly from Android quick settings.
