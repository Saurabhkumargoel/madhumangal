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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.ActiveRed
import com.echo.recorder.ui.theme.CardBorder
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.theme.TextPrimary
import com.echo.recorder.ui.theme.TextSecondary
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AudioPlayerCard(
    transcription: TranscriptionWithAudio,
    isTranscribing: Boolean = false,
    onDelete: (Long) -> Unit,
    onRetranscribe: (Long) -> Unit = {},
    onUpdateText: (Long, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isPlaying by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf(transcription.text) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }

    val file = remember(transcription.filePath) { File(transcription.filePath) }
    val audioExists = remember(file) { file.exists() }
    val fileName = remember(file) { file.name }

    val formattedTime = remember(transcription.timestampMillis) {
        SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(Date(transcription.timestampMillis))
    }

    val isPlaceholder = transcription.text.startsWith("[") && transcription.text.contains("chunk_")

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

            Spacer(modifier = Modifier.height(10.dp))

            // File Tag Info
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

            Spacer(modifier = Modifier.height(10.dp))

            // Main Transcription Body
            if (!isPlaceholder && transcription.text.isNotBlank()) {
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
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tap 'Transcribe Audio' below to convert this audio chunk into Hindi/Hinglish text.",
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

            Spacer(modifier = Modifier.height(10.dp))

            // Manual Action Bar: Transcribe / Re-Transcribe Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        onRetranscribe(transcription.id)
                        Toast.makeText(context, "Transcribing audio chunk...", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isTranscribing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isPlaceholder) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (isTranscribing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DarkBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isPlaceholder) DarkBackground else PrimaryBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = if (isTranscribing) "Transcribing..." else if (isPlaceholder) "Transcribe Audio" else "Re-Transcribe",
                        fontWeight = FontWeight.Bold,
                        color = if (isPlaceholder) DarkBackground else TextPrimary,
                        fontSize = 13.sp
                    )
                }

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
                    Text(text = "Edit", color = TextSecondary, fontSize = 13.sp)
                }
            }
        }
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
