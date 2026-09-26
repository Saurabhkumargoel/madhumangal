package com.echo.recorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioSegmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegment(segment: AudioSegment): Long

    @Update
    suspend fun updateSegment(segment: AudioSegment): Int

    @Query("SELECT * FROM audio_segments ORDER BY startTimeMillis DESC")
    fun getAllSegments(): Flow<List<AudioSegment>>

    @Query("SELECT * FROM audio_segments WHERE status = :status ORDER BY startTimeMillis ASC")
    suspend fun getSegmentsByStatus(status: String): List<AudioSegment>

    @Query("DELETE FROM audio_segments WHERE id = :id")
    suspend fun deleteSegment(id: Long)

    @Query("SELECT * FROM audio_segments WHERE id = :id LIMIT 1")
    suspend fun getSegmentById(id: Long): AudioSegment?
}
