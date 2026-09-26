package com.echo.recorder.data

data class UserPreferences(
    val bufferLengthSeconds: Int = 15, // Default to smaller 15s chunks
    val overlapSeconds: Int = 3,       // Default 3s overlap
    val sampleRate: Int = 8000,        // 8000 Hz = Tiniest Quality (Default from Good-Echo)
    val liveTranscribe: Boolean = true,// Live transcribe enabled by default
    val skipSilence: Boolean = true,   // Voice Activity Detection / skip silence
    val autoDeleteAudio: Boolean = false,
    val selectedLanguage: String = "hi-IN" // Hindi / Hinglish
) {
    companion object {
        const val QUALITY_TINIEST = 8000    // 8 kHz (lowest file size, speech optimized)
        const val QUALITY_SPEECH = 16000   // 16 kHz (standard STT)
        const val QUALITY_STANDARD = 22050 // 22.05 kHz
        const val QUALITY_HIGH = 44100     // 44.1 kHz (CD quality)
    }
}
