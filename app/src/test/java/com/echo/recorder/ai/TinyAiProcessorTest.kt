package com.echo.recorder.ai

import com.echo.recorder.data.NoteTask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TinyAiProcessorTest {

    private lateinit var processor: TinyAiProcessor

    @Before
    fun setUp() {
        processor = TinyAiProcessor()
    }

    @Test
    fun testHinglishCallTaskExtraction() {
        val transcript = "Ramesh ko call karna hai kal subah 10 baje"
        val tasks = processor.processTranscription(transcript, 101L)

        assertTrue(tasks.isNotEmpty())
        val task = tasks.first()
        assertEquals("Personal", task.categoryGroup)
        assertTrue(task.isLowHangingFruit)
        assertEquals(NoteTask.PRIORITY_MEDIUM, task.priority)
    }

    @Test
    fun testHindiShoppingTaskExtraction() {
        val transcript = "Doodh aur bread khareedna hai aaj sham tak"
        val tasks = processor.processTranscription(transcript, 102L)

        assertTrue(tasks.isNotEmpty())
        assertTrue(tasks.any { it.categoryGroup == "Shopping" })
        assertTrue(tasks.any { it.title.contains("Doodh", ignoreCase = true) || it.title.contains("Milk", ignoreCase = true) })
    }

    @Test
    fun testRealWorldSabziListExtraction() {
        // Exact user log from field test
        val transcript = "सब्जी कौन कौन से गांधी है आलू टमाटर भिंडी करेला बैंगन"
        val tasks = processor.processTranscription(transcript, 104L)

        assertTrue("Should extract tasks from sabzi list", tasks.isNotEmpty())
        // All tasks must be categorized into Shopping (NOT Ideas!)
        assertTrue("All items must be in Shopping", tasks.all { it.categoryGroup == "Shopping" })
        // Must contain master Buy Vegetables task
        assertTrue("Must contain master vegetables task", tasks.any { it.title.startsWith("Buy Vegetables:") })
        // Must split individual items
        assertTrue("Must have task for aalu", tasks.any { it.title.contains("आलू") })
        assertTrue("Must have task for tamatar", tasks.any { it.title.contains("टमाटर") })
        assertTrue("Must have task for bhindi", tasks.any { it.title.contains("भिंडी") })
        assertTrue("Must have task for karela", tasks.any { it.title.contains("करेला") })
        assertTrue("Must have task for baingan", tasks.any { it.title.contains("बैंगन") })
    }

    @Test
    fun testUrgentWorkTaskExtraction() {
        val transcript = "Urgent project report submit karni hai"
        val tasks = processor.processTranscription(transcript, 103L)

        assertTrue(tasks.isNotEmpty())
        val task = tasks.first()
        assertEquals("Work", task.categoryGroup)
        assertEquals(NoteTask.PRIORITY_HIGH, task.priority)
    }
}
