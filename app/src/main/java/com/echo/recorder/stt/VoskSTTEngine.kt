package com.echo.recorder.stt

import android.content.Context
import com.echo.recorder.util.AppLogger
import com.echo.recorder.util.AppStorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.io.FileInputStream

/**
 * VoskSTTEngine
 *
 * Genuine 100% offline audio-file transcription using the Vosk library (Alpha Cephei).
 * Supports Hindi (hi) and Hinglish without any Google/Cloud API key.
 *
 * Model required: vosk-model-small-hi-0.22  (~45 MB zip)
 * Download URL: https://alphacephei.com/vosk/models/vosk-model-small-hi-0.22.zip
 * Expected extracted dir: <modelsDir>/vosk_hindi/
 */
class VoskSTTEngine(
    private val context: Context
) : SpeechToTextEngine {

    companion object {
        /** Sub-directory inside the models folder that holds the extracted Vosk Hindi model */
        const val MODEL_DIR_NAME = "vosk_hindi"

        private const val TAG = "VOSK_STT"

        /** Minimum WAV data bytes to attempt recognition (skip near-silent headers) */
        private const val MIN_AUDIO_BYTES = 44 + 1024

        /** Buffer size for streaming audio bytes into Vosk */
        private const val BUFFER_SIZE = 4096
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    override suspend fun transcribeAudioFile(audioFile: File): Result<String> =
        withContext(Dispatchers.IO) {
            if (!audioFile.exists() || audioFile.length() < MIN_AUDIO_BYTES) {
                AppLogger.w(TAG, "Audio file missing or too small: ${audioFile.absolutePath}")
                return@withContext Result.failure(IllegalArgumentException("Audio file empty or missing"))
            }

            val modelDir = getModelDir(context)
            if (modelDir == null || !modelDir.exists() || modelDir.listFiles().isNullOrEmpty()) {
                AppLogger.w(TAG, "Vosk Hindi model not found at expected path. Download it first.")
                return@withContext Result.failure(
                    IllegalStateException(
                        "Vosk Hindi model not installed. Tap 'Download Model' in Settings → Transcription Models."
                    )
                )
            }

            AppLogger.i(TAG, "Loading Vosk model from: ${modelDir.absolutePath}")
            var model: Model? = null
            var recognizer: Recognizer? = null

            try {
                model = Model(modelDir.absolutePath)
                AppLogger.i(TAG, "Vosk model loaded OK")

                // Read WAV parameters from header (bytes 22-27 contain channels + sample rate)
                val (inputSampleRate, channels) = parseWavHeader(audioFile)
                AppLogger.i(TAG, "WAV: ${inputSampleRate} Hz, $channels ch, ${audioFile.length()} bytes")

                // Vosk works best at 16kHz. Open recognizer with the file's actual sample rate
                // so it can handle 8 kHz files too — Vosk resamples internally when rate differs.
                recognizer = Recognizer(model, inputSampleRate.toFloat())
                recognizer.setMaxAlternatives(0)
                recognizer.setWords(false)

                val transcript = streamAudioToVosk(audioFile, recognizer)

                if (transcript.isBlank()) {
                    AppLogger.i(TAG, "Vosk returned no speech for ${audioFile.name}")
                    Result.success("[No speech detected in ${audioFile.name}]")
                } else {
                    AppLogger.i(TAG, "Vosk transcript for ${audioFile.name}: \"$transcript\"")
                    Result.success(transcript)
                }
            } catch (e: Exception) {
                AppLogger.e(TAG, "Vosk transcription error for ${audioFile.name}", e)
                Result.failure(RuntimeException("Vosk transcription failed: ${e.message}", e))
            } finally {
                try { recognizer?.close() } catch (_: Exception) {}
                try { model?.close() } catch (_: Exception) {}
            }
        }

    // ──────────────────────────────────────────────────────────────────────────
    // Internal helpers
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Streams raw PCM bytes from the WAV file (skipping the 44-byte header) into the Vosk
     * recognizer and collects partial + final results into one transcript string.
     */
    private fun streamAudioToVosk(audioFile: File, recognizer: Recognizer): String {
        val resultParts = mutableListOf<String>()
        val buffer = ByteArray(BUFFER_SIZE)

        FileInputStream(audioFile).use { fis ->
            // Skip WAV header (44 bytes for standard PCM WAV)
            fis.skip(44)

            var bytesRead: Int
            while (fis.read(buffer).also { bytesRead = it } != -1) {
                val chunk = if (bytesRead == buffer.size) buffer else buffer.copyOf(bytesRead)
                if (recognizer.acceptWaveForm(chunk, bytesRead)) {
                    val partial = parseVoskResult(recognizer.result)
                    if (partial.isNotBlank()) resultParts.add(partial)
                }
            }
        }

        // Flush final partial result
        val finalResult = parseVoskResult(recognizer.finalResult)
        if (finalResult.isNotBlank()) resultParts.add(finalResult)

        return resultParts.joinToString(" ").trim()
    }

    /**
     * Extracts the "text" field from Vosk's JSON response.
     * Vosk returns: {"text": "some words"} or {"partial": "some words"}
     */
    private fun parseVoskResult(json: String?): String {
        if (json.isNullOrBlank()) return ""
        // Simple extraction without a JSON parser dependency
        val textMatch = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(json)
        return textMatch?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    /** Returns the extracted Vosk Hindi model directory, or null if not present */
    private fun getModelDir(context: Context): File? {
        val modelsDir = AppStorageManager.getModelsDirectory(context)
        val modelDir = File(modelsDir, MODEL_DIR_NAME)
        return if (modelDir.exists() && modelDir.isDirectory) modelDir else null
    }

    /** Parses sample rate and channel count from a standard 44-byte PCM WAV header */
    private fun parseWavHeader(file: File): Pair<Int, Int> {
        return try {
            FileInputStream(file).use { fis ->
                val header = ByteArray(44)
                fis.read(header)
                val channels = (header[22].toInt() and 0xFF) or ((header[23].toInt() and 0xFF) shl 8)
                val sampleRate = (header[24].toInt() and 0xFF) or
                        ((header[25].toInt() and 0xFF) shl 8) or
                        ((header[26].toInt() and 0xFF) shl 16) or
                        ((header[27].toInt() and 0xFF) shl 24)
                Pair(
                    if (sampleRate > 0) sampleRate else 8000,
                    if (channels > 0) channels else 1
                )
            }
        } catch (e: Exception) {
            Pair(8000, 1)
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Static utility for MainViewModel to check if model is ready
    // ──────────────────────────────────────────────────────────────────────────
    object ModelStatus {
        fun isInstalled(context: Context): Boolean {
            val modelsDir = AppStorageManager.getModelsDirectory(context)
            val modelDir = File(modelsDir, MODEL_DIR_NAME)
            return modelDir.exists() && modelDir.isDirectory && !modelDir.listFiles().isNullOrEmpty()
        }

        fun getModelDir(context: Context): File =
            File(AppStorageManager.getModelsDirectory(context), MODEL_DIR_NAME)
    }
}
