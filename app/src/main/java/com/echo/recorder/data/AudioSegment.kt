package com.echo.recorder.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audio_segments")
data class AudioSegment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val filePath: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long,
    val durationSeconds: Int,
    val overlapSeconds: Int = 5,
    val status: String = STATUS_PENDING
) {
    companion object {
        const val STATUS_PENDING = "PENDING"
        const val STATUS_TRANSCRIBING = "TRANSCRIBING"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
    }
}
