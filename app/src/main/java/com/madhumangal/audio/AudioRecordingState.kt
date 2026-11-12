package com.madhumangal.audio

import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Global single source of truth for recording service state.
 * Guarantees 100% synchronous reactivity in Compose UI without service binding race conditions.
 */
object AudioRecordingState {
    val isRunning = MutableStateFlow(false)
    val currentAmplitude = MutableStateFlow(0f)
    val currentDb = MutableStateFlow(-60)
    val elapsedSeconds = MutableStateFlow(0L)
    val liveTranscript = MutableStateFlow("")
    val createdSegmentsCount = MutableStateFlow(0)
    val isSpeechDetected = MutableStateFlow(false)
    val statusMessage = MutableStateFlow("STANDBY")
    val actualSampleRate = MutableStateFlow(8000)
    val currentSessionId = MutableStateFlow("")

    fun reset() {
        isRunning.value = false
        currentAmplitude.value = 0f
        currentDb.value = -60
        elapsedSeconds.value = 0L
        isSpeechDetected.value = false
        statusMessage.value = "STANDBY"
        currentSessionId.value = ""
    }
}
