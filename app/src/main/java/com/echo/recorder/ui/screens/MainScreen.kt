package com.echo.recorder.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.PermMedia
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.SettingsMode
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.components.AbortSessionDialog
import com.echo.recorder.ui.components.AiAskBottomSheet
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.viewmodel.MainViewModel

enum class MainTab {
    HOME,
    DUMB_ASSISTANT,
    DASHBOARD,
    RECORDER,
    AI,
    MEDIA,
    LOGS,
    SETTINGS
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel,
    isServiceRunning: Boolean,
    currentAmplitude: Float,
    currentDb: Int = -60,
    elapsedSeconds: Long = 0L,
    liveTranscript: String = "",
    createdSegmentsCount: Int = 0,
    isSpeechDetected: Boolean = false,
    statusMessage: String = "STANDBY",
    onRequestPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showAiAskSheet by remember { mutableStateOf(false) }

    var showWhatsNewDialog by remember { mutableStateOf(false) }
    var releasesToShow by remember { mutableStateOf<List<com.echo.recorder.ui.releasenotes.ReleaseEntry>>(emptyList()) }

    val userPreferences by viewModel.userPreferences.collectAsState(initial = UserPreferences())

    LaunchedEffect(userPreferences.lastSeenVersionCode) {
        val currentCode = com.echo.recorder.util.AppConstants.VERSION_CODE
        if (currentCode > userPreferences.lastSeenVersionCode) {
            val unread = com.echo.recorder.ui.releasenotes.ReleaseNotesRegistry.getReleasesSince(userPreferences.lastSeenVersionCode)
            if (unread.isNotEmpty()) {
                releasesToShow = unread
                showWhatsNewDialog = true
            }
        }
    }
    val transcriptions by viewModel.transcriptions.collectAsState(initial = emptyList())
    val transcribingIds by viewModel.transcribingIds.collectAsState(initial = emptySet())
    val processingAiIds by viewModel.processingAiIds.collectAsState(initial = emptySet())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val noteTasks by viewModel.noteTasks.collectAsState(initial = emptyList())
    val lowHangingFruits by viewModel.lowHangingFruits.collectAsState(initial = emptyList())

    val isHindi = userPreferences.appLanguage == "hi"

    val visibleTabs = remember(
        userPreferences.settingsMode,
        userPreferences.homeTab,
        userPreferences.showAssistantTab,
        userPreferences.showDashboardTab,
        userPreferences.showRecorderTab,
        userPreferences.showAiTab,
        userPreferences.showMediaTab,
        userPreferences.showLogsTab,
        userPreferences.tabOrder
    ) {
        if (userPreferences.settingsMode == SettingsMode.SMART) {
            listOf(MainTab.HOME, MainTab.SETTINGS)
        } else {
            val enabledTabs = buildSet {
                if (userPreferences.showAssistantTab || userPreferences.homeTab == "DUMB_ASSISTANT") {
                    add(MainTab.DUMB_ASSISTANT)
                }
                if (userPreferences.showDashboardTab || userPreferences.homeTab == "DASHBOARD") {
                    add(MainTab.DASHBOARD)
                }
                if (userPreferences.showRecorderTab || userPreferences.homeTab == "RECORDER") {
                    add(MainTab.RECORDER)
                }
                if (userPreferences.showAiTab || userPreferences.homeTab == "AI") {
                    add(MainTab.AI)
                }
                if (userPreferences.showMediaTab || userPreferences.homeTab == "MEDIA") {
                    add(MainTab.MEDIA)
                }
                if (userPreferences.showLogsTab) {
                    add(MainTab.LOGS)
                }
                add(MainTab.SETTINGS)
            }

            val orderTokens = userPreferences.tabOrder.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val nameToTab = mapOf(
                "DUMB_ASSISTANT" to MainTab.DUMB_ASSISTANT,
                "DASHBOARD" to MainTab.DASHBOARD,
                "RECORDER" to MainTab.RECORDER,
                "AI" to MainTab.AI,
                "MEDIA" to MainTab.MEDIA,
                "LOGS" to MainTab.LOGS,
                "SETTINGS" to MainTab.SETTINGS
            )

            val ordered = mutableListOf<MainTab>()
            for (token in orderTokens) {
                val tab = nameToTab[token]
                if (tab != null && tab in enabledTabs && tab !in ordered) {
                    ordered.add(tab)
                }
            }
            for (tab in enabledTabs) {
                if (tab !in ordered) {
                    ordered.add(tab)
                }
            }
            ordered
        }
    }

    var selectedTab by remember {
        mutableStateOf(
            if (userPreferences.settingsMode == SettingsMode.SMART) {
                MainTab.HOME
            } else {
                when (userPreferences.homeTab) {
                    "DUMB_ASSISTANT" -> MainTab.DUMB_ASSISTANT
                    "DASHBOARD" -> MainTab.DASHBOARD
                    "RECORDER" -> MainTab.RECORDER
                    "AI" -> MainTab.AI
                    "MEDIA" -> MainTab.MEDIA
                    else -> MainTab.DUMB_ASSISTANT
                }
            }
        )
    }

    LaunchedEffect(visibleTabs, userPreferences.settingsMode) {
        if (userPreferences.settingsMode == SettingsMode.SMART) {
            if (selectedTab != MainTab.HOME && selectedTab != MainTab.SETTINGS) {
                selectedTab = MainTab.HOME
            }
        } else {
            if (selectedTab !in visibleTabs) {
                val targetHome = when (userPreferences.homeTab) {
                    "DUMB_ASSISTANT" -> MainTab.DUMB_ASSISTANT
                    "DASHBOARD" -> MainTab.DASHBOARD
                    "RECORDER" -> MainTab.RECORDER
                    "AI" -> MainTab.AI
                    "MEDIA" -> MainTab.MEDIA
                    else -> MainTab.DUMB_ASSISTANT
                }
                selectedTab = if (targetHome in visibleTabs) targetHome else visibleTabs.firstOrNull() ?: MainTab.SETTINGS
            }
        }
    }

    var showAbortDialog by remember { mutableStateOf(false) }
    var abortSegments by remember { mutableStateOf<List<com.echo.recorder.data.AudioSegment>>(emptyList()) }

    if (showAbortDialog) {
        AbortSessionDialog(
            segments = abortSegments,
            defaultSelectionMode = userPreferences.defaultAbortSelection,
            onConfirmAbort = { idsToDelete ->
                showAbortDialog = false
                viewModel.abortRecordingSession(context, idsToDelete) { count ->
                    android.widget.Toast.makeText(
                        context,
                        if (isHindi) "सत्र निरस्त किया गया। $count फ़ाइलें हटाई गईं।" else "Session aborted. $count audio chunks deleted.",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                }
            },
            onDismiss = { showAbortDialog = false },
            isHindi = isHindi
        )
    }
    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark
            ) {
                visibleTabs.forEach { tab ->
                    when (tab) {
                        MainTab.HOME -> NavigationBarItem(
                            selected = selectedTab == MainTab.HOME,
                            onClick = { selectedTab = MainTab.HOME },
                            icon = {
                                val homeIcon = when (userPreferences.homeTab) {
                                    "RECORDER" -> Icons.Default.Mic
                                    "AI" -> Icons.Default.AutoAwesome
                                    "DASHBOARD" -> Icons.Default.CheckCircle
                                    "MEDIA" -> Icons.Default.PermMedia
                                    else -> Icons.Default.SmartToy
                                }
                                Icon(homeIcon, contentDescription = "Home")
                            },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_home", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.DUMB_ASSISTANT -> NavigationBarItem(
                            selected = selectedTab == MainTab.DUMB_ASSISTANT,
                            onClick = { selectedTab = MainTab.DUMB_ASSISTANT },
                            icon = { Icon(Icons.Default.SmartToy, contentDescription = "Good Bot") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_assistant", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.DASHBOARD -> NavigationBarItem(
                            selected = selectedTab == MainTab.DASHBOARD,
                            onClick = { selectedTab = MainTab.DASHBOARD },
                            icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Dashboard") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_dashboard", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.RECORDER -> NavigationBarItem(
                            selected = selectedTab == MainTab.RECORDER,
                            onClick = { selectedTab = MainTab.RECORDER },
                            icon = { Icon(Icons.Default.Mic, contentDescription = "Recorder") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_recorder", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.AI -> NavigationBarItem(
                            selected = selectedTab == MainTab.AI,
                            onClick = { selectedTab = MainTab.AI },
                            icon = {
                                androidx.compose.material3.Surface(
                                    shape = androidx.compose.foundation.shape.CircleShape,
                                    color = if (selectedTab == MainTab.AI) AccentPurple else AccentPurple.copy(alpha = 0.2f),
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = "Ask AI",
                                            tint = if (selectedTab == MainTab.AI) Color.White else AccentPurple,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = com.echo.recorder.util.AppStrings.get("tab_ai", isHindi),
                                    fontWeight = if (selectedTab == MainTab.AI) FontWeight.Bold else FontWeight.Normal,
                                    color = if (selectedTab == MainTab.AI) AccentPurple else TextMuted,
                                    fontSize = 10.sp
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = Color.Transparent
                            )
                        )
                        MainTab.MEDIA -> NavigationBarItem(
                            selected = selectedTab == MainTab.MEDIA,
                            onClick = { selectedTab = MainTab.MEDIA },
                            icon = { Icon(Icons.Default.PermMedia, contentDescription = "Media") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_media", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.LOGS -> NavigationBarItem(
                            selected = selectedTab == MainTab.LOGS,
                            onClick = { selectedTab = MainTab.LOGS },
                            icon = { Icon(Icons.Default.Terminal, contentDescription = "Logs") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_logs", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                        MainTab.SETTINGS -> NavigationBarItem(
                            selected = selectedTab == MainTab.SETTINGS,
                            onClick = { selectedTab = MainTab.SETTINGS },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                            label = { Text(com.echo.recorder.util.AppStrings.get("tab_settings", isHindi), fontSize = 10.sp) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryBlue,
                                selectedTextColor = PrimaryBlue,
                                indicatorColor = DarkBackground,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        when (selectedTab) {
            MainTab.HOME -> {
                when (userPreferences.homeTab) {
                    "RECORDER" -> RecorderTab(
                        viewModel = viewModel,
                        isServiceRunning = isServiceRunning,
                        currentAmplitude = currentAmplitude,
                        currentDb = currentDb,
                        elapsedSeconds = elapsedSeconds,
                        liveTranscript = liveTranscript,
                        createdSegmentsCount = createdSegmentsCount,
                        isSpeechDetected = isSpeechDetected,
                        statusMessage = statusMessage,
                        userPreferences = userPreferences,
                        onRequestPermission = onRequestPermission,
                        modifier = Modifier.padding(paddingValues)
                    )
                    "AI" -> AiTab(
                        viewModel = viewModel,
                        tasks = noteTasks,
                        modifier = Modifier.padding(paddingValues)
                    )
                    "DASHBOARD" -> DashboardTab(
                        tasks = noteTasks,
                        lowHangingFruits = lowHangingFruits,
                        userPreferences = userPreferences,
                        onToggleCompletion = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                        onDeleteTask = { id -> viewModel.deleteTask(id) },
                        onCreateTask = { title, cat, prio, fruit, due, parent, autoDone ->
                            viewModel.createTask(title, cat, prio, fruit, due, parent, autoDone)
                        },
                        onAddSubtask = { parentId, title ->
                            viewModel.addSubtask(parentId, title)
                        },
                        onMoveTaskToCategory = { id, newCat ->
                            viewModel.moveTaskToCategory(id, newCat)
                        },
                        onTogglePinCategory = { cat ->
                            viewModel.togglePinCategory(cat)
                        },
                        onAddCustomCategory = { cat ->
                            viewModel.addCustomCategory(cat)
                        },
                        onDeleteCustomCategory = { cat ->
                            viewModel.deleteCustomCategory(cat)
                        },
                        onExportDashboard = { format ->
                            viewModel.exportDashboardToFile(format) { success, msg, _ ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onOpenAiAsk = {
                            if (MainTab.AI in visibleTabs) selectedTab = MainTab.AI else showAiAskSheet = true
                        },
                        onEditTask = { id, title, desc, prio, fruit, due ->
                            viewModel.editTask(id, title, desc, prio, fruit, due)
                        },
                        onReorderTasks = { fromIndex, toIndex, updatedList ->
                            viewModel.reorderTasks(fromIndex, toIndex, updatedList)
                        },
                        onReorderGroups = { fromIndex, toIndex, updatedCategories ->
                            viewModel.reorderCategoryGroups(fromIndex, toIndex, updatedCategories)
                        },
                        onReorderTasksBulk = { selectedIds, moveUp, siblingList ->
                            viewModel.reorderTasksBulk(selectedIds, moveUp, siblingList)
                        },
                        onDeleteTasksBulk = { ids ->
                            viewModel.deleteTasksBulk(ids)
                        },
                        onImportTasksJson = { jsonStr, callback ->
                            viewModel.importTasksFromJson(jsonStr, callback)
                        },
                        isRecording = isServiceRunning,
                        elapsedSeconds = elapsedSeconds,
                        onToggleRecording = {
                            if (isServiceRunning) {
                                viewModel.stopRecordingService(context)
                            } else {
                                onRequestPermission()
                                viewModel.startRecordingService(context)
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                    "MEDIA" -> TranscriptsTab(
                        transcriptions = transcriptions,
                        allTasks = noteTasks,
                        transcribingIds = transcribingIds,
                        processingAiIds = processingAiIds,
                        defaultSttModel = userPreferences.selectedSttModel,
                        defaultAiModel = userPreferences.selectedAiModel,
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                        onDeleteTranscription = { viewModel.deleteTranscription(it) },
                        onRetranscribe = { id ->
                            viewModel.retranscribeSegment(id) { success, msg ->
                                android.widget.Toast.makeText(context, if (success) "✅ Segment #$id transcribed successfully!" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onRetranscribeWithModel = { id, modelId ->
                            viewModel.retranscribeSegment(id, modelId) { success, msg ->
                                android.widget.Toast.makeText(context, if (success) "✅ Segment #$id transcribed!" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onReprocessAiWithModel = { id, aiModelId ->
                            viewModel.reprocessAiTasks(id, aiModelId) { success, msg ->
                                android.widget.Toast.makeText(context, if (success) "✅ $msg" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onUpdateText = { id, text -> viewModel.updateTranscriptionText(id, text) },
                        onToggleTaskCompletion = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                        onUpdateTaskDetails = { id, title, desc, cat, prio -> viewModel.updateTaskDetails(id, title, desc, cat, prio) },
                        onDeleteTask = { id -> viewModel.deleteTask(id) },
                        onMergeSession = { sessionId ->
                            viewModel.mergeSessionTranscriptions(sessionId) { success, msg, _ ->
                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onExportMedia = {
                            viewModel.exportAllMedia { success, message ->
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onSyncMedia = {
                            viewModel.syncStorageFilesWithDatabase { success, message ->
                                android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        onRunFlow = { sessionId, chunkId ->
                            viewModel.runCompleteFlow(
                                sessionId = sessionId,
                                chunkId = chunkId,
                                onProgress = { step ->
                                    android.widget.Toast.makeText(context, step, android.widget.Toast.LENGTH_SHORT).show()
                                }
                            ) { success, msg ->
                                android.widget.Toast.makeText(context, if (success) "✅ $msg" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                            }
                        },
                        isRecording = isServiceRunning,
                        elapsedSeconds = elapsedSeconds,
                        onToggleRecording = {
                            if (isServiceRunning) {
                                viewModel.stopRecordingService(context)
                            } else {
                                onRequestPermission()
                                viewModel.startRecordingService(context)
                            }
                        },
                        onAbortRecording = {
                            scope.launch {
                                abortSegments = viewModel.getCurrentSessionSegments()
                                showAbortDialog = true
                            }
                        },
                        recorderLayoutMode = userPreferences.recorderLayoutMode,
                        isHindi = isHindi,
                        modifier = Modifier.padding(paddingValues)
                    )
                    else -> DumbHomeTab(
                        viewModel = viewModel,
                        isRecording = isServiceRunning,
                        currentAmplitude = currentAmplitude,
                        currentDb = currentDb,
                        elapsedSeconds = elapsedSeconds,
                        liveTranscript = liveTranscript,
                        isSpeechDetected = isSpeechDetected,
                        userPreferences = userPreferences,
                        onToggleRecording = {
                            if (isServiceRunning) {
                                viewModel.stopRecordingService(context)
                            } else {
                                onRequestPermission()
                                viewModel.startRecordingService(context)
                            }
                        },
                        modifier = Modifier.padding(paddingValues)
                    )
                }
            }
            MainTab.DUMB_ASSISTANT -> DumbHomeTab(
                viewModel = viewModel,
                isRecording = isServiceRunning,
                currentAmplitude = currentAmplitude,
                currentDb = currentDb,
                elapsedSeconds = elapsedSeconds,
                liveTranscript = liveTranscript,
                isSpeechDetected = isSpeechDetected,
                userPreferences = userPreferences,
                onToggleRecording = {
                    if (isServiceRunning) {
                        viewModel.stopRecordingService(context)
                    } else {
                        onRequestPermission()
                        viewModel.startRecordingService(context)
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.DASHBOARD -> DashboardTab(
                tasks = noteTasks,
                lowHangingFruits = lowHangingFruits,
                userPreferences = userPreferences,
                onToggleCompletion = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                onDeleteTask = { id -> viewModel.deleteTask(id) },
                onCreateTask = { title, cat, prio, fruit, due, parent, autoDone ->
                    viewModel.createTask(title, cat, prio, fruit, due, parent, autoDone)
                },
                onAddSubtask = { parentId, title ->
                    viewModel.addSubtask(parentId, title)
                },
                onMoveTaskToCategory = { id, newCat ->
                    viewModel.moveTaskToCategory(id, newCat)
                },
                onTogglePinCategory = { cat ->
                    viewModel.togglePinCategory(cat)
                },
                onAddCustomCategory = { cat ->
                    viewModel.addCustomCategory(cat)
                },
                onDeleteCustomCategory = { cat ->
                    viewModel.deleteCustomCategory(cat)
                },
                onExportDashboard = { format ->
                    viewModel.exportDashboardToFile(format) { success, msg, _ ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onOpenAiAsk = {
                    if (MainTab.AI in visibleTabs) selectedTab = MainTab.AI else showAiAskSheet = true
                },
                onEditTask = { id, title, desc, prio, fruit, due ->
                    viewModel.editTask(id, title, desc, prio, fruit, due)
                },
                onReorderTasks = { fromIndex, toIndex, updatedList ->
                    viewModel.reorderTasks(fromIndex, toIndex, updatedList)
                },
                onReorderGroups = { fromIndex, toIndex, updatedCategories ->
                    viewModel.reorderCategoryGroups(fromIndex, toIndex, updatedCategories)
                },
                onReorderTasksBulk = { selectedIds, moveUp, siblingList ->
                    viewModel.reorderTasksBulk(selectedIds, moveUp, siblingList)
                },
                onDeleteTasksBulk = { ids ->
                    viewModel.deleteTasksBulk(ids)
                },
                onImportTasksJson = { jsonStr, callback ->
                    viewModel.importTasksFromJson(jsonStr, callback)
                },
                isRecording = isServiceRunning,
                elapsedSeconds = elapsedSeconds,
                onToggleRecording = {
                    if (isServiceRunning) {
                        viewModel.stopRecordingService(context)
                    } else {
                        onRequestPermission()
                        viewModel.startRecordingService(context)
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.RECORDER -> RecorderTab(
                viewModel = viewModel,
                isServiceRunning = isServiceRunning,
                currentAmplitude = currentAmplitude,
                currentDb = currentDb,
                elapsedSeconds = elapsedSeconds,
                liveTranscript = liveTranscript,
                createdSegmentsCount = createdSegmentsCount,
                isSpeechDetected = isSpeechDetected,
                statusMessage = statusMessage,
                userPreferences = userPreferences,
                onRequestPermission = onRequestPermission,
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.AI -> AiTab(
                viewModel = viewModel,
                tasks = noteTasks,
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.MEDIA -> TranscriptsTab(
                transcriptions = transcriptions,
                allTasks = noteTasks,
                transcribingIds = transcribingIds,
                processingAiIds = processingAiIds,
                defaultSttModel = userPreferences.selectedSttModel,
                defaultAiModel = userPreferences.selectedAiModel,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onDeleteTranscription = { viewModel.deleteTranscription(it) },
                onRetranscribe = { id ->
                    viewModel.retranscribeSegment(id) { success, msg ->
                        android.widget.Toast.makeText(context, if (success) "✅ Segment #$id transcribed successfully!" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onRetranscribeWithModel = { id, modelId ->
                    viewModel.retranscribeSegment(id, modelId) { success, msg ->
                        android.widget.Toast.makeText(context, if (success) "✅ Segment #$id transcribed!" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onReprocessAiWithModel = { id, aiModelId ->
                    viewModel.reprocessAiTasks(id, aiModelId) { success, msg ->
                        android.widget.Toast.makeText(context, if (success) "✅ $msg" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onUpdateText = { id, text -> viewModel.updateTranscriptionText(id, text) },
                onToggleTaskCompletion = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                onUpdateTaskDetails = { id, title, desc, cat, prio -> viewModel.updateTaskDetails(id, title, desc, cat, prio) },
                onDeleteTask = { id -> viewModel.deleteTask(id) },
                onMergeSession = { sessionId ->
                    viewModel.mergeSessionTranscriptions(sessionId) { success, msg, _ ->
                        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onExportMedia = {
                    viewModel.exportAllMedia { success, message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onSyncMedia = {
                    viewModel.syncStorageFilesWithDatabase { success, message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                onRunFlow = { sessionId, chunkId ->
                    viewModel.runCompleteFlow(
                        sessionId = sessionId,
                        chunkId = chunkId,
                        onProgress = { step ->
                            android.widget.Toast.makeText(context, step, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) { success, msg ->
                        android.widget.Toast.makeText(context, if (success) "✅ $msg" else "⚠️ $msg", android.widget.Toast.LENGTH_LONG).show()
                    }
                },
                isRecording = isServiceRunning,
                elapsedSeconds = elapsedSeconds,
                onToggleRecording = {
                    if (isServiceRunning) {
                        viewModel.stopRecordingService(context)
                    } else {
                        onRequestPermission()
                        viewModel.startRecordingService(context)
                    }
                },
                onAbortRecording = {
                    scope.launch {
                        abortSegments = viewModel.getCurrentSessionSegments()
                        showAbortDialog = true
                    }
                },
                recorderLayoutMode = userPreferences.recorderLayoutMode,
                isHindi = isHindi,
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.LOGS -> LogsTab(
                viewModel = viewModel,
                userPreferences = userPreferences,
                isHindi = isHindi,
                modifier = Modifier.padding(paddingValues)
            )
            MainTab.SETTINGS -> SettingsTab(
                viewModel = viewModel,
                userPreferences = userPreferences,
                modifier = Modifier.padding(paddingValues)
            )
        }

        if (showAiAskSheet) {
            AiAskBottomSheet(
                viewModel = viewModel,
                onDismissRequest = { showAiAskSheet = false },
                isHindi = isHindi
            )
        }

        if (showWhatsNewDialog) {
            com.echo.recorder.ui.releasenotes.WhatsNewDialog(
                entriesToShow = releasesToShow,
                showAllHistoryOption = true,
                onDismiss = {
                    showWhatsNewDialog = false
                    viewModel.updateLastSeenVersionCode(com.echo.recorder.util.AppConstants.VERSION_CODE)
                }
            )
        }
    }
}
