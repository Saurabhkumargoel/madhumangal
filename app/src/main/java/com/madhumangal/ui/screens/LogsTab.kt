package com.madhumangal.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.text.selection.SelectionContainer
import com.madhumangal.ui.viewmodel.MainViewModel
import com.madhumangal.data.UserPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhumangal.ui.theme.AccentPurple
import com.madhumangal.ui.theme.ActiveRed
import com.madhumangal.ui.theme.CardBorder
import com.madhumangal.ui.theme.DarkBackground
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import com.madhumangal.util.AppLogger
import com.madhumangal.util.AppStorageManager
import com.madhumangal.util.LogEntry
import androidx.core.content.FileProvider

data class LogSessionSeries(
    val id: String,
    val files: List<File>,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val title: String,
    val totalSizeBytes: Long
)

fun parseLogFileTimestamp(file: File): Long {
    val name = file.name
    val matchFull = Regex("20\\d{6}_\\d{6}").find(name)
    if (matchFull != null) {
        try {
            val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val d = sdf.parse(matchFull.value)
            if (d != null) return d.time
        } catch (_: Exception) {}
    }
    val matchShort = Regex("20\\d{10}").find(name)
    if (matchShort != null) {
        try {
            val sdf = SimpleDateFormat("yyyyMMddHHmm", Locale.US)
            val d = sdf.parse(matchShort.value)
            if (d != null) return d.time
        } catch (_: Exception) {}
    }
    return file.lastModified()
}

fun groupLogFilesByTimeGap(
    files: List<File>,
    gapMinutes: Int,
    isHindi: Boolean
): List<LogSessionSeries> {
    if (files.isEmpty()) return emptyList()

    val sorted = files.sortedBy { parseLogFileTimestamp(it) }

    if (gapMinutes <= 0) {
        val timeFmt = SimpleDateFormat("MMM d, hh:mm:ss a", if (isHindi) Locale("hi", "IN") else Locale.US)
        return sorted.reversed().map { file ->
            val t = parseLogFileTimestamp(file)
            LogSessionSeries(
                id = file.name,
                files = listOf(file),
                startTimeMillis = t,
                endTimeMillis = t,
                title = timeFmt.format(Date(t)),
                totalSizeBytes = file.length()
            )
        }
    }

    val gapMillis = gapMinutes * 60 * 1000L
    val clusters = mutableListOf<MutableList<Pair<File, Long>>>()
    var currentCluster = mutableListOf<Pair<File, Long>>()

    for (file in sorted) {
        val t = parseLogFileTimestamp(file)
        if (currentCluster.isEmpty()) {
            currentCluster.add(file to t)
        } else {
            val lastTime = currentCluster.last().second
            if (t - lastTime <= gapMillis) {
                currentCluster.add(file to t)
            } else {
                clusters.add(currentCluster)
                currentCluster = mutableListOf(file to t)
            }
        }
    }
    if (currentCluster.isNotEmpty()) {
        clusters.add(currentCluster)
    }

    val timeFmt = SimpleDateFormat("hh:mm a", if (isHindi) Locale("hi", "IN") else Locale.US)
    val dateFmt = SimpleDateFormat("MMM d", if (isHindi) Locale("hi", "IN") else Locale.US)

    return clusters.reversed().mapIndexed { index, cluster ->
        val clusterFiles = cluster.map { it.first }.reversed()
        val startT = cluster.first().second
        val endT = cluster.last().second
        val totalBytes = clusterFiles.sumOf { it.length() }
        val id = "series_${startT}_${cluster.size}_$index"

        val title = if (cluster.size == 1) {
            "${dateFmt.format(Date(startT))}, ${timeFmt.format(Date(startT))}"
        } else {
            val dateStr = dateFmt.format(Date(startT))
            val startStr = timeFmt.format(Date(startT))
            val endStr = timeFmt.format(Date(endT))
            val spanMinutes = ((endT - startT) / (60 * 1000L)).coerceAtLeast(1)
            "$dateStr, $startStr - $endStr ($spanMinutes min)"
        }

        LogSessionSeries(
            id = id,
            files = clusterFiles,
            startTimeMillis = startT,
            endTimeMillis = endT,
            title = title,
            totalSizeBytes = totalBytes
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LogsTab(
    viewModel: MainViewModel? = null,
    userPreferences: UserPreferences? = null,
    listState: LazyListState = rememberLazyListState(),
    isHindi: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val logs by AppLogger.logsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }

    var isManageFilesExpanded by remember { mutableStateOf(false) }
    var selectedFiles by remember { mutableStateOf<Set<File>>(emptySet()) }
    var showClearLogsConfirm by remember { mutableStateOf(false) }
    var showDeleteFilesConfirm by remember { mutableStateOf(false) }
    var refreshFilesTrigger by remember { mutableStateOf(0) }

    // Intercept Back button: cancel file selection first before ever exiting the app
    BackHandler(enabled = selectedFiles.isNotEmpty() || showClearLogsConfirm || showDeleteFilesConfirm) {
        when {
            showClearLogsConfirm -> showClearLogsConfirm = false
            showDeleteFilesConfirm -> showDeleteFilesConfirm = false
            selectedFiles.isNotEmpty() -> selectedFiles = emptySet()
        }
    }

    var currentLogPage by remember { mutableStateOf(0) }
    var collapsedSeriesIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val logGapMinutes = userPreferences?.logGroupingGapMinutes ?: 5
    val filesPerPageSetting = userPreferences?.logFilesPerPage ?: 10

    val sessionFiles = remember(logs.size, isManageFilesExpanded, refreshFilesTrigger) { AppLogger.getSessionFiles(context) }
    val sortedSessionFiles = remember(sessionFiles) {
        sessionFiles.sortedByDescending { parseLogFileTimestamp(it) }
    }
    val totalFiles = sortedSessionFiles.size
    val pageSize = if (filesPerPageSetting <= 0) totalFiles.coerceAtLeast(1) else filesPerPageSetting
    val totalPages = if (totalFiles == 0) 1 else ((totalFiles + pageSize - 1) / pageSize)
    val safePage = currentLogPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
    val startIndex = safePage * pageSize
    val endIndex = (startIndex + pageSize).coerceAtMost(totalFiles)
    val pagedFiles = if (totalFiles == 0) emptyList() else sortedSessionFiles.subList(startIndex, endIndex)

    val groupedSeries = remember(pagedFiles, logGapMinutes, isHindi) {
        groupLogFilesByTimeGap(pagedFiles, logGapMinutes, isHindi)
    }

    // ── Dialog: Confirm Clear Logs ──
    if (showClearLogsConfirm) {
        AlertDialog(
            onDismissRequest = { showClearLogsConfirm = false },
            title = { Text(if (isHindi) "🗑️ लॉग साफ़ करें?" else "🗑️ Clear All Logs?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(if (isHindi) "क्या आप वाकई वर्तमान सत्र और मेमोरी से सभी लॉग हटाना चाहते हैं?" else "Are you sure you want to clear all in-memory and active session logs?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        AppLogger.clearLogs(context)
                        showClearLogsConfirm = false
                        Toast.makeText(context, if (isHindi) "लॉग साफ़ किए गए" else "Logs cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "साफ़ करें" else "Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearLogsConfirm = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Dialog: Confirm Delete Selected Log Files ──
    if (showDeleteFilesConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteFilesConfirm = false },
            title = { Text(if (isHindi) "🗑️ चयनित फ़ाइलें हटाएं?" else "🗑️ Delete Selected Log Files?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(if (isHindi) "क्या आप वाकई ${selectedFiles.size} चयनित लॉग फ़ाइल(ओं) को स्थायी रूप से हटाना चाहते हैं?" else "Are you sure you want to permanently delete ${selectedFiles.size} selected log file(s)?", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        var deletedCount = 0
                        selectedFiles.forEach { f ->
                            if (f.delete()) deletedCount++
                        }
                        selectedFiles = emptySet()
                        refreshFilesTrigger++
                        showDeleteFilesConfirm = false
                        Toast.makeText(context, if (isHindi) "$deletedCount लॉग फ़ाइल(ें) हटाई गईं" else "Deleted $deletedCount log file(s)", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteFilesConfirm = false }) {
                    Text(if (isHindi) "रद्द करें" else "Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> logs
            "ERROR" -> logs.filter { it.level == "ERROR" || it.level == "WARN" }
            "STT" -> logs.filter { it.tag == "STT" || it.tag == "STT_ENGINE" }
            "AUDIO" -> logs.filter { it.tag == "AUDIO" || it.tag == "SERVICE" }
            "AI" -> logs.filter { it.tag == "AI" }
            else -> logs
        }
    }

    // Auto-scroll to bottom as new logs arrive
    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.size - 1)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
        // Title & Entry Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (isHindi) "सिस्टम और एआई लॉग्स" else "System & AI Logs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isHindi) "रीयल-टाइम बैकग्राउंड ऑडियो, वाक् व एआई घटनाएं" else "Real-time background audio, STT, and AI events",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PrimaryBlue.copy(alpha = 0.15f),
                border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue)
            ) {
                Text(
                    text = if (isHindi) "${logs.size} प्रविष्टियां" else "${logs.size} entries",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Session Log File Banner with 'New File' button
        val sessionFile = remember(logs.size) { AppLogger.getCurrentSessionFile() }
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isHindi) "📄 सक्रिय: ${sessionFile?.name ?: "session_active.log"}" else "📄 Active: ${sessionFile?.name ?: "session_active.log"}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = sessionFile?.absolutePath ?: "Download/App data/poc/yesboss/logs/",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = {
                            AppLogger.startNewSession(context)
                            Toast.makeText(context, if (isHindi) "नया सत्र लॉग फ़ाइल शुरू हुआ!" else "Started new session log file!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                    ) {
                        Text(if (isHindi) "➕ नया लॉग" else "➕ New Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val activeFile = AppLogger.getCurrentSessionFile()
                            val textToExport = if (selectedFilter != "ALL") {
                                AppLogger.getFilteredLogsText(selectedFilter, filteredLogs)
                            } else if (activeFile != null && activeFile.exists()) {
                                activeFile.readText()
                            } else {
                                AppLogger.getAllLogsText()
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, textToExport)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, if (isHindi) "सक्रिय सत्र शेयर करें" else "Share Active Session (${activeFile?.name ?: "active"})")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(if (isHindi) "शेयर" else "Share", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(8.dp))

        // ── ⚙️ Log Extension Selector (.log, .txt, .md) ──
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "लॉग फ़ाइल एक्सटेंशन:" else "Log Extension:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                val currentExt = userPreferences?.logFileExtension ?: "log"
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("log", "txt", "md").forEach { ext ->
                        val isSelected = currentExt.equals(ext, ignoreCase = true)
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) PrimaryBlue else Color.Transparent,
                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                            modifier = Modifier.clickable {
                                viewModel?.updateLogFileExtension(ext)
                                AppLogger.setLogFileExtension(ext)
                                Toast.makeText(context, if (isHindi) "एक्सटेंशन .$ext सेट हुआ" else "Extension set to .$ext", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text(
                                text = ".$ext",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) DarkBackground else TextPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ── 📁 Manage Past Session Files (Multi-Select & Bulk Export) ──
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isManageFilesExpanded = !isManageFilesExpanded },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "पुराने सत्र लॉग (${sessionFiles.size} फ़ाइलें)" else "Past Session Logs (${sessionFiles.size} files)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (selectedFiles.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(shape = RoundedCornerShape(4.dp), color = AccentPurple.copy(alpha = 0.2f)) {
                                Text("${selectedFiles.size} sel", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                            }
                        }
                    }
                    Icon(
                        imageVector = if (isManageFilesExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = "Toggle",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (isManageFilesExpanded) {
                    Spacer(modifier = Modifier.height(8.dp))
                    // Select All / Deselect All Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                selectedFiles = if (selectedFiles.size == sessionFiles.size) emptySet() else sessionFiles.toSet()
                            },
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text(
                                text = if (selectedFiles.size == sessionFiles.size) (if (isHindi) "चयन हटाएं" else "Deselect All") else (if (isHindi) "सभी चुनें" else "Select All"),
                                fontSize = 11.sp,
                                color = PrimaryBlue
                            )
                        }

                        if (selectedFiles.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Share Selected Actual Files via FileProvider
                                OutlinedButton(
                                    onClick = {
                                        shareLogFiles(context, selectedFiles.toList(), isHindi)
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(if (isHindi) "फ़ाइलें (${selectedFiles.size})" else "Files (${selectedFiles.size})", fontSize = 10.sp, color = AccentPurple, fontWeight = FontWeight.Bold)
                                }

                                // Copy .md
                                OutlinedButton(
                                    onClick = {
                                        val combined = AppLogger.exportSelectedSessionLogs(selectedFiles.toList())
                                        clipboardManager.setText(AnnotatedString(combined))
                                        Toast.makeText(context, if (isHindi) "${selectedFiles.size} फ़ाइलें .md कॉपी की गईं!" else "Copied ${selectedFiles.size} log file(s) as .md!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(".md", fontSize = 10.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }

                                // Copy .txt
                                OutlinedButton(
                                    onClick = {
                                        val combined = AppLogger.exportSelectedSessionLogs(selectedFiles.toList())
                                        clipboardManager.setText(AnnotatedString(combined))
                                        Toast.makeText(context, if (isHindi) "${selectedFiles.size} फ़ाइलें .txt कॉपी की गईं!" else "Copied ${selectedFiles.size} log file(s) as .txt!", Toast.LENGTH_SHORT).show()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(11.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text(".txt", fontSize = 10.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                }

                                // Delete Selected
                                OutlinedButton(
                                    onClick = { showDeleteFilesConfirm = true },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(26.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = ActiveRed, modifier = Modifier.size(12.dp))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // 1. Threshold Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "श्रृंखला गैप:" else "Series Gap:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val gapOptions = listOf(
                                0 to (if (isHindi) "फ्लैट" else "Flat"),
                                2 to "2m",
                                5 to "5m",
                                15 to "15m",
                                30 to "30m",
                                60 to "1h"
                            )
                            gapOptions.forEach { (mins, label) ->
                                val isSelected = logGapMinutes == mins
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) AccentPurple else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                    modifier = Modifier.clickable {
                                        viewModel?.updateLogGroupingGapMinutes(mins)
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 2. Files Per Page Selector Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isHindi) "प्रति पेज फ़ाइलें:" else "Files / Page:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val pageOptions = listOf(
                                5 to "5",
                                10 to "10",
                                20 to "20",
                                50 to "50",
                                0 to (if (isHindi) "सभी" else "All")
                            )
                            pageOptions.forEach { (count, label) ->
                                val isSelected = filesPerPageSetting == count
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isSelected) PrimaryBlue else Color.Transparent,
                                    border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                    modifier = Modifier.clickable {
                                        viewModel?.updateLogFilesPerPage(count)
                                        currentLogPage = 0
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (sessionFiles.isEmpty()) {
                        Text(
                            text = if (isHindi) "कोई पुरानी सत्र फ़ाइलें नहीं मिलीं" else "No past session log files found",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            groupedSeries.forEach { series ->
                                val isCollapsed = series.id in collapsedSeriesIds
                                val allInSeriesSelected = series.files.isNotEmpty() && series.files.all { it in selectedFiles }
                                val someInSeriesSelected = series.files.any { it in selectedFiles }
                                val seriesKb = String.format(Locale.US, "%.1f KB", series.totalSizeBytes / 1024f)

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                    border = BorderStroke(1.dp, if (allInSeriesSelected) PrimaryBlue.copy(alpha = 0.7f) else CardBorder),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        // Series Header
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    collapsedSeriesIds = if (isCollapsed) collapsedSeriesIds - series.id else collapsedSeriesIds + series.id
                                                },
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = allInSeriesSelected,
                                                onCheckedChange = { chk ->
                                                    selectedFiles = if (chk) {
                                                        selectedFiles + series.files
                                                    } else {
                                                        selectedFiles - series.files.toSet()
                                                    }
                                                },
                                                colors = CheckboxDefaults.colors(
                                                    checkedColor = PrimaryBlue,
                                                    uncheckedColor = if (someInSeriesSelected) AccentPurple else CardBorder
                                                ),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = series.title,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (allInSeriesSelected) PrimaryBlue else TextPrimary,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "${series.files.size} ${if (series.files.size == 1) "file" else "files"} • $seriesKb",
                                                    fontSize = 9.sp,
                                                    color = TextMuted
                                                )
                                            }
                                            Icon(
                                                imageVector = if (isCollapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                                contentDescription = "Expand Series",
                                                tint = TextMuted,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Files in Series (if not collapsed)
                                        if (!isCollapsed) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Column(
                                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier.padding(start = 22.dp)
                                            ) {
                                                series.files.forEach { file ->
                                                    val isFileChecked = file in selectedFiles
                                                    val fileKb = String.format(Locale.US, "%.1f KB", file.length() / 1024f)
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable {
                                                                selectedFiles = if (isFileChecked) selectedFiles - file else selectedFiles + file
                                                            }
                                                            .padding(vertical = 1.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Checkbox(
                                                            checked = isFileChecked,
                                                            onCheckedChange = { chk ->
                                                                selectedFiles = if (chk) selectedFiles + file else selectedFiles - file
                                                            },
                                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            text = file.name,
                                                            fontSize = 10.sp,
                                                            color = if (isFileChecked) PrimaryBlue else TextSecondary,
                                                            modifier = Modifier.weight(1f),
                                                            maxLines = 1
                                                        )
                                                        Text(
                                                            text = fileKb,
                                                            fontSize = 9.sp,
                                                            color = TextMuted
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. Pagination Bar
                    if (totalPages > 1) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { if (safePage > 0) currentLogPage = safePage - 1 },
                                enabled = safePage > 0,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(if (isHindi) "◀ पिछला" else "◀ Prev", fontSize = 10.sp)
                            }

                            Text(
                                text = if (isHindi) "पेज ${safePage + 1} / $totalPages ($totalFiles फ़ाइलें)" else "Page ${safePage + 1} of $totalPages ($totalFiles files)",
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )

                            OutlinedButton(
                                onClick = { if (safePage < totalPages - 1) currentLogPage = safePage + 1 },
                                enabled = safePage < totalPages - 1,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Text(if (isHindi) "अगला ▶" else "Next ▶", fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Chips: ALL, STT, AUDIO, AI, ERROR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "STT", "AUDIO", "AI", "ERROR").forEach { filterTag ->
                val isSelected = selectedFilter == filterTag
                val label = when (filterTag) {
                    "ALL" -> if (isHindi) "सभी" else "ALL"
                    "AUDIO" -> if (isHindi) "ऑडियो" else "AUDIO"
                    "ERROR" -> if (isHindi) "त्रुटि" else "ERROR"
                    else -> filterTag
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = filterTag }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) DarkBackground else TextPrimary,
                        modifier = Modifier.padding(vertical = 6.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Logs Feed Container (Text is Selectable via SelectionContainer)
        SelectionContainer {
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
            ) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (logs.isEmpty()) (if (isHindi) "अभी तक कोई लॉग रिकॉर्ड नहीं हुआ।\nगतिविधि देखने के लिए रिकॉर्डिंग या प्रतिलेखन शुरू करें।" else "No logs captured yet.\nStart recording or transcribing to see activity.")
                               else (if (isHindi) "'$selectedFilter' फ़िल्टर से कोई लॉग मेल नहीं खाता।" else "No logs match '$selectedFilter' filter."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                val logSections = remember(filteredLogs) {
                    val sections = mutableListOf<LogSection>()
                    var currentHeader: LogEntry? = null
                    var currentList = mutableListOf<LogEntry>()

                    for (entry in filteredLogs) {
                        val isSessionHeader = entry.message.contains("=== New Session Started:") ||
                                entry.message.contains("Active session file:") ||
                                entry.message.contains("rolled over to new session:")
                        if (isSessionHeader) {
                            if (currentHeader != null || currentList.isNotEmpty()) {
                                sections.add(LogSection(currentHeader, currentList))
                                currentList = mutableListOf()
                            }
                            currentHeader = entry
                        } else {
                            currentList.add(entry)
                        }
                    }
                    if (currentHeader != null || currentList.isNotEmpty()) {
                        sections.add(LogSection(currentHeader, currentList))
                    }
                    sections
                }

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logSections.forEachIndexed { index, section ->
                        if (section.header != null) {
                            stickyHeader(key = "log_header_${index}_${section.header.timestamp}") {
                                SessionDividerRow(
                                    entry = section.header,
                                    selectedFilter = selectedFilter,
                                    filteredLogs = filteredLogs,
                                    isHindi = isHindi
                                )
                            }
                        }
                        items(
                            items = section.logs,
                            key = { "${it.timestamp}_${it.tag}_${it.message.hashCode()}_${it.level}" }
                        ) { entry ->
                            LogItemRow(entry)
                        }
                    }
                }
            }
        }
    }
    }

    // Floating Share Button at bottom left: shares visible sessions (or selected sessions) with live count
    val targetShareFiles = remember(selectedFiles, pagedFiles) {
        if (selectedFiles.isNotEmpty()) selectedFiles.toList() else pagedFiles
    }
    val shareCount = targetShareFiles.size

    ExtendedFloatingActionButton(
        onClick = {
            shareLogFiles(context, targetShareFiles, isHindi)
        },
        containerColor = AccentPurple,
        contentColor = Color.White,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(start = 16.dp, bottom = 16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.Share,
            contentDescription = "Share Session Files",
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = if (selectedFiles.isNotEmpty())
                (if (isHindi) "📤 शेयर ($shareCount)" else "📤 Share ($shareCount)")
            else
                (if (isHindi) "📤 दृश्य सत्र ($shareCount)" else "📤 Visible ($shareCount)"),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
}

private fun shareLogFiles(context: Context, files: List<File>, isHindi: Boolean) {
    if (files.isEmpty()) {
        Toast.makeText(context, if (isHindi) "शेयर करने के लिए कोई फ़ाइल नहीं मिली" else "No log files to share", Toast.LENGTH_SHORT).show()
        return
    }
    try {
        val authority = "${context.packageName}.fileprovider"
        if (files.size == 1) {
            val file = files.first()
            val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, file.name)
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, if (isHindi) "लॉग फ़ाइल शेयर करें" else "Share Log File (${file.name})"))
        } else {
            val uris = ArrayList(files.map { file ->
                androidx.core.content.FileProvider.getUriForFile(context, authority, file)
            })
            val sendIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                putExtra(Intent.EXTRA_SUBJECT, "Madhu Mangal Log Files (${files.size})")
                type = "text/plain"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(sendIntent, if (isHindi) "${files.size} लॉग फ़ाइलें शेयर करें" else "Share ${files.size} Log Files"))
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Error sharing log files: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private data class LogSection(
    val header: LogEntry?,
    val logs: List<LogEntry>
)

@Composable
fun SessionDividerRow(
    entry: LogEntry,
    selectedFilter: String = "ALL",
    filteredLogs: List<LogEntry> = emptyList(),
    isHindi: Boolean = false
) {
    val context = LocalContext.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = entry.message.trim(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PrimaryBlue,
                modifier = Modifier.weight(1f)
            )

            // Small export button for this specific session file
            Button(
                onClick = {
                    val sessionName = when {
                        entry.message.contains("=== New Session Started: ") ->
                            entry.message.substringAfter("=== New Session Started: ").substringBefore(" ===").trim()
                        entry.message.contains("Active session file: ") ->
                            java.io.File(entry.message.substringAfter("Active session file: ").trim()).name
                        entry.message.contains("rolled over to new session: ") ->
                            entry.message.substringAfter("rolled over to new session: ").trim()
                        else -> "session.log"
                    }
                    val files = AppLogger.getSessionFiles(context)
                    val targetFile = files.find { it.name == sessionName }
                    val contentToShare = if (selectedFilter != "ALL") {
                        AppLogger.getFilteredLogsText(selectedFilter, filteredLogs)
                    } else if (targetFile != null && targetFile.exists()) {
                        targetFile.readText()
                    } else {
                        AppLogger.getAllLogsText()
                    }
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, contentToShare)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Export Session ($sessionName)")
                    context.startActivity(shareIntent)
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share Session",
                    tint = DarkBackground,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (selectedFilter != "ALL") (if (isHindi) "शेयर" else "Share") else (if (isHindi) "शेयर" else "Share"), color = DarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = entry.timestamp,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@Composable
fun LogItemRow(entry: LogEntry) {
    val levelColor = when (entry.level) {
        "ERROR" -> ActiveRed
        "WARN" -> Color(0xFFF59E0B) // Amber
        "DEBUG" -> AccentPurple
        else -> PrimaryBlue
    }

    val tagBgColor = when (entry.tag) {
        "STT" -> PrimaryBlue.copy(alpha = 0.2f)
        "AUDIO", "SERVICE" -> Color(0xFF10B981).copy(alpha = 0.2f) // Emerald
        "AI" -> AccentPurple.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Timestamp
        Text(
            text = entry.timestamp,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.width(72.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Level Badge
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = levelColor.copy(alpha = 0.2f)
        ) {
            Text(
                text = entry.level.take(1),
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = levelColor,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Tag Chip
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = tagBgColor
        ) {
            Text(
                text = entry.tag,
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Message
        Text(
            text = entry.message,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = if (entry.level == "ERROR") ActiveRed else TextPrimary,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
