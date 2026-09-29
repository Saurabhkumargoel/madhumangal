package com.echo.recorder

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.echo.recorder.ui.screens.MainScreen
import com.echo.recorder.ui.theme.GoodEchoTheme
import com.echo.recorder.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            viewModel.startRecordingService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            val userPreferences by viewModel.userPreferences.collectAsState()
            GoodEchoTheme(
                themeName = userPreferences.activeTheme,
                customThemeJson = userPreferences.customThemeJson,
                fontScale = userPreferences.fontScale,
                fontMode = userPreferences.fontMode
            ) {
                val isServiceRunning by viewModel.isServiceRunning.collectAsState()
                val currentAmplitude by viewModel.currentAmplitude.collectAsState()
                val currentDb by viewModel.currentDb.collectAsState()
                val elapsedSeconds by viewModel.elapsedSeconds.collectAsState()
                val liveTranscript by viewModel.liveTranscript.collectAsState()
                val createdSegmentsCount by viewModel.createdSegmentsCount.collectAsState()
                val isSpeechDetected by viewModel.isSpeechDetected.collectAsState()
                val statusMessage by viewModel.statusMessage.collectAsState()

                MainScreen(
                    viewModel = viewModel,
                    isServiceRunning = isServiceRunning,
                    currentAmplitude = currentAmplitude,
                    currentDb = currentDb,
                    elapsedSeconds = elapsedSeconds,
                    liveTranscript = liveTranscript,
                    createdSegmentsCount = createdSegmentsCount,
                    isSpeechDetected = isSpeechDetected,
                    statusMessage = statusMessage,
                    onRequestPermission = { checkAndRequestPermissions() }
                )
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.echo.recorder.util.AppLogger.logSafeExit()
    }
}
