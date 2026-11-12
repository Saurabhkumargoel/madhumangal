package com.madhumangal.storage

import com.madhumangal.data.NoteTask
import com.madhumangal.util.AppStorageManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppStorageManagerTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var customBaseDir: File

    @Before
    fun setUp() {
        customBaseDir = tempFolder.newFolder("test_poc_yesboss")
    }

    @Test
    fun testModelsDirectoryIsUnderBaseDirectory() {
        val modelsDir = AppStorageManager.getModelsDirectory(customBasePath = customBaseDir.absolutePath)
        assertNotNull(modelsDir)
        assertTrue(modelsDir.exists())
        assertEquals(File(customBaseDir, "models").absolutePath, modelsDir.absolutePath)
    }

    @Test
    fun testTranscriptionHistoryIsPreservedAcrossRetranscribes() {
        val audioName = "chunk_20260928_120000_1.wav"
        val transDir = AppStorageManager.getTranscriptionsDirectory(customPath = File(customBaseDir, "transcriptions").absolutePath)

        // First transcription
        AppStorageManager.saveTranscriptionToFile(
            audioFileName = audioName,
            text = "First transcription result",
            durationSeconds = 10,
            overlapSeconds = 2,
            engineName = "whisper_tiny",
            customTranscriptionDir = transDir.absolutePath
        )

        // Wait small interval so timestamp changes
        Thread.sleep(1100)

        // Second transcription (re-transcription with different model/result)
        AppStorageManager.saveTranscriptionToFile(
            audioFileName = audioName,
            text = "Second transcription re-run result",
            durationSeconds = 10,
            overlapSeconds = 2,
            engineName = "whisper_base",
            customTranscriptionDir = transDir.absolutePath
        )

        // Verify that multiple versioned files exist and previous files were NOT deleted or overwritten
        val versionedFiles = transDir.listFiles { f -> f.name.startsWith("transcription_chunk_20260928_120000_1_v") && f.name.endsWith(".md") }
        assertNotNull(versionedFiles)
        assertTrue("Expected at least 2 versioned files, but found ${versionedFiles?.size}", (versionedFiles?.size ?: 0) >= 2)

        // Verify latest snapshot file exists and contains latest text
        val latestFile = File(transDir, "transcription_chunk_20260928_120000_1.md")
        assertTrue(latestFile.exists())
        assertTrue(latestFile.readText().contains("Second transcription re-run result"))
    }

    @Test
    fun testNotesHistoryIsPreservedAcrossRetranscribes() {
        val audioName = "chunk_20260928_120000_2.wav"
        val notesDir = AppStorageManager.getNotesDirectory(customPath = File(customBaseDir, "notes").absolutePath)
        val tasks = listOf(
            NoteTask(
                transcriptionId = 1L,
                title = "Test Task 1",
                description = "Desc",
                categoryGroup = "Tasks",
                priority = 1,
                isLowHangingFruit = false
            )
        )

        // First notes save
        AppStorageManager.saveNotesToFile(
            audioFileName = audioName,
            transcriptionText = "Initial transcript",
            tasks = tasks,
            customNotesDir = notesDir.absolutePath
        )

        Thread.sleep(1100)

        // Second notes save
        AppStorageManager.saveNotesToFile(
            audioFileName = audioName,
            transcriptionText = "Updated transcript",
            tasks = tasks,
            customNotesDir = notesDir.absolutePath
        )

        val versionedMdFiles = notesDir.listFiles { f -> f.name.startsWith("notes_chunk_20260928_120000_2_v") && f.name.endsWith(".md") }
        val versionedHtmlFiles = notesDir.listFiles { f -> f.name.startsWith("notes_chunk_20260928_120000_2_v") && f.name.endsWith(".html") }

        assertTrue("Expected at least 2 versioned .md notes", (versionedMdFiles?.size ?: 0) >= 2)
        assertTrue("Expected at least 2 versioned .html notes", (versionedHtmlFiles?.size ?: 0) >= 2)
    }

    @Test
    fun testSrtSubtitleContentGeneration() {
        val srt = AppStorageManager.generateSrtContent("Hello world. How are you today? All good.", 12)
        assertTrue(srt.contains("00:00:00,000 -->"))
        assertTrue(srt.contains("Hello world."))
        assertFalse(srt.contains("Kaam jaldi"))
    }
}
