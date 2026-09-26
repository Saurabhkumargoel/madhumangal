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
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.echo.recorder.EchoApplication
import com.echo.recorder.MainActivity
import com.echo.recorder.R
import com.echo.recorder.data.AudioSegment
import com.echo.recorder.data.UserPreferences
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

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null
    private var timerJob: Job? = null

    private lateinit var bufferManager: AudioBufferManager
    private var hardwareSampleRate = 16000
    private var targetSampleRate = UserPreferences.QUALITY_TINIEST // 8000 Hz default

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
            bufferLengthSeconds = 15,
            overlapSeconds = 3,
            sampleRate = UserPreferences.QUALITY_TINIEST,
            skipSilence = true,
            onSegmentCreated = { file, startTime, endTime, duration, overlap ->
                onAudioSegmentSaved(file, startTime, endTime, duration, overlap)
            }
        )

        // Sync preferences
        serviceScope.launch {
            prefsRepo.userPreferencesFlow.collectLatest { prefs ->
                bufferManager.bufferLengthSeconds = prefs.bufferLengthSeconds
                bufferManager.overlapSeconds = prefs.overlapSeconds
                bufferManager.sampleRate = prefs.sampleRate
                bufferManager.skipSilence = prefs.skipSilence
                targetSampleRate = prefs.sampleRate
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
        bufferManager.sampleRate = targetSampleRate
        AudioRecordingState.actualSampleRate.value = targetSampleRate

        startForeground(NOTIFICATION_ID, createNotification())
        isRecording = true

        // Update centralized state immediately
        AudioRecordingState.isRunning.value = true
        AudioRecordingState.statusMessage.value = "RECORDING LIVE"
        AudioRecordingState.elapsedSeconds.value = 0L
        AudioRecordingState.liveTranscript.value = "Listening to your voice..."

        // Start ticking timer
        timerJob = serviceScope.launch {
            while (isActive && isRecording) {
                delay(1000)
                AudioRecordingState.elapsedSeconds.value = AudioRecordingState.elapsedSeconds.value + 1
            }
        }

        audioRecord?.startRecording()

        recordingThread = Thread {
            val audioBuffer = ByteArray(2048)
            val shouldDownsample = (hardwareSampleRate == 16000 && targetSampleRate == 8000)

            while (isRecording && !Thread.currentThread().isInterrupted) {
                val read = audioRecord?.read(audioBuffer, 0, audioBuffer.size) ?: 0
                if (read > 0) {
                    val pcmToSend = if (shouldDownsample) {
                        downsample16kTo8k(audioBuffer, read)
                    } else {
                        audioBuffer.copyOf(read)
                    }

                    bufferManager.appendPcmData(pcmToSend, pcmToSend.size)

                    // Calculate amplitude & decibels
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
     * Tries preferred sample rate, falls back to 16000, 44100, 48000 if device HAL doesn't support 8000.
     */
    @SuppressLint("MissingPermission")
    private fun createAudioRecord(preferredRate: Int): Pair<AudioRecord, Int>? {
        val rates = listOf(preferredRate, 16000, 44100, 48000, 8000).distinct()
        for (rate in rates) {
            val minBuf = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (minBuf > 0) {
                try {
                    val record = AudioRecord(
                        MediaRecorder.AudioSource.MIC,
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
                    // Try next candidate rate
                }
            }
        }
        return null
    }

    /**
     * Downsamples 16kHz 16-bit mono PCM to 8kHz by taking alternate 2-byte samples.
     */
    private fun downsample16kTo8k(src: ByteArray, length: Int): ByteArray {
        val outLength = (length / 4) * 2
        val out = ByteArray(outLength)
        var outIdx = 0
        for (i in 0 until length - 3 step 4) {
            out[outIdx] = src[i]
            out[outIdx + 1] = src[i + 1]
            outIdx += 2
        }
        return out
    }

    fun stopRecording() {
        if (!isRecording) return

        isRecording = false
        AudioRecordingState.reset()

        timerJob?.cancel()
        timerJob = null

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
            AudioRecordingState.createdSegmentsCount.value = AudioRecordingState.createdSegmentsCount.value + 1

            // Trigger offline transcription and live feed update
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
