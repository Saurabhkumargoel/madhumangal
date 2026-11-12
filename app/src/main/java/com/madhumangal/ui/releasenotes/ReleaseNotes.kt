package com.madhumangal.ui.releasenotes

import com.madhumangal.util.AppConstants

data class ReleaseHighlight(
    val category: String, // "NEW", "FIX", "IMPROVEMENT"
    val text: String
)

data class ReleaseEntry(
    val versionCode: Int,
    val versionName: String,
    val releaseDate: String,
    val summary: String,
    val highlights: List<ReleaseHighlight>
)

object ReleaseNotesRegistry {

    val releases: List<ReleaseEntry> = listOf(
        ReleaseEntry(
            versionCode = 202611131,
            versionName = "0.0.2026.11.13",
            releaseDate = "November 13, 2026",
            summary = "Voice Training Studio for Custom Bot Names, Material You Theming, Multi-File Log Sharing, List/Grid/Detailed Media View, 2.5x Voice Orb with Dev Positioning, and Safe Run Flow Confirmation.",
            highlights = listOf(
                ReleaseHighlight("NEW", "🎙️ Custom Bot Name & Voice Acoustic Training Studio: Set your custom wake name and train the app on your voice. As you speak, the studio records what STT hears and expands your custom acoustic alias list."),
                ReleaseHighlight("NEW", "🎨 Material You Theme & Polished Light Theme: Android dynamic Monet theming matched to your wallpaper, plus crisp, high-contrast, corporate-grade Light Theme."),
                ReleaseHighlight("NEW", "📤 Multi-File Direct Log Sharing: Select multiple log sessions and share the actual .txt files directly via system share sheet with visible session count indicator."),
                ReleaseHighlight("NEW", "🗂️ Media View Mode Switching: Switch seamlessly between List (compact), Grid (2-column waveforms), and Detailed views in Recordings tab."),
                ReleaseHighlight("NEW", "🔮 2.5x Voice Orb & Dev Mode Customizer: Scaled voice orb 2.5x with fixed center anchor (no jumping when touched) and dev mode sliders for scale & offset."),
                ReleaseHighlight("NEW", "⚡ Safe 'Run Flow' Confirmation Dialog: Prompts before running heavy AI/STT pipeline with customizable options (Notes, Tasks, STT Model)."),
                ReleaseHighlight("IMPROVEMENT", "⏱️ 30-Minute Slices & 10s Overlap Defaults: Professional long-duration background audio recording standard."),
                ReleaseHighlight("IMPROVEMENT", "🏷️ Madhu Mangal Branding: Added space in app name ('Madhu Mangal') and unified assistant title.")
            )
        ),
        ReleaseEntry(
            versionCode = 202611121,
            versionName = "0.0.2026.11.12",
            releaseDate = "November 12, 2026",
            summary = "Multi-Turn Conversational Follow-Up Window Fix, Strict Vosk Kaldi Validation, and Live Google Speech Hybrid Integration.",
            highlights = listOf(
                ReleaseHighlight("FIX", "🗣️ Conversational Follow-Up Window: Countdown strictly pauses while Madhu is speaking, giving you the full 15-second window after speech finishes for follow-up questions."),
                ReleaseHighlight("FIX", "🧠 Vosk Model Validation & Integrity: Strict conjunction check for Kaldi components (am, conf, graph) and nested directory auto-resolution to eliminate 'Failed to create a model' errors."),
                ReleaseHighlight("NEW", "🌐 Google Speech Hybrid Live Recognizer: Active live streaming recognition in background service with multilingual Hindi/Hinglish detection."),
                ReleaseHighlight("NEW", "📁 Storage Access Framework (SAF) Custom Export: Pick folder and filename on every backup.")
            )
        ),
        ReleaseEntry(
            versionCode = 202611051,
            versionName = "0.0.2026.11.05",
            releaseDate = "November 5, 2026",
            summary = "'मुझे याद दिलाना' Calendar Reminders, SAF Custom Settings Export, Granular Tier Filtering (Dev/Adv/Smart), Dynamic Home Animations, and Onboarding Setup Wizard.",
            highlights = listOf(
                ReleaseHighlight("NEW", "📅 'मुझे याद दिलाना' Voice Reminders: Speak 'कल शाम 5 बजे मुझे याद दिलाना' or 'remind me at 4pm' to automatically extract title/time and launch your calendar with prefilled reminder details."),
                ReleaseHighlight("NEW", "📁 SAF Custom Path Settings Export: Prompts to select folder and filename on every export via Android Storage Access Framework."),
                ReleaseHighlight("NEW", "🎛️ Granular Dev/Adv/Smart Visibility: Devs can configure each setting and sub-option to be visible in Smart, Advanced, or Developer mode."),
                ReleaseHighlight("NEW", "🚀 First-Run Onboarding Setup Wizard: Prompts for language, mode, and voice model selection on initial startup."),
                ReleaseHighlight("FIX", "🗣️ Conversational Interruption Window: Interruption silences speech without closing the listening window so follow-up queries work seamlessly."),
                ReleaseHighlight("IMPROVEMENT", "🎨 Rich Home Aesthetics: Charcoal gradient themes, pulsing audio waves, larger recording button, and space-optimized padding.")
            )
        ),
        ReleaseEntry(
            versionCode = 202610202,
            versionName = "0.0.2026.10.20.1",
            releaseDate = "October 20, 2026",
            summary = "Multi-Turn Conversational Madhu, Safe 2-Back Exit, Selection Cancellation on Back, Google Offline Language Pack Settings, and Smart Hybrid Hinglish STT.",
            highlights = listOf(
                ReleaseHighlight("FIX", "🤖 Multi-Turn Continuous Madhu: Conversational window remains active for 12s after each response so you can ask consecutive questions ('और मौसम?', 'तारीख?') without repeating wake-words."),
                ReleaseHighlight("FIX", "🔁 Repeated Utterance Support: Removed string equality deduplication block so repeated queries like 'तारीख' or 'समय' are immediately recognized."),
                ReleaseHighlight("NEW", "📅 Instant Date & Day Voice Answers: Direct answers for 'तारीख बताएं', 'दिन कौन सा है', 'आज क्या तारीख है', 'आज क्या दिन है'."),
                ReleaseHighlight("NEW", "🔙 Smart Back Navigation: Pressing Back cancels item selection in Logs and Dashboard first. Pressing Back twice within 5 seconds exits app with Toast prompt on 1st back."),
                ReleaseHighlight("NEW", "📥 Google Offline Language Pack Settings: 1-tap button in Settings to directly open Google Voice Typing to download Hindi and English offline models."),
                ReleaseHighlight("IMPROVEMENT", "🇮🇳 Smart Hybrid Multilingual STT (Hinglish): Fixed intent extras typo and enabled Google Speech multilingual language detection (hi-IN, en-IN, hi-Latn, en-US) for natural mixed speech."),
                ReleaseHighlight("IMPROVEMENT", "🗣️ Latest Acoustic Log Variations: Added 'गुड भौत', 'गॉड बाद', 'गत बात', 'गत डॉट', 'रोड बाद', 'घर बैठे', 'घोर बाद', 'घोर बॉट' to wake-word matcher.")
            )
        ),
        ReleaseEntry(
            versionCode = 202610201,
            versionName = "0.0.2026.10.20",
            releaseDate = "October 20, 2026",
            summary = "Log Series Grouping & Pagination, Dynamic Madhu Reasoning & Web Search, Noise Gate Threshold, and Studio Pencil FAB in All Modes.",
            highlights = listOf(
                ReleaseHighlight("NEW", "📁 Time-Gap Log Series Grouping: Automatically clusters log sessions based on configurable time-gap thresholds (2m, 5m, 15m, 30m, 1h, or Flat) with select-all and time span badges."),
                ReleaseHighlight("NEW", "📄 Log Pagination & Page Sizes: Configurable files per page (5, 10, 20, 50, All) with smooth Prev/Next page navigation."),
                ReleaseHighlight("NEW", "🧠 Dynamic Conversational Madhu: Dynamic assistant reasoning answering 'तुम्हारा नाम क्या है', 'तुम क्या क्या कर सकते हो', 'कल क्या दिन था' (calendar math), today, tomorrow, and general questions."),
                ReleaseHighlight("NEW", "🌐 DuckDuckGo Web Search Integration: Prominent Live Web Search toggle in Settings feeding real-time internet context directly into AI responses."),
                ReleaseHighlight("NEW", "✏️ Madhu Action Studio in All Modes: Action Studio pencil FAB is now unlocked and accessible across all modes (Normal, Advanced, Developer)."),
                ReleaseHighlight("IMPROVEMENT", "🛡️ Anti-Interruption Noise Gate: Configurable dB threshold (-35 dB to -15 dB, default -20 dB) and 1200ms grace period prevents Madhu from cutting itself off while speaking."),
                ReleaseHighlight("IMPROVEMENT", "🧹 Crisp Settings Cleanup: Removed duplicate tab reordering and redundant bottom banner in Smart Mode for an uncluttered, intuitive settings experience."),
                ReleaseHighlight("IMPROVEMENT", "🗣️ Comprehensive Hindi Acoustic Variations: Added all phonetic approximations identified in real device logs ('गत बॉट', 'दुखद मौत', 'गॉड गॉड', 'गोल्ड बाद', 'गर दौर', etc.).")
            )
        ),
        ReleaseEntry(
            versionCode = 202610192,
            versionName = "0.0.2026.10.19",
            releaseDate = "October 19, 2026",
            summary = "Developer Mode, Madhu Custom Studio, Zero-Crash Audio Stop, and Conversational Voice Assistant.",
            highlights = listOf(
                ReleaseHighlight("FIX", "🛑 Zero-Crash Recording Stop: Fixed critical crash when stopping recording while speaking by safely joining audio threads before native memory release."),
                ReleaseHighlight("FIX", "🔔 Remnant Notification Fixed: Dismisses zombie 'Madhumangal Recording Live' notifications and ensures notification stop button cleanly ends service."),
                ReleaseHighlight("NEW", "🤖 Conversational Madhu (15s Window): After saying 'Madhu', the bot stays active for 15s to hear your question without repeating wake words."),
                ReleaseHighlight("NEW", "🗣️ Expanded Hindi Acoustic Matching: Recognizes Vosk Hindi phonetic variations ('मधु', 'गॉड बॉट', 'गुड बात', 'गॉड बहुत', etc.)."),
                ReleaseHighlight("NEW", "⚡ Instant Voice Answers: Instant on-device TTS voice responses for Time, Date, and Battery status."),
                ReleaseHighlight("NEW", "✏️ Madhu Action Studio & Dev Mode: Pencil FAB on Madhu tab (in Developer Mode) lets you create custom buttons with size and screen placement."),
                ReleaseHighlight("NEW", "⚙️ Beta Tester Settings Export/Import: 1-tap export of entire app preferences to JSON file via Android Share sheet, plus fast JSON import."),
                ReleaseHighlight("NEW", "📄 Direct .txt Log Files: Session logs now save and export natively with .txt extension for instant tap-to-open."),
                ReleaseHighlight("NEW", "🏷️ Dynamic Build Version: App header and settings dynamically reflect the exact installed build version.")
            )
        ),
        ReleaseEntry(
            versionCode = 202610191,
            versionName = "0.0.2026.10.19.1800",
            releaseDate = "October 19, 2026",
            summary = "Offline Vosk Streaming STT and Zero-Contention PCM Audio Pipeline.",
            highlights = listOf(
                ReleaseHighlight("NEW", "🎙️ Streaming Vosk STT: Real-time offline speech transcription directly fed from raw PCM audio buffer, bypassing Android AudioPolicy mic blocks."),
                ReleaseHighlight("IMPROVEMENT", "🔄 Dynamic PCM Resampler: Smoothly converts between hardware sample rate (8kHz-48kHz) and Vosk 16kHz standard."),
                ReleaseHighlight("NEW", "📊 Multi-Session Log Export: Select multiple session logs and export/share merged logs directly.")
            )
        ),
        ReleaseEntry(
            versionCode = 202609291,
            versionName = "0.0.2026.09.29",
            releaseDate = "September 29, 2026",
            summary = "Core Smart Assistant, AI Task Extractor, and Customizable Layouts.",
            highlights = listOf(
                ReleaseHighlight("NEW", "🤖 Minimalist Smart Assistant (Madhu) with interactive orb and dynamic speech wave."),
                ReleaseHighlight("NEW", "📑 Background Continuous Audio Recording with automated 15-second rotation and overlap."),
                ReleaseHighlight("NEW", "🗂️ Reorderable App Tabs in Settings via drag-and-drop handles."),
                ReleaseHighlight("NEW", "🧠 Gemini & Local LLM Task Extractor with category grouping and temporal deadlines.")
            )
        )
    )

    fun getAllReleases(): List<ReleaseEntry> {
        val currentVersion = try { AppConstants.APP_VERSION } catch (_: Throwable) { "" }
        val currentCode = try { AppConstants.VERSION_CODE } catch (_: Throwable) { 0 }
        if (releases.isEmpty()) return emptyList()
        val latest = releases.first()
        val dynamicLatest = latest.copy(
            versionName = if (currentVersion.isNotBlank()) currentVersion else latest.versionName,
            versionCode = if (currentCode > 0) currentCode else latest.versionCode
        )
        return listOf(dynamicLatest) + releases.drop(1)
    }

    fun getReleasesSince(lastSeenVersionCode: Int): List<ReleaseEntry> {
        val all = getAllReleases()
        val currentCode = try { AppConstants.VERSION_CODE } catch (_: Throwable) { 0 }
        if (lastSeenVersionCode <= 0 || (currentCode > 0 && currentCode > lastSeenVersionCode)) {
            return all.take(1)
        }
        val unread = all.filter { it.versionCode > lastSeenVersionCode }
        return if (unread.isNotEmpty()) unread else all.take(1)
    }
}
