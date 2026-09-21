package com.example.data.local.mapper

import com.example.data.local.entity.NoteEntity
import com.example.domain.model.Note
import org.json.JSONArray

object NoteMapper {

    fun toEntity(note: Note): NoteEntity {
        val jsonArray = JSONArray()
        note.tags.forEach { jsonArray.put(it) }
        return NoteEntity(
            id = note.id,
            uuid = note.uuid,
            title = note.title,
            content = note.content,
            colorIndex = note.colorIndex,
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            isDeleted = note.isDeleted,
            reminderTime = note.reminderTime,
            createdAt = note.createdAt,
            updatedAt = note.updatedAt,
            tagsJson = jsonArray.toString(),
            isCheckedItemsList = note.isCheckedItemsList
        )
    }

    fun toDomain(entity: NoteEntity): Note {
        val tagsList = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(entity.tagsJson)
            for (i in 0 until jsonArray.length()) {
                tagsList.add(jsonArray.getString(i))
            }
        } catch (_: Exception) {
            // fallback
        }

        return Note(
            id = entity.id,
            uuid = entity.uuid,
            title = entity.title,
            content = entity.content,
            colorIndex = entity.colorIndex,
            isPinned = entity.isPinned,
            isArchived = entity.isArchived,
            isDeleted = entity.isDeleted,
            reminderTime = entity.reminderTime,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            tags = tagsList,
            isCheckedItemsList = entity.isCheckedItemsList
        )
    }
}
