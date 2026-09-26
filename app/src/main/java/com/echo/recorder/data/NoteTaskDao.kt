package com.echo.recorder.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteTaskDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: NoteTask): Long

    @Update
    suspend fun updateTask(task: NoteTask)

    @Query("DELETE FROM note_tasks WHERE id = :id")
    suspend fun deleteTask(id: Long)

    @Query("SELECT * FROM note_tasks ORDER BY isCompleted ASC, priority DESC, timestampMillis DESC")
    fun getAllTasksSortedByPriority(): Flow<List<NoteTask>>

    @Query("SELECT * FROM note_tasks WHERE isLowHangingFruit = 1 AND isCompleted = 0 ORDER BY priority DESC, timestampMillis DESC")
    fun getLowHangingFruits(): Flow<List<NoteTask>>

    @Query("SELECT * FROM note_tasks WHERE categoryGroup = :category ORDER BY isCompleted ASC, priority DESC, timestampMillis DESC")
    fun getTasksByCategory(category: String): Flow<List<NoteTask>>

    @Query("UPDATE note_tasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateTaskCompletion(id: Long, isCompleted: Boolean)
}
