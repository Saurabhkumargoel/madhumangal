package com.echo.recorder.audio

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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.echo.recorder.EchoApplication
import com.echo.recorder.MainActivity
import com.echo.recorder.R
import com.echo.recorder.data.AudioSegment
import com.echo.recorder.data.UserPreferences
import com.echo.recorder.stt.LiveSpeechRecognizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.log10

class AudioRecordingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var timerJob: Job? = null

    private lateinit var bufferManager: AudioBufferManager
    private var liveRecognizer: LiveSpeechRecognizer? = null

    private var currentSampleRate = UserPreferences.QUALITY_TINIEST // 8000 Hz default

    private val _serviceState = MutableStateFlow(StateIsRunning(false))
    val serviceState: StateFlow<StateIsRunning> = _serviceState.asStateFlow()

    private val _currentAmplitude = MutableStateFlow(0f)
    val currentAmplitude: StateFlow<Float> = _currentAmplitude.asStateFlow()

    private val _currentDb = MutableStateFlow(-60)
    val currentDb: StateFlow<Int> = _currentDb.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _createdSegmentsCount = MutableStateFlow(0)
    val createdSegmentsCount: StateFlow<Int> = _createdSegmentsCount.asStateFlow()

    data class StateIsRunning(val isRunning: Boolean)

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()

        val audioDir = File(filesDir, "audio_chunks").apply { if (!exists()) mkdirs() }
        val app = applicationContext as EchoApplication
        val prefsRepo = app.preferencesRepository

        bufferManager = AudioBufferManager(
            outputDirectory = audioDir,
            bufferLengthSeconds = 15, // Default to smaller 15s chunks
            overlapSeconds = 3,
            sampleRate = UserPreferences.QUALITY_TINIEST,
            skipSilence = true,
            onSegmentCreated = { file, startTime, endTime, duration, overlap ->
                onAudioSegmentSaved(file, startTime, endTime, duration, overlap)
            }
        )

        liveRecognizer = LiveSpeechRecognizer(this) { finalSpeech ->
            _liveTranscript.value = finalSpeech
        }

        serviceScope.launch {
            liveRecognizer?.liveTranscript?.collectLatest { text ->
                _liveTranscript.value = text
            }
        }

        // Sync preferences
        serviceScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                bufferManager.bufferLengthSeconds = prefs.bufferLengthSeconds
                bufferManager.overlapSeconds = prefs.overlapSeconds
                bufferManager.sampleRate = prefs.sampleRate
                bufferManager.skipSilence = prefs.skipSilence
                currentSampleRate = prefs.sampleRate
                liveRecognizer?.setLanguage(prefs.selectedLanguage)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_STICKY
    }

    @SuppressLint("MissingPermission")
    fun startRecording() {
        if (isRecording) return

        val sampleRate = currentSampleRate
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = (minBufferSize * 2).coerceAtLeast(2048)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            return
        }

        startForeground(NOTIFICATION_ID, createNotification())
        isRecording = true
        _serviceState.value = StateIsRunning(true)
        _elapsedSeconds.value = 0L

        // Start ticking timer
        timerJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                _elapsedSeconds.value = _elapsedSeconds.value + 1
            }
        }

        // Start live streaming speech recognizer
        liveRecognizer?.startListening()

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioBuffer = ByteArray(1024)
            while (isRecording && !Thread.currentThread().isInterrupted) {
                val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (read > 0) {
                    bufferManager.appendPcmData(audioBuffer, read)

                    // Calculate amplitude & decibels
                    var maxAmp = 0
                    for (i in 0 until read - 1 step 2) {
                        val sample = (audioBuffer[i].toInt() and 0xFF) or (audioBuffer[i + 1].toInt() shl 8)
                        val absSample = abs(sample.toShort().toInt())
                        if (absSample > maxAmp) maxAmp = absSample
                    }
                    val normalizedAmp = (maxAmp / 32768f).coerceIn(0f, 1f)
                    _currentAmplitude.value = normalizedAmp

                    // Decibels calculation: 20 * log10(amp)
                    val db = if (maxAmp > 0) (20 * log10(maxAmp.toDouble() / 32768.0)).toInt().coerceIn(-60, 0) else -60
                    _currentDb.value = db
                }
            }
        }.apply { start() }
    }

    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        _serviceState.value = StateIsRunning(false)
        _currentAmplitude.value = 0f
        _currentDb.value = -60

        timerJob?.cancel()
        timerJob = null

        liveRecognizer?.stopListening()

        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            audioRecord = null
        }

        recordingThread?.interrupt()
        recordingThread = null

        bufferManager.flushSegment()

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun onAudioSegmentSaved(
        file: File,
        startTimeMillis: Long,
        endTimeMillis: Long,
        durationSeconds: Int,
        overlapSeconds: Int
    ) {
        serviceScope.launch {
            val app = applicationContext as EchoApplication
            val segment = AudioSegment(
                filePath = file.absolutePath,
                startTimeMillis = startTimeMillis,
                endTimeMillis = endTimeMillis,
                durationSeconds = durationSeconds,
                overlapSeconds = overlapSeconds,
                status = AudioSegment.STATUS_PENDING
            )
            val segmentId = app.database.audioSegmentDao().insertSegment(segment)
            _createdSegmentsCount.value = _createdSegmentsCount.value + 1

            // Trigger offline transcription
            app.transcriptionManager.enqueueTranscription(segmentId)
        }
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, EchoApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🔴 GoodEcho: Recording Live")
            .setContentText("Continuous listening active (${bufferManager.bufferLengthSeconds}s chunks, ${bufferManager.sampleRate}Hz)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop", stopPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }

    companion object {
        const val NOTIFICATION_ID = 1001
        const val ACTION_START = "com.echo.recorder.ACTION_START"
        const val ACTION_STOP = "com.echo.recorder.ACTION_STOP"
    }
}
