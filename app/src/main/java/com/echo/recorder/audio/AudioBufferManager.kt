package com.echo.recorder.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.abs
import kotlin.math.sqrt

class AudioBufferManager(
    private val outputDirectory: File,
    bufferLengthSeconds: Int = 15,
    overlapSeconds: Int = 3,
    var sampleRate: Int = 8000,
    var skipSilence: Boolean = true,
    private val onSegmentCreated: (file: File, startTimeMillis: Long, endTimeMillis: Long, durationSeconds: Int, overlapSeconds: Int) -> Unit
) {

    val bytesPerSample: Int = 2 // 16-bit mono
    val bytesPerSecond: Int
        get() = sampleRate * bytesPerSample

    var bufferLengthSeconds: Int = bufferLengthSeconds.coerceAtLeast(5)
        set(value) {
            field = value.coerceAtLeast(5)
        }

    var overlapSeconds: Int = overlapSeconds.coerceAtLeast(0)
        set(value) {
            field = value.coerceAtLeast(0)
        }

    private val pcmStream = ByteArrayOutputStream()
    private var cycleStartTimeMillis: Long = System.currentTimeMillis()
    private var accumulatedCycleBytes = 0
    private val segmentCounter = AtomicInteger(1)

    @Synchronized
    fun appendPcmData(data: ByteArray, length: Int) {
        if (length <= 0) return

        if (pcmStream.size() == 0 && accumulatedCycleBytes == 0) {
            cycleStartTimeMillis = System.currentTimeMillis()
        }

        pcmStream.write(data, 0, length)
        accumulatedCycleBytes += length

        val targetCycleBytes = bufferLengthSeconds * bytesPerSecond

        if (accumulatedCycleBytes >= targetCycleBytes) {
            flushSegment()
        }
    }

    @Synchronized
    fun flushSegment() {
        val fullData = pcmStream.toByteArray()
        if (fullData.isEmpty()) return

        val endTimeMillis = System.currentTimeMillis()
        val totalDurationSeconds = (fullData.size / bytesPerSecond).coerceAtLeast(1)

        // Voice Activity Detection / Silence Check
        val hasSpeech = if (skipSilence) checkSpeechActivity(fullData) else true

        if (hasSpeech) {
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            val timestampStr = dateFormat.format(Date(cycleStartTimeMillis))
            val fileName = "chunk_${timestampStr}_${segmentCounter.getAndIncrement()}.wav"
            val outputFile = File(outputDirectory, fileName)

            // Save WAV file with current sample rate
            val success = WavFileWriter.writeWavFile(fullData, outputFile, sampleRate)
            if (success) {
                onSegmentCreated(outputFile, cycleStartTimeMillis, endTimeMillis, totalDurationSeconds, overlapSeconds)
            }
        }

        // Prepare next buffer carrying forward the overlap data
        pcmStream.reset()
        accumulatedCycleBytes = 0
        cycleStartTimeMillis = System.currentTimeMillis() - (overlapSeconds * 1000L)

        val overlapBytes = overlapSeconds * bytesPerSecond
        if (fullData.size > overlapBytes && overlapBytes > 0) {
            val overlapData = fullData.copyOfRange(fullData.size - overlapBytes, fullData.size)
            pcmStream.write(overlapData, 0, overlapData.size)
        }
    }

    /**
     * Calculates RMS energy of 16-bit PCM samples to filter out pure ambient silence.
     */
    private fun checkSpeechActivity(pcmData: ByteArray): Boolean {
        if (pcmData.size < 400) return false
        var sumSquares = 0.0
        val sampleCount = pcmData.size / 2

        for (i in 0 until pcmData.size - 1 step 2) {
            val sample = (pcmData[i].toInt() and 0xFF) or (pcmData[i + 1].toInt() shl 8)
            val shortVal = sample.toShort().toDouble()
            sumSquares += shortVal * shortVal
        }

        val rms = sqrt(sumSquares / sampleCount)
        // Energy threshold: typical silence is < 150 RMS
        return rms > 160.0
    }

    @Synchronized
    fun clear() {
        pcmStream.reset()
        accumulatedCycleBytes = 0
    }
}
