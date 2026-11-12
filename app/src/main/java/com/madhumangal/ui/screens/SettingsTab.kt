package com.madhumangal.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import com.madhumangal.ui.viewmodel.ModelDownloadStatus
import java.util.Locale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.madhumangal.data.SettingsMode
import com.madhumangal.data.SttCapability
import com.madhumangal.data.UserPreferences
import com.madhumangal.ui.theme.AccentPurple
import com.madhumangal.ui.theme.ActiveRed
import com.madhumangal.ui.theme.AppThemes
import com.madhumangal.ui.theme.CardBorder
import com.madhumangal.ui.theme.DarkBackground
import com.madhumangal.ui.theme.PrimaryBlue
import com.madhumangal.ui.theme.SurfaceDark
import com.madhumangal.ui.theme.TextMuted
import com.madhumangal.ui.theme.TextPrimary
import com.madhumangal.ui.theme.TextSecondary
import com.madhumangal.ui.viewmodel.MainViewModel
import com.madhumangal.util.AppConstants
import com.madhumangal.util.AppStorageManager
import com.madhumangal.util.AppStrings
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

@Composable
fun SettingsTab(
    viewModel: MainViewModel,
    userPreferences: UserPreferences,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val isHindi = userPreferences.appLanguage == "hi"
    val isIgnoringBattery = viewModel.isIgnoringBatteryOptimizations(context)
    val storageMb by viewModel.audioStorageMb.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isSyncingFiles by viewModel.isSyncingFiles.collectAsState()
    val modelDownloadStates by viewModel.modelDownloadStates.collectAsState()

    var expandedSttSetupModelId by remember { mutableStateOf<String?>(null) }
    var expandedAiSetupModelId by remember { mutableStateOf<String?>(null) }

    // Download progress tracking per model
    val downloadingProgress = remember { mutableStateMapOf<String, Float>() }

    // JSON Settings Export / Import Dialog States
    var showExportDialog by remember { mutableStateOf(false) }
    var exportJsonPreview by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonInput by remember { mutableStateOf("") }

    // Custom Theme Creator Dialog States
    var showCustomThemeDialog by remember { mutableStateOf(false) }
    var customPrimaryHex by remember { mutableStateOf("#38BDF8") }
    var customBgHex by remember { mutableStateOf("#0F172A") }
    var customSurfaceHex by remember { mutableStateOf("#1E293B") }
    var customBorderHex by remember { mutableStateOf("#334155") }

    // Explore Models Catalog Dialog States
    var showExploreModelsDialog by remember { mutableStateOf(false) }
    var customModelNameInput by remember { mutableStateOf("") }
    var customModelIdInput by remember { mutableStateOf("") }
    var customModelEndpointInput by remember { mutableStateOf("") }

    // Export JSON Section Checkboxes
    var exportAudio by remember { mutableStateOf(true) }
    var exportStt by remember { mutableStateOf(true) }
    var exportAi by remember { mutableStateOf(true) }
    var exportAppearance by remember { mutableStateOf(true) }
    var exportStorage by remember { mutableStateOf(true) }

    // Import JSON Section Checkboxes
    var importAudio by remember { mutableStateOf(true) }
    var importStt by remember { mutableStateOf(true) }
    var importAi by remember { mutableStateOf(true) }
    var importAppearance by remember { mutableStateOf(true) }
    var importStorage by remember { mutableStateOf(true) }

    // Collapsible Category Section States (Persisted in ViewModel across tab switches)
    var isStorageExpanded by viewModel::isSettingsStorageExpanded
    var isThemesExpanded by viewModel::isSettingsThemesExpanded
    var isAudioExpanded by viewModel::isSettingsAudioExpanded
    var isSttExpanded by viewModel::isSettingsSttExpanded
    var isAiExpanded by viewModel::isSettingsAiExpanded
    var isSystemPromptExpanded by viewModel::isSettingsSystemPromptExpanded
    var isBackupExpanded by viewModel::isSettingsBackupExpanded

    // Target Base Folder SAF Picker Launcher
    val targetFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = AppStorageManager.resolveTreeUriToPath(context, uri)
            if (!resolved.isNullOrBlank()) {
                viewModel.updateBaseFolderPath(resolved)
                Toast.makeText(context, "📁 Destination folder set to:\n$resolved", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Could not resolve selected folder path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Source Folder SAF Picker Launcher (Manual source migration)
    val sourceFolderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val resolved = AppStorageManager.resolveTreeUriToPath(context, uri)
            if (!resolved.isNullOrBlank()) {
                viewModel.updateLastExportSourcePath(resolved)
                Toast.makeText(context, "📁 Source folder set to:\n$resolved", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Could not resolve selected source path", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

    // Model File Picker Launcher (.gguf / weights)
    var targetImportModelFileId by remember { mutableStateOf<String?>(null) }
    val modelFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && targetImportModelFileId != null) {
            viewModel.importModelFileFromUri(uri, targetImportModelFileId!!) { success, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }


    // JSON Settings file export launcher (User selects custom destination path each time)
    var pendingExportJson by remember { mutableStateOf<String?>(null) }
    val jsonExportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null && pendingExportJson != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { os ->
                    os.write(pendingExportJson!!.toByteArray(Charsets.UTF_8))
                }
                Toast.makeText(context, "💾 Settings saved to chosen file path!", Toast.LENGTH_LONG).show()
                showExportDialog = false
                pendingExportJson = null
            } catch (e: Exception) {
                Toast.makeText(context, "Error saving file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // JSON Settings file picker launcher
    val jsonImportPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val content = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
                if (!content.isNullOrBlank()) {
                    importJsonInput = content
                    Toast.makeText(context, "Loaded JSON from file!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error reading file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val isAdvanced = userPreferences.isAdvancedOrDev
    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var isGeneralExpanded by viewModel::isSettingsGeneralExpanded
    var showApiKey by remember { mutableStateOf(false) }
    var showHfToken by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var modelToDelete by remember { mutableStateOf<String?>(null) }

    if (modelToDelete != null) {
        val mid = modelToDelete!!
        AlertDialog(
            onDismissRequest = { modelToDelete = null },
            title = { Text(if (isHindi) "🗑️ मॉडल हटाएं?" else "🗑️ Delete Model?", fontWeight = FontWeight.Bold, color = TextPrimary) },
            text = { Text(if (isHindi) "क्या आप वाकई '$mid' मॉडल फ़ाइल हटाना चाहते हैं? ऑफ़लाइन उपयोग के लिए इसे दोबारा डाउनलोड करना होगा।" else "Are you sure you want to delete model '$mid' from storage? You will need to download it again for offline usage.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteModel(mid)
                        modelToDelete = null
                        Toast.makeText(context, if (isHindi) "मॉडल हटाया गया" else "Model deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "हटाएं" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { modelToDelete = null }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ── Top Screen Header with Version Badge (Screen Title, NOT in Section 0) ──
        item(key = "screen_title_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = AppStrings.get("settings_title", isHindi),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Echo Intelligence • Voice & Offline Task Manager",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = PrimaryBlue.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { showWhatsNewDialog = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "v${AppConstants.APP_VERSION}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryBlue
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = Icons.Default.NewReleases,
                            contentDescription = "Release Notes",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 0: ⚙️ GENERAL & SYSTEM SETTINGS
        // =========================================================================
        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_section_0") {
            SettingsStickyHeader(
                title = if (isAdvanced) "0. General & System Settings" else (if (isHindi) "0. सामान्य एवं आवाज़ सेटिंग्स" else "0. General & Voice Settings"),
                summaryBadge = if (userPreferences.settingsMode == SettingsMode.SMART) "Smart Mode" else "Advanced",
                badgeColor = PrimaryBlue,
                icon = Icons.Default.Tune,
                isExpanded = isGeneralExpanded,
                onToggle = { isGeneralExpanded = !isGeneralExpanded }
            )
        }

        if (isGeneralExpanded) {
            item(key = "content_section_0") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // ── 🌐 App Language Selector (Clean English without flag) ────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Language,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("app_language", isHindi),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }

                                val isEnglish = userPreferences.appLanguage != "hi"
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isEnglish) PrimaryBlue else Color.Transparent,
                                        border = BorderStroke(1.dp, if (isEnglish) PrimaryBlue else CardBorder),
                                        modifier = Modifier.clickable {
                                            viewModel.updateAppLanguage("en")
                                            Toast.makeText(context, "App language set to English", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "English",
                                            fontWeight = FontWeight.Bold,
                                            color = if (isEnglish) DarkBackground else TextPrimary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (!isEnglish) PrimaryBlue else Color.Transparent,
                                        border = BorderStroke(1.dp, if (!isEnglish) PrimaryBlue else CardBorder),
                                        modifier = Modifier.clickable {
                                            viewModel.updateAppLanguage("hi")
                                            Toast.makeText(context, "ऐप की भाषा हिन्दी सेट की गई", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text(
                                            text = "🇮🇳 हिन्दी",
                                            fontWeight = FontWeight.Bold,
                                            color = if (!isEnglish) DarkBackground else TextPrimary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // ── 🧠 AI Output Language Selector ────────────────────────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AccentPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "एआई भाषा" else "AI Language",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(
                                        Triple("auto", "Auto", "ऑटो"),
                                        Triple("hi", "हिन्दी", "हिन्दी"),
                                        Triple("en", "English", "English")
                                    ).forEach { (langCode, engLabel, hiLabel) ->
                                        val isSelected = userPreferences.aiLanguage == langCode
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) AccentPurple else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                            modifier = Modifier.clickable {
                                                viewModel.updateAiLanguage(langCode)
                                                Toast.makeText(context, if (isHindi) "एआई भाषा: $hiLabel" else "AI Language: $engLabel", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = if (isHindi) hiLabel else engLabel,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── ⚡ Operation Mode: Compact Segmented Select Button (After AI Language) ────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (userPreferences.settingsMode == SettingsMode.SMART) Icons.Default.Bolt else Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = if (userPreferences.settingsMode == SettingsMode.SMART) PrimaryBlue else AccentPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = AppStrings.get("mode_selection", isHindi),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    val currentMode = userPreferences.settingsMode
                                    listOf(
                                        Triple(SettingsMode.SMART, "⚡ Smart", PrimaryBlue),
                                        Triple(SettingsMode.ADVANCED, "⚙️ Advanced", AccentPurple),
                                        Triple(SettingsMode.DEVELOPER, "🛠️ Dev", Color(0xFF10B981))
                                    ).forEach { (mode, label, activeColor) ->
                                        val isSel = currentMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSel) activeColor else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isSel) activeColor else CardBorder),
                                            modifier = Modifier.clickable {
                                                viewModel.updateSettingsMode(mode)
                                                Toast.makeText(context, "Mode switched to: ${mode.label}", Toast.LENGTH_SHORT).show()
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSel) DarkBackground else TextPrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 🔄 Inverse Recordings Sorting (Oldest at top, reverse WhatsApp-style scroll) ────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("⇅", fontSize = 14.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "रिकॉर्डिंग उलटी क्रमबद्ध करें" else "Inverse Recordings Sort",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = if (isHindi) "पुरानी रिकॉर्डिंग ऊपर व चैट (व्हाट्सएप) जैसा उलटा स्क्रॉल" else "Oldest at top, reverse WhatsApp-style history scroll",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = userPreferences.inverseRecordingsSort,
                                    onCheckedChange = { viewModel.updateInverseRecordingsSort(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = PrimaryBlue,
                                        checkedTrackColor = PrimaryBlue.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        // ── 📅 Calendar App for "Mujhe Yaad Dilana" Reminders ──────────
                        val isCalendarConfigVisible = userPreferences.isDeveloperMode ||
                                (userPreferences.settingsMode == SettingsMode.ADVANCED && userPreferences.devAdvancedVisibleSettings.contains("sec_calendar_config"))
                        if (isCalendarConfigVisible) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (userPreferences.isDeveloperMode) {
                                                DevModeVisibilityCheckbox(
                                                    settingKey = "sec_calendar_config",
                                                    isCurrentlyActive = true,
                                                    isAdvancedVisible = userPreferences.devAdvancedVisibleSettings.contains("sec_calendar_config"),
                                                    onToggle = { enable ->
                                                        viewModel.toggleDevAdvancedVisibleSetting("sec_calendar_config", enable)
                                                    }
                                                )
                                            }
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHindi) "📅 कैलेंडर ऐप (मुझे याद दिलाना)" else "📅 Calendar App (Reminders)",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }

                                        val activeCalLabel = when (userPreferences.selectedCalendarApp) {
                                            UserPreferences.CALENDAR_APP_GOOGLE -> "Google"
                                            UserPreferences.CALENDAR_APP_SAMSUNG -> "Samsung"
                                            else -> "Default"
                                        }
                                        Text(
                                            text = activeCalLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF10B981)
                                        )
                                    }

                                    Text(
                                        text = if (isHindi) "\"मुझे याद दिलाना\" बोलने पर चुने गए कैलेंडर ऐप में रिमाइंडर सेट होगा।" else "Voice reminders (\"mujhe yaad dilana\") will be scheduled into this calendar app.",
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )

                                    val calendarOptions = listOf(
                                        Triple(UserPreferences.CALENDAR_APP_GOOGLE, "Google Calendar", "com.google.android.calendar"),
                                        Triple(UserPreferences.CALENDAR_APP_SAMSUNG, "Samsung Calendar", "com.samsung.android.calendar"),
                                        Triple(UserPreferences.CALENDAR_APP_DEFAULT, "System Default", "Default calendar intent")
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        calendarOptions.forEach { (calKey, label, _) ->
                                            val isSel = userPreferences.selectedCalendarApp == calKey
                                            val isOptVisibleInAdv = userPreferences.devAdvancedVisibleSettings.contains("cal_$calKey") || isSel
                                            if (userPreferences.isDeveloperMode || isOptVisibleInAdv) {
                                                Surface(
                                                    shape = RoundedCornerShape(8.dp),
                                                    color = if (isSel) Color(0xFF10B981).copy(alpha = 0.2f) else Color.Transparent,
                                                    border = BorderStroke(1.dp, if (isSel) Color(0xFF10B981) else CardBorder),
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable {
                                                            viewModel.updateSelectedCalendarApp(calKey)
                                                            Toast.makeText(context, "Calendar set to: $label", Toast.LENGTH_SHORT).show()
                                                        }
                                                ) {
                                                    Row(
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp),
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.Center
                                                    ) {
                                                        if (userPreferences.isDeveloperMode) {
                                                            DevModeVisibilityCheckbox(
                                                                settingKey = "cal_$calKey",
                                                                isCurrentlyActive = isSel,
                                                                isAdvancedVisible = userPreferences.devAdvancedVisibleSettings.contains("cal_$calKey"),
                                                                onToggle = { enable ->
                                                                    viewModel.toggleDevAdvancedVisibleSetting("cal_$calKey", enable)
                                                                }
                                                            )
                                                        }
                                                        Text(
                                                            text = label,
                                                            fontSize = 10.sp,
                                                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                                            color = if (isSel) Color(0xFF10B981) else TextPrimary,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── 🏠 Home Screen Selector (Smart & Advanced Modes) ───────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Home,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "होम स्क्रीन" else "Home Screen",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = if (userPreferences.settingsMode == SettingsMode.SMART) {
                                            if (isHindi) "2-टैब नेविगेशन" else "2-Tab Navigation"
                                        } else {
                                            if (isHindi) "प्रारंभिक स्क्रीन" else "Start Screen"
                                        },
                                        fontSize = 10.sp,
                                        color = TextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                val homeOptions = if (userPreferences.settingsMode == SettingsMode.SMART) {
                                    listOf(
                                        Triple("DUMB_ASSISTANT", "🤖 Madhu Assistant", "🤖 मधु सहायक"),
                                        Triple("RECORDER", "🎙️ Audio Recorder", "🎙️ ऑडियो रिकॉर्डर"),
                                        Triple("AI", "✨ Ask AI", "✨ एआई सहायक")
                                    )
                                } else {
                                    listOf(
                                        Triple("DUMB_ASSISTANT", "🤖 Madhu", "🤖 मधु"),
                                        Triple("DASHBOARD", "📋 Tasks", "📋 कार्य सूची"),
                                        Triple("RECORDER", "🎙️ Recorder", "🎙️ रिकॉर्डर"),
                                        Triple("AI", "✨ Ask AI", "✨ एआई"),
                                        Triple("MEDIA", "📁 Media", "📁 मीडिया")
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    homeOptions.forEach { (key, engLabel, hiLabel) ->
                                        val isCurrentHome = userPreferences.homeTab == key
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isCurrentHome) PrimaryBlue else Color.Transparent,
                                            border = BorderStroke(1.dp, if (isCurrentHome) PrimaryBlue else CardBorder),
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateHomeTab(key)
                                                    // Ensure the selected home tab is enabled
                                                    when (key) {
                                                        "DUMB_ASSISTANT" -> viewModel.updateShowAssistantTab(true)
                                                        "DASHBOARD" -> viewModel.updateShowDashboardTab(true)
                                                        "RECORDER" -> viewModel.updateShowRecorderTab(true)
                                                        "AI" -> viewModel.updateShowAiTab(true)
                                                        "MEDIA" -> viewModel.updateShowMediaTab(true)
                                                    }
                                                    Toast.makeText(context, if (isHindi) "होम: $hiLabel" else "Home set to: $engLabel", Toast.LENGTH_SHORT).show()
                                                }
                                        ) {
                                            Text(
                                                text = if (isHindi) hiLabel else engLabel,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isCurrentHome) DarkBackground else TextPrimary,
                                                maxLines = 1,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 📑 App Tabs Reordering with Hamburger (Smart Mode) ─────────
                        AppTabReorderingCard(
                            userPreferences = userPreferences,
                            viewModel = viewModel,
                            isHindi = isHindi
                        )

                        // ── 🔊 Voice Speech Speed Slider (Smart & Advanced) ───────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Speed,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "बोलने की गति (TTS Speed)" else "Voice Speech Speed",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "${String.format(java.util.Locale.US, "%.2f", userPreferences.ttsSpeed)}x",
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue,
                                        fontSize = 12.sp
                                    )
                                }
                                Slider(
                                    value = userPreferences.ttsSpeed,
                                    onValueChange = { viewModel.updateTtsSpeed(it) },
                                    valueRange = 0.5f..2.0f,
                                    steps = 14,
                                    colors = SliderDefaults.colors(
                                        thumbColor = PrimaryBlue,
                                        activeTrackColor = PrimaryBlue
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("0.5x (Slow)", fontSize = 9.sp, color = TextMuted)
                                    Text("1.0x (Normal)", fontSize = 9.sp, color = TextMuted)
                                    Text("2.0x (Fast)", fontSize = 9.sp, color = TextMuted)
                                }
                            }
                        }

                        // ── 🎙️ Madhu Voice Interruption Sensitivity (Noise Gate) ────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.GraphicEq,
                                            contentDescription = null,
                                            tint = AccentPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "वॉयस रुकावट संवेदनशीलता" else "Bot Voice Interruption Gate",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Text(
                                        text = "${userPreferences.botInterruptionNoiseGateDb.toInt()} dB",
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = if (isHindi)
                                        "बोलते समय रोके जाने के लिए न्यूनतम आवाज़। बैकग्राउंड शोर या स्पीकर गूंज से बचने के लिए इसे -18 dB या -16 dB सेट करें।"
                                    else
                                        "Minimum volume to interrupt Madhu while speaking. Set higher (-18 dB to -16 dB) to prevent speaker echo or room noise from cutting off the bot.",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                Slider(
                                    value = userPreferences.botInterruptionNoiseGateDb.toFloat(),
                                    onValueChange = { viewModel.updateBotInterruptionNoiseGateDb(it.toInt()) },
                                    valueRange = -35f..-15f,
                                    steps = 19,
                                    colors = SliderDefaults.colors(
                                        thumbColor = AccentPurple,
                                        activeTrackColor = AccentPurple
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(if (isHindi) "-35 dB (अधिक संवेदनशील)" else "-35 dB (Sensitive)", fontSize = 9.sp, color = TextMuted)
                                    Text("-20 dB (Default)", fontSize = 9.sp, color = AccentPurple)
                                    Text(if (isHindi) "-15 dB (सख्त / तेज़ आवाज़)" else "-15 dB (Strict/Loud)", fontSize = 9.sp, color = TextMuted)
                                }
                            }
                        }

                        // ── 🌐 Live Internet Search Toggle (DuckDuckGo) ─────────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TravelExplore,
                                        contentDescription = null,
                                        tint = Color(0xFF06B6D4),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            text = if (isHindi) "🌐 लाइव इंटरनेट सर्च (Web Search)" else "🌐 Live Web Search",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                        Text(
                                            text = if (isHindi)
                                                "सवालों के ताज़ा जवाबों के लिए रीयल-टाइम इंटरनेट खोज सक्षम करें।"
                                            else
                                                "Allows Madhu and Ask AI to search the internet for live info.",
                                            fontSize = 10.sp,
                                            color = TextMuted
                                        )
                                    }
                                }
                                Switch(
                                    checked = userPreferences.isWebSearchEnabled,
                                    onCheckedChange = { viewModel.updateWebSearchEnabled(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = DarkBackground,
                                        checkedTrackColor = Color(0xFF06B6D4)
                                    )
                                )
                            }
                        }

                        // ── ⚙️ Advanced System Controls (Visible only in Advanced Mode) ──
                        if (isAdvanced) {
                            // ── 📑 App Sections & Tab Visibility Checkboxes ──
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = AccentPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = if (isHindi) "ऐप अनुभाग और टैब दृश्यता" else "App Sections & Tab Visibility",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                            Text(
                                                text = if (isHindi) "होम और सेटिंग्स टैब अक्षम नहीं किए जा सकते" else "Home and Settings tabs cannot be disabled",
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    // Sections list:
                                    // 1. Madhu Assistant
                                    val isAssistantHome = userPreferences.homeTab == "DUMB_ASSISTANT"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAssistantHome) "🤖 Madhu Assistant (Home - Locked)" else "🤖 Madhu Assistant",
                                            fontSize = 11.sp,
                                            color = if (isAssistantHome) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isAssistantHome) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isAssistantHome || userPreferences.showAssistantTab,
                                            enabled = !isAssistantHome,
                                            onCheckedChange = { viewModel.updateShowAssistantTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                        )
                                    }

                                    // 2. Tasks Dashboard
                                    val isDashboardHome = userPreferences.homeTab == "DASHBOARD"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isDashboardHome) "📋 Tasks Dashboard (Home - Locked)" else "📋 Tasks Dashboard",
                                            fontSize = 11.sp,
                                            color = if (isDashboardHome) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isDashboardHome) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isDashboardHome || userPreferences.showDashboardTab,
                                            enabled = !isDashboardHome,
                                            onCheckedChange = { viewModel.updateShowDashboardTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                        )
                                    }

                                    // 3. Audio Recorder
                                    val isRecorderHome = userPreferences.homeTab == "RECORDER"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isRecorderHome) "🎙️ Audio Recorder (Home - Locked)" else "🎙️ Audio Recorder",
                                            fontSize = 11.sp,
                                            color = if (isRecorderHome) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isRecorderHome) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isRecorderHome || userPreferences.showRecorderTab,
                                            enabled = !isRecorderHome,
                                            onCheckedChange = { viewModel.updateShowRecorderTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                        )
                                    }

                                    // 4. Ask AI
                                    val isAiHome = userPreferences.homeTab == "AI"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isAiHome) "✨ Ask AI (Home - Locked)" else "✨ Ask AI",
                                            fontSize = 11.sp,
                                            color = if (isAiHome) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isAiHome) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isAiHome || userPreferences.showAiTab,
                                            enabled = !isAiHome,
                                            onCheckedChange = { viewModel.updateShowAiTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = AccentPurple)
                                        )
                                    }

                                    // 5. Media Transcripts
                                    val isMediaHome = userPreferences.homeTab == "MEDIA"
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (isMediaHome) "📁 Media Transcripts (Home - Locked)" else "📁 Media Transcripts",
                                            fontSize = 11.sp,
                                            color = if (isMediaHome) PrimaryBlue else TextPrimary,
                                            fontWeight = if (isMediaHome) FontWeight.Bold else FontWeight.Normal
                                        )
                                        Checkbox(
                                            checked = isMediaHome || userPreferences.showMediaTab,
                                            enabled = !isMediaHome,
                                            onCheckedChange = { viewModel.updateShowMediaTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                        )
                                    }

                                    // 6. System Logs
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "📜 System Logs",
                                            fontSize = 11.sp,
                                            color = TextPrimary
                                        )
                                        Checkbox(
                                            checked = userPreferences.showLogsTab,
                                            onCheckedChange = { viewModel.updateShowLogsTab(it) },
                                            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                        )
                                    }

                                    // 7. Settings (Always Active)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "⚙️ Settings (Always Active)",
                                            fontSize = 11.sp,
                                            color = TextMuted,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Checkbox(
                                            checked = true,
                                            enabled = false,
                                            onCheckedChange = {},
                                            colors = CheckboxDefaults.colors(disabledCheckedColor = PrimaryBlue.copy(alpha = 0.5f))
                                        )
                                    }
                                }
                            }

                            // ── 📜 Log File Extension Selector ───────────────────────────
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Description,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "लॉग एक्सटेंशन" else "Log Extension",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("log", "txt", "md").forEach { ext ->
                                            val isSelected = userPreferences.logFileExtension.equals(ext, ignoreCase = true)
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) PrimaryBlue else Color.Transparent,
                                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                                modifier = Modifier.clickable {
                                                    viewModel.updateLogFileExtension(ext)
                                                    Toast.makeText(context, if (isHindi) "लॉग एक्सटेंशन: .$ext" else "Log extension set to .$ext", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = ".$ext",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DarkBackground else TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── 🎙️ Recorder Layout Mode Selector ──────────────────────────
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.GraphicEq,
                                                contentDescription = null,
                                                tint = PrimaryBlue,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHindi) "रिकॉर्डर लेआउट" else "Recorder Layout",
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf(
                                            Triple("BOTH_TABS", "Dual (Both)", "दोनों"),
                                            Triple("BOTTOM_MEDIA_ONLY", "Media Bottom", "मीडिया नीचे"),
                                            Triple("DEDICATED_TAB_ONLY", "Tab Only", "केवल टैब")
                                        ).forEach { (mode, engLabel, hiLabel) ->
                                            val isSelected = userPreferences.recorderLayoutMode == mode
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) PrimaryBlue else Color.Transparent,
                                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clickable {
                                                        viewModel.updateRecorderLayoutMode(mode)
                                                        Toast.makeText(context, if (isHindi) "रिकॉर्डर लेआउट: $hiLabel" else "Recorder layout: $engLabel", Toast.LENGTH_SHORT).show()
                                                    }
                                            ) {
                                                Text(
                                                    text = if (isHindi) hiLabel else engLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DarkBackground else TextPrimary,
                                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                                    modifier = Modifier.padding(vertical = 5.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // ── ⏹️ Default Abort Selection Mode ───────────────────────────
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteSweep,
                                            contentDescription = null,
                                            tint = ActiveRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "एबॉर्शन डिफ़ॉल्ट" else "Default Abort",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 12.sp
                                        )
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf(
                                            Triple("ALL_FILES", "All Chunks", "सभी फ़ाइलें"),
                                            Triple("LAST_FILE_ONLY", "Last Only", "केवल अंतिम")
                                        ).forEach { (opt, engLabel, hiLabel) ->
                                            val isSelected = userPreferences.defaultAbortSelection == opt
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) ActiveRed.copy(alpha = 0.2f) else Color.Transparent,
                                                border = BorderStroke(1.dp, if (isSelected) ActiveRed else CardBorder),
                                                modifier = Modifier.clickable {
                                                    viewModel.updateDefaultAbortSelection(opt)
                                                    Toast.makeText(context, if (isHindi) "डिफ़ॉल्ट एबॉर्शन: $hiLabel" else "Default abort: $engLabel", Toast.LENGTH_SHORT).show()
                                                }
                                            ) {
                                                Text(
                                                    text = if (isHindi) hiLabel else engLabel,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) ActiveRed else TextPrimary,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
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

        // =========================================================================
        // CATEGORY 1: 📁 STORAGE DIRECTORIES & MEDIA MANAGEMENT (Advanced Only)
        // =========================================================================
        if (isAdvanced) {
            val targetBasePath = userPreferences.baseFolderPath
            val sourceBasePath = userPreferences.lastExportSourcePath
            val activeBaseDir = AppStorageManager.getBaseDirectory(context, targetBasePath.ifBlank { null })

            @OptIn(ExperimentalFoundationApi::class)
            stickyHeader(key = "header_storage") {
            SettingsStickyHeader(
                title = AppStrings.get("section_storage", isHindi),
                summaryBadge = if (targetBasePath.isNotBlank()) "Custom Path" else "Default (Downloads)",
                badgeColor = PrimaryBlue,
                icon = Icons.Default.Folder,
                isExpanded = isStorageExpanded,
                onToggle = { isStorageExpanded = !isStorageExpanded }
            )
        }

        if (isStorageExpanded) {
            item(key = "content_storage") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Manage your active storage directory, browse concrete subfolders, or restore existing files from disk.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // ── Card 1: Active Base Storage Directory ───────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (targetBasePath.isNotBlank()) PrimaryBlue else CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🎯 Base Storage Directory",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = PrimaryBlue
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (targetBasePath.isNotBlank()) PrimaryBlue.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
                                        border = BorderStroke(1.dp, if (targetBasePath.isNotBlank()) PrimaryBlue else CardBorder)
                                    ) {
                                        Text(
                                            text = if (targetBasePath.isNotBlank()) "Custom Location" else "Downloads (Default)",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (targetBasePath.isNotBlank()) PrimaryBlue else TextSecondary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = activeBaseDir.absolutePath,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { targetFolderPickerLauncher.launch(null) },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Change", fontSize = 11.sp, color = PrimaryBlue)
                                    }
                                    if (targetBasePath.isNotBlank()) {
                                        OutlinedButton(
                                            onClick = {
                                                viewModel.updateBaseFolderPath("")
                                                Toast.makeText(context, "Reset base folder to default Downloads", Toast.LENGTH_SHORT).show()
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                            modifier = Modifier.weight(1f).height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextSecondary)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Reset", fontSize = 11.sp, color = TextSecondary)
                                        }
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            AppStorageManager.openFolderWithIntent(context, activeBaseDir)
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(14.dp), tint = TextPrimary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Open Base", fontSize = 11.sp, color = TextPrimary)
                                    }
                                }
                            }
                        }

                        // ── Card 2: Concrete Subfolders List ────────────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "📂 Concrete Storage Subdirectories",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Each category has its own concrete subfolder inside your base directory. Tap 'Open' to inspect files directly:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(2.dp))

                                val subfolders = listOf(
                                    Triple("🎵 Audio Recordings", "audio", AppStorageManager.getAudioDirectory(context, targetBasePath.ifBlank { null })),
                                    Triple("📝 Transcriptions & Subtitles", "transcriptions", AppStorageManager.getTranscriptionsDirectory(context, targetBasePath.ifBlank { null })),
                                    Triple("📋 AI Notes & Tasks", "notes", AppStorageManager.getNotesDirectory(context, targetBasePath.ifBlank { null })),
                                    Triple("🤖 Voice & AI Models", "models", AppStorageManager.getModelsDirectory(context, targetBasePath.ifBlank { null })),
                                    Triple("📜 System Session Logs", "logs", AppStorageManager.getLogsDirectory(context, targetBasePath.ifBlank { null }))
                                )

                                subfolders.forEach { (label, folderName, folderFile) ->
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                                Text(
                                                    text = label,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                Text(
                                                    text = "${activeBaseDir.name}/$folderName/",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = PrimaryBlue
                                                )
                                            }
                                            OutlinedButton(
                                                onClick = {
                                                    AppStorageManager.openFolderWithIntent(context, folderFile)
                                                },
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Open", fontSize = 11.sp, color = PrimaryBlue)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ── Card 3: Storage Scanner & File Recovery ─────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔄 Scan & Restore Files from Storage",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                }
                                Text(
                                    text = "Scans device storage (Downloads/App data/poc/yesboss, custom source folders, and internal app memory) to discover any audio recordings (.wav/.m4a/.mp3) and transcriptions (.md), restoring them immediately into your app UI.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )
                                Button(
                                    onClick = {
                                        viewModel.syncStorageFilesWithDatabase { success, message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isSyncingFiles,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                                ) {
                                    if (isSyncingFiles) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkBackground)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scanning & Restoring Storage...", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Scan & Restore Files Now", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // ── Card 4: Media Migration & Sync ──────────────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = SurfaceDark,
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "📦 Media Migration & Sync",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryBlue
                                )
                                Text(
                                    text = "Copy recordings, transcripts, and models from a source location to your target destination directory.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "Source Folder (Import From):",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = AccentPurple
                                            )
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                if (sourceBasePath.isNotBlank()) {
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.updateLastExportSourcePath("")
                                                            Toast.makeText(context, "Source reset to internal app directory", Toast.LENGTH_SHORT).show()
                                                        },
                                                        shape = RoundedCornerShape(6.dp),
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(24.dp)
                                                    ) {
                                                        Text("Reset", fontSize = 10.sp, color = TextSecondary)
                                                    }
                                                }
                                                OutlinedButton(
                                                    onClick = { sourceFolderPickerLauncher.launch(null) },
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(24.dp)
                                                ) {
                                                    Text("Select", fontSize = 10.sp, color = AccentPurple)
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (sourceBasePath.isNotBlank()) sourceBasePath else "Internal App Directory (Default)",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (sourceBasePath.isNotBlank()) TextPrimary else TextMuted
                                        )
                                    }
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            text = "Destination Folder (Save To):",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = activeBaseDir.absolutePath,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TextPrimary
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        viewModel.exportAllMedia { success, message ->
                                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        }
                                    },
                                    enabled = !isExporting,
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    if (isExporting) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DarkBackground)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Migrating Files...", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    } else {
                                        Icon(Icons.Default.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp), tint = DarkBackground)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Migrate / Export Media to Destination", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    }
                                }
                            }
                        }

                        // ── Card 5: Audio Cache & Storage Usage (Moved to Section 1) ───
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("🎵 Audio Storage Usage", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(String.format(java.util.Locale.US, "%.2f MB used in storage", storageMb), fontSize = 11.sp, color = TextMuted)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.clearAudioCache()
                                            Toast.makeText(context, "Audio cache cleared", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Clear Cache", fontSize = 11.sp, color = ActiveRed)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        } // End if (isAdvanced) for Category 1

        // =========================================================================
        // CATEGORY 2: 🎨 APPEARANCE & COLOR THEMES (Visible in Both Modes)
        // =========================================================================
        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_themes") {
            SettingsStickyHeader(
                title = if (isAdvanced) "2. Themes & Typography" else (if (isHindi) "1. थीम और रूप-रंग" else "1. Themes & Appearance"),
                summaryBadge = userPreferences.activeTheme.replace("_", " "),
                badgeColor = AccentPurple,
                icon = Icons.Default.Palette,
                isExpanded = isThemesExpanded,
                onToggle = { isThemesExpanded = !isThemesExpanded }
            )
        }

        if (isThemesExpanded) {
            item(key = "content_themes") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Select from popular open community color themes. Switches immediately across the app.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        // HOME SCREEN THEME SELECTOR
                        Text(
                            text = if (isHindi) "🏠 होम स्क्रीन थीम (Madhu Tab Theme)" else "🏠 Home Screen Theme (Madhu Tab)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(UserPreferences.HOME_THEME_CHARCOAL, "Obsidian", Color(0xFF0F141C)),
                                Triple(UserPreferences.HOME_THEME_MIDNIGHT, "Midnight", Color(0xFF0A1224)),
                                Triple(UserPreferences.HOME_THEME_EMERALD, "Emerald", Color(0xFF071B14)),
                                Triple(UserPreferences.HOME_THEME_CARBON, "Carbon", Color(0xFF14151A))
                            ).forEach { (tKey, tLabel, tColor) ->
                                val isCur = userPreferences.homeTheme == tKey
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isCur) PrimaryBlue.copy(alpha = 0.2f) else SurfaceDark,
                                    border = BorderStroke(1.dp, if (isCur) PrimaryBlue else CardBorder),
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable {
                                            viewModel.updateHomeTheme(tKey)
                                            Toast.makeText(context, "Home Theme: $tLabel", Toast.LENGTH_SHORT).show()
                                        }
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.padding(vertical = 6.dp)
                                    ) {
                                        Surface(
                                            shape = CircleShape,
                                            color = tColor,
                                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                                            modifier = Modifier.size(16.dp)
                                        ) {}
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = tLabel,
                                            fontSize = 10.sp,
                                            fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isCur) PrimaryBlue else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))

                        val visibleThemes = remember(userPreferences.settingsMode, userPreferences.devAdvancedVisibleSettings, userPreferences.activeTheme) {
                            when (userPreferences.settingsMode) {
                                SettingsMode.SMART -> AppThemes.ALL_THEMES.filter {
                                    it.id in listOf("GRADIENT_BLACK", "MIDNIGHT_BLUE", "DARK", "LIGHT") || it.id == userPreferences.activeTheme
                                }
                                SettingsMode.ADVANCED -> AppThemes.ALL_THEMES.filter {
                                    it.id == userPreferences.activeTheme || userPreferences.devAdvancedVisibleSettings.contains("theme_${it.id}")
                                }
                                SettingsMode.DEVELOPER -> AppThemes.ALL_THEMES
                            }
                        }

                        visibleThemes.forEach { themeOption ->
                            val isSelected = userPreferences.activeTheme == themeOption.id

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else SurfaceDark,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) themeOption.primaryColor else CardBorder
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.updateActiveTheme(themeOption.id)
                                        Toast.makeText(context, "Theme switched to ${themeOption.name}", Toast.LENGTH_SHORT).show()
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                            if (userPreferences.isDeveloperMode) {
                                                DevModeVisibilityCheckbox(
                                                    settingKey = "theme_${themeOption.id}",
                                                    isCurrentlyActive = isSelected,
                                                    isAdvancedVisible = userPreferences.devAdvancedVisibleSettings.contains("theme_${themeOption.id}"),
                                                    onToggle = { enable ->
                                                        viewModel.toggleDevAdvancedVisibleSetting("theme_${themeOption.id}", enable)
                                                    }
                                                )
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                Surface(shape = CircleShape, color = themeOption.primaryColor, modifier = Modifier.size(16.dp)) {}
                                                Surface(shape = CircleShape, color = themeOption.accentColor, modifier = Modifier.size(16.dp)) {}
                                                Surface(shape = CircleShape, color = themeOption.surfaceColor, modifier = Modifier.size(16.dp)) {}
                                            }

                                            Spacer(modifier = Modifier.width(10.dp))

                                            Column {
                                                Text(
                                                    text = themeOption.name,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) themeOption.primaryColor else TextPrimary
                                                )
                                                Text(
                                                    text = themeOption.description,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }
                                        }

                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { viewModel.updateActiveTheme(themeOption.id) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = themeOption.primaryColor,
                                                unselectedColor = TextMuted
                                            )
                                        )
                                    }

                                    if (themeOption.id == "CUSTOM") {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { showCustomThemeDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, PrimaryBlue),
                                            modifier = Modifier.fillMaxWidth().height(32.dp)
                                        ) {
                                            Icon(Icons.Default.FormatPaint, contentDescription = null, modifier = Modifier.size(14.dp), tint = PrimaryBlue)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("✏️ Customize Theme Colors (Telegram Style)", fontSize = 11.sp, color = PrimaryBlue, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // ── 🔤 Font Size & Typography Style ────────────────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = AppStrings.get("font_size", isHindi),
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                }

                                Text("Text Size Scale Factor:", fontSize = 11.sp, color = TextMuted)
                                val scales = listOf(
                                    Pair(0.85f, AppStrings.get("font_small", isHindi)),
                                    Pair(1.0f, AppStrings.get("font_default", isHindi)),
                                    Pair(1.15f, AppStrings.get("font_large", isHindi)),
                                    Pair(1.3f, "Extra")
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    scales.forEach { (scale, label) ->
                                        val isSelected = Math.abs(userPreferences.fontScale - scale) < 0.05f
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                            modifier = Modifier.weight(1f).clickable { viewModel.updateFontScale(scale) }
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 6.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = label,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) DarkBackground else TextPrimary,
                                                    fontSize = 11.sp
                                                )
                                                Text(
                                                    text = "${scale}x",
                                                    fontSize = 9.sp,
                                                    color = if (isSelected) DarkBackground.copy(alpha = 0.8f) else TextMuted
                                                )
                                            }
                                        }
                                    }
                                }

                                Text("Font Weight Mode (Comfort / Thin / Default):", fontSize = 11.sp, color = TextMuted)
                                val modes = listOf(
                                    Pair("DEFAULT", AppStrings.get("font_mode_regular", isHindi)),
                                    Pair("THIN", AppStrings.get("font_mode_thin", isHindi)),
                                    Pair("BOLD", AppStrings.get("font_mode_bold", isHindi))
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    modes.forEach { (mode, label) ->
                                        val isSelected = userPreferences.fontMode.equals(mode, ignoreCase = true)
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surface,
                                            border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                            modifier = Modifier.weight(1f).clickable { viewModel.updateFontMode(mode) }
                                        ) {
                                            Text(
                                                text = label,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else TextPrimary,
                                                fontSize = 11.sp,
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORIES 3-7: ADVANCED TECHNICAL DIALS & ENGINES (Advanced Mode Only)
        // =========================================================================
        if (isAdvanced) {
            // =========================================================================
            // CATEGORY 3: 🎙️ AUDIO RECORDING & CHUNKING
            // =========================================================================
            @OptIn(ExperimentalFoundationApi::class)
            stickyHeader(key = "header_audio") {
            SettingsStickyHeader(
                title = AppStrings.get("section_audio", isHindi),
                summaryBadge = "${userPreferences.sampleRate / 1000} kHz • ${userPreferences.bufferLengthSeconds}s",
                badgeColor = Color(0xFFF59E0B),
                icon = Icons.Default.GraphicEq,
                isExpanded = isAudioExpanded,
                onToggle = { isAudioExpanded = !isAudioExpanded }
            )
        }

        if (isAudioExpanded) {
            item(key = "content_audio") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Sample Rate (Audio Quality):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        val rates = listOf(
                            Triple(UserPreferences.QUALITY_TINIEST, "8 kHz", "Tiniest"),
                            Triple(UserPreferences.QUALITY_SPEECH, "16 kHz", "Speech"),
                            Triple(UserPreferences.QUALITY_STANDARD, "22 kHz", "Standard"),
                            Triple(UserPreferences.QUALITY_HIGH, "44 kHz", "High")
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            rates.forEach { (rate, label, desc) ->
                                val isSelected = userPreferences.sampleRate == rate
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).clickable { viewModel.updateSampleRate(rate) }
                                ) {
                                    Column(modifier = Modifier.padding(vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(label, fontWeight = FontWeight.Bold, color = if (isSelected) DarkBackground else TextPrimary, fontSize = 11.sp)
                                        Text(desc, fontSize = 9.sp, color = if (isSelected) DarkBackground.copy(alpha = 0.8f) else TextMuted)
                                    }
                                }
                            }
                        }

                        val durationHuman = when {
                            userPreferences.bufferLengthSeconds < 60 -> "${userPreferences.bufferLengthSeconds}s"
                            userPreferences.bufferLengthSeconds < 3600 -> "${userPreferences.bufferLengthSeconds / 60}m ${if (userPreferences.bufferLengthSeconds % 60 != 0) "${userPreferences.bufferLengthSeconds % 60}s" else ""}".trim()
                            else -> "${userPreferences.bufferLengthSeconds / 3600}h ${if ((userPreferences.bufferLengthSeconds % 3600) / 60 != 0) "${(userPreferences.bufferLengthSeconds % 3600) / 60}m" else ""}".trim()
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Audio Split Duration (Buffer Length):", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Splits continuous audio into chunks from seconds up to hours", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text(durationHuman, fontWeight = FontWeight.Bold, color = AccentPurple, fontSize = 14.sp)
                        }

                        val chunkPresets = listOf(
                            15 to "15s",
                            30 to "30s",
                            60 to "1m",
                            300 to "5m",
                            900 to "15m",
                            1800 to "30m",
                            3600 to "1h",
                            7200 to "2h"
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            chunkPresets.forEach { (seconds, label) ->
                                val isSelected = userPreferences.bufferLengthSeconds == seconds
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) AccentPurple else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).clickable { viewModel.updateBufferLength(seconds) }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else TextPrimary,
                                        modifier = Modifier.padding(vertical = 6.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text("Continuous Audio Overlap Window:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Audio overlap (3s to 60s) carried to next chunk to prevent cut-offs", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Text("${userPreferences.overlapSeconds}s", fontWeight = FontWeight.Bold, color = PrimaryBlue, fontSize = 14.sp)
                        }

                        Slider(
                            value = userPreferences.overlapSeconds.toFloat().coerceIn(3f, 60f),
                            onValueChange = { viewModel.updateOverlap(it.toInt()) },
                            valueRange = 3f..60f,
                            steps = 18,
                            colors = SliderDefaults.colors(thumbColor = PrimaryBlue, activeTrackColor = PrimaryBlue)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(3, 5, 10, 15, 30, 60).forEach { ov ->
                                val isSelected = userPreferences.overlapSeconds == ov
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (isSelected) PrimaryBlue else MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f).clickable { viewModel.updateOverlap(ov) }
                                ) {
                                    Text(
                                        text = "${ov}s",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) DarkBackground else TextPrimary,
                                        modifier = Modifier.padding(vertical = 5.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        // VAD / Skip Silence
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Skip Silence (VAD)", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Only saves audio when speech is detected", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = userPreferences.skipSilence,
                                onCheckedChange = { viewModel.updateSkipSilence(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryBlue)
                            )
                        }

                        // Live Transcribe Toggle
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Live Real-Time Transcribe", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Live speech recognition on screen while recording", style = MaterialTheme.typography.labelSmall, color = TextMuted)
                            }
                            Switch(
                                checked = userPreferences.liveTranscribe,
                                onCheckedChange = { viewModel.updateLiveTranscribe(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = PrimaryBlue)
                            )
                        }

                        // ── Background Battery Exemption (Moved to Section 3) ───────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, if (!isIgnoringBattery) ActiveRed.copy(alpha = 0.5f) else CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.BatterySaver,
                                                contentDescription = null,
                                                tint = if (isIgnoringBattery) Color(0xFF10B981) else ActiveRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Background Battery Exemption", fontWeight = FontWeight.SemiBold, color = TextPrimary, fontSize = 13.sp)
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isIgnoringBattery) "Battery optimization ignored (Service will run uninterrupted in background)" else "App may be suspended or killed while recording in background",
                                            fontSize = 11.sp,
                                            color = if (isIgnoringBattery) Color(0xFF10B981) else ActiveRed
                                        )
                                    }
                                    if (!isIgnoringBattery) {
                                        Button(
                                            onClick = { viewModel.requestBatteryOptimizationExemption(context) },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = ActiveRed),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("Exempt", fontSize = 11.sp, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 4: 🤖 SPEECH-TO-TEXT (STT) MODELS & DOWNLOADS
        // =========================================================================
        val activeStt = UserPreferences.AVAILABLE_STT_MODELS.find { it.id == userPreferences.selectedSttModel }

        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_stt") {
            SettingsStickyHeader(
                title = AppStrings.get("section_stt", isHindi),
                summaryBadge = activeStt?.name ?: "Vosk Hindi",
                badgeColor = Color(0xFF10B981),
                icon = Icons.Default.SmartToy,
                isExpanded = isSttExpanded,
                onToggle = { isSttExpanded = !isSttExpanded }
            )
        }

        if (isSttExpanded) {
            item(key = "content_stt") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // ── 🌐 Spoken Speech Recognition Language ──────────────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "पहचान भाषा (Live Speech Language)" else "Live Speech Language",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = TextPrimary
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = PrimaryBlue.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = if (userPreferences.selectedLanguage == "en-IN") "English (IN)" else "Hinglish (Hybrid)",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isHindi)
                                        "Google और Vosk में हाइब्रिड हिन्दी+अंग्रेजी (Hinglish) या Indian English पहचान चुनें।"
                                    else
                                        "Select hybrid Hindi+English (Hinglish) or Indian English for live recognition.",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val langOptions = listOf(
                                        "hi-IN" to (if (isHindi) "🇮🇳 हाइब्रिड (Hinglish)" else "🇮🇳 Hinglish Hybrid"),
                                        "en-IN" to (if (isHindi) "🇮🇳 Indian English" else "🇮🇳 Indian English")
                                    )
                                    langOptions.forEach { (code, label) ->
                                        val isSelected = userPreferences.selectedLanguage == code || (code == "hi-IN" && userPreferences.selectedLanguage != "en-IN")
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (isSelected) PrimaryBlue else SurfaceDark,
                                            border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                            modifier = Modifier.weight(1f).clickable {
                                                viewModel.updateLanguage(code)
                                            }
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) DarkBackground else TextPrimary,
                                                modifier = Modifier.padding(vertical = 7.dp),
                                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ── 📥 Google Speech Offline Language Packs Shortcut ────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = PrimaryBlue.copy(alpha = 0.08f),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.35f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CloudDownload,
                                            contentDescription = null,
                                            tint = PrimaryBlue,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isHindi) "Google ऑफ़लाइन भाषा डाउनलोड" else "Google Speech Offline Languages",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = PrimaryBlue
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (isHindi)
                                        "Google वॉयस टाइपिंग सेटिंग्स खोलकर हिन्दी और English भाषा पैक डाउनलोड करें ताकि बिना इंटरनेट भी सटीक हाइब्रिड पहचान मिले।"
                                    else
                                        "Download Hindi & English offline voice packs in Google settings for fast, intelligent hybrid recognition without internet.",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { openGoogleVoiceLanguageSettings(context) },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.fillMaxWidth().height(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "📥 Google ऑफ़लाइन भाषा सेटिंग्स खोलें" else "📥 Open Google Offline Language Settings",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        val visibleSttModels = remember(userPreferences.settingsMode, userPreferences.devAdvancedVisibleSettings, userPreferences.selectedSttModel) {
                            when (userPreferences.settingsMode) {
                                SettingsMode.SMART -> UserPreferences.AVAILABLE_STT_MODELS.filter {
                                    it.id in listOf(UserPreferences.STT_MODEL_GOOGLE_SPEECH, UserPreferences.STT_MODEL_VOSK_HINDI) || it.id == userPreferences.selectedSttModel
                                }
                                SettingsMode.ADVANCED -> UserPreferences.AVAILABLE_STT_MODELS.filter {
                                    it.id == userPreferences.selectedSttModel || userPreferences.devAdvancedVisibleSettings.contains("stt_${it.id}")
                                }
                                SettingsMode.DEVELOPER -> UserPreferences.AVAILABLE_STT_MODELS
                            }
                        }

                        visibleSttModels.forEach { model ->
                            val isSelected = userPreferences.selectedSttModel == model.id
                            val isDownloaded = UserPreferences.isSttModelRequirementFulfilled(
                                context = context,
                                modelId = model.id,
                                cloudApiKey = userPreferences.cloudApiKey,
                                downloadedModels = userPreferences.downloadedModels
                            )
                            val downloadProgress = modelDownloadStates[model.id]
                            val isDownloadingOrPaused = downloadProgress?.status in listOf(
                                ModelDownloadStatus.DOWNLOADING,
                                ModelDownloadStatus.PAUSED,
                                ModelDownloadStatus.EXTRACTING
                            )
                            val isSetupExpanded = expandedSttSetupModelId == model.id || isDownloadingOrPaused

                            val onModelClick = {
                                if (isDownloaded) {
                                    viewModel.updateSttModel(model.id)
                                    Toast.makeText(context, "Selected: ${model.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    expandedSttSetupModelId = if (expandedSttSetupModelId == model.id) null else model.id
                                    Toast.makeText(
                                        context,
                                        "⚠️ ${model.name} is not installed. Please download or set it up first below.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else SurfaceDark,
                                border = BorderStroke(1.dp, if (isSelected) PrimaryBlue else CardBorder),
                                modifier = Modifier.fillMaxWidth().clickable { onModelClick() }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (userPreferences.isDeveloperMode) {
                                            DevModeVisibilityCheckbox(
                                                settingKey = "stt_${model.id}",
                                                isCurrentlyActive = isSelected,
                                                isAdvancedVisible = userPreferences.devAdvancedVisibleSettings.contains("stt_${model.id}"),
                                                onToggle = { enable ->
                                                    viewModel.toggleDevAdvancedVisibleSetting("stt_${model.id}", enable)
                                                }
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(model.name, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else TextPrimary, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = PrimaryBlue.copy(alpha = 0.15f)
                                                ) {
                                                    Text(model.tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(model.description, fontSize = 11.sp, color = TextMuted)
                                        }

                                        RadioButton(
                                            selected = isSelected && isDownloaded,
                                            onClick = { onModelClick() },
                                            colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                                        )
                                    }

                                    if (model.id == UserPreferences.STT_MODEL_GOOGLE_SPEECH && isSelected) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedButton(
                                            onClick = { openGoogleVoiceLanguageSettings(context) },
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, PrimaryBlue),
                                            modifier = Modifier.fillMaxWidth().height(32.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Icon(Icons.Default.CloudDownload, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isHindi) "📥 ऑफ़लाइन भाषा पैक डाउनलोड करें" else "📥 Download Offline Language Packs",
                                                fontSize = 11.sp,
                                                color = PrimaryBlue,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }

                                    val isOfflineDownloadable = model.id in listOf(
                                        UserPreferences.STT_MODEL_VOSK_HINDI,
                                        UserPreferences.STT_MODEL_VOSK_HINDI_LARGE,
                                        UserPreferences.STT_MODEL_VOSK_EN_IN,
                                        UserPreferences.STT_MODEL_SHERPA_ONNX_HINDI
                                    ) || !model.downloadUrl.isNullOrBlank()

                                    if (isOfflineDownloadable) {
                                        if (isDownloaded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                                                    Text("✅ Downloaded & Ready (${model.sizeText})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                                }
                                                OutlinedButton(
                                                    onClick = { modelToDelete = model.id },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Delete", fontSize = 10.sp, color = ActiveRed)
                                                }
                                            }
                                        } else if (isSetupExpanded || isSelected) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFFF59E0B).copy(alpha = 0.12f),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Text(
                                                    text = "📥 Model file required (${model.sizeText}). Use Pause/Resume/Cancel controls below or import ZIP manually.",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier.padding(8.dp)
                                                )
                                            }
                                            ModelDownloadControls(
                                                modelId = model.id,
                                                sizeText = model.sizeText,
                                                downloadProgress = downloadProgress,
                                                onStartDownload = {
                                                    viewModel.startOrResumeModelDownload(modelId = model.id, onComplete = { success ->
                                                        if (success) {
                                                            viewModel.updateSttModel(model.id)
                                                            Toast.makeText(context, "✅ ${model.name} installed and selected!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, "❌ Download failed. Check connection.", Toast.LENGTH_SHORT).show()
                                                        }
                                                    })
                                                },
                                                onPauseDownload = { viewModel.pauseModelDownload(model.id) },
                                                onResumeDownload = {
                                                    viewModel.startOrResumeModelDownload(modelId = model.id, onComplete = { success ->
                                                        if (success) {
                                                            viewModel.updateSttModel(model.id)
                                                            Toast.makeText(context, "✅ ${model.name} installed and selected!", Toast.LENGTH_SHORT).show()
                                                        }
                                                    })
                                                },
                                                onCancelDownload = { viewModel.cancelModelDownload(model.id) },
                                                onImportManual = {
                                                    targetImportModelId = model.id
                                                    zipPickerLauncher.launch("application/zip")
                                                },
                                                accentColor = PrimaryBlue,
                                                importLabel = "Import ZIP"
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 5: 🧠 AI MODELS & ACTION EXTRACTION
        // =========================================================================
        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_ai") {
            SettingsStickyHeader(
                title = AppStrings.get("section_ai", isHindi),
                summaryBadge = userPreferences.selectedAiModel.replace("_", " "),
                badgeColor = Color(0xFF06B6D4),
                icon = Icons.Default.Psychology,
                isExpanded = isAiExpanded,
                onToggle = { isAiExpanded = !isAiExpanded }
            )
        }

        if (isAiExpanded) {
            item(key = "content_ai") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // ── 🔑 Hugging Face Access Token Input ────────────────────────
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Key,
                                        contentDescription = null,
                                        tint = Color(0xFFF59E0B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isHindi) "हगिंग फेस एक्सेस टोकन" else "Hugging Face Access Token",
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(
                                    text = if (isHindi) "Google Gemma जैसे लाइसेंस-स्वीकृत मॉडलों को डाउनलोड करने के लिए टोकन आवश्यक है।" else "Required to authorize downloads for gated models like Google Gemma 2 / 3.",
                                    fontSize = 10.sp,
                                    color = TextMuted,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                OutlinedTextField(
                                    value = userPreferences.huggingFaceToken,
                                    onValueChange = { viewModel.updateHuggingFaceToken(it) },
                                    label = { Text("HF Token (hf_...)", fontSize = 11.sp) },
                                    placeholder = { Text("hf_xxxxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                                    visualTransformation = if (showHfToken) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showHfToken = !showHfToken }) {
                                            Icon(
                                                imageVector = if (showHfToken) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = if (showHfToken) "Hide" else "Show",
                                                tint = TextMuted,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        val visibleAiModels = remember(userPreferences.settingsMode, userPreferences.devAdvancedVisibleSettings, userPreferences.selectedAiModel) {
                            when (userPreferences.settingsMode) {
                                SettingsMode.SMART -> UserPreferences.AVAILABLE_AI_MODELS.filter {
                                    it.id in listOf(UserPreferences.AI_MODEL_LOCAL_TINY, UserPreferences.AI_MODEL_LOCAL_LLAMA) || it.id == userPreferences.selectedAiModel
                                }
                                SettingsMode.ADVANCED -> UserPreferences.AVAILABLE_AI_MODELS.filter {
                                    it.id == userPreferences.selectedAiModel || userPreferences.devAdvancedVisibleSettings.contains("ai_${it.id}")
                                }
                                SettingsMode.DEVELOPER -> UserPreferences.AVAILABLE_AI_MODELS
                            }
                        }

                        visibleAiModels.forEach { model ->
                            val isSelected = userPreferences.selectedAiModel == model.id
                            val isFulfilled = UserPreferences.isAiModelRequirementFulfilled(
                                context = context,
                                modelId = model.id,
                                cloudApiKey = userPreferences.cloudApiKey,
                                downloadedModels = userPreferences.downloadedModels
                            )
                            val downloadProgress = modelDownloadStates[model.id]
                            val isDownloadingOrPaused = downloadProgress?.status in listOf(
                                ModelDownloadStatus.DOWNLOADING,
                                ModelDownloadStatus.PAUSED,
                                ModelDownloadStatus.EXTRACTING
                            )
                            val isSetupExpanded = expandedAiSetupModelId == model.id || isDownloadingOrPaused

                            val onModelClick = {
                                if (isFulfilled) {
                                    viewModel.updateAiModel(model.id)
                                    Toast.makeText(context, "Selected AI Model: ${model.name}", Toast.LENGTH_SHORT).show()
                                } else {
                                    expandedAiSetupModelId = if (expandedAiSetupModelId == model.id) null else model.id
                                    Toast.makeText(
                                        context,
                                        "⚠️ ${model.name} is not set up. Please download or configure it below first.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }

                            val modelSpecs = when (model.id) {
                                UserPreferences.AI_MODEL_LOCAL_TINY -> "On-Device Rule Parser • Instant • Zero RAM required"
                                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_1B -> "1 Billion Params • ~1.2 GB • 2 GB RAM minimum • GGUF Q4_K_M"
                                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_4B -> "4 Billion Params • ~2.8 GB • 4 GB RAM minimum • GGUF Q4_K_M"
                                UserPreferences.AI_MODEL_LOCAL_GEMMA_3_12B -> "12 Billion Params • ~7.5 GB • 8 GB+ RAM required • GGUF Q4_K_M"
                                UserPreferences.AI_MODEL_LOCAL_GEMMA -> "2.6 Billion Params • ~1.7 GB • 3 GB RAM minimum • GGUF Q4_K_M"
                                UserPreferences.AI_MODEL_LOCAL_LLAMA -> "1.2 Billion Params • ~1.1 GB • 2 GB RAM minimum • GGUF Q4_K_M"
                                "local_qwen" -> "1.5 Billion Params • ~1.3 GB • 2 GB RAM minimum • GGUF Q4_K_M"
                                "local_phi3" -> "3.8 Billion Params • ~2.4 GB • 4 GB RAM minimum • GGUF Q4_K_M"
                                UserPreferences.AI_MODEL_CLOUD_GEMINI -> "Google Cloud Intelligence • Gemini 1.5 Flash • Fast & Lightweight"
                                UserPreferences.AI_MODEL_CLOUD_OPENAI -> "OpenAI Cloud Intelligence • GPT-4o-mini"
                                else -> "${model.sizeText} • Local GGUF Model"
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else SurfaceDark,
                                border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                modifier = Modifier.fillMaxWidth().clickable { onModelClick() }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (userPreferences.isDeveloperMode) {
                                            DevModeVisibilityCheckbox(
                                                settingKey = "ai_${model.id}",
                                                isCurrentlyActive = isSelected,
                                                isAdvancedVisible = userPreferences.devAdvancedVisibleSettings.contains("ai_${model.id}"),
                                                onToggle = { enable ->
                                                    viewModel.toggleDevAdvancedVisibleSetting("ai_${model.id}", enable)
                                                }
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(model.name, fontWeight = FontWeight.Bold, color = if (isSelected) AccentPurple else TextPrimary, fontSize = 14.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = AccentPurple.copy(alpha = 0.15f)
                                                ) {
                                                    Text(model.tag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(model.description, fontSize = 11.sp, color = TextMuted)
                                        }

                                        RadioButton(
                                            selected = isSelected && isFulfilled,
                                            onClick = { onModelClick() },
                                            colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                                        )
                                    }

                                    if (isFulfilled) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFF10B981).copy(alpha = 0.2f)) {
                                                Text(
                                                    text = "✅ Ready & Configured (${model.sizeText})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF10B981),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                            if (model.id != UserPreferences.AI_MODEL_LOCAL_TINY && !model.id.startsWith("cloud_")) {
                                                OutlinedButton(
                                                    onClick = { modelToDelete = model.id },
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text("Delete", fontSize = 10.sp, color = ActiveRed)
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF59E0B).copy(alpha = 0.15f)) {
                                                Text(
                                                    text = "⚠️ Setup Required (${model.sizeText})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFF59E0B),
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                )
                                            }
                                            Text(
                                                text = if (isSetupExpanded) "▲ Hide Setup" else "▼ Show Setup",
                                                fontSize = 11.sp,
                                                color = AccentPurple,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }

                                        if (isSetupExpanded) {
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color(0xFF1E293B),
                                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(10.dp)) {
                                                    Text("⚙️ Specifications:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                                    Spacer(modifier = Modifier.height(2.dp))
                                                    Text(modelSpecs, fontSize = 10.sp, color = TextMuted)

                                                    if (model.id in listOf(UserPreferences.AI_MODEL_CLOUD_GEMINI, UserPreferences.AI_MODEL_CLOUD_OPENAI)) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        if (userPreferences.cloudApiKey.isBlank()) {
                                                            Text("⚠️ Gemini Cloud API Key is missing. Configure it in Section 6 below to use this model.", fontSize = 11.sp, color = ActiveRed)
                                                        } else {
                                                            Text("✅ Cloud API key is configured.", fontSize = 11.sp, color = Color(0xFF10B981))
                                                        }
                                                    } else if (model.id != UserPreferences.AI_MODEL_LOCAL_TINY) {
                                                        Spacer(modifier = Modifier.height(6.dp))
                                                        ModelDownloadControls(
                                                            modelId = model.id,
                                                            sizeText = model.sizeText,
                                                            downloadProgress = downloadProgress,
                                                            onStartDownload = {
                                                                viewModel.startOrResumeModelDownload(modelId = model.id, onComplete = { success ->
                                                                    if (success) {
                                                                        viewModel.updateAiModel(model.id)
                                                                        Toast.makeText(context, "✅ ${model.name} downloaded & selected!", Toast.LENGTH_SHORT).show()
                                                                    } else {
                                                                        Toast.makeText(context, "❌ Download failed.", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                })
                                                            },
                                                            onPauseDownload = { viewModel.pauseModelDownload(model.id) },
                                                            onResumeDownload = {
                                                                viewModel.startOrResumeModelDownload(modelId = model.id, onComplete = { success ->
                                                                    if (success) {
                                                                        viewModel.updateAiModel(model.id)
                                                                        Toast.makeText(context, "✅ ${model.name} downloaded & selected!", Toast.LENGTH_SHORT).show()
                                                                    }
                                                                })
                                                            },
                                                            onCancelDownload = { viewModel.cancelModelDownload(model.id) },
                                                            onImportManual = {
                                                                targetImportModelFileId = model.id
                                                                modelFilePickerLauncher.launch("*/*")
                                                            },
                                                            accentColor = AccentPurple,
                                                            importLabel = "Import GGUF"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Render User-Added Custom AI Models
                        val customModelsList = remember(userPreferences.customAiModels) {
                            val list = mutableListOf<Triple<String, String, String>>()
                            if (userPreferences.customAiModels.isNotBlank()) {
                                try {
                                    val arr = JSONArray(userPreferences.customAiModels)
                                    for (i in 0 until arr.length()) {
                                        val obj = arr.getJSONObject(i)
                                        list.add(Triple(obj.optString("id"), obj.optString("name"), obj.optString("tag", "CUSTOM MODEL")))
                                    }
                                } catch (_: Exception) {}
                            }
                            list
                        }

                        customModelsList.forEach { (cId, cName, cTag) ->
                            val isSelected = userPreferences.selectedAiModel == cId
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else SurfaceDark,
                                border = BorderStroke(1.dp, if (isSelected) AccentPurple else CardBorder),
                                modifier = Modifier.fillMaxWidth().clickable { viewModel.updateAiModel(cId) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(cName, fontWeight = FontWeight.Bold, color = if (isSelected) AccentPurple else TextPrimary, fontSize = 14.sp)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = AccentPurple.copy(alpha = 0.15f)
                                            ) {
                                                Text(cTag, fontSize = 8.sp, fontWeight = FontWeight.Bold, color = AccentPurple, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                            }
                                        }
                                        Text("Custom user-configured AI model endpoint", fontSize = 11.sp, color = TextMuted)
                                    }
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = { viewModel.updateAiModel(cId) },
                                        colors = RadioButtonDefaults.colors(selectedColor = AccentPurple)
                                    )
                                }
                            }
                        }

                        // ➕ Explore & Add AI Models Catalog Button
                        OutlinedButton(
                            onClick = { showExploreModelsDialog = true },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, AccentPurple),
                            modifier = Modifier.fillMaxWidth().height(42.dp)
                        ) {
                            Icon(Icons.Default.Explore, contentDescription = null, tint = AccentPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "➕ Explore & Add Models (Gemma, Llama, Qwen & Custom)",
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 6: ⚡ SYSTEM PROMPT & RAG SEARCH
        // =========================================================================
        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_system") {
            SettingsStickyHeader(
                title = AppStrings.get("section_system", isHindi),
                summaryBadge = if (userPreferences.customSystemPrompt.isBlank()) "Default Prompt" else "Customized",
                badgeColor = AccentPurple,
                icon = Icons.Default.Code,
                isExpanded = isSystemPromptExpanded,
                onToggle = { isSystemPromptExpanded = !isSystemPromptExpanded }
            )
        }

        if (isSystemPromptExpanded) {
            item(key = "content_system") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        // ── AI System Prompt (LLM Guidance & Task Grouping) ──────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🤖 Custom System Prompt",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentPurple
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = AccentPurple.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            text = if (userPreferences.customSystemPrompt.isBlank()) "Default Hardcoded" else "Customized",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentPurple,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Guides local & cloud LLMs in grouping tasks, extracting hierarchies/varieties, and answering queries. If empty, the default expert prompt is displayed and applied.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                var promptDraft by remember(userPreferences.customSystemPrompt) {
                                    mutableStateOf(userPreferences.customSystemPrompt.ifBlank { UserPreferences.DEFAULT_SYSTEM_PROMPT })
                                }

                                OutlinedTextField(
                                    value = promptDraft,
                                    onValueChange = { promptDraft = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    minLines = 4,
                                    maxLines = 8,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = Color(0xFF0F172A),
                                        unfocusedContainerColor = Color(0xFF0F172A),
                                        focusedBorderColor = AccentPurple,
                                        unfocusedBorderColor = Color(0xFF334155),
                                        focusedTextColor = TextPrimary,
                                        unfocusedTextColor = TextPrimary
                                    )
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = {
                                            viewModel.updateCustomSystemPrompt(promptDraft)
                                            Toast.makeText(context, "System prompt saved!", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                        modifier = Modifier.weight(1f).height(34.dp)
                                    ) {
                                        Text("💾 Save Prompt", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }

                                    OutlinedButton(
                                        onClick = {
                                            promptDraft = UserPreferences.DEFAULT_SYSTEM_PROMPT
                                            viewModel.updateCustomSystemPrompt(UserPreferences.DEFAULT_SYSTEM_PROMPT)
                                            Toast.makeText(context, "Reset to default system prompt", Toast.LENGTH_SHORT).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        border = BorderStroke(1.dp, Color(0xFF475569)),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Text("↺ Reset Default", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }
                            }
                        }

                        // ── Internet Search & RAG Semantic Memory ────────────────
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            border = BorderStroke(1.dp, Color(0xFF334155)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "🌐 Live Internet Search (DuckDuckGo)",
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "Enables real-time web querying tools for external intelligence when asking questions to AI.",
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                    }
                                    Switch(
                                        checked = userPreferences.isWebSearchEnabled,
                                        onCheckedChange = { viewModel.updateWebSearchEnabled(it) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = DarkBackground, checkedTrackColor = Color(0xFF06B6D4))
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF0F172A),
                                    border = BorderStroke(1.dp, Color(0xFF334155))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🧠", fontSize = 14.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "RAG Context: Audio transcripts and active task hierarchies are automatically indexed and injected into AI queries.",
                                            fontSize = 10.sp,
                                            color = Color(0xFF38BDF8),
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        // Cloud Gemini API Key Field (Optional)
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = userPreferences.cloudApiKey,
                            onValueChange = { viewModel.updateCloudApiKey(it) },
                            label = { Text("Gemini Cloud API Key (Optional)", fontSize = 12.sp) },
                            placeholder = { Text("Enter AI Studio API key...", fontSize = 11.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E293B),
                                unfocusedContainerColor = Color(0xFF1E293B),
                                focusedBorderColor = AccentPurple,
                                unfocusedBorderColor = Color(0xFF334155),
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            visualTransformation = if (showApiKey) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { showApiKey = !showApiKey }) {
                                    Icon(
                                        imageVector = if (showApiKey) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle visibility",
                                        tint = TextMuted
                                    )
                                }
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }
            }
        }

        // =========================================================================
        // CATEGORY 7: 💾 BACKUP & RESTORE (JSON SETTINGS)
        // =========================================================================
        @OptIn(ExperimentalFoundationApi::class)
        stickyHeader(key = "header_backup") {
            SettingsStickyHeader(
                title = AppStrings.get("section_backup", isHindi),
                summaryBadge = "JSON Export / Import",
                badgeColor = Color(0xFFF59E0B),
                icon = Icons.Default.SettingsBackupRestore,
                isExpanded = isBackupExpanded,
                onToggle = { isBackupExpanded = !isBackupExpanded }
            )
        }

        if (isBackupExpanded) {
            item(key = "content_backup") {
                SettingsSectionCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Export and import your entire configuration (paths, audio parameters, STT/AI models, system prompt, language, categories) in standard JSON format. A live editable preview allows tweaking settings before applying.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )

                        Button(
                            onClick = {
                                val fullJson = com.madhumangal.util.SettingsExportHelper.exportToJson(userPreferences)
                                com.madhumangal.util.SettingsExportHelper.shareSettingsJson(context, fullJson)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("🚀 Beta Tester 1-Tap Export (Share Settings JSON)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    exportJsonPreview = viewModel.exportSettingsToJson()
                                    showExportDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📤", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Export JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = DarkBackground)
                            }

                            OutlinedButton(
                                onClick = {
                                    importJsonInput = viewModel.exportSettingsToJson()
                                    showImportDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("📥", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Import JSON", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFF59E0B))
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Restore Factory Defaults Button (Advanced Mode)
                        OutlinedButton(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ActiveRed),
                            border = BorderStroke(1.dp, ActiveRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isHindi) "फ़ैक्टरी सेटिंग्स पर रीसेट करें" else "Restore Factory Defaults",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
        } // End if (isAdvanced) for Categories 3-7

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }

    // ── Export Settings Modal with Checkboxes, Indented View & Save to File ─
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("📤 Export Settings", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Select sections to include in the exported package. Shown as an indented list without curly braces:",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Section Checkboxes
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = exportAudio,
                                    onCheckedChange = { exportAudio = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                                Text("🎙️ Audio Recording Settings", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = exportStt,
                                    onCheckedChange = { exportStt = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                                Text("🗣️ Speech Recognition (STT)", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = exportAi,
                                    onCheckedChange = { exportAi = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                                Text("🤖 AI Intelligence & Prompts", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = exportAppearance,
                                    onCheckedChange = { exportAppearance = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                                Text("🎨 Appearance, Themes & Typography", fontSize = 12.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = exportStorage,
                                    onCheckedChange = { exportStorage = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
                                )
                                Text("📁 Storage Paths & Categories", fontSize = 12.sp, color = TextPrimary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Indented View (Pretty-Print style, no raw curly braces):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(4.dp))

                    val prettyText = buildString {
                        appendLine("📦 Echo Configuration Manifest")
                        appendLine("    App Version: ${AppConstants.APP_VERSION}")
                        appendLine()
                        if (exportAudio) {
                            appendLine("├── 🎙️ Audio Recording Engine")
                            appendLine("│   ├── Sample Rate: ${userPreferences.sampleRate / 1000} kHz")
                            appendLine("│   ├── Buffer Length: ${userPreferences.bufferLengthSeconds}s")
                            appendLine("│   ├── Overlap: ${userPreferences.overlapSeconds}s")
                            appendLine("│   └── Skip Silence (VAD): ${userPreferences.skipSilence}")
                        }
                        if (exportStt) {
                            appendLine("├── 🗣️ Speech Recognition (STT)")
                            appendLine("│   ├── Engine Model: ${userPreferences.selectedSttModel}")
                            appendLine("│   └── Spoken Language: ${userPreferences.selectedLanguage}")
                        }
                        if (exportAi) {
                            appendLine("├── 🤖 AI Intelligence & Models")
                            appendLine("│   ├── Active Model: ${userPreferences.selectedAiModel}")
                            appendLine("│   ├── AI Response Language: ${userPreferences.aiLanguage}")
                            appendLine("│   ├── Internet Search: ${userPreferences.isWebSearchEnabled}")
                            appendLine("│   └── TTS Speech Speed: ${userPreferences.ttsSpeed}x")
                        }
                        if (exportAppearance) {
                            appendLine("├── 🎨 Appearance & Typography")
                            appendLine("│   ├── Active Theme: ${userPreferences.activeTheme}")
                            appendLine("│   ├── Font Scale: ${userPreferences.fontScale}x")
                            appendLine("│   ├── Font Mode: ${userPreferences.fontMode}")
                            appendLine("│   ├── App Language: ${userPreferences.appLanguage}")
                            appendLine("│   └── Mode: ${userPreferences.settingsMode}")
                        }
                        if (exportStorage) {
                            appendLine("└── 📁 Storage & Categories")
                            appendLine("    ├── Base Directory: ${userPreferences.baseFolderPath.ifBlank { "Downloads (Default)" }}")
                            appendLine("    └── Pinned Categories: [${userPreferences.pinnedCategories.joinToString(", ")}]")
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF0F172A),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        Text(
                            text = prettyText,
                            fontSize = 11.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            modifier = Modifier
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = {
                            val json = viewModel.exportSelectedSettingsToJson(
                                exportAudio, exportStt, exportAi, exportAppearance, exportStorage
                            )
                            pendingExportJson = json
                            val defaultName = "echo_settings_backup_${java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(java.util.Date())}.json"
                            jsonExportFileLauncher.launch(defaultName)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("💾 Save JSON File", fontWeight = FontWeight.Bold, color = DarkBackground, fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val json = viewModel.exportSelectedSettingsToJson(
                                exportAudio, exportStt, exportAi, exportAppearance, exportStorage
                            )
                            clipboardManager.setText(AnnotatedString(json))
                            Toast.makeText(context, "JSON copied to clipboard!", Toast.LENGTH_SHORT).show()
                            showExportDialog = false
                        }
                    ) {
                        Text("📋 Copy", fontSize = 11.sp, color = TextPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            val fullJson = com.madhumangal.util.SettingsExportHelper.exportToJson(userPreferences)
                            com.madhumangal.util.SettingsExportHelper.shareSettingsJson(context, fullJson)
                            showExportDialog = false
                        }
                    ) {
                        Text("↗️ Share", fontSize = 11.sp, color = Color(0xFF10B981))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── Import Settings Modal with Visual Section Toggles & SAF File Loader ─
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text("📥 Import Settings (JSON)", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Select Sections to Import:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(
                                onClick = {
                                    importAudio = true
                                    importStt = true
                                    importAi = true
                                    importAppearance = true
                                    importStorage = true
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("All", fontSize = 10.sp, color = PrimaryBlue)
                            }
                            TextButton(
                                onClick = {
                                    importAudio = false
                                    importStt = false
                                    importAi = false
                                    importAppearance = false
                                    importStorage = false
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("None", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, CardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = importAudio,
                                    onCheckedChange = { importAudio = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎙️ Audio Recording Settings", fontSize = 11.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = importStt,
                                    onCheckedChange = { importStt = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🗣️ Speech Recognition (STT)", fontSize = 11.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = importAi,
                                    onCheckedChange = { importAi = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🤖 AI Intelligence & Prompts", fontSize = 11.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = importAppearance,
                                    onCheckedChange = { importAppearance = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🎨 Appearance, Themes & Typography", fontSize = 11.sp, color = TextPrimary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = importStorage,
                                    onCheckedChange = { importStorage = it },
                                    colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("📁 Storage Paths & Categories", fontSize = 11.sp, color = TextPrimary)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Paste or load settings JSON:",
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedButton(
                            onClick = { jsonImportPickerLauncher.launch("*/*") },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(12.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📁 File", fontSize = 10.sp, color = PrimaryBlue)
                        }
                    }

                    OutlinedTextField(
                        value = importJsonInput,
                        onValueChange = { importJsonInput = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        shape = RoundedCornerShape(10.dp),
                        placeholder = { Text("{\n  \"sampleRate\": 16000,\n  ...\n}", fontSize = 11.sp, color = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF1E293B),
                            unfocusedContainerColor = Color(0xFF1E293B),
                            focusedBorderColor = Color(0xFFF59E0B),
                            unfocusedBorderColor = Color(0xFF334155),
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val imported = com.madhumangal.util.SettingsExportHelper.importFromJson(importJsonInput)
                        if (imported != null) {
                            viewModel.applyImportedPreferences(imported)
                            Toast.makeText(context, "✅ All settings imported successfully!", Toast.LENGTH_LONG).show()
                            showImportDialog = false
                        } else {
                            viewModel.importSelectedSettingsFromJson(
                                importJsonInput,
                                importAudio,
                                importStt,
                                importAi,
                                importAppearance,
                                importStorage
                            ) { success, msg ->
                                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                if (success) showImportDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B))
                ) {
                    Text("Apply Settings", fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── 🎨 Telegram-Style Custom Theme Creator Dialog ──────────────────────
    if (showCustomThemeDialog) {
        AlertDialog(
            onDismissRequest = { showCustomThemeDialog = false },
            title = {
                Text("🎨 Telegram-Style Custom Theme", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = AppStrings.get("clone_from_existing", isHindi),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentPurple
                    )

                    // Cloneable Presets (Telegram, Dracula, AMOLED, Light, etc.)
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cloneOptions = listOf(
                            Triple("Telegram", "#5288C1", "#0E1621") to Pair("#17212B", "#2B5278"),
                            Triple("AMOLED High", "#38BDF8", "#000000") to Pair("#000000", "#1A1A1A"),
                            Triple("AMOLED Pitch", "#38BDF8", "#000000") to Pair("#121212", "#242424"),
                            Triple("Dark Slate", "#38BDF8", "#0B0F17") to Pair("#151D2A", "#242F3D"),
                            Triple("Light", "#0284C7", "#F8FAFC") to Pair("#FFFFFF", "#E2E8F0"),
                            Triple("Midnight", "#38BDF8", "#0F172A") to Pair("#1E293B", "#334155"),
                            Triple("Dracula", "#BD93F9", "#282A36") to Pair("#44475A", "#6272A4"),
                            Triple("Emerald", "#10B981", "#05201A") to Pair("#0A362C", "#134E42"),
                            Triple("Cyberpunk", "#FFE600", "#0D0221") to Pair("#1B1035", "#3D1E6D")
                        )

                        items(cloneOptions) { (info, colors) ->
                            val (pName, pPrim, pBg) = info
                            val (pSurf, pBord) = colors
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(1.dp, CardBorder),
                                modifier = Modifier.clickable {
                                    customPrimaryHex = pPrim
                                    customBgHex = pBg
                                    customSurfaceHex = pSurf
                                    customBorderHex = pBord
                                    Toast.makeText(context, "Cloned $pName!", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Surface(shape = CircleShape, color = Color(android.graphics.Color.parseColor(pPrim)), modifier = Modifier.size(10.dp)) {}
                                    Surface(shape = CircleShape, color = Color(android.graphics.Color.parseColor(pBg)), modifier = Modifier.size(10.dp), border = BorderStroke(0.5.dp, Color.Gray)) {}
                                    Text("Clone $pName", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Primary Hex
                    Text("Primary Accent (Buttons, Active Icons):", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val primColor = try { Color(android.graphics.Color.parseColor(customPrimaryHex)) } catch (_: Exception) { PrimaryBlue }
                        Surface(shape = CircleShape, color = primColor, modifier = Modifier.size(24.dp), border = BorderStroke(1.dp, Color.White)) {}
                        OutlinedTextField(
                            value = customPrimaryHex,
                            onValueChange = { customPrimaryHex = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true
                        )
                    }

                    // Background Hex
                    Text("Background Color:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bgColor = try { Color(android.graphics.Color.parseColor(customBgHex)) } catch (_: Exception) { DarkBackground }
                        Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(24.dp), border = BorderStroke(1.dp, Color.White)) {}
                        OutlinedTextField(
                            value = customBgHex,
                            onValueChange = { customBgHex = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true
                        )
                    }

                    // Surface Hex
                    Text("Surface Card Color:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val surfColor = try { Color(android.graphics.Color.parseColor(customSurfaceHex)) } catch (_: Exception) { SurfaceDark }
                        Surface(shape = CircleShape, color = surfColor, modifier = Modifier.size(24.dp), border = BorderStroke(1.dp, Color.White)) {}
                        OutlinedTextField(
                            value = customSurfaceHex,
                            onValueChange = { customSurfaceHex = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true
                        )
                    }

                    // Border Hex
                    Text("Border Divider Color:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val bordColor = try { Color(android.graphics.Color.parseColor(customBorderHex)) } catch (_: Exception) { CardBorder }
                        Surface(shape = CircleShape, color = bordColor, modifier = Modifier.size(24.dp), border = BorderStroke(1.dp, Color.White)) {}
                        OutlinedTextField(
                            value = customBorderHex,
                            onValueChange = { customBorderHex = it },
                            modifier = Modifier.weight(1f).height(48.dp),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val json = JSONObject().apply {
                            put("primary", customPrimaryHex)
                            put("background", customBgHex)
                            put("surface", customSurfaceHex)
                            put("border", customBorderHex)
                        }
                        viewModel.updateCustomThemeJson(json.toString())
                        viewModel.updateActiveTheme("CUSTOM")
                        Toast.makeText(context, "Custom theme applied!", Toast.LENGTH_SHORT).show()
                        showCustomThemeDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) {
                    Text("Save & Apply Theme", fontWeight = FontWeight.Bold, color = DarkBackground)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomThemeDialog = false }) {
                    Text("Cancel", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── 🤖 Explore AI Models Catalog Dialog ────────────────────────────────
    if (showExploreModelsDialog) {
        AlertDialog(
            onDismissRequest = { showExploreModelsDialog = false },
            title = {
                Text("🤖 Explore & Add AI Models", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Select from best available open-source models with pros & cons, or configure a custom endpoint:",
                        fontSize = 11.sp,
                        color = TextMuted
                    )

                    val catalogModels = listOf(
                        CatalogModelInfo(
                            id = "local_gemma3_1b",
                            name = "Google Gemma 3 1B Instruct",
                            tag = "GEMMA 3 • 1.1 GB • MULTILINGUAL",
                            pros = "Google DeepMind official Gemma 3 weights, native multilingual reasoning, lowest battery and RAM footprint.",
                            cons = "Smaller context size than 12B."
                        ),
                        CatalogModelInfo(
                            id = "local_gemma3_4b",
                            name = "Google Gemma 3 4B Instruct",
                            tag = "GEMMA 3 • 3.2 GB • HIGH REASONING",
                            pros = "State-of-the-art mobile reasoning, superior task extraction, nuanced Hindi & Hinglish comprehension.",
                            cons = "Requires ~4GB device RAM headroom."
                        ),
                        CatalogModelInfo(
                            id = "local_gemma3_12b",
                            name = "Google Gemma 3 12B Instruct",
                            tag = "GEMMA 3 • 8.5 GB • FRONTIER",
                            pros = "Frontier intelligence with multimodal vision and deep logical deduction.",
                            cons = "Requires external Ollama server host or flagship workstation."
                        ),
                        CatalogModelInfo(
                            id = "gemma_2b",
                            name = "Google Gemma 2 2B Instruct",
                            tag = "OFFICIAL GOOGLE • 1.6 GB",
                            pros = "Blazing fast on Android, official Google lightweight weights, low RAM usage.",
                            cons = "Shorter context window, simpler reasoning on long nested conversations."
                        ),
                        CatalogModelInfo(
                            id = "gemma_9b",
                            name = "Google Gemma 2 9B Instruct",
                            tag = "DEEP REASONING • 5.4 GB",
                            pros = "Exceptional reasoning benchmark scores, handles nuanced Hindi & context deeply.",
                            cons = "Requires 8GB+ RAM or external Ollama host machine."
                        ),
                        CatalogModelInfo(
                            id = "llama_3.2_1b",
                            name = "Meta Llama 3.2 1B",
                            tag = "META EDGE • 1.2 GB",
                            pros = "Meta's most compact edge architecture, swift token generation.",
                            cons = "Smaller knowledge cutoff base."
                        ),
                        CatalogModelInfo(
                            id = "llama_3.2_3b",
                            name = "Meta Llama 3.2 3B",
                            tag = "META COMPACT • 2.0 GB",
                            pros = "Sweet spot for on-device reasoning and multilingual translation.",
                            cons = "Requires ~3GB device RAM headroom."
                        ),
                        CatalogModelInfo(
                            id = "qwen_2.5_1.5b",
                            name = "Alibaba Qwen 2.5 1.5B",
                            tag = "HINDI NATIVE • 1.1 GB",
                            pros = "Superior multilingual tokenization, native Hindi mastery, perfect structured JSON.",
                            cons = "Smaller context size than 7B."
                        ),
                        CatalogModelInfo(
                            id = "phi_3.5_mini",
                            name = "Microsoft Phi-3.5 Mini",
                            tag = "MICROSOFT REASONING • 2.2 GB",
                            pros = "High-density synthetic training, superb logical deduction and task grouping.",
                            cons = "Slightly slower token generation on older processors."
                        )
                    )

                    catalogModels.forEach { modelItem ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            border = BorderStroke(1.dp, CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(modelItem.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(modelItem.tag, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = AccentPurple)
                                    }
                                    Button(
                                        onClick = {
                                            // Add to customAiModels
                                            val currentList = try {
                                                if (userPreferences.customAiModels.isNotBlank()) JSONArray(userPreferences.customAiModels) else JSONArray()
                                            } catch (_: Exception) { JSONArray() }

                                            val newObj = JSONObject().apply {
                                                put("id", modelItem.id)
                                                put("name", modelItem.name)
                                                put("tag", modelItem.tag)
                                            }
                                            currentList.put(newObj)
                                            viewModel.updateCustomAiModels(currentList.toString())
                                            viewModel.updateAiModel(modelItem.id)
                                            Toast.makeText(context, "Added & selected: ${modelItem.name}", Toast.LENGTH_SHORT).show()
                                            showExploreModelsDialog = false
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text("Add Model", fontSize = 10.sp, color = Color.White)
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("✅ Pros: ${modelItem.pros}", fontSize = 10.sp, color = Color(0xFF10B981))
                                Text("⚠️ Cons: ${modelItem.cons}", fontSize = 10.sp, color = TextMuted)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Or Add Custom Model Endpoint:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TextPrimary)
                    OutlinedTextField(
                        value = customModelNameInput,
                        onValueChange = { customModelNameInput = it },
                        label = { Text("Model Display Name", fontSize = 10.sp) },
                        placeholder = { Text("e.g. My Ollama Gemma", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customModelIdInput,
                        onValueChange = { customModelIdInput = it },
                        label = { Text("Model Identifier", fontSize = 10.sp) },
                        placeholder = { Text("e.g. custom_ollama_gemma", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = customModelEndpointInput,
                        onValueChange = { customModelEndpointInput = it },
                        label = { Text("Endpoint URL (Optional)", fontSize = 10.sp) },
                        placeholder = { Text("http://192.168.1.100:11434/v1", fontSize = 10.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (customModelNameInput.isNotBlank() && customModelIdInput.isNotBlank()) {
                                val currentList = try {
                                    if (userPreferences.customAiModels.isNotBlank()) JSONArray(userPreferences.customAiModels) else JSONArray()
                                } catch (_: Exception) { JSONArray() }

                                val newObj = JSONObject().apply {
                                    put("id", customModelIdInput.trim())
                                    put("name", customModelNameInput.trim())
                                    put("tag", "CUSTOM ENDPOINT")
                                    put("endpoint", customModelEndpointInput.trim())
                                }
                                currentList.put(newObj)
                                viewModel.updateCustomAiModels(currentList.toString())
                                viewModel.updateAiModel(customModelIdInput.trim())
                                Toast.makeText(context, "Custom model added!", Toast.LENGTH_SHORT).show()
                                showExploreModelsDialog = false
                            } else {
                                Toast.makeText(context, "Please enter Model Name and ID", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Text("Add Custom Endpoint", fontWeight = FontWeight.Bold, color = DarkBackground, fontSize = 11.sp)
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showExploreModelsDialog = false }) {
                    Text("Close", color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    // ── ⚠️ Reset All Preferences Confirmation Dialog ───────────────────────────
    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Text(
                    text = if (isHindi) "⚠️ सभी सेटिंग्स रीसेट करें?" else "⚠️ Reset All Settings?",
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    text = if (isHindi)
                        "क्या आप सभी सेटिंग्स को फ़ैक्टरी डिफ़ॉल्ट पर रीसेट करना चाहते हैं? आपकी सभी कस्टम प्राथमिकताएँ, लेआउट और निर्देश मूल स्थिति में लौट जाएँगे।"
                    else
                        "Are you sure you want to restore all settings to factory defaults? Your custom directories, layouts, and thresholds will be reset.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllPreferences {
                            Toast.makeText(
                                context,
                                if (isHindi) "सभी सेटिंग्स डिफ़ॉल्ट पर रीसेट कर दी गईं" else "All settings reset to factory defaults",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ActiveRed)
                ) {
                    Text(if (isHindi) "रीसेट करें" else "Reset", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text(AppStrings.get("cancel", isHindi), color = TextMuted)
                }
            },
            containerColor = SurfaceDark
        )
    }

    if (showWhatsNewDialog) {
        com.madhumangal.ui.releasenotes.WhatsNewDialog(
            entriesToShow = com.madhumangal.ui.releasenotes.ReleaseNotesRegistry.getAllReleases(),
            showAllHistoryOption = false,
            onDismiss = { showWhatsNewDialog = false }
        )
    }
}

/**
 * Download, Pause, Resume, and Cancel Controls for STT and AI Models.
 */
@Composable
fun ModelDownloadControls(
    modelId: String,
    sizeText: String,
    downloadProgress: com.madhumangal.ui.viewmodel.ModelDownloadProgress?,
    onStartDownload: () -> Unit,
    onPauseDownload: () -> Unit,
    onResumeDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onImportManual: () -> Unit,
    accentColor: Color = PrimaryBlue,
    importLabel: String = "Import ZIP"
) {
    val status = downloadProgress?.status ?: ModelDownloadStatus.IDLE
    val progress = downloadProgress?.progress ?: 0f

    when (status) {
        ModelDownloadStatus.DOWNLOADING -> {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏳ Downloading: ${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                        if (downloadProgress != null && downloadProgress.totalBytes > 0) {
                            Text(
                                text = "${downloadProgress.downloadedBytes / (1024 * 1024)} MB / ${downloadProgress.totalBytes / (1024 * 1024)} MB",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onPauseDownload,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Pause,
                                contentDescription = "Pause",
                                tint = Color(0xFFF59E0B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = ActiveRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = accentColor
                )
            }
        }
        ModelDownloadStatus.PAUSED -> {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "⏸️ Paused at ${(progress * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF59E0B)
                        )
                        if (downloadProgress != null && downloadProgress.totalBytes > 0) {
                            Text(
                                text = "${downloadProgress.downloadedBytes / (1024 * 1024)} MB / ${downloadProgress.totalBytes / (1024 * 1024)} MB",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onResumeDownload,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Resume",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = onCancelDownload,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel",
                                tint = ActiveRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp),
                    color = Color(0xFFF59E0B)
                )
            }
        }
        ModelDownloadStatus.EXTRACTING -> {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = "📦 Extracting model files... Please wait",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = accentColor
                )
            }
        }
        ModelDownloadStatus.ERROR -> {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                Text(
                    text = "❌ ${downloadProgress?.errorMessage ?: "Download failed"}",
                    fontSize = 11.sp,
                    color = ActiveRed
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onStartDownload,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Retry Download", fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = onImportManual,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text(importLabel, fontSize = 11.sp)
                    }
                }
            }
        }
        else -> {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onStartDownload,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Download Model ($sizeText)", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = onImportManual,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text(importLabel, fontSize = 11.sp)
                }
            }
        }
    }
}

/**
 * VS Code / Sticky Scroll Header for Settings Sections.
 * Pins at the top of the canvas while scrolling its category options, creating the stacked header effect.
 */
@Composable
fun SettingsStickyHeader(
    title: String,
    summaryBadge: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(vertical = 2.dp)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() },
            shape = if (isExpanded) RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp) else RoundedCornerShape(14.dp),
            color = Color(0xFF1E293B),
            border = BorderStroke(1.dp, if (isExpanded) badgeColor.copy(alpha = 0.5f) else CardBorder),
            shadowElevation = 4.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = summaryBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

/**
 * Body card for settings categories beneath the sticky header.
 */
@Composable
fun SettingsSectionCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, CardBorder)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content
        )
    }
}

/**
 * Clean, collapsible accordion card component for settings categories
 */
@Composable
fun CollapsibleSettingsSection(
    title: String,
    summaryBadge: String,
    badgeColor: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(CardBorder))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row (Clickable to toggle expand)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(imageVector = icon, contentDescription = null, tint = badgeColor, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = badgeColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = summaryBadge,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = tween(200)) + fadeIn(),
                exit = shrinkVertically(animationSpec = tween(200)) + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }
}

private data class CatalogModelInfo(
    val id: String,
    val name: String,
    val tag: String,
    val pros: String,
    val cons: String
)

@Composable
fun AppTabReorderingCard(
    userPreferences: UserPreferences,
    viewModel: MainViewModel,
    isHindi: Boolean
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val rawOrder = userPreferences.tabOrder
    val defaultTabs = listOf(
        "DUMB_ASSISTANT",
        "DASHBOARD",
        "RECORDER",
        "AI",
        "MEDIA",
        "LOGS",
        "SETTINGS"
    )

    val currentList = remember(rawOrder) {
        val tokens = rawOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val result = mutableListOf<String>()
        for (t in tokens) {
            if (t in defaultTabs && t !in result) {
                result.add(t)
            }
        }
        for (d in defaultTabs) {
            if (d !in result) {
                result.add(d)
            }
        }
        result
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        border = BorderStroke(1.dp, CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Hamburger",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isHindi) "टैब क्रम व्यवस्थित करें (Hamburger)" else "App Tabs Arrangement (Hamburger)",
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }

                TextButton(
                    onClick = {
                        val defaultOrder = defaultTabs.joinToString(",")
                        viewModel.updateTabOrder(defaultOrder)
                        Toast.makeText(context, if (isHindi) "डिफ़ॉल्ट क्रम रीसेट" else "Reset to default order", Toast.LENGTH_SHORT).show()
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isHindi) "रीसेट" else "Reset",
                        fontSize = 11.sp,
                        color = AccentPurple,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Text(
                text = if (isHindi) "बॉटम नेविगेशन बार में टैब का क्रम बदलने के लिए 🔼 या 🔽 दबाएं।"
                       else "Use 🔼 and 🔽 buttons with the hamburger handles to reorder tabs in the bottom bar.",
                fontSize = 10.sp,
                color = TextMuted,
                lineHeight = 14.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            currentList.forEachIndexed { index, tabId ->
                val (engLabel, hiLabel, icon) = when (tabId) {
                    "DUMB_ASSISTANT" -> Triple("Madhu Assistant", "मधु", Icons.Default.SmartToy)
                    "DASHBOARD" -> Triple("Tasks Dashboard", "कार्य सूची", Icons.Default.CheckCircle)
                    "RECORDER" -> Triple("Audio Recorder", "ऑडियो रिकॉर्डर", Icons.Default.Mic)
                    "AI" -> Triple("Ask AI Studio", "एआई स्टूडियो", Icons.Default.AutoAwesome)
                    "MEDIA" -> Triple("Media Transcripts", "मीडिया ट्रांसक्रिप्ट्स", Icons.Default.PermMedia)
                    "LOGS" -> Triple("System Logs", "सिस्टम लॉग", Icons.Default.Terminal)
                    "SETTINGS" -> Triple("Settings", "सेटिंग्स", Icons.Default.Settings)
                    else -> Triple(tabId, tabId, Icons.Default.Tune)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, CardBorder.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Hamburger icon handle
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Reorder handle",
                            tint = PrimaryBlue.copy(alpha = 0.8f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))

                        // Tab icon
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))

                        // Tab name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isHindi) hiLabel else engLabel,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Position #${index + 1}",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }

                        // Move Up
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = currentList.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index - 1]
                                    mutable[index - 1] = temp
                                    viewModel.updateTabOrder(mutable.joinToString(","))
                                }
                            },
                            enabled = index > 0,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = if (index > 0) PrimaryBlue else TextMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Move Down
                        IconButton(
                            onClick = {
                                if (index < currentList.size - 1) {
                                    val mutable = currentList.toMutableList()
                                    val temp = mutable[index]
                                    mutable[index] = mutable[index + 1]
                                    mutable[index + 1] = temp
                                    viewModel.updateTabOrder(mutable.joinToString(","))
                                }
                            },
                            enabled = index < currentList.size - 1,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = if (index < currentList.size - 1) PrimaryBlue else TextMuted.copy(alpha = 0.3f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Intent cascade to open Google Speech Services / Voice Typing settings
 * to allow downloading offline language packs (Hindi, Hinglish, English).
 */
fun openGoogleVoiceLanguageSettings(context: android.content.Context) {
    val intents = listOf(
        // Direct intent to Google Offline Voice Recognition Download page
        Intent("com.google.android.voicesearch.intent.action.DOWNLOAD_VOICE_PACK").apply {
            setPackage("com.google.android.googlequicksearchbox")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        Intent("com.google.android.voicesearch.intent.action.DOWNLOAD_VOICE_PACK").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        // Speech Services by Google settings
        Intent().apply {
            component = android.content.ComponentName("com.google.android.tts", "com.google.android.clockwork.settings.VoiceSettingsActivity")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        // System Voice Input Settings
        Intent(android.provider.Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        // Google App Settings
        Intent("com.google.android.googlequicksearchbox.action.SETTINGS").apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        },
        // Locale / Language settings
        Intent(android.provider.Settings.ACTION_LOCALE_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    )

    var launched = false
    for (intent in intents) {
        try {
            context.startActivity(intent)
            launched = true
            break
        } catch (_: Exception) {
            // Try next fallback intent
        }
    }
    if (!launched) {
        android.widget.Toast.makeText(
            context,
            "Please open Android Settings -> System -> Languages & Input -> Voice input.",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}

/**
 * Developer Mode Meta-Control:
 * Renders granular tier selection chips [Smt] [Adv] [Dev] for any setting or sub-option.
 * - If [isCurrentlyActive] is true, the active tier chip is safeguarded.
 * - Touching only this selector toggles tier inclusion without triggering the row's main click.
 */
@Composable
fun DevModeVisibilityCheckbox(
    settingKey: String,
    isCurrentlyActive: Boolean,
    isAdvancedVisible: Boolean,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTier = if (isAdvancedVisible) "ADVANCED" else "DEVELOPER"
    DevModeTierSelector(
        settingKey = settingKey,
        currentTier = currentTier,
        isCurrentlyActive = isCurrentlyActive,
        onTierSelected = { selectedTier ->
            onToggle(selectedTier == "ADVANCED" || selectedTier == "SMART")
        },
        modifier = modifier
    )
}

@Composable
fun DevModeTierSelector(
    settingKey: String,
    currentTier: String,
    isCurrentlyActive: Boolean,
    onTierSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(end = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf("SMART" to "Smt", "ADVANCED" to "Adv", "DEVELOPER" to "Dev").forEach { (tierKey, label) ->
            val isSelected = currentTier == tierKey || (isCurrentlyActive && tierKey == currentTier)
            val chipColor = when (tierKey) {
                "SMART" -> PrimaryBlue
                "ADVANCED" -> AccentPurple
                else -> Color(0xFF10B981)
            }
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = if (isSelected) chipColor.copy(alpha = 0.22f) else Color(0xFF141B26),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) chipColor else Color(0xFF263345)
                ),
                modifier = Modifier.clickable(enabled = !isCurrentlyActive || isSelected) {
                    onTierSelected(tierKey)
                }
            ) {
                Text(
                    text = label,
                    fontSize = 8.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) chipColor else if (isCurrentlyActive) TextMuted.copy(alpha = 0.4f) else TextMuted,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
        }
    }
}



