package com.echo.recorder.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.zIndex
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.components.AudioWaveformVisualizer
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import com.echo.recorder.ui.viewmodel.MainViewModel
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.echo.recorder.ui.components.AbortSessionDialog
import com.echo.recorder.data.AudioSegment

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecorderTab(
    viewModel: MainViewModel,
    isServiceRunning: Boolean,
    currentAmplitude: Float,
    currentDb: Int,
    elapsedSeconds: Long,
    liveTranscript: String,
    createdSegmentsCount: Int,
    isSpeechDetected: Boolean,
    statusMessage: String,
    userPreferences: UserPreferences,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var validationError by remember { mutableStateOf<com.echo.recorder.data.ModelValidationResult?>(null) }
    var showAbortDialog by remember { mutableStateOf(false) }
    var abortSegments by remember { mutableStateOf<List<AudioSegment>>(emptyList()) }

    fun triggerAbortDialog() {
        scope.launch {
            abortSegments = viewModel.getCurrentSessionSegments()
            showAbortDialog = true
        }
    }

    if (showAbortDialog) {
        AbortSessionDialog(
            segments = abortSegments,
            defaultSelectionMode = userPreferences.defaultAbortSelection,
            onConfirmAbort = { idsToDelete ->
                showAbortDialog = false
                viewModel.abortRecordingSession(context, idsToDelete) { count ->
                    Toast.makeText(
                        context,
                        if (userPreferences.appLanguage == "hi") "सत्र निरस्त किया गया। $count फ़ाइलें हटाई गईं।" else "Session aborted. $count audio chunks deleted.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showAbortDialog = false },
            isHindi = userPreferences.appLanguage == "hi"
        )
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    var showMissingModelDialog by remember { mutableStateOf(false) }

    val isVoskInstalled = remember(userPreferences.downloadedModels) {
        com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME) ||
        com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN) ||
        com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE)
    }

    val modelDownloadStates by viewModel.modelDownloadStates.collectAsState()
    val voskDownload = modelDownloadStates[com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI]

    val onStartRecordingAttempt = {
        if (!isVoskInstalled) {
            showMissingModelDialog = true
        } else {
            val validation = viewModel.validateCurrentModel(context)
            if (validation is com.echo.recorder.data.ModelValidationResult.Valid) {
                onRequestPermission()
                viewModel.startRecordingService(context)
            } else {
                validationError = validation
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isServiceRunning) 1.22f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val formattedTimer = remember(elapsedSeconds) {
        val hours = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    val isHindi = userPreferences.appLanguage == "hi"

    val qualityLabel = when (userPreferences.sampleRate) {
        UserPreferences.QUALITY_TINIEST -> if (isHindi) "8 kHz (न्यूनतम आकार)" else "8 kHz (Tiniest Quality)"
        UserPreferences.QUALITY_SPEECH -> if (isHindi) "16 kHz (ध्वनि)" else "16 kHz (Speech)"
        UserPreferences.QUALITY_STANDARD -> "22.05 kHz"
        else -> if (isHindi) "44.1 kHz (एचडी)" else "44.1 kHz (HD)"
    }

    val activeModelName = when (userPreferences.selectedSttModel) {
        UserPreferences.STT_MODEL_VOSK_HINDI -> "Vosk Hindi"
        UserPreferences.STT_MODEL_VOSK_HINDI_LARGE -> "Vosk Hindi (Large)"
        UserPreferences.STT_MODEL_VOSK_EN_IN -> "Vosk Indian English"
        UserPreferences.STT_MODEL_SHERPA_ONNX_HINDI -> "Sherpa Hindi (Zipformer)"
        UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper Base"
        UserPreferences.STT_MODEL_WHISPER_LARGE -> "Whisper Large-v3"
        UserPreferences.STT_MODEL_CLOUD_GEMINI -> "Gemini 1.5 Flash"
        UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper Tiny"
        UserPreferences.STT_MODEL_INDIC_CONFORMER -> "IndicConformer"
        else -> "Google Speech"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header & Live Status Badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GoodEcho",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isHindi) "$qualityLabel • ${userPreferences.bufferLengthSeconds} से. टुकड़े" else "$qualityLabel • ${userPreferences.bufferLengthSeconds}s Chunks",
                    style = MaterialTheme.typography.labelSmall,
                    color = PrimaryBlue
                )
            }

            // Unmistakable Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isServiceRunning) ActiveRed.copy(alpha = 0.25f) else SurfaceDark,
                border = androidx.compose.foundation.BorderStroke(
                    width = 1.5.dp,
                    color = if (isServiceRunning) ActiveRed else CardBorder
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FiberManualRecord,
                        contentDescription = null,
                        tint = if (isServiceRunning) ActiveRed else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isServiceRunning) (if (isHindi) "लाइव रिकॉर्डिंग" else "RECORDING LIVE") else (if (isHindi) "तैयार" else "STANDBY"),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) ActiveRed else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── ⚡ Prominent Vosk Offline Model Setup & Download Progress Card ──
        if (!isVoskInstalled) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = AccentPurple.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, AccentPurple.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (voskDownload?.status == com.echo.recorder.ui.viewmodel.ModelDownloadStatus.DOWNLOADING ||
                                           voskDownload?.status == com.echo.recorder.ui.viewmodel.ModelDownloadStatus.EXTRACTING) {
                                    if (isHindi) "ऑफ़लाइन मॉडल डाउनलोड हो रहा है..." else "Downloading Speech Model..."
                                } else {
                                    if (isHindi) "⚡ ऑफ़लाइन वाक् मॉडल आवश्यक (45 MB)" else "⚡ Offline Speech Model Required (45 MB)"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary
                            )
                        }

                        if (voskDownload?.status != com.echo.recorder.ui.viewmodel.ModelDownloadStatus.DOWNLOADING &&
                            voskDownload?.status != com.echo.recorder.ui.viewmodel.ModelDownloadStatus.EXTRACTING) {
                            Button(
                                onClick = { viewModel.downloadModel(com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isHindi) "डाउनलोड (45 MB)" else "Download (45 MB)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (voskDownload?.status == com.echo.recorder.ui.viewmodel.ModelDownloadStatus.DOWNLOADING ||
                        voskDownload?.status == com.echo.recorder.ui.viewmodel.ModelDownloadStatus.EXTRACTING) {
                        Spacer(modifier = Modifier.height(8.dp))
                        val progress = voskDownload.progress.coerceIn(0f, 1f)
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = AccentPurple,
                            trackColor = SurfaceDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (voskDownload.status == com.echo.recorder.ui.viewmodel.ModelDownloadStatus.EXTRACTING) "अनपैक हो रहा है..." else "${(progress * 100).toInt()}%",
                                fontSize = 11.sp,
                                color = AccentPurple,
                                fontWeight = FontWeight.Bold
                            )
                            val downloadedMb = String.format(java.util.Locale.US, "%.1f", voskDownload.downloadedBytes / (1024f * 1024f))
                            val totalMb = String.format(java.util.Locale.US, "%.1f", voskDownload.totalBytes / (1024f * 1024f))
                            Text(
                                text = "$downloadedMb MB / $totalMb MB",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isHindi) "बिना इंटरनेट के लाइव और फ़ाइल प्रतिलेखन के लिए Vosk Hindi (45 MB) डाउनलोड करें।"
                                   else "Download Vosk Hindi (45 MB) to enable real-time speech-to-text without audio conflicts.",
                            fontSize = 11.sp,
                            color = TextMuted,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        } else if (!isServiceRunning) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF10B981).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "⚡ ऑफ़लाइन वाक् प्रतिलेखन सक्रिय (Vosk Hindi)" else "⚡ 100% Offline Live STT Active (Vosk Hindi)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Large Elapsed Timer & Voice dB Level
        if (isServiceRunning) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = formattedTimer,
                    fontSize = 46.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isSpeechDetected) PrimaryBlue else TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Sound Level",
                        tint = if (isSpeechDetected) PrimaryBlue else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "$currentDb dB • ${if (isSpeechDetected) "आवाज सक्रिय 🎤" else "शांत / परिवेश"}"
                               else "$currentDb dB • ${if (isSpeechDetected) "Voice Active 🎤" else "Ambient / Silence"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSpeechDetected) PrimaryBlue else TextMuted
                    )
                }
            }
        } else {
            Text(
                text = if (isHindi) "निरंतर सुनने के लिए नीचे 'रिकॉर्डिंग शुरू करें' पर टैप करें" else "Tap Start Recording below to begin continuous listening",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Waveform
        AudioWaveformVisualizer(
            amplitude = currentAmplitude,
            isRecording = isServiceRunning
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Big Pulsing Button
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(150.dp)
        ) {
            if (isServiceRunning) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .scale(pulseScale)
                        .background(
                            color = ActiveRed.copy(alpha = 0.25f),
                            shape = CircleShape
                        )
                )
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(115.dp)
                    .background(
                        brush = if (isServiceRunning) {
                            Brush.radialGradient(listOf(ActiveRed, Color(0xFF991B1B)))
                        } else {
                            Brush.radialGradient(listOf(PrimaryBlue, AccentPurple))
                        },
                        shape = CircleShape
                    )
                    .combinedClickable(
                        onClick = {
                            if (isServiceRunning) {
                                viewModel.stopRecordingService(context)
                            } else {
                                onStartRecordingAttempt()
                            }
                        },
                        onLongClick = {
                            if (isServiceRunning) {
                                triggerAbortDialog()
                            }
                        }
                    )
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isServiceRunning) (if (isHindi) "रिकॉर्डिंग रोकें (निरस्त करने के लिए दबाकर रखें)" else "Stop Recording (Hold to Abort)") else (if (isHindi) "रिकॉर्डिंग शुरू करें" else "Start Recording"),
                    tint = TextPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explicit Action Buttons: Clear STOP / START / ABORT
        if (isServiceRunning) {
            Row(
                modifier = Modifier.fillMaxWidth(0.85f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // STOP Button with Long Press Support
                Button(
                    onClick = { viewModel.stopRecordingService(context) },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isHindi) "रोकें" else "STOP",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }

                // Explicit ABORT Button
                Button(
                    onClick = { triggerAbortDialog() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    border = BorderStroke(1.dp, ActiveRed),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        text = if (isHindi) "🚫 निरस्त" else "🚫 ABORT",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            Button(
                onClick = { onStartRecordingAttempt() },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = null,
                    tint = DarkBackground,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isHindi) "रिकॉर्डिंग शुरू करें" else "START RECORDING",
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground,
                    fontSize = 15.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Files for Audio Source: Pick pre-recorded file as input
        OutlinedButton(
            onClick = { audioPickerLauncher.launch("audio/*") },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(0.75f).height(42.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = PrimaryBlue,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isHindi) "📁 ऑडियो फ़ाइल चुनें" else "📁 Files for Audio Source",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Live Speech Transcribe Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(
                brush = androidx.compose.ui.graphics.SolidColor(if (isServiceRunning) PrimaryBlue else CardBorder)
            )
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = null,
                            tint = if (isServiceRunning) PrimaryBlue else TextMuted
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isHindi) "लाइव वाक् प्रतिलेखन" else "Live Speech Transcribe",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isServiceRunning) PrimaryBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = if (isServiceRunning) "LIVE • $activeModelName" else activeModelName.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunning) PrimaryBlue else TextMuted,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (liveTranscript.isNotBlank() && isServiceRunning) {
                        liveTranscript
                    } else if (isServiceRunning) {
                        if (isHindi) "$activeModelName के साथ हिंदी, हिंग्लिश या अंग्रेजी में सुन रहा है... बोलने पर तुरंत लिखा जाएगा।"
                        else "Listening with $activeModelName for voice in Hindi, Hinglish, or English... Speak now to transcribe."
                    } else {
                        if (isHindi) "$activeModelName के साथ प्रतिलेखन और कार्य निकालने के लिए रिकॉर्डिंग शुरू करें।"
                        else "Start recording to transcribe and extract action items with $activeModelName."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (liveTranscript.isNotBlank() && isServiceRunning) TextPrimary else TextMuted,
                    lineHeight = 22.sp
                )

                if (isServiceRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isHindi) "एआई मॉडल: $activeModelName • प्रत्येक ${userPreferences.bufferLengthSeconds}s पर टुकड़े सहेजे गए • डैशबोर्ड से सिंक"
                               else "AI Model: $activeModelName • Slices saved every ${userPreferences.bufferLengthSeconds}s • Synced to Dashboard",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPurple
                    )
                }
            }
        }



        Spacer(modifier = Modifier.height(18.dp))

        // Session Stats Footer
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${userPreferences.bufferLengthSeconds}s",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryBlue
                    )
                    Text(
                        text = if (isHindi) "टुकड़ा चक्र" else "Chunk Cycle",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(CardBorder)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${userPreferences.sampleRate / 1000} kHz",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )
                    Text(
                        text = if (isHindi) "गुणवत्ता" else "Quality",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(30.dp)
                        .background(CardBorder)
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$createdSegmentsCount",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = if (isHindi) "ऑडियो टुकड़े" else "Audio Slices",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }
        }

        // Validation Warning Alert Dialog
        if (validationError != null) {
            val err = validationError!!
            AlertDialog(
                onDismissRequest = { validationError = null },
                icon = { Icon(Icons.Default.Warning, contentDescription = null, tint = ActiveRed) },
                title = {
                    Text(
                        text = when (err) {
                            is com.echo.recorder.data.ModelValidationResult.IncompatibleMode -> if (isHindi) "मोड और मॉडल असंगत" else "Mode & Model Mismatch"
                            is com.echo.recorder.data.ModelValidationResult.MissingDownload -> if (isHindi) "मॉडल डाउनलोड नहीं है" else "Model Not Downloaded"
                            is com.echo.recorder.data.ModelValidationResult.MissingApiKey -> if (isHindi) "एपीआई कुंजी आवश्यक" else "API Key Required"
                            is com.echo.recorder.data.ModelValidationResult.ServiceUnavailable -> if (isHindi) "सेवा अनुपलब्ध" else "Service Unavailable"
                            else -> if (isHindi) "अमान्य चयन" else "Invalid Selection"
                        },
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Column {
                        Text(
                            text = when (err) {
                                is com.echo.recorder.data.ModelValidationResult.IncompatibleMode -> err.message
                                is com.echo.recorder.data.ModelValidationResult.MissingDownload -> err.message
                                is com.echo.recorder.data.ModelValidationResult.MissingApiKey -> err.message
                                is com.echo.recorder.data.ModelValidationResult.ServiceUnavailable -> err.message
                                else -> if (isHindi) "चयनित मॉडल रिकॉर्डिंग के लिए अमान्य है।" else "Selected model is not valid for recording."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = if (isHindi) "💡 संकेत: आप बिना माइक्रोफ़ोन के सीधे 'ऑडियो फ़ाइल चुनें' पर टैप करके ऑडियो ट्रांसक्राइब कर सकते हैं।"
                                   else "💡 Tip: You can also tap 'Files for Audio Source' to pick and transcribe pre-recorded audio files directly without microphone recording.",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue
                        )
                    }
                },
                confirmButton = {
                    if (err is com.echo.recorder.data.ModelValidationResult.IncompatibleMode) {
                        Button(
                            onClick = {
                                viewModel.updateSttModel(err.suggestedModelId)
                                validationError = null
                                onRequestPermission()
                                viewModel.startRecordingService(context)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(if (isHindi) "स्वतः ठीक करें और रिकॉर्ड करें" else "Auto-Fix & Record", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else if (err is com.echo.recorder.data.ModelValidationResult.MissingDownload) {
                        Button(
                            onClick = {
                                viewModel.downloadModel(err.modelId)
                                validationError = null
                                Toast.makeText(context, if (isHindi) "डाउनलोड शुरू हुआ! सेटिंग्स में प्रगति देखें।" else "Download started! Monitor progress in Settings.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(if (isHindi) "मॉडल डाउनलोड करें" else "Download Model", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { validationError = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text(if (isHindi) "ठीक है" else "OK", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            validationError = null
                            audioPickerLauncher.launch("audio/*")
                        }
                    ) {
                        Text(if (isHindi) "ऑडियो फ़ाइल चुनें" else "Pick Audio File", color = TextPrimary)
                    }
                },
                containerColor = SurfaceDark
            )
        }

        // ── ⚡ Missing Model Confirmation Dialog ──
        if (showMissingModelDialog) {
            AlertDialog(
                onDismissRequest = { showMissingModelDialog = false },
                icon = { Icon(Icons.Default.CloudDownload, contentDescription = null, tint = AccentPurple) },
                title = {
                    Text(
                        text = if (isHindi) "⚡ ऑफ़लाइन वाक् मॉडल आवश्यक" else "⚡ Offline Speech Model Required",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = if (isHindi) "गुड-इको आपके डिवाइस पर 100% ऑफ़लाइन प्रतिलेखन करता है। लाइव बोलने पर स्क्रीन पर शब्द देखने के लिए Vosk Hindi (45 MB) मॉडल आवश्यक है।\n\nक्या आप इसे अभी डाउनलोड करना चाहते हैं या केवल ऑडियो रिकॉर्ड करना चाहते हैं?"
                               else "GoodEcho transcribes your speech 100% on-device without internet or audio conflicts. To see live text while speaking, the lightweight Vosk Hindi model (45 MB) is required.\n\nWould you like to download it now or record raw audio only?",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showMissingModelDialog = false
                            viewModel.downloadModel(com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI)
                            onRequestPermission()
                            viewModel.startRecordingService(context)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                    ) {
                        Text(if (isHindi) "डाउनलोड और रिकॉर्ड (45 MB)" else "Download & Record (45 MB)", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = {
                            showMissingModelDialog = false
                            onRequestPermission()
                            viewModel.startRecordingService(context)
                        }
                    ) {
                        Text(if (isHindi) "केवल ऑडियो रिकॉर्ड करें" else "Record Audio Only", color = TextPrimary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
