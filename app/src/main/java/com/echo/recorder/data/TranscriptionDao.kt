package com.echo.recorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class TranscriptionWithAudio(
    val id: Long,
    val segmentId: Long,
    val text: String,
    val timestampMillis: Long,
    val durationSeconds: Int,
    val filePath: String,
    val overlapSeconds: Int,
    val sessionId: String = ""
)

@Dao
interface TranscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTranscription(transcription: Transcription): Long

    @Query("SELECT t.id, t.segmentId, t.text, t.timestampMillis, t.durationSeconds, a.filePath, a.overlapSeconds, a.sessionId FROM transcriptions t INNER JOIN audio_segments a ON t.segmentId = a.id ORDER BY t.timestampMillis DESC")
    fun getAllTranscriptionsWithAudio(): Flow<List<TranscriptionWithAudio>>

    @Query("SELECT t.id, t.segmentId, t.text, t.timestampMillis, t.durationSeconds, a.filePath, a.overlapSeconds, a.sessionId FROM transcriptions t INNER JOIN audio_segments a ON t.segmentId = a.id WHERE t.text LIKE '%' || :query || '%' ORDER BY t.timestampMillis DESC")
    fun searchTranscriptions(query: String): Flow<List<TranscriptionWithAudio>>

    @Query("SELECT t.id, t.segmentId, t.text, t.timestampMillis, t.durationSeconds, a.filePath, a.overlapSeconds, a.sessionId FROM transcriptions t INNER JOIN audio_segments a ON t.segmentId = a.id WHERE t.id = :id LIMIT 1")
    suspend fun getTranscriptionWithAudioById(id: Long): TranscriptionWithAudio?

    @Query("SELECT t.id, t.segmentId, t.text, t.timestampMillis, t.durationSeconds, a.filePath, a.overlapSeconds, a.sessionId FROM transcriptions t INNER JOIN audio_segments a ON t.segmentId = a.id WHERE a.sessionId = :sessionId ORDER BY t.timestampMillis ASC")
    suspend fun getTranscriptionsBySessionId(sessionId: String): List<TranscriptionWithAudio>

    @Query("SELECT * FROM transcriptions WHERE id = :id LIMIT 1")
    suspend fun getTranscriptionById(id: Long): Transcription?

    @Query("UPDATE transcriptions SET text = :text WHERE id = :id")
    suspend fun updateTranscriptionText(id: Long, text: String)

    @Query("UPDATE transcriptions SET text = :defaultText WHERE text LIKE '[Pending:%' OR text LIKE 'Voice speech recorded in%' OR text LIKE '[Whisper Base]:%'")
    suspend fun cleanupPendingErrorTexts(defaultText: String = "Audio slice ready for transcription")

    @Query("SELECT * FROM transcriptions WHERE text LIKE '%Audio Source:%' OR text LIKE '- **%' OR text LIKE '# 🎙️%'")
    suspend fun getContaminatedTranscriptions(): List<Transcription>

    @Query("DELETE FROM transcriptions WHERE id = :id")
    suspend fun deleteTranscription(id: Long)

    @Query("DELETE FROM transcriptions WHERE segmentId = :segmentId")
    suspend fun deleteBySegmentId(segmentId: Long)
}
