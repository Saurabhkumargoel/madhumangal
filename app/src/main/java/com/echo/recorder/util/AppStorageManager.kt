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

    fun getBaseDirectory(context: Context? = null, customBasePath: String? = null): File {
        if (!customBasePath.isNullOrBlank()) {
            val custom = File(customBasePath)
            if (custom.isAbsolute) {
                if (!custom.exists()) custom.mkdirs()
                if (custom.canWrite() || custom.exists()) return custom
            }
        }

        val ctx = context
        // Try primary public Downloads folder: "Download/App data/poc/yesboss"
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val baseDir = File(downloadsDir, "App data/poc/yesboss")
            if (!baseDir.exists()) {
                baseDir.mkdirs()
            }
            if (baseDir.exists() && (baseDir.canWrite() || baseDir.isDirectory)) {
                baseDir
            } else if (ctx != null) {
                getAppSpecificBaseDir(ctx)
            } else {
                baseDir
            }
        } catch (e: Exception) {
            if (ctx != null) getAppSpecificBaseDir(ctx) else File("Download/App data/poc/yesboss").apply { mkdirs() }
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

    fun getAudioDirectory(context: Context? = null, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_AUDIO).apply { if (!exists()) mkdirs() }
    }

    fun getTranscriptionsDirectory(context: Context? = null, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_TRANSCRIPTIONS).apply { if (!exists()) mkdirs() }
    }

    fun getNotesDirectory(context: Context? = null, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_NOTES).apply { if (!exists()) mkdirs() }
    }

    fun getLogsDirectory(context: Context? = null, customPath: String? = null): File {
        if (!customPath.isNullOrBlank()) {
            val f = File(customPath)
            if (!f.exists()) f.mkdirs()
            if (f.exists()) return f
        }
        return File(getBaseDirectory(context), FOLDER_LOGS).apply { if (!exists()) mkdirs() }
    }

    fun getModelsDirectory(context: Context? = null, customBasePath: String? = null): File {
        val baseDir = getBaseDirectory(context, customBasePath)
        val modelsDir = File(baseDir, FOLDER_MODELS).apply { if (!exists()) mkdirs() }

        // Auto-migrate any existing models from legacy internal app storage (context.filesDir/models) to public storage
        try {
            val ctx = context
            if (ctx != null) {
                val legacyDir = File(ctx.filesDir, FOLDER_MODELS)
                if (legacyDir.exists() && legacyDir.isDirectory) {
                    val legacyFiles = legacyDir.listFiles()
                    legacyFiles?.forEach { legacyFile ->
                        val dest = File(modelsDir, legacyFile.name)
                        if (!dest.exists() || dest.length() < legacyFile.length()) {
                            legacyFile.copyTo(dest, overwrite = true)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to migrate legacy models from filesDir", e)
        }

        return modelsDir
    }

    /**
     * Saves transcription text to a persistent, versioned Markdown (.md) file.
     * Retains history of all transcriptions even when re-transcribed.
     * Always writes a new versioned file (vYYYYMMDD_HHmmss.md) and updates the latest file.
     */
    fun saveTranscriptionToFile(
        context: Context? = null,
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
            appendLine("- **Timestamp:** $dateStr (Version: $timestamp)")
            appendLine("- **Duration:** ${durationSeconds}s (Overlap: ${overlapSeconds}s)")
            appendLine("- **STT Engine:** $engineName")
            appendLine("- **Versioned File:** `${versionedFile.name}`")
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

        val versionedSrtFile = File(dir, "${baseName}_v${timestamp}.srt")
        val latestSrtFile = File(dir, "${baseName}.srt")
        val srtContent = generateSrtContent(text, durationSeconds)

        try {
            versionedFile.writeText(content)
            latestFile.writeText(content)
            if (srtContent.isNotBlank()) {
                versionedSrtFile.writeText(srtContent)
                latestSrtFile.writeText(srtContent)
            }
            AppLogger.i("STORAGE", "Saved transcription: ${versionedFile.name} (Latest updated: ${latestFile.name})")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to write transcription file", e)
        }

        return latestFile
    }

    /**
     * Generates standard SubRip (.srt) subtitle format with HH:mm:ss,SSS timestamps
     */
    fun generateSrtContent(text: String, durationSeconds: Int): String {
        val clean = text.trim()
        if (clean.isBlank() || clean.startsWith("[") || clean.startsWith("Voice speech recorded") || clean.startsWith("Audio slice")) {
            return ""
        }

        val parts = clean.split(Regex("(?<=[.!?।])\\s+|\n+")).filter { it.isNotBlank() }
        if (parts.isEmpty()) return ""

        val totalMs = (durationSeconds.coerceAtLeast(1) * 1000L)
        val sliceMs = totalMs / parts.size

        val sb = StringBuilder()
        for (i in parts.indices) {
            val startMs = i * sliceMs
            val endMs = ((i + 1) * sliceMs).coerceAtMost(totalMs)
            sb.appendLine("${i + 1}")
            sb.appendLine("${formatSrtTimestamp(startMs)} --> ${formatSrtTimestamp(endMs)}")
            sb.appendLine(parts[i].trim())
            sb.appendLine()
        }
        return sb.toString().trimEnd() + "\n"
    }

    fun formatSrtTimestamp(ms: Long): String {
        val hours = ms / 3600000
        val minutes = (ms % 3600000) / 60000
        val seconds = (ms % 60000) / 1000
        val millis = ms % 1000
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", hours, minutes, seconds, millis)
    }

    /**
     * Appends a live real-time speech subtitle entry to the session .srt file
     */
    fun appendLiveSessionSrt(
        context: Context,
        sessionTimestamp: String,
        entryIndex: Int,
        startMs: Long,
        endMs: Long,
        text: String,
        customDir: String? = null
    ): File {
        val dir = getTranscriptionsDirectory(context, customDir)
        val srtFile = File(dir, "subtitles_session_${sessionTimestamp}.srt")
        val entry = buildString {
            appendLine("$entryIndex")
            appendLine("${formatSrtTimestamp(startMs)} --> ${formatSrtTimestamp(endMs)}")
            appendLine(text.trim())
            appendLine()
        }
        try {
            srtFile.appendText(entry)
            AppLogger.d("STORAGE", "Appended live subtitle #$entryIndex to ${srtFile.name}")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed appending live subtitle", e)
        }
        return srtFile
    }

    /**
     * Saves a chunk-specific .srt subtitle file alongside the .wav audio file.
     * Always writes a new versioned file (vYYYYMMDD_HHmmss.srt) and updates latest.
     */
    fun saveChunkSrt(
        context: Context? = null,
        audioFileName: String,
        text: String,
        durationSeconds: Int,
        customDir: String? = null
    ): File? {
        val baseName = audioFileName.substringBeforeLast(".")
        val srtContent = generateSrtContent(text, durationSeconds)
        if (srtContent.isBlank()) return null

        val dir = getTranscriptionsDirectory(context, customDir)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val versionedSrt = File(dir, "${baseName}_v${timestamp}.srt")
        val latestSrt = File(dir, "${baseName}.srt")
        try {
            versionedSrt.writeText(srtContent)
            latestSrt.writeText(srtContent)
            AppLogger.i("STORAGE", "Saved chunk subtitle files: ${versionedSrt.name} & ${latestSrt.name}")
            return latestSrt
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to save chunk subtitle file", e)
            return null
        }
    }

    /**
     * Saves extracted notes & action items to dedicated notes files (.md and .html).
     * Always writes new versioned files (vYYYYMMDD_HHmmss) and updates latest files.
     */
    fun saveNotesToFile(
        context: Context? = null,
        audioFileName: String,
        transcriptionText: String,
        tasks: List<NoteTask>,
        customNotesDir: String? = null
    ): Pair<File, File?> {
        val dir = getNotesDirectory(context, customNotesDir)
        val baseName = audioFileName.substringBeforeLast(".")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val versionedMd = File(dir, "notes_${baseName}_v${timestamp}.md")
        val latestMd = File(dir, "notes_${baseName}.md")
        val versionedHtml = File(dir, "notes_${baseName}_v${timestamp}.html")
        val latestHtml = File(dir, "notes_${baseName}.html")

        val mdContent = buildString {
            appendLine("# 🧠 AI Notes & Tasks: $baseName (v$timestamp)")
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
            appendLine("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Notes - $baseName (v$timestamp)</title>")
            appendLine("<style>body{font-family:sans-serif;background:#0d1117;color:#c9d1d9;padding:24px;max-width:800px;margin:auto;}")
            appendLine("h1{color:#58a6ff;}h2{color:#79c0ff;border-bottom:1px solid #30363d;padding-bottom:6px;}")
            appendLine(".task{background:#161b22;padding:12px;margin:8px 0;border-radius:8px;border:1px solid #30363d;}")
            appendLine(".badge{display:inline-block;padding:2px 8px;border-radius:4px;font-size:12px;font-weight:bold;margin-right:8px;background:#1f6feb;color:#fff;}")
            appendLine("</style></head><body>")
            appendLine("<h1>🧠 AI Notes & Tasks</h1><p><strong>Source:</strong> $audioFileName | <strong>Date:</strong> $dateStr | <strong>Version:</strong> $timestamp</p>")
            appendLine("<h2>Transcription</h2><p>$transcriptionText</p>")
            appendLine("<h2>Action Items (${tasks.size})</h2>")
            for (t in tasks) {
                appendLine("<div class='task'><span class='badge'>${t.categoryGroup}</span><strong>${t.title}</strong><p>${t.description}</p></div>")
            }
            appendLine("</body></html>")
        }

        try {
            versionedMd.writeText(mdContent)
            latestMd.writeText(mdContent)
            versionedHtml.writeText(htmlContent)
            latestHtml.writeText(htmlContent)
            AppLogger.i("STORAGE", "Saved AI notes: ${versionedMd.name} & ${versionedHtml.name} (Latest updated)")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to write notes files", e)
        }

        return Pair(latestMd, latestHtml)
    }

    fun getLatestTranscriptionFile(context: Context, audioFileName: String, customPath: String? = null): File? {
        val dir = getTranscriptionsDirectory(context, customPath)
        val baseName = audioFileName.substringBeforeLast(".")
        val latest = File(dir, "transcription_${baseName}.md")
        if (latest.exists()) return latest
        val files = dir.listFiles { f -> f.name.startsWith("transcription_${baseName}") }
        return files?.maxByOrNull { it.lastModified() }
    }

    fun getLatestSubtitleFile(context: Context, audioFileName: String, customPath: String? = null): File? {
        val dir = getTranscriptionsDirectory(context, customPath)
        val baseName = audioFileName.substringBeforeLast(".")
        val srt = File(dir, "${baseName}.srt")
        if (srt.exists()) return srt
        val transSrt = File(dir, "transcription_${baseName}.srt")
        if (transSrt.exists()) return transSrt
        val files = dir.listFiles { f -> f.name.startsWith(baseName) && f.name.endsWith(".srt") }
        return files?.maxByOrNull { it.lastModified() }
    }

    fun openFolderWithIntent(context: Context, folder: File): Boolean {
        try {
            if (!folder.exists()) folder.mkdirs()

            // 1. Copy absolute path to clipboard so user can always paste in file explorer
            copyPathToClipboard(context, folder.absolutePath)

            // 2. Try launching system DocumentsUI / SAF viewer for external storage
            val relativePath = if (folder.absolutePath.contains("/storage/emulated/0/")) {
                folder.absolutePath.substringAfter("/storage/emulated/0/")
            } else {
                folder.name
            }
            val encodedPath = relativePath.replace("/", "%2F")
            val docUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3A$encodedPath")

            val intentDocuments = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(docUri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStartActivity(context, intentDocuments)) {
                android.widget.Toast.makeText(context, "📁 Opening folder: ${folder.name}", android.widget.Toast.LENGTH_SHORT).show()
                return true
            }

            // 3. Try standard folder mime types with FileProvider
            try {
                val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", folder)
                val folderIntent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(contentUri, "resource/folder")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                if (tryStartActivity(context, folderIntent)) {
                    android.widget.Toast.makeText(context, "📁 Opening folder: ${folder.name}", android.widget.Toast.LENGTH_SHORT).show()
                    return true
                }
            } catch (_: Exception) {}

            // 4. Try Downloads intent
            val dlIntent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (tryStartActivity(context, dlIntent)) {
                android.widget.Toast.makeText(context, "📁 Opening Downloads (Path copied to clipboard)", android.widget.Toast.LENGTH_SHORT).show()
                return true
            }

            // 5. Toast path copied
            android.widget.Toast.makeText(
                context,
                "📁 Path copied to clipboard: ${folder.absolutePath}",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return true
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to open folder: ${folder.absolutePath}", e)
            android.widget.Toast.makeText(
                context,
                "Path copied: ${folder.name}",
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return false
        }
    }

    private fun tryStartActivity(context: Context, intent: Intent): Boolean {
        return try {
            context.startActivity(intent)
            true
        } catch (_: Exception) {
            false
        }
    }

    fun copyPathToClipboard(context: Context, path: String) {
        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
            val clip = android.content.ClipData.newPlainText("Folder Path", path)
            clipboard?.setPrimaryClip(clip)
            AppLogger.i("STORAGE", "Copied path to clipboard: $path")
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Failed to copy to clipboard", e)
        }
    }

    fun openFileWithIntent(context: Context, file: File): Boolean {
        return try {
            if (!file.exists()) return false
            if (file.isDirectory) {
                return openFolderWithIntent(context, file)
            }
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = when (file.extension.lowercase(Locale.ROOT)) {
                "md", "txt", "log", "srt", "vtt" -> "text/plain"
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

    data class ExportResult(
        val totalAudioMoved: Int,
        val totalTranscriptsMoved: Int,
        val totalNotesMoved: Int,
        val totalLogsMoved: Int,
        val totalModelsMoved: Int = 0,
        val targetBasePath: String,
        val isSuccess: Boolean,
        val message: String
    )

    /**
     * Scans and exports/migrates all existing media (audio WAV chunks, transcriptions, notes, logs, and models)
     * to the user's currently configured destination folders, updating Room database paths.
     */
    suspend fun exportAllMediaToCurrentLocation(
        context: Context,
        database: com.echo.recorder.data.AppDatabase,
        customAudioDir: String? = null,
        customTranscriptionsDir: String? = null,
        customNotesDir: String? = null,
        customLogsDir: String? = null,
        customModelsDir: String? = null
    ): ExportResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var audioMoved = 0
            var transcriptsMoved = 0
            var notesMoved = 0
            var logsMoved = 0
            var modelsMoved = 0

            val targetAudioDir = getAudioDirectory(context, customAudioDir)
            val targetTranscriptionsDir = getTranscriptionsDirectory(context, customTranscriptionsDir)
            val targetNotesDir = getNotesDirectory(context, customNotesDir)
            val targetLogsDir = getLogsDirectory(context, customLogsDir)
            val targetModelsDir = getModelsDirectory(context, customModelsDir)

            try {
                // 1. Move all audio segments tracked in database
                val allSegments = database.audioSegmentDao().getAllSegmentsList()
                for (segment in allSegments) {
                    val oldFile = File(segment.filePath)
                    if (oldFile.exists()) {
                        val isAlreadyInTarget = try {
                            oldFile.parentFile?.canonicalPath == targetAudioDir.canonicalPath
                        } catch (e: Exception) {
                            oldFile.parentFile?.absolutePath == targetAudioDir.absolutePath
                        }

                        if (!isAlreadyInTarget) {
                            val newFile = File(targetAudioDir, oldFile.name)
                            oldFile.copyTo(newFile, overwrite = true)
                            if (newFile.exists() && newFile.length() == oldFile.length()) {
                                if (oldFile.absolutePath != newFile.absolutePath) {
                                    oldFile.delete()
                                }
                                database.audioSegmentDao().updateSegment(segment.copy(filePath = newFile.absolutePath))
                                audioMoved++
                                AppLogger.d("STORAGE", "Migrated audio segment #${segment.id}: ${oldFile.name} -> ${newFile.absolutePath}")
                            }
                        }
                    }
                }

                // 2. Scan internal audio dirs (e.g. files/audio_chunks or cacheDir) for untracked WAV files
                val internalDirsToScan = listOf(
                    context.filesDir,
                    File(context.filesDir, "audio_chunks"),
                    File(context.filesDir, "poc/yesboss/audio"),
                    context.cacheDir
                )

                for (scanDir in internalDirsToScan) {
                    if (scanDir.exists() && scanDir.isDirectory) {
                        scanDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.extension.lowercase(Locale.ROOT) == "wav") {
                                val isAlreadyInTarget = try {
                                    file.parentFile?.canonicalPath == targetAudioDir.canonicalPath
                                } catch (e: Exception) {
                                    file.parentFile?.absolutePath == targetAudioDir.absolutePath
                                }

                                if (!isAlreadyInTarget) {
                                    val newFile = File(targetAudioDir, file.name)
                                    file.copyTo(newFile, overwrite = true)
                                    if (newFile.exists() && newFile.length() == file.length()) {
                                        if (file.absolutePath != newFile.absolutePath) {
                                            file.delete()
                                        }
                                        audioMoved++
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Migrate Markdown transcription files
                val transcriptScanDirs = listOf(
                    context.filesDir,
                    File(context.filesDir, "transcriptions"),
                    File(context.filesDir, "poc/yesboss/transcriptions"),
                    getBaseDirectory(context)
                )

                for (scanDir in transcriptScanDirs) {
                    if (scanDir.exists() && scanDir.isDirectory) {
                        scanDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.name.startsWith("transcription_") && file.extension.lowercase(Locale.ROOT) == "md") {
                                val isAlreadyInTarget = try {
                                    file.parentFile?.canonicalPath == targetTranscriptionsDir.canonicalPath
                                } catch (e: Exception) {
                                    file.parentFile?.absolutePath == targetTranscriptionsDir.absolutePath
                                }

                                if (!isAlreadyInTarget) {
                                    val newFile = File(targetTranscriptionsDir, file.name)
                                    file.copyTo(newFile, overwrite = true)
                                    if (newFile.exists() && file.absolutePath != newFile.absolutePath) {
                                        file.delete()
                                    }
                                    transcriptsMoved++
                                }
                            }
                        }
                    }
                }

                // 4. Migrate AI Notes files (.md and .html)
                val notesScanDirs = listOf(
                    context.filesDir,
                    File(context.filesDir, "notes"),
                    File(context.filesDir, "poc/yesboss/notes"),
                    getBaseDirectory(context)
                )

                for (scanDir in notesScanDirs) {
                    if (scanDir.exists() && scanDir.isDirectory) {
                        scanDir.listFiles()?.forEach { file ->
                            if (file.isFile && file.name.startsWith("notes_") &&
                                (file.extension.lowercase(Locale.ROOT) in listOf("md", "html"))
                            ) {
                                val isAlreadyInTarget = try {
                                    file.parentFile?.canonicalPath == targetNotesDir.canonicalPath
                                } catch (e: Exception) {
                                    file.parentFile?.absolutePath == targetNotesDir.absolutePath
                                }

                                if (!isAlreadyInTarget) {
                                    val newFile = File(targetNotesDir, file.name)
                                    file.copyTo(newFile, overwrite = true)
                                    if (newFile.exists() && file.absolutePath != newFile.absolutePath) {
                                        file.delete()
                                    }
                                    notesMoved++
                                }
                            }
                        }
                    }
                }

                // 5. Migrate Session Logs (.log and app_logs.txt)
                val logScanDirs = listOf(
                    context.filesDir,
                    File(context.filesDir, "logs"),
                    File(context.filesDir, "poc/yesboss/logs")
                )

                for (scanDir in logScanDirs) {
                    if (scanDir.exists() && scanDir.isDirectory) {
                        scanDir.listFiles()?.forEach { file ->
                            if (file.isFile && (file.extension.lowercase(Locale.ROOT) == "log" || file.name == "app_logs.txt")) {
                                val isAlreadyInTarget = try {
                                    file.parentFile?.canonicalPath == targetLogsDir.canonicalPath
                                } catch (e: Exception) {
                                    file.parentFile?.absolutePath == targetLogsDir.absolutePath
                                }

                                if (!isAlreadyInTarget) {
                                    val newFile = File(targetLogsDir, file.name)
                                    file.copyTo(newFile, overwrite = true)
                                    logsMoved++
                                }
                            }
                        }
                    }
                }

                // 6. Migrate and backup Models (Vosk models, GGUF weights, zips)
                val modelScanDirs = listOf(
                    context.filesDir,
                    File(context.filesDir, "models"),
                    File(context.filesDir, "poc/yesboss/models"),
                    context.cacheDir,
                    File(context.cacheDir, "models")
                )

                for (scanDir in modelScanDirs) {
                    if (scanDir.exists() && scanDir.isDirectory) {
                        scanDir.listFiles()?.forEach { fileOrDir ->
                            val isAlreadyInTarget = try {
                                fileOrDir.canonicalPath.startsWith(targetModelsDir.canonicalPath)
                            } catch (e: Exception) {
                                fileOrDir.absolutePath.startsWith(targetModelsDir.absolutePath)
                            }

                            if (!isAlreadyInTarget && (fileOrDir.name.contains("vosk") ||
                                fileOrDir.name.endsWith(".zip") ||
                                fileOrDir.name.endsWith(".gguf") ||
                                fileOrDir.name.endsWith(".bin"))
                            ) {
                                val dest = File(targetModelsDir, fileOrDir.name)
                                if (fileOrDir.isDirectory) {
                                    fileOrDir.copyRecursively(dest, overwrite = true)
                                } else {
                                    fileOrDir.copyTo(dest, overwrite = true)
                                }
                                modelsMoved++
                            }
                        }
                    }
                }

                // 7. Clean up any previous broken [Pending: error strings from the database
                database.transcriptionDao().cleanupPendingErrorTexts()

                val baseTargetPath = targetAudioDir.parentFile?.absolutePath ?: targetAudioDir.absolutePath
                val summaryMsg = "Successfully exported $audioMoved audio recordings, $transcriptsMoved transcriptions, $notesMoved notes, $logsMoved logs, and $modelsMoved models to: $baseTargetPath"
                AppLogger.i("STORAGE", summaryMsg)

                ExportResult(
                    totalAudioMoved = audioMoved,
                    totalTranscriptsMoved = transcriptsMoved,
                    totalNotesMoved = notesMoved,
                    totalLogsMoved = logsMoved,
                    totalModelsMoved = modelsMoved,
                    targetBasePath = baseTargetPath,
                    isSuccess = true,
                    message = summaryMsg
                )
            } catch (e: Exception) {
                AppLogger.e("STORAGE", "Error during media export", e)
                ExportResult(
                    totalAudioMoved = audioMoved,
                    totalTranscriptsMoved = transcriptsMoved,
                    totalNotesMoved = notesMoved,
                    totalLogsMoved = logsMoved,
                    totalModelsMoved = modelsMoved,
                    targetBasePath = targetAudioDir.absolutePath,
                    isSuccess = false,
                    message = "Export error: ${e.message}"
                )
            }
        }
    }

    /**
     * Dedicated model exporter: copies all models in the models directory to the public storage location.
     */
    suspend fun exportModelsDirectory(
        context: Context,
        customDestDir: String? = null
    ): ExportResult {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val sourceDir = getModelsDirectory(context)
            val targetDir = if (!customDestDir.isNullOrBlank()) {
                File(customDestDir).apply { mkdirs() }
            } else {
                File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "App data/poc/yesboss/models").apply { mkdirs() }
            }
            var modelsCopied = 0
            try {
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.listFiles()?.forEach { fileOrDir ->
                        val dest = File(targetDir, fileOrDir.name)
                        if (fileOrDir.isDirectory) {
                            fileOrDir.copyRecursively(dest, overwrite = true)
                        } else {
                            fileOrDir.copyTo(dest, overwrite = true)
                        }
                        modelsCopied++
                    }
                }
                val summary = "Successfully exported $modelsCopied models/weights to: ${targetDir.absolutePath}"
                AppLogger.i("STORAGE", summary)
                ExportResult(
                    totalAudioMoved = 0,
                    totalTranscriptsMoved = 0,
                    totalNotesMoved = 0,
                    totalLogsMoved = 0,
                    totalModelsMoved = modelsCopied,
                    targetBasePath = targetDir.absolutePath,
                    isSuccess = true,
                    message = summary
                )
            } catch (e: Exception) {
                AppLogger.e("STORAGE", "Error exporting models directory", e)
                ExportResult(
                    totalAudioMoved = 0,
                    totalTranscriptsMoved = 0,
                    totalNotesMoved = 0,
                    totalLogsMoved = 0,
                    totalModelsMoved = modelsCopied,
                    targetBasePath = targetDir.absolutePath,
                    isSuccess = false,
                    message = "Export failed: ${e.message}"
                )
            }
        }
    }

    /**
     * Packages all models into a single ZIP file for backup and PC transfer.
     */
    suspend fun exportModelsToZip(context: Context): File? {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sourceDir = getModelsDirectory(context)
                val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                val destDir = getBaseDirectory(context)
                val zipFile = File(destDir, "yesboss_models_backup_$timeStr.zip")
                java.util.zip.ZipOutputStream(zipFile.outputStream()).use { zos ->
                    zipSubfolder(sourceDir, sourceDir, zos)
                }
                AppLogger.i("STORAGE", "Exported models zip created: ${zipFile.absolutePath} (${zipFile.length()} bytes)")
                zipFile
            } catch (e: Exception) {
                AppLogger.e("STORAGE", "Failed to create models zip export", e)
                null
            }
        }
    }

    private fun zipSubfolder(rootDir: File, currentFile: File, zos: java.util.zip.ZipOutputStream) {
        if (currentFile.isDirectory) {
            currentFile.listFiles()?.forEach { child ->
                zipSubfolder(rootDir, child, zos)
            }
        } else {
            val relativePath = currentFile.relativeTo(rootDir).path.replace('\\', '/')
            val entry = java.util.zip.ZipEntry(relativePath)
            zos.putNextEntry(entry)
            currentFile.inputStream().use { input ->
                input.copyTo(zos)
            }
            zos.closeEntry()
        }
    }

    /**
     * Deletes dummy waste stub files (.bin with fake text) created by previous versions,
     * while preserving genuine models.
     */
    fun cleanupWasteModels(context: Context): Int {
        var deletedCount = 0
        try {
            val modelsDir = getModelsDirectory(context)
            val wasteStubs = listOf(
                "whisper_tiny.bin",
                "whisper_base.bin",
                "indic_conformer.bin",
                "local_llama.bin"
            )
            for (stubName in wasteStubs) {
                val stubFile = File(modelsDir, stubName)
                if (stubFile.exists()) {
                    if (stubFile.length() < 10_000L) {
                        if (stubFile.delete()) {
                            deletedCount++
                            AppLogger.i("STORAGE", "Deleted waste stub model: $stubName")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("STORAGE", "Error cleaning up waste models", e)
        }
        return deletedCount
    }
}
