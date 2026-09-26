package com.echo.recorder.stt

import java.io.File

interface SpeechToTextEngine {
    suspend fun transcribeAudioFile(audioFile: File): Result<String>
}
