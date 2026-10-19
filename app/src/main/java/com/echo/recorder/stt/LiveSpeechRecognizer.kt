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

class LiveSpeechRecognizer(
    private val context: Context,
    private val onPartialResult: ((String) -> Unit)? = null,
    private val onFinalResult: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var languageCode: String = "hi-IN" // Default Hindi / Hinglish

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _isRecognizing = MutableStateFlow(false)
    val isRecognizing: StateFlow<Boolean> = _isRecognizing.asStateFlow()

    fun setLanguage(lang: String) {
        val normalized = resolveLanguageTag(lang)
        if (languageCode != normalized) {
            languageCode = normalized
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
            clean.equals("hi", ignoreCase = true) || clean.equals("hin", ignoreCase = true) -> "hi-IN"
            clean.equals("en", ignoreCase = true) || clean.equals("eng", ignoreCase = true) -> "en-IN"
            clean.equals("auto", ignoreCase = true) -> "hi-IN"
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
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, resolvedLang)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, resolvedLang)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, resolvedLang)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }

    private fun setupAndStartRecognizer() {
        if (!isListening) return

        try {
            if (speechRecognizer == null) {
                val recognizer = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S &&
                    SpeechRecognizer.isOnDeviceRecognitionAvailable(context)) {
                    try {
                        com.echo.recorder.util.AppLogger.i("STT", "Initializing On-Device SpeechRecognizer...")
                        SpeechRecognizer.createOnDeviceSpeechRecognizer(context)
                    } catch (e: Exception) {
                        SpeechRecognizer.createSpeechRecognizer(context)
                    }
                } else {
                    SpeechRecognizer.createSpeechRecognizer(context)
                }

                recognizer.setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecognizing.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        _isRecognizing.value = false
                        val errorName = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO (3: Audio recording error)"
                            SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT (5: Client side error)"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS (9)"
                            SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK (2)"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT (1)"
                            SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH (7: No speech detected in slice)"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY (8)"
                            SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER (4)"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT (6)"
                            else -> "ERROR_CODE_$error"
                        }
                        com.echo.recorder.util.AppLogger.d("STT", "LiveSpeechRecognizer status: $errorName")

                        if (isListening) {
                            val isFatal = error == SpeechRecognizer.ERROR_CLIENT ||
                                    error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                                    error == SpeechRecognizer.ERROR_AUDIO

                            if (isFatal) {
                                try {
                                    speechRecognizer?.destroy()
                                } catch (_: Exception) {}
                                speechRecognizer = null
                            }

                            val delayMs = when (error) {
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 600L
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 1500L
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 150L
                                SpeechRecognizer.ERROR_AUDIO -> 500L
                                else -> 300L
                            }

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
                            }, delayMs)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
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
                            }, 150)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
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
            e.printStackTrace()
            restartListening()
        }
    }
}
