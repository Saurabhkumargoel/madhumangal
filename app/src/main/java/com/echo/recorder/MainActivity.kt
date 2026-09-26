package com.echo.recorder

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.echo.recorder.audio.AudioRecordingService
import com.echo.recorder.ui.screens.MainScreen
import com.echo.recorder.ui.theme.GoodEchoTheme
import com.echo.recorder.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var audioService: AudioRecordingService? = null
    private var isBound by mutableStateOf(false)

    private var isServiceRunning by mutableStateOf(false)
    private var currentAmplitude by mutableFloatStateOf(0f)
    private var currentDb by mutableIntStateOf(-60)
    private var elapsedSeconds by mutableLongStateOf(0L)
    private var liveTranscript by mutableStateOf("")
    private var createdSegmentsCount by mutableIntStateOf(0)

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as AudioRecordingService.LocalBinder
            val instance = binder.getService()
            audioService = instance
            isBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            audioService = null
            isBound = false
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            // Auto start recording immediately on permission grant so user doesn't wonder!
            viewModel.startRecordingService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            GoodEchoTheme {
                // Collect states from bound service
                audioService?.let { service ->
                    val serviceRunningState by service.serviceState.collectAsState()
                    val amplitudeState by service.currentAmplitude.collectAsState()
                    val dbState by service.currentDb.collectAsState()
                    val timerState by service.elapsedSeconds.collectAsState()
                    val liveTextState by service.liveTranscript.collectAsState()
                    val countState by service.createdSegmentsCount.collectAsState()

                    isServiceRunning = serviceRunningState.isRunning
                    currentAmplitude = amplitudeState
                    currentDb = dbState
                    elapsedSeconds = timerState
                    liveTranscript = liveTextState
                    createdSegmentsCount = countState
                }

                MainScreen(
                    viewModel = viewModel,
                    isServiceRunning = isServiceRunning,
                    currentAmplitude = currentAmplitude,
                    currentDb = currentDb,
                    elapsedSeconds = elapsedSeconds,
                    liveTranscript = liveTranscript,
                    createdSegmentsCount = createdSegmentsCount,
                    onRequestPermission = { checkAndRequestPermissions() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        Intent(this, AudioRecordingService::class.java).also { intent ->
            bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onStop() {
        super.onStop()
        if (isBound) {
            unbindService(serviceConnection)
            isBound = false
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
}
