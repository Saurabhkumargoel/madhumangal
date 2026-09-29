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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.components.AiAskBottomSheet
import com.echo.recorder.ui.theme.AccentPurple
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.viewmodel.MainViewModel

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
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Recorder Tab
    var showAiAskSheet by remember { mutableStateOf(false) }

    val userPreferences by viewModel.userPreferences.collectAsState(initial = UserPreferences())
    val transcriptions by viewModel.transcriptions.collectAsState(initial = emptyList())
    val transcribingIds by viewModel.transcribingIds.collectAsState(initial = emptySet())
    val processingAiIds by viewModel.processingAiIds.collectAsState(initial = emptySet())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val noteTasks by viewModel.noteTasks.collectAsState(initial = emptyList())
    val lowHangingFruits by viewModel.lowHangingFruits.collectAsState(initial = emptyList())

    val isHindi = userPreferences.appLanguage == "hi"

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark
            ) {
                // 0: Tasks / Dashboard
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
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

                // 1: Recorder
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
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

                // 2: Ask AI (Dedicated Tab)
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.CircleShape,
                            color = if (selectedTab == 2) AccentPurple else AccentPurple.copy(alpha = 0.2f),
                            modifier = Modifier.size(28.dp)
                        ) {
                            androidx.compose.foundation.layout.Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = "Ask AI",
                                    tint = if (selectedTab == 2) Color.White else AccentPurple,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    label = {
                        Text(
                            text = com.echo.recorder.util.AppStrings.get("tab_ai", isHindi),
                            fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                            color = if (selectedTab == 2) AccentPurple else TextMuted,
                            fontSize = 10.sp
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = Color.Transparent
                    )
                )

                // 3: Media
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
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

                // 4: Logs (Restored tab!)
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
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

                // 5: Settings
                NavigationBarItem(
                    selected = selectedTab == 5,
                    onClick = { selectedTab = 5 },
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
    ) { paddingValues ->
        when (selectedTab) {
            0 -> DashboardTab(
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
                onOpenAiAsk = { selectedTab = 2 },
                onEditTask = { id, title, desc, prio, fruit, due ->
                    viewModel.editTask(id, title, desc, prio, fruit, due)
                },
                onReorderTasks = { fromIndex, toIndex, updatedList ->
                    viewModel.reorderTasks(fromIndex, toIndex, updatedList)
                },
                onReorderGroups = { fromIndex, toIndex, updatedCategories ->
                    viewModel.reorderCategoryGroups(fromIndex, toIndex, updatedCategories)
                },
                isRecording = isServiceRunning,
                elapsedSeconds = elapsedSeconds,
                onToggleRecording = {
                    if (isServiceRunning) {
                        viewModel.stopRecordingService(context)
                    } else {
                        viewModel.startRecordingService(context)
                    }
                },
                modifier = Modifier.padding(paddingValues)
            )
            1 -> RecorderTab(
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
            2 -> AiTab(
                viewModel = viewModel,
                tasks = noteTasks,
                modifier = Modifier.padding(paddingValues)
            )
            3 -> TranscriptsTab(
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
                isHindi = isHindi,
                modifier = Modifier.padding(paddingValues)
            )
            4 -> LogsTab(
                isHindi = isHindi,
                modifier = Modifier.padding(paddingValues)
            )
            5 -> SettingsTab(
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
    }
}
