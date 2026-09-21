package com.example.domain.repository

import com.example.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun getActiveNotes(): Flow<List<Note>>
    fun getArchivedNotes(): Flow<List<Note>>
    fun getTrashNotes(): Flow<List<Note>>
    fun getReminderNotes(): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    suspend fun saveNote(note: Note): Long
    suspend fun moveToTrash(noteId: Long)
    suspend fun restoreFromTrash(noteId: Long)
    suspend fun deletePermanently(noteId: Long)
    suspend fun clearTrash()
    suspend fun togglePin(noteId: Long, isPinned: Boolean)
    suspend fun toggleArchive(noteId: Long, isArchived: Boolean)
    fun searchNotes(query: String): Flow<List<Note>>
}
