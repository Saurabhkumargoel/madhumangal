package com.echo.recorder.audio

import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class AudioBufferManager(
    private val outputDirectory: File,
    bufferLengthSeconds: Int = 60,
    overlapSeconds: Int = 5,
    private val onSegmentCreated: (file: File, startTimeMillis: Long, endTimeMillis: Long, durationSeconds: Int, overlapSeconds: Int) -> Unit
) {

    companion object {
        const val SAMPLE_RATE = 16000
        const val BYTES_PER_SAMPLE = 2 // 16-bit mono
        const val BYTES_PER_SECOND = SAMPLE_RATE * BYTES_PER_SAMPLE // 32,000 bytes/sec
    }

    var bufferLengthSeconds: Int = bufferLengthSeconds.coerceAtLeast(10)
        set(value) {
            field = value.coerceAtLeast(10)
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

        val targetCycleBytes = bufferLengthSeconds * BYTES_PER_SECOND

        if (accumulatedCycleBytes >= targetCycleBytes) {
            flushSegment()
        }
    }

    @Synchronized
    fun flushSegment() {
        val fullData = pcmStream.toByteArray()
        if (fullData.isEmpty()) return

        val endTimeMillis = System.currentTimeMillis()
        val totalDurationSeconds = fullData.size / BYTES_PER_SECOND

        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val timestampStr = dateFormat.format(Date(cycleStartTimeMillis))
        val fileName = "segment_${timestampStr}_${segmentCounter.getAndIncrement()}.wav"
        val outputFile = File(outputDirectory, fileName)

        // Save WAV file
        val success = WavFileWriter.writeWavFile(fullData, outputFile)
        if (success) {
            onSegmentCreated(outputFile, cycleStartTimeMillis, endTimeMillis, totalDurationSeconds, overlapSeconds)
        }

        // Prepare next buffer carrying forward the overlap data (e.g. 5 seconds)
        pcmStream.reset()
        accumulatedCycleBytes = 0
        cycleStartTimeMillis = System.currentTimeMillis() - (overlapSeconds * 1000L)

        val overlapBytes = overlapSeconds * BYTES_PER_SECOND
        if (fullData.size > overlapBytes && overlapBytes > 0) {
            val overlapData = fullData.copyOfRange(fullData.size - overlapBytes, fullData.size)
            pcmStream.write(overlapData, 0, overlapData.size)
        }
    }

    @Synchronized
    fun clear() {
        pcmStream.reset()
        accumulatedCycleBytes = 0
    }
}
