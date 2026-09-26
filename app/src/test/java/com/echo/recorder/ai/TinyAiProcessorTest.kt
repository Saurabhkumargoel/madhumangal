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

        assertEquals(1, tasks.size)
        val task = tasks.first()
        assertEquals("Personal", task.categoryGroup)
        assertTrue(task.isLowHangingFruit)
        assertEquals(NoteTask.PRIORITY_MEDIUM, task.priority)
    }

    @Test
    fun testHindiShoppingTaskExtraction() {
        val transcript = "Doodh aur bread khareedna hai aaj sham tak"
        val tasks = processor.processTranscription(transcript, 102L)

        assertEquals(1, tasks.size)
        val task = tasks.first()
        assertEquals("Shopping", task.categoryGroup)
        assertTrue(task.isLowHangingFruit)
    }

    @Test
    fun testUrgentWorkTaskExtraction() {
        val transcript = "Urgent project report submit karni hai"
        val tasks = processor.processTranscription(transcript, 103L)

        assertEquals(1, tasks.size)
        val task = tasks.first()
        assertEquals("Work", task.categoryGroup)
        assertEquals(NoteTask.PRIORITY_HIGH, task.priority)
    }
}
