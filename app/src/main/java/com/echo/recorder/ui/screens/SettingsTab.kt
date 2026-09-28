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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DriveFileMove
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
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.ui.draw.clip
import com.echo.recorder.data.SettingsMode
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
import androidx.compose.material3.TextButton
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
    var showAllModels by remember { mutableStateOf(false) }
    var expandedSttModelId by remember { mutableStateOf<String?>(null) }
    var expandedAiModelId by remember { mutableStateOf<String?>(null) }

    // Smart vs. Advanced Modes Management
    val currentGlobalMode = userPreferences.settingsMode
    val sectionAdvancedOverrides = remember { mutableStateMapOf<String, Boolean>() }
    fun isSectionAdvanced(key: String): Boolean =
        (currentGlobalMode == SettingsMode.ADVANCED) || (sectionAdvancedOverrides[key] == true)

    // Manual Model ZIP file picker launcher
    var targetImportModelId by remember { mutableStateOf<String?>(null) }
    val zipPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && targetImportModelId != null) {
            viewModel.importModelZipFromUri(uri, targetImportModelId!!) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }

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
        // GLOBAL MODE SELECTOR: SMART (Automated) vs ADVANCED (Full Control)
        // =========================================================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val isSmart = currentGlobalMode == SettingsMode.SMART
                    // Smart Mode Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSmart) PrimaryBlue else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.updateSettingsMode(SettingsMode.SMART)
                                Toast.makeText(context, "⚡ Smart Mode: Automated & minimal manual work", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = if (isSmart) DarkBackground else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Smart Mode",
                                fontWeight = FontWeight.Bold,
                                color = if (isSmart) DarkBackground else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Advanced Mode Button
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (!isSmart) Color(0xFF8B5CF6) else Color.Transparent,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                viewModel.updateSettingsMode(SettingsMode.ADVANCED)
                                Toast.makeText(context, "🛠️ Advanced Mode: Granular knobs & diagnostic tools revealed", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = if (!isSmart) Color.White else TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Advanced Mode",
                                fontWeight = FontWeight.Bold,
                                color = if (!isSmart) Color.White else TextSecondary,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (currentGlobalMode == SettingsMode.SMART)
                        "⚡ Smart Mode: Automated downloads and zero-friction setup. On auto-download failure, sections switch to Advanced automatically."
                    else
                        "🛠️ Advanced Mode: Full control, manual model ZIP imports, custom mirror URLs, audio telemetry, and model export tools.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (currentGlobalMode == SettingsMode.SMART) PrimaryBlue.copy(alpha = 0.9f) else Color(0xFFC4B5FD),
                    fontSize = 11.sp
                )
            }
        }

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
                val isStorageAdvanced = isSectionAdvanced("storage")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "Storage", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "1. Storage & Locations",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    SectionModePill(
                        isAdvanced = isStorageAdvanced,
                        onToggle = { sectionAdvancedOverrides["storage"] = !isStorageAdvanced }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Standard path for audio chunks, transcription markdown files, AI notes, models, and logs:",
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

                // If in Advanced Mode: show individual directory breakdowns
                AnimatedVisibility(visible = isStorageAdvanced) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        val folderDestinations = listOf(
                            Triple("Audio Files (.wav)", "audio/", userPreferences.audioFolderPath),
                            Triple("Transcriptions (.md)", "transcriptions/", userPreferences.transcriptionFolderPath),
                            Triple("AI Notes & Action Items", "notes/", userPreferences.notesFolderPath),
                            Triple("Offline Models & Weights", "models/", ""),
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
                            val opened = AppStorageManager.openFolderWithIntent(context, baseDir)
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

                Spacer(modifier = Modifier.height(12.dp))

                // Export / Migrate Media & Models Button
                val isExporting by viewModel.isExporting.collectAsState()

                Button(
                    onClick = {
                        viewModel.exportAllMedia { success, message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = DarkBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Exporting & Moving Media & Models...", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    } else {
                        Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export All Media & Models to Public Storage", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Advanced Model Export Options (Revealed in Advanced Mode)
                AnimatedVisibility(visible = isStorageAdvanced) {
                    Column(modifier = Modifier.padding(top = 10.dp)) {
                        Text(
                            text = "🛠️ Dedicated Model Export & Maintenance:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFC4B5FD)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.exportModelsDirectory { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export Models", fontSize = 11.sp, color = PrimaryBlue)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.exportModelsZip { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Export ZIP", fontSize = 11.sp, color = AccentPurple)
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                val cleaned = AppStorageManager.cleanupWasteModels(context)
                                Toast.makeText(context, "Cleaned up $cleaned obsolete model stubs.", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(14.dp), tint = ActiveRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clean Obsolete Model Stubs", fontSize = 11.sp, color = ActiveRed)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Moves all previously recorded audio chunks (.wav), transcriptions (.md), notes, and logs from internal storage to the currently configured folder.",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted,
                    fontSize = 11.sp
                )
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
                val isAudioAdvanced = isSectionAdvanced("audio")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    SectionModePill(
                        isAdvanced = isAudioAdvanced,
                        onToggle = { sectionAdvancedOverrides["audio"] = !isAudioAdvanced }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure audio quality and smaller buffer lengths for faster transcription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Audio Quality (8 kHz default in Smart Mode; full matrix in Advanced Mode)
                if (isAudioAdvanced) {
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
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "⚡ Optimized: ${userPreferences.sampleRate / 1000} kHz Speech Rate (Switch to Advanced for 16-44 kHz)",
                            style = MaterialTheme.typography.labelSmall,
                            color = PrimaryBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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
                val isSttAdvanced = isSectionAdvanced("stt_models")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Default.SmartToy, contentDescription = "STT", tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3. Transcription Models & Download",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    SectionModePill(
                        isAdvanced = isSttAdvanced,
                        onToggle = { sectionAdvancedOverrides["stt_models"] = !isSttAdvanced }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select voice model. Auto-downloads from source by default; switches to manual options on error.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Mode-Aware Filter Banner
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (userPreferences.liveTranscribe) PrimaryBlue.copy(alpha = 0.12f) else AccentPurple.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = if (userPreferences.liveTranscribe) PrimaryBlue.copy(alpha = 0.4f) else AccentPurple.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (userPreferences.liveTranscribe) "🎙️ Live Mode Active" else "📁 Audio File Mode Active",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = if (userPreferences.liveTranscribe) PrimaryBlue else AccentPurple
                            )
                            Text(
                                text = if (showAllModels) {
                                    "Showing all ${UserPreferences.AVAILABLE_STT_MODELS.size} models (Filter bypassed)"
                                } else if (userPreferences.liveTranscribe) {
                                    "Showing models that support real-time live streaming (Live Only & Both)"
                                } else {
                                    "Showing models that transcribe saved audio files (Offline & Cloud)"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = TextMuted
                            )
                        }
                        TextButton(onClick = { showAllModels = !showAllModels }) {
                            Text(
                                text = if (showAllModels) "Filter Mode" else "All (${UserPreferences.AVAILABLE_STT_MODELS.size})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (userPreferences.liveTranscribe) PrimaryBlue else AccentPurple
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                val displayedSttModels = remember(userPreferences.liveTranscribe, showAllModels) {
                    if (showAllModels) {
                        UserPreferences.AVAILABLE_STT_MODELS
                    } else if (userPreferences.liveTranscribe) {
                        UserPreferences.AVAILABLE_STT_MODELS.filter {
                            it.capability == com.echo.recorder.data.SttCapability.LIVE_ONLY ||
                            it.capability == com.echo.recorder.data.SttCapability.BOTH
                        }
                    } else {
                        UserPreferences.AVAILABLE_STT_MODELS.filter {
                            it.capability == com.echo.recorder.data.SttCapability.FILE_ONLY ||
                            it.capability == com.echo.recorder.data.SttCapability.BOTH
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    displayedSttModels.forEach { modelOption ->
                        val isSelected = userPreferences.selectedSttModel == modelOption.id
                        val isExpanded = expandedSttModelId == modelOption.id
                        val isRequirementFulfilled = UserPreferences.isSttModelRequirementFulfilled(
                            context = context,
                            modelId = modelOption.id,
                            cloudApiKey = userPreferences.cloudApiKey,
                            downloadedModels = userPreferences.downloadedModels
                        )
                        val isDownloaded = userPreferences.downloadedModels.contains(modelOption.id) ||
                            (modelOption.id == UserPreferences.STT_MODEL_VOSK_HINDI &&
                                com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)) ||
                            (modelOption.id == UserPreferences.STT_MODEL_VOSK_EN_IN &&
                                com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)) ||
                            File(AppStorageManager.getModelsDirectory(context), "${modelOption.id}.bin").exists()
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
                                .clickable {
                                    // Touching expands or collapses only! Does NOT select!
                                    expandedSttModelId = if (expandedSttModelId == modelOption.id) null else modelOption.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    RadioButton(
                                        selected = isSelected,
                                        enabled = isRequirementFulfilled,
                                        onClick = {
                                            if (isRequirementFulfilled) {
                                                viewModel.updateSttModel(modelOption.id)
                                                Toast.makeText(context, "Selected ${modelOption.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                expandedSttModelId = modelOption.id
                                                Toast.makeText(context, "Please complete requirements to use this model", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = PrimaryBlue,
                                            disabledSelectedColor = PrimaryBlue.copy(alpha = 0.4f),
                                            disabledUnselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(text = modelOption.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = when (modelOption.capability) {
                                                    com.echo.recorder.data.SttCapability.LIVE_ONLY -> PrimaryBlue.copy(alpha = 0.2f)
                                                    com.echo.recorder.data.SttCapability.FILE_ONLY -> AccentPurple.copy(alpha = 0.2f)
                                                    com.echo.recorder.data.SttCapability.BOTH -> Color(0xFF10B981).copy(alpha = 0.2f)
                                                }
                                            ) {
                                                Text(
                                                    text = modelOption.capability.label.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = when (modelOption.capability) {
                                                        com.echo.recorder.data.SttCapability.LIVE_ONLY -> PrimaryBlue
                                                        com.echo.recorder.data.SttCapability.FILE_ONLY -> AccentPurple
                                                        com.echo.recorder.data.SttCapability.BOTH -> Color(0xFF10B981)
                                                    },
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                    fontSize = 9.sp
                                                )
                                            }
                                        }
                                        Text(text = modelOption.tag, style = MaterialTheme.typography.labelSmall, color = if (isSelected) PrimaryBlue else TextMuted)
                                    }
                                    IconButton(
                                        onClick = {
                                            expandedSttModelId = if (expandedSttModelId == modelOption.id) null else modelOption.id
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = if (isExpanded) PrimaryBlue else TextMuted
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        Text(text = modelOption.description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Requirements status chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isRequirementFulfilled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isRequirementFulfilled) "✅ Requirements Fulfilled" else "⚠️ Requirements Not Met (Action Needed)",
                                                color = if (isRequirementFulfilled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Model Download / Cloud Config
                                        if (modelOption.id == UserPreferences.STT_MODEL_CLOUD_GEMINI) {
                                            Text("Requires Gemini Cloud API key in Section 4 below.", fontSize = 11.sp, color = TextMuted)
                                        } else if (modelOption.id != UserPreferences.STT_MODEL_GOOGLE_SPEECH) {
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
                                                                    } else {
                                                                        // Auto-fail: switch section to Advanced mode to reveal manual options
                                                                        sectionAdvancedOverrides["stt_models"] = true
                                                                        Toast.makeText(context, "⚠️ Auto-download failed. Switched to Advanced Mode for manual download & import options.", Toast.LENGTH_LONG).show()
                                                                    }
                                                                }
                                                            )
                                                        },
                                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                                                    ) {
                                                        Icon(Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text("Download Model", fontSize = 12.sp, color = DarkBackground, fontWeight = FontWeight.Bold)
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
                                                        shape = RoundedCornerShape(10.dp),
                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                                    ) {
                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = ActiveRed, modifier = Modifier.size(16.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Delete", fontSize = 12.sp, color = ActiveRed, fontWeight = FontWeight.Bold)
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

                                            // Manual Fallback Controls (Revealed when in Advanced Mode or upon Auto-Download Failure)
                                            if (isSttAdvanced && !isDownloaded && modelOption.id != UserPreferences.STT_MODEL_GOOGLE_SPEECH) {
                                                Spacer(modifier = Modifier.height(10.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(10.dp),
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    border = BorderStroke(1.dp, CardBorder),
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                        Text(
                                                            text = "🛠️ Manual Fallback Options (Advanced):",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFC4B5FD)
                                                        )
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                        ) {
                                                            if (!modelOption.downloadUrl.isNullOrEmpty()) {
                                                                OutlinedButton(
                                                                    onClick = {
                                                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(modelOption.downloadUrl))
                                                                        context.startActivity(intent)
                                                                    },
                                                                    modifier = Modifier.weight(1f),
                                                                    shape = RoundedCornerShape(8.dp),
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                                ) {
                                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                                                    Spacer(modifier = Modifier.width(4.dp))
                                                                    Text("Open Browser Link", fontSize = 11.sp, color = PrimaryBlue)
                                                                }
                                                            }

                                                            OutlinedButton(
                                                                onClick = {
                                                                    targetImportModelId = modelOption.id
                                                                    zipPickerLauncher.launch("application/zip")
                                                                },
                                                                modifier = Modifier.weight(1f),
                                                                shape = RoundedCornerShape(8.dp),
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                            ) {
                                                                Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFFC4B5FD))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Import Model ZIP", fontSize = 11.sp, color = Color(0xFFC4B5FD))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Column {
                                                Text(
                                                    text = "Google Speech streams live microphone voice. For transcribing saved chunk files offline, select Vosk Hindi or Gemini Flash.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                OutlinedButton(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent("android.settings.VOICE_INPUT_SETTINGS")
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            try {
                                                                context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
                                                            } catch (_: Exception) {
                                                                Toast.makeText(context, "Open Android Settings -> Voice Input", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("📱 Download Android Offline Speech Packs", fontSize = 11.sp, color = PrimaryBlue)
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // Explicit "Use" button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (isRequirementFulfilled) {
                                                        viewModel.updateSttModel(modelOption.id)
                                                        Toast.makeText(context, "Selected ${modelOption.name} as active STT model", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Please download/complete requirements before using", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                enabled = isRequirementFulfilled && !isSelected,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFF10B981) else PrimaryBlue,
                                                    disabledContainerColor = if (isSelected) Color(0xFF10B981).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    disabledContentColor = if (isSelected) Color(0xFF10B981) else TextMuted
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("✓ Selected", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                } else if (isRequirementFulfilled) {
                                                    Text("Use", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
                                                } else {
                                                    Text("Requirements Incomplete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
        // FLOW STEP 4: AI INTELLIGENCE & NOTES EXTRACTOR (ACCORDION / COLLAPSIBLE CARDS)
        // =========================================================================
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                val isAiAdvanced = isSectionAdvanced("ai_models")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    SectionModePill(
                        isAdvanced = isAiAdvanced,
                        onToggle = { sectionAdvancedOverrides["ai_models"] = !isAiAdvanced }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select intelligence engine. Each AI option expands its unique setup when selected.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AccentPurple.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💡 Note: This AI engine parses your speech transcriptions into categorized todo tasks and notes. To change voice recognition (STT) models, configure Section 3 above.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(10.dp),
                        fontSize = 11.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    UserPreferences.AVAILABLE_AI_MODELS.forEach { modelOption ->
                        val isSelected = userPreferences.selectedAiModel == modelOption.id
                        val isExpanded = expandedAiModelId == modelOption.id
                        val isRequirementFulfilled = UserPreferences.isAiModelRequirementFulfilled(
                            context = context,
                            modelId = modelOption.id,
                            cloudApiKey = userPreferences.cloudApiKey,
                            downloadedModels = userPreferences.downloadedModels
                        )

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) AccentPurple.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = androidx.compose.foundation.BorderStroke(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) AccentPurple else CardBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Touching expands or collapses only! Does NOT select!
                                    expandedAiModelId = if (expandedAiModelId == modelOption.id) null else modelOption.id
                                }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Accordion Header Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        enabled = isRequirementFulfilled,
                                        onClick = {
                                            if (isRequirementFulfilled) {
                                                viewModel.updateAiModel(modelOption.id)
                                                Toast.makeText(context, "Selected ${modelOption.name}", Toast.LENGTH_SHORT).show()
                                            } else {
                                                expandedAiModelId = modelOption.id
                                                Toast.makeText(context, "Please complete setup requirements to use this model", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = AccentPurple,
                                            disabledSelectedColor = AccentPurple.copy(alpha = 0.4f),
                                            disabledUnselectedColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                        )
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
                                    IconButton(
                                        onClick = {
                                            expandedAiModelId = if (expandedAiModelId == modelOption.id) null else modelOption.id
                                        }
                                    ) {
                                        Icon(
                                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Expand",
                                            tint = if (isExpanded) AccentPurple else TextMuted
                                        )
                                    }
                                }

                                // Unique Accordion Body - ONLY Expanded when tapped!
                                AnimatedVisibility(visible = isExpanded) {
                                    Column(modifier = Modifier.padding(top = 10.dp)) {
                                        // Requirements status chip
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isRequirementFulfilled) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = if (isRequirementFulfilled) "✅ Requirements Ready" else "⚠️ Setup Required to Enable",
                                                color = if (isRequirementFulfilled) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
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
                                                val isDeployed = userPreferences.downloadedModels.contains(modelOption.id)
                                                val isDeploying = downloadingProgress.containsKey(modelOption.id)
                                                val deployProgress = downloadingProgress[modelOption.id] ?: 0f
                                                val modelsDir = AppStorageManager.getModelsDirectory(context)

                                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                                    Surface(
                                                        shape = RoundedCornerShape(10.dp),
                                                        color = DarkBackground,
                                                        modifier = Modifier.fillMaxWidth()
                                                    ) {
                                                        Column(modifier = Modifier.padding(12.dp)) {
                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                Text("Google Gemma 2B (Local LLM)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                                                Surface(
                                                                    shape = RoundedCornerShape(6.dp),
                                                                    color = if (isDeployed) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFF59E0B).copy(alpha = 0.2f)
                                                                ) {
                                                                    Text(
                                                                        text = if (isDeployed) "✅ Deployed & Active" else "🟡 Ready to Deploy",
                                                                        color = if (isDeployed) Color(0xFF10B981) else Color(0xFFF59E0B),
                                                                        fontWeight = FontWeight.Bold,
                                                                        fontSize = 11.sp,
                                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                                    )
                                                                }
                                                            }

                                                            Spacer(modifier = Modifier.height(8.dp))
                                                            Text(
                                                                text = "📖 On-Device Deployment Instructions:",
                                                                style = MaterialTheme.typography.labelSmall,
                                                                fontWeight = FontWeight.Bold,
                                                                color = AccentPurple
                                                            )
                                                            Spacer(modifier = Modifier.height(4.dp))
                                                            Text(
                                                                text = "1. Download the quantized weights: gemma-2-2b-it-Q4_K_M.gguf (~1.6 GB) from the official HuggingFace repository.\n" +
                                                                        "2. Place the .gguf file into your device models folder:\n" +
                                                                        "   Download/App data/poc/yesboss/models/gemma-2-2b-it.gguf\n" +
                                                                        "3. Tap 'Auto-Deploy On-Device Engine' below to mount and initialize the local weights for 100% offline task intelligence.",
                                                                style = MaterialTheme.typography.bodySmall,
                                                                fontSize = 11.sp,
                                                                color = TextSecondary,
                                                                lineHeight = 16.sp
                                                            )

                                                                                  if (!isDeployed && !isDeploying) {
                                                                Button(
                                                                    onClick = {
                                                                        downloadingProgress[modelOption.id] = 0.1f
                                                                        viewModel.deployLocalAiModel(
                                                                            modelId = modelOption.id,
                                                                            onProgress = { p -> downloadingProgress[modelOption.id] = p },
                                                                            onComplete = { success, msg ->
                                                                                downloadingProgress.remove(modelOption.id)
                                                                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                                                if (!success) {
                                                                                    // Auto-fail: switch section to Advanced mode
                                                                                    sectionAdvancedOverrides["ai_models"] = true
                                                                                    Toast.makeText(context, "⚠️ Auto-deploy failed. Switched to Advanced Mode for manual GGUF import.", Toast.LENGTH_LONG).show()
                                                                                }
                                                                            }
                                                                        )
                                                                    },
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                                                ) {
                                                                    Icon(Icons.Default.Download, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(16.dp))
                                                                    Spacer(modifier = Modifier.width(6.dp))
                                                                    Text("📥 Auto-Deploy On-Device Engine", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
                                                                }
                                                            } else if (isDeploying) {
                                                                Row(
                                                                    modifier = Modifier.fillMaxWidth(),
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.Center
                                                                ) {
                                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AccentPurple)
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    Text("Deploying Gemma on device ${(deployProgress * 100).toInt()}%...", fontSize = 12.sp, color = AccentPurple)
                                                                }
                                                            } else {
                                                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                                    Surface(
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                                                                        modifier = Modifier.weight(1f)
                                                                    ) {
                                                                        Text(
                                                                            text = "⚡ Local Gemma engine running offline with zero API calls.",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            color = Color(0xFF10B981),
                                                                            modifier = Modifier.padding(8.dp),
                                                                            fontSize = 11.sp
                                                                        )
                                                                    }
                                                                    Spacer(modifier = Modifier.width(8.dp))
                                                                    OutlinedButton(
                                                                        onClick = {
                                                                            viewModel.deleteModel(modelOption.id)
                                                                            Toast.makeText(context, "Undeployed Gemma 2B", Toast.LENGTH_SHORT).show()
                                                                        },
                                                                        shape = RoundedCornerShape(8.dp),
                                                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                                                    ) {
                                                                        Icon(Icons.Default.Delete, contentDescription = null, tint = ActiveRed, modifier = Modifier.size(14.dp))
                                                                        Spacer(modifier = Modifier.width(4.dp))
                                                                        Text("Reset", fontSize = 11.sp, color = ActiveRed)
                                                                    }
                                                                }
                                                            }

                                                            // Advanced Manual Options for Gemma 2B
                                                            if (isAiAdvanced) {
                                                                Spacer(modifier = Modifier.height(10.dp))
                                                                Surface(
                                                                    shape = RoundedCornerShape(10.dp),
                                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                                    border = BorderStroke(1.dp, CardBorder),
                                                                    modifier = Modifier.fillMaxWidth()
                                                                ) {
                                                                    Column(modifier = Modifier.padding(10.dp)) {
                                                                        Text(
                                                                            text = "🛠️ Manual Setup & Diagnostic Links (Advanced):",
                                                                            style = MaterialTheme.typography.labelSmall,
                                                                            fontWeight = FontWeight.Bold,
                                                                            color = Color(0xFFC4B5FD)
                                                                        )
                                                                        Spacer(modifier = Modifier.height(6.dp))
                                                                        Row(
                                                                            modifier = Modifier.fillMaxWidth(),
                                                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                                        ) {
                                                                            OutlinedButton(
                                                                                onClick = {
                                                                                    val opened = AppStorageManager.openFolderWithIntent(context, modelsDir)
                                                                                    if (!opened) {
                                                                                        Toast.makeText(context, "Models folder: ${modelsDir.absolutePath}", Toast.LENGTH_LONG).show()
                                                                                    }
                                                                                },
                                                                                modifier = Modifier.weight(1f),
                                                                                shape = RoundedCornerShape(8.dp),
                                                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                                            ) {
                                                                                Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Text("Open Models Folder", fontSize = 11.sp, color = TextSecondary)
                                                                            }

                                                                            OutlinedButton(
                                                                                onClick = {
                                                                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://huggingface.co/google/gemma-2-2b-it"))
                                                                                    context.startActivity(intent)
                                                                                },
                                                                                modifier = Modifier.weight(1f),
                                                                                shape = RoundedCornerShape(8.dp),
                                                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                                                            ) {
                                                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp), tint = AccentPurple)
                                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                                Text("HuggingFace Repo", fontSize = 11.sp, color = AccentPurple)
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
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

                                        Spacer(modifier = Modifier.height(14.dp))

                                        // Explicit "Use" button for AI model
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            Button(
                                                onClick = {
                                                    if (isRequirementFulfilled) {
                                                        viewModel.updateAiModel(modelOption.id)
                                                        Toast.makeText(context, "Selected ${modelOption.name} as active AI model", Toast.LENGTH_SHORT).show()
                                                    } else {
                                                        Toast.makeText(context, "Please complete setup requirements before using", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                enabled = isRequirementFulfilled && !isSelected,
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isSelected) Color(0xFF10B981) else AccentPurple,
                                                    disabledContainerColor = if (isSelected) Color(0xFF10B981).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                                                    disabledContentColor = if (isSelected) Color(0xFF10B981) else TextMuted
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                                            ) {
                                                if (isSelected) {
                                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("✓ Selected", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                } else if (isRequirementFulfilled) {
                                                    Text("Use", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
                                                } else {
                                                    Text("Setup Incomplete", fontWeight = FontWeight.Bold, fontSize = 12.sp)
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
                val isLogsAdvanced = isSectionAdvanced("logs")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
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
                    SectionModePill(
                        isAdvanced = isLogsAdvanced,
                        onToggle = { sectionAdvancedOverrides["logs"] = !isLogsAdvanced }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Per-session log files are continuously saved into your selected logs folder.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(10.dp))

                var activeSessionName by remember { mutableStateOf(AppLogger.getCurrentSessionFile()?.name ?: "session_active.log") }
                val sessionFile = remember(activeSessionName) { AppLogger.getCurrentSessionFile() }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(text = "Active Session File:", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                        Text(text = sessionFile?.name ?: activeSessionName, fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 12.sp)
                        Text(text = sessionFile?.absolutePath ?: "Download/App data/poc/yesboss/logs/", color = TextMuted, fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Share Log", fontSize = 12.sp, color = PrimaryBlue)
                    }

                    OutlinedButton(
                        onClick = {
                            AppLogger.clearLogs()
                            Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(14.dp), tint = ActiveRed)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Logs", fontSize = 12.sp, color = ActiveRed)
                    }
                }

                // Advanced Log Tools
                if (isLogsAdvanced) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "🛠️ Advanced Log Management:",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFC4B5FD)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        AppLogger.startNewSession(context)
                                        activeSessionName = AppLogger.getCurrentSessionFile()?.name ?: "session_new.log"
                                        Toast.makeText(context, "Created new active session log file!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("New Session", fontSize = 11.sp, color = PrimaryBlue)
                                }

                                OutlinedButton(
                                    onClick = {
                                        val logsDir = AppStorageManager.getLogsDirectory(context, userPreferences.logsFolderPath)
                                        val opened = AppStorageManager.openFolderWithIntent(context, logsDir)
                                        if (!opened) {
                                            Toast.makeText(context, "Logs directory: ${logsDir.absolutePath}", Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Open Logs Dir", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
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

/**
 * Granular Section Mode Pill Badge
 * Allows user to toggle individual sections between Smart and Advanced mode
 */
@Composable
fun SectionModePill(
    isAdvanced: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isAdvanced) Color(0xFF8B5CF6).copy(alpha = 0.15f) else PrimaryBlue.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, if (isAdvanced) Color(0xFF8B5CF6).copy(alpha = 0.5f) else PrimaryBlue.copy(alpha = 0.5f)),
        modifier = modifier.clickable { onToggle() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAdvanced) Icons.Default.Tune else Icons.Default.Bolt,
                contentDescription = null,
                tint = if (isAdvanced) Color(0xFFC4B5FD) else PrimaryBlue,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isAdvanced) "Advanced" else "Smart",
                color = if (isAdvanced) Color(0xFFC4B5FD) else PrimaryBlue,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
