package com.madhumangal.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.madhumangal.MadhumangalApplication
import com.madhumangal.MainActivity
import com.madhumangal.R
import com.madhumangal.data.AudioSegment
import com.madhumangal.data.UserPreferences
import com.madhumangal.stt.LiveSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.log10

class AudioRecordingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    private val mainHandler = Handler(Looper.getMainLooper())

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var timerJob: Job? = null

    private lateinit var bufferManager: AudioBufferManager
    private var hardwareSampleRate = 16000
    private var targetSampleRate = UserPreferences.QUALITY_SPEECH // 16000 Hz standard for STT
    private var pcmResampler = PcmResampler(16000, 16000)

    private var voskLiveTranscriber: com.madhumangal.stt.VoskLiveTranscriber? = null
    private var liveSpeechRecognizer: LiveSpeechRecognizer? = null
    private val currentCycleSpeech = StringBuilder()
    private var lastPartialSpeech = ""
    private val speechLock = Any()

    private var sessionTimestamp: String = ""
    private var sessionStartMillis: Long = 0L
    private var subtitleSequence: Int = 0
    private var customTranscriptionDir: String? = null
    private var currentLanguage: String = "hi-IN"
    private var currentLogExtension: String = "txt"
    private val prefsRepo: com.madhumangal.data.PreferencesRepository by lazy {
        (applicationContext as MadhumangalApplication).preferencesRepository
    }

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        instance = this
        val app = applicationContext as MadhumangalApplication
        val prefsRepo = app.preferencesRepository
        val audioDir = com.madhumangal.util.AppStorageManager.getAudioDirectory(applicationContext)

        bufferManager = AudioBufferManager(
            outputDirectory = audioDir,
            bufferLengthSeconds = 1800,
            overlapSeconds = 10,
            sampleRate = UserPreferences.QUALITY_TINIEST,
            skipSilence = true,
            onSegmentCreated = { file, startTime, endTime, duration, overlap ->
                voskLiveTranscriber?.flushRemaining()
                val speechCaptured = synchronized(speechLock) {
                    val s = currentCycleSpeech.toString().trim()
                    currentCycleSpeech.clear()
                    if (s.isNotBlank()) {
                        lastPartialSpeech = ""
                        s
                    } else if (lastPartialSpeech.isNotBlank()) {
                        val p = lastPartialSpeech.trim()
                        lastPartialSpeech = ""
                        p
                    } else {
                        ""
                    }
                }
                onAudioSegmentSaved(file, startTime, endTime, duration, overlap, speechCaptured)
            }
        )

        // Sync preferences
        serviceScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                bufferManager.outputDirectory = com.madhumangal.util.AppStorageManager.getAudioDirectory(applicationContext, prefs.audioFolderPath)
                customTranscriptionDir = prefs.transcriptionFolderPath
                bufferManager.bufferLengthSeconds = prefs.bufferLengthSeconds
                bufferManager.overlapSeconds = prefs.overlapSeconds
                if (targetSampleRate != prefs.sampleRate) {
                    targetSampleRate = prefs.sampleRate
                    pcmResampler = PcmResampler(hardwareSampleRate, targetSampleRate)
                    bufferManager.sampleRate = targetSampleRate
                    AudioRecordingState.actualSampleRate.value = targetSampleRate
                }
                bufferManager.skipSilence = prefs.skipSilence
                currentLanguage = prefs.selectedLanguage
                currentLogExtension = prefs.logFileExtension
                liveSpeechRecognizer?.setLanguage(prefs.selectedLanguage)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
            else -> {
                if (!isRecording) {
                    try {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.cancel(NOTIFICATION_ID)
                    } catch (_: Exception) {}
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val recordPair = createAudioRecord(targetSampleRate)
        if (recordPair == null) {
            AudioRecordingState.statusMessage.value = "ERROR: Microphone Incompatible"
            return
        }

        audioRecord = recordPair.first
        hardwareSampleRate = recordPair.second
        pcmResampler = PcmResampler(hardwareSampleRate, targetSampleRate)
        bufferManager.sampleRate = targetSampleRate
        AudioRecordingState.actualSampleRate.value = targetSampleRate

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            )
        } else {
            startForeground(NOTIFICATION_ID, createNotification())
        }
        isRecording = true

        sessionTimestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
        sessionStartMillis = System.currentTimeMillis()
        subtitleSequence = 0

        com.madhumangal.util.AppLogger.startNewSession(applicationContext, extension = currentLogExtension)
        com.madhumangal.util.AppLogger.i("AUDIO", "Recording session started ($sessionTimestamp): TargetRate=${targetSampleRate}Hz, HardwareRate=${hardwareSampleRate}Hz, Resampling=${hardwareSampleRate != targetSampleRate}")

        // Update centralized state immediately
        AudioRecordingState.isRunning.value = true
        AudioRecordingState.currentSessionId.value = "session_$sessionTimestamp"
        AudioRecordingState.statusMessage.value = "RECORDING LIVE"
        AudioRecordingState.elapsedSeconds.value = 0L
        AudioRecordingState.liveTranscript.value = ""

        // Start ticking timer
        timerJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                AudioRecordingState.elapsedSeconds.value = AudioRecordingState.elapsedSeconds.value + 1
            }
        }

        // Determine STT engine preference (Google Speech hybrid vs Vosk 100% offline)
        val currentPrefs = kotlinx.coroutines.runBlocking { prefsRepo.userPreferencesFlow.first() }
        val isGoogleSpeech = currentPrefs.selectedSttModel == com.madhumangal.data.UserPreferences.STT_MODEL_GOOGLE_SPEECH

        if (isGoogleSpeech) {
            com.madhumangal.util.AppLogger.i("STT", "Starting LiveSpeechRecognizer (Google Speech Recognizer) with language: ${currentPrefs.selectedLanguage}")
            mainHandler.post {
                try {
                    liveSpeechRecognizer = com.madhumangal.stt.LiveSpeechRecognizer(
                        context = applicationContext,
                        initialLanguage = currentPrefs.selectedLanguage,
                        onPartialResult = { partial ->
                            synchronized(speechLock) {
                                lastPartialSpeech = partial
                            }
                            AudioRecordingState.liveTranscript.value = partial
                        },
                        onFinalResult = { text ->
                            val now = System.currentTimeMillis()
                            val utteranceEndMs = (now - sessionStartMillis).coerceAtLeast(1000L)
                            val utteranceStartMs = (utteranceEndMs - 3500L).coerceAtLeast(0L)
                            subtitleSequence++

                            // Save live speech to timestamped subtitle SRT file
                            com.madhumangal.util.AppStorageManager.appendLiveSessionSrt(
                                context = applicationContext,
                                sessionTimestamp = sessionTimestamp,
                                entryIndex = subtitleSequence,
                                startMs = utteranceStartMs,
                                endMs = utteranceEndMs,
                                text = text,
                                customDir = customTranscriptionDir
                            )

                            synchronized(speechLock) {
                                if (currentCycleSpeech.isNotEmpty()) currentCycleSpeech.append(" ")
                                currentCycleSpeech.append(text)
                                lastPartialSpeech = ""
                            }
                            AudioRecordingState.liveTranscript.value = text
                        }
                    ).apply {
                        startListening()
                    }
                } catch (e: Exception) {
                    com.madhumangal.util.AppLogger.e("STT", "Failed to start LiveSpeechRecognizer", e)
                }
            }
        } else {
            // Initialize Vosk live streaming transcriber (100% offline, zero mic conflict)
            val voskModelDir = when {
                com.madhumangal.stt.VoskSTTEngine.ModelStatus.isInstalled(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_NAME) ->
                    com.madhumangal.stt.VoskSTTEngine.ModelStatus.getModelDir(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_NAME)
                com.madhumangal.stt.VoskSTTEngine.ModelStatus.isInstalled(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_EN_IN) ->
                    com.madhumangal.stt.VoskSTTEngine.ModelStatus.getModelDir(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_EN_IN)
                com.madhumangal.stt.VoskSTTEngine.ModelStatus.isInstalled(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE) ->
                    com.madhumangal.stt.VoskSTTEngine.ModelStatus.getModelDir(applicationContext, com.madhumangal.stt.VoskSTTEngine.MODEL_DIR_HINDI_LARGE)
                else -> null
            }

            if (voskModelDir != null) {
                com.madhumangal.util.AppLogger.i("STT", "Starting VoskLiveTranscriber with model: ${voskModelDir.name} at 16000Hz (Zero mic conflict)")
                voskLiveTranscriber = com.madhumangal.stt.VoskLiveTranscriber(
                    modelDir = voskModelDir,
                    sampleRate = 16000f,
                    onPartial = { partial ->
                        synchronized(speechLock) {
                            lastPartialSpeech = partial
                        }
                        AudioRecordingState.liveTranscript.value = partial
                    },
                    onFinal = { text ->
                        val now = System.currentTimeMillis()
                        val utteranceEndMs = (now - sessionStartMillis).coerceAtLeast(1000L)
                        val utteranceStartMs = (utteranceEndMs - 3500L).coerceAtLeast(0L)
                        subtitleSequence++

                        // Save live speech to timestamped subtitle SRT file
                        com.madhumangal.util.AppStorageManager.appendLiveSessionSrt(
                            context = applicationContext,
                            sessionTimestamp = sessionTimestamp,
                            entryIndex = subtitleSequence,
                            startMs = utteranceStartMs,
                            endMs = utteranceEndMs,
                            text = text,
                            customDir = customTranscriptionDir
                        )

                        synchronized(speechLock) {
                            if (currentCycleSpeech.isNotEmpty()) currentCycleSpeech.append(" ")
                            currentCycleSpeech.append(text)
                            lastPartialSpeech = ""
                        }
                        AudioRecordingState.liveTranscript.value = text
                    }
                )
            } else {
                com.madhumangal.util.AppLogger.w("STT", "No Vosk offline speech model installed. Live speech transcription inactive until model is downloaded.")
                AudioRecordingState.liveTranscript.value = "(Offline speech model not installed - download in Settings or Recorder screen)"
            }
        }

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioBuffer = ByteArray(2048)
            try {
                while (isRecording && !Thread.currentThread().isInterrupted) {
                    val record = audioRecord ?: break
                    val read = try {
                        record.read(audioBuffer, 0, audioBuffer.size)
                    } catch (_: Throwable) { -1 }

                    if (read > 0 && isRecording) {
                        val pcmToSend = pcmResampler.resample(audioBuffer, read)
                        bufferManager.appendPcmData(pcmToSend, pcmToSend.size)

                        // Feed 16kHz PCM data directly to Vosk live streaming transcriber
                        voskLiveTranscriber?.feedAudio(pcmToSend, pcmToSend.size)

                        // Calculate amplitude & decibels from raw microphone input
                        var maxAmp = 0
                        for (i in 0 until read - 1 step 2) {
                            val sample = (audioBuffer[i].toInt() and 0xFF) or (audioBuffer[i + 1].toInt() shl 8)
                            val absSample = abs(sample.toShort().toInt())
                            if (absSample > maxAmp) maxAmp = absSample
                        }
                        val normalizedAmp = (maxAmp / 32768f).coerceIn(0f, 1f)
                        AudioRecordingState.currentAmplitude.value = normalizedAmp

                        val db = if (maxAmp > 0) (20 * log10(maxAmp.toDouble() / 32768.0)).toInt().coerceIn(-60, 0) else -60
                        AudioRecordingState.currentDb.value = db
                        AudioRecordingState.isSpeechDetected.value = db > -40
                    }
                }
            } catch (e: Throwable) {
                com.madhumangal.util.AppLogger.w("AUDIO", "Recording thread terminated cleanly: ${e.message}")
            }
        }.apply {
            name = "Madhumangal-AudioRecord"
            start()
        }
    }

    /**
     * Tries VOICE_RECOGNITION first (for concurrent speech recognition support),
     * falling back to standard MIC across candidate sample rates.
     */
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(preferredRate: Int): Pair<AudioRecord, Int>? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT
        )
        val rates = listOf(16000, preferredRate, 44100, 48000, 8000).distinct()

        for (source in sources) {
            for (rate in rates) {
                val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
                if (minBuf > 0) {
                    try {
                        val record = AudioRecord(
                            source,
                            rate,
                            AudioFormat.CHANNEL_IN_MONO,
                            AudioFormat.ENCODING_PCM_16BIT,
                            (minBuf * 2).coerceAtLeast(4096)
                        )
                        if (record.state == AudioRecord.STATE_INITIALIZED) {
                            return Pair(record, rate)
                        } else {
                            record.release()
                        }
                    } catch (e: Exception) {
                        // Try next candidate configuration
                    }
                }
            }
        }
        return null
    }

    fun stopRecording() {
        val wasRecording = isRecording
        isRecording = false
        AudioRecordingState.reset()

        timerJob?.cancel()
        timerJob = null

        // 1. Interrupt & join recordingThread first so it finishes any read/feedAudio
        try {
            recordingThread?.interrupt()
            recordingThread?.join(500)
        } catch (_: Exception) {}
        recordingThread = null

        // 2. Stop live speech recognizer
        mainHandler.post {
            try {
                liveSpeechRecognizer?.stopListening()
                liveSpeechRecognizer = null
            } catch (_: Exception) {}
        }

        // 3. Flush & close Vosk transcriber
        try {
            voskLiveTranscriber?.flushRemaining()
            voskLiveTranscriber?.close()
        } catch (e: Exception) {
            com.madhumangal.util.AppLogger.w("AUDIO", "Error closing Vosk transcriber: ${e.message}")
        } finally {
            voskLiveTranscriber = null
        }

        // 4. Safely stop and release AudioRecord
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            com.madhumangal.util.AppLogger.w("AUDIO", "Error releasing AudioRecord: ${e.message}")
        } finally {
            audioRecord = null
        }

        if (wasRecording) {
            try {
                bufferManager.flushSegment()
            } catch (_: Exception) {}
            com.madhumangal.util.AppLogger.i("AUDIO", "Recording stopped and buffer flushed")
        }

        // 5. Unconditionally remove notification and stop service
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (_: Exception) {}
        stopSelf()
    }

    private fun onAudioSegmentSaved(
        file: File,
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationSeconds: Int,
        overlapSeconds: Int,
        speechCaptured: String
    ) {
        serviceScope.launch {
            val app = applicationContext as MadhumangalApplication
            val segment = AudioSegment(
                filePath = file.absolutePath,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                durationSeconds = durationSeconds,
                overlapSeconds = overlapSeconds,
                status = AudioSegment.STATUS_PENDING,
                sessionId = "session_$sessionTimestamp"
            )
            val segmentId = app.database.audioSegmentDao().insertSegment(segment)
            AudioRecordingState.createdSegmentsCount.value = AudioRecordingState.createdSegmentsCount.value + 1

            com.madhumangal.util.AppLogger.i("AUDIO", "Created segment #$segmentId: ${file.name} (${durationSeconds}s, capturedSpeech=\"$speechCaptured\")")

            if (speechCaptured.isNotBlank()) {
                com.madhumangal.util.AppStorageManager.saveChunkSrt(
                    context = applicationContext,
                    audioFileName = file.name,
                    text = speechCaptured,
                    durationSeconds = durationSeconds,
                    customDir = customTranscriptionDir
                )
            }

            // Trigger transcription and pass live captured speech if available
            app.transcriptionManager.enqueueTranscription(segmentId, speechCaptured.ifBlank { null })
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = ACTION_NOTIFICATION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, MadhumangalApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🔴 Madhumangal: Recording Live")
            .setContentText("Listening continuously (${bufferManager.bufferLengthSeconds}s chunks, ${bufferManager.sampleRate}Hz)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        stopRecording()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.madhumangal.ACTION_START"
        const val ACTION_STOP = "com.madhumangal.ACTION_STOP"
        const val ACTION_NOTIFICATION_STOP = "com.madhumangal.ACTION_NOTIFICATION_STOP"

        @Volatile
        var instance: AudioRecordingService? = null
            private set

        /**
         * Thread-safe stop helper that coordinates across running service instance
         * and cancels any orphaned/remnant notifications unconditionally.
         */
        fun stop(context: Context) {
            try {
                val activeService = instance
                if (activeService != null) {
                    activeService.stopRecording()
                } else {
                    cancelRemnantNotification(context)
                    AudioRecordingState.reset()
                    val stopIntent = Intent(context, AudioRecordingService::class.java).apply {
                        action = ACTION_STOP
                    }
                    context.stopService(stopIntent)
                }
            } catch (e: Exception) {
                com.madhumangal.util.AppLogger.w("AUDIO", "Error stopping service via companion stop(): ${e.message}")
            } finally {
                cancelRemnantNotification(context)
            }
        }

        fun cancelRemnantNotification(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
            } catch (_: Exception) {}
        }
    }
}
