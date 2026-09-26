package com.echo.recorder.ui.screens

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
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
                            onRequestPermission()
                            viewModel.startRecordingService(context)
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
                    onRequestPermission()
                    viewModel.startRecordingService(context)
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
    }
}
