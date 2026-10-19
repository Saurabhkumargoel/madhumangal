package com.echo.recorder.stt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.echo.recorder.audio.AudioRecordingState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class LiveSpeechRecognizer(
    private val context: Context,
    initialLanguage: String = "hi-IN",
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onFinalResult: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var languageCode: String = resolveLanguageTag(initialLanguage)
    private var hasFallenBackLanguage = false

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    fun setLanguage(lang: String) {
        val normalized = resolveLanguageTag(lang)
        if (languageCode != normalized) {
            languageCode = normalized
            hasFallenBackLanguage = false
            if (isListening) {
                com.echo.recorder.util.AppLogger.i("STT", "LiveSpeechRecognizer language updated to: $languageCode")
                mainHandler.post {
                    restartListening()
                }
            }
        }
    }

    private fun resolveLanguageTag(lang: String): String {
        val clean = lang.trim()
        return when {
            clean.equals("hi", ignoreCase = true) || clean.equals("hin", ignoreCase = true) || clean.equals("hi-IN", ignoreCase = true) -> "hi-IN"
            clean.equals("en", ignoreCase = true) || clean.equals("eng", ignoreCase = true) || clean.equals("en-IN", ignoreCase = true) -> "en-IN"
            clean.equals("hinglish", ignoreCase = true) || clean.equals("hybrid", ignoreCase = true) || clean.equals("auto", ignoreCase = true) -> "hi-IN"
            clean.isBlank() -> "hi-IN"
            else -> clean
        }
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            com.echo.recorder.util.AppLogger.w("STT", "SpeechRecognizer.isRecognitionAvailable() is FALSE on this device")
            AudioRecordingState.liveTranscript.value = "(Voice recognition service unavailable on device)"
            return
        }

        com.echo.recorder.util.AppLogger.i("STT", "Starting LiveSpeechRecognizer with language: $languageCode")
        isListening = true
        mainHandler.post {
            setupAndStartRecognizer()
        }
    }

    fun stopListening() {
        isListening = false
        mainHandler.post {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                speechRecognizer = null
                _isRecognizing.value = false
                AudioRecordingState.isSpeechDetected.value = false
            }
        }
    }

    private fun restartListening() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        speechRecognizer = null
        if (isListening) {
            setupAndStartRecognizer()
        }
    }

    private fun createRecognizerIntent(): Intent {
        val resolvedLang = resolveLanguageTag(languageCode)
        val additionalLangs = when (resolvedLang) {
            "hi-IN" -> arrayOf("en-IN", "hi-Latn", "en-US")
            "en-IN" -> arrayOf("hi-IN", "hi-Latn", "en-US")
            else -> arrayOf("hi-IN", "en-IN", "hi-Latn", "en-US")
        }
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLang)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

            // Multilingual / Hybrid Hinglish Recognition Extras
            // Standard Android RecognizerIntent.EXTRA_ADDITIONAL_LANGUAGES is "android.speech.extra.ADDITIONAL_LANGUAGES"
            putExtra("android.speech.extra.ADDITIONAL_LANGUAGES", additionalLangs)
            putExtra("android.speech.extra.ENABLE_LANGUAGE_DETECTION", true)
            putExtra("android.speech.extra.LANGUAGE_DETECTION_ALLOWED_LANGUAGES", arrayOf("hi-IN", "en-IN", "hi-Latn", "en-US"))
            putExtra("android.speech.extra.LANGUAGE_SWITCH_INITIAL_ACTIVE_DURATION_TIME_MILLIS", 15000L)
            putExtra("android.speech.extra.DICTATION_MODE", true)
            // Use cloud streaming if available, seamlessly falling back to offline voice packs when offline
            putExtra("android.speech.extra.PREFER_OFFLINE", false)
        }
    }

    private fun setupAndStartRecognizer() {
        if (!isListening) return

        try {
            if (speechRecognizer == null) {
                com.echo.recorder.util.AppLogger.i("STT", "Initializing SpeechRecognizer for live transcription ($languageCode)...")
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context)

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecognizing.value = true
                    }

                    override fun onBeginningOfSpeech() {
                        AudioRecordingState.isSpeechDetected.value = true
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        if (rmsdB > 1.5f) {
                            AudioRecordingState.isSpeechDetected.value = true
                        }
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        AudioRecordingState.isSpeechDetected.value = false
                    }

                    override fun onError(error: Int) {
                        _isRecognizing.value = false
                        AudioRecordingState.isSpeechDetected.value = false

                        val errorName = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3: Audio recording error)"
                            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (5: Client side error)"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9: Missing RECORD_AUDIO)"
                            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2: No internet connection)"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (1: Network timeout)"
                            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (7: Silence/No speech in slice)"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8: Service busy)"
                            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4: Server error)"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6: Speech timeout)"
                            10 -> "ERROR_TOO_MANY_REQUESTS (10: Rate limit exceeded)"
                            11 -> "ERROR_SERVER_DISCONNECTED (11: Server disconnected)"
                            12 -> "ERROR_LANGUAGE_NOT_SUPPORTED (12: Language not supported)"
                            13 -> "ERROR_LANGUAGE_UNAVAILABLE (13: Offline language pack not installed)"
                            14 -> "ERROR_CANNOT_CHECK_SUPPORT (14: Cannot check support)"
                            else -> "ERROR_CODE_$error"
                        }

                        // Silence / no-match is completely normal in conversational pauses: log only at debug
                        if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                            com.echo.recorder.util.AppLogger.d("STT", "LiveSpeechRecognizer status: $errorName")
                        } else {
                            com.echo.recorder.util.AppLogger.w("STT", "LiveSpeechRecognizer status: $errorName")
                        }

                        if (!isListening) return

                        when (error) {
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                                com.echo.recorder.util.AppLogger.e("STT", "Speech recognition stopped: Missing permission")
                                AudioRecordingState.statusMessage.value = "ERROR: Missing Permission"
                                stopListening()
                            }

                            12, 13 -> {
                                // Language unavailable or not supported by recognizer
                                com.echo.recorder.util.AppLogger.w("STT", "Language $languageCode unavailable for live recognizer. Handling fallback...")
                                try {
                                    speechRecognizer?.destroy()
                                } catch (_: Exception) {}
                                speechRecognizer = null

                                if (!hasFallenBackLanguage && languageCode.startsWith("hi", ignoreCase = true)) {
                                    hasFallenBackLanguage = true
                                    languageCode = "en-IN"
                                    com.echo.recorder.util.AppLogger.i("STT", "Falling back language to: $languageCode")
                                    mainHandler.postDelayed({
                                        if (isListening) setupAndStartRecognizer()
                                    }, 300L)
                                } else {
                                    // Language unavailable and fallback exhausted: delay retries so we don't spam loop
                                    com.echo.recorder.util.AppLogger.w("STT", "Offline speech pack missing for $languageCode. Retrying after delay...")
                                    mainHandler.postDelayed({
                                        if (isListening) setupAndStartRecognizer()
                                    }, 4000L)
                                }
                            }

                            SpeechRecognizer.ERROR_CLIENT, SpeechRecognizer.ERROR_AUDIO -> {
                                try {
                                    speechRecognizer?.destroy()
                                } catch (_: Exception) {}
                                speechRecognizer = null

                                val delayMs = if (error == SpeechRecognizer.ERROR_AUDIO) 500L else 300L
                                mainHandler.postDelayed({
                                    if (isListening) setupAndStartRecognizer()
                                }, delayMs)
                            }

                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT, 11 -> {
                                // Network unavailable / offline: back off to 3 seconds
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        if (speechRecognizer != null) {
                                            try {
                                                speechRecognizer?.startListening(createRecognizerIntent())
                                            } catch (e: Exception) {
                                                restartListening()
                                            }
                                        } else {
                                            setupAndStartRecognizer()
                                        }
                                    }
                                }, 3000L)
                            }

                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        try {
                                            speechRecognizer?.startListening(createRecognizerIntent())
                                        } catch (e: Exception) {
                                            restartListening()
                                        }
                                    }
                                }, 800L)
                            }

                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                // Seamlessly resume listening immediately for next utterance
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        try {
                                            speechRecognizer?.cancel()
                                            speechRecognizer?.startListening(createRecognizerIntent())
                                        } catch (e: Exception) {
                                            restartListening()
                                        }
                                    }
                                }, 150L)
                            }

                            else -> {
                                mainHandler.postDelayed({
                                    if (isListening) {
                                        if (speechRecognizer != null) {
                                            try {
                                                speechRecognizer?.startListening(createRecognizerIntent())
                                            } catch (e: Exception) {
                                                restartListening()
                                            }
                                        } else {
                                            setupAndStartRecognizer()
                                        }
                                    }
                                }, 500L)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            com.echo.recorder.util.AppLogger.i("STT", "LiveSpeechRecognizer captured final text: \"$text\"")
                            _liveTranscript.value = text
                            AudioRecordingState.liveTranscript.value = text
                            onFinalResult(text)
                        }
                        if (isListening) {
                            mainHandler.postDelayed({
                                if (isListening) {
                                    try {
                                        speechRecognizer?.startListening(createRecognizerIntent())
                                    } catch (e: Exception) {
                                        restartListening()
                                    }
                                }
                            }, 100L)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            com.echo.recorder.util.AppLogger.d("STT", "LiveSpeechRecognizer partial: \"$partial\"")
                            _liveTranscript.value = partial
                            AudioRecordingState.liveTranscript.value = partial
                            onPartialResult?.invoke(partial)
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })

                speechRecognizer = recognizer
            }

            speechRecognizer?.startListening(createRecognizerIntent())
        } catch (e: Exception) {
            com.echo.recorder.util.AppLogger.e("STT", "Exception in setupAndStartRecognizer: ${e.message}", e)
            restartListening()
        }
    }
}
