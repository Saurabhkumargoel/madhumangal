package com.echo.recorder.stt

import android.content.Context
import com.echo.recorder.ai.GeminiApiService
import com.echo.recorder.data.PreferencesRepository
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.util.AppLogger
import com.echo.recorder.util.AppStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.util.Locale
import kotlin.math.sqrt

class HindiHinglishSTTEngine(
    private val context: Context,
    private val prefsRepo: PreferencesRepository? = null
) : SpeechToTextEngine {

    override suspend fun transcribeAudioFile(audioFile: File): Result<String> {
        return transcribeAudioFile(audioFile, null)
    }

    override suspend fun transcribeAudioFile(audioFile: File, modelOverride: String?): Result<String> = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() <= 44) {
            AppLogger.w("STT_ENGINE", "Audio file is empty or missing: ${audioFile.absolutePath}")
            return@withContext Result.failure(IllegalArgumentException("Audio file is empty or missing"))
        }

        val prefs = prefsRepo?.userPreferencesFlow?.first() ?: UserPreferences()
        val model = modelOverride ?: prefs.selectedSttModel
        val rms = calculateWavRms(audioFile)
        AppLogger.i("STT_ENGINE", "Transcribing file: ${audioFile.name}, size: ${audioFile.length()} bytes, RMS: ${String.format(Locale.US, "%.1f", rms)}, selected model: $model")

        // If audio file is purely background ambient noise
        if (rms < 90.0) {
            AppLogger.i("STT_ENGINE", "File ${audioFile.name} classified as ambient background noise (RMS < 90)")
            return@withContext Result.success("[Ambient background segment - Low noise]")
        }

        // ── 1. VOSK: Real 100% offline neural transcription of audio files ──────
        if (model == UserPreferences.STT_MODEL_VOSK_HINDI || model == UserPreferences.STT_MODEL_VOSK_EN_IN) {
            val dirName = if (model == UserPreferences.STT_MODEL_VOSK_EN_IN) VoskSTTEngine.MODEL_DIR_EN_IN else VoskSTTEngine.MODEL_DIR_NAME
            AppLogger.i("STT_ENGINE", "Routing to VoskSTTEngine for offline file transcription (dir=$dirName)")
            val voskResult = VoskSTTEngine(context, dirName).transcribeAudioFile(audioFile)
            return@withContext if (voskResult.isSuccess) {
                voskResult
            } else {
                val errMsg = voskResult.exceptionOrNull()?.message ?: "Unknown Vosk error"
                AppLogger.w("STT_ENGINE", "VoskSTTEngine failed: $errMsg")
                Result.failure(RuntimeException(errMsg))
            }
        }

        // ── 2. Cloud Gemini: transcribe if API Key is available ─────────────────
        if (model == UserPreferences.AI_MODEL_CLOUD_GEMINI || model == "cloud_gemini" || prefs.cloudApiKey.isNotBlank()) {
            if (prefs.cloudApiKey.isNotBlank()) {
                AppLogger.i("STT_ENGINE", "Cloud API Key found. Transcribing with Gemini Audio API for verbatim accuracy...")
                val geminiResult = GeminiApiService.transcribeAudioWithGemini(audioFile, prefs.cloudApiKey)
                if (geminiResult.isSuccess) {
                    val text = geminiResult.getOrNull()
                    if (!text.isNullOrBlank()) {
                        AppLogger.i("STT_ENGINE", "Gemini transcription successful: \"$text\"")
                        return@withContext Result.success(text)
                    }
                } else {
                    AppLogger.w("STT_ENGINE", "Gemini API transcription failed: ${geminiResult.exceptionOrNull()?.message}")
                }
            } else if (model == "cloud_gemini") {
                AppLogger.w("STT_ENGINE", "Cloud Gemini chosen but no API key configured.")
            }
        }

        // ── 3. Offline fallback: If Vosk Hindi or Vosk En is installed, decode via Vosk! ──
        val voskHindiInstalled = VoskSTTEngine.ModelStatus.isInstalled(context, VoskSTTEngine.MODEL_DIR_NAME)
        val voskEnInstalled = VoskSTTEngine.ModelStatus.isInstalled(context, VoskSTTEngine.MODEL_DIR_EN_IN)

        if (voskHindiInstalled || voskEnInstalled) {
            val fallbackDir = if (model == UserPreferences.STT_MODEL_VOSK_EN_IN || (!voskHindiInstalled && voskEnInstalled))
                VoskSTTEngine.MODEL_DIR_EN_IN else VoskSTTEngine.MODEL_DIR_NAME
            AppLogger.i("STT_ENGINE", "Model '$model' requested for audio file. Decoding with on-device neural Vosk engine ($fallbackDir)...")
            val fallbackResult = VoskSTTEngine(context, fallbackDir).transcribeAudioFile(audioFile)
            if (fallbackResult.isSuccess) {
                return@withContext fallbackResult
            }
        }

        val audioInfo = parseWavHeader(audioFile)
        val sampleRate = audioInfo.first
        val channels = audioInfo.second
        val dataBytes = (audioFile.length() - 44).coerceAtLeast(0)
        val bytesPerSample = 2 * channels
        val totalSamples = if (bytesPerSample > 0) dataBytes / bytesPerSample else 0
        val durationSec = if (sampleRate > 0) totalSamples.toDouble() / sampleRate else 5.0

        val modelName = when (model) {
            UserPreferences.STT_MODEL_WHISPER_TINY -> "Whisper Tiny"
            UserPreferences.STT_MODEL_WHISPER_BASE -> "Whisper Base"
            UserPreferences.STT_MODEL_INDIC_CONFORMER -> "IndicConformer"
            UserPreferences.STT_MODEL_GOOGLE_SPEECH -> "Google Speech Recognizer"
            UserPreferences.AI_MODEL_CLOUD_GEMINI, "cloud_gemini" -> "Gemini 1.5 Flash"
            else -> model
        }

        AppLogger.i("STT_ENGINE", "Audio evaluation: duration=${String.format(Locale.US, "%.1f", durationSec)}s, RMS=${rms.toInt()}, model=$modelName")

        val statusMsg = "[Offline model not ready for: $modelName. Go to Settings → Transcription Models and tap 'Download Model' for Vosk Hindi to enable 100% offline file transcription.]"
        Result.success(statusMsg)
    }

    private fun parseWavHeader(file: File): Pair<Int, Int> {
        return try {
            FileInputStream(file).use { input ->
                val header = ByteArray(44)
                input.read(header)
                val channels = (header[22].toInt() and 0xFF) or ((header[23].toInt() and 0xFF) shl 8)
                val sampleRate = (header[24].toInt() and 0xFF) or
                        ((header[25].toInt() and 0xFF) shl 8) or
                        ((header[26].toInt() and 0xFF) shl 16) or
                        ((header[27].toInt() and 0xFF) shl 24)
                Pair(if (sampleRate > 0) sampleRate else 8000, if (channels > 0) channels else 1)
            }
        } catch (e: Exception) {
            Pair(8000, 1)
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
