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

    @Query("SELECT * FROM note_tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Long): NoteTask?

    @Query("SELECT * FROM note_tasks WHERE parentId = :parentId ORDER BY orderIndex ASC, timestampMillis ASC")
    suspend fun getSubtasksForParent(parentId: Long): List<NoteTask>

    @Query("DELETE FROM note_tasks WHERE parentId = :parentId")
    suspend fun deleteSubtasks(parentId: Long)

    @Query("UPDATE note_tasks SET categoryGroup = :newCategory WHERE id = :id")
    suspend fun updateTaskCategory(id: Long, newCategory: String)

    @Query("UPDATE note_tasks SET title = :title, description = :description, categoryGroup = :category, priority = :priority WHERE id = :id")
    suspend fun updateTaskDetails(id: Long, title: String, description: String, category: String, priority: Int)

    @Query("UPDATE note_tasks SET orderIndex = :orderIndex WHERE id = :id")
    suspend fun updateTaskOrder(id: Long, orderIndex: Int)

    @Query("SELECT * FROM note_tasks ORDER BY isCompleted ASC, priority DESC, timestampMillis DESC")
    suspend fun getAllTasksList(): List<NoteTask>

    @Query("DELETE FROM note_tasks WHERE title LIKE 'Voice speech recorded in%' OR title LIKE '[Whisper Base]:%' OR title LIKE '[Pending:%' OR title LIKE '%Audio Source:%' OR title LIKE '- **%' OR title LIKE '# 🎙️%'")
    suspend fun cleanupDummyTasks()
}
