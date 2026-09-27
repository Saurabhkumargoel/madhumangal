package com.echo.recorder.audio

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Binder
import android.os.Handler
import android.os.IBinder
import android.os.Looper
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
import kotlinx.coroutines.flow.collectLatest
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
    private var targetSampleRate = UserPreferences.QUALITY_TINIEST // 8000 Hz default
    private var pcmResampler = PcmResampler(16000, 8000)

    private var liveSpeechRecognizer: LiveSpeechRecognizer? = null
    private val currentCycleSpeech = StringBuilder()
    private val speechLock = Any()

    private var sessionTimestamp: String = ""
    private var sessionStartMillis: Long = 0L
    private var subtitleSequence: Int = 0
    private var customTranscriptionDir: String? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioRecordingService = this@AudioRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        val app = applicationContext as EchoApplication
        val prefsRepo = app.preferencesRepository
        val audioDir = com.echo.recorder.util.AppStorageManager.getAudioDirectory(applicationContext)

        bufferManager = AudioBufferManager(
            outputDirectory = audioDir,
            bufferLengthSeconds = 15,
            overlapSeconds = 3,
            sampleRate = UserPreferences.QUALITY_TINIEST,
            skipSilence = true,
            onSegmentCreated = { file, startTime, endTime, duration, overlap ->
                val speechCaptured = synchronized(speechLock) {
                    val s = currentCycleSpeech.toString().trim()
                    currentCycleSpeech.clear()
                    s
                }
                onAudioSegmentSaved(file, startTime, endTime, duration, overlap, speechCaptured)
            }
        )

        // Sync preferences
        serviceScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                bufferManager.outputDirectory = com.echo.recorder.util.AppStorageManager.getAudioDirectory(applicationContext, prefs.audioFolderPath)
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
                liveSpeechRecognizer?.setLanguage(prefs.selectedLanguage)
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

        com.echo.recorder.util.AppLogger.startNewSession(applicationContext)
        com.echo.recorder.util.AppLogger.i("AUDIO", "Recording session started ($sessionTimestamp): TargetRate=${targetSampleRate}Hz, HardwareRate=${hardwareSampleRate}Hz, Resampling=${hardwareSampleRate != targetSampleRate}")

        // Update centralized state immediately
        AudioRecordingState.isRunning.value = true
        AudioRecordingState.statusMessage.value = "RECORDING LIVE"
        AudioRecordingState.elapsedSeconds.value = 0L
        AudioRecordingState.liveTranscript.value = "Listening for Hindi & Hinglish voice..."

        // Start ticking timer
        timerJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                AudioRecordingState.elapsedSeconds.value = AudioRecordingState.elapsedSeconds.value + 1
            }
        }

        // Initialize and start live speech recognizer on Main Thread
        mainHandler.post {
            liveSpeechRecognizer = LiveSpeechRecognizer(applicationContext) { text ->
                val now = System.currentTimeMillis()
                val utteranceEndMs = (now - sessionStartMillis).coerceAtLeast(1000L)
                val utteranceStartMs = (utteranceEndMs - 3500L).coerceAtLeast(0L)
                subtitleSequence++

                // Save live speech to timestamped subtitle SRT file
                com.echo.recorder.util.AppStorageManager.appendLiveSessionSrt(
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
                }
                AudioRecordingState.liveTranscript.value = text
            }.apply {
                startListening()
            }
        }

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioBuffer = ByteArray(2048)

            while (isRecording && !Thread.currentThread().isInterrupted) {
                val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (read > 0) {
                    val pcmToSend = pcmResampler.resample(audioBuffer, read)
                    bufferManager.appendPcmData(pcmToSend, pcmToSend.size)

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
        }.apply { start() }
    }

    /**
     * Tries VOICE_RECOGNITION first (for concurrent speech recognition support),
     * falling back to standard MIC across candidate sample rates.
     */
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(preferredRate: Int): Pair<AudioRecord, Int>? {
        val sources = listOf(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            MediaRecorder.AudioSource.MIC
        )
        val rates = listOf(preferredRate, 16000, 44100, 48000, 8000).distinct()

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
        if (!isRecording) return

        isRecording = false
        AudioRecordingState.reset()

        timerJob?.cancel()
        timerJob = null

        mainHandler.post {
            liveSpeechRecognizer?.stopListening()
            liveSpeechRecognizer = null
        }

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
        com.echo.recorder.util.AppLogger.i("AUDIO", "Recording stopped and buffer flushed")

        stopForeground(STOP_FOREGROUND_REMOVE)
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
            AudioRecordingState.createdSegmentsCount.value = AudioRecordingState.createdSegmentsCount.value + 1

            com.echo.recorder.util.AppLogger.i("AUDIO", "Created segment #$segmentId: ${file.name} (${durationSeconds}s, capturedSpeech=\"$speechCaptured\")")

            if (speechCaptured.isNotBlank()) {
                com.echo.recorder.util.AppStorageManager.saveChunkSrt(
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

        val stopIntent = Intent(this, AudioRecordingService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 1, stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, EchoApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("🔴 GoodEcho: Recording Live")
            .setContentText("Listening continuously (${bufferManager.bufferLengthSeconds}s chunks, ${bufferManager.sampleRate}Hz)")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "Stop Recording", stopPendingIntent)
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
