package com.echo.recorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val BUFFER_LENGTH_SECONDS = intPreferencesKey("buffer_length_seconds")
        val OVERLAP_SECONDS = intPreferencesKey("overlap_seconds")
        val SAMPLE_RATE = intPreferencesKey("sample_rate")
        val LIVE_TRANSCRIBE = booleanPreferencesKey("live_transcribe")
        val SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val AUTO_DELETE_AUDIO = booleanPreferencesKey("auto_delete_audio")
        val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
        val SELECTED_STT_MODEL = stringPreferencesKey("selected_stt_model")
        val SELECTED_AI_MODEL = stringPreferencesKey("selected_ai_model")
        val CLOUD_API_KEY = stringPreferencesKey("cloud_api_key")
        val AUDIO_FOLDER_PATH = stringPreferencesKey("audio_folder_path")
        val TRANSCRIPTION_FOLDER_PATH = stringPreferencesKey("transcription_folder_path")
        val NOTES_FOLDER_PATH = stringPreferencesKey("notes_folder_path")
        val LOGS_FOLDER_PATH = stringPreferencesKey("logs_folder_path")
        val BASE_FOLDER_PATH = stringPreferencesKey("base_folder_path")
        val LAST_EXPORT_SOURCE_PATH = stringPreferencesKey("last_export_source_path")
        val ACTIVE_THEME = stringPreferencesKey("active_theme")
        val PINNED_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("pinned_categories")
        val CUSTOM_CATEGORIES = androidx.datastore.preferences.core.stringSetPreferencesKey("custom_categories")
        val DOWNLOADED_MODELS = androidx.datastore.preferences.core.stringSetPreferencesKey("downloaded_models")
        val SETTINGS_MODE = stringPreferencesKey("settings_mode")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val IS_WEB_SEARCH_ENABLED = booleanPreferencesKey("is_web_search_enabled")
        val CUSTOM_SYSTEM_PROMPT = stringPreferencesKey("custom_system_prompt")
        val AI_LANGUAGE = stringPreferencesKey("ai_language")
        val LOG_FILE_EXTENSION = stringPreferencesKey("log_file_extension")
        val HUGGING_FACE_TOKEN = stringPreferencesKey("hugging_face_token")
        val RECORDER_LAYOUT_MODE = stringPreferencesKey("recorder_layout_mode")
        val DEFAULT_ABORT_SELECTION = stringPreferencesKey("default_abort_selection")
        val SHOW_LOGS_TAB = booleanPreferencesKey("show_logs_tab")
        val SHOW_AI_TAB = booleanPreferencesKey("show_ai_tab")
        val HOME_TAB = stringPreferencesKey("home_tab")
        val SHOW_DASHBOARD_TAB = booleanPreferencesKey("show_dashboard_tab")
        val SHOW_RECORDER_TAB = booleanPreferencesKey("show_recorder_tab")
        val SHOW_MEDIA_TAB = booleanPreferencesKey("show_media_tab")
        val SHOW_ASSISTANT_TAB = booleanPreferencesKey("show_assistant_tab")
        val TAB_ORDER = stringPreferencesKey("tab_order")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val FONT_MODE = stringPreferencesKey("font_mode")
        val CUSTOM_THEME_JSON = stringPreferencesKey("custom_theme_json")
        val CUSTOM_AI_MODELS = stringPreferencesKey("custom_ai_models")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            settingsMode = preferences[PreferencesKeys.SETTINGS_MODE]?.let { runCatching { SettingsMode.valueOf(it) }.getOrNull() } ?: SettingsMode.SMART,
            bufferLengthSeconds = preferences[PreferencesKeys.BUFFER_LENGTH_SECONDS] ?: 15,
            overlapSeconds = preferences[PreferencesKeys.OVERLAP_SECONDS] ?: 3,
            sampleRate = preferences[PreferencesKeys.SAMPLE_RATE] ?: UserPreferences.QUALITY_TINIEST,
            liveTranscribe = preferences[PreferencesKeys.LIVE_TRANSCRIBE] ?: true,
            skipSilence = preferences[PreferencesKeys.SKIP_SILENCE] ?: true,
            autoDeleteAudio = preferences[PreferencesKeys.AUTO_DELETE_AUDIO] ?: false,
            selectedLanguage = preferences[PreferencesKeys.SELECTED_LANGUAGE] ?: "hi-IN",
            selectedSttModel = preferences[PreferencesKeys.SELECTED_STT_MODEL] ?: UserPreferences.STT_MODEL_VOSK_HINDI,
            selectedAiModel = preferences[PreferencesKeys.SELECTED_AI_MODEL] ?: UserPreferences.AI_MODEL_LOCAL_TINY,
            cloudApiKey = preferences[PreferencesKeys.CLOUD_API_KEY] ?: "",
            audioFolderPath = preferences[PreferencesKeys.AUDIO_FOLDER_PATH] ?: "",
            transcriptionFolderPath = preferences[PreferencesKeys.TRANSCRIPTION_FOLDER_PATH] ?: "",
            notesFolderPath = preferences[PreferencesKeys.NOTES_FOLDER_PATH] ?: "",
            logsFolderPath = preferences[PreferencesKeys.LOGS_FOLDER_PATH] ?: "",
            baseFolderPath = preferences[PreferencesKeys.BASE_FOLDER_PATH] ?: "",
            lastExportSourcePath = preferences[PreferencesKeys.LAST_EXPORT_SOURCE_PATH] ?: "",
            activeTheme = preferences[PreferencesKeys.ACTIVE_THEME] ?: "MIDNIGHT_BLUE",
            appLanguage = preferences[PreferencesKeys.APP_LANGUAGE] ?: "en",
            isWebSearchEnabled = preferences[PreferencesKeys.IS_WEB_SEARCH_ENABLED] ?: true,
            customSystemPrompt = preferences[PreferencesKeys.CUSTOM_SYSTEM_PROMPT] ?: "",
            aiLanguage = preferences[PreferencesKeys.AI_LANGUAGE] ?: "auto",
            fontScale = preferences[PreferencesKeys.FONT_SCALE] ?: 1.0f,
            fontMode = preferences[PreferencesKeys.FONT_MODE] ?: "DEFAULT",
            customThemeJson = preferences[PreferencesKeys.CUSTOM_THEME_JSON] ?: "",
            customAiModels = preferences[PreferencesKeys.CUSTOM_AI_MODELS] ?: "",
            ttsSpeed = preferences[PreferencesKeys.TTS_SPEED] ?: 1.0f,
            pinnedCategories = preferences[PreferencesKeys.PINNED_CATEGORIES] ?: setOf("Urgent", "Shopping"),
            customCategories = preferences[PreferencesKeys.CUSTOM_CATEGORIES] ?: emptySet(),
            downloadedModels = preferences[PreferencesKeys.DOWNLOADED_MODELS] ?: emptySet(),
            logFileExtension = preferences[PreferencesKeys.LOG_FILE_EXTENSION] ?: "log",
            huggingFaceToken = preferences[PreferencesKeys.HUGGING_FACE_TOKEN] ?: "",
            recorderLayoutMode = preferences[PreferencesKeys.RECORDER_LAYOUT_MODE] ?: "BOTH_TABS",
            defaultAbortSelection = preferences[PreferencesKeys.DEFAULT_ABORT_SELECTION] ?: "ALL_FILES",
            showLogsTab = preferences[PreferencesKeys.SHOW_LOGS_TAB] ?: false,
            showAiTab = preferences[PreferencesKeys.SHOW_AI_TAB] ?: true,
            homeTab = preferences[PreferencesKeys.HOME_TAB] ?: "DUMB_ASSISTANT",
            showDashboardTab = preferences[PreferencesKeys.SHOW_DASHBOARD_TAB] ?: true,
            showRecorderTab = preferences[PreferencesKeys.SHOW_RECORDER_TAB] ?: true,
            showMediaTab = preferences[PreferencesKeys.SHOW_MEDIA_TAB] ?: true,
            showAssistantTab = preferences[PreferencesKeys.SHOW_ASSISTANT_TAB] ?: true,
            tabOrder = preferences[PreferencesKeys.TAB_ORDER] ?: "DUMB_ASSISTANT,DASHBOARD,RECORDER,AI,MEDIA,LOGS,SETTINGS"
        )
    }

    suspend fun updateBufferLength(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BUFFER_LENGTH_SECONDS] = seconds
        }
    }

    suspend fun updateOverlap(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.OVERLAP_SECONDS] = seconds
        }
    }

    suspend fun updateSampleRate(sampleRate: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SAMPLE_RATE] = sampleRate
        }
    }

    suspend fun updateLiveTranscribe(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LIVE_TRANSCRIBE] = enabled
        }
    }

    suspend fun updateSkipSilence(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SKIP_SILENCE] = enabled
        }
    }

    suspend fun updateAutoDeleteAudio(autoDelete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_AUDIO] = autoDelete
        }
    }

    suspend fun updateLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_LANGUAGE] = lang
        }
    }

    suspend fun updateSttModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_STT_MODEL] = modelId
        }
    }

    suspend fun updateAiModel(modelId: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_AI_MODEL] = modelId
        }
    }

    suspend fun updateCloudApiKey(apiKey: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CLOUD_API_KEY] = apiKey.trim()
        }
    }

    suspend fun updateAudioFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUDIO_FOLDER_PATH] = path.trim()
        }
    }

    suspend fun updateTranscriptionFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TRANSCRIPTION_FOLDER_PATH] = path.trim()
        }
    }

    suspend fun updateNotesFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTES_FOLDER_PATH] = path.trim()
        }
    }

    suspend fun updateLogsFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOGS_FOLDER_PATH] = path.trim()
        }
    }

    suspend fun updateBaseFolderPath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.BASE_FOLDER_PATH] = path.trim()
        }
    }

    suspend fun updateLastExportSourcePath(path: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_EXPORT_SOURCE_PATH] = path.trim()
        }
    }

    suspend fun updateActiveTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_THEME] = theme
        }
    }

    suspend fun updatePinnedCategories(pinned: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PINNED_CATEGORIES] = pinned
        }
    }

    suspend fun updateCustomCategories(custom: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_CATEGORIES] = custom
        }
    }

    suspend fun setModelDownloaded(modelId: String, isDownloaded: Boolean) {
        context.dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.DOWNLOADED_MODELS] ?: emptySet()
            preferences[PreferencesKeys.DOWNLOADED_MODELS] = if (isDownloaded) current + modelId else current - modelId
        }
    }

    suspend fun updateSettingsMode(mode: SettingsMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SETTINGS_MODE] = mode.name
        }
    }

    suspend fun updateAppLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_LANGUAGE] = lang
        }
    }

    suspend fun updateCustomSystemPrompt(prompt: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_SYSTEM_PROMPT] = prompt
        }
    }

    suspend fun updateWebSearchEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_WEB_SEARCH_ENABLED] = enabled
        }
    }

    suspend fun updateAiLanguage(lang: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AI_LANGUAGE] = lang
        }
    }

    suspend fun updateFontScale(scale: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_SCALE] = scale
        }
    }

    suspend fun updateFontMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.FONT_MODE] = mode
        }
    }

    suspend fun updateCustomThemeJson(json: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_THEME_JSON] = json
        }
    }

    suspend fun updateCustomAiModels(json: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_AI_MODELS] = json
        }
    }

    suspend fun updateTtsSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TTS_SPEED] = speed
        }
    }

    suspend fun updateLogFileExtension(ext: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LOG_FILE_EXTENSION] = ext
        }
    }

    suspend fun updateHuggingFaceToken(token: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HUGGING_FACE_TOKEN] = token
        }
    }

    suspend fun updateRecorderLayoutMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RECORDER_LAYOUT_MODE] = mode
        }
    }

    suspend fun updateDefaultAbortSelection(selection: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEFAULT_ABORT_SELECTION] = selection
        }
    }

    suspend fun updateShowLogsTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_LOGS_TAB] = show
        }
    }

    suspend fun updateShowAiTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_AI_TAB] = show
        }
    }

    suspend fun updateHomeTab(homeTab: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HOME_TAB] = homeTab
        }
    }

    suspend fun updateShowDashboardTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_DASHBOARD_TAB] = show
        }
    }

    suspend fun updateShowRecorderTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_RECORDER_TAB] = show
        }
    }

    suspend fun updateShowMediaTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MEDIA_TAB] = show
        }
    }

    suspend fun updateShowAssistantTab(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_ASSISTANT_TAB] = show
        }
    }

    suspend fun updateTabOrder(tabOrder: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TAB_ORDER] = tabOrder
        }
    }

    suspend fun resetAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
