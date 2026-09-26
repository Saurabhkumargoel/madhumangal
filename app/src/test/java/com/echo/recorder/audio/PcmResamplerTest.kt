package com.echo.recorder.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PcmResamplerTest {

    @Test
    fun testPassThrough_whenSameRate() {
        val resampler = PcmResampler(16000, 16000)
        val input = byteArrayOf(10, 0, 20, 0, 30, 0)
        val result = resampler.resample(input, input.size)
        assertEquals(input.size, result.size)
        for (i in input.indices) {
            assertEquals(input[i], result[i])
        }
    }

    @Test
    fun testDownsample_16kTo8k_halvesSampleCount() {
        val resampler = PcmResampler(16000, 8000)
        // 16000 samples = 32000 bytes (1 second of audio)
        val input = ByteArray(32000)
        val result = resampler.resample(input, input.size)
        // Should yield 8000 samples = 16000 bytes (exactly 1 second at 8 kHz)
        assertEquals(16000, result.size)
    }

    @Test
    fun testUpsample_16kTo44k_preservesDuration() {
        val resampler = PcmResampler(16000, 44100)
        // 1600 samples = 3200 bytes (0.1 second at 16 kHz)
        val input = ByteArray(3200)
        val result = resampler.resample(input, input.size)
        // At 44.1 kHz, 0.1 second should be 4410 samples = 8820 bytes
        val expectedBytes = 4410 * 2
        assertEquals(expectedBytes, result.size)
    }

    @Test
    fun testResample_16kTo22k_preservesDuration() {
        val resampler = PcmResampler(16000, 22050)
        // 16000 samples = 32000 bytes (1 second at 16 kHz)
        val input = ByteArray(32000)
        val result = resampler.resample(input, input.size)
        // At 22050 Hz, 1 second is 22050 samples = 44100 bytes
        assertEquals(44100, result.size)
    }

    @Test
    fun testEmptyInput_returnsEmpty() {
        val resampler = PcmResampler(16000, 8000)
        val result = resampler.resample(ByteArray(0), 0)
        assertEquals(0, result.size)
    }
}
