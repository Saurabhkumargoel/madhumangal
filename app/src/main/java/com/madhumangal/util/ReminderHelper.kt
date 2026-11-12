package com.madhumangal.util

import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import com.madhumangal.data.UserPreferences
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedReminder(
    val title: String,
    val timeMillis: Long,
    val formattedTime: String,
    val rawQuery: String
)

object ReminderHelper {

    private val REMINDER_TRIGGER_REGEX = Regex(
        "(?i)(mujhe\\s*yaad\\s*dilan|yaad\\s*dilan|yaad\\s*dilao|yaad\\s*rakh|remind\\s*me|याद\\s*दिला|याद\\s*रख|रिमाइंडर|रिमाइंड|set\\s*(a\\s*)?reminder)"
    )

    private val HINDI_NUMERAL_MAP = mapOf(
        "एक" to 1, "दो" to 2, "तीन" to 3, "चार" to 4, "पांच" to 5, "पाँच" to 5,
        "छह" to 6, "सात" to 7, "आठ" to 8, "नौ" to 9, "दस" to 10, "ग्यारह" to 11, "बारह" to 12,
        "ek" to 1, "do" to 2, "teen" to 3, "char" to 4, "paanch" to 5,
        "chhe" to 6, "saat" to 7, "aath" to 8, "nau" to 9, "das" to 10, "gyarah" to 11, "barah" to 12
    )

    fun isReminderQuery(query: String): Boolean {
        return REMINDER_TRIGGER_REGEX.containsMatchIn(query)
    }

    fun parseReminder(query: String, nowMillis: Long = System.currentTimeMillis()): ParsedReminder {
        val qLower = query.lowercase(Locale.ROOT).trim()

        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMillis
        }

        val isTomorrow = qLower.contains("kal") || qLower.contains("कल") || qLower.contains("tomorrow")
        val isDayAfterTomorrow = qLower.contains("parso") || qLower.contains("परसों") || qLower.contains("day after tomorrow")

        if (isDayAfterTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 2)
        } else if (isTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val isEvening = qLower.contains("shaam") || qLower.contains("शाम") || qLower.contains("evening")
        val isMorning = qLower.contains("subah") || qLower.contains("सुबह") || qLower.contains("morning")
        val isAfternoon = qLower.contains("dopahar") || qLower.contains("दोपहर") || qLower.contains("afternoon")
        val isNight = qLower.contains("raat") || qLower.contains("रात") || qLower.contains("night")
        val isPmExplicit = qLower.contains("pm") || qLower.contains("पीएम")
        val isAmExplicit = qLower.contains("am") || qLower.contains("एएम")

        var hour = -1
        var minute = 0

        // 1. Try matching digits: e.g. "5:30", "5 baje", "10 बजे", "4 pm"
        val digitMatch = Regex("(\\d{1,2})(?::(\\d{2}))?\\s*(?:baje|बजे|am|pm|एएम|पीएम|o'clock)?").find(qLower)
        if (digitMatch != null) {
            val h = digitMatch.groupValues[1].toIntOrNull()
            if (h != null && h in 1..24) {
                hour = h
                val minStr = digitMatch.groupValues[2]
                if (minStr.isNotEmpty()) {
                    minute = minStr.toIntOrNull() ?: 0
                }
            }
        }

        // 2. If no digit match, try Hindi/Hinglish word numerals: e.g. "पाँच बजे", "das baje"
        if (hour == -1) {
            for ((word, num) in HINDI_NUMERAL_MAP) {
                if (qLower.contains("$word baje") || qLower.contains("$word बजे") || qLower.contains("$word")) {
                    hour = num
                    break
                }
            }
        }

        // Adjust hour for AM/PM context
        if (hour != -1) {
            if ((isEvening || isNight || isAfternoon || isPmExplicit) && hour in 1..11) {
                hour += 12
            } else if (isMorning && hour == 12) {
                hour = 0
            } else if (isAmExplicit && hour == 12) {
                hour = 0
            } else if (hour in 1..6 && !isMorning && !isAmExplicit) {
                // If user says "5 baje" without saying morning/evening, assume PM for 1..6
                hour += 12
            }
            cal.set(Calendar.HOUR_OF_DAY, hour)
            cal.set(Calendar.MINUTE, minute)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            // If scheduled for today but time has already passed, bump to tomorrow
            if (!isTomorrow && !isDayAfterTomorrow && cal.timeInMillis <= nowMillis) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }
        } else {
            // Default time if no hour detected: +1 hour or tomorrow 9 AM if late
            val currentHour = cal.get(Calendar.HOUR_OF_DAY)
            if (currentHour >= 21) {
                cal.add(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 9)
                cal.set(Calendar.MINUTE, 0)
            } else {
                cal.add(Calendar.HOUR_OF_DAY, 1)
                cal.set(Calendar.MINUTE, 0)
            }
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
        }

        // Extract Title: strip trigger keywords and temporal markers
        var cleanTitle = query
        val triggersToRemove = listOf(
            "mujhe yaad dilana ki", "mujhe yaad dilana", "mujhe yaad dilao ki", "mujhe yaad dilao",
            "yaad dilana ki", "yaad dilana", "yaad dilao ki", "yaad dilao", "yaad rakhna ki", "yaad rakhna",
            "remind me to", "remind me that", "remind me", "set a reminder for", "set a reminder to", "set reminder",
            "मुझे याद दिलाना कि", "मुझे याद दिलाना", "याद दिलाना कि", "याद दिलाना",
            "याद दिलाओ कि", "याद दिलाओ", "याद रखना कि", "याद रखना",
            "रिमाइंडर लगाना", "रिमाइंडर सेट करना", "रिमाइंडर"
        )
        for (trig in triggersToRemove) {
            cleanTitle = cleanTitle.replace(Regex("(?i)$trig"), "")
        }

        // Remove temporal words from title
        val timeWordsToRemove = listOf(
            "kal", "aaj", "parso", "tomorrow", "today", "tonight",
            "कल", "आज", "परसों",
            "shaam ko", "shaam", "subah ko", "subah", "dopahar ko", "dopahar", "raat ko", "raat",
            "शाम को", "शाम", "सुबह को", "सुबह", "दोपहर को", "दोपहर", "रात को", "रात",
            "ko", "को", "baje", "बजे"
        )
        for (w in timeWordsToRemove) {
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$w\\b"), "")
        }

        // Remove digit time tokens from title (e.g. "5:30", "10")
        cleanTitle = cleanTitle.replace(Regex("\\b\\d{1,2}(?::\\d{2})?\\s*(?:am|pm|एएम|पीएम)?\\b", RegexOption.IGNORE_CASE), "")

        // Remove Hindi numeral words from title
        for (w in HINDI_NUMERAL_MAP.keys) {
            cleanTitle = cleanTitle.replace(Regex("(?i)\\b$w\\b"), "")
        }

        // Clean trailing auxiliary verbs and punctuation
        val cleanRegex = Regex("(?i)\\b(hai|hoga|karna hai|karni hai|h|to|about)\\b")
        cleanTitle = cleanTitle.replace(cleanRegex, "").trim()
        cleanTitle = cleanTitle.replace(Regex("^[\\s,.:;!?-]+|[\\s,.:;!?-]+$"), "").trim()

        if (cleanTitle.isBlank()) {
            cleanTitle = if (query.contains(Regex("[\\u0900-\\u097F]"))) "रिमाइंडर" else "Reminder"
        }

        val sdf = SimpleDateFormat("EEEE, d MMM hh:mm a", Locale.getDefault())
        val formattedTime = sdf.format(cal.time)

        return ParsedReminder(
            title = cleanTitle,
            timeMillis = cal.timeInMillis,
            formattedTime = formattedTime,
            rawQuery = query
        )
    }

    fun launchCalendarReminder(context: Context, reminder: ParsedReminder, targetCalendarApp: String): Boolean {
        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, reminder.title)
            putExtra(CalendarContract.Events.DESCRIPTION, "Created by Echo: ${reminder.rawQuery}")
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, reminder.timeMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, reminder.timeMillis + 30 * 60 * 1000)
            putExtra(CalendarContract.Events.HAS_ALARM, 1)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val targetPackage = when (targetCalendarApp) {
            UserPreferences.CALENDAR_APP_GOOGLE -> "com.google.android.calendar"
            UserPreferences.CALENDAR_APP_SAMSUNG -> "com.samsung.android.calendar"
            else -> null
        }

        if (!targetPackage.isNullOrBlank()) {
            intent.setPackage(targetPackage)
        }

        return try {
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            // Fallback if specific calendar package is not installed
            if (intent.`package` != null) {
                try {
                    intent.setPackage(null)
                    context.startActivity(intent)
                    true
                } catch (e2: Exception) {
                    AppLogger.e("REMINDER", "Failed to launch calendar intent: ${e2.message}", e2)
                    false
                }
            } else {
                AppLogger.e("REMINDER", "Failed to launch default calendar intent: ${e.message}", e)
                false
            }
        }
    }
}
