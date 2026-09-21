package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Fts4

/**
 * Виртуальная таблица FTS4 для молниеносного полнотекстового поиска
 * с поддержкой токенизатора unicode61 для качественного поиска по кириллице и латинице
 */
@Entity(tableName = "notes_fts")
@Fts4(contentEntity = NoteEntity::class, tokenizer = "unicode61")
data class NoteFtsEntity(
    val title: String,
    val content: String,
    val tagsJson: String
)
