package com.madhumangal.stt

import java.io.File

interface SpeechToTextEngine {
    suspend fun transcribeAudioFile(audioFile: File): Result<String>
    suspend fun transcribeAudioFile(audioFile: File, modelOverride: String?): Result<String> = transcribeAudioFile(audioFile)
}
