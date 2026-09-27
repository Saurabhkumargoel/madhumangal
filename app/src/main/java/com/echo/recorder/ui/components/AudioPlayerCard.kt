package com.echo.recorder.ui.components

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.util.AppStorageManager
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AudioPlayerCard(
    transcription: TranscriptionWithAudio,
    isTranscribing: Boolean = false,
    isProcessingAi: Boolean = false,
    defaultSttModel: String = UserPreferences.STT_MODEL_VOSK_HINDI,
    defaultAiModel: String = UserPreferences.AI_MODEL_LOCAL_TINY,
    onDelete: (Long) -> Unit,
    onRetranscribe: (Long) -> Unit = {},
    onRetranscribeWithModel: (Long, String) -> Unit = { _, _ -> },
    onReprocessAiWithModel: (Long, String) -> Unit = { _, _ -> },
    onUpdateText: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isPlaying by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showSttModelMenu by remember { mutableStateOf(false) }
    var showAiModelMenu by remember { mutableStateOf(false) }

    // Initialized from Settings default; updates automatically when Settings default changes unless overridden!
    var selectedSttModel by remember(defaultSttModel) { mutableStateOf(defaultSttModel) }
    var selectedAiModel by remember(defaultAiModel) { mutableStateOf(defaultAiModel) }

    var showFileViewerDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf(transcription.text) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val file = remember(transcription.filePath) { File(transcription.filePath) }
    val audioExists = remember(file) { file.exists() }
    val fileName = remember(file) { file.name }

    val formattedTime = remember(transcription.timestampMillis) {
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(transcription.timestampMillis))
    }

    val isPlaceholder = (transcription.text.startsWith("[") && transcription.text.contains("chunk_")) ||
            transcription.text.startsWith("Voice speech recorded in") ||
            transcription.text.startsWith("Captured Hindi/Hinglish speech from") ||
            transcription.text.startsWith("Audio chunk recorded") ||
            transcription.text.startsWith("Audio slice ready") ||
            transcription.text.startsWith("[Pending:")
    val hasValidTranscript = transcription.text.isNotBlank() && !isPlaceholder

    fun togglePlay() {
        if (!audioExists) {
            Toast.makeText(context, "Audio file not found", Toast.LENGTH_SHORT).show()
            return
        }
        if (isPlaying) {
            mediaPlayer?.pause()
            isPlaying = false
        } else {
            try {
                if (mediaPlayer == null) {
                    mediaPlayer = MediaPlayer().apply {
                        setDataSource(transcription.filePath)
                        prepare()
                        setOnCompletionListener {
                            isPlaying = false
                        }
                    }
                }
                mediaPlayer?.start()
                isPlaying = true
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Playback error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    DisposableEffect(transcription.filePath) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Timestamp, Duration badge, Copy & Delete buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentPurple.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = "${transcription.durationSeconds}s (${transcription.overlapSeconds}s overlap)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPurple,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Copy Button (copies actual transcript text, shows feedback Toast)
                IconButton(
                    onClick = {
                        val textToCopy = if (isPlaceholder) {
                            "Audio Recording: $fileName (${transcription.durationSeconds}s)"
                        } else {
                            transcription.text
                        }
                        clipboardManager.setText(AnnotatedString(textToCopy))
                        Toast.makeText(context, "Copied transcription to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy Text",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Delete Button
                IconButton(
                    onClick = { onDelete(transcription.id) },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // File Tag Info & Path chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                ) {
                    Text(
                        text = "📁 $fileName",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Transcription Body
            if (hasValidTranscript) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "TRANSCRIPT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                onClick = {
                                    editTextValue = transcription.text
                                    showEditDialog = true
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Text",
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = transcription.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextPrimary,
                            lineHeight = 22.sp
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Audio slice captured (${transcription.durationSeconds}s)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Transcribe Audio' below or choose a model via 'Model ▾' to convert this chunk into Hindi/Hinglish text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Entire Playback Bar is Clickable Everywhere!
            if (audioExists) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isPlaying) PrimaryBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isPlaying) PrimaryBlue else CardBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { togglePlay() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isPlaying) PrimaryBlue else DarkBackground,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = if (isPlaying) DarkBackground else PrimaryBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPlaying) "Playing audio chunk..." else "Play audio chunk",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isPlaying) PrimaryBlue else TextPrimary
                            )
                            Text(
                                text = "Touch anywhere on this bar to ${if (isPlaying) "pause" else "play"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = null,
                            tint = if (isPlaying) PrimaryBlue else TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Spacer(modifier = Modifier.height(12.dp))

            val sttShortName = when (selectedSttModel) {
                UserPreferences.STT_MODEL_VOSK_HINDI -> "Vosk (Hi)"
                UserPreferences.STT_MODEL_VOSK_EN_IN -> "Vosk (En)"
                UserPreferences.STT_MODEL_CLOUD_GEMINI -> "Gemini"
                UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper (T)"
                UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper (B)"
                UserPreferences.STT_MODEL_INDIC_CONFORMER -> "Indic"
                UserPreferences.STT_MODEL_GOOGLE_SPEECH -> "Google"
                else -> "Vosk"
            }

            val aiShortName = when (selectedAiModel) {
                UserPreferences.AI_MODEL_LOCAL_TINY -> "Tiny AI"
                UserPreferences.AI_MODEL_CLOUD_GEMINI -> "Gemini"
                UserPreferences.AI_MODEL_LOCAL_GEMMA -> "Gemma 2B"
                UserPreferences.AI_MODEL_LOCAL_LLAMA -> "Llama 1B"
                UserPreferences.AI_MODEL_CLOUD_OPENAI -> "GPT-4o"
                else -> "Tiny AI"
            }

            // ── Action Buttons Row 1: STT Transcription ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main Transcribe button
                Button(
                    onClick = {
                        Toast.makeText(context, "Transcribing with $sttShortName...", Toast.LENGTH_SHORT).show()
                        onRetranscribeWithModel(transcription.id, selectedSttModel)
                    },
                    enabled = !isTranscribing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!hasValidTranscript) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.5.dp,
                            color = if (!hasValidTranscript) DarkBackground else PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = if (!hasValidTranscript) Icons.Default.AutoAwesome else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (!hasValidTranscript) DarkBackground else PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isTranscribing) "Transcribing..." else if (!hasValidTranscript) "Transcribe ($sttShortName)" else "Transcribe Again ($sttShortName)",
                        fontWeight = FontWeight.Bold,
                        color = if (!hasValidTranscript) DarkBackground else TextPrimary,
                        fontSize = 12.sp
                    )
                }

                // STT Model Selector Button with current short name
                Box {
                    OutlinedButton(
                        onClick = { showSttModelMenu = true },
                        enabled = !isTranscribing,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(sttShortName, color = PrimaryBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select STT Model",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showSttModelMenu,
                        onDismissRequest = { showSttModelMenu = false }
                    ) {
                        UserPreferences.AVAILABLE_STT_MODELS.forEach { modelOpt ->
                            val isDefault = modelOpt.id == defaultSttModel
                            val isSelected = modelOpt.id == selectedSttModel
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = modelOpt.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) PrimaryBlue else TextPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (isDefault) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PrimaryBlue.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Default ⚙️",
                                                        color = PrimaryBlue,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "✓ Selected",
                                                        color = Color(0xFF10B981),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedSttModel = modelOpt.id
                                    showSttModelMenu = false
                                    Toast.makeText(context, "Selected for chunk: ${modelOpt.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action Buttons Row 2: AI Notes & Task Extraction (Vibrant Purple) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Main AI Processing Button
                Button(
                    onClick = {
                        Toast.makeText(context, "Extracting notes & tasks via $aiShortName...", Toast.LENGTH_SHORT).show()
                        onReprocessAiWithModel(transcription.id, selectedAiModel)
                    },
                    enabled = !isProcessingAi && hasValidTranscript,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentPurple.copy(alpha = 0.2f),
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.6f)),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isProcessingAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.5.dp,
                            color = AccentPurple
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Text("🤖", fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isProcessingAi) "Extracting Tasks..." else "AI Notes ($aiShortName)",
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple,
                        fontSize = 12.sp
                    )
                }

                // AI Model Selector Button
                Box {
                    OutlinedButton(
                        onClick = { showAiModelMenu = true },
                        enabled = !isProcessingAi,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.6f))
                    ) {
                        Text(aiShortName, color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select AI Model",
                            tint = AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showAiModelMenu,
                        onDismissRequest = { showAiModelMenu = false }
                    ) {
                        UserPreferences.AVAILABLE_AI_MODELS.forEach { aiOpt ->
                            val isDefault = aiOpt.id == defaultAiModel
                            val isSelected = aiOpt.id == selectedAiModel
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f, fill = false)) {
                                            Text(
                                                text = aiOpt.name,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                color = if (isSelected) AccentPurple else TextPrimary,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            if (isDefault) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AccentPurple.copy(alpha = 0.15f)
                                                ) {
                                                    Text(
                                                        text = "Default ⚙️",
                                                        color = AccentPurple,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            if (isSelected) {
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFF10B981).copy(alpha = 0.2f)
                                                ) {
                                                    Text(
                                                        text = "✓ Selected",
                                                        color = Color(0xFF10B981),
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 10.sp,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                onClick = {
                                    selectedAiModel = aiOpt.id
                                    showAiModelMenu = false
                                    Toast.makeText(context, "Selected AI: ${aiOpt.name}", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons Row 2: "Open Transcription & Subtitles" file viewer & "Edit" button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // "Open Transcription" Button (opens versioned transcription & SRT file)
                OutlinedButton(
                    onClick = { showFileViewerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "View Notes & SRT Subtitles", color = AccentPurple, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Edit Button
                OutlinedButton(
                    onClick = {
                        editTextValue = transcription.text
                        showEditDialog = true
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Edit", color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }

    // "Open Transcription & Subtitles" File Viewer & Intent Dialog
    if (showFileViewerDialog) {
        var viewerTab by remember { mutableStateOf(0) } // 0 = Markdown Notes (.md), 1 = Subtitles (.srt)

        val latestMdFile = remember(fileName) {
            AppStorageManager.getLatestTranscriptionFile(context, fileName)
        }
        val latestSrtFile = remember(fileName) {
            AppStorageManager.getLatestSubtitleFile(context, fileName)
        }

        val mdContent = remember(latestMdFile, transcription.text) {
            if (latestMdFile != null && latestMdFile.exists()) {
                latestMdFile.readText()
            } else {
                "Transcription file (.md):\nDownload/App data/poc/yesboss/transcriptions/transcription_${fileName.substringBeforeLast(".")}.md\n\nTranscript Content:\n${transcription.text}"
            }
        }

        val srtContent = remember(latestSrtFile, transcription.text) {
            if (latestSrtFile != null && latestSrtFile.exists()) {
                latestSrtFile.readText()
            } else {
                val generated = AppStorageManager.generateSrtContent(transcription.text, transcription.durationSeconds)
                if (generated.isNotBlank()) generated else "1\n00:00:00,000 --> 00:00:${String.format(Locale.US, "%02d", transcription.durationSeconds)},000\n${transcription.text}\n"
            }
        }

        val currentActiveFile = if (viewerTab == 0) latestMdFile else latestSrtFile
        val currentContent = if (viewerTab == 0) mdContent else srtContent

        AlertDialog(
            onDismissRequest = { showFileViewerDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Description, contentDescription = null, tint = AccentPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (viewerTab == 0) "Transcription Notes (.md)" else "Subtitle Timestamps (.srt)", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Tab switch between Markdown and Subtitles SRT
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewerTab == 0) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewerTab = 0 }
                        ) {
                            Text(
                                text = "📝 Notes (.md)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (viewerTab == 0) DarkBackground else TextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (viewerTab == 1) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewerTab = 1 }
                        ) {
                            Text(
                                text = "⏱️ Subtitles (.srt)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (viewerTab == 1) DarkBackground else TextPrimary,
                                modifier = Modifier.padding(vertical = 8.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "File Path: ${currentActiveFile?.absolutePath ?: "Download/App data/poc/yesboss/transcriptions/"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = currentContent,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                if (currentActiveFile != null && currentActiveFile.exists()) {
                    Button(
                        onClick = {
                            val opened = AppStorageManager.openFileWithIntent(context, currentActiveFile)
                            if (!opened) {
                                Toast.makeText(context, "Copied content to clipboard.", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString(currentContent))
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open File in App", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(currentContent))
                            Toast.makeText(context, "Copied content to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Copy Text", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showFileViewerDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            }
        )
    }

    // Edit Transcription Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(text = "Edit Transcription", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "Correct or add words to this audio slice:",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editTextValue,
                        onValueChange = { editTextValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 6
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateText(transcription.id, editTextValue)
                        showEditDialog = false
                        Toast.makeText(context, "Transcription updated", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}
