package com.echo.recorder.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val level: String, // "INFO", "WARN", "ERROR", "DEBUG"
    val tag: String,
    val message: String
)

object AppLogger {
    private const val MAX_IN_MEMORY_LOGS = 500
    private const val MAX_SESSION_LOG_SIZE_BYTES = 512 * 1024L // 512 KB rollover limit
    private val logList = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var appContext: Context? = null
    private var customLogsDirPath: String? = null
    private var appLogFile: File? = null
    private var sessionLogFile: File? = null
    private var sessionLogsDir: File? = null

    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val sessionDateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)

    private var logFileExtension: String = "txt"

    fun setLogFileExtension(ext: String) {
        logFileExtension = ext.removePrefix(".")
    }

    fun init(context: Context, customLogsDir: String? = null, extension: String = "txt") {
        appContext = context.applicationContext
        customLogsDirPath = customLogsDir
        logFileExtension = extension.removePrefix(".")
        sessionLogsDir = AppStorageManager.getLogsDirectory(context, customLogsDir)
        appLogFile = File(context.filesDir, "app_logs.txt")

        val sessionTimestamp = sessionDateFormat.format(Date())
        sessionLogFile = File(sessionLogsDir, "session_${sessionTimestamp}.${logFileExtension}")

        i("SYSTEM", "Madhumangal Logger initialized. Active session file: ${sessionLogFile?.absolutePath}")
    }

    fun startNewSession(context: Context, customLogsDir: String? = null, extension: String? = null) {
        appContext = context.applicationContext
        if (customLogsDir != null) customLogsDirPath = customLogsDir
        if (extension != null) logFileExtension = extension.removePrefix(".")
        sessionLogsDir = AppStorageManager.getLogsDirectory(context, customLogsDirPath)
        val sessionTimestamp = sessionDateFormat.format(Date())
        sessionLogFile = File(sessionLogsDir, "session_${sessionTimestamp}.${logFileExtension}")
        i("SYSTEM", "=== New Session Started: ${sessionLogFile?.name} ===")
    }

    @Synchronized
    fun log(level: String, tag: String, message: String, throwable: Throwable? = null) {
        val now = Date()
        val timeStr = timeFormat.format(now)
        val fileTimeStr = fileDateFormat.format(now)
        val fullMsg = if (throwable != null) {
            "$message | Exception: ${throwable.message}\n${Log.getStackTraceString(throwable)}"
        } else {
            message
        }

        try {
            when (level) {
                "DEBUG" -> Log.d(tag, fullMsg)
                "INFO" -> Log.i(tag, fullMsg)
                "WARN" -> Log.w(tag, fullMsg)
                "ERROR" -> Log.e(tag, fullMsg)
                else -> Log.v(tag, fullMsg)
            }
        } catch (_: Throwable) {
            println("[$level] [$tag] $fullMsg")
        }

        val entry = LogEntry(timeStr, level, tag, fullMsg)
        logList.add(entry)
        if (logList.size > MAX_IN_MEMORY_LOGS) {
            logList.removeAt(0)
        }
        _logsFlow.value = logList.toList()

        val logLine = "[$fileTimeStr] [$level] [$tag] $fullMsg\n"

        // Check for automatic rollover if session log exceeds 512 KB
        val currentFile = sessionLogFile
        if (currentFile != null && currentFile.exists() && currentFile.length() > MAX_SESSION_LOG_SIZE_BYTES) {
            val oldName = currentFile.name
            appContext?.let { ctx ->
                startNewSession(ctx, customLogsDirPath)
                val rolledMsg = "Log file $oldName exceeded 512KB. Automatically rolled over to new session: ${sessionLogFile?.name}"
                val rolloverEntry = LogEntry(timeFormat.format(Date()), "INFO", "SYSTEM", rolledMsg)
                logList.add(rolloverEntry)
                _logsFlow.value = logList.toList()
            }
        }

        // Append to current session log file in user-configured folder
        sessionLogFile?.let { file ->
            try {
                FileWriter(file, true).use { writer ->
                    writer.write(logLine)
                }
            } catch (e: Exception) {
                // Ignore file write error
            }
        }

        // Also append to main internal app log file
        appLogFile?.let { file ->
            try {
                FileWriter(file, true).use { writer ->
                    writer.write(logLine)
                }
            } catch (e: Exception) {
                // Ignore file write error
            }
        }
    }

    fun d(tag: String, msg: String) = log("DEBUG", tag, msg)
    fun i(tag: String, msg: String) = log("INFO", tag, msg)
    fun w(tag: String, msg: String) = log("WARN", tag, msg)
    fun e(tag: String, msg: String, t: Throwable? = null) = log("ERROR", tag, msg, t)

    fun getAllLogsText(): String {
        return logList.joinToString("\n") { "[${it.timestamp}] [${it.level}] [${it.tag}] ${it.message}" }
    }

    fun getAllSessionLogsText(context: Context): String {
        val files = getSessionFiles(context).sortedBy { it.lastModified() }
        if (files.isEmpty()) return getAllLogsText()
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("   MADHUMANGAL GLOBAL EXPORT OF ALL SESSIONS\n")
        sb.append("   Total Sessions: ${files.size}\n")
        sb.append("   Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("=========================================\n\n")

        for (file in files) {
            sb.append("--------------------------------------------------\n")
            sb.append("SESSION FILE: ${file.name} (${file.length() / 1024} KB)\n")
            sb.append("--------------------------------------------------\n")
            try {
                sb.append(file.readText())
            } catch (e: Exception) {
                sb.append("[Error reading file ${file.name}: ${e.message}]\n")
            }
            sb.append("\n\n")
        }
        return sb.toString()
    }

    fun getLogFile(): File? = sessionLogFile ?: appLogFile

    fun getCurrentSessionFile(): File? = sessionLogFile

    fun getSessionFiles(context: Context, customLogsDir: String? = null): List<File> {
        val dir = sessionLogsDir ?: AppStorageManager.getLogsDirectory(context, customLogsDir)
        return dir.listFiles { f -> f.isFile && (f.extension.equals("log", true) || f.extension.equals("txt", true) || f.extension.equals("md", true)) }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()
    }

    fun exportSelectedSessionLogs(files: List<File>): String {
        if (files.isEmpty()) return ""
        val sb = StringBuilder()
        sb.append("=========================================\n")
        sb.append("   MADHUMANGAL MULTI-SESSION LOG EXPORT\n")
        sb.append("   Total Selected Sessions: ${files.size}\n")
        sb.append("   Export Time: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("=========================================\n\n")

        for (file in files) {
            sb.append("--------------------------------------------------\n")
            sb.append("SESSION FILE: ${file.name} (${file.length() / 1024} KB)\n")
            sb.append("--------------------------------------------------\n")
            try {
                sb.append(file.readText())
            } catch (e: Exception) {
                sb.append("[Error reading file ${file.name}: ${e.message}]\n")
            }
            sb.append("\n\n")
        }
        return sb.toString()
    }

    fun logSafeExit() {
        i("SYSTEM", "=== Application Session Safely Exited & Flushed ===")
    }

    fun getFilteredLogsText(filter: String, entries: List<LogEntry>): String {
        val sb = StringBuilder()
        val activeFile = getCurrentSessionFile()
        sb.append("====================================================\n")
        sb.append("   MADHUMANGAL SESSION LOGS\n")
        sb.append("   Session File: ${activeFile?.name ?: "active"}\n")
        sb.append("   Filter Applied: $filter\n")
        sb.append("   Total Matching Entries: ${entries.size}\n")
        sb.append("   Exported At: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())}\n")
        sb.append("====================================================\n\n")

        for (entry in entries) {
            sb.append("[${entry.timestamp}] [${entry.level}] [${entry.tag}] ${entry.message}\n")
        }
        return sb.toString()
    }

    @Synchronized
    fun clearLogs(context: Context? = null) {
        logList.clear()
        _logsFlow.value = emptyList()
        val ctx = context ?: appContext
        if (ctx != null) {
            val sessionTimestamp = sessionDateFormat.format(Date())
            sessionLogFile = File(sessionLogsDir ?: AppStorageManager.getLogsDirectory(ctx, customLogsDirPath), "session_${sessionTimestamp}.${logFileExtension}")
        }
        i("SYSTEM", "Display cleared by user. New session started: ${sessionLogFile?.name ?: "active"}")
    }
}
