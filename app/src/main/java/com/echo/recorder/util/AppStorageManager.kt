package com.echo.recorder.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.echo.recorder.data.NoteTask
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppStorageManager {

    const val DEFAULT_RELATIVE_PATH = "Download/App data/poc/yesboss"
    private const val FOLDER_AUDIO = "audio"
    private const val FOLDER_TRANSCRIPTIONS = "transcriptions"
    private const val FOLDER_NOTES = "notes"
    private const val FOLDER_LOGS = "logs"
    private const val FOLDER_MODELS = "models"

    fun getBaseDirectory(context: Context, customBasePath: String? = null): File {
        if (!customBasePath.isNullOrBlank()) {
            val custom = File(customBasePath)
            if (custom.isAbsolute) {
                if (!custom.exists()) custom.mkdirs()
                if (custom.canWrite() || custom.exists()) return custom
            }
        }

        // Try primary public Downloads folder: "Download/App data/poc/yesboss"
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = File(downloadsDir, "App data/poc/yesboss")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            if (baseDir.exists() && (baseDir.canWrite() || baseDir.isDirectory)) {
                baseDir
            } else {
                getAppSpecificBaseDir(context)
            }
        } catch (e: Exception) {
            getAppSpecificBaseDir(context)
        }
    }

    private fun getAppSpecificBaseDir(context: Context): File {
        val ext = context.getExternalFilesDir(null)
        val dir = if (ext != null) {
            File(ext, "poc/yesboss")
        } else {
            File(context.filesDir, "poc/yesboss")
        }
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAudioDirectory(context: Context, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_AUDIO).apply { if (!exists()) mkdirs() }
    }

    fun getTranscriptionsDirectory(context: Context, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_TRANSCRIPTIONS).apply { if (!exists()) mkdirs() }
    }

    fun getNotesDirectory(context: Context, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_NOTES).apply { if (!exists()) mkdirs() }
    }

    fun getLogsDirectory(context: Context, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_LOGS).apply { if (!exists()) mkdirs() }
    }

    fun getModelsDirectory(context: Context): File {
        return File(context.filesDir, FOLDER_MODELS).apply { if (!exists()) mkdirs() }
    }

    /**
     * Saves transcription text to a persistent, versioned Markdown (.md) file.
     * Retains history of all transcriptions even when re-transcribed.
     */
    fun saveTranscriptionToFile(
        context: Context,
        audioFileName: String,
        text: String,
        durationSeconds: Int,
        overlapSeconds: Int,
        engineName: String,
        tasks: List<NoteTask> = emptyList(),
        customTranscriptionDir: String? = null
    ): File {
        val dir = getTranscriptionsDirectory(context, customTranscriptionDir)
        val baseName = audioFileName.substringBeforeLast(".")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val versionedFile = File(dir, "transcription_${baseName}_v${timestamp}.md")
        val latestFile = File(dir, "transcription_${baseName}.md")

        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val content = buildString {
            appendLine("# 🎙️ Audio Transcription: $baseName")
            appendLine()
            appendLine("- **Audio Source:** `$audioFileName`")
            appendLine("- **Timestamp:** $dateStr")
            appendLine("- **Duration:** ${durationSeconds}s (Overlap: ${overlapSeconds}s)")
            appendLine("- **STT Engine:** $engineName")
            appendLine("- **Saved Path:** `${versionedFile.absolutePath}`")
            appendLine()
            appendLine("---")
            appendLine()
            appendLine("## 📝 Verbatim Transcript")
            appendLine()
            appendLine(text.ifBlank { "*(No speech detected in this audio segment)*" })
            appendLine()

            if (tasks.isNotEmpty()) {
                appendLine("---")
                appendLine()
                appendLine("## 📋 Extracted Tasks & Action Items (${tasks.size})")
                appendLine()
                for (task in tasks) {
                    val priorityBadge = when (task.priority) {
                        NoteTask.PRIORITY_HIGH -> "🔴 HIGH"
                        NoteTask.PRIORITY_MEDIUM -> "🟡 MED"
                        else -> "🟢 LOW"
                    }
                    val fruitBadge = if (task.isLowHangingFruit) " • ⚡ QUICK WIN" else ""
                    val check = if (task.isCompleted) "[x]" else "[ ]"
                    appendLine("- $check **${task.title}** `[$priorityBadge • ${task.categoryGroup}$fruitBadge]`")
                    if (task.description.isNotBlank()) {
                        appendLine("  > ${task.description}")
                    }
                }
                appendLine()
            }
        }

        try {
            versionedFile.writeText(content)
            latestFile.writeText(content)
            AppLogger.i("STORAGE", "Saved transcription file: ${versionedFile.name} (${versionedFile.length()} bytes)")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to write transcription file", e)
        }

        return latestFile
    }

    /**
     * Saves extracted notes & action items to a dedicated notes file (.md and .html)
     */
    fun saveNotesToFile(
        context: Context,
        audioFileName: String,
        transcriptionText: String,
        tasks: List<NoteTask>,
        customNotesDir: String? = null
    ): Pair<File, File?> {
        val dir = getNotesDirectory(context, customNotesDir)
        val baseName = audioFileName.substringBeforeLast(".")
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val mdFile = File(dir, "notes_${baseName}.md")
        val htmlFile = File(dir, "notes_${baseName}.html")

        val mdContent = buildString {
            appendLine("# 🧠 AI Notes & Tasks: $baseName")
            appendLine("- **Generated:** $dateStr")
            appendLine("- **Source File:** `$audioFileName`")
            appendLine()
            appendLine("## Summary & Context")
            appendLine(transcriptionText)
            appendLine()
            appendLine("## Action Items")
            for (task in tasks) {
                appendLine("- [ ] **${task.title}** (${task.categoryGroup}, Priority: ${task.priority})")
            }
        }

        val htmlContent = buildString {
            appendLine("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Notes - $baseName</title>")
            appendLine("<style>body{font-family:sans-serif;background:#0d1117;color:#c9d1d9;padding:24px;max-width:800px;margin:auto;}")
            appendLine("h1{color:#58a6ff;}h2{color:#79c0ff;border-bottom:1px solid #30363d;padding-bottom:6px;}")
            appendLine(".task{background:#161b22;padding:12px;margin:8px 0;border-radius:8px;border:1px solid #30363d;}")
            appendLine(".badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:bold;margin-right:8px;background:#1f6feb;color:#fff;}")
            appendLine("</style></head><body>")
            appendLine("<h1>🧠 AI Notes & Tasks</h1><p><strong>Source:</strong> $audioFileName | <strong>Date:</strong> $dateStr</p>")
            appendLine("<h2>Transcription</h2><p>$transcriptionText</p>")
            appendLine("<h2>Action Items (${tasks.size})</h2>")
            for (t in tasks) {
                appendLine("<div class='task'><span class='badge'>${t.categoryGroup}</span><strong>${t.title}</strong><p>${t.description}</p></div>")
            }
            appendLine("</body></html>")
        }

        try {
            mdFile.writeText(mdContent)
            htmlFile.writeText(htmlContent)
            AppLogger.i("STORAGE", "Saved AI notes: ${mdFile.name} and ${htmlFile.name}")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to write notes files", e)
        }

        return Pair(mdFile, htmlFile)
    }

    fun getLatestTranscriptionFile(context: Context, audioFileName: String, customPath: String? = null): File? {
        val dir = getTranscriptionsDirectory(context, customPath)
        val baseName = audioFileName.substringBeforeLast(".")
        val latest = File(dir, "transcription_${baseName}.md")
        if (latest.exists()) return latest
        val files = dir.listFiles { f -> f.name.startsWith("transcription_${baseName}") }
        return files?.maxByOrNull { it.lastModified() }
    }

    fun openFileWithIntent(context: Context, file: File): Boolean {
        return try {
            if (!file.exists()) return false
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when (file.extension.lowercase(Locale.ROOT)) {
                "md", "txt", "log" -> "text/plain"
                "html" -> "text/html"
                "wav", "mp3" -> "audio/*"
                else -> "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to open file via Intent: ${file.name}", e)
            false
        }
    }
}
