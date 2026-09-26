package com.echo.recorder.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echo.recorder.EchoApplication
import com.echo.recorder.audio.AudioRecordingService
import com.echo.recorder.data.NoteTask
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EchoApplication
    private val prefsRepo = app.preferencesRepository
    private val transcriptionDao = app.database.transcriptionDao()
    private val noteTaskDao = app.database.noteTaskDao()

    val userPreferences: StateFlow<UserPreferences> = prefsRepo.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.Lazily, UserPreferences())

    val noteTasks: StateFlow<List<NoteTask>> = noteTaskDao.getAllTasksSortedByPriority()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val lowHangingFruits: StateFlow<List<NoteTask>> = noteTaskDao.getLowHangingFruits()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _audioStorageMb = MutableStateFlow(0f)
    val audioStorageMb: StateFlow<Float> = _audioStorageMb.asStateFlow()

    init {
        refreshStorageUsage()
    }

    fun refreshStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val audioDir = File(app.filesDir, "audio_chunks")
            if (audioDir.exists()) {
                val totalBytes = audioDir.listFiles()?.sumOf { it.length() } ?: 0L
                _audioStorageMb.value = totalBytes / (1024f * 1024f)
            }
        }
    }

    fun clearAudioCache() {
        viewModelScope.launch(Dispatchers.IO) {
            val audioDir = File(app.filesDir, "audio_chunks")
            if (audioDir.exists()) {
                audioDir.listFiles()?.forEach { it.delete() }
            }
            refreshStorageUsage()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val transcriptions: StateFlow<List<TranscriptionWithAudio>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                transcriptionDao.getAllTranscriptionsWithAudio()
            } else {
                transcriptionDao.searchTranscriptions(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateBufferLength(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateBufferLength(seconds)
        }
    }

    fun updateOverlap(seconds: Int) {
        viewModelScope.launch {
            prefsRepo.updateOverlap(seconds)
        }
    }

    fun updateSampleRate(sampleRate: Int) {
        viewModelScope.launch {
            prefsRepo.updateSampleRate(sampleRate)
        }
    }

    fun updateLiveTranscribe(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateLiveTranscribe(enabled)
        }
    }

    fun updateSkipSilence(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateSkipSilence(enabled)
        }
    }

    fun updateAutoDelete(autoDelete: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAutoDeleteAudio(autoDelete)
        }
    }

    fun updateLanguage(language: String) {
        viewModelScope.launch {
            prefsRepo.updateLanguage(language)
        }
    }

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            noteTaskDao.updateTaskCompletion(id, isCompleted)
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            noteTaskDao.deleteTask(id)
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            transcriptionDao.deleteTranscription(id)
            refreshStorageUsage()
        }
    }

    fun startRecordingService(context: Context) {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_START
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopRecordingService(context: Context) {
        val intent = Intent(context, AudioRecordingService::class.java).apply {
            action = AudioRecordingService.ACTION_STOP
        }
        context.startService(intent)
    }

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun requestBatteryOptimizationExemption(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
