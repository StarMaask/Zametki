package com.example.domain.model

/**
 * Доменная модель заметки
 */
data class Note(
    val id: Long = 0,
    val uuid: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val colorIndex: Int = 0, // 0..7 из палитры NoteTagColors
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false, // Soft delete для корзины (30 дней)
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList(),
    val isCheckedItemsList: Boolean = false,
    val imageUris: List<String> = emptyList(),
    val isLocked: Boolean = false,
    val folder: String = "",
    val audioUri: String? = null
)

/**
 * Чек-лист элемент заметки
 */
data class CheckListItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val text: String,
    val isDone: Boolean = false
)
