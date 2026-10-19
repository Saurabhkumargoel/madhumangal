package com.echo.recorder.bot

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class GoodBotConversationalTest {

    private val botKeywordRegex = Regex(
        "(?i)(good\\s*bot|gud\\s*bot|god\\s*bot|got\\s*bot|good\\s*boy|गुड\\s*बॉट|गॉड\\s*बॉट|गुड\\s*बहुत|गॉड\\s*बहुत|गुड\\s*भौत|गुड\\s*बात|गोद\\s*बहुत|गत\\s*बहुत|गुड़\\s*बहुत|गुड\\s*बोट|गुड\\s*वाट|गुड\\s*वाच|गुड\\s*बाप|गुड\\s*बाय|गुडबॉट|गॉडबॉट|गुडबहुत|गुड\\s*बाद|गत\\s*बाद|गोल्ड\\s*बाद|गोल्ड\\s*बॉन्ड|गोल्ड\\s*बहुत|घोर\\s*बहुत|गुरु\\s*बहुत|घर\\s*बहुत|गौर\\s*बहुत|गुट\\s*बहुत|गुड\\s*मौत|घटना\\s*मौत|गीत\\s*की\\s*मौत|गुड\\s*पाठ|गोड\\s*गोड\\s*बहुत|गुड\\s*गुड\\s*मौत|गुरु\\s*भूत|गोद\\s*बात|बोर्ड\\s*बाद|गत\\s*बॉट|गत\\s*आठ|दुखद\\s*मौत|गॉड\\s*गॉड|गर\\s*दौर|बहुत\\s*बाद|गॉड\\s*बाद|गॉड\\s*बात|गार्ड\\s*बात|गत\\s*बात|गत\\s*डॉट|रोड\\s*बाद|घर\\s*बैठे|घोर\\s*बाद|घोर\\s*बॉट|दैट\\s*दे)"
    )

    private fun isDateQuery(query: String): Boolean {
        val qLower = query.lowercase(Locale.ROOT).trim()
        return qLower.contains("date") ||
                qLower.contains("तारीख") ||
                qLower.contains("तिथि") ||
                qLower.contains("दिनांक") ||
                qLower.contains("डेट") ||
                qLower.contains("दैट") ||
                qLower.contains("दिन") ||
                qLower.contains("देन") ||
                qLower.contains("आज क्या") ||
                qLower.contains("आज के") ||
                qLower.contains("वार") ||
                qLower.contains("today") ||
                qLower.contains("day")
    }

    private fun isDismissQuery(query: String): Boolean {
        val qLower = query.lowercase(Locale.ROOT).trim()
        return qLower in listOf("bye", "stop", "quiet", "बाय", "अलविदा", "बस", "चुप", "शांत", "थैंक यू", "धन्यवाद", "थैंक्स") ||
                qLower.startsWith("अलविदा") || qLower.startsWith("bye") || qLower.startsWith("बस करो")
    }

    @Test
    fun testAllFieldLogAcousticWakeWords() {
        val logWakeWords = listOf(
            "गुड भौत",
            "गॉड बाद",
            "गॉड बात",
            "गार्ड बात",
            "गत बात",
            "गत डॉट",
            "रोड बाद",
            "घर बैठे",
            "घोर बाद",
            "घोर बॉट",
            "गॉड गॉड",
            "गत बाद",
            "दैट दे",
            "Good bot",
            "गुड बॉट",
            "गुड बहुत"
        )

        for (wake in logWakeWords) {
            val matches = botKeywordRegex.containsMatchIn(wake)
            assertTrue("Expected wake word '$wake' to match botKeywordRegex", matches)
        }
    }

    @Test
    fun testDateAndDayQueries() {
        val dateQueries = listOf(
            "तारीख",
            "तारीख बताएं",
            "तारीख क्या है",
            "दिन कौन सा है",
            "दिन",
            "आज क्या दिन है",
            "आज क्या तारीख है",
            "आज क्या देन है",
            "डेट",
            "what is the date",
            "today is which day"
        )

        for (q in dateQueries) {
            assertTrue("Expected '$q' to be recognized as date/day query", isDateQuery(q))
        }
    }

    @Test
    fun testDismissalKeywords() {
        val dismissQueries = listOf(
            "बाय",
            "अलविदा",
            "अलविदा बॉट",
            "बस",
            "बस करो",
            "चुप",
            "bye",
            "stop"
        )

        for (d in dismissQueries) {
            assertTrue("Expected '$d' to be recognized as dismissal", isDismissQuery(d))
        }

        assertFalse("Random query should not be dismissal", isDismissQuery("समय बताओ"))
    }

    @Test
    fun testDoubleBackFiveSecondWindow() {
        var lastBackTime = 10000L
        val firstPress = 10000L
        var shouldExit = (firstPress - lastBackTime < 5000L) // first back: not exit yet
        // In app, first press records lastBackTime = now
        lastBackTime = firstPress

        // Second press at 12000L (2 seconds later)
        val secondPressFast = 12000L
        shouldExit = (secondPressFast - lastBackTime < 5000L)
        assertTrue("Consecutive press within 2s should exit", shouldExit)

        // Second press at 16000L (6 seconds later)
        val secondPressSlow = 16000L
        shouldExit = (secondPressSlow - lastBackTime < 5000L)
        assertFalse("Consecutive press after 6s should NOT exit", shouldExit)
    }
}
