package com.example.util

import com.example.domain.model.Note
import org.json.JSONArray
import org.json.JSONObject

object BackupRestoreUtil {

    fun exportToJson(notes: List<Note>): String {
        val root = JSONObject()
        root.put("version", 1)
        root.put("exportDate", System.currentTimeMillis())

        val array = JSONArray()
        for (note in notes) {
            val item = JSONObject()
            item.put("uuid", note.uuid)
            item.put("title", note.title)
            item.put("content", note.content)
            item.put("colorIndex", note.colorIndex)
            item.put("isPinned", note.isPinned)
            item.put("isArchived", note.isArchived)
            item.put("isDeleted", note.isDeleted)
            if (note.reminderTime != null) {
                item.put("reminderTime", note.reminderTime)
            }
            item.put("createdAt", note.createdAt)
            item.put("updatedAt", note.updatedAt)
            item.put("isCheckedItemsList", note.isCheckedItemsList)
            item.put("isLocked", note.isLocked)

            val tagsArray = JSONArray()
            note.tags.forEach { tagsArray.put(it) }
            item.put("tags", tagsArray)

            val imagesArray = JSONArray()
            note.imageUris.forEach { imagesArray.put(it) }
            item.put("imageUris", imagesArray)

            array.put(item)
        }
        root.put("notes", array)

        return root.toString(2)
    }

    fun importFromJson(jsonString: String): List<Note> {
        val notes = mutableListOf<Note>()
        try {
            val root = JSONObject(jsonString)
            val array = root.optJSONArray("notes") ?: return emptyList()

            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val tagsList = mutableListOf<String>()
                val tagsArray = item.optJSONArray("tags")
                if (tagsArray != null) {
                    for (t in 0 until tagsArray.length()) {
                        tagsList.add(tagsArray.getString(t))
                    }
                }

                val imagesList = mutableListOf<String>()
                val imagesArray = item.optJSONArray("imageUris")
                if (imagesArray != null) {
                    for (img in 0 until imagesArray.length()) {
                        imagesList.add(imagesArray.getString(img))
                    }
                }

                val note = Note(
                    id = 0L,
                    uuid = item.optString("uuid", java.util.UUID.randomUUID().toString()),
                    title = item.optString("title", ""),
                    content = item.optString("content", ""),
                    colorIndex = item.optInt("colorIndex", 0),
                    isPinned = item.optBoolean("isPinned", false),
                    isArchived = item.optBoolean("isArchived", false),
                    isDeleted = item.optBoolean("isDeleted", false),
                    reminderTime = if (item.has("reminderTime")) item.getLong("reminderTime") else null,
                    createdAt = item.optLong("createdAt", System.currentTimeMillis()),
                    updatedAt = item.optLong("updatedAt", System.currentTimeMillis()),
                    tags = tagsList,
                    isCheckedItemsList = item.optBoolean("isCheckedItemsList", false),
                    imageUris = imagesList,
                    isLocked = item.optBoolean("isLocked", false)
                )
                notes.add(note)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return notes
    }
}
