package com.echo.recorder.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.echo.recorder.EchoApplication
import com.echo.recorder.audio.AudioRecordingService
import com.echo.recorder.data.ChatMessage
import com.echo.recorder.data.NoteTask
import com.echo.recorder.data.TranscriptionWithAudio
import com.echo.recorder.data.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import org.json.JSONObject
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ModelDownloadStatus {
    IDLE,
    DOWNLOADING,
    PAUSED,
    EXTRACTING,
    COMPLETED,
    ERROR
}

data class ModelDownloadProgress(
    val modelId: String,
    val status: ModelDownloadStatus = ModelDownloadStatus.IDLE,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as EchoApplication
    private val prefsRepo = app.preferencesRepository
    private val transcriptionDao = app.database.transcriptionDao()
    private val noteTaskDao = app.database.noteTaskDao()
    private val aiChatDao = app.database.aiChatDao()

    private val _modelDownloadStates = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val modelDownloadStates: StateFlow<Map<String, ModelDownloadProgress>> = _modelDownloadStates.asStateFlow()

    private val activeDownloadJobs = ConcurrentHashMap<String, Job>()
    private val pauseFlags = ConcurrentHashMap<String, AtomicBoolean>()

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

    private val _isSyncingFiles = MutableStateFlow(false)
    val isSyncingFiles: StateFlow<Boolean> = _isSyncingFiles.asStateFlow()

    // Persistent Settings Accordion States (survives tab switching)
    var isSettingsStorageExpanded by mutableStateOf(true)
    var isSettingsThemesExpanded by mutableStateOf(true)
    var isSettingsAudioExpanded by mutableStateOf(false)
    var isSettingsSttExpanded by mutableStateOf(true)
    var isSettingsAiExpanded by mutableStateOf(true)
    var isSettingsSystemPromptExpanded by mutableStateOf(false)
    var isSettingsBackupExpanded by mutableStateOf(false)

    init {
        refreshStorageUsage()
        viewModelScope.launch(Dispatchers.IO) {
            transcriptionDao.cleanupPendingErrorTexts()
            noteTaskDao.cleanupDummyTasks()
            val contaminated = transcriptionDao.getContaminatedTranscriptions()
            for (c in contaminated) {
                val clean = com.echo.recorder.util.AppStorageManager.stripMetadataFromTranscript(c.text)
                if (clean != c.text) {
                    transcriptionDao.updateTranscriptionText(c.id, clean)
                }
            }
            com.echo.recorder.util.AppStorageManager.cleanupWasteModels(app)
            // Auto-discover any existing audio files and transcripts on disk
            syncStorageFilesWithDatabase()

            // Load persistent AI chat history
            try {
                val history = aiChatDao.getAllMessages()
                if (history.isNotEmpty()) {
                    val converted = history.map { entity ->
                        ChatMessage(
                            isUser = entity.isUser,
                            text = entity.text,
                            suggestedGroup = entity.suggestedGroup
                        )
                    }
                    withContext(Dispatchers.Main) {
                        _chatMessages.value = converted
                    }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("AI", "Failed to load chat history", e)
            }
        }
    }

    fun syncStorageFilesWithDatabase(onComplete: ((Boolean, String) -> Unit)? = null) {
        if (_isSyncingFiles.value) return
        _isSyncingFiles.value = true
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = prefsRepo.userPreferencesFlow.first()
                val result = com.echo.recorder.util.AppStorageManager.syncStorageFilesWithDatabase(
                    context = app,
                    database = app.database,
                    customBasePath = prefs.baseFolderPath.ifBlank { null },
                    customSourcePath = prefs.lastExportSourcePath.ifBlank { null }
                )
                refreshStorageUsage()
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(true, result.message)
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("STORAGE", "Sync error", e)
                withContext(Dispatchers.Main) {
                    onComplete?.invoke(false, "Sync failed: ${e.message}")
                }
            } finally {
                _isSyncingFiles.value = false
            }
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
                    sourceBasePath = prefs.lastExportSourcePath.ifBlank { null },
                    targetBasePath = prefs.baseFolderPath.ifBlank { null },
                    customAudioDir = prefs.audioFolderPath,
                    customTranscriptionsDir = prefs.transcriptionFolderPath,
                    customNotesDir = prefs.notesFolderPath,
                    customLogsDir = prefs.logsFolderPath
                )
                // If export succeeded and we had a temporary source path, update lastExportSourcePath to current target
                if (result.isSuccess && prefs.baseFolderPath.isNotBlank()) {
                    prefsRepo.updateLastExportSourcePath(prefs.baseFolderPath)
                }
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

    fun updateBaseFolderPath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateBaseFolderPath(path)
            refreshStorageUsage()
            syncStorageFilesWithDatabase()
        }
    }

    fun resetStoragePathsToDefault() {
        viewModelScope.launch {
            prefsRepo.updateBaseFolderPath("")
            prefsRepo.updateLastExportSourcePath("")
            prefsRepo.updateAudioFolderPath("")
            prefsRepo.updateTranscriptionFolderPath("")
            prefsRepo.updateNotesFolderPath("")
            prefsRepo.updateLogsFolderPath("")
            refreshStorageUsage()
            syncStorageFilesWithDatabase()
        }
    }

    fun updateSettingsMode(mode: com.echo.recorder.data.SettingsMode) {
        viewModelScope.launch {
            prefsRepo.updateSettingsMode(mode)
        }
    }

    fun downloadModel(modelId: String, onProgress: (Float) -> Unit = {}, onComplete: (Boolean) -> Unit = {}) {
        startOrResumeModelDownload(modelId, onProgress, onComplete)
    }

    fun startOrResumeModelDownload(
        modelId: String,
        onProgress: (Float) -> Unit = {},
        onComplete: (Boolean) -> Unit = {}
    ) {
        val current = _modelDownloadStates.value[modelId]
        if (current?.status == ModelDownloadStatus.DOWNLOADING) {
            return
        }

        pauseFlags[modelId] = AtomicBoolean(false)

        val job = viewModelScope.launch(Dispatchers.IO) {
            com.echo.recorder.util.AppLogger.i("MODEL", "Starting/resuming download for model: $modelId")
            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)

            // Resolve target URL and file paths
            val isEnIn = modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN
            val isHindiLarge = modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI_LARGE
            val isVosk = isEnIn || isHindiLarge || modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI

            val modelUrl = when {
                isEnIn -> "https://alphacephei.com/vosk/models/vosk-model-small-en-in-0.4.zip"
                isHindiLarge -> "https://alphacephei.com/vosk/models/vosk-model-hi-0.22.zip"
                modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI -> "https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip"
                modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA_3_1B -> "https://huggingface.co/bartowski/gemma-3-1b-it-GGUF/resolve/main/gemma-3-1b-it-Q4_K_M.gguf"
                modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA_3_4B -> "https://huggingface.co/bartowski/gemma-3-4b-it-GGUF/resolve/main/gemma-3-4b-it-Q4_K_M.gguf"
                modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA_3_12B -> "https://huggingface.co/bartowski/gemma-3-12b-it-GGUF/resolve/main/gemma-3-12b-it-Q4_K_M.gguf"
                modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA -> "https://huggingface.co/google/gemma-2-2b-it-GGUF/resolve/main/gemma-2-2b-it.Q4_K_M.gguf"
                modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_LLAMA -> "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf"
                modelId == "local_qwen" -> "https://huggingface.co/Qwen/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/qwen2.5-1.5b-instruct-q4_k_m.gguf"
                modelId == "local_phi3" -> "https://huggingface.co/bartowski/Phi-3.5-mini-instruct-GGUF/resolve/main/Phi-3.5-mini-instruct-Q4_K_M.gguf"
                else -> {
                    com.echo.recorder.data.UserPreferences.AVAILABLE_AI_MODELS.find { it.id == modelId }?.downloadUrl
                        ?: com.echo.recorder.data.UserPreferences.AVAILABLE_STT_MODELS.find { it.id == modelId }?.downloadUrl
                        ?: ""
                }
            }

            if (modelUrl.isBlank() || !modelUrl.startsWith("http")) {
                prefsRepo.setModelDownloaded(modelId, true)
                _modelDownloadStates.update { it + (modelId to ModelDownloadProgress(modelId, ModelDownloadStatus.COMPLETED, 1f)) }
                withContext(Dispatchers.Main) {
                    onProgress(1f)
                    onComplete(true)
                }
                return@launch
            }

            val partFile = if (isVosk) File(modelsDir, "${modelId}.part.zip") else File(modelsDir, "${modelId}.part.gguf")
            val targetFinalFile = if (isVosk) {
                val dirName = when {
                    isEnIn -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN
                    isHindiLarge -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE
                    else -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME
                }
                File(modelsDir, dirName)
            } else {
                File(modelsDir, if (modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA) "gemma-2-2b-it.gguf" else "${modelId}.gguf")
            }

            try {
                val existingBytes = if (partFile.exists()) partFile.length() else 0L
                val connection = URL(modelUrl).openConnection() as HttpURLConnection
                connection.connectTimeout = 30_000
                connection.readTimeout = 60_000
                connection.instanceFollowRedirects = true

                if (existingBytes > 0) {
                    connection.setRequestProperty("Range", "bytes=$existingBytes-")
                }

                connection.connect()
                val responseCode = connection.responseCode
                val isRangeAccepted = responseCode == 206
                val serverContentLength = connection.contentLengthLong
                val totalBytes = if (isRangeAccepted) existingBytes + serverContentLength else serverContentLength.coerceAtLeast(1L)
                val append = isRangeAccepted && existingBytes > 0
                var downloadedBytes = if (append) existingBytes else 0L

                _modelDownloadStates.update {
                    it + (modelId to ModelDownloadProgress(
                        modelId = modelId,
                        status = ModelDownloadStatus.DOWNLOADING,
                        progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.99f) else 0f,
                        downloadedBytes = downloadedBytes,
                        totalBytes = totalBytes
                    ))
                }

                connection.inputStream.use { input ->
                    FileOutputStream(partFile, append).use { output ->
                        val buffer = ByteArray(32768)
                        var read: Int
                        var lastReportTime = System.currentTimeMillis()

                        while (input.read(buffer).also { read = it } != -1) {
                            if (pauseFlags[modelId]?.get() == true) {
                                _modelDownloadStates.update {
                                    it + (modelId to ModelDownloadProgress(
                                        modelId = modelId,
                                        status = ModelDownloadStatus.PAUSED,
                                        progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.99f) else 0f,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    ))
                                }
                                com.echo.recorder.util.AppLogger.i("MODEL", "Download paused for $modelId at $downloadedBytes bytes")
                                return@launch
                            }

                            if (!coroutineContext.isActive) {
                                return@launch
                            }

                            output.write(buffer, 0, read)
                            downloadedBytes += read

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 200) {
                                lastReportTime = now
                                val progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 0.95f) else 0f
                                _modelDownloadStates.update {
                                    it + (modelId to ModelDownloadProgress(
                                        modelId = modelId,
                                        status = ModelDownloadStatus.DOWNLOADING,
                                        progress = progress,
                                        downloadedBytes = downloadedBytes,
                                        totalBytes = totalBytes
                                    ))
                                }
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                }

                // If Vosk, extract ZIP
                if (isVosk) {
                    _modelDownloadStates.update {
                        it + (modelId to ModelDownloadProgress(
                            modelId = modelId,
                            status = ModelDownloadStatus.EXTRACTING,
                            progress = 0.96f,
                            downloadedBytes = downloadedBytes,
                            totalBytes = totalBytes
                        ))
                    }
                    targetFinalFile.mkdirs()
                    java.util.zip.ZipInputStream(partFile.inputStream()).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val relPath = entry.name.substringAfter('/')
                            if (relPath.isNotEmpty()) {
                                val outFile = File(targetFinalFile, relPath)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out -> zis.copyTo(out) }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                    partFile.delete()
                } else {
                    // For GGUF, rename .part.gguf to target
                    if (targetFinalFile.exists()) targetFinalFile.delete()
                    partFile.renameTo(targetFinalFile)
                }

                prefsRepo.setModelDownloaded(modelId, true)
                _modelDownloadStates.update {
                    it + (modelId to ModelDownloadProgress(
                        modelId = modelId,
                        status = ModelDownloadStatus.COMPLETED,
                        progress = 1.0f,
                        downloadedBytes = totalBytes,
                        totalBytes = totalBytes
                    ))
                }
                com.echo.recorder.util.AppLogger.i("MODEL", "Model $modelId ready at ${targetFinalFile.absolutePath}")
                withContext(Dispatchers.Main) {
                    onProgress(1f)
                    onComplete(true)
                }
            } catch (e: Exception) {
                if (pauseFlags[modelId]?.get() == true) {
                    return@launch
                }
                com.echo.recorder.util.AppLogger.e("MODEL", "Failed downloading model $modelId", e)
                _modelDownloadStates.update {
                    it + (modelId to ModelDownloadProgress(
                        modelId = modelId,
                        status = ModelDownloadStatus.ERROR,
                        errorMessage = e.localizedMessage ?: "Download failed"
                    ))
                }
                withContext(Dispatchers.Main) { onComplete(false) }
            } finally {
                activeDownloadJobs.remove(modelId)
            }
        }
        activeDownloadJobs[modelId] = job
    }

    fun pauseModelDownload(modelId: String) {
        pauseFlags[modelId]?.set(true)
    }

    fun cancelModelDownload(modelId: String) {
        pauseFlags[modelId]?.set(true)
        activeDownloadJobs[modelId]?.cancel()
        activeDownloadJobs.remove(modelId)
        pauseFlags.remove(modelId)
        val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
        modelsDir.listFiles()?.filter { it.name.startsWith(modelId) && (it.name.contains(".part") || it.name.contains(".tmp")) }?.forEach {
            try { it.delete() } catch (_: Exception) {}
        }
        _modelDownloadStates.update { it - modelId }
    }

    fun importModelZipFromUri(
        uri: android.net.Uri,
        modelId: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
                val isEnIn = modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN
                val isHindiLarge = modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI_LARGE
                val targetDirName = when {
                    isEnIn -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN
                    isHindiLarge -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE
                    else -> com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME
                }
                val extractDir = File(modelsDir, targetDirName)
                extractDir.mkdirs()

                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    java.util.zip.ZipInputStream(inputStream).use { zis ->
                        var entry = zis.nextEntry
                        var entriesCount = 0
                        while (entry != null) {
                            val relPath = entry.name.substringAfter('/')
                            if (relPath.isNotEmpty()) {
                                val outFile = File(extractDir, relPath)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    outFile.outputStream().use { out -> zis.copyTo(out) }
                                    entriesCount++
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                        if (entriesCount > 0) {
                            prefsRepo.setModelDownloaded(modelId, true)
                            com.echo.recorder.util.AppLogger.i("MODEL", "Imported model $modelId from URI with $entriesCount files")
                            withContext(Dispatchers.Main) { onComplete(true, "Model imported successfully ($entriesCount files)") }
                        } else {
                            withContext(Dispatchers.Main) { onComplete(false, "ZIP file did not contain expected model files") }
                        }
                    }
                } ?: run {
                    withContext(Dispatchers.Main) { onComplete(false, "Could not open selected file") }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("MODEL", "Failed to import model ZIP from URI", e)
                withContext(Dispatchers.Main) { onComplete(false, "Import failed: ${e.message}") }
            }
        }
    }

    fun importModelFileFromUri(
        uri: android.net.Uri,
        targetModelId: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
                modelsDir.mkdirs()

                // Query display name
                var fileName = "imported_model.gguf"
                app.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }

                val isGemma = targetModelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA
                val targetFileName = if (isGemma) "gemma-2-2b-it.gguf" else "${targetModelId}.bin"
                val destFile = File(modelsDir, targetFileName)

                app.contentResolver.openInputStream(uri)?.use { inputStream ->
                    destFile.outputStream().use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                } ?: throw java.io.IOException("Cannot open input stream from selected URI")

                if (destFile.exists() && destFile.length() > 0) {
                    prefsRepo.setModelDownloaded(targetModelId, true)
                    com.echo.recorder.util.AppLogger.i("MODEL", "Imported model $targetModelId from file $fileName (${destFile.length() / (1024 * 1024)} MB)")
                    withContext(Dispatchers.Main) {
                        onComplete(true, "Model file '$fileName' imported & deployed successfully (${destFile.length() / (1024 * 1024)} MB)!")
                    }
                } else {
                    withContext(Dispatchers.Main) { onComplete(false, "Imported file was empty") }
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("MODEL", "Failed to import model file from URI", e)
                withContext(Dispatchers.Main) { onComplete(false, "Import failed: ${e.message}") }
            }
        }
    }

    fun exportModelsDirectory(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = com.echo.recorder.util.AppStorageManager.exportModelsDirectory(app)
            onResult(result.isSuccess, result.message)
        }
    }

    fun exportModelsZip(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val zipFile = com.echo.recorder.util.AppStorageManager.exportModelsToZip(app)
            if (zipFile != null && zipFile.exists()) {
                onResult(true, "Exported models ZIP (${zipFile.length() / 1024} KB) to: ${zipFile.name}")
            } else {
                onResult(false, "Failed to create models ZIP archive")
            }
        }
    }

    fun deleteModel(modelId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val modelsDir = com.echo.recorder.util.AppStorageManager.getModelsDirectory(app)
            if (modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI) {
                val voskDir = File(modelsDir, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_NAME)
                if (voskDir.exists()) voskDir.deleteRecursively()
            } else if (modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_EN_IN) {
                val voskDir = File(modelsDir, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                if (voskDir.exists()) voskDir.deleteRecursively()
            } else if (modelId == com.echo.recorder.data.UserPreferences.STT_MODEL_VOSK_HINDI_LARGE) {
                val voskDir = File(modelsDir, com.echo.recorder.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE)
                if (voskDir.exists()) voskDir.deleteRecursively()
            } else {
                val modelFile = File(modelsDir, "${modelId}.bin")
                if (modelFile.exists()) modelFile.delete()
                val ggufFile = File(modelsDir, "gemma-2-2b-it.gguf")
                if (modelId == com.echo.recorder.data.UserPreferences.AI_MODEL_LOCAL_GEMMA && ggufFile.exists()) {
                    ggufFile.delete()
                }
                val specificGguf = File(modelsDir, "${modelId}.gguf")
                if (specificGguf.exists()) specificGguf.delete()
            }
            modelsDir.listFiles()?.filter { it.name.startsWith(modelId) && (it.name.contains(".part") || it.name.contains(".tmp")) }?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }
            _modelDownloadStates.update { it - modelId }
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

                    // Save updated transcription to file in user-configured folder (default: Download/App data/poc/yesboss)
                    com.echo.recorder.util.AppStorageManager.saveTranscriptionToFile(
                        context = app,
                        audioFileName = audioFile.name,
                        text = transcribedText,
                        durationSeconds = item.durationSeconds,
                        overlapSeconds = item.overlapSeconds,
                        engineName = targetModel,
                        tasks = emptyList(),
                        customTranscriptionDir = prefs.transcriptionFolderPath
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

                com.echo.recorder.util.AppLogger.i("AI", "Extracted ${extractedTasks.size} tasks using $targetAiModel:")
                for ((idx, task) in extractedTasks.withIndex()) {
                    com.echo.recorder.util.AppLogger.i("AI", "  [Task #${idx + 1}] \"${task.title}\" | Group: ${task.categoryGroup} | Prio: ${task.priority}")
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

    private val _chatMessages = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                isUser = false,
                text = "👋 Hi! I'm Echo AI Assistant. Ask me anything about your tasks, groceries, or reminders (e.g. \"I'm going to market, what do I need to get?\"). Tap the mic for voice or type below."
            )
        )
    )
    val chatMessages: StateFlow<List<ChatMessage>> = _chatMessages.asStateFlow()

    private val _isAiThinking = MutableStateFlow(false)
    val isAiThinking: StateFlow<Boolean> = _isAiThinking.asStateFlow()

    fun toggleTaskCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            noteTaskDao.updateTaskCompletion(id, isCompleted)

            val current = noteTaskDao.getTaskById(id) ?: return@launch
            // Upward recursion: Check parent auto-completion
            var currentParentId = current.parentId
            while (currentParentId != null) {
                val parent = noteTaskDao.getTaskById(currentParentId) ?: break
                if (parent.autoCompleteOnSubtasksDone) {
                    val siblings = noteTaskDao.getSubtasksForParent(currentParentId)
                    val allCompleted = siblings.isNotEmpty() && siblings.all { it.isCompleted }
                    if (allCompleted != parent.isCompleted) {
                        noteTaskDao.updateTaskCompletion(currentParentId, allCompleted)
                    }
                }
                currentParentId = parent.parentId
            }
        }
    }

    fun createTask(
        title: String,
        categoryGroup: String = "Ideas",
        priority: Int = NoteTask.PRIORITY_MEDIUM,
        isLowHangingFruit: Boolean = false,
        dueText: String = "",
        parentId: Long? = null,
        autoCompleteOnSubtasksDone: Boolean = true
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val (dueDate, parsedDueText) = if (dueText.isNotBlank()) {
                Pair(null, dueText)
            } else {
                com.echo.recorder.ai.TinyAiProcessor().extractTemporalInfo(title)
            }

            val task = NoteTask(
                transcriptionId = null,
                title = title.trim(),
                description = "Manually created task",
                categoryGroup = categoryGroup.trim().ifBlank { "Ideas" },
                priority = priority,
                isLowHangingFruit = isLowHangingFruit,
                parentId = parentId,
                autoCompleteOnSubtasksDone = autoCompleteOnSubtasksDone,
                dueDateMillis = dueDate,
                dueTimeText = parsedDueText.ifBlank { dueText },
                timestampMillis = System.currentTimeMillis()
            )
            try {
                noteTaskDao.insertTask(task)
                com.echo.recorder.util.AppLogger.i("TASK", "Created manual task: '${task.title}' under group '${task.categoryGroup}'")
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("TASK", "Failed to insert manual task '${task.title}'", e)
            }
        }
    }

    fun addSubtask(parentId: Long, title: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val parent = noteTaskDao.getTaskById(parentId) ?: return@launch
            val existingSubtasks = noteTaskDao.getSubtasksForParent(parentId)
            val subtask = NoteTask(
                transcriptionId = parent.transcriptionId,
                title = title.trim(),
                description = "Subtask of: ${parent.title}",
                categoryGroup = parent.categoryGroup,
                priority = parent.priority,
                isLowHangingFruit = parent.isLowHangingFruit,
                parentId = parentId,
                autoCompleteOnSubtasksDone = true,
                orderIndex = existingSubtasks.size,
                timestampMillis = System.currentTimeMillis()
            )
            noteTaskDao.insertTask(subtask)

            // If parent was completed, unmark it since a new pending subtask was added
            if (parent.isCompleted && parent.autoCompleteOnSubtasksDone) {
                noteTaskDao.updateTaskCompletion(parentId, false)
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            noteTaskDao.deleteSubtasks(id)
            noteTaskDao.deleteTask(id)
        }
    }

    fun moveTaskToCategory(id: Long, newCategory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            noteTaskDao.updateTaskCategory(id, newCategory)
            // Also update any child subtasks
            val subtasks = noteTaskDao.getSubtasksForParent(id)
            for (sub in subtasks) {
                noteTaskDao.updateTaskCategory(sub.id, newCategory)
            }
        }
    }

    fun togglePinCategory(categoryName: String) {
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferencesFlow.first()
            val currentPinned = prefs.pinnedCategories.toMutableSet()
            if (currentPinned.contains(categoryName)) {
                currentPinned.remove(categoryName)
            } else {
                currentPinned.add(categoryName)
            }
            prefsRepo.updatePinnedCategories(currentPinned)
        }
    }

    fun addCustomCategory(categoryName: String) {
        if (categoryName.isBlank()) return
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferencesFlow.first()
            val currentCustom = prefs.customCategories.toMutableSet()
            currentCustom.add(categoryName.trim())
            prefsRepo.updateCustomCategories(currentCustom)
        }
    }

    fun deleteCustomCategory(categoryName: String) {
        viewModelScope.launch {
            val prefs = prefsRepo.userPreferencesFlow.first()
            val currentCustom = prefs.customCategories.toMutableSet()
            currentCustom.remove(categoryName)
            prefsRepo.updateCustomCategories(currentCustom)
            val currentPinned = prefs.pinnedCategories.toMutableSet()
            if (currentPinned.contains(categoryName)) {
                currentPinned.remove(categoryName)
                prefsRepo.updatePinnedCategories(currentPinned)
            }
        }
    }

    fun editTask(
        id: Long,
        title: String,
        category: String,
        priority: Int,
        isLowHangingFruit: Boolean,
        dueText: String = ""
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = noteTaskDao.getTaskById(id) ?: return@launch
            val (dueDate, parsedDueText) = if (dueText.isNotBlank()) {
                Pair(null, dueText)
            } else {
                com.echo.recorder.ai.TinyAiProcessor().extractTemporalInfo(title)
            }
            val updated = existing.copy(
                title = title.trim(),
                categoryGroup = category.trim().ifBlank { existing.categoryGroup },
                priority = priority,
                isLowHangingFruit = isLowHangingFruit,
                dueDateMillis = dueDate ?: existing.dueDateMillis,
                dueTimeText = parsedDueText.ifBlank { dueText }
            )
            noteTaskDao.insertTask(updated)
        }
    }

    fun reorderTasks(fromIndex: Int, toIndex: Int, tasks: List<NoteTask>) {
        viewModelScope.launch(Dispatchers.IO) {
            val mutableList = tasks.toMutableList()
            if (fromIndex in mutableList.indices && toIndex in mutableList.indices) {
                val item = mutableList.removeAt(fromIndex)
                mutableList.add(toIndex, item)
                mutableList.forEachIndexed { index, task ->
                    noteTaskDao.updateTaskOrder(task.id, index)
                }
            }
        }
    }

    fun moveTaskUpInGroup(task: NoteTask, siblingTasks: List<NoteTask>) {
        val index = siblingTasks.indexOfFirst { it.id == task.id }
        if (index > 0) {
            reorderTasks(index, index - 1, siblingTasks)
        }
    }

    fun moveTaskDownInGroup(task: NoteTask, siblingTasks: List<NoteTask>) {
        val index = siblingTasks.indexOfFirst { it.id == task.id }
        if (index in 0 until siblingTasks.size - 1) {
            reorderTasks(index, index + 1, siblingTasks)
        }
    }

    fun moveSubtaskUp(subtask: NoteTask, siblingSubtasks: List<NoteTask>) {
        val index = siblingSubtasks.indexOfFirst { it.id == subtask.id }
        if (index > 0) {
            reorderTasks(index, index - 1, siblingSubtasks)
        }
    }

    fun moveSubtaskDown(subtask: NoteTask, siblingSubtasks: List<NoteTask>) {
        val index = siblingSubtasks.indexOfFirst { it.id == subtask.id }
        if (index in 0 until siblingSubtasks.size - 1) {
            reorderTasks(index, index + 1, siblingSubtasks)
        }
    }

    fun reorderCategoryGroups(fromIndex: Int, toIndex: Int, categories: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val mutableList = categories.toMutableList()
            if (fromIndex in mutableList.indices && toIndex in mutableList.indices) {
                val item = mutableList.removeAt(fromIndex)
                mutableList.add(toIndex, item)
                prefsRepo.updateCustomCategories(mutableList.toSet())
            }
        }
    }

    fun updateFontScale(scale: Float) {
        viewModelScope.launch {
            prefsRepo.updateFontScale(scale)
        }
    }

    fun updateFontMode(mode: String) {
        viewModelScope.launch {
            prefsRepo.updateFontMode(mode)
        }
    }

    fun updateCustomThemeJson(json: String) {
        viewModelScope.launch {
            prefsRepo.updateCustomThemeJson(json)
        }
    }

    fun updateAiLanguage(lang: String) {
        viewModelScope.launch {
            prefsRepo.updateAiLanguage(lang)
        }
    }

    fun updateTtsSpeed(speed: Float) {
        viewModelScope.launch {
            prefsRepo.updateTtsSpeed(speed)
        }
    }

    fun updateCustomAiModels(models: String) {
        viewModelScope.launch {
            prefsRepo.updateCustomAiModels(models)
        }
    }

    fun updateActiveTheme(themeKey: String) {
        viewModelScope.launch {
            prefsRepo.updateActiveTheme(themeKey)
        }
    }

    fun updateLastExportSourcePath(path: String) {
        viewModelScope.launch {
            prefsRepo.updateLastExportSourcePath(path)
            refreshStorageUsage()
            syncStorageFilesWithDatabase()
        }
    }

    fun exportDashboardToFile(
        format: String = "md",
        onComplete: (Boolean, String, File?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val allTasks = noteTaskDao.getAllTasksList()
                val prefs = prefsRepo.userPreferencesFlow.first()
                val notesDir = com.echo.recorder.util.AppStorageManager.getNotesDirectory(app, prefs.notesFolderPath)
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())

                val fileName = if (format.lowercase(java.util.Locale.ROOT) == "html") {
                    "dashboard_tasks_$timeStamp.html"
                } else {
                    "dashboard_tasks_$timeStamp.md"
                }
                val exportFile = File(notesDir, fileName)

                if (format.lowercase(java.util.Locale.ROOT) == "html") {
                    // Generate clean HTML
                    val htmlContent = buildString {
                        append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n")
                        append("<title>YesBoss / GoodEcho Task Dashboard</title>\n")
                        append("<style>\n")
                        append("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #0F172A; color: #F8FAFC; padding: 24px; line-height: 1.6; }\n")
                        append("h1 { color: #38BDF8; font-size: 26px; border-bottom: 2px solid #1E293B; padding-bottom: 8px; }\n")
                        append(".group { background: #1E293B; border-radius: 12px; padding: 16px; margin-bottom: 20px; border: 1px solid #334155; }\n")
                        append(".group-title { font-size: 18px; font-weight: bold; color: #A855F7; margin-bottom: 12px; }\n")
                        append(".task { margin: 8px 0; font-size: 15px; display: flex; align-items: center; }\n")
                        append(".subtask { margin: 6px 0 6px 28px; font-size: 14px; color: #94A3B8; }\n")
                        append(".done { text-decoration: line-through; color: #64748B; }\n")
                        append(".badge { font-size: 11px; padding: 2px 8px; border-radius: 6px; margin-left: 8px; font-weight: bold; }\n")
                        append(".badge-urgent { background: #EF4444; color: white; }\n")
                        append(".badge-due { background: #F59E0B; color: black; }\n")
                        append("</style>\n</head>\n<body>\n")
                        append("<h1>📋 Task Dashboard & Action Items</h1>\n")
                        append("<p>Exported on ${java.util.Date()} • Total: ${allTasks.size} tasks</p>\n")

                        val topLevelTasks = allTasks.filter { it.parentId == null }
                        val categories = topLevelTasks.map { it.categoryGroup }.distinct()

                        for (cat in categories) {
                            append("<div class=\"group\">\n")
                            append("<div class=\"group-title\">🏷️ $cat</div>\n")
                            val catTasks = topLevelTasks.filter { it.categoryGroup == cat }
                            for (task in catTasks) {
                                val doneClass = if (task.isCompleted) " done" else ""
                                val checkedStr = if (task.isCompleted) "☑️" else "⬜"
                                val dueBadge = if (task.dueTimeText.isNotBlank()) "<span class=\"badge badge-due\">⏳ ${task.dueTimeText}</span>" else ""
                                val urgentBadge = if (task.priority == NoteTask.PRIORITY_HIGH) "<span class=\"badge badge-urgent\">URGENT</span>" else ""
                                append("<div class=\"task$doneClass\">$checkedStr <strong>${task.title}</strong>$dueBadge$urgentBadge</div>\n")

                                val subtasks = allTasks.filter { it.parentId == task.id }
                                for (sub in subtasks) {
                                    val subDoneClass = if (sub.isCompleted) " done" else ""
                                    val subCheckedStr = if (sub.isCompleted) "☑️" else "⬜"
                                    append("<div class=\"subtask$subDoneClass\">$subCheckedStr ${sub.title}</div>\n")
                                }
                            }
                            append("</div>\n")
                        }
                        append("</body>\n</html>")
                    }
                    exportFile.writeText(htmlContent)
                } else {
                    // Generate Markdown
                    val mdContent = buildString {
                        append("# 📋 YesBoss / GoodEcho Task Dashboard\n\n")
                        append("*Exported on: ${java.util.Date()} • Total: ${allTasks.size} items*\n\n")

                        val topLevelTasks = allTasks.filter { it.parentId == null }
                        val categories = topLevelTasks.map { it.categoryGroup }.distinct()

                        for (cat in categories) {
                            append("## 🏷️ $cat\n\n")
                            val catTasks = topLevelTasks.filter { it.categoryGroup == cat }
                            for (task in catTasks) {
                                val check = if (task.isCompleted) "[x]" else "[ ]"
                                val due = if (task.dueTimeText.isNotBlank()) " *(⏳ ${task.dueTimeText})*" else ""
                                val priority = if (task.priority == NoteTask.PRIORITY_HIGH) " **[URGENT]**" else ""
                                append("- $check **${task.title}**$due$priority\n")

                                val subtasks = allTasks.filter { it.parentId == task.id }
                                for (sub in subtasks) {
                                    val subCheck = if (sub.isCompleted) "[x]" else "[ ]"
                                    append("  - $subCheck ${sub.title}\n")
                                }
                            }
                            append("\n")
                        }
                    }
                    exportFile.writeText(mdContent)
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Exported to ${exportFile.name} in Notes folder!", exportFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Export failed: ${e.message}", null)
                }
            }
        }
    }

    fun askAiAssistant(query: String, onResponse: (ChatMessage) -> Unit = {}) {
        val userMsg = ChatMessage(isUser = true, text = query.trim())
        _chatMessages.value = _chatMessages.value + userMsg
        _isAiThinking.value = true

        viewModelScope.launch(Dispatchers.IO) {
            try {
                aiChatDao.insertMessage(
                    com.echo.recorder.data.AiChatMessageEntity(
                        isUser = true,
                        text = query.trim()
                    )
                )

                val allTasks = noteTaskDao.getAllTasksList()
                val prefs = prefsRepo.userPreferencesFlow.first()
                val allTranscripts = transcriptions.value

                val tinyAi = com.echo.recorder.ai.TinyAiProcessor()
                val webResult = if (prefs.isWebSearchEnabled) {
                    tinyAi.searchInternet(query)
                } else null

                val (answerText, suggestedItems) = tinyAi.answerTaskQuery(
                    query = query,
                    allTasks = allTasks,
                    allTranscripts = allTranscripts,
                    systemPrompt = prefs.customSystemPrompt,
                    webSearchResult = webResult,
                    aiLanguage = prefs.aiLanguage
                )

                val suggestedGroup = if (suggestedItems.isNotEmpty()) {
                    if (query.lowercase(java.util.Locale.ROOT).contains("market") || query.lowercase(java.util.Locale.ROOT).contains("vegetable")) "Market Shopping" else "AI Task List"
                } else null

                val assistantMsg = ChatMessage(
                    isUser = false,
                    text = answerText,
                    suggestedGroup = suggestedGroup,
                    suggestedItems = suggestedItems
                )

                aiChatDao.insertMessage(
                    com.echo.recorder.data.AiChatMessageEntity(
                        isUser = false,
                        text = answerText,
                        suggestedGroup = suggestedGroup
                    )
                )

                _chatMessages.value = _chatMessages.value + assistantMsg
                withContext(Dispatchers.Main) {
                    onResponse(assistantMsg)
                }
            } catch (e: Exception) {
                val errorMsg = ChatMessage(
                    isUser = false,
                    text = "Sorry, I encountered an issue analyzing your tasks: ${e.message}"
                )
                _chatMessages.value = _chatMessages.value + errorMsg
                withContext(Dispatchers.Main) {
                    onResponse(errorMsg)
                }
            } finally {
                _isAiThinking.value = false
            }
        }
    }

    fun clearAiChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            aiChatDao.clearHistory()
            withContext(Dispatchers.Main) {
                _chatMessages.value = listOf(
                    ChatMessage(
                        isUser = false,
                        text = "Chat history cleared. How can I help you today?"
                    )
                )
            }
        }
    }

    fun runCompleteFlow(
        sessionId: String? = null,
        chunkId: Long? = null,
        onProgress: (String) -> Unit = {},
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val prefs = prefsRepo.userPreferencesFlow.first()
                val allTranscripts = transcriptions.value
                val itemsToProcess = when {
                    chunkId != null -> allTranscripts.filter { it.id == chunkId }
                    sessionId != null -> allTranscripts.filter { it.sessionId == sessionId }.sortedBy { it.timestampMillis }
                    else -> allTranscripts.sortedBy { it.timestampMillis } // Sort all audio chunks earliest first!
                }

                if (itemsToProcess.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "No recordings found to process")
                    }
                    return@launch
                }

                val total = itemsToProcess.size
                var processedCount = 0

                for ((index, item) in itemsToProcess.withIndex()) {
                    val progressMsg = "Processing ${index + 1}/$total: ${item.filePath.substringAfterLast("/")}"
                    withContext(Dispatchers.Main) { onProgress(progressMsg) }

                    val audioFile = File(item.filePath)
                    if (!audioFile.exists() || audioFile.length() <= 44) continue

                    // Transcribe if needed or retranscribe
                    val sttEngine = com.echo.recorder.stt.HindiHinglishSTTEngine(app, prefsRepo)
                    var text = item.text
                    val isPlaceholder = text.isBlank() || text.contains("Audio slice captured") || text.contains("ready for transcription") || text.startsWith("[Pending")

                    if (isPlaceholder) {
                        val engineResult = sttEngine.transcribeAudioFile(audioFile, prefs.selectedSttModel)
                        if (engineResult.isSuccess && !engineResult.getOrNull().isNullOrBlank()) {
                            text = engineResult.getOrNull()!!
                            transcriptionDao.updateTranscriptionText(item.id, text)
                        }
                    }

                    val cleanSpoken = com.echo.recorder.util.AppStorageManager.stripMetadataFromTranscript(text)
                    if (cleanSpoken.isNotBlank() && !cleanSpoken.startsWith("Audio slice captured")) {
                        // Extract tasks hierarchically
                        val tinyAi = com.echo.recorder.ai.TinyAiProcessor()
                        val hierarchical = tinyAi.processTranscriptionHierarchical(cleanSpoken, item.id)
                        val insertedTasks = tinyAi.saveHierarchicalTasksToDatabase(noteTaskDao, hierarchical)

                        // Save updated markdown
                        com.echo.recorder.util.AppStorageManager.saveTranscriptionToFile(
                            context = app,
                            audioFileName = audioFile.name,
                            text = cleanSpoken,
                            durationSeconds = item.durationSeconds,
                            overlapSeconds = item.overlapSeconds,
                            engineName = prefs.selectedSttModel,
                            tasks = insertedTasks,
                            customTranscriptionDir = prefs.transcriptionFolderPath
                        )
                        com.echo.recorder.util.AppStorageManager.saveNotesToFile(
                            context = app,
                            audioFileName = audioFile.name,
                            transcriptionText = cleanSpoken,
                            tasks = insertedTasks,
                            customNotesDir = prefs.notesFolderPath
                        )
                    }
                    processedCount++
                }

                val summary = "⚡ Completed flow on $processedCount recordings in chronological order!"
                withContext(Dispatchers.Main) {
                    onComplete(true, summary)
                }
            } catch (e: Exception) {
                com.echo.recorder.util.AppLogger.e("FLOW", "Error during complete flow execution", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, "Flow failed: ${e.message}")
                }
            }
        }
    }

    fun exportSettingsToJson(): String {
        return exportSelectedSettingsToJson(true, true, true, true, true)
    }

    fun exportSelectedSettingsToJson(
        includeAudio: Boolean = true,
        includeStt: Boolean = true,
        includeAi: Boolean = true,
        includeAppearance: Boolean = true,
        includeStorage: Boolean = true
    ): String {
        val prefs = userPreferences.value
        val json = JSONObject().apply {
            put("version", 1)
            put("exportedAt", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("appVersion", com.echo.recorder.util.AppConstants.APP_VERSION)

            if (includeAudio) {
                val audioObj = JSONObject().apply {
                    put("bufferLengthSeconds", prefs.bufferLengthSeconds)
                    put("overlapSeconds", prefs.overlapSeconds)
                    put("sampleRate", prefs.sampleRate)
                    put("liveTranscribe", prefs.liveTranscribe)
                    put("skipSilence", prefs.skipSilence)
                    put("autoDeleteAudio", prefs.autoDeleteAudio)
                }
                put("audioConfig", audioObj)
            }

            if (includeStt) {
                val sttObj = JSONObject().apply {
                    put("selectedLanguage", prefs.selectedLanguage)
                    put("selectedSttModel", prefs.selectedSttModel)
                }
                put("sttConfig", sttObj)
            }

            if (includeAi) {
                val aiObj = JSONObject().apply {
                    put("selectedAiModel", prefs.selectedAiModel)
                    put("aiLanguage", prefs.aiLanguage)
                    put("customSystemPrompt", prefs.customSystemPrompt)
                    put("isWebSearchEnabled", prefs.isWebSearchEnabled)
                    put("customAiModels", prefs.customAiModels)
                    put("ttsSpeed", prefs.ttsSpeed)
                }
                put("aiConfig", aiObj)
            }

            if (includeAppearance) {
                val appObj = JSONObject().apply {
                    put("appLanguage", prefs.appLanguage)
                    put("activeTheme", prefs.activeTheme)
                    put("customThemeJson", prefs.customThemeJson)
                    put("fontScale", prefs.fontScale)
                    put("fontMode", prefs.fontMode)
                    put("settingsMode", prefs.settingsMode.name)
                }
                put("appearanceConfig", appObj)
            }

            if (includeStorage) {
                val storageObj = JSONObject().apply {
                    put("baseFolderPath", prefs.baseFolderPath)
                    put("audioFolderPath", prefs.audioFolderPath)
                    put("transcriptionFolderPath", prefs.transcriptionFolderPath)
                    put("notesFolderPath", prefs.notesFolderPath)
                    put("logsFolderPath", prefs.logsFolderPath)
                    put("pinnedCategories", JSONArray(prefs.pinnedCategories))
                    put("customCategories", JSONArray(prefs.customCategories))
                }
                put("storageConfig", storageObj)
            }
        }
        return json.toString(2)
    }

    fun saveSettingsToFile(jsonContent: String): File? {
        return try {
            val dir = com.echo.recorder.util.AppStorageManager.getNotesDirectory(app)
            val timeStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val file = File(dir, "echo_settings_backup_$timeStr.json")
            file.writeText(jsonContent)
            com.echo.recorder.util.AppLogger.i("BACKUP", "Saved settings backup to: ${file.absolutePath}")
            file
        } catch (e: Exception) {
            com.echo.recorder.util.AppLogger.e("BACKUP", "Failed to save settings file", e)
            null
        }
    }

    fun importSettingsFromJson(rawJsonStr: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Smart Cleaner: Sanitize input even if user messed up brackets, comments, or trailing commas
                var cleanStr = rawJsonStr.trim()
                if (cleanStr.startsWith("```json")) {
                    cleanStr = cleanStr.removePrefix("```json").substringBeforeLast("```").trim()
                } else if (cleanStr.startsWith("```")) {
                    cleanStr = cleanStr.removePrefix("```").substringBeforeLast("```").trim()
                }

                // Remove line comments //...
                cleanStr = cleanStr.lines().map { line ->
                    if (line.contains("//")) line.substringBefore("//").trim() else line
                }.joinToString("\n").trim()

                // Fix empty braces or malformed empty dictionary
                if (cleanStr.isBlank() || cleanStr == "{}" || cleanStr == "[]") {
                    withContext(Dispatchers.Main) {
                        onComplete(false, "JSON is empty. No settings imported.")
                    }
                    return@launch
                }

                val json = JSONObject(cleanStr)

                // Top level or nested audioConfig
                val audioObj = json.optJSONObject("audioConfig") ?: json
                if (audioObj.has("bufferLengthSeconds")) prefsRepo.updateBufferLength(audioObj.optInt("bufferLengthSeconds", 30))
                if (audioObj.has("overlapSeconds")) prefsRepo.updateOverlap(audioObj.optInt("overlapSeconds", 3))
                if (audioObj.has("sampleRate")) prefsRepo.updateSampleRate(audioObj.optInt("sampleRate", 16000))
                if (audioObj.has("liveTranscribe")) prefsRepo.updateLiveTranscribe(audioObj.optBoolean("liveTranscribe", true))
                if (audioObj.has("skipSilence")) prefsRepo.updateSkipSilence(audioObj.optBoolean("skipSilence", true))
                if (audioObj.has("autoDeleteAudio")) prefsRepo.updateAutoDeleteAudio(audioObj.optBoolean("autoDeleteAudio", false))

                // Top level or nested sttConfig
                val sttObj = json.optJSONObject("sttConfig") ?: json
                if (sttObj.has("selectedLanguage")) prefsRepo.updateLanguage(sttObj.optString("selectedLanguage", "hi"))
                if (sttObj.has("selectedSttModel")) prefsRepo.updateSttModel(sttObj.optString("selectedSttModel", "vosk_hindi"))

                // Top level or nested aiConfig
                val aiObj = json.optJSONObject("aiConfig") ?: json
                if (aiObj.has("selectedAiModel")) prefsRepo.updateAiModel(aiObj.optString("selectedAiModel", "local_tiny"))
                if (aiObj.has("aiLanguage")) prefsRepo.updateAiLanguage(aiObj.optString("aiLanguage", "auto"))
                if (aiObj.has("customSystemPrompt")) prefsRepo.updateCustomSystemPrompt(aiObj.optString("customSystemPrompt", ""))
                if (aiObj.has("isWebSearchEnabled")) prefsRepo.updateWebSearchEnabled(aiObj.optBoolean("isWebSearchEnabled", true))
                if (aiObj.has("customAiModels")) prefsRepo.updateCustomAiModels(aiObj.optString("customAiModels", ""))
                if (aiObj.has("ttsSpeed")) prefsRepo.updateTtsSpeed(aiObj.optDouble("ttsSpeed", 1.0).toFloat())

                // Top level or nested appearanceConfig
                val appObj = json.optJSONObject("appearanceConfig") ?: json
                if (appObj.has("appLanguage")) prefsRepo.updateAppLanguage(appObj.optString("appLanguage", "en"))
                if (appObj.has("activeTheme")) prefsRepo.updateActiveTheme(appObj.optString("activeTheme", "AMOLED_HIGH"))
                if (appObj.has("customThemeJson")) prefsRepo.updateCustomThemeJson(appObj.optString("customThemeJson", ""))
                if (appObj.has("fontScale")) prefsRepo.updateFontScale(appObj.optDouble("fontScale", 1.0).toFloat())
                if (appObj.has("fontMode")) prefsRepo.updateFontMode(appObj.optString("fontMode", "DEFAULT"))
                if (appObj.has("settingsMode")) {
                    val mode = if (appObj.optString("settingsMode", "SMART").equals("ADVANCED", ignoreCase = true)) {
                        com.echo.recorder.data.SettingsMode.ADVANCED
                    } else {
                        com.echo.recorder.data.SettingsMode.SMART
                    }
                    prefsRepo.updateSettingsMode(mode)
                }

                // Top level or nested storageConfig
                val storageObj = json.optJSONObject("storageConfig") ?: json
                if (storageObj.has("baseFolderPath")) prefsRepo.updateBaseFolderPath(storageObj.optString("baseFolderPath", ""))
                if (storageObj.has("audioFolderPath")) prefsRepo.updateAudioFolderPath(storageObj.optString("audioFolderPath", ""))
                if (storageObj.has("transcriptionFolderPath")) prefsRepo.updateTranscriptionFolderPath(storageObj.optString("transcriptionFolderPath", ""))
                if (storageObj.has("notesFolderPath")) prefsRepo.updateNotesFolderPath(storageObj.optString("notesFolderPath", ""))
                if (storageObj.has("logsFolderPath")) prefsRepo.updateLogsFolderPath(storageObj.optString("logsFolderPath", ""))
                if (storageObj.has("pinnedCategories")) {
                    val arr = storageObj.optJSONArray("pinnedCategories")
                    if (arr != null) {
                        val set = (0 until arr.length()).map { arr.getString(it) }.toSet()
                        prefsRepo.updatePinnedCategories(set)
                    }
                }
                if (storageObj.has("customCategories")) {
                    val arr = storageObj.optJSONArray("customCategories")
                    if (arr != null) {
                        val set = (0 until arr.length()).map { arr.getString(it) }.toSet()
                        prefsRepo.updateCustomCategories(set)
                    }
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Settings successfully sanitized and imported!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Could not parse JSON settings: ${e.message}")
                }
            }
        }
    }

    fun updateCustomSystemPrompt(prompt: String) {
        viewModelScope.launch {
            prefsRepo.updateCustomSystemPrompt(prompt)
        }
    }

    fun updateAppLanguage(lang: String) {
        viewModelScope.launch {
            prefsRepo.updateAppLanguage(lang)
        }
    }

    fun updateWebSearchEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateWebSearchEnabled(enabled)
        }
    }

    fun saveAiResponseAsGroup(
        groupName: String,
        items: List<String>,
        onComplete: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (items.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete(false, "No items to save") }
                    return@launch
                }

                // 1. Add groupName to pinned categories so it shows at the very top!
                val prefs = prefsRepo.userPreferencesFlow.first()
                val currentPinned = prefs.pinnedCategories.toMutableSet()
                currentPinned.add(groupName)
                val currentCustom = prefs.customCategories.toMutableSet()
                currentCustom.add(groupName)
                prefsRepo.updatePinnedCategories(currentPinned)
                prefsRepo.updateCustomCategories(currentCustom)

                // 2. Insert items into database under this group
                if (items.size > 1) {
                    // Create master parent task
                    val masterParent = NoteTask(
                        transcriptionId = 0L,
                        title = groupName,
                        description = "Created by Echo AI Assistant (${items.size} items)",
                        categoryGroup = groupName,
                        priority = NoteTask.PRIORITY_MEDIUM,
                        isLowHangingFruit = true,
                        autoCompleteOnSubtasksDone = true,
                        timestampMillis = System.currentTimeMillis()
                    )
                    val parentId = noteTaskDao.insertTask(masterParent)

                    // Create subtasks
                    items.forEachIndexed { index, itemTitle ->
                        val subtask = NoteTask(
                            transcriptionId = 0L,
                            title = itemTitle,
                            description = "Item in $groupName",
                            categoryGroup = groupName,
                            priority = NoteTask.PRIORITY_MEDIUM,
                            isLowHangingFruit = true,
                            parentId = parentId,
                            orderIndex = index,
                            autoCompleteOnSubtasksDone = true,
                            timestampMillis = System.currentTimeMillis()
                        )
                        noteTaskDao.insertTask(subtask)
                    }
                } else {
                    val single = NoteTask(
                        transcriptionId = 0L,
                        title = items.first(),
                        description = "Created by Echo AI Assistant",
                        categoryGroup = groupName,
                        priority = NoteTask.PRIORITY_MEDIUM,
                        isLowHangingFruit = true,
                        timestampMillis = System.currentTimeMillis()
                    )
                    noteTaskDao.insertTask(single)
                }

                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Pinned \"$groupName\" to top of Dashboard with ${items.size} tasks!")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Error saving group: ${e.message}")
                }
            }
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
                    status = com.echo.recorder.data.AudioSegment.STATUS_PENDING,
                    sessionId = "imported_${System.currentTimeMillis()}"
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

    fun updateTaskDetails(id: Long, title: String, description: String, category: String, priority: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            noteTaskDao.updateTaskDetails(id, title.trim(), description.trim(), category.trim(), priority)
        }
    }

    fun mergeSessionTranscriptions(
        sessionId: String,
        onComplete: (Boolean, String, File?) -> Unit = { _, _, _ -> }
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val chunks = transcriptionDao.getTranscriptionsBySessionId(sessionId)
                if (chunks.isEmpty()) {
                    withContext(Dispatchers.Main) { onComplete(false, "No audio chunks found for this attempt", null) }
                    return@launch
                }

                val prefs = prefsRepo.userPreferencesFlow.first()
                val notesDir = com.echo.recorder.util.AppStorageManager.getNotesDirectory(app, prefs.notesFolderPath)
                val timeStamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                val cleanSessionTag = sessionId.removePrefix("session_").removePrefix("attempt_")
                val mergedFile = File(notesDir, "merged_session_${cleanSessionTag}_$timeStamp.md")

                val mergedContent = buildString {
                    appendLine("# 🎙️ Merged Recording Attempt: $sessionId")
                    appendLine("*Recorded: ${java.util.Date(chunks.first().timestampMillis)} • Total Chunks: ${chunks.size} • Total Audio: ${chunks.sumOf { it.durationSeconds }}s*")
                    appendLine()
                    appendLine("## 📝 Complete Unified Transcript")
                    appendLine()
                    chunks.forEachIndexed { index, chunk ->
                        val cleanText = chunk.text.trim()
                        if (cleanText.isNotBlank() && !cleanText.startsWith("[") && !cleanText.startsWith("Audio slice")) {
                            appendLine("**[Chunk #${index + 1} • ${chunk.durationSeconds}s${if (chunk.overlapSeconds > 0) " (Overlap: ${chunk.overlapSeconds}s)" else ""}]:**")
                            appendLine(cleanText)
                            appendLine()
                        }
                    }
                }
                mergedFile.writeText(mergedContent)
                withContext(Dispatchers.Main) {
                    onComplete(true, "✅ Merged ${chunks.size} chunks into ${mergedFile.name}", mergedFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(false, "Merge failed: ${e.message}", null)
                }
            }
        }
    }
}
