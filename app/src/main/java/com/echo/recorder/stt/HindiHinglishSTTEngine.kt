package com.echo.recorder.stt

import android.content.Context
import com.echo.recorder.data.PreferencesRepository
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.sqrt

class HindiHinglishSTTEngine(
    private val context: Context,
    private val prefsRepo: PreferencesRepository? = null
) : SpeechToTextEngine {

    override suspend fun transcribeAudioFile(audioFile: File): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() <= 44) {
            AppLogger.w("STT_ENGINE", "Audio file is empty or missing: ${audioFile.absolutePath}")
            return@withContext Result.failure(IllegalArgumentException("Audio file is empty or missing"))
        }

        val prefs = prefsRepo?.userPreferencesFlow?.first() ?: UserPreferences()
        val model = prefs.selectedSttModel
        val rms = calculateWavRms(audioFile)
        AppLogger.i("STT_ENGINE", "Transcribing file: ${audioFile.name}, size: ${audioFile.length()} bytes, RMS: ${String.format(java.util.Locale.US, "%.1f", rms)}, selected model: $model")

        // If audio file is purely background ambient noise
        if (rms < 90.0) {
            AppLogger.i("STT_ENGINE", "File ${audioFile.name} classified as ambient background noise (RMS < 90)")
            return@withContext Result.success("[Ambient background segment - Low noise]")
        }

        // Google Speech Recognizer is built into Android system for real-time live mic streaming
        if (model == UserPreferences.STT_MODEL_GOOGLE_SPEECH) {
            AppLogger.w("STT_ENGINE", "Google Speech Recognizer requires live microphone input. For transcribing recorded audio files, use Gemini 1.5 Flash.")
            return@withContext Result.failure(IllegalStateException("To transcribe recorded audio chunks, enter a free Gemini API key in Settings or use Live Recording mode."))
        }

        val isModelDownloaded = prefs.downloadedModels.contains(model)
        val modelFile = File(com.echo.recorder.util.AppStorageManager.getModelsDirectory(context), "${model}.bin")

        val modelName = when (model) {
            UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper Tiny"
            UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper Base"
            UserPreferences.STT_MODEL_INDIC_CONFORMER -> "IndicConformer"
            else -> "Offline Model"
        }

        if (isModelDownloaded && modelFile.exists()) {
            AppLogger.i("STT_ENGINE", "Using downloaded offline model: ${modelFile.name} (${modelFile.length()} bytes)")
            Result.failure(IllegalStateException("On-device offline transformer engine requires Gemini 1.5 Flash for verbatim speech transcription of saved files."))
        } else {
            AppLogger.w("STT_ENGINE", "Offline STT model '$modelName' is not downloaded on device")
            Result.failure(IllegalStateException("MODEL_NOT_DOWNLOADED:$model:$modelName"))
        }
    }

    private fun calculateWavRms(file: File): Double {
        return try {
            FileInputStream(file).use { input ->
                // Skip 44-byte WAV header
                val header = ByteArray(44)
                input.read(header)

                val buffer = ByteArray(2048)
                var bytesRead: Int
                var sumSquares = 0.0
                var sampleCount = 0L

                while (input.read(buffer).also { bytesRead = it } != -1) {
                    for (i in 0 until bytesRead - 1 step 2) {
                        val sample = (buffer[i].toInt() and 0xFF) or (buffer[i + 1].toInt() shl 8)
                        val shortVal = sample.toShort().toDouble()
                        sumSquares += shortVal * shortVal
                        sampleCount++
                    }
                }

                if (sampleCount > 0) sqrt(sumSquares / sampleCount) else 0.0
            }
        } catch (e: Exception) {
            e.printStackTrace()
            0.0
        }
    }
}
