package com.echo.recorder.stt

import com.echo.recorder.util.AppLogger
import org.vosk.Model
import org.vosk.Recognizer
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * VoskLiveTranscriber
 *
 * Real-time, 100% offline streaming speech recognition powered by Vosk.
 * Accepts continuous raw PCM chunks from AudioRecord directly, avoiding
 * Android's hardware microphone contention and AudioPolicy muting.
 *
 * Thread-safe with robust rejection handling on stop/close.
 */
class VoskLiveTranscriber(
    private val modelDir: File,
    private val sampleRate: Float = 16000f,
    private val onPartial: (String) -> Unit,
    private val onFinal: (String) -> Unit
) {
    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var isClosed = false
    @Volatile private var isReady = false
    private val nativeLock = Any()

    init {
        try {
            executor.execute {
                try {
                    AppLogger.i("VOSK_LIVE", "Initializing Vosk Live Transcriber from ${modelDir.absolutePath} at ${sampleRate}Hz...")
                    val m = Model(modelDir.absolutePath)
                    val rec = Recognizer(m, sampleRate).apply {
                        setMaxAlternatives(0)
                        setWords(false)
                    }
                    synchronized(nativeLock) {
                        model = m
                        recognizer = rec
                        isReady = true
                    }
                    AppLogger.i("VOSK_LIVE", "Vosk Live Transcriber initialized successfully. Ready to stream.")
                } catch (e: Throwable) {
                    AppLogger.e("VOSK_LIVE", "Failed to initialize Vosk Live Transcriber: ${e.message}", e)
                }
            }
        } catch (_: RejectedExecutionException) {}
    }

    /**
     * Feeds raw PCM bytes (mono, 16-bit) to the Vosk streaming recognizer.
     * Safe to call from any audio thread; silently ignores chunks if closing.
     */
    fun feedAudio(pcmBytes: ByteArray, length: Int) {
        if (isClosed || !isReady || length <= 0 || executor.isShutdown) return
        val chunk = if (pcmBytes.size == length) pcmBytes.clone() else pcmBytes.copyOf(length)
        try {
            executor.execute {
                if (isClosed) return@execute
                try {
                    var finalUtterance = ""
                    var partialUtterance = ""

                    synchronized(nativeLock) {
                        if (isClosed) return@execute
                        val rec = recognizer ?: return@execute
                        if (rec.acceptWaveForm(chunk, chunk.size)) {
                            finalUtterance = parseText(rec.result)
                        } else {
                            partialUtterance = parsePartial(rec.partialResult)
                        }
                    }

                    if (finalUtterance.isNotBlank()) {
                        AppLogger.d("VOSK_LIVE", "Live utterance finalized: \"$finalUtterance\"")
                        onFinal(finalUtterance)
                    } else if (partialUtterance.isNotBlank()) {
                        onPartial(partialUtterance)
                    }
                } catch (e: Throwable) {
                    AppLogger.w("VOSK_LIVE", "Error processing audio chunk in Vosk: ${e.message}")
                }
            }
        } catch (_: RejectedExecutionException) {
            // Service stopping; drop chunk gracefully without crashing
        } catch (e: Throwable) {
            AppLogger.w("VOSK_LIVE", "feedAudio exception: ${e.message}")
        }
    }

    /**
     * Flushes any remaining speech in the buffer (e.g., when a segment rotates or recording stops).
     */
    fun flushRemaining(): String {
        var finalResult = ""
        if (isClosed) return ""
        try {
            synchronized(nativeLock) {
                val rec = recognizer ?: return ""
                val finalJson = rec.finalResult
                finalResult = parseText(finalJson)
            }
            if (finalResult.isNotBlank()) {
                AppLogger.i("VOSK_LIVE", "Flushed final audio buffer speech: \"$finalResult\"")
                onFinal(finalResult)
            }
        } catch (e: Throwable) {
            AppLogger.w("VOSK_LIVE", "Error flushing Vosk final result: ${e.message}")
        }
        return finalResult
    }

    /**
     * Releases native C++ Vosk model and recognizer memory safely.
     */
    fun close() {
        if (isClosed) return
        isClosed = true
        isReady = false
        try {
            executor.shutdownNow()
        } catch (_: Throwable) {}

        Thread {
            try {
                synchronized(nativeLock) {
                    recognizer?.close()
                    model?.close()
                    recognizer = null
                    model = null
                }
                AppLogger.d("VOSK_LIVE", "Vosk Live Transcriber closed and native memory freed.")
            } catch (e: Throwable) {
                AppLogger.w("VOSK_LIVE", "Error closing Vosk native handles: ${e.message}")
            }
        }.start()
    }

    companion object {
        fun parseText(json: String?): String {
            if (json.isNullOrBlank()) return ""
            val match = Regex("\"text\"\\s*:\\s*\"([^\"]*)\"").find(json)
            val raw = match?.groupValues?.getOrNull(1)?.trim() ?: ""
            return unescapeUnicode(raw)
        }

        fun parsePartial(json: String?): String {
            if (json.isNullOrBlank()) return ""
            val match = Regex("\"partial\"\\s*:\\s*\"([^\"]*)\"").find(json)
            val raw = match?.groupValues?.getOrNull(1)?.trim() ?: ""
            return unescapeUnicode(raw)
        }

        private fun unescapeUnicode(input: String): String {
            if (!input.contains("\\u")) return input
            val regex = Regex("\\\\u([0-9a-fA-F]{4})")
            return regex.replace(input) { m ->
                m.groupValues[1].toInt(16).toChar().toString()
            }
        }
    }
}
