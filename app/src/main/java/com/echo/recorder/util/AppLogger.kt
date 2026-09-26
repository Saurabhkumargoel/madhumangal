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
    private const val MAX_IN_MEMORY_LOGS = 400
    private val logList = mutableListOf<LogEntry>()
    private val _logsFlow = MutableStateFlow<List<LogEntry>>(emptyList())
    val logsFlow: StateFlow<List<LogEntry>> = _logsFlow.asStateFlow()

    private var logFile: File? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    private val fileDateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)

    fun init(context: Context) {
        logFile = File(context.filesDir, "app_logs.txt")
        i("SYSTEM", "GoodEcho Logger initialized. Log file: ${logFile?.absolutePath}")
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

        when (level) {
            "DEBUG" -> Log.d(tag, fullMsg)
            "INFO" -> Log.i(tag, fullMsg)
            "WARN" -> Log.w(tag, fullMsg)
            "ERROR" -> Log.e(tag, fullMsg)
            else -> Log.v(tag, fullMsg)
        }

        val entry = LogEntry(timeStr, level, tag, fullMsg)
        logList.add(entry)
        if (logList.size > MAX_IN_MEMORY_LOGS) {
            logList.removeAt(0)
        }
        _logsFlow.value = logList.toList()

        // Append to persistent log file
        logFile?.let { file ->
            try {
                FileWriter(file, true).use { writer ->
                    writer.write("[$fileTimeStr] [$level] [$tag] $fullMsg\n")
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

    fun getLogFile(): File? = logFile

    @Synchronized
    fun clearLogs() {
        logList.clear()
        _logsFlow.value = emptyList()
        try {
            logFile?.writeText("")
        } catch (e: Exception) {
            // Ignore
        }
        i("SYSTEM", "Logs cleared by user")
    }
}
