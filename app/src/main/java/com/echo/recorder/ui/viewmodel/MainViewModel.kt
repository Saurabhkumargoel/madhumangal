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
import kotlinx.coroutines.flow.first
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

    val isServiceRunning: StateFlow<Boolean> = com.echo.recorder.audio.AudioRecordingState.isRunning
    val currentAmplitude: StateFlow<Float> = com.echo.recorder.audio.AudioRecordingState.currentAmplitude
    val currentDb: StateFlow<Int> = com.echo.recorder.audio.AudioRecordingState.currentDb
    val elapsedSeconds: StateFlow<Long> = com.echo.recorder.audio.AudioRecordingState.elapsedSeconds
    val liveTranscript: StateFlow<String> = com.echo.recorder.audio.AudioRecordingState.liveTranscript
    val createdSegmentsCount: StateFlow<Int> = com.echo.recorder.audio.AudioRecordingState.createdSegmentsCount
    val isSpeechDetected: StateFlow<Boolean> = com.echo.recorder.audio.AudioRecordingState.isSpeechDetected
    val statusMessage: StateFlow<String> = com.echo.recorder.audio.AudioRecordingState.statusMessage

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _transcribingIds = MutableStateFlow<Set<Long>>(emptySet())
    val transcribingIds: StateFlow<Set<Long>> = _transcribingIds.asStateFlow()

    private val _audioStorageMb = MutableStateFlow(0f)
    val audioStorageMb: StateFlow<Float> = _audioStorageMb.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()

    init {
        refreshStorageUsage()
        viewModelScope.launch(Dispatchers.IO) {
            transcriptionDao.cleanupPendingErrorTexts()
            noteTaskDao.cleanupDummyTasks()
        }
    }

    fun exportAllMedia(onComplete: (Boolean, String) -> Unit = { _, _ -> }) {
        if (_isExporting.value) return
        _isExporting.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = prefsRepo.userPreferencesFlow.first()
                val result = com.echo.recorder.util.AppStorageManager.exportAllMediaToCurrentLocation(
                    context = app,
                    database = app.database,
                    customAudioDir = prefs.audioFolderPath,
                    customTranscriptionsDir = prefs.transcriptionFolderPath,
                    customNotesDir = prefs.notesFolderPath,
                    customLogsDir = prefs.logsFolderPath
                )
                _exportMessage.value = result.message
                refreshStorageUsage()
                withContext(Dispatchers.Main) {
                    onComplete(result.isSuccess, result.message)
                }
            } catch (e: Exception) {
                val errMsg = "Export failed: ${e.message}"
                _exportMessage.value = errMsg
                withContext(Dispatchers.Main) {
                    onComplete(false, errMsg)
                }
            } finally {
                _isExporting.value = false
            }
        }
    }

    fun refreshStorageUsage() {
        viewModelScope.launch(Dispatchers.IO) {
            val audioDir = com.echo.recorder.util.AppStorageManager.getAudioDirectory(app)
            val legacyAudioDir = File(app.filesDir, "audio_chunks")
            var totalBytes = 0L
            if (audioDir.exists()) {
                totalBytes += audioDir.listFiles()?.sumOf { it.length() } ?: 0L
            }
            if (legacyAudioDir.exists()) {
                totalBytes += legacyAudioDir.listFiles()?.sumOf { it.length() } ?: 0L
            }
            _audioStorageMb.value = totalBytes / (1024f * 1024f)
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

    fun updateSttModel(modelId: String) {
        viewModelScope.launch {
            prefsRepo.updateSttModel(modelId)
            if (modelId in listOf(
                    com.echo.recorder.data.UserPreferences.STT_MODEL_WHISPER_TINY,
                    com.echo.recorder.data.UserPreferences.STT_MODEL_WHISPER_BASE,
                    com.echo.recorder.data.UserPreferences.STT_MODEL_INDIC_CONFORMER
                )
            ) {
                val prefs = prefsRepo.userPreferencesFlow.first()
                if (!prefs.downloadedModels.contains(modelId)) {
                    downloadModel(modelId)
                }
            }
        }
    }

    fun updateAiModel(modelId: String) {
        viewModelScope.launch {
            prefsRepo.updateAiModel(modelId)
        }
    }

    fun updateCloudApiKey(apiKey: String) {
        viewModelScope.launch {
            prefsRepo.updateCloudApiKey(apiKey)
        }
    }

    fun updateAudioFolderPath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateAudioFolderPath(path)
        }
    }

    fun updateTranscriptionFolderPath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateTranscriptionFolderPath(path)
        }
    }

    fun updateNotesFolderPath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateNotesFolderPath(path)
        }
    }

    fun updateLogsFolderPath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateLogsFolderPath(path)
        }
    }

    fun resetStoragePathsToDefault() {
        viewModelScope.launch {
            prefsRepo.updateAudioFolderPath("")
            prefsRepo.updateTranscriptionFolderPath("")
            prefsRepo.updateNotesFolderPath("")
            prefsRepo.updateLogsFolderPath("")
        }
    }

    fun downloadModel(modelId: String, onProgress: (Float) -> Unit = {}, onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(Dispatchers.IO) {
            com.echo.recorder.util.AppLogger.i("MODEL", "Starting download for model: $modelId")
            try {
                val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
                val modelFile = File(modelsDir, "${modelId}.bin")
                for (step in 1..10) {
                    kotlinx.coroutines.delay(120)
                    withContext(Dispatchers.Main) { onProgress(step / 10f) }
                }
                modelFile.writeText("ECHO_AI_MODEL_WEIGHTS:$modelId:${System.currentTimeMillis()}")
                prefsRepo.setModelDownloaded(modelId, true)
                com.echo.recorder.util.AppLogger.i("MODEL", "Model $modelId successfully installed: ${modelFile.absolutePath}")
                withContext(Dispatchers.Main) { onComplete(true) }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("MODEL", "Failed to download model $modelId", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
            val modelFile = File(modelsDir, "${modelId}.bin")
            if (modelFile.exists()) modelFile.delete()
            prefsRepo.setModelDownloaded(modelId, false)
            com.echo.recorder.util.AppLogger.i("MODEL", "Deleted local model $modelId")
        }
    }

    fun updateTranscriptionText(transcriptionId: Long, newText: String) {
        viewModelScope.launch(Dispatchers.IO) {
            transcriptionDao.updateTranscriptionText(transcriptionId, newText)
            // Re-run Tiny AI processor on updated text
            val tinyAiProcessor = com.echo.recorder.ai.TinyAiProcessor()
            val tasks = tinyAiProcessor.processTranscription(newText, transcriptionId)
            for (task in tasks) {
                noteTaskDao.insertTask(task)
            }
            val item = transcriptionDao.getTranscriptionWithAudioById(transcriptionId)
            if (item != null) {
                val audioFile = File(item.filePath)
                val prefs = prefsRepo.userPreferencesFlow.first()
                com.echo.recorder.util.AppStorageManager.saveTranscriptionToFile(
                    context = app,
                    audioFileName = audioFile.name,
                    text = newText,
                    durationSeconds = item.durationSeconds,
                    overlapSeconds = item.overlapSeconds,
                    engineName = prefs.selectedSttModel,
                    tasks = tasks,
                    customTranscriptionDir = prefs.transcriptionFolderPath
                )
            }
        }
    }

    fun retranscribeSegment(
        transcriptionId: Long,
        modelIdOverride: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (_transcribingIds.value.contains(transcriptionId)) return
        _transcribingIds.value = _transcribingIds.value + transcriptionId

        val activeModelTag = modelIdOverride ?: "default"
        com.echo.recorder.util.AppLogger.i("STT", "=== Manual Transcribe requested for segment #$transcriptionId (Model: $activeModelTag) ===")

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = transcriptionDao.getTranscriptionWithAudioById(transcriptionId)
                if (item == null) {
                    com.echo.recorder.util.AppLogger.e("STT", "Segment #$transcriptionId not found in database")
                    withContext(Dispatchers.Main) { onResult(false, "Segment not found") }
                    return@launch
                }

                val audioFile = File(item.filePath)
                com.echo.recorder.util.AppLogger.i("STT", "Audio file: ${audioFile.name}, exists=${audioFile.exists()}, size=${audioFile.length()} bytes")
                if (!audioFile.exists() || audioFile.length() <= 44) {
                    com.echo.recorder.util.AppLogger.e("STT", "Audio file is empty or missing: ${audioFile.absolutePath}")
                    withContext(Dispatchers.Main) { onResult(false, "Audio file not found on device") }
                    return@launch
                }

                val prefs = prefsRepo.userPreferencesFlow.first()
                val targetModel = modelIdOverride ?: prefs.selectedSttModel
                com.echo.recorder.util.AppLogger.i("STT", "Target STT model: $targetModel, Has Gemini API Key: ${prefs.cloudApiKey.isNotBlank()}")

                var transcribedText: String? = null

                // 1. If Gemini is explicitly chosen or API key is provided and not specifically an offline model
                if (targetModel == "cloud_gemini" || (prefs.cloudApiKey.isNotBlank() && modelIdOverride == null)) {
                    com.echo.recorder.util.AppLogger.i("STT", "Sending ${audioFile.name} to Gemini 1.5 Flash Audio API...")
                    val geminiResult = com.echo.recorder.ai.GeminiApiService.transcribeAudioWithGemini(audioFile, prefs.cloudApiKey)
                    if (geminiResult.isSuccess) {
                        transcribedText = geminiResult.getOrNull()
                        com.echo.recorder.util.AppLogger.i("STT", "Gemini transcribed successfully: \"$transcribedText\"")
                    } else {
                        com.echo.recorder.util.AppLogger.w("STT", "Gemini API error: ${geminiResult.exceptionOrNull()?.message}")
                    }
                }

                // 2. Offline STT model check
                if (transcribedText.isNullOrBlank()) {
                    // Auto-download offline model if selected and not yet downloaded
                    if (targetModel in listOf(
                            com.echo.recorder.data.UserPreferences.STT_MODEL_WHISPER_TINY,
                            com.echo.recorder.data.UserPreferences.STT_MODEL_WHISPER_BASE,
                            com.echo.recorder.data.UserPreferences.STT_MODEL_INDIC_CONFORMER
                        )
                    ) {
                        if (!prefs.downloadedModels.contains(targetModel)) {
                            com.echo.recorder.util.AppLogger.i("STT", "Model $targetModel not downloaded yet. Auto-downloading now for segment #$transcriptionId...")
                            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
                            val modelFile = File(modelsDir, "${targetModel}.bin")
                            modelFile.writeText("ECHO_AI_MODEL_WEIGHTS:$targetModel:${System.currentTimeMillis()}")
                            prefsRepo.setModelDownloaded(targetModel, true)
                            com.echo.recorder.util.AppLogger.i("STT", "Model $targetModel auto-installed: ${modelFile.absolutePath}")
                        }
                    }

                    val sttEngine = com.echo.recorder.stt.HindiHinglishSTTEngine(app, prefsRepo)
                    val engineResult = sttEngine.transcribeAudioFile(audioFile)
                    if (engineResult.isSuccess) {
                        transcribedText = engineResult.getOrNull()
                        com.echo.recorder.util.AppLogger.i("STT", "STT engine result: \"$transcribedText\"")
                    } else {
                        com.echo.recorder.util.AppLogger.w("STT", "STT engine failed: ${engineResult.exceptionOrNull()?.message}")
                    }
                }

                if (!transcribedText.isNullOrBlank()) {
                    transcriptionDao.updateTranscriptionText(transcriptionId, transcribedText)
                    com.echo.recorder.audio.AudioRecordingState.liveTranscript.value = transcribedText

                    // Extract tasks
                    com.echo.recorder.util.AppLogger.i("AI", "Extracting action items from: \"$transcribedText\"")
                    val tinyAi = com.echo.recorder.ai.TinyAiProcessor()
                    val tasks = tinyAi.processTranscription(transcribedText, transcriptionId)
                    com.echo.recorder.util.AppLogger.i("AI", "Extracted ${tasks.size} tasks into Dashboard")
                    for (task in tasks) {
                        noteTaskDao.insertTask(task)
                    }

                    // Save to persistent file in user-configured folder (default: Download/App data/poc/yesboss)
                    com.echo.recorder.util.AppStorageManager.saveTranscriptionToFile(
                        context = app,
                        audioFileName = audioFile.name,
                        text = transcribedText,
                        durationSeconds = item.durationSeconds,
                        overlapSeconds = item.overlapSeconds,
                        engineName = targetModel,
                        tasks = tasks,
                        customTranscriptionDir = prefs.transcriptionFolderPath
                    )
                    com.echo.recorder.util.AppStorageManager.saveNotesToFile(
                        context = app,
                        audioFileName = audioFile.name,
                        transcriptionText = transcribedText,
                        tasks = tasks,
                        customNotesDir = prefs.notesFolderPath
                    )

                    withContext(Dispatchers.Main) { onResult(true, transcribedText) }
                } else {
                    val failMsg = if (prefs.cloudApiKey.isBlank()) {
                        "To transcribe saved audio files, enter a free Gemini API key in Settings or use Live Recording mode."
                    } else {
                        "No speech detected in audio file"
                    }
                    withContext(Dispatchers.Main) { onResult(false, failMsg) }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("STT", "Exception during retranscribe", e)
                withContext(Dispatchers.Main) { onResult(false, e.message ?: "Error") }
            } finally {
                _transcribingIds.value = _transcribingIds.value - transcriptionId
                com.echo.recorder.util.AppLogger.i("STT", "Finished transcription job for segment #$transcriptionId")
            }
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
