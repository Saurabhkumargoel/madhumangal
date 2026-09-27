package com.echo.recorder.ui.screens

import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.util.AppLogger
import com.echo.recorder.util.LogEntry

@Composable
fun LogsTab(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val logs by AppLogger.logsFlow.collectAsState()
    var selectedFilter by remember { mutableStateOf("ALL") }
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedFilter) {
        when (selectedFilter) {
            "ALL" -> logs
            "ERROR" -> logs.filter { it.level == "ERROR" || it.level == "WARN" }
            "STT" -> logs.filter { it.tag == "STT" }
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

    Column(
        modifier = modifier
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
                    text = "System & AI Logs",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Real-time background audio, STT, and AI events",
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
                    text = "${logs.size} entries",
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
                        text = "📄 Active: ${sessionFile?.name ?: "session_active.log"}",
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
                            Toast.makeText(context, "Started new session log file!", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                    ) {
                        Text("➕ New Log", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            val activeFile = AppLogger.getCurrentSessionFile()
                            val textToExport = if (activeFile != null && activeFile.exists()) {
                                activeFile.readText()
                            } else {
                                AppLogger.getAllLogsText()
                            }
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, textToExport)
                                type = "text/plain"
                            }
                            val shareIntent = Intent.createChooser(sendIntent, "Export Active Session (${activeFile?.name ?: "active"})")
                            context.startActivity(shareIntent)
                        },
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Export", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action Buttons: Copy All, Share Log File, Global Export All, Clear
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Button(
                onClick = {
                    val allText = AppLogger.getAllLogsText()
                    clipboardManager.setText(AnnotatedString(allText))
                    Toast.makeText(context, "Current session logs copied (${logs.size})!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Copy", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val allText = AppLogger.getAllLogsText()
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, allText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Share Current Session Logs")
                    context.startActivity(shareIntent)
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Share", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    val allSessionsText = AppLogger.getAllSessionLogsText(context)
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        putExtra(Intent.EXTRA_TEXT, allSessionsText)
                        type = "text/plain"
                    }
                    val shareIntent = Intent.createChooser(sendIntent, "Global Export: All Session Logs")
                    context.startActivity(shareIntent)
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                modifier = Modifier.weight(1.3f)
            ) {
                Text("🌐 All Logs", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            OutlinedButton(
                onClick = {
                    AppLogger.clearLogs()
                    Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
            ) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = ActiveRed, modifier = Modifier.size(14.dp))
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
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedFilter = filterTag }
                ) {
                    Text(
                        text = filterTag,
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

        // Logs Feed Container
        Card(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            if (filteredLogs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (logs.isEmpty()) "No logs captured yet.\nStart recording or transcribing to see activity." else "No logs match '$selectedFilter' filter.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filteredLogs) { entry ->
                        val isSessionHeader = entry.message.contains("=== New Session Started:") ||
                                entry.message.contains("Active session file:") ||
                                entry.message.contains("rolled over to new session:")
                        if (isSessionHeader) {
                            SessionDividerRow(entry)
                        } else {
                            LogItemRow(entry)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDividerRow(entry: LogEntry) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.12f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f))
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
                    val contentToShare = if (targetFile != null && targetFile.exists()) {
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
                    contentDescription = "Export Session",
                    tint = DarkBackground,
                    modifier = Modifier.size(11.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export", color = DarkBackground, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
