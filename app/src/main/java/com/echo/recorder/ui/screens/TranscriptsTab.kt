package com.echo.recorder.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.NoteTask
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.components.AudioPlayerCard
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import com.echo.recorder.ui.theme.ActiveRed

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TranscriptsTab(
    transcriptions: List<TranscriptionWithAudio>,
    allTasks: List<NoteTask> = emptyList(),
    transcribingIds: Set<Long> = emptySet(),
    processingAiIds: Set<Long> = emptySet(),
    defaultSttModel: String = UserPreferences.STT_MODEL_VOSK_HINDI,
    defaultAiModel: String = UserPreferences.AI_MODEL_LOCAL_TINY,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDeleteTranscription: (Long) -> Unit,
    onRetranscribe: (Long) -> Unit = {},
    onRetranscribeWithModel: (Long, String) -> Unit = { _, _ -> },
    onReprocessAiWithModel: (Long, String) -> Unit = { _, _ -> },
    onUpdateText: (Long, String) -> Unit = { _, _ -> },
    onToggleTaskCompletion: (Long, Boolean) -> Unit = { _, _ -> },
    onUpdateTaskDetails: (Long, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    onMergeSession: (String) -> Unit = {},
    onExportMedia: () -> Unit = {},
    onSyncMedia: () -> Unit = {},
    onRunFlow: (sessionId: String?, chunkId: Long?) -> Unit = { _, _ -> },
    isRecording: Boolean = false,
    elapsedSeconds: Long = 0L,
    onToggleRecording: () -> Unit = {},
    onAbortRecording: () -> Unit = {},
    recorderLayoutMode: String = "BOTH_TABS",
    inverseRecordingsSort: Boolean = false,
    isHindi: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Group transcriptions by attempt/session ID (PID), sorted by inverseRecordingsSort
    val orderedTranscriptions = remember(transcriptions, inverseRecordingsSort) {
        if (inverseRecordingsSort) {
            transcriptions.sortedBy { it.timestampMillis }
        } else {
            transcriptions.sortedByDescending { it.timestampMillis }
        }
    }

    val groupedSessions = remember(orderedTranscriptions) {
        orderedTranscriptions.groupBy { item ->
            if (item.sessionId.isNotBlank()) item.sessionId
            else {
                val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date(item.timestampMillis))
                "session_legacy_$dateTag"
            }
        }.toList()
    }

    // State for collapsible sessions: default all expanded
    var expandedSessions by remember(groupedSessions) {
        mutableStateOf(groupedSessions.map { it.first }.toSet())
    }
    val allExpanded = expandedSessions.size == groupedSessions.size && groupedSessions.isNotEmpty()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Top Header Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isHindi) "रिकॉर्डिंग और मीडिया" else "Recordings & Media",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isHindi) "सभी ऑडियो टुकड़े, वाक् प्रतिलेखन और सहेजी गई फ़ाइलें" else "All audio chunks, speech transcriptions & saved markdown files",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onSyncMedia,
                    modifier = Modifier
                        .size(42.dp)
                        .background(AccentPurple.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Scan & Sync Files",
                        tint = AccentPurple,
                        modifier = Modifier.size(22.dp)
                    )
                }

                IconButton(
                    onClick = onExportMedia,
                    modifier = Modifier
                        .size(42.dp)
                        .background(PrimaryBlue.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.DriveFileMove,
                        contentDescription = "Export All Media",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(if (isHindi) "प्रतिलेखन खोजें..." else "Search transcriptions...", color = TextMuted) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = PrimaryBlue)
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedBorderColor = PrimaryBlue,
                unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Chronological Flow: Transcribe All & Process Tasks in Date Order
        if (transcriptions.isNotEmpty()) {
            Button(
                onClick = { onRunFlow(null, null) },
                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("⚡", fontSize = 15.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "सभी को ट्रांसक्राइब और प्रोसेस करें (कालक्रम प्रवाह)" else "Transcribe & Process All (Chronological Flow)",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Session Group Count & Global Expand/Collapse Toggle
        if (groupedSessions.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isHindi) "📁 ${groupedSessions.size} सत्र • ${transcriptions.size} ऑडियो टुकड़े"
                           else "📁 ${groupedSessions.size} Sessions • ${transcriptions.size} Audio Chunks",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )

                TextButton(
                    onClick = {
                        expandedSessions = if (allExpanded) {
                            emptySet()
                        } else {
                            groupedSessions.map { it.first }.toSet()
                        }
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (allExpanded) (if (isHindi) "सभी समेटें 🔼" else "Collapse All 🔼")
                               else (if (isHindi) "सभी खोलें 🔽" else "Expand All 🔽"),
                        color = PrimaryBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (transcriptions.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) (if (isHindi) "कोई रिकॉर्डिंग या प्रतिलेखन दिखाई नहीं दे रहा" else "No recordings or transcripts visible")
                               else (if (isHindi) "कोई मेल खाता प्रतिलेखन नहीं मिला" else "No matching transcriptions found"),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) (if (isHindi) "यदि आपके डिवाइस में पहले से ऑडियो या ट्रांसक्रिप्ट फाइलें हैं, तो उन्हें ऐप में रीस्टोर करने के लिए नीचे टैप करें।" else "If audio or transcript files are already stored on your device, tap below to restore them into the app.")
                               else (if (isHindi) "किसी अन्य कीवर्ड से खोजने का प्रयास करें।" else "Try searching for a different keyword."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextMuted,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    if (searchQuery.isBlank()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(
                            onClick = onSyncMedia,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isHindi) "🔄 स्टोरेज से फ़ाइलें स्कैन व रीस्टोर करें" else "🔄 Scan & Restore Files from Storage", color = PrimaryBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 12.dp),
                reverseLayout = inverseRecordingsSort
            ) {
                groupedSessions.forEach { (sessionId, sessionChunks) ->
                    val isExpanded = expandedSessions.contains(sessionId)
                    val totalDuration = sessionChunks.sumOf { it.durationSeconds }
                    val hasOverlap = sessionChunks.any { it.overlapSeconds > 0 }
                    val sessionTitle = formatSessionTitle(sessionId, sessionChunks.firstOrNull()?.timestampMillis ?: System.currentTimeMillis())

                    stickyHeader(key = "header_$sessionId") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorder),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        expandedSessions = if (isExpanded) {
                                            expandedSessions - sessionId
                                        } else {
                                            expandedSessions + sessionId
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎙️", fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sessionTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isHindi) "${sessionChunks.size} टुकड़े • कुल ${totalDuration} से." else "${sessionChunks.size} chunks • ${totalDuration}s total",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = TextMuted
                                        )
                                        if (hasOverlap) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AccentPurple.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = if (isHindi) "↺ ओवरलैप्ड" else "↺ Overlapped",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = AccentPurple,
                                                    fontSize = 10.sp,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }

                                // Run Attempt Flow Button
                                OutlinedButton(
                                    onClick = { onRunFlow(sessionId, null) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.8f)),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.padding(end = 4.dp)
                                ) {
                                    Text(
                                        text = if (isHindi) "⚡ फ्लो चलाएं" else "⚡ Run Flow",
                                        color = AccentPurple,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }

                                // Merge Attempt Button
                                if (sessionChunks.size > 1) {
                                    OutlinedButton(
                                        onClick = { onMergeSession(sessionId) },
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.5f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.padding(end = 4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isHindi) "जोड़ें" else "Merge",
                                            color = PrimaryBlue,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }

                                // Expand / Collapse chevron
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    if (isExpanded) {
                        items(
                            items = sessionChunks,
                            key = { "chunk_${it.id}" }
                        ) { chunk ->
                            val chunkTasks = remember(allTasks, chunk.id) {
                                allTasks.filter { it.transcriptionId == chunk.id }
                            }
                            AudioPlayerCard(
                                transcription = chunk,
                                tasks = chunkTasks,
                                isTranscribing = transcribingIds.contains(chunk.id),
                                isProcessingAi = processingAiIds.contains(chunk.id),
                                defaultSttModel = defaultSttModel,
                                defaultAiModel = defaultAiModel,
                                onDelete = onDeleteTranscription,
                                onRetranscribe = onRetranscribe,
                                onRetranscribeWithModel = onRetranscribeWithModel,
                                onReprocessAiWithModel = onReprocessAiWithModel,
                                onUpdateText = onUpdateText,
                                onToggleTaskCompletion = onToggleTaskCompletion,
                                onUpdateTaskDetails = onUpdateTaskDetails,
                                onDeleteTask = onDeleteTask,
                                onRunFlow = { id -> onRunFlow(sessionId, id) },
                                isHindi = isHindi,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }

        // ── Compact Static Recorder at Bottom of Media Tab ──────────────────
        if (recorderLayoutMode != "DEDICATED_TAB_ONLY") {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, if (isRecording) ActiveRed else CardBorder),
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: Mic status & duration timer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (isRecording) ActiveRed.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = if (isRecording) ActiveRed else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            val timerMinutes = elapsedSeconds / 60
                            val timerSeconds = elapsedSeconds % 60
                            val timerStr = String.format(Locale.US, "%02d:%02d", timerMinutes, timerSeconds)
                            Text(
                                text = if (isRecording) "🔴 REC: $timerStr" else (if (isHindi) "मीडिया रिकॉर्डर (तैयार)" else "Media Recorder (Ready)"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isRecording) ActiveRed else TextPrimary
                            )
                            Text(
                                text = if (isRecording) (if (isHindi) "निरंतर ऑडियो कैप्चर जारी है..." else "Continuous audio recording active...") else (if (isHindi) "रिकॉर्ड शुरू करने के लिए टैप करें" else "Tap record to start capturing"),
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    // Right: Control buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isRecording) {
                            // Abort button
                            OutlinedButton(
                                onClick = onAbortRecording,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ActiveRed),
                                border = BorderStroke(1.dp, ActiveRed),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text(if (isHindi) "🚫 निरस्त" else "🚫 Abort", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Button(
                            onClick = onToggleRecording,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (isRecording) ActiveRed else PrimaryBlue),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            modifier = Modifier.height(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                tint = if (isRecording) TextPrimary else DarkBackground,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isRecording) (if (isHindi) "रोकें" else "Stop") else (if (isHindi) "रिकॉर्ड" else "Record"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isRecording) TextPrimary else DarkBackground
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatSessionTitle(sessionId: String, fallbackTimeMillis: Long): String {
    return if (sessionId.startsWith("session_")) {
        val raw = sessionId.removePrefix("session_")
        try {
            val parser = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val date = parser.parse(raw)
            if (date != null) {
                "Attempt: " + SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(date)
            } else {
                "Attempt: $raw"
            }
        } catch (_: Exception) {
            "Attempt: $raw"
        }
    } else if (sessionId.startsWith("imported_")) {
        "Imported Audio File"
    } else if (sessionId.isNotBlank()) {
        "Attempt: $sessionId"
    } else {
        "Recording Attempt: " + SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(fallbackTimeMillis))
    }
}
