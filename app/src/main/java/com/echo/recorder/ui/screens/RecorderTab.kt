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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.Composable
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
    var validationError by remember { mutableStateOf<com.echo.recorder.data.ModelValidationResult?>(null) }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.importAudioFile(uri) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

    val onStartRecordingAttempt = {
        val validation = viewModel.validateCurrentModel(context)
        if (validation is com.echo.recorder.data.ModelValidationResult.Valid) {
            onRequestPermission()
            viewModel.startRecordingService(context)
        } else {
            validationError = validation
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

    val qualityLabel = when (userPreferences.sampleRate) {
        UserPreferences.QUALITY_TINIEST -> "8 kHz (Tiniest Quality)"
        UserPreferences.QUALITY_SPEECH -> "16 kHz (Speech)"
        UserPreferences.QUALITY_STANDARD -> "22.05 kHz"
        else -> "44.1 kHz (HD)"
    }

    val activeModelName = when (userPreferences.selectedSttModel) {
        UserPreferences.STT_MODEL_VOSK_HINDI -> "Vosk Hindi"
        UserPreferences.STT_MODEL_VOSK_EN_IN -> "Vosk Indian English"
        UserPreferences.STT_MODEL_CLOUD_GEMINI -> "Gemini 1.5 Flash"
        UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper Tiny"
        UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper Base"
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
                    text = "$qualityLabel • ${userPreferences.bufferLengthSeconds}s Chunks",
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
                        text = if (isServiceRunning) "RECORDING LIVE" else "STANDBY",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) ActiveRed else TextMuted
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

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
                        text = "$currentDb dB • ${if (isSpeechDetected) "Voice Active 🎤" else "Ambient / Silence"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSpeechDetected) PrimaryBlue else TextMuted
                    )
                }
            }
        } else {
            Text(
                text = "Tap Start Recording below to begin continuous listening",
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
                    .clickable {
                        if (isServiceRunning) {
                            viewModel.stopRecordingService(context)
                        } else {
                            onStartRecordingAttempt()
                        }
                    }
            ) {
                Icon(
                    imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isServiceRunning) "Stop Recording" else "Start Recording",
                    tint = TextPrimary,
                    modifier = Modifier.size(52.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Explicit Action Button: Clear "STOP" or "START" button
        Button(
            onClick = {
                if (isServiceRunning) {
                    viewModel.stopRecordingService(context)
                } else {
                    onStartRecordingAttempt()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isServiceRunning) ActiveRed else PrimaryBlue
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(48.dp)
        ) {
            Icon(
                imageVector = if (isServiceRunning) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = null,
                tint = if (isServiceRunning) TextPrimary else DarkBackground,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isServiceRunning) "STOP RECORDING" else "START RECORDING",
                fontWeight = FontWeight.Bold,
                color = if (isServiceRunning) TextPrimary else DarkBackground,
                fontSize = 15.sp
            )
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
                text = "📁 Files for Audio Source",
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
                            text = "Live Speech Transcribe",
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
                        "Listening with $activeModelName for voice in Hindi, Hinglish, or English... Speak now to transcribe."
                    } else {
                        "Start recording to transcribe and extract action items with $activeModelName."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (liveTranscript.isNotBlank() && isServiceRunning) TextPrimary else TextMuted,
                    lineHeight = 22.sp
                )

                if (isServiceRunning) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AI Model: $activeModelName • Slices saved every ${userPreferences.bufferLengthSeconds}s • Synced to Dashboard",
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
                        text = "Chunk Cycle",
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
                        text = "Quality",
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
                        text = "Audio Slices",
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
                            is com.echo.recorder.data.ModelValidationResult.IncompatibleMode -> "Mode & Model Mismatch"
                            is com.echo.recorder.data.ModelValidationResult.MissingDownload -> "Model Not Downloaded"
                            is com.echo.recorder.data.ModelValidationResult.MissingApiKey -> "API Key Required"
                            is com.echo.recorder.data.ModelValidationResult.ServiceUnavailable -> "Service Unavailable"
                            else -> "Invalid Selection"
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
                                else -> "Selected model is not valid for recording."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "💡 Tip: You can also tap 'Files for Audio Source' to pick and transcribe pre-recorded audio files directly without microphone recording.",
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
                            Text("Auto-Fix & Record", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else if (err is com.echo.recorder.data.ModelValidationResult.MissingDownload) {
                        Button(
                            onClick = {
                                viewModel.downloadModel(err.modelId)
                                validationError = null
                                Toast.makeText(context, "Download started! Monitor progress in Settings.", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("Download Model", color = DarkBackground, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { validationError = null },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Text("OK", color = DarkBackground, fontWeight = FontWeight.Bold)
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
                        Text("Pick Audio File", color = TextPrimary)
                    }
                },
                containerColor = SurfaceDark
            )
        }
    }
}
