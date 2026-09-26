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
        val AUTO_DELETE_AUDIO = booleanPreferencesKey("auto_delete_audio")
        val SELECTED_STT_ENGINE = stringPreferencesKey("selected_stt_engine")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            bufferLengthSeconds = preferences[PreferencesKeys.BUFFER_LENGTH_SECONDS] ?: 60,
            overlapSeconds = preferences[PreferencesKeys.OVERLAP_SECONDS] ?: 5,
            autoDeleteAudio = preferences[PreferencesKeys.AUTO_DELETE_AUDIO] ?: false,
            selectedSttEngine = preferences[PreferencesKeys.SELECTED_STT_ENGINE] ?: "LOCAL_OFFLINE"
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

    suspend fun updateAutoDeleteAudio(autoDelete: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_DELETE_AUDIO] = autoDelete
        }
    }

    suspend fun updateSttEngine(engine: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_STT_ENGINE] = engine
        }
    }
}
