package com.echo.recorder.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.util.Locale
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.echo.recorder.ui.viewmodel.MainViewModel
import com.echo.recorder.util.AppLogger
import com.echo.recorder.util.AppStorageManager
import java.io.File

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    userPreferences: UserPreferences,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations(context)
    val storageMb by viewModel.audioStorageMb.collectAsState()

    // Download progress tracking per model
    val downloadingProgress = remember { mutableStateMapOf<String, Float>() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "Configured in operational flow order: Storage → Audio → STT → AI Models → Logs",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 1: STORAGE & DIRECTORY LOCATIONS (Default: Download/App data/poc/yesboss)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Folder, contentDescription = "Storage", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "1. Storage & Directory Locations",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Default base path for audio chunks, transcription markdown files, AI notes, and logs:",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                val baseDir = remember { AppStorageManager.getBaseDirectory(context) }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "📁 Base Directory (Default):",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Text(
                            text = AppStorageManager.DEFAULT_RELATIVE_PATH,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = baseDir.absolutePath,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Folder destination chips
                val folderDestinations = listOf(
                    Triple("Audio Files (.wav)", "audio/", userPreferences.audioFolderPath),
                    Triple("Transcriptions (.md, .txt)", "transcriptions/", userPreferences.transcriptionFolderPath),
                    Triple("AI Notes & Action Items", "notes/", userPreferences.notesFolderPath),
                    Triple("Session Logs (.log)", "logs/", userPreferences.logsFolderPath)
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    folderDestinations.forEach { (label, subpath, custom) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• $label:",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                            Text(
                                text = if (custom.isNotBlank()) custom else subpath,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = PrimaryBlue
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetStoragePathsToDefault()
                            Toast.makeText(context, "Reset all folders to Download/App data/poc/yesboss/", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset to Default", fontSize = 12.sp, color = TextSecondary)
                    }

                    OutlinedButton(
                        onClick = {
                            val opened = AppStorageManager.openFileWithIntent(context, baseDir)
                            if (!opened) {
                                Toast.makeText(context, "Path: ${baseDir.absolutePath}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open Folder", fontSize = 12.sp, color = PrimaryBlue)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 2: AUDIO & RECORDING CONTROLS (Tiniest Quality, Buffer Chunks, VAD)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.GraphicEq, contentDescription = "Audio", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "2. Audio Recording & Chunking",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure audio quality and smaller buffer lengths for faster transcription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Audio Quality (8 kHz default)
                Text(text = "Audio Sample Rate (Quality)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                val qualityOptions = listOf(
                    Triple(UserPreferences.QUALITY_TINIEST, "8 kHz", "Tiniest"),
                    Triple(UserPreferences.QUALITY_SPEECH, "16 kHz", "Speech"),
                    Triple(UserPreferences.QUALITY_STANDARD, "22 kHz", "Standard"),
                    Triple(UserPreferences.QUALITY_HIGH, "44 kHz", "High")
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    qualityOptions.forEach { (rate, label, desc) ->
                        val isSelected = userPreferences.sampleRate == rate
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateSampleRate(rate) }
                        ) {
                            Column(modifier = Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = label, fontWeight = FontWeight.Bold, color = if (isSelected) DarkBackground else TextPrimary, fontSize = 12.sp)
                                Text(text = desc, color = if (isSelected) DarkBackground.copy(alpha = 0.8f) else TextMuted, fontSize = 10.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Buffer Length Slider (Smaller Chunks)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Audio File Chunk Length", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = "Smaller chunks transcribe faster", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    Text(text = "${userPreferences.bufferLengthSeconds}s", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = AccentPurple)
                }
                Slider(
                    value = userPreferences.bufferLengthSeconds.toFloat(),
                    onValueChange = { viewModel.updateBufferLength(it.toInt()) },
                    valueRange = 5f..60f,
                    steps = 10,
                    colors = SliderDefaults.colors(thumbColor = AccentPurple, activeTrackColor = AccentPurple)
                )

                // Presets
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(5, 10, 15, 30, 60).forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (userPreferences.bufferLengthSeconds == preset) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { viewModel.updateBufferLength(preset) }
                        ) {
                            Text(
                                text = "${preset}s",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userPreferences.bufferLengthSeconds == preset) DarkBackground else TextPrimary,
                                modifier = Modifier.padding(vertical = 6.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // VAD / Skip Silence
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Skip Ambient Silence (VAD)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = "Only saves files when voice activity is detected", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Switch(
                        checked = userPreferences.skipSilence,
                        onCheckedChange = { viewModel.updateSkipSilence(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryBlue)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Live Transcribe Toggle
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Live Real-Time Transcribe", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = "Displays live speech text on screen while listening", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                    }
                    Switch(
                        checked = userPreferences.liveTranscribe,
                        onCheckedChange = { viewModel.updateLiveTranscribe(it) },
                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryBlue)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 3: SPEECH-TO-TEXT (STT) MODELS & ON-DEVICE MODEL DOWNLOAD
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.SmartToy, contentDescription = "STT", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "3. Transcription Models & Model Download",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select your voice-to-text model. Download offline models directly to device for offline inference.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserPreferences.AVAILABLE_STT_MODELS.forEach { modelOption ->
                        val isSelected = userPreferences.selectedSttModel == modelOption.id
                        val isDownloaded = userPreferences.downloadedModels.contains(modelOption.id)
                        val isDownloading = downloadingProgress.containsKey(modelOption.id)
                        val progress = downloadingProgress[modelOption.id] ?: 0f

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) PrimaryBlue.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) PrimaryBlue else CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateSttModel(modelOption.id) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateSttModel(modelOption.id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(text = modelOption.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant
                                            ) {
                                                Text(
                                                    text = modelOption.tag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DarkBackground else TextMuted,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                        Text(text = modelOption.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                    }
                                }

                                // Model Download Options for offline models
                                if (modelOption.id != UserPreferences.STT_MODEL_GOOGLE_SPEECH) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDownloaded) "Status: Installed (${modelOption.sizeText}) ✅" else "Status: Not Downloaded (${modelOption.sizeText})",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isDownloaded) Color(0xFF10B981) else Color(0xFFF59E0B)
                                        )

                                        if (!isDownloaded && !isDownloading) {
                                            Button(
                                                onClick = {
                                                    downloadingProgress[modelOption.id] = 0.1f
                                                    viewModel.downloadModel(
                                                        modelId = modelOption.id,
                                                        onProgress = { p -> downloadingProgress[modelOption.id] = p },
                                                        onComplete = { success ->
                                                            downloadingProgress.remove(modelOption.id)
                                                            if (success) {
                                                                Toast.makeText(context, "${modelOption.name} installed successfully!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    )
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Download Model", fontSize = 11.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
                                            }
                                        } else if (isDownloading) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = PrimaryBlue)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Downloading ${(progress * 100).toInt()}%", fontSize = 11.sp, color = PrimaryBlue)
                                            }
                                        } else {
                                            OutlinedButton(
                                                onClick = {
                                                    viewModel.deleteModel(modelOption.id)
                                                    Toast.makeText(context, "Deleted ${modelOption.name}", Toast.LENGTH_SHORT).show()
                                                },
                                                shape = RoundedCornerShape(8.dp),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, tint = ActiveRed, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Delete", fontSize = 11.sp, color = ActiveRed)
                                            }
                                        }
                                    }

                                    if (isDownloading) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { progress },
                                            modifier = Modifier.fillMaxWidth().height(4.dp),
                                            color = PrimaryBlue
                                        )
                                    }
                                } else {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Status: Built-in Android Speech Service (Ready)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF10B981),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 4: AI INTELLIGENCE & NOTES EXTRACTOR (ACCORDION / COLLAPSIBLE CARDS)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Psychology, contentDescription = "AI", tint = AccentPurple)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "4. AI Task & Notes Extractor Model",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select intelligence engine. Each AI option expands its unique setup when selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserPreferences.AVAILABLE_AI_MODELS.forEach { modelOption ->
                        val isSelected = userPreferences.selectedAiModel == modelOption.id

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentPurple else CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.updateAiModel(modelOption.id) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Accordion Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateAiModel(modelOption.id) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = modelOption.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = modelOption.tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (isSelected) AccentPurple else TextMuted
                                        )
                                    }
                                    Icon(
                                        imageVector = if (isSelected) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Expand",
                                        tint = if (isSelected) AccentPurple else TextMuted
                                    )
                                }

                                // Unique Accordion Body - ONLY Expanded when this AI model is selected!
                                AnimatedVisibility(visible = isSelected) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(
                                            text = modelOption.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextSecondary
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        when (modelOption.id) {
                                            // Unique UI for Google Gemini (API Key configuration)
                                            UserPreferences.AI_MODEL_CLOUD_GEMINI -> {
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = DarkBackground,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(12.dp)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Icon(Icons.Default.Key, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text("Google Gemini API Key", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                                                        }
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Text("Transcribes raw audio files verbatim in Hindi/Hinglish and extracts smart task lists.", fontSize = 11.sp, color = TextMuted)

                                                        var apiKeyInput by remember(userPreferences.cloudApiKey) { mutableStateOf(userPreferences.cloudApiKey) }
                                                        var showKey by remember { mutableStateOf(false) }

                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        OutlinedTextField(
                                                            value = apiKeyInput,
                                                            onValueChange = { apiKeyInput = it },
                                                            modifier = Modifier.fillMaxWidth(),
                                                            placeholder = { Text("AIzaSy... (Paste Gemini key)", color = TextMuted, fontSize = 12.sp) },
                                                            singleLine = true,
                                                            visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                                                            trailingIcon = {
                                                                IconButton(onClick = { showKey = !showKey }) {
                                                                    Icon(if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility, contentDescription = null, tint = TextMuted)
                                                                }
                                                            }
                                                        )

                                                        Spacer(modifier = Modifier.height(8.dp))
                                                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/app/apikey"))
                                                                    context.startActivity(intent)
                                                                },
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Text("Get Free Gemini Key", fontSize = 11.sp, color = PrimaryBlue)
                                                            }
                                                            Button(
                                                                onClick = {
                                                                    viewModel.updateCloudApiKey(apiKeyInput)
                                                                    Toast.makeText(context, "Gemini API Key saved!", Toast.LENGTH_SHORT).show()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                                shape = RoundedCornerShape(8.dp)
                                                            ) {
                                                                Text("Save Key", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                            }

                                            // Unique UI for Local Gemma 2B
                                            UserPreferences.AI_MODEL_LOCAL_GEMMA -> {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Size: 1.6 GB GGUF", fontSize = 11.sp, color = TextMuted)
                                                    OutlinedButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/google/gemma-2-2b-it"))
                                                            context.startActivity(intent)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("HuggingFace Repo", fontSize = 11.sp, color = AccentPurple)
                                                    }
                                                }
                                            }

                                            // Unique UI for Local Llama 3.2 1B
                                            UserPreferences.AI_MODEL_LOCAL_LLAMA -> {
                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("Size: 1.1 GB GGUF", fontSize = 11.sp, color = TextMuted)
                                                    OutlinedButton(
                                                        onClick = {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/meta-llama/Llama-3.2-1B-Instruct"))
                                                            context.startActivity(intent)
                                                        },
                                                        shape = RoundedCornerShape(8.dp)
                                                    ) {
                                                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Meta HF Repo", fontSize = 11.sp, color = AccentPurple)
                                                    }
                                                }
                                            }

                                            // Built-in Tiny AI
                                            UserPreferences.AI_MODEL_LOCAL_TINY -> {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = DarkBackground,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Text(
                                                        text = "✅ 0 MB Offline Semantic Engine Active. No internet or API keys required.",
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = Color(0xFF10B981),
                                                        modifier = Modifier.padding(10.dp)
                                                    )
                                                }
                                            }

                                            // OpenAI GPT-4o-mini
                                            else -> {
                                                OutlinedButton(
                                                    onClick = {
                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://platform.openai.com/api-keys"))
                                                        context.startActivity(intent)
                                                    },
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text("OpenAI Key Settings", fontSize = 11.sp, color = AccentPurple)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 5: LOGS & SESSION DEBUGGING
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Terminal, contentDescription = "Logs", tint = PrimaryBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5. Session Logs & Diagnostics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Per-session log files are continuously saved into your selected logs folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                val sessionFile = remember { AppLogger.getCurrentSessionFile() }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Active Session File:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = sessionFile?.name ?: "session_active.log", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 12.sp)
                        Text(text = sessionFile?.absolutePath ?: "Download/App data/poc/yesboss/logs/", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(
                        onClick = {
                            val fileToShare = AppLogger.getLogFile()
                            if (fileToShare != null && fileToShare.exists()) {
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    fileToShare
                                )
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share GoodEcho Session Log"))
                            } else {
                                Toast.makeText(context, "No log file to share yet", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Log File", fontSize = 12.sp, color = PrimaryBlue)
                    }

                    OutlinedButton(
                        onClick = {
                            AppLogger.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = ActiveRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", fontSize = 12.sp, color = ActiveRed)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // =========================================================================
        // FLOW STEP 6: BATTERY OPTIMIZATION & STORAGE USAGE
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.BatterySaver, contentDescription = "Battery", tint = if (isIgnoringBattery) PrimaryBlue else ActiveRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "6. System & Battery Optimization", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isIgnoringBattery) "Unrestricted Background Active. Continuous recording will stay alive." else "Exempt app to prevent Android from killing background listening.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                if (!isIgnoringBattery) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { viewModel.requestBatteryOptimizationExemption(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Exempt Battery Optimization", color = DarkBackground, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Storage usage & clear
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text(text = "Cached Audio Storage", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(text = String.format(Locale.US, "%.2f MB used", storageMb), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                    }
                    OutlinedButton(
                        onClick = { viewModel.clearAudioCache() },
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = ActiveRed, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Cache", color = ActiveRed, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
