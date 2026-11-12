package com.madhumangal.bot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class MadhuConversationalTest {

    private val botKeywordRegex = Regex(
        "(?i)(madhu\\s*bot|madhu|मधु\\s*बॉट|मधु|मधुमंगल|हे\\s*मधु|hey\\s*madhu|hello\\s*madhu|madhumangal|good\\s*bot|gud\\s*bot|god\\s*bot|got\\s*bot|good\\s*boy|गुड\\s*बॉट|गॉड\\s*बॉट|गुड\\s*बहुत|गॉड\\s*बहुत|गुड\\s*भौत|गुड\\s*बात|गोद\\s*बहुत|गत\\s*बहुत|गुड़\\s*बहुत|गुड\\s*बोट|गुड\\s*वाट|गुड\\s*वाच|गुड\\s*बाप|गुड\\s*बाय|गुडबॉट|गॉडबॉट|गुडबहुत|गुड\\s*बाद|गत\\s*बाद|गोल्ड\\s*बाद|गोल्ड\\s*बॉन्ड|गोल्ड\\s*बहुत|घोर\\s*बहुत|गुरु\\s*बहुत|घर\\s*बहुत|गौर\\s*बहुत|गुट\\s*बहुत|गुड\\s*मौत|घटना\\s*मौत|गीत\\s*की\\s*मौत|गुड\\s*पाठ|गोड\\s*गोड\\s*बहुत|गुड\\s*गुड\\s*मौत|गुरु\\s*भूत|गोद\\s*बात|बोर्ड\\s*बाद|गत\\s*बॉट|गत\\s*आठ|दुखद\\s*मौत|गॉड\\s*गॉड|गर\\s*दौर|बहुत\\s*बाद|गॉड\\s*बाद|गॉड\\s*बात|गार्ड\\s*बात|गत\\s*बात|गत\\s*डॉट|रोड\\s*बाद|घर\\s*बैठे|घोर\\s*बाद|घोर\\s*बॉट|दैट\\s*दे)"
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
    fun testMadhuAndMadhumangalWakeWords() {
        val madhuWakeWords = listOf(
            "madhu",
            "Madhu",
            "मधु",
            "हे मधु",
            "hey madhu",
            "hello madhu",
            "madhumangal",
            "मधुमंगल",
            "मधु बॉट",
            "madhu bot"
        )
        for (wake in madhuWakeWords) {
            val matches = botKeywordRegex.containsMatchIn(wake)
            assertTrue("Expected wake word '$wake' to match botKeywordRegex", matches)
        }
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
            "Madhu",
            "मधु",
            "मधु"
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

    @Test
    fun testReminderTriggerPhrases() {
        val reminderPhrases = listOf(
            "मुझे याद दिलाना",
            "mujhe yaad dilana",
            "याद दिलाना",
            "याद दिलाओ",
            "याद दिला",
            "याद रखना",
            "remind me",
            "सेट रिमाइंडर",
            "कल शाम 5 बजे मुझे याद दिलाना दवा लेने के लिए",
            "remind me at 4pm to call doctor"
        )

        for (phrase in reminderPhrases) {
            val isReminder = phrase.contains("याद दिला") ||
                    phrase.contains("याद रखना") ||
                    phrase.lowercase(Locale.ROOT).contains("remind me") ||
                    phrase.lowercase(Locale.ROOT).contains("yaad dila") ||
                    phrase.lowercase(Locale.ROOT).contains("yaad rakh") ||
                    phrase.contains("रिमाइंडर")
            assertTrue("Expected '$phrase' to be recognized as reminder query", isReminder)
        }
    }

    @Test
    fun testReminderTimeParsingExtraction() {
        val test1 = "कल शाम 5 बजे दवा लेना मुझे याद दिलाना"
        var cleanTitle = test1.replace("मुझे याद दिलाना", "")
            .replace("याद दिलाना", "")
            .replace("remind me", "", ignoreCase = true)
            .trim()
        assertTrue(cleanTitle.contains("दवा लेना"))

        val test2 = "remind me at 4pm to call doctor"
        val isPm = test2.lowercase(Locale.ROOT).contains("pm")
        assertTrue(isPm)
    }

    @Test
    fun testCombinedTimeAndDateQueries() {
        val combinedQueries = listOf(
            "समय और तारीख",
            "समय तारीख",
            "time and date",
            "date and time",
            "कितने बजे हैं और आज क्या तारीख है"
        )

        for (q in combinedQueries) {
            val qLower = q.lowercase(Locale.ROOT)
            val hasTime = qLower.contains("time") || qLower.contains("टाइम") || qLower.contains("कितने बजे") || qLower.contains("के बजे") || (qLower.contains("समय") && !qLower.contains("मौसम")) || qLower.contains("वक्त")
            val hasDate = qLower.contains("date") || qLower.contains("तारीख") || qLower.contains("तिथि") || qLower.contains("दिनांक") || qLower.contains("डेट") || qLower.contains("दैट") || qLower.contains("दिन") || qLower.contains("देन") || qLower.contains("आज क्या") || qLower.contains("आज के") || qLower.contains("वार") || qLower.contains("today") || qLower.contains("day")
            assertTrue("Expected query '$q' to match both time and date", hasTime && hasDate)
        }
    }

    @Test
    fun testDirectUtilityQueriesWithoutWakeWord() {
        val utilityQueries = listOf(
            "क्या समय हुआ है",
            "कितने बजे हैं",
            "तारीख क्या है",
            "दिन कौन सा है",
            "battery percentage",
            "बैटरी कितनी है"
        )

        for (q in utilityQueries) {
            val qLower = q.lowercase(Locale.ROOT)
            val hasTime = qLower.contains("time") || qLower.contains("टाइम") || qLower.contains("कितने बजे") || (qLower.contains("समय") && !qLower.contains("मौसम"))
            val hasDate = qLower.contains("date") || qLower.contains("तारीख") || qLower.contains("दिन") || qLower.contains("today")
            val isBattery = qLower.contains("battery") || qLower.contains("बैटरी")
            val isDirect = hasTime || hasDate || isBattery
            assertTrue("Expected '$q' to be classified as direct utility query", isDirect)
        }
    }

    @Test
    fun testGranularTierVisibilityLogic() {
        fun isVisible(settingTier: String, appMode: String, isCurrentlyActive: Boolean): Boolean {
            if (isCurrentlyActive) return true
            return when (appMode) {
                "DEVELOPER" -> true
                "ADVANCED" -> settingTier == "SMART" || settingTier == "ADVANCED"
                "SMART" -> settingTier == "SMART"
                else -> false
            }
        }

        // SMART tier setting: visible everywhere
        assertTrue(isVisible("SMART", "SMART", false))
        assertTrue(isVisible("SMART", "ADVANCED", false))
        assertTrue(isVisible("SMART", "DEVELOPER", false))

        // ADVANCED tier setting: hidden in Smart, visible in Advanced and Dev
        assertFalse(isVisible("ADVANCED", "SMART", false))
        assertTrue(isVisible("ADVANCED", "ADVANCED", false))
        assertTrue(isVisible("ADVANCED", "DEVELOPER", false))

        // DEVELOPER tier setting: hidden in Smart & Advanced, visible in Dev
        assertFalse(isVisible("DEVELOPER", "SMART", false))
        assertFalse(isVisible("DEVELOPER", "ADVANCED", false))
        assertTrue(isVisible("DEVELOPER", "DEVELOPER", false))

        // Safeguarded active setting: always visible even if tier is Developer in Smart mode!
        assertTrue(isVisible("DEVELOPER", "SMART", true))
    }

    @Test
    fun testHomeThemesSupported() {
        val themes = listOf(
            com.madhumangal.data.UserPreferences.HOME_THEME_CHARCOAL,
            com.madhumangal.data.UserPreferences.HOME_THEME_MIDNIGHT,
            com.madhumangal.data.UserPreferences.HOME_THEME_EMERALD,
            com.madhumangal.data.UserPreferences.HOME_THEME_CARBON
        )
        assertEquals(4, themes.size)
        assertTrue(themes.contains(com.madhumangal.data.UserPreferences.HOME_THEME_CHARCOAL))
        assertTrue(themes.contains(com.madhumangal.data.UserPreferences.HOME_THEME_MIDNIGHT))
        assertTrue(themes.contains(com.madhumangal.data.UserPreferences.HOME_THEME_EMERALD))
        assertTrue(themes.contains(com.madhumangal.data.UserPreferences.HOME_THEME_CARBON))
    }

    @Test
    fun testReminderTriggersDetection() {
        val reminderQueries = listOf(
            "mujhe yaad dilana",
            "Mujhe Yaad Dilana",
            "मुझे याद दिलाना",
            "याद दिलाना",
            "याद दिलाओ",
            "याद दिला देना",
            "याद दिला",
            "remind me to call Mom",
            "remind me at 5pm",
            "याद रखना कल मीटिंग है"
        )

        for (q in reminderQueries) {
            val qLower = q.lowercase(Locale.ROOT)
            val isReminder = qLower.contains("याद दिला") ||
                    qLower.contains("याद दिलाना") ||
                    qLower.contains("remind me") ||
                    qLower.contains("याद रखना") ||
                    qLower.contains("yaad dilana") ||
                    qLower.contains("yaad rakhna")
            assertTrue("Expected query '$q' to be detected as reminder", isReminder)
        }
    }

    @Test
    fun testReminderFutureTimeCalculation() {
        fun extractReminderTime(query: String, baseNow: Long = System.currentTimeMillis()): Long {
            val qLower = query.lowercase(Locale.ROOT)
            val cal = java.util.Calendar.getInstance().apply { timeInMillis = baseNow }
            return when {
                qLower.contains("कल") || qLower.contains("tomorrow") -> {
                    cal.add(java.util.Calendar.DAY_OF_YEAR, 1)
                    cal.set(java.util.Calendar.HOUR_OF_DAY, 9)
                    cal.set(java.util.Calendar.MINUTE, 0)
                    cal.timeInMillis
                }
                qLower.contains("minute") || qLower.contains("मिनट") -> {
                    val match = Regex("(\\d+)\\s*(minute|min|मिनट)").find(qLower)
                    val mins = match?.groupValues?.get(1)?.toIntOrNull() ?: 10
                    baseNow + (mins * 60 * 1000L)
                }
                else -> baseNow + (60 * 60 * 1000L)
            }
        }

        val baseNow = System.currentTimeMillis()
        val tomorrowTime = extractReminderTime("मुझे कल याद दिलाना", baseNow)
        assertTrue("Tomorrow time must be greater than now", tomorrowTime > baseNow)

        val fifteenMinTime = extractReminderTime("remind me in 15 minutes", baseNow)
        val diffMins = (fifteenMinTime - baseNow) / (60 * 1000)
        assertEquals(15L, diffMins)
    }

    @Test
    fun testDevModeAdvancedOptionVisibility() {
        val devVisibleSettings = setOf(
            "card_display_mode",
            "card_theme",
            "card_voice_output"
        )

        fun isOptionVisibleInAdvancedMode(
            optionKey: String,
            activeValue: String,
            currentOptionValue: String,
            allowedDevSet: Set<String>
        ): Boolean {
            // Rule: Currently active option must ALWAYS be visible (safeguarded)
            if (activeValue == currentOptionValue) return true
            return allowedDevSet.contains(optionKey)
        }

        // Active option is always visible even if not in dev visible set
        assertTrue(isOptionVisibleInAdvancedMode("card_debug", "ACTIVE_VAL", "ACTIVE_VAL", devVisibleSettings))

        // In dev visible set, not active: visible
        assertTrue(isOptionVisibleInAdvancedMode("card_display_mode", "OTHER_VAL", "NEW_VAL", devVisibleSettings))

        // Not in dev visible set and not active: hidden
        assertFalse(isOptionVisibleInAdvancedMode("card_hidden_feature", "OTHER_VAL", "NEW_VAL", devVisibleSettings))
    }

    @Test
    fun testReverseRecordingsSortLogic() {
        data class Chunk(val id: Long, val timestamp: Long)
        val chunks = listOf(
            Chunk(1, 1000L),
            Chunk(2, 2000L),
            Chunk(3, 3000L)
        )

        // Normal order (chronological)
        val normal = chunks.sortedBy { it.timestamp }
        assertEquals(1L, normal.first().id)
        assertEquals(3L, normal.last().id)

        // Inverted order (newest first)
        val inverted = chunks.sortedByDescending { it.timestamp }
        assertEquals(3L, inverted.first().id)
        assertEquals(1L, inverted.last().id)
    }

    @Test
    fun testVoskModelValidationRequiresAllComponents() {
        val tempDir = java.io.File.createTempFile("vosk_test", "dir")
        tempDir.delete()
        tempDir.mkdirs()

        try {
            // Empty dir is NOT valid
            assertFalse(com.madhumangal.stt.VoskSTTEngine.isValidModelFolder(tempDir))

            // Only conf is NOT valid
            val confDir = java.io.File(tempDir, "conf").apply { mkdirs() }
            java.io.File(confDir, "model.conf").writeText("test")
            assertFalse(com.madhumangal.stt.VoskSTTEngine.isValidModelFolder(tempDir))

            // Only conf + am is NOT valid (still missing graph)
            val amDir = java.io.File(tempDir, "am").apply { mkdirs() }
            java.io.File(amDir, "final.mdl").writeText("test")
            assertFalse(com.madhumangal.stt.VoskSTTEngine.isValidModelFolder(tempDir))

            // All three (conf + am + graph) IS valid!
            val graphDir = java.io.File(tempDir, "graph").apply { mkdirs() }
            java.io.File(graphDir, "HCLG.fst").writeText("test")
            assertTrue(com.madhumangal.stt.VoskSTTEngine.isValidModelFolder(tempDir))
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testVoskModelFindsNestedSubdirectory() {
        val rootDir = java.io.File.createTempFile("vosk_nested", "dir")
        rootDir.delete()
        rootDir.mkdirs()

        try {
            val nestedModel = java.io.File(rootDir, "vosk-model-small-hi-0.22").apply { mkdirs() }
            java.io.File(nestedModel, "conf").apply { mkdirs() }.let { java.io.File(it, "model.conf").writeText("test") }
            java.io.File(nestedModel, "am").apply { mkdirs() }.let { java.io.File(it, "final.mdl").writeText("test") }
            java.io.File(nestedModel, "graph").apply { mkdirs() }.let { java.io.File(it, "Gr.fst").writeText("test") }

            val found = com.madhumangal.stt.VoskSTTEngine.findValidModelDir(rootDir)
            assertTrue("Expected to resolve nested valid model directory", found != null)
            assertEquals(nestedModel.absolutePath, found?.absolutePath)
        } finally {
            rootDir.deleteRecursively()
        }
    }

    @Test
    fun testMultiTurnConversationalWindowCountdownLogic() {
        var isWaitingForFollowUp = true
        var isBotSpeaking = true
        var windowSecondsRemaining = 15

        // While bot is speaking, timer must not decrement
        if (isWaitingForFollowUp && !isBotSpeaking) {
            windowSecondsRemaining--
        }
        assertEquals(15, windowSecondsRemaining)

        // Once bot finishes speaking, timer decrements
        isBotSpeaking = false
        if (isWaitingForFollowUp && !isBotSpeaking) {
            windowSecondsRemaining--
        }
        assertEquals(14, windowSecondsRemaining)
    }

    @Test
    fun testCustomBotNameAndAcousticAliasesMatching() {
        val customBotName = "Echo"
        val customWakeAliases = setOf("एकको", "ऐको", "एक्को", "इको")
        val baseVariants = mutableListOf("madhu", "मधु", Regex.escape(customBotName), "hey\\s*${Regex.escape(customBotName)}")
        customWakeAliases.forEach { baseVariants.add(Regex.escape(it)) }
        val dynamicRegex = Regex("(?i)(" + baseVariants.joinToString("|") + ")")

        assertTrue(dynamicRegex.containsMatchIn("Echo what is the time"))
        assertTrue(dynamicRegex.containsMatchIn("hey echo how are you"))
        assertTrue(dynamicRegex.containsMatchIn("एकको तारीख बताओ"))
        assertTrue(dynamicRegex.containsMatchIn("मधु समय बताओ"))
    }

    @Test
    fun testDefaultBufferLengthAndOverlapSettings() {
        val prefs = com.madhumangal.data.UserPreferences()
        assertEquals(1800, prefs.bufferLengthSeconds)
        assertEquals(10, prefs.overlapSeconds)
        assertTrue(prefs.inverseRecordingsSort)
        assertEquals("DETAILED", prefs.mediaViewMode)
        assertEquals(2.5f, prefs.orbScaleMultiplier, 0.01f)
    }

    @Test
    fun testReleaseNotesRegistryDynamicLatestVersion() {
        val allReleases = com.madhumangal.ui.releasenotes.ReleaseNotesRegistry.getAllReleases()
        assertTrue(allReleases.isNotEmpty())
        val latest = allReleases.first()
        assertTrue(latest.versionName.isNotBlank())
        assertTrue(latest.highlights.isNotEmpty())
    }
}

