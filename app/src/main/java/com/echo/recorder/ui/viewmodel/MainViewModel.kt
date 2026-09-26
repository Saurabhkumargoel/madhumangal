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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

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

    fun updateAutoDelete(autoDelete: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateAutoDeleteAudio(autoDelete)
        }
    }

    fun deleteTranscription(id: Long) {
        viewModelScope.launch {
            transcriptionDao.deleteTranscription(id)
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

    fun addManualTask(title: String, categoryGroup: String, priority: Int, isLowHangingFruit: Boolean) {
        viewModelScope.launch {
            val task = NoteTask(
                title = title,
                categoryGroup = categoryGroup,
                priority = priority,
                isLowHangingFruit = isLowHangingFruit
            )
            noteTaskDao.insertTask(task)
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
