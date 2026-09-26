package com.echo.recorder.data

data class UserPreferences(
    val bufferLengthSeconds: Int = 60,
    val overlapSeconds: Int = 5,
    val autoDeleteAudio: Boolean = false,
    val selectedSttEngine: String = "LOCAL_OFFLINE"
)
