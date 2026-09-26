package com.echo.recorder.audio

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object WavFileWriter {

    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16

    /**
     * Writes raw 16-bit PCM byte array to a standard WAV audio file with RIFF header.
     * Supports dynamic sample rates (e.g. 8000 Hz for tiniest quality, 16000 Hz, etc.)
     */
    fun writeWavFile(pcmData: ByteArray, outputFile: File, sampleRate: Int = 8000): Boolean {
        return try {
            FileOutputStream(outputFile).use { out ->
                val totalAudioLen = pcmData.size.toLong()
                val totalDataLen = totalAudioLen + 36
                val byteRate = sampleRate * CHANNELS * BITS_PER_SAMPLE / 8

                val header = createWavHeader(totalAudioLen, totalDataLen, sampleRate, CHANNELS, byteRate)
                out.write(header, 0, 44)
                out.write(pcmData)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    private fun createWavHeader(
        audioLen: Long,
        dataLen: Long,
        sampleRate: Int,
        channels: Int,
        byteRate: Int
    ): ByteArray {
        val header = ByteArray(44)
        header[0] = 'R'.code.toByte() // RIFF/WAVE header
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (dataLen and 0xffL).toByte()
        header[5] = (dataLen shr 8 and 0xffL).toByte()
        header[6] = (dataLen shr 16 and 0xffL).toByte()
        header[7] = (dataLen shr 24 and 0xffL).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte() // 'fmt ' chunk
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16 // 16 for PCM format
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1 // Audio format (1 = PCM)
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = (sampleRate shr 8 and 0xff).toByte()
        header[26] = (sampleRate shr 16 and 0xff).toByte()
        header[27] = (sampleRate shr 24 and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = (byteRate shr 8 and 0xff).toByte()
        header[30] = (byteRate shr 16 and 0xff).toByte()
        header[31] = (byteRate shr 24 and 0xff).toByte()
        header[32] = (channels * BITS_PER_SAMPLE / 8).toByte() // block align
        header[33] = 0
        header[34] = BITS_PER_SAMPLE.toByte() // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (audioLen and 0xffL).toByte()
        header[41] = (audioLen shr 8 and 0xffL).toByte()
        header[42] = (audioLen shr 16 and 0xffL).toByte()
        header[43] = (audioLen shr 24 and 0xffL).toByte()
        return header
    }
}
