package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Основная сущность заметки в Room
 */
@Entity(
    tableName = "notes",
    indices = [
        Index("isDeleted"),
        Index("isArchived"),
        Index("isPinned"),
        Index("reminderTime"),
        Index("updatedAt")
    ]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val uuid: String,
    val title: String,
    val content: String,
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val reminderTime: Long? = null,
    val createdAt: Long,
    val updatedAt: Long,
    val tagsJson: String = "[]", // Храним список тегов компактно
    val isCheckedItemsList: Boolean = false
)
