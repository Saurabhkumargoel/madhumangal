package com.echo.recorder.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.ui.theme.DarkBackground
import com.echo.recorder.ui.theme.PrimaryBlue
import com.echo.recorder.ui.theme.SurfaceDark
import com.echo.recorder.ui.theme.TextMuted
import com.echo.recorder.ui.viewmodel.MainViewModel

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
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Recorder Tab for instant clarity!
    val userPreferences by viewModel.userPreferences.collectAsState(initial = UserPreferences())
    val transcriptions by viewModel.transcriptions.collectAsState(initial = emptyList())
    val searchQuery by viewModel.searchQuery.collectAsState(initial = "")
    val noteTasks by viewModel.noteTasks.collectAsState(initial = emptyList())
    val lowHangingFruits by viewModel.lowHangingFruits.collectAsState(initial = emptyList())

    Scaffold(
        containerColor = DarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.CheckCircle, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = DarkBackground,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Mic, contentDescription = "Recorder") },
                    label = { Text("Recorder") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = DarkBackground,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Description, contentDescription = "Transcripts") },
                    label = { Text("Transcripts") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = PrimaryBlue,
                        selectedTextColor = PrimaryBlue,
                        indicatorColor = DarkBackground,
                        unselectedIconColor = TextMuted,
                        unselectedTextColor = TextMuted
                    )
                )

                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
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
                onToggleCompletion = { id, done -> viewModel.toggleTaskCompletion(id, done) },
                onDeleteTask = { id -> viewModel.deleteTask(id) },
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
            2 -> TranscriptsTab(
                transcriptions = transcriptions,
                searchQuery = searchQuery,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onDeleteTranscription = { viewModel.deleteTranscription(it) },
                modifier = Modifier.padding(paddingValues)
            )
            3 -> SettingsTab(
                viewModel = viewModel,
                userPreferences = userPreferences,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}
