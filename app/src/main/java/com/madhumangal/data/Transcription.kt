package com.madhumangal.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transcriptions",
    foreignKeys = [
        ForeignKey(
            entity = AudioSegment::class,
            parentColumns = ["id"],
            childColumns = ["segmentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["segmentId"])]
)
data class Transcription(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val segmentId: Long,
    val text: String,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val confidence: Float = 1.0f
)
