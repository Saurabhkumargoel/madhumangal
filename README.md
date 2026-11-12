# 🌸 Madhumangal (मधुमंगल)

> **Autonomous On-Device Voice Assistant & Real-Time Speech Transcription for Android**
> *Privacy-First, 100% Offline Capable, Supporting Hindi, Hinglish & English.*

[![Platform](https://img.shields.io/badge/Platform-Android_10+-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![UI](https://img.shields.io/badge/UI-Jetpack_Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![STT](https://img.shields.io/badge/STT-Vosk_Offline_(45MB)-orange.svg?style=flat)](https://alphacephei.com/vosk/)
[![AI](https://img.shields.io/badge/AI-Google_Gemini_1.5-blue.svg?style=flat&logo=google)](https://ai.google.dev/)
[![License](https://img.shields.io/badge/License-Apache_2.0-green.svg)](LICENSE)

---

## 📖 Overview

**Madhumangal** is a privacy-first personal assistant and audio transcription engine engineered natively for Android. Designed specifically to excel with mixed Hindi and Indian English (Hinglish), it pairs a lightning-fast on-device speech recognizer with **Madhu (मधु)**, an intelligent conversational companion that organizes your schedule, creates reminders, transcribes long meetings, and answers questions without sending your raw audio to third-party cloud servers.

---

## ✨ Key Features

### 🎙️ 1. Madhu (मधु) — The Conversational Voice Assistant
- **Natural Wake-Word Detection:** Say *"मधु"* (*"Madhu"*), *"हे मधु"*, *"Hey Madhu"*, or *"मधुमंगल"* to activate the listening orb instantly.
- **12–15s Conversational Follow-Up Window:** Once activated, Madhu maintains an active listening window after responding, allowing you to ask follow-up questions (*"और मौसम?"*, *"तारीख?"*) naturally without repeating the wake-word.
- **Acoustic Noise-Gate Interruption:** Talk while Madhu is speaking to interrupt smoothly. Configurable dB threshold (-35 dB to -15 dB) and grace period prevent speaker feedback from cutting off speech prematurely.

### ⚡ 2. 100% Offline Speech-to-Text (Vosk Hindi)
- Powered by the lightweight **Vosk Hindi Small acoustic model (45 MB)**.
- Transcribes spoken Hindi and Hinglish in real time directly on your phone's CPU.
- Works in airplanes, subways, or remote locations with zero mobile data or Wi-Fi.

### 🔔 3. Natural Language Reminders & Calendar Bridge
- Speak naturally: *"मुझे याद दिलाना कल 10 बजे मीटिंग है"* or *"Remind me tomorrow at 4 PM to call doctor"*.
- Madhu parses the event title and scheduled time, automatically populating the system calendar via Android's `CalendarContract.ACTION_INSERT` intent.
- Configurable target calendar app (Google Calendar, Samsung Calendar, or system default).

### 🎚️ 4. Adaptive Tier System (Smart, Advanced, Developer)
- **Smart Mode:** Distraction-free, OLED dark minimalist dashboard with essential voice controls.
- **Advanced Mode:** Carefully curated power options for audio quality, language preferences, and cloud reasoning.
- **Developer Mode:** Complete engineering control with granular visibility toggles, allowing developers to configure which settings appear in Advanced Mode while safeguarding active configurations.

### 📁 5. Scoped Storage SAF JSON Export
- Export settings, transcript logs, and audio backups safely using Android's **Storage Access Framework (SAF)** `ActivityResultContracts.CreateDocument`.
- Choose the exact destination directory every time without granting invasive storage permissions.

---

## 🏛️ Architecture & Tech Stack

```text
com.echo.recorder
├── ai/                     # Gemini API client & on-device TinyAi reasoning
├── audio/                  # AudioRecordingService, PCM resampler, WAV writer
├── data/                   # Room database, Entities, DAOs, UserPreferences
├── stt/                    # Vosk offline engine, Android SpeechRecognizer
├── ui/
│   ├── components/         # Waveform visualizer, dialogs, soundwave bars
│   ├── screens/            # DumbHomeTab (Madhu), RecorderTab, SettingsTab
│   ├── theme/              # OLED black gradient palette, Typography
│   └── viewmodel/          # MainViewModel (StateFlow, Coroutine lifecycle)
└── util/                   # ReminderHelper, StorageManager, AppLogger
```

- **Language & Runtime:** Kotlin 2.0 with Coroutines and StateFlow.
- **UI Toolkit:** Jetpack Compose with Material 3 design tokens.
- **Audio Pipeline:** Low-latency 16 kHz Mono PCM audio recording buffer.
- **Local Persistence:** Room Database with full SQLite indexing.
- **Cloud Reasoning:** Google Gemini 1.5 Flash via REST endpoints.

---

## 🚀 Getting Started & Building from Source

### Prerequisites
- **Android Studio:** Ladybug (2024.2) or newer.
- **JDK:** Version 17 or higher.
- **Android SDK:** Compile SDK 35, Min SDK 26 (Android 8.0+).

### Quick Build
```bash
# Clone the repository
git clone https://github.com/Saurabhkumargoel/madhumangal.git
cd madhumangal

# Run unit tests
./gradlew testDebugUnitTest

# Assemble Debug APK (automatically exported to .shared/ directory)
./gradlew assembleDebug
```

---

## 👥 Team & Contributing

We welcome contributions! Please review [`TODO.md`](TODO.md) for our upcoming modularization roadmap and zero-cost GitHub collaboration workflow.

### Branching Convention
When creating new branches, please use the standard format:
- `feature/<short-description>` for new capabilities
- `bug/<short-description>` for bug fixes
- `hotfix/<short-description>` for urgent patches

---

## 📄 License

This project is licensed under the Apache License 2.0. See the `LICENSE` file for details.
