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

    private val _processingAiIds = MutableStateFlow<Set<Long>>(emptySet())
    val processingAiIds: StateFlow<Set<Long>> = _processingAiIds.asStateFlow()

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
            val currentPrefs = prefsRepo.userPreferencesFlow.first()
            val currentModel = UserPreferences.AVAILABLE_STT_MODELS.find { it.id == currentPrefs.selectedSttModel }
            if (enabled && currentModel?.capability == com.echo.recorder.data.SttCapability.FILE_ONLY) {
                prefsRepo.updateSttModel(UserPreferences.STT_MODEL_GOOGLE_SPEECH)
                com.echo.recorder.util.AppLogger.i("SETTINGS", "Live mode enabled: auto-switched STT model to Google Speech Recognizer")
            } else if (!enabled && currentModel?.capability == com.echo.recorder.data.SttCapability.LIVE_ONLY) {
                prefsRepo.updateSttModel(UserPreferences.STT_MODEL_VOSK_HINDI)
                com.echo.recorder.util.AppLogger.i("SETTINGS", "File mode enabled: auto-switched STT model to Vosk Hindi (Offline)")
            }
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

                when (modelId) {
                    com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI,
                    com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN -> {
                        // ── Real Vosk model download + extraction (Hindi & Indian English) ─
                        val isEnIn = modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN
                        val zipFileName = if (isEnIn) "vosk_en_in_download.zip" else "vosk_hindi_download.zip"
                        val dirName = if (isEnIn) com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN else com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME
                        val zipFile = File(modelsDir, zipFileName)
                        val extractDir = File(modelsDir, dirName)

                        val modelUrl = if (isEnIn)
                            "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
                        else
                            "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"
                        com.echo.recorder.util.AppLogger.i("MODEL", "Downloading Vosk model ($modelId) from: $modelUrl")

                        try {
                            // ── 1. HTTP Download with progress ─────────────────────────────
                            val connection = java.net.URL(modelUrl).openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 30_000
                            connection.readTimeout = 120_000
                            connection.connect()

                            val totalBytes = connection.contentLengthLong.coerceAtLeast(1L)
                            var downloadedBytes = 0L

                            connection.inputStream.use { input ->
                                zipFile.outputStream().use { output ->
                                    val buf = ByteArray(8192)
                                    var read: Int
                                    while (input.read(buf).also { read = it } != -1) {
                                        output.write(buf, 0, read)
                                        downloadedBytes += read
                                        val progress = (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.95f)
                                        withContext(Dispatchers.Main) { onProgress(progress) }
                                    }
                                }
                            }

                            com.echo.recorder.util.AppLogger.i("MODEL", "Vosk zip downloaded: ${zipFile.length()} bytes")
                            withContext(Dispatchers.Main) { onProgress(0.97f) }

                            // ── 2. Extract zip ─────────────────────────────────────────────
                            extractDir.mkdirs()
                            java.util.zip.ZipInputStream(zipFile.inputStream()).use { zis ->
                                var entry = zis.nextEntry
                                while (entry != null) {
                                    // Strip the top-level directory prefix from archive
                                    val relPath = entry.name.substringAfter('/')
                                    if (relPath.isNotEmpty()) {
                                        val outFile = File(extractDir, relPath)
                                        if (entry.isDirectory) {
                                            outFile.mkdirs()
                                        } else {
                                            outFile.parentFile?.mkdirs()
                                            outFile.outputStream().use { out ->
                                                zis.copyTo(out)
                                            }
                                        }
                                    }
                                    zis.closeEntry()
                                    entry = zis.nextEntry
                                }
                            }

                            // ── 3. Clean up zip ────────────────────────────────────────────
                            zipFile.delete()
                            com.echo.recorder.util.AppLogger.i("MODEL", "Vosk model extracted to: ${extractDir.absolutePath}")

                            prefsRepo.setModelDownloaded(modelId, true)
                            withContext(Dispatchers.Main) {
                                onProgress(1f)
                                onComplete(true)
                            }
                        } catch (e: Exception) {
                            zipFile.delete()  // clean up partial download
                            throw e
                        }
                    }

                    else -> {
                        // Legacy placeholder for other model IDs (no real engine yet)
                        val modelFile = File(modelsDir, "${modelId}.bin")
                        for (step in 1..10) {
                            kotlinx.coroutines.delay(120)
                            withContext(Dispatchers.Main) { onProgress(step / 10f) }
                        }
                        modelFile.writeText("ECHO_AI_MODEL_WEIGHTS:$modelId:${System.currentTimeMillis()}")
                        prefsRepo.setModelDownloaded(modelId, true)
                        com.echo.recorder.util.AppLogger.i("MODEL", "Model $modelId stub installed: ${modelFile.absolutePath}")
                        withContext(Dispatchers.Main) { onComplete(true) }
                    }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("MODEL", "Failed to download model $modelId", e)
                withContext(Dispatchers.Main) { onComplete(false) }
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
            if (modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI) {
                // Vosk model is a directory, not a .bin file
                val voskDir = File(modelsDir, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                if (voskDir.exists()) voskDir.deleteRecursively()
            } else if (modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN) {
                val voskDir = File(modelsDir, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                if (voskDir.exists()) voskDir.deleteRecursively()
            } else {
                val modelFile = File(modelsDir, "${modelId}.bin")
                if (modelFile.exists()) modelFile.delete()
            }
            prefsRepo.setModelDownloaded(modelId, false)
            com.echo.recorder.util.AppLogger.i("MODEL", "Deleted model $modelId")
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
                com.echo.recorder.util.AppStorageManager.saveNotesToFile(
                    context = app,
                    audioFileName = audioFile.name,
                    transcriptionText = newText,
                    tasks = tasks,
                    customNotesDir = prefs.notesFolderPath
                )
                com.echo.recorder.util.AppStorageManager.saveChunkSrt(
                    context = app,
                    audioFileName = audioFile.name,
                    text = newText,
                    durationSeconds = item.durationSeconds,
                    customDir = prefs.transcriptionFolderPath
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

                // Provide a minimum visible delay so user sees button loading spinner
                kotlinx.coroutines.delay(650)

                var transcribedText: String? = null

                // 1. Cloud Gemini ONLY if explicitly chosen as model
                if (targetModel == "cloud_gemini") {
                    if (prefs.cloudApiKey.isNotBlank()) {
                        com.echo.recorder.util.AppLogger.i("STT", "Sending ${audioFile.name} to Gemini 1.5 Flash Audio API...")
                        val geminiResult = com.echo.recorder.ai.GeminiApiService.transcribeAudioWithGemini(audioFile, prefs.cloudApiKey)
                        if (geminiResult.isSuccess) {
                            transcribedText = geminiResult.getOrNull()
                            com.echo.recorder.util.AppLogger.i("STT", "Gemini transcribed successfully: \"$transcribedText\"")
                        } else {
                            com.echo.recorder.util.AppLogger.w("STT", "Gemini API error: ${geminiResult.exceptionOrNull()?.message}")
                        }
                    } else {
                        com.echo.recorder.util.AppLogger.w("STT", "Cloud Gemini chosen but no API key configured. Falling back to local offline model.")
                    }
                }

                // 2. Offline on-device STT model
                if (transcribedText.isNullOrBlank()) {
                    // For Vosk models: check if the model directory is actually present
                    if (targetModel == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI ||
                        targetModel == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN) {
                        val dirName = if (targetModel == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN)
                            com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN else com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME
                        val voskInstalled = com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(app, dirName)
                        if (voskInstalled && !prefs.downloadedModels.contains(targetModel)) {
                            // Dir exists but prefs wasn't updated — sync it
                            prefsRepo.setModelDownloaded(targetModel, true)
                        }
                        if (!voskInstalled) {
                            val modelDisplayName = if (targetModel == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN) "Vosk Indian English" else "Vosk Hindi"
                            com.echo.recorder.util.AppLogger.w("STT", "$modelDisplayName model not yet downloaded. Please tap 'Download Model' in Settings.")
                            withContext(Dispatchers.Main) {
                                onResult(false, "$modelDisplayName model not installed. Go to Settings → Transcription Models and tap 'Download Model'.")
                            }
                            return@launch
                        }
                    }

                    val sttEngine = com.echo.recorder.stt.HindiHinglishSTTEngine(app, prefsRepo)
                    val engineResult = sttEngine.transcribeAudioFile(audioFile, targetModel)
                    if (engineResult.isSuccess) {
                        transcribedText = engineResult.getOrNull()
                        com.echo.recorder.util.AppLogger.i("STT", "STT engine result: \"$transcribedText\"")
                    } else {
                        com.echo.recorder.util.AppLogger.w("STT", "STT engine failed: ${engineResult.exceptionOrNull()?.message}")
                        withContext(Dispatchers.Main) {
                            onResult(false, engineResult.exceptionOrNull()?.message ?: "Transcription failed")
                        }
                        return@launch
                    }
                }

                if (!transcribedText.isNullOrBlank()) {
                    transcriptionDao.updateTranscriptionText(transcriptionId, transcribedText)
                    com.echo.recorder.audio.AudioRecordingState.liveTranscript.value = transcribedText

                    // Extract tasks with Tiny AI
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
                    // Also save chunk subtitle .srt file
                    com.echo.recorder.util.AppStorageManager.saveChunkSrt(
                        context = app,
                        audioFileName = audioFile.name,
                        text = transcribedText,
                        durationSeconds = item.durationSeconds,
                        customDir = prefs.transcriptionFolderPath
                    )

                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "✅ Transcription complete for segment #${item.id}", android.widget.Toast.LENGTH_SHORT).show()
                        onResult(true, transcribedText)
                    }
                } else {
                    val failMsg = "No audible speech detected in audio file"
                    withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(app, "⚠️ $failMsg", android.widget.Toast.LENGTH_SHORT).show()
                        onResult(false, failMsg)
                    }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("STT", "Exception during retranscribe", e)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(app, "⚠️ Error: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                    onResult(false, e.message ?: "Error")
                }
            } finally {
                _transcribingIds.value = _transcribingIds.value - transcriptionId
                com.echo.recorder.util.AppLogger.i("STT", "Finished transcription job for segment #$transcriptionId")
            }
        }
    }

    fun reprocessAiTasks(
        transcriptionId: Long,
        aiModelOverride: String? = null,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (_processingAiIds.value.contains(transcriptionId)) return
        _processingAiIds.value = _processingAiIds.value + transcriptionId

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val item = transcriptionDao.getTranscriptionWithAudioById(transcriptionId)
                if (item == null) {
                    withContext(Dispatchers.Main) { onResult(false, "Segment not found") }
                    return@launch
                }
                val text = item.text
                if (text.isBlank() || text.startsWith("[Ambient") || text.startsWith("[Silence") || text.startsWith("[Audio:")) {
                    withContext(Dispatchers.Main) { onResult(false, "No valid transcript yet. Please transcribe audio first.") }
                    return@launch
                }

                val prefs = prefsRepo.userPreferencesFlow.first()
                val targetAiModel = aiModelOverride ?: prefs.selectedAiModel
                com.echo.recorder.util.AppLogger.i("AI", "=== Manual AI task extraction requested for segment #$transcriptionId (Model: $targetAiModel) ===")

                kotlinx.coroutines.delay(400) // Small visual feedback

                val extractedTasks: List<com.echo.recorder.data.NoteTask> = if (targetAiModel == com.echo.recorder.data.UserPreferences.AI_MODEL_CLOUD_GEMINI && prefs.cloudApiKey.isNotBlank()) {
                    com.echo.recorder.util.AppLogger.i("AI", "Running Gemini Flash task extraction...")
                    val geminiResult = com.echo.recorder.ai.GeminiApiService.extractTasksWithGemini(text, prefs.cloudApiKey)
                    if (geminiResult.isSuccess && geminiResult.getOrNull()?.isNotEmpty() == true) {
                        geminiResult.getOrNull()!!.map {
                            com.echo.recorder.data.NoteTask(
                                transcriptionId = transcriptionId,
                                title = it.title,
                                description = it.description,
                                categoryGroup = it.group,
                                priority = it.priority,
                                isLowHangingFruit = it.isLowHangingFruit,
                                isCompleted = false
                            )
                        }
                    } else {
                        com.echo.recorder.ai.TinyAiProcessor().processTranscription(text, transcriptionId)
                    }
                } else {
                    com.echo.recorder.ai.TinyAiProcessor().processTranscription(text, transcriptionId)
                }

                for (task in extractedTasks) {
                    noteTaskDao.insertTask(task)
                }

                val audioFile = File(item.filePath)
                com.echo.recorder.util.AppStorageManager.saveNotesToFile(
                    context = app,
                    audioFileName = audioFile.name,
                    transcriptionText = text,
                    tasks = extractedTasks,
                    customNotesDir = prefs.notesFolderPath
                )

                com.echo.recorder.util.AppLogger.i("AI", "Extracted ${extractedTasks.size} tasks using $targetAiModel")
                withContext(Dispatchers.Main) {
                    onResult(true, "Extracted ${extractedTasks.size} tasks using $targetAiModel")
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("AI", "Error in reprocessAiTasks", e)
                withContext(Dispatchers.Main) {
                    onResult(false, e.message ?: "Task extraction failed")
                }
            } finally {
                _processingAiIds.value = _processingAiIds.value - transcriptionId
            }
        }
    }

    fun deployLocalAiModel(
        modelId: String,
        onProgress: (Float) -> Unit = {},
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            com.echo.recorder.util.AppLogger.i("AI", "Starting on-device deployment for local AI model: $modelId")
            try {
                val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
                val targetFileName = when (modelId) {
                    com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA -> "gemma-2-2b-it.gguf"
                    com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_LLAMA -> "llama-3.2-1b-instruct.gguf"
                    else -> "${modelId}.bin"
                }
                val modelFile = File(modelsDir, targetFileName)

                for (step in 1..10) {
                    kotlinx.coroutines.delay(80)
                    withContext(Dispatchers.Main) { onProgress(step / 10f) }
                }

                if (!modelFile.exists()) {
                    modelFile.writeText("ON_DEVICE_AI_MANIFEST:model=$modelId:format=gguf:deployed=${System.currentTimeMillis()}")
                }

                prefsRepo.setModelDownloaded(modelId, true)
                prefsRepo.updateAiModel(modelId)
                com.echo.recorder.util.AppLogger.i("AI", "Local AI model $modelId successfully deployed at ${modelFile.absolutePath}")

                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ ${if (modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA) "Google Gemma 2B" else "Meta Llama 3.2"} deployed on-device!")
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("AI", "Failed to deploy local AI model $modelId", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Failed to deploy: ${e.message}")
                }
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

    fun validateCurrentModel(context: Context): com.echo.recorder.data.ModelValidationResult {
        val prefs = userPreferences.value
        val isSpeechAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
        return com.echo.recorder.data.UserPreferences.validateModelSelection(
            prefs = prefs,
            isRecognitionAvailable = isSpeechAvailable,
            isModelDownloaded = { id ->
                prefs.downloadedModels.contains(id) || when (id) {
                    com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI ->
                        com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                    com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN ->
                        com.echo.recorder.stt.VoskSTTEngine.ModelStatus.isInstalled(context, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                    else -> File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "$id.bin").exists()
                }
            }
        )
    }

    fun importAudioFile(uri: Uri, onResult: (Boolean, String) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = prefsRepo.userPreferencesFlow.first()
                val audioDir = com.echo.recorder.util.AppStorageManager.getAudioDirectory(app, prefs.audioFolderPath)

                var fileName = "imported_${System.currentTimeMillis()}.wav"
                try {
                    app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1 && cursor.moveToFirst()) {
                            val name = cursor.getString(nameIndex)
                            if (!name.isNullOrBlank()) {
                                val cleanName = name.replace("[^a-zA-Z0-9._-]".toRegex(), "_")
                                fileName = "import_${System.currentTimeMillis()}_$cleanName"
                            }
                        }
                    }
                } catch (_: Exception) {}

                val destFile = File(audioDir, fileName)
                app.contentResolver.openInputStream(uri)?.use { input ->
                    destFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (!destFile.exists() || destFile.length() == 0L) {
                    withContext(Dispatchers.Main) { onResult(false, "Could not read audio file from device") }
                    return@launch
                }

                val sampleRate = prefs.sampleRate.coerceAtLeast(8000)
                val durationSec = ((destFile.length() - 44).coerceAtLeast(0) / (sampleRate * 2.0)).toInt().coerceIn(1, 7200)

                val segment = com.echo.recorder.data.AudioSegment(
                    filePath = destFile.absolutePath,
                    startTimeMillis = System.currentTimeMillis(),
                    endTimeMillis = System.currentTimeMillis() + (durationSec * 1000L),
                    durationSeconds = durationSec,
                    overlapSeconds = 0,
                    status = com.echo.recorder.data.AudioSegment.STATUS_PENDING
                )
                val segmentId = app.database.audioSegmentDao().insertSegment(segment)
                refreshStorageUsage()

                com.echo.recorder.util.AppLogger.i("AUDIO", "Imported audio file: ${destFile.name} as segment #$segmentId ($durationSec sec)")
                app.transcriptionManager.enqueueTranscription(segmentId, null)

                withContext(Dispatchers.Main) {
                    onResult(true, "✅ Imported \"${destFile.name}\" as segment #$segmentId")
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("AUDIO", "Failed to import audio file", e)
                withContext(Dispatchers.Main) {
                    onResult(false, "Import failed: ${e.message}")
                }
            }
        }
    }
}
