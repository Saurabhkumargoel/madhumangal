package com.echo.recorder.ui.releasenotes

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
            versionCode = 202610201,
            versionName = "0.0.2026.10.20",
            releaseDate = "October 20, 2026",
            summary = "Log Series Grouping & Pagination, Dynamic GoodBot Reasoning & Web Search, Noise Gate Threshold, and Studio Pencil FAB in All Modes.",
            highlights = listOf(
                ReleaseHighlight("NEW", "📁 Time-Gap Log Series Grouping: Automatically clusters log sessions based on configurable time-gap thresholds (2m, 5m, 15m, 30m, 1h, or Flat) with select-all and time span badges."),
                ReleaseHighlight("NEW", "📄 Log Pagination & Page Sizes: Configurable files per page (5, 10, 20, 50, All) with smooth Prev/Next page navigation."),
                ReleaseHighlight("NEW", "🧠 Dynamic Conversational GoodBot: Dynamic assistant reasoning answering 'तुम्हारा नाम क्या है', 'तुम क्या क्या कर सकते हो', 'कल क्या दिन था' (calendar math), today, tomorrow, and general questions."),
                ReleaseHighlight("NEW", "🌐 DuckDuckGo Web Search Integration: Prominent Live Web Search toggle in Settings feeding real-time internet context directly into AI responses."),
                ReleaseHighlight("NEW", "✏️ GoodBot Action Studio in All Modes: Action Studio pencil FAB is now unlocked and accessible across all modes (Normal, Advanced, Developer)."),
                ReleaseHighlight("IMPROVEMENT", "🛡️ Anti-Interruption Noise Gate: Configurable dB threshold (-35 dB to -15 dB, default -20 dB) and 1200ms grace period prevents GoodBot from cutting itself off while speaking."),
                ReleaseHighlight("IMPROVEMENT", "🧹 Crisp Settings Cleanup: Removed duplicate tab reordering and redundant bottom banner in Smart Mode for an uncluttered, intuitive settings experience."),
                ReleaseHighlight("IMPROVEMENT", "🗣️ Comprehensive Hindi Acoustic Variations: Added all phonetic approximations identified in real device logs ('गत बॉट', 'दुखद मौत', 'गॉड गॉड', 'गोल्ड बाद', 'गर दौर', etc.).")
            )
        ),
        ReleaseEntry(
            versionCode = 202610192,
            versionName = "0.0.2026.10.19",
            releaseDate = "October 19, 2026",
            summary = "Developer Mode, GoodBot Custom Studio, Zero-Crash Audio Stop, and Conversational Voice Assistant.",
            highlights = listOf(
                ReleaseHighlight("FIX", "🛑 Zero-Crash Recording Stop: Fixed critical crash when stopping recording while speaking by safely joining audio threads before native memory release."),
                ReleaseHighlight("FIX", "🔔 Remnant Notification Fixed: Dismisses zombie 'GoodEcho Recording Live' notifications and ensures notification stop button cleanly ends service."),
                ReleaseHighlight("NEW", "🤖 Conversational GoodBot (15s Window): After saying 'Good Bot', the bot stays active for 15s to hear your question without repeating wake words."),
                ReleaseHighlight("NEW", "🗣️ Expanded Hindi Acoustic Matching: Recognizes Vosk Hindi phonetic variations ('गुड बहुत', 'गॉड बॉट', 'गुड बात', 'गॉड बहुत', etc.)."),
                ReleaseHighlight("NEW", "⚡ Instant Voice Answers: Instant on-device TTS voice responses for Time, Date, and Battery status."),
                ReleaseHighlight("NEW", "✏️ GoodBot Action Studio & Dev Mode: Pencil FAB on GoodBot tab (in Developer Mode) lets you create custom buttons with size and screen placement."),
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
                ReleaseHighlight("NEW", "🤖 Minimalist Smart Assistant (GoodBot) with interactive orb and dynamic speech wave."),
                ReleaseHighlight("NEW", "📑 Background Continuous Audio Recording with automated 15-second rotation and overlap."),
                ReleaseHighlight("NEW", "🗂️ Reorderable App Tabs in Settings via drag-and-drop handles."),
                ReleaseHighlight("NEW", "🧠 Gemini & Local LLM Task Extractor with category grouping and temporal deadlines.")
            )
        )
    )

    fun getReleasesSince(lastSeenVersionCode: Int): List<ReleaseEntry> {
        if (lastSeenVersionCode <= 0) {
            // First time or legacy install: show the latest release, with all past releases available
            return releases.take(1)
        }
        val unread = releases.filter { it.versionCode > lastSeenVersionCode }
        return if (unread.isNotEmpty()) unread else releases.take(1)
    }

    fun getAllReleases(): List<ReleaseEntry> = releases
}
