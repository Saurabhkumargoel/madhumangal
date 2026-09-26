package com.echo.recorder.audio

import kotlin.math.roundToInt

/**
 * Linear-interpolation PCM Resampler for 16-bit Mono Little-Endian Audio.
 * Converts PCM audio stream between any hardware sampling rate (e.g. 16000, 44100, 48000)
 * and target sampling rate (8000, 16000, 22050, 44100) with zero pitch distortion.
 */
class PcmResampler(
    val inSampleRate: Int,
    val outSampleRate: Int
) {
    private val isPassThrough = (inSampleRate == outSampleRate)
    private val ratio = inSampleRate.toDouble() / outSampleRate.toDouble()

    /**
     * Resamples the given 16-bit mono PCM byte array from [inSampleRate] to [outSampleRate].
     * @param input Raw bytes of 16-bit little-endian PCM
     * @param length Number of valid bytes in [input]
     * @return New byte array resampled to [outSampleRate]
     */
    fun resample(input: ByteArray, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        if (isPassThrough) {
            return input.copyOf(length)
        }

        val inSamplesCount = length / 2
        if (inSamplesCount == 0) return ByteArray(0)

        // Calculate expected output sample count
        val outSamplesCount = (inSamplesCount / ratio).roundToInt().coerceAtLeast(1)
        val output = ByteArray(outSamplesCount * 2)

        for (i in 0 until outSamplesCount) {
            val inPos = i * ratio
            val idx = inPos.toInt()
            val frac = inPos - idx

            val s1 = getSample(input, idx, inSamplesCount)
            val s2 = if (idx + 1 < inSamplesCount) getSample(input, idx + 1, inSamplesCount) else s1

            val interpolated = ((1.0 - frac) * s1 + frac * s2).roundToInt().coerceIn(-32768, 32767)

            // Write 16-bit sample in little-endian format
            output[i * 2] = (interpolated and 0xFF).toByte()
            output[i * 2 + 1] = ((interpolated shr 8) and 0xFF).toByte()
        }

        return output
    }

    private fun getSample(buffer: ByteArray, index: Int, maxSamples: Int): Short {
        if (index < 0 || index >= maxSamples) return 0
        val byteIndex = index * 2
        val low = buffer[byteIndex].toInt() and 0xFF
        val high = buffer[byteIndex + 1].toInt()
        return ((high shl 8) or low).toShort()
    }
}
