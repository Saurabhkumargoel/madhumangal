package com.echo.recorder.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Entity(tableName = "ai_chat_messages")
data class AiChatMessageEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val suggestedGroup: String? = null,
    val suggestedItemsJson: String = ""
)

@Dao
interface AiChatDao {
    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<AiChatMessageEntity>>

    @Query("SELECT * FROM ai_chat_messages ORDER BY timestamp ASC")
    suspend fun getAllMessages(): List<AiChatMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: AiChatMessageEntity)

    @Query("DELETE FROM ai_chat_messages")
    suspend fun clearHistory()
}
