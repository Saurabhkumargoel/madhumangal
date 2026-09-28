package com.echo.recorder.ai

import com.echo.recorder.data.NoteTask
import java.util.Locale

class TinyAiProcessor {

    companion object {
        // Rolling cache of recently extracted task titles to prevent duplication across overlapping audio chunks
        private val recentTaskTitles = java.util.Collections.synchronizedList(mutableListOf<String>())
        private const val MAX_RECENT_HISTORY = 40

        // Known Grocery, Vegetable & Shopping items in Hindi, Hinglish and English
        private val SHOPPING_ITEMS = listOf(
            "आलू" to "Aalu (Potato)", "aalu" to "Aalu (Potato)", "aloo" to "Aalu (Potato)",
            "टमाटर" to "Tamatar (Tomato)", "tamatar" to "Tamatar (Tomato)", "tomato" to "Tomato",
            "भिंडी" to "Bhindi (Okra)", "bhindi" to "Bhindi (Okra)", "okra" to "Okra",
            "करेला" to "Karela (Bitter Gourd)", "karela" to "Karela (Bitter Gourd)",
            "बैंगन" to "Baingan (Eggplant)", "baingan" to "Baingan (Eggplant)", "eggplant" to "Eggplant",
            "प्याज" to "Pyaz (Onion)", "pyaz" to "Pyaz (Onion)", "onion" to "Onion",
            "अदरक" to "Adrak (Ginger)", "adrak" to "Adrak (Ginger)", "ginger" to "Ginger",
            "लहसुन" to "Lahsun (Garlic)", "lahsun" to "Lahsun (Garlic)", "garlic" to "Garlic",
            "मिर्च" to "Mirchi (Chilli)", "mirchi" to "Mirchi (Chilli)", "chilli" to "Chilli",
            "धनिया" to "Dhaniya (Coriander)", "dhaniya" to "Dhaniya (Coriander)",
            "गोभी" to "Gobhi (Cauliflower)", "gobhi" to "Gobhi (Cauliflower)",
            "मटर" to "Matar (Peas)", "matar" to "Matar (Peas)", "peas" to "Peas",
            "पालक" to "Palak (Spinach)", "palak" to "Palak (Spinach)", "spinach" to "Spinach",
            "गाजर" to "Gajar (Carrot)", "gajar" to "Gajar (Carrot)", "carrot" to "Carrot",
            "खीरा" to "Kheera (Cucumber)", "kheera" to "Kheera (Cucumber)", "cucumber" to "Cucumber",
            "दूध" to "Milk", "doodh" to "Milk", "milk" to "Milk",
            "दही" to "Curd", "dahi" to "Curd",
            "मक्खन" to "Butter", "butter" to "Butter",
            "पनीर" to "Paneer", "paneer" to "Paneer",
            "ब्रेड" to "Bread", "bread" to "Bread",
            "अंडे" to "Eggs", "anda" to "Eggs", "eggs" to "Eggs",
            "दाल" to "Daal (Lentils)", "daal" to "Daal (Lentils)", "dal" to "Daal (Lentils)",
            "चावल" to "Rice", "chawal" to "Rice", "rice" to "Rice",
            "चीनी" to "Sugar", "cheeni" to "Sugar", "sugar" to "Sugar",
            "नमक" to "Salt", "namak" to "Salt", "salt" to "Salt",
            "तेल" to "Cooking Oil", "tel" to "Cooking Oil", "oil" to "Cooking Oil",
            "मसाला" to "Spices", "masala" to "Spices",
            "चाय" to "Tea", "chai" to "Tea", "tea" to "Tea",
            "कॉफ़ी" to "Coffee", "coffee" to "Coffee",
            "फल" to "Fruits", "fruit" to "Fruits", "fruits" to "Fruits",
            "सेब" to "Apple", "seb" to "Apple", "apple" to "Apple",
            "केला" to "Banana", "kela" to "Banana", "banana" to "Banana",
            "दवा" to "Medicine", "dawa" to "Medicine", "medicine" to "Medicine",
            "साबुन" to "Soap", "soap" to "Soap",
            "शैम्पू" to "Shampoo", "shampoo" to "Shampoo"
        )

        // Filler and speech artifact words to exclude when distilling item names
        private val FILLER_WORDS = setOf(
            "सब्जी", "sabzi", "sabji", "kaun", "kaun se", "kya", "kya kya", "lana", "hai", "lena",
            "gandhi", "gaandhi", "गांधी", "he", "aur", "and", "bhi", "tha", "thi", "hoga", "hogi",
            "ek", "do", "kilo", "gram", "packet", "list", "banao", "chahiye", "mangwana"
        )
    }

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
        if (cleanedText.isBlank() || 
            cleanedText.startsWith("[") ||
            cleanedText.startsWith("Voice speech recorded in") ||
            cleanedText.startsWith("Captured Hindi/Hinglish speech from") ||
            cleanedText.startsWith("Audio slice ready") ||
            cleanedText.startsWith("Audio chunk recorded")
        ) {
            return emptyList()
        }

        val tasks = mutableListOf<NoteTask>()

        // 1. Check for Shopping / Grocery / Sabzi item enumerations
        val shoppingTasks = extractShoppingListTasks(cleanedText, transcriptionId)
        if (shoppingTasks.isNotEmpty()) {
            tasks.addAll(shoppingTasks)
        }

        // 2. Split compound sentences by clause conjunctions ("aur", "and", "then", "fir", "pehle", punctuation)
        val clauses = splitIntoActionClauses(cleanedText)

        for (clause in clauses) {
            val sentence = clause.trim()
            if (sentence.length < 3) continue

            val extracted = analyzeSentence(sentence)
            if (extracted != null) {
                addTaskIfUnique(tasks, transcriptionId, extracted)
            }
        }

        // 3. Fallback: If no explicit task keywords or shopping items were detected, categorize smartly
        if (tasks.isEmpty() && cleanedText.length > 3) {
            val group = categorizeText(cleanedText)
            val isFruit = isQuickWin(cleanedText)
            val priority = determinePriority(cleanedText)
            val cleanTitle = rephraseAsCleanTitle(cleanedText, group)

            addTaskIfUnique(
                tasks,
                transcriptionId,
                ExtractedTask(
                    title = cleanTitle,
                    description = cleanedText,
                    group = group,
                    priority = priority,
                    isLowHangingFruit = isFruit
                )
            )
        }

        return tasks
    }

    /**
     * Detects grocery, vegetable, and shopping item enumerations in Hindi or English,
     * splits them into individual checkable tasks, and groups them cleanly under "Shopping".
     */
    private fun extractShoppingListTasks(text: String, transcriptionId: Long): List<NoteTask> {
        val lower = text.lowercase(Locale.ROOT)
        val foundItems = mutableListOf<Pair<String, String>>() // (matchedTerm, cleanName)

        for ((term, label) in SHOPPING_ITEMS) {
            // Match whole word or Hindi substring
            val regex = Regex("\\b${Regex.escape(term)}\\b|${Regex.escape(term)}")
            if (regex.containsMatchIn(lower) || lower.contains(term)) {
                if (foundItems.none { it.second == label }) {
                    foundItems.add(term to label)
                }
            }
        }

        if (foundItems.isEmpty()) return emptyList()

        val results = mutableListOf<NoteTask>()

        // If multiple items are mentioned, create a comprehensive master Shopping item
        if (foundItems.size >= 2) {
            val itemNamesList = foundItems.map { it.first }.joinToString(", ")
            val masterTitle = if (lower.contains("सब्जी") || lower.contains("sabzi") || lower.contains("sabji") || lower.contains("vegetable")) {
                "Buy Vegetables: $itemNamesList"
            } else {
                "Buy Groceries: $itemNamesList"
            }

            results.add(
                NoteTask(
                    transcriptionId = transcriptionId,
                    title = masterTitle,
                    description = "Shopping list extracted from voice: \"$text\"",
                    categoryGroup = "Shopping",
                    priority = NoteTask.PRIORITY_MEDIUM,
                    isLowHangingFruit = true,
                    timestampMillis = System.currentTimeMillis()
                )
            )

            // Also split each item into individual checkable shopping points
            for ((term, label) in foundItems) {
                results.add(
                    NoteTask(
                        transcriptionId = transcriptionId,
                        title = "Buy $term ($label)",
                        description = "Item in shopping list from voice: $term",
                        categoryGroup = "Shopping",
                        priority = NoteTask.PRIORITY_MEDIUM,
                        isLowHangingFruit = true,
                        timestampMillis = System.currentTimeMillis()
                    )
                )
            }
        } else if (foundItems.size == 1) {
            val (term, label) = foundItems.first()
            results.add(
                NoteTask(
                    transcriptionId = transcriptionId,
                    title = "Buy $term ($label)",
                    description = "Shopping item from voice: \"$text\"",
                    categoryGroup = "Shopping",
                    priority = NoteTask.PRIORITY_MEDIUM,
                    isLowHangingFruit = true,
                    timestampMillis = System.currentTimeMillis()
                )
            )
        }

        return results
    }

    /**
     * Splits compound voice sentences by clause conjunctions ("aur", "and", "then", "fir", etc.)
     */
    private fun splitIntoActionClauses(text: String): List<String> {
        val delimiters = Regex("(?i)[.!?\\n\\u0964]|\\b(aur|and|then|fir|phir|tatha|evam)\\b|,")
        return text.split(delimiters)
            .map { it.trim() }
            .filter { it.length > 2 }
    }

    private fun addTaskIfUnique(tasks: MutableList<NoteTask>, transcriptionId: Long, extracted: ExtractedTask) {
        val normTitle = extracted.title.lowercase(Locale.ROOT).trim()
        val isDuplicate = synchronized(recentTaskTitles) {
            recentTaskTitles.any { existing ->
                existing == normTitle || (existing.length > 8 && normTitle.length > 8 && (existing.contains(normTitle) || normTitle.contains(existing)))
            }
        }
        if (!isDuplicate) {
            synchronized(recentTaskTitles) {
                if (recentTaskTitles.size >= MAX_RECENT_HISTORY) {
                    recentTaskTitles.removeAt(0)
                }
                recentTaskTitles.add(normTitle)
            }
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

    private fun analyzeSentence(sentence: String): ExtractedTask? {
        val lower = sentence.lowercase(Locale.ROOT)

        // Action keywords in Hindi/Hinglish/English
        val actionKeywords = listOf(
            "karna hai", "karne hai", "karo", "bhoolna mat", "yaad rakhna", "chahiye",
            "buy", "call", "send", "fix", "clean", "meeting", "submit", "pay", "order", "book",
            "doodh", "khareedna", "baat karna", "email", "reminder", "todo", "to-do",
            "bhejna", "dekhna hai", "milna", "jana hai", "mangwana", "transfer", "note",
            "सब्जी", "खरीदना", "लाना", "देना", "फोन", "कॉल", "भेजना", "जाना", "मिलना"
        )

        val hasAction = actionKeywords.any { lower.contains(it) }
        if (!hasAction) return null

        val group = categorizeText(sentence)
        val priority = determinePriority(sentence)
        val isFruit = isQuickWin(sentence)

        val cleanTitle = rephraseAsCleanTitle(sentence, group)

        return ExtractedTask(
            title = cleanTitle,
            description = "Extracted from voice transcript: \"$sentence\"",
            group = group,
            priority = priority,
            isLowHangingFruit = isFruit
        )
    }

    private fun rephraseAsCleanTitle(raw: String, group: String): String {
        var clean = raw.trim()

        // Strip leading filler questions/artifacts
        val prefixesToStrip = listOf(
            "kaun kaun se", "kaun se", "kya kya", "ye hai", "ye ye",
            "list banao", "mujhe chahiye", "humko chahiye", "ek kaam karo"
        )
        var lower = clean.lowercase(Locale.ROOT)
        for (prefix in prefixesToStrip) {
            if (lower.startsWith(prefix)) {
                clean = clean.substring(prefix.length).trim()
                lower = clean.lowercase(Locale.ROOT)
            }
        }

        // Add action prefix if missing
        return when (group) {
            "Shopping" -> if (!clean.startsWith("Buy", ignoreCase = true) && !clean.startsWith("Khareedna", ignoreCase = true)) "Buy: ${clean.capitalize(Locale.ROOT)}" else clean.capitalize(Locale.ROOT)
            "Personal" -> if (clean.contains("call") || clean.contains("कॉल") || clean.contains("फोन")) "Call: ${clean.capitalize(Locale.ROOT)}" else clean.capitalize(Locale.ROOT)
            "Work" -> if (!clean.startsWith("Task:", ignoreCase = true)) "Work: ${clean.capitalize(Locale.ROOT)}" else clean.capitalize(Locale.ROOT)
            "Urgent" -> "⚠️ Urgent: ${clean.capitalize(Locale.ROOT)}"
            else -> clean.capitalize(Locale.ROOT)
        }.take(80)
    }

    private fun categorizeText(text: String): String {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            // Shopping, Groceries, Vegetables, Medicines
            lower.contains("buy") || lower.contains("khareedna") || lower.contains("खरीदना") ||
            lower.contains("sabzi") || lower.contains("सब्जी") || lower.contains("vegetable") ||
            lower.contains("vegetables") || lower.contains("groceries") || lower.contains("kirana") ||
            lower.contains("किराना") || lower.contains("rashan") || lower.contains("राशन") ||
            lower.contains("doodh") || lower.contains("दूध") || lower.contains("bread") ||
            lower.contains("market") || lower.contains("dukaan") || lower.contains("store") ||
            lower.contains("order") || lower.contains("mangwana") || lower.contains("दवा") ||
            lower.contains("medicine") || SHOPPING_ITEMS.any { lower.contains(it.first) } -> "Shopping"

            // Work, Office, Software, Clients, Meetings
            lower.contains("work") || lower.contains("office") || lower.contains("ऑफिस") ||
            lower.contains("project") || lower.contains("प्रोजेक्ट") || lower.contains("meeting") ||
            lower.contains("मीटिंग") || lower.contains("report") || lower.contains("email") ||
            lower.contains("ईमेल") || lower.contains("code") || lower.contains("bug") ||
            lower.contains("client") || lower.contains("क्लाइंट") || lower.contains("boss") ||
            lower.contains("salary") || lower.contains("invoice") || lower.contains("deadline") ||
            lower.contains("task") || lower.contains("टास्क") || lower.contains("kam") ||
            lower.contains("kaam") || lower.contains("काम") -> "Work"

            // Personal, Family, Health, Friends
            lower.contains("call") || lower.contains("कॉल") || lower.contains("phone") ||
            lower.contains("फोन") || lower.contains("mummy") || lower.contains("मम्मी") ||
            lower.contains("papa") || lower.contains("पापा") || lower.contains("home") ||
            lower.contains("ghar") || lower.contains("घर") || lower.contains("family") ||
            lower.contains("फैमिली") || lower.contains("friend") || lower.contains("दोस्त") ||
            lower.contains("birthday") || lower.contains("बर्थडे") || lower.contains("doctor") ||
            lower.contains("डॉक्टर") || lower.contains("gym") || lower.contains("जिम") ||
            lower.contains("walk") || lower.contains("वॉक") -> "Personal"

            // Urgent if no specific domain matched
            lower.contains("urgent") || lower.contains("turant") || lower.contains("तुरंत") ||
            lower.contains("asap") || lower.contains("emergency") || lower.contains("इमरजेंसी") ||
            lower.contains("jaldi") || lower.contains("जल्दी") || lower.contains("pay bill") ||
            lower.contains("bill") || lower.contains("बिल") -> "Urgent"

            else -> "Ideas"
        }
    }

    private fun determinePriority(text: String): Int {
        val lower = text.lowercase(Locale.ROOT)
        return when {
            lower.contains("urgent") || lower.contains("turant") || lower.contains("asap") ||
            lower.contains("important") || lower.contains("today") || lower.contains("aaj") ||
            lower.contains("emergency") || lower.contains("तुरंत") || lower.contains("जल्दी") -> NoteTask.PRIORITY_HIGH

            lower.contains("later") || lower.contains("baad me") || lower.contains("someday") ||
            lower.contains("maybe") || lower.contains("फुर्सत") -> NoteTask.PRIORITY_LOW

            else -> NoteTask.PRIORITY_MEDIUM
        }
    }

    private fun isQuickWin(text: String): Boolean {
        val lower = text.lowercase(Locale.ROOT)
        // Quick wins (Low-hanging fruits): calls, grocery items, quick reminders under 5 mins
        return lower.contains("call") || lower.contains("phone") || lower.contains("कॉल") ||
               lower.contains("message") || lower.contains("buy") || lower.contains("doodh") ||
               lower.contains("milk") || lower.contains("wish") || lower.contains("remind") ||
               lower.contains("quick") || lower.contains("send") || lower.contains("sabzi") ||
               lower.contains("सब्जी") || SHOPPING_ITEMS.any { lower.contains(it.first) }
    }

    private fun String.capitalize(locale: Locale): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}
