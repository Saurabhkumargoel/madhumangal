package com.madhumangal.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_tasks",
    foreignKeys = [
        ForeignKey(
            entity = Transcription::class,
            parentColumns = ["id"],
            childColumns = ["transcriptionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["transcriptionId"])]
)
data class NoteTask(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val transcriptionId: Long? = null,
    val parentId: Long? = null,
    val title: String,
    val description: String = "",
    val categoryGroup: String = "General", // Work, Personal, Shopping, Urgent, Ideas, General, or custom
    val priority: Int = PRIORITY_MEDIUM, // 3 = HIGH, 2 = MEDIUM, 1 = LOW
    val isLowHangingFruit: Boolean = false,
    val isCompleted: Boolean = false,
    val autoCompleteOnSubtasksDone: Boolean = true,
    val dueDateMillis: Long? = null,
    val dueTimeText: String = "",
    val orderIndex: Int = 0,
    val timestampMillis: Long = System.currentTimeMillis()
) {
    companion object {
        const val PRIORITY_HIGH = 3
        const val PRIORITY_MEDIUM = 2
        const val PRIORITY_LOW = 1
    }
}
