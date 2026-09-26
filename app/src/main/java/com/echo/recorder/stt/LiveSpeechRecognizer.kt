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
        languageCode = lang
    }

    fun startListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            AudioRecordingState.liveTranscript.value = "(Voice recognition service unavailable on device)"
            return
        }

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
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                speechRecognizer = null
                _isRecognizing.value = false
            }
        }
    }

    private fun setupAndStartRecognizer() {
        if (!isListening) return

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isRecognizing.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {}

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {}

                    override fun onError(error: Int) {
                        _isRecognizing.value = false
                        if (isListening) {
                            val delayMs = when (error) {
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> 800L
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> 1500L
                                SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> 250L
                                else -> 500L
                            }
                            mainHandler.postDelayed({
                                if (isListening) {
                                    setupAndStartRecognizer()
                                }
                            }, delayMs)
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotBlank()) {
                            _liveTranscript.value = text
                            AudioRecordingState.liveTranscript.value = text
                            onFinalResult(text)
                        }
                        if (isListening) {
                            mainHandler.postDelayed({
                                if (isListening) {
                                    setupAndStartRecognizer()
                                }
                            }, 200)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull() ?: ""
                        if (partial.isNotBlank()) {
                            _liveTranscript.value = partial
                            AudioRecordingState.liveTranscript.value = partial
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            if (isListening) {
                mainHandler.postDelayed({
                    if (isListening) {
                        setupAndStartRecognizer()
                    }
                }, 1000)
            }
        }
    }
}
