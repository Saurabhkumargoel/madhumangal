package com.echo.recorder.ai

import com.echo.recorder.data.NoteTask
import com.echo.recorder.data.NoteTaskDao
import java.util.Locale

class TinyAiProcessor {

    companion object {
        // Rolling cache of recently extracted task titles to prevent duplication across overlapping audio chunks
        private val recentTaskTitles = java.util.Collections.synchronizedList(mutableListOf<String>())
        private const val MAX_RECENT_HISTORY = 40

        data class ItemSpec(val hi: String, val en: String)

        private val SHOPPING_ITEMS_MAP = mapOf(
            "आलू" to ItemSpec("आलू", "Potato"), "aalu" to ItemSpec("आलू", "Potato"), "aloo" to ItemSpec("आलू", "Potato"),
            "टमाटर" to ItemSpec("टमाटर", "Tomato"), "tamatar" to ItemSpec("टमाटर", "Tomato"), "tomato" to ItemSpec("टमाटर", "Tomato"),
            "भिंडी" to ItemSpec("भिंडी", "Okra"), "bhindi" to ItemSpec("भिंडी", "Okra"), "okra" to ItemSpec("भिंडी", "Okra"),
            "करेला" to ItemSpec("करेला", "Bitter Gourd"), "karela" to ItemSpec("करेला", "Bitter Gourd"),
            "बैंगन" to ItemSpec("बैंगन", "Eggplant"), "baingan" to ItemSpec("बैंगन", "Eggplant"), "eggplant" to ItemSpec("बैंगन", "Eggplant"),
            "प्याज" to ItemSpec("प्याज", "Onion"), "pyaz" to ItemSpec("प्याज", "Onion"), "onion" to ItemSpec("प्याज", "Onion"),
            "अदरक" to ItemSpec("अदरक", "Ginger"), "adrak" to ItemSpec("अदरक", "Ginger"), "ginger" to ItemSpec("अदरक", "Ginger"),
            "लहसुन" to ItemSpec("लहसुन", "Garlic"), "lahsun" to ItemSpec("लहसुन", "Garlic"), "garlic" to ItemSpec("लहसुन", "Garlic"),
            "मिर्च" to ItemSpec("मिर्च", "Chilli"), "mirchi" to ItemSpec("मिर्च", "Chilli"), "chilli" to ItemSpec("मिर्च", "Chilli"),
            "धनिया" to ItemSpec("धनिया", "Coriander"), "dhaniya" to ItemSpec("धनिया", "Coriander"),
            "गोभी" to ItemSpec("गोभी", "Cauliflower"), "gobhi" to ItemSpec("गोभी", "Cauliflower"),
            "मटर" to ItemSpec("मटर", "Peas"), "matar" to ItemSpec("मटर", "Peas"), "peas" to ItemSpec("मटर", "Peas"),
            "पालक" to ItemSpec("पालक", "Spinach"), "palak" to ItemSpec("पालक", "Spinach"), "spinach" to ItemSpec("पालक", "Spinach"),
            "गाजर" to ItemSpec("गाजर", "Carrot"), "gajar" to ItemSpec("गाजर", "Carrot"), "carrot" to ItemSpec("गाजर", "Carrot"),
            "खीरा" to ItemSpec("खीरा", "Cucumber"), "kheera" to ItemSpec("खीरा", "Cucumber"), "cucumber" to ItemSpec("खीरा", "Cucumber"),
            "दूध" to ItemSpec("दूध", "Milk"), "doodh" to ItemSpec("दूध", "Milk"), "milk" to ItemSpec("दूध", "Milk"),
            "दही" to ItemSpec("दही", "Curd"), "dahi" to ItemSpec("दही", "Curd"),
            "मक्खन" to ItemSpec("मक्खन", "Butter"), "butter" to ItemSpec("मक्खन", "Butter"),
            "पनीर" to ItemSpec("पनीर", "Paneer"), "paneer" to ItemSpec("पनीर", "Paneer"),
            "ब्रेड" to ItemSpec("ब्रेड", "Bread"), "bread" to ItemSpec("ब्रेड", "Bread"),
            "अंडे" to ItemSpec("अंडे", "Eggs"), "anda" to ItemSpec("अंडे", "Eggs"), "eggs" to ItemSpec("अंडे", "Eggs"),
            "दाल" to ItemSpec("दाल", "Lentils"), "daal" to ItemSpec("दाल", "Lentils"), "dal" to ItemSpec("दाल", "Lentils"),
            "चावल" to ItemSpec("चावल", "Rice"), "chawal" to ItemSpec("चावल", "Rice"), "rice" to ItemSpec("चावल", "Rice"),
            "चीनी" to ItemSpec("चीनी", "Sugar"), "cheeni" to ItemSpec("चीनी", "Sugar"), "sugar" to ItemSpec("चीनी", "Sugar"),
            "नमक" to ItemSpec("नमक", "Salt"), "namak" to ItemSpec("नमक", "Salt"), "salt" to ItemSpec("नमक", "Salt"),
            "तेल" to ItemSpec("तेल", "Cooking Oil"), "tel" to ItemSpec("तेल", "Cooking Oil"), "oil" to ItemSpec("तेल", "Cooking Oil"),
            "मसाला" to ItemSpec("मसाला", "Spices"), "masala" to ItemSpec("मसाला", "Spices"),
            "चाय" to ItemSpec("चाय", "Tea"), "chai" to ItemSpec("चाय", "Tea"), "tea" to ItemSpec("चाय", "Tea"),
            "कॉफ़ी" to ItemSpec("कॉफ़ी", "Coffee"), "coffee" to ItemSpec("कॉफ़ी", "Coffee"),
            "फल" to ItemSpec("फल", "Fruits"), "fruit" to ItemSpec("फल", "Fruits"), "fruits" to ItemSpec("फल", "Fruits"),
            "आम" to ItemSpec("आम", "Mango"), "aam" to ItemSpec("आम", "Mango"), "mango" to ItemSpec("आम", "Mango"),
            "सेब" to ItemSpec("सेब", "Apple"), "seb" to ItemSpec("सेब", "Apple"), "apple" to ItemSpec("सेब", "Apple"),
            "केला" to ItemSpec("केला", "Banana"), "kela" to ItemSpec("केला", "Banana"), "banana" to ItemSpec("केला", "Banana"),
            "अंगूर" to ItemSpec("अंगूर", "Grapes"), "angoor" to ItemSpec("अंगूर", "Grapes"), "grapes" to ItemSpec("अंगूर", "Grapes"),
            "संतरा" to ItemSpec("संतरा", "Orange"), "santra" to ItemSpec("संतरा", "Orange"), "orange" to ItemSpec("संतरा", "Orange"),
            "पपीता" to ItemSpec("पपीता", "Papaya"), "papita" to ItemSpec("पपीता", "Papaya"), "papaya" to ItemSpec("पपीता", "Papaya"),
            "अमरूद" to ItemSpec("अमरूद", "Guava"), "amrood" to ItemSpec("अमरूद", "Guava"), "guava" to ItemSpec("अमरूद", "Guava"),
            "तरबूज" to ItemSpec("तरबूज", "Watermelon"), "tarbooz" to ItemSpec("तरबूज", "Watermelon"), "watermelon" to ItemSpec("तरबूज", "Watermelon"),
            "अनार" to ItemSpec("अनार", "Pomegranate"), "anaar" to ItemSpec("अनार", "Pomegranate"), "pomegranate" to ItemSpec("अनार", "Pomegranate")
        )

        private val ATTRIBUTES_MAP = mapOf(
            "हरा" to ItemSpec("हरा", "Green"), "हरे" to ItemSpec("हरे", "Green"), "हरी" to ItemSpec("हरी", "Green"),
            "hare" to ItemSpec("हरे", "Green"), "hara" to ItemSpec("हरा", "Green"), "green" to ItemSpec("हरा", "Green"),
            "लाल" to ItemSpec("लाल", "Red"), "laal" to ItemSpec("लाल", "Red"), "lal" to ItemSpec("लाल", "Red"), "red" to ItemSpec("लाल", "Red"),
            "पीला" to ItemSpec("पीला", "Yellow"), "पीले" to ItemSpec("पीले", "Yellow"), "peela" to ItemSpec("पीला", "Yellow"), "yellow" to ItemSpec("पीला", "Yellow"),
            "सफेद" to ItemSpec("सफेद", "White"), "safed" to ItemSpec("सफेद", "White"), "white" to ItemSpec("सफेद", "White"),
            "काला" to ItemSpec("काला", "Black"), "काले" to ItemSpec("काले", "Black"), "kala" to ItemSpec("काला", "Black"), "black" to ItemSpec("काला", "Black"),
            "कच्चा" to ItemSpec("कच्चा", "Raw"), "कच्चे" to ItemSpec("कच्चे", "Raw"), "kaccha" to ItemSpec("कच्चा", "Raw"), "raw" to ItemSpec("कच्चा", "Raw"),
            "पक्का" to ItemSpec("पक्का", "Ripe"), "पक्के" to ItemSpec("पक्के", "Ripe"), "pakka" to ItemSpec("पक्का", "Ripe"), "ripe" to ItemSpec("पक्का", "Ripe"),
            "ताजा" to ItemSpec("ताजा", "Fresh"), "ताजे" to ItemSpec("ताजे", "Fresh"), "taza" to ItemSpec("ताजा", "Fresh"), "fresh" to ItemSpec("ताजा", "Fresh"),
            "छोटा" to ItemSpec("छोटा", "Small"), "छोटे" to ItemSpec("छोटे", "Small"), "chota" to ItemSpec("छोटा", "Small"), "small" to ItemSpec("छोटा", "Small"),
            "बड़ा" to ItemSpec("बड़ा", "Large"), "बड़े" to ItemSpec("बड़े", "Large"), "bada" to ItemSpec("बड़ा", "Large"), "large" to ItemSpec("बड़ा", "Large")
        )

        fun resolveItemName(spec: ItemSpec, aiLanguage: String, isDevanagari: Boolean): String {
            return when (aiLanguage) {
                "hi" -> spec.hi
                "en" -> spec.en
                else -> if (isDevanagari) spec.hi else spec.en
            }
        }

        // Filler and speech artifact words to exclude when distilling item names
        private val FILLER_WORDS = setOf(
            "सब्जी", "sabzi", "sabji", "kaun", "kaun se", "kya", "kya kya", "lana", "hai", "lena",
            "gandhi", "gaandhi", "गांधी", "he", "aur", "and", "bhi", "tha", "thi", "hoga", "hogi",
            "ek", "do", "kilo", "gram", "packet", "list", "banao", "chahiye", "mangwana"
        )
    }

    data class TaskWithSubtasks(
        val task: NoteTask,
        val subtasks: List<NoteTask> = emptyList(),
        val subtaskChildren: Map<Int, List<NoteTask>> = emptyMap()
    )

    data class ExtractedTask(
        val title: String,
        val description: String,
        val group: String,
        val priority: Int,
        val isLowHangingFruit: Boolean,
        val dueDateMillis: Long? = null,
        val dueTimeText: String = "",
        val autoCompleteOnSubtasksDone: Boolean = true,
        val subtasks: List<ExtractedTask> = emptyList()
    )

    /**
     * Extracts date/time horizon from spoken natural language in Hindi, Hinglish, or English.
     */
    fun extractTemporalInfo(text: String): Pair<Long?, String> {
        val lower = text.lowercase(Locale.ROOT)
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()

        // Today / tonight / aaj / shaam ko
        if (lower.contains("aaj") || lower.contains("today") || lower.contains("tonight") || lower.contains("this evening") || lower.contains("aaj shaam")) {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 20)
            calendar.set(java.util.Calendar.MINUTE, 0)
            val label = if (lower.contains("shaam") || lower.contains("evening") || lower.contains("tonight")) "This Evening" else "Today"
            return Pair(calendar.timeInMillis, label)
        }

        // Tomorrow / kal
        if (lower.contains("kal") || lower.contains("tomorrow") || lower.contains("kal subah") || lower.contains("kal shaam")) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, if (lower.contains("subah") || lower.contains("morning")) 10 else 18)
            calendar.set(java.util.Calendar.MINUTE, 0)
            val label = if (lower.contains("subah") || lower.contains("morning")) "Tomorrow Morning" else if (lower.contains("shaam") || lower.contains("evening")) "Tomorrow Evening" else "Tomorrow"
            return Pair(calendar.timeInMillis, label)
        }

        // Day after tomorrow / parso
        if (lower.contains("parso") || lower.contains("day after tomorrow")) {
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 2)
            calendar.set(java.util.Calendar.HOUR_OF_DAY, 12)
            return Pair(calendar.timeInMillis, "In 2 Days")
        }

        // This weekend / weekend pe
        if (lower.contains("weekend") || lower.contains("hafte ke end")) {
            calendar.set(java.util.Calendar.DAY_OF_WEEK, java.util.Calendar.SATURDAY)
            return Pair(calendar.timeInMillis, "This Weekend")
        }

        // Next week / agle hafte / agla hafta
        if (lower.contains("next week") || lower.contains("agle hafte") || lower.contains("agla hafta")) {
            calendar.add(java.util.Calendar.WEEK_OF_YEAR, 1)
            return Pair(calendar.timeInMillis, "Next Week")
        }

        return Pair(null, "")
    }

    /**
     * Parses raw transcribed text in Hindi, Hinglish, or English into hierarchical tasks (parent + nested subtasks).
     */
    fun processTranscriptionHierarchical(text: String, transcriptionId: Long, aiLanguage: String = "auto"): List<TaskWithSubtasks> {
        val stripped = com.echo.recorder.util.AppStorageManager.stripMetadataFromTranscript(text)
        val cleanedText = stripped.trim()
        if (cleanedText.isBlank() || 
            cleanedText.startsWith("[") ||
            cleanedText.startsWith("Voice speech recorded in") ||
            cleanedText.startsWith("Captured Hindi/Hinglish speech from") ||
            cleanedText.startsWith("Audio slice ready") ||
            cleanedText.startsWith("Audio chunk recorded") ||
            cleanedText.startsWith("- **Audio Source:") ||
            cleanedText.startsWith("- **") ||
            cleanedText.startsWith("#")
        ) {
            return emptyList()
        }

        val results = mutableListOf<TaskWithSubtasks>()

        // 1. Check for Shopping / Grocery / Sabzi item enumerations
        val shoppingHierarchical = extractShoppingListTasks(cleanedText, transcriptionId, aiLanguage)
        if (shoppingHierarchical.isNotEmpty()) {
            results.addAll(shoppingHierarchical)
        }

        // 2. Split compound sentences by clause conjunctions ("aur", "and", "then", "fir", "pehle", punctuation)
        val clauses = splitIntoActionClauses(cleanedText)

        for (clause in clauses) {
            val sentence = clause.trim()
            if (sentence.length < 3) continue

            val extracted = analyzeSentence(sentence)
            if (extracted != null) {
                val (dueDate, dueText) = extractTemporalInfo(sentence)
                val task = NoteTask(
                    transcriptionId = transcriptionId,
                    title = extracted.title,
                    description = extracted.description,
                    categoryGroup = extracted.group,
                    priority = extracted.priority,
                    isLowHangingFruit = extracted.isLowHangingFruit,
                    autoCompleteOnSubtasksDone = extracted.autoCompleteOnSubtasksDone,
                    dueDateMillis = dueDate,
                    dueTimeText = dueText,
                    timestampMillis = System.currentTimeMillis()
                )
                results.add(TaskWithSubtasks(task, emptyList()))
            }
        }

        // 3. Fallback: If no explicit task keywords or shopping items were detected, categorize smartly
        if (results.isEmpty() && cleanedText.length > 3) {
            val group = categorizeText(cleanedText)
            val isFruit = isQuickWin(cleanedText)
            val priority = determinePriority(cleanedText)
            val cleanTitle = rephraseAsCleanTitle(cleanedText, group)
            val (dueDate, dueText) = extractTemporalInfo(cleanedText)

            val fallbackTask = NoteTask(
                transcriptionId = transcriptionId,
                title = cleanTitle,
                description = cleanedText,
                categoryGroup = group,
                priority = priority,
                isLowHangingFruit = isFruit,
                dueDateMillis = dueDate,
                dueTimeText = dueText,
                timestampMillis = System.currentTimeMillis()
            )
            results.add(TaskWithSubtasks(fallbackTask, emptyList()))
        }

        return results
    }

    /**
     * Backward-compatible flat task list for legacy callers.
     */
    fun processTranscription(text: String, transcriptionId: Long, aiLanguage: String = "auto"): List<NoteTask> {
        val hierarchical = processTranscriptionHierarchical(text, transcriptionId, aiLanguage)
        val flat = mutableListOf<NoteTask>()
        for (item in hierarchical) {
            flat.add(item.task)
            flat.addAll(item.subtasks)
            item.subtaskChildren.values.forEach { grandChildren ->
                flat.addAll(grandChildren)
            }
        }
        return flat
    }

    /**
     * Helper to insert hierarchical tasks and link child subtasks to their generated parent IDs in SQLite.
     * Supports arbitrary multi-level nested tasks.
     */
    suspend fun saveHierarchicalTasksToDatabase(
        dao: NoteTaskDao,
        hierarchicalTasks: List<TaskWithSubtasks>
    ): List<NoteTask> {
        val insertedList = mutableListOf<NoteTask>()
        for (item in hierarchicalTasks) {
            val parentId = dao.insertTask(item.task)
            insertedList.add(item.task.copy(id = parentId))
            for ((index, sub) in item.subtasks.withIndex()) {
                val child = sub.copy(parentId = parentId, orderIndex = index)
                val childId = dao.insertTask(child)
                val savedChild = child.copy(id = childId)
                insertedList.add(savedChild)

                val grandChildren = item.subtaskChildren[index] ?: emptyList()
                for ((gIdx, grand) in grandChildren.withIndex()) {
                    val grandChild = grand.copy(parentId = childId, orderIndex = gIdx)
                    val grandId = dao.insertTask(grandChild)
                    insertedList.add(grandChild.copy(id = grandId))
                }
            }
        }
        return insertedList
    }

    /**
     * Detects grocery, vegetable, fruit and shopping item enumerations in Hindi or English,
     * builds a master parent task, child items, and nested variety/attribute sub-subtasks.
     */
    private fun extractShoppingListTasks(
        text: String,
        transcriptionId: Long,
        aiLanguage: String = "auto"
    ): List<TaskWithSubtasks> {
        val lower = text.lowercase(Locale.ROOT)
        val isDevanagari = text.any { it in '\u0900'..'\u097F' }
        val (dueDate, dueText) = extractTemporalInfo(text)
        val foundItems = mutableListOf<Pair<String, ItemSpec>>() // (matchedTerm, spec)

        for ((term, spec) in SHOPPING_ITEMS_MAP) {
            val regex = Regex("\\b${Regex.escape(term)}\\b|${Regex.escape(term)}")
            if (regex.containsMatchIn(lower) || lower.contains(term)) {
                if (foundItems.none { it.second == spec }) {
                    foundItems.add(term to spec)
                }
            }
        }

        if (foundItems.isEmpty()) return emptyList()

        fun extractAttributesForItem(): List<String> {
            val matchedAttrs = mutableListOf<String>()
            for ((attrWord, attrSpec) in ATTRIBUTES_MAP) {
                if (lower.contains(attrWord)) {
                    val resolvedAttr = resolveItemName(attrSpec, aiLanguage, isDevanagari)
                    if (!matchedAttrs.contains(resolvedAttr)) {
                        matchedAttrs.add(resolvedAttr)
                    }
                }
            }
            return matchedAttrs
        }

        val results = mutableListOf<TaskWithSubtasks>()
        val hasGenericFruit = foundItems.any { it.second.en.equals("Fruits", ignoreCase = true) }
        val hasGenericVeg = foundItems.any { lower.contains("सब्जी") || lower.contains("sabzi") || it.second.en.equals("Vegetables", ignoreCase = true) }
        val specificItems = foundItems.filter { !it.second.en.equals("Fruits", ignoreCase = true) && !it.second.en.equals("Vegetables", ignoreCase = true) }

        val itemsToProcess = if (specificItems.isNotEmpty()) specificItems else foundItems
        val useHindi = (aiLanguage == "hi") || (aiLanguage == "auto" && isDevanagari)

        val masterCategoryTitle = when {
            hasGenericFruit -> if (useHindi) "फल खरीदें" else "Buy Fruits"
            hasGenericVeg || lower.contains("सब्जी") || lower.contains("sabzi") -> if (useHindi) "सब्जियां खरीदें" else "Buy Vegetables"
            else -> if (useHindi) "सामान खरीदें" else "Buy Groceries"
        }

        if (itemsToProcess.size >= 2 || (itemsToProcess.size == 1 && (hasGenericFruit || hasGenericVeg))) {
            val itemNamesList = itemsToProcess.map { resolveItemName(it.second, aiLanguage, isDevanagari) }.joinToString(", ")
            val masterTitle = "$masterCategoryTitle: $itemNamesList"

            val masterTask = NoteTask(
                transcriptionId = transcriptionId,
                title = masterTitle,
                description = "Shopping list: $itemNamesList",
                categoryGroup = "Shopping",
                priority = NoteTask.PRIORITY_MEDIUM,
                isLowHangingFruit = true,
                autoCompleteOnSubtasksDone = true,
                dueDateMillis = dueDate,
                dueTimeText = dueText,
                timestampMillis = System.currentTimeMillis()
            )

            val subtasks = mutableListOf<NoteTask>()
            val subtaskChildrenMap = mutableMapOf<Int, List<NoteTask>>()

            itemsToProcess.forEachIndexed { index, (_, spec) ->
                val resolvedName = resolveItemName(spec, aiLanguage, isDevanagari)
                val attributes = extractAttributesForItem()
                val itemTitle = if (attributes.isNotEmpty()) {
                    "$resolvedName (${attributes.first()})"
                } else {
                    resolvedName
                }

                val itemTask = NoteTask(
                    transcriptionId = transcriptionId,
                    title = itemTitle,
                    description = "Item: $resolvedName",
                    categoryGroup = "Shopping",
                    priority = NoteTask.PRIORITY_MEDIUM,
                    isLowHangingFruit = true,
                    autoCompleteOnSubtasksDone = true,
                    dueDateMillis = dueDate,
                    dueTimeText = dueText,
                    orderIndex = index,
                    timestampMillis = System.currentTimeMillis()
                )
                subtasks.add(itemTask)

                if (attributes.isNotEmpty()) {
                    val grandChildren = attributes.mapIndexed { gIdx, attr ->
                        NoteTask(
                            transcriptionId = transcriptionId,
                            title = attr,
                            description = "Specified variety for $resolvedName: $attr",
                            categoryGroup = "Shopping",
                            priority = NoteTask.PRIORITY_MEDIUM,
                            isLowHangingFruit = true,
                            autoCompleteOnSubtasksDone = true,
                            dueDateMillis = dueDate,
                            dueTimeText = dueText,
                            orderIndex = gIdx,
                            timestampMillis = System.currentTimeMillis()
                        )
                    }
                    subtaskChildrenMap[index] = grandChildren
                }
            }

            results.add(TaskWithSubtasks(masterTask, subtasks, subtaskChildrenMap))
        } else if (itemsToProcess.size == 1) {
            val (_, spec) = itemsToProcess.first()
            val resolvedName = resolveItemName(spec, aiLanguage, isDevanagari)
            val attributes = extractAttributesForItem()
            val itemTitle = if (attributes.isNotEmpty()) {
                "$resolvedName (${attributes.first()})"
            } else {
                resolvedName
            }

            val singleTask = NoteTask(
                transcriptionId = transcriptionId,
                title = itemTitle,
                description = "Shopping item: $resolvedName",
                categoryGroup = "Shopping",
                priority = NoteTask.PRIORITY_MEDIUM,
                isLowHangingFruit = true,
                autoCompleteOnSubtasksDone = true,
                dueDateMillis = dueDate,
                dueTimeText = dueText,
                timestampMillis = System.currentTimeMillis()
            )

            val childList = if (attributes.isNotEmpty()) {
                attributes.mapIndexed { idx, attr ->
                    NoteTask(
                        transcriptionId = transcriptionId,
                        title = attr,
                        description = "Specified variety for $resolvedName: $attr",
                        categoryGroup = "Shopping",
                        priority = NoteTask.PRIORITY_MEDIUM,
                        isLowHangingFruit = true,
                        autoCompleteOnSubtasksDone = true,
                        dueDateMillis = dueDate,
                        dueTimeText = dueText,
                        orderIndex = idx,
                        timestampMillis = System.currentTimeMillis()
                    )
                }
            } else emptyList()

            results.add(TaskWithSubtasks(singleTask, childList, emptyMap()))
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
            lower.contains("medicine") || SHOPPING_ITEMS_MAP.keys.any { lower.contains(it) } -> "Shopping"

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
               lower.contains("सब्जी") || SHOPPING_ITEMS_MAP.keys.any { lower.contains(it) }
    }

    /**
     * Quick internet search tool using DuckDuckGo Instant Answers API.
     */
    suspend fun searchInternet(query: String): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val cleanQuery = query.replace("?", "").trim()
            val encoded = java.net.URLEncoder.encode(cleanQuery, "UTF-8")
            val url = java.net.URL("https://api.duckduckgo.com/?q=$encoded&format=json&no_html=1&skip_disambig=1")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                connectTimeout = 4000
                readTimeout = 5000
                setRequestProperty("User-Agent", "EchoRecorder/1.0")
            }
            if (conn.responseCode == 200) {
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = org.json.JSONObject(body)
                val abstract = json.optString("AbstractText", "")
                val heading = json.optString("Heading", "")
                if (abstract.isNotBlank()) {
                    return@withContext if (heading.isNotBlank()) "$heading: $abstract" else abstract
                }
                val related = json.optJSONArray("RelatedTopics")
                if (related != null && related.length() > 0) {
                    val first = related.optJSONObject(0)
                    val text = first?.optString("Text", "") ?: ""
                    if (text.isNotBlank()) return@withContext text
                }
            }
        } catch (_: Exception) {}
        null
    }

    fun answerTaskQuery(query: String, allTasks: List<NoteTask>): Pair<String, List<String>> {
        return answerTaskQuery(query, allTasks, emptyList(), "", null, "en")
    }

    /**
     * Answers queries across the user's task and shopping lists using RAG (Audio Transcripts + Active Tasks + Web Search).
     * Output is clean, concise, free of redundant emojis or duplicated bilingual terms.
     */
    fun answerTaskQuery(
        query: String,
        allTasks: List<NoteTask>,
        allTranscripts: List<com.echo.recorder.data.TranscriptionWithAudio> = emptyList(),
        systemPrompt: String = "",
        webSearchResult: String? = null,
        aiLanguage: String = "en"
    ): Pair<String, List<String>> {
        val lower = query.lowercase(Locale.ROOT)
        val pendingTasks = allTasks.filter { !it.isCompleted }
        val isHindi = aiLanguage == "hi"

        // 1. Market / Shopping / Groceries / Vegetables Query
        val isMarketQuery = lower.contains("market") || lower.contains("bazar") || lower.contains("bazaar") ||
                lower.contains("shopping") || lower.contains("vegetable") || lower.contains("sabzi") ||
                lower.contains("grocer") || lower.contains("kirana") || lower.contains("kya lena") ||
                lower.contains("buy") || lower.contains("get") || lower.contains("store")

        if (isMarketQuery) {
            val shoppingTasks = pendingTasks.filter { task ->
                task.categoryGroup.equals("Shopping", ignoreCase = true) ||
                task.title.startsWith("Buy", ignoreCase = true) ||
                task.title.startsWith("खरीदें", ignoreCase = true) ||
                task.description.contains("shopping", ignoreCase = true) ||
                SHOPPING_ITEMS_MAP.keys.any { task.title.contains(it, ignoreCase = true) }
            }

            val itemTitles = shoppingTasks.map { it.title.removePrefix("Buy: ").removePrefix("Buy ").removePrefix("खरीदें: ").trim() }
            val responseText = buildString {
                if (shoppingTasks.isNotEmpty()) {
                    append(if (isHindi) "बाजार के लिए आपकी सूची के सामान:\n" else "Items from your list for the market:\n")
                    shoppingTasks.forEach { task ->
                        val cleanTitle = task.title.removePrefix("Buy: ").removePrefix("Buy ").removePrefix("खरीदें: ").trim()
                        val dueInfo = if (task.dueTimeText.isNotBlank()) " (${task.dueTimeText})" else ""
                        append("- $cleanTitle$dueInfo\n")
                    }
                } else {
                    append(if (isHindi) "आपकी सूची में कोई लंबित खरीदारी या सब्जी का सामान नहीं है।" else "You do not have any pending shopping or grocery items in your lists right now.")
                }

                if (!webSearchResult.isNullOrBlank()) {
                    append(if (isHindi) "\n\nइंटरनेट खोज परिणाम:\n$webSearchResult" else "\n\nWeb Search:\n$webSearchResult")
                }
            }
            return Pair(responseText, itemTitles)
        }

        // 2. Urgent / High Priority Query
        val isUrgentQuery = lower.contains("urgent") || lower.contains("priority") || lower.contains("important") ||
                lower.contains("jaldi") || lower.contains("emergency") || lower.contains("turant")
        if (isUrgentQuery) {
            val urgentTasks = pendingTasks.filter {
                it.priority == NoteTask.PRIORITY_HIGH || it.categoryGroup.equals("Urgent", ignoreCase = true)
            }
            val items = urgentTasks.map { it.title }
            val responseText = buildString {
                if (urgentTasks.isNotEmpty()) {
                    append(if (isHindi) "उच्च प्राथमिकता वाले जरूरी कार्य:\n" else "High-priority tasks requiring attention:\n")
                    urgentTasks.forEach { task ->
                        append("- ${task.title} [${task.categoryGroup}]\n")
                    }
                } else {
                    append(if (isHindi) "कोई जरूरी या उच्च प्राथमिकता वाला कार्य लंबित नहीं है।" else "No urgent or high-priority tasks pending.")
                }

                if (!webSearchResult.isNullOrBlank()) {
                    append(if (isHindi) "\n\nइंटरनेट खोज परिणाम:\n$webSearchResult" else "\n\nWeb Search:\n$webSearchResult")
                }
            }
            return Pair(responseText, items)
        }

        // 3. Today / Upcoming Query
        val isTodayQuery = lower.contains("today") || lower.contains("aaj") || lower.contains("tonight") ||
                lower.contains("upcoming") || lower.contains("tomorrow") || lower.contains("kal")
        if (isTodayQuery) {
            val dueTodayTasks = pendingTasks.filter { it.dueTimeText.isNotBlank() }
            if (dueTodayTasks.isNotEmpty()) {
                val items = dueTodayTasks.map { it.title }
                val responseText = buildString {
                    append(if (isHindi) "आगामी समय सीमा वाले कार्य:\n" else "Tasks scheduled for upcoming horizons:\n")
                    dueTodayTasks.forEach { task ->
                        append("- ${task.title} (${task.dueTimeText})\n")
                    }
                    if (!webSearchResult.isNullOrBlank()) {
                        append(if (isHindi) "\n\nइंटरनेट खोज परिणाम:\n$webSearchResult" else "\n\nWeb Search:\n$webSearchResult")
                    }
                }
                return Pair(responseText, items)
            }
        }

        // 4. Keyword Match across all tasks & RAG across Audio Transcriptions
        val words = lower.split(Regex("\\s+")).filter { it.length > 2 && it !in listOf("what", "where", "which", "list", "tasks", "items", "show", "tell", "kya", "hai", "batao", "mere", "audio", "notes") }
        val matchingTasks = pendingTasks.filter { task ->
            val titleLower = task.title.lowercase(Locale.ROOT)
            val descLower = task.description.lowercase(Locale.ROOT)
            val catLower = task.categoryGroup.lowercase(Locale.ROOT)
            words.any { titleLower.contains(it) || descLower.contains(it) || catLower.contains(it) }
        }

        val matchingTranscripts = if (words.isNotEmpty()) {
            allTranscripts.filter { t ->
                val clean = com.echo.recorder.util.AppStorageManager.stripMetadataFromTranscript(t.text).lowercase(Locale.ROOT)
                words.any { clean.contains(it) }
            }.take(3)
        } else emptyList()

        if (matchingTasks.isNotEmpty() || matchingTranscripts.isNotEmpty()) {
            val items = matchingTasks.map { it.title }
            val responseText = buildString {
                if (matchingTasks.isNotEmpty()) {
                    append(if (isHindi) "आपके प्रश्न से मिलते-जुलते कार्य:\n" else "Tasks matching your query:\n")
                    matchingTasks.forEach { task ->
                        append("- ${task.title} (${task.categoryGroup})\n")
                    }
                }

                if (matchingTranscripts.isNotEmpty()) {
                    if (matchingTasks.isNotEmpty()) append("\n")
                    append(if (isHindi) "संदर्भित ऑडियो प्रतिलेख (RAG):\n" else "Referenced Audio Transcripts:\n")
                    matchingTranscripts.forEach { t ->
                        val clean = com.echo.recorder.util.AppStorageManager.stripMetadataFromTranscript(t.text)
                        append("- \"${clean.take(90)}${if (clean.length > 90) "..." else ""}\"\n")
                    }
                }

                if (!webSearchResult.isNullOrBlank()) {
                    append(if (isHindi) "\n\nइंटरनेट खोज परिणाम:\n$webSearchResult" else "\n\nWeb Search:\n$webSearchResult")
                }
            }
            return Pair(responseText, items)
        }

        // 5. Default General Summary + Web Search Integration
        val sample = pendingTasks.take(5).map { it.title }
        val responseText = buildString {
            if (pendingTasks.isNotEmpty()) {
                append(if (isHindi) "वर्तमान में आपकी सूची में ${pendingTasks.size} सक्रिय कार्य हैं:\n" else "You currently have ${pendingTasks.size} active tasks across your lists:\n")
                pendingTasks.take(5).forEach { task ->
                    append("- ${task.title} [${task.categoryGroup}]\n")
                }
                if (pendingTasks.size > 5) {
                    append(if (isHindi) "...और ${pendingTasks.size - 5} अन्य।\n" else "...and ${pendingTasks.size - 5} more.\n")
                }
            } else {
                append(if (isHindi) "कोई लंबित कार्य नहीं है।" else "You have no pending tasks.")
            }

            if (!webSearchResult.isNullOrBlank()) {
                append(if (isHindi) "\n\nइंटरनेट खोज परिणाम:\n$webSearchResult" else "\n\nWeb Search:\n$webSearchResult")
            }
        }
        return Pair(responseText, sample)
    }

    private fun String.capitalize(locale: Locale): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

