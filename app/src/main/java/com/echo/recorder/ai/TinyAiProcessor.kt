package com.echo.recorder.ai

import com.echo.recorder.data.NoteTask
import java.util.Locale

class TinyAiProcessor {

    data class ExtractedTask(
        val title: String,
        val description: String,
        val group: String,
        val priority: Int,
        val isLowHangingFruit: Boolean
    )

    /**
     * Parses raw transcribed text in Hindi, Hinglish, or English into structured NoteTask definitions.
     */
    fun processTranscription(text: String, transcriptionId: Long): List<NoteTask> {
        val cleanedText = text.trim()
        if (cleanedText.isBlank() || cleanedText.startsWith("[")) {
            return emptyList()
        }

        val tasks = mutableListOf<NoteTask>()
        val lines = cleanedText.split(Regex("[.!?\n]"))

        for (line in lines) {
            val sentence = line.trim()
            if (sentence.length < 3) continue

            val extracted = analyzeSentence(sentence)
            if (extracted != null) {
                tasks.add(
                    NoteTask(
                        transcriptionId = transcriptionId,
                        title = extracted.title,
                        description = extracted.description,
                        categoryGroup = extracted.group,
                        priority = extracted.priority,
                        isLowHangingFruit = extracted.isLowHangingFruit,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        }

        // If no explicit task keywords were found, turn the non-empty speech into a note entry
        if (tasks.isEmpty() && cleanedText.length > 5) {
            val group = categorizeText(cleanedText)
            val isFruit = isQuickWin(cleanedText)
            val priority = determinePriority(cleanedText)
            tasks.add(
                NoteTask(
                    transcriptionId = transcriptionId,
                    title = cleanedText.take(60),
                    description = cleanedText,
                    categoryGroup = group,
                    priority = priority,
                    isLowHangingFruit = isFruit,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }

        return tasks
    }

    private fun analyzeSentence(sentence: String): ExtractedTask? {
        val lower = sentence.lowercase(Locale.ROOT)

        // Action keywords in Hindi/Hinglish/English
        val actionKeywords = listOf(
            "karna hai", "karne hai", "karo", "bhoolna mat", "yaad rakhna",
            "buy", "call", "send", "fix", "clean", "meeting", "submit", "pay",
            "doodh", "khareedna", "baat karna", "email", "reminder", "todo", "to-do"
        )

        val hasAction = actionKeywords.any { lower.contains(it) }
        if (!hasAction) return null

        val group = categorizeText(sentence)
        val priority = determinePriority(sentence)
        val isFruit = isQuickWin(sentence)

        val cleanTitle = sentence.capitalize(Locale.ROOT)

        return ExtractedTask(
            title = cleanTitle,
            description = "Extracted from voice transcript: \"$sentence\"",
            group = group,
            priority = priority,
            isLowHangingFruit = isFruit
        )
    }

    private fun categorizeText(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("work") || lower.contains("office") || lower.contains("project") ||
            lower.contains("meeting") || lower.contains("report") || lower.contains("email") ||
            lower.contains("code") || lower.contains("bug") || lower.contains("client") -> "Work"

            lower.contains("buy") || lower.contains("khareedna") || lower.contains("doodh") ||
            lower.contains("bread") || lower.contains("groceries") || lower.contains("market") ||
            lower.contains("dukaan") || lower.contains("store") -> "Shopping"

            lower.contains("call") || lower.contains("phone") || lower.contains("mummy") ||
            lower.contains("papa") || lower.contains("home") || lower.contains("ghar") ||
            lower.contains("family") || lower.contains("friend") || lower.contains("birthday") -> "Personal"

            lower.contains("urgent") || lower.contains("turant") || lower.contains("asap") ||
            lower.contains("emergency") || lower.contains("pay bill") || lower.contains("bill") -> "Urgent"

            else -> "Ideas"
        }
    }

    private fun determinePriority(text: String): Int {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("urgent") || lower.contains("turant") || lower.contains("asap") ||
            lower.contains("important") || lower.contains("today") || lower.contains("aaj") ||
            lower.contains("emergency") -> NoteTask.PRIORITY_HIGH

            lower.contains("later") || lower.contains("baad me") || lower.contains("someday") ||
            lower.contains("maybe") -> NoteTask.PRIORITY_LOW

            else -> NoteTask.PRIORITY_MEDIUM
        }
    }

    private fun isQuickWin(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        // Quick wins (Low-hanging fruits): calls, simple buys, quick reminders under 5 mins
        return lower.contains("call") || lower.contains("phone") || lower.contains("message") ||
               lower.contains("buy milk") || lower.contains("doodh") || lower.contains("wish") ||
               lower.contains("remind") || lower.contains("quick") || lower.contains("send email")
    }

    private fun String.capitalize(locale: Locale): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}
