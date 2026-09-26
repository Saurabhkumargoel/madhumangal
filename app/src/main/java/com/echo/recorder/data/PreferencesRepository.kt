package com.echo.recorder.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
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
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            bufferLengthSeconds = preferences[PreferencesKeys.BUFFER_LENGTH_SECONDS] ?: 15,
            overlapSeconds = preferences[PreferencesKeys.OVERLAP_SECONDS] ?: 3,
            sampleRate = preferences[PreferencesKeys.SAMPLE_RATE] ?: UserPreferences.QUALITY_TINIEST,
            liveTranscribe = preferences[PreferencesKeys.LIVE_TRANSCRIBE] ?: true,
            skipSilence = preferences[PreferencesKeys.SKIP_SILENCE] ?: true,
            autoDeleteAudio = preferences[PreferencesKeys.AUTO_DELETE_AUDIO] ?: false,
            selectedLanguage = preferences[PreferencesKeys.SELECTED_LANGUAGE] ?: "hi-IN",
            selectedSttModel = preferences[PreferencesKeys.SELECTED_STT_MODEL] ?: UserPreferences.STT_MODEL_GOOGLE_SPEECH,
            selectedAiModel = preferences[PreferencesKeys.SELECTED_AI_MODEL] ?: UserPreferences.AI_MODEL_LOCAL_TINY,
            cloudApiKey = preferences[PreferencesKeys.CLOUD_API_KEY] ?: ""
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
}
