package com.echo.recorder.ui.components

import android.media.MediaPlayer
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material3.AlertDialog
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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.NoteTask
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
    tasks: List<NoteTask> = emptyList(),
    isTranscribing: Boolean = false,
    isProcessingAi: Boolean = false,
    defaultSttModel: String = UserPreferences.STT_MODEL_VOSK_HINDI,
    defaultAiModel: String = UserPreferences.AI_MODEL_LOCAL_TINY,
    onDelete: (Long) -> Unit,
    onRetranscribe: (Long) -> Unit = {},
    onRetranscribeWithModel: (Long, String) -> Unit = { _, _ -> },
    onReprocessAiWithModel: (Long, String) -> Unit = { _, _ -> },
    onUpdateText: (Long, String) -> Unit = { _, _ -> },
    onToggleTaskCompletion: (Long, Boolean) -> Unit = { _, _ -> },
    onUpdateTaskDetails: (Long, String, String, String, Int) -> Unit = { _, _, _, _, _ -> },
    onDeleteTask: (Long) -> Unit = {},
    onRunFlow: (Long) -> Unit = {},
    isHindi: Boolean = false,
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
    var showTasksDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<NoteTask?>(null) }
    var editTextValue by remember { mutableStateOf(transcription.text) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var totalDurationMs by remember { mutableIntStateOf((transcription.durationSeconds * 1000).toInt().coerceAtLeast(1000)) }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val mp = mediaPlayer
            if (mp != null) {
                try {
                    currentPositionMs = mp.currentPosition
                    if (mp.duration > 0) totalDurationMs = mp.duration
                } catch (_: Exception) {}
            }
            kotlinx.coroutines.delay(100)
        }
    }

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
                            currentPositionMs = 0
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

            // Visual Overlap Buffer Indicator
            if (transcription.overlapSeconds > 0) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentPurple.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("↺", color = AccentPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isHindi) "ओवरलैप सीमा: ~${transcription.overlapSeconds} से. पिछले टुकड़े से"
                                   else "Overlap Boundary: ~${transcription.overlapSeconds}s carried over from previous slice",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPurple,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

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
                                text = if (isHindi) "प्रतिलेख" else "TRANSCRIPT",
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
                            text = if (isHindi) "ऑडियो टुकड़ा रिकॉर्ड हुआ (${transcription.durationSeconds} से.)"
                                   else "Audio slice captured (${transcription.durationSeconds}s)",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "इस टुकड़े को हिंदी/हिंग्लिश टेक्स्ट में बदलने के लिए नीचे 'ट्रांसक्राइब' दबाएं या 'मॉडल ▾' चुनें।"
                                   else "Tap 'Transcribe Audio' below or choose a model via 'Model ▾' to convert this chunk into Hindi/Hinglish text.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Rich Media Player: Sound Mountains Waveform, Seekbar, -5s, Play/Pause, +5s
            if (audioExists) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (isPlaying) PrimaryBlue.copy(alpha = 0.8f) else CardBorder
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        // 1. Sound Mountains (Waveform Visualizer)
                        val progressRatio = (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        val samplePeaks = remember(transcription.id, transcription.filePath) {
                            val seed = (transcription.id * 31 + transcription.filePath.hashCode()).toLong()
                            val rnd = java.util.Random(seed)
                            FloatArray(36) { (0.20f + rnd.nextFloat() * 0.80f) }
                        }

                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(26.dp)
                                .pointerInput(totalDurationMs) {
                                    detectTapGestures { offset ->
                                        val tapRatio = (offset.x / size.width).coerceIn(0f, 1f)
                                        val targetMs = (tapRatio * totalDurationMs).toInt()
                                        mediaPlayer?.seekTo(targetMs)
                                        currentPositionMs = targetMs
                                    }
                                }
                        ) {
                            val barCount = samplePeaks.size
                            val barWidth = (size.width / (barCount * 1.5f)).coerceAtLeast(3f)
                            val spacing = barWidth * 0.5f
                            val totalBarsWidth = barCount * (barWidth + spacing)
                            val startX = (size.width - totalBarsWidth) / 2f

                            for (i in 0 until barCount) {
                                val x = startX + i * (barWidth + spacing)
                                val peak = samplePeaks[i]
                                val barHeight = (size.height * peak).coerceAtLeast(4f)
                                val y = (size.height - barHeight) / 2f
                                val isPlayed = (x / size.width) <= progressRatio
                                drawRoundRect(
                                    color = if (isPlayed) PrimaryBlue else TextMuted.copy(alpha = 0.35f),
                                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f)
                                )
                            }
                            // Active playhead indicator line
                            val headX = size.width * progressRatio
                            drawLine(
                                color = PrimaryBlue,
                                start = androidx.compose.ui.geometry.Offset(headX, 0f),
                                end = androidx.compose.ui.geometry.Offset(headX, size.height),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // 2. Interactive Seekbar slider & Time indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val curSec = currentPositionMs / 1000
                            val totSec = totalDurationMs / 1000
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", curSec / 60, curSec % 60),
                                fontSize = 10.sp,
                                color = if (isPlaying) PrimaryBlue else TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                            Slider(
                                value = currentPositionMs.toFloat().coerceIn(0f, totalDurationMs.toFloat()),
                                onValueChange = { newPos ->
                                    currentPositionMs = newPos.toInt()
                                    mediaPlayer?.seekTo(newPos.toInt())
                                },
                                valueRange = 0f..totalDurationMs.toFloat(),
                                modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 6.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryBlue,
                                    activeTrackColor = PrimaryBlue,
                                    inactiveTrackColor = CardBorder
                                )
                            )
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", totSec / 60, totSec % 60),
                                fontSize = 10.sp,
                                color = TextMuted,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        // 3. Media Controls: -5s, Play/Pause, +5s
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // -5s Seek Button
                            IconButton(
                                onClick = {
                                    val target = ((mediaPlayer?.currentPosition ?: currentPositionMs) - 5000).coerceAtLeast(0)
                                    mediaPlayer?.seekTo(target)
                                    currentPositionMs = target
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay5,
                                    contentDescription = "-5s",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // Play / Pause Circle Button
                            Surface(
                                shape = CircleShape,
                                color = PrimaryBlue,
                                modifier = Modifier.size(38.dp).clickable { togglePlay() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Pause" else "Play",
                                        tint = DarkBackground,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            // +5s Seek Button
                            IconButton(
                                onClick = {
                                    val target = ((mediaPlayer?.currentPosition ?: currentPositionMs) + 5000).coerceAtMost(totalDurationMs)
                                    mediaPlayer?.seekTo(target)
                                    currentPositionMs = target
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Forward5,
                                    contentDescription = "+5s",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            val sttShortName = when (selectedSttModel) {
                UserPreferences.STT_MODEL_VOSK_HINDI -> "Vosk (Hi)"
                UserPreferences.STT_MODEL_VOSK_HINDI_LARGE -> "Vosk (L)"
                UserPreferences.STT_MODEL_SHERPA_ONNX_HINDI -> "Sherpa (Zip)"
                UserPreferences.STT_MODEL_VOSK_EN_IN -> "Vosk (En)"
                UserPreferences.STT_MODEL_CLOUD_GEMINI -> "Gemini"
                UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper (B)"
                UserPreferences.STT_MODEL_WHISPER_LARGE -> "Whisper (L)"
                UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper (T)"
                UserPreferences.STT_MODEL_INDIC_CONFORMER -> "Indic"
                UserPreferences.STT_MODEL_GOOGLE_SPEECH -> "Google"
                else -> "Vosk"
            }

            val aiShortName = when (selectedAiModel) {
                UserPreferences.AI_MODEL_LOCAL_TINY -> "Tiny AI"
                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_1B -> "Gemma 3 1B"
                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_4B -> "Gemma 3 4B"
                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_12B -> "Gemma 3 12B"
                UserPreferences.AI_MODEL_CLOUD_GEMINI -> "Gemini"
                UserPreferences.AI_MODEL_LOCAL_GEMMA -> "Gemma 2B"
                UserPreferences.AI_MODEL_LOCAL_LLAMA -> "Llama 1B"
                UserPreferences.AI_MODEL_CLOUD_OPENAI -> "GPT-4o"
                else -> "AI"
            }

            // ── Primary Action: Run Complete Flow (STT + AI Note/Task Extraction) ──
            Button(
                onClick = {
                    Toast.makeText(context, "Running complete flow for this chunk...", Toast.LENGTH_SHORT).show()
                    onRunFlow(transcription.id)
                },
                enabled = !isTranscribing && !isProcessingAi,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isTranscribing || isProcessingAi) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.5.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTranscribing) (if (isHindi) "ऑडियो ट्रांसक्राइब हो रहा है..." else "Transcribing Audio...")
                               else (if (isHindi) "कार्य निकाले जा रहे हैं..." else "Extracting Hierarchical Tasks..."),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                } else {
                    Text("⚡", fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasValidTranscript) (if (isHindi) "फ्लो चलाएं (पुनः प्रतिलेखन व कार्य)" else "Run Flow (Re-Transcribe & Extract Tasks)")
                               else (if (isHindi) "फ्लो चलाएं (प्रतिलेखन व कार्य)" else "Run Flow (Transcribe & Extract Tasks)"),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action Buttons Row 1: STT Transcription & Notes ──────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = if (!hasValidTranscript) DarkBackground else PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Icon(
                            imageVector = if (!hasValidTranscript) Icons.Default.AutoAwesome else Icons.Default.Refresh,
                            contentDescription = null,
                            tint = if (!hasValidTranscript) DarkBackground else PrimaryBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isTranscribing) (if (isHindi) "प्रतिलेखन जारी..." else "Transcribing...")
                               else if (!hasValidTranscript) (if (isHindi) "ट्रांसक्राइब" else "Transcribe")
                               else (if (isHindi) "पुनः ट्रांसक्राइब" else "Transcribe"),
                        fontWeight = FontWeight.Bold,
                        color = if (!hasValidTranscript) DarkBackground else TextPrimary,
                        fontSize = 11.sp
                    )
                }

                // STT Model Selector Button with current short name
                Box {
                    OutlinedButton(
                        onClick = { showSttModelMenu = true },
                        enabled = !isTranscribing,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(sttShortName, color = PrimaryBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select STT Model",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(14.dp)
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
                                    val isInstalled = when (modelOpt.id) {
                                        UserPreferences.STT_MODEL_GOOGLE_SPEECH -> true
                                        UserPreferences.STT_MODEL_VOSK_HINDI -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                                        UserPreferences.STT_MODEL_VOSK_HINDI_LARGE -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE)
                                        UserPreferences.STT_MODEL_VOSK_EN_IN -> com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                                        UserPreferences.STT_MODEL_SHERPA_ONNX_HINDI -> File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "sherpa-onnx-hindi").exists()
                                        else -> true
                                    }
                                    if (isInstalled) {
                                        selectedSttModel = modelOpt.id
                                        showSttModelMenu = false
                                        Toast.makeText(context, "Selected for chunk: ${modelOpt.name}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        showSttModelMenu = false
                                        Toast.makeText(context, "⚠️ ${modelOpt.name} is not installed! Download it from Settings first.", Toast.LENGTH_LONG).show()
                                    }
                                }
                            )
                        }
                    }
                }

                // Compact "📝 Notes" button moved into STT Row
                OutlinedButton(
                    onClick = { showFileViewerDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Notes",
                        color = PrimaryBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ── Action Buttons Row 2: AI Notes & Tasks (Vibrant Purple) ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1.3f)
                ) {
                    if (isProcessingAi) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                            color = AccentPurple
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    } else {
                        Text("🤖", fontSize = 13.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = if (isProcessingAi) (if (isHindi) "कार्य निकाले जा रहे हैं..." else "Extracting...")
                               else (if (isHindi) "एआई विश्लेषण" else "AI Notes"),
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple,
                        fontSize = 11.sp
                    )
                }

                // AI Model Selector Button
                Box {
                    OutlinedButton(
                        onClick = { showAiModelMenu = true },
                        enabled = !isProcessingAi,
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.6f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(aiShortName, color = AccentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Select AI Model",
                            tint = AccentPurple,
                            modifier = Modifier.size(14.dp)
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

                // Compact "📋 Tasks" button moved into AI Row
                OutlinedButton(
                    onClick = { showTasksDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (tasks.isNotEmpty()) Color(0xFF10B981) else TextMuted.copy(alpha = 0.5f)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("📋", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "${tasks.size}",
                        color = if (tasks.isNotEmpty()) Color(0xFF10B981) else TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
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

                    if (transcription.overlapSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = AccentPurple.copy(alpha = 0.15f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentPurple.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        ) {
                            Text(
                                text = "↺ Overlap Window: First ~${transcription.overlapSeconds}s contains continuous speech carried over from the preceding slice.",
                                style = MaterialTheme.typography.labelSmall,
                                color = AccentPurple,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }

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

    // Fetched Tasks Dialog for this Audio Chunk
    if (showTasksDialog) {
        AlertDialog(
            onDismissRequest = { showTasksDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("📋", fontSize = 20.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tasks from Audio (${tasks.size})",
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Audio Segment Info Card
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("🎙️", fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = fileName,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Duration: ${transcription.durationSeconds}s • Recorded: $formattedTime",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                                if (transcription.overlapSeconds > 0) {
                                    Text(
                                        text = "↺ Overlap: ~${transcription.overlapSeconds}s boundary buffer",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentPurple
                                    )
                                }
                            }
                        }
                    }

                    if (tasks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "No tasks extracted yet",
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Tap 'AI Notes' on this card to automatically extract actionable tasks, shopping items, and ideas.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        tasks.forEach { task ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = task.isCompleted,
                                        onCheckedChange = { onToggleTaskCompletion(task.id, it) },
                                        colors = CheckboxDefaults.colors(
                                            checkedColor = Color(0xFF10B981),
                                            uncheckedColor = TextMuted
                                        )
                                    )

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 4.dp)
                                    ) {
                                        Text(
                                            text = task.title,
                                            fontWeight = FontWeight.SemiBold,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = if (task.isCompleted) TextMuted else TextPrimary,
                                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null
                                        )
                                        if (task.description.isNotBlank()) {
                                            Text(
                                                text = task.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = TextSecondary,
                                                maxLines = 2
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = PrimaryBlue.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = task.categoryGroup,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryBlue,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = "⏱️ In chunk (0-${transcription.durationSeconds}s)",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }

                                    // Pencil Button to edit task
                                    IconButton(
                                        onClick = { editingTask = task },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Edit Task",
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    // Delete Button
                                    IconButton(
                                        onClick = { onDeleteTask(task.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Task",
                                            tint = TextMuted,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showTasksDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Done", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Edit Task Dialog
    if (editingTask != null) {
        val taskToEdit = editingTask!!
        var editTitle by remember(taskToEdit.id) { mutableStateOf(taskToEdit.title) }
        var editDesc by remember(taskToEdit.id) { mutableStateOf(taskToEdit.description) }
        var editCat by remember(taskToEdit.id) { mutableStateOf(taskToEdit.categoryGroup) }
        var editPriority by remember(taskToEdit.id) { mutableStateOf(taskToEdit.priority) }

        AlertDialog(
            onDismissRequest = { editingTask = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Fetched Task", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Task Title",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Text(
                        text = "Details / Description",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 4
                    )

                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    val categories = listOf("Shopping", "Work", "Personal", "Urgent", "Ideas", "General")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.take(3).forEach { cat ->
                            val isSel = editCat == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editCat = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) DarkBackground else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        categories.drop(3).forEach { cat ->
                            val isSel = editCat == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editCat = cat }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) DarkBackground else TextPrimary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }

                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Triple(NoteTask.PRIORITY_LOW, "🟢 Low", Color(0xFF10B981)),
                            Triple(NoteTask.PRIORITY_MEDIUM, "🟡 Medium", Color(0xFFF59E0B)),
                            Triple(NoteTask.PRIORITY_HIGH, "🔴 High", ActiveRed)
                        ).forEach { (prioVal, label, color) ->
                            val isSel = editPriority == prioVal
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSel) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSel) androidx.compose.foundation.BorderStroke(1.dp, color) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { editPriority = prioVal }
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) color else TextSecondary,
                                    modifier = Modifier.padding(vertical = 6.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editTitle.isNotBlank()) {
                            onUpdateTaskDetails(taskToEdit.id, editTitle.trim(), editDesc.trim(), editCat, editPriority)
                        }
                        editingTask = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save Changes", color = DarkBackground, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingTask = null }) {
                    Text("Cancel", color = TextMuted)
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
