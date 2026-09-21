package com.example.data.repository

import com.example.data.local.dao.NoteDao
import com.example.data.local.mapper.NoteMapper
import com.example.domain.model.Note
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val noteDao: NoteDao
) : NoteRepository {

    override fun getActiveNotes(): Flow<List<Note>> {
        return noteDao.getActiveNotesFlow().map { list ->
            list.map { NoteMapper.toDomain(it) }
        }
    }

    override fun getArchivedNotes(): Flow<List<Note>> {
        return noteDao.getArchivedNotesFlow().map { list ->
            list.map { NoteMapper.toDomain(it) }
        }
    }

    override fun getTrashNotes(): Flow<List<Note>> {
        return noteDao.getTrashNotesFlow().map { list ->
            list.map { NoteMapper.toDomain(it) }
        }
    }

    override fun getReminderNotes(): Flow<List<Note>> {
        return noteDao.getNotesWithRemindersFlow().map { list ->
            list.map { NoteMapper.toDomain(it) }
        }
    }

    override fun searchNotes(query: String): Flow<List<Note>> {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) {
            return getActiveNotes()
        }
        // Пытаемся FTS4 с match query, при исключении синтаксиса откатываемся на LIKE
        val ftsFormatted = "*$cleanQuery*"
        return noteDao.searchNotesFts(ftsFormatted)
            .catch {
                // Если FTS4 дал ошибку парсера (например, спецсимволы), прозрачно переключаемся на LIKE
                emitAll(noteDao.searchNotesLike(cleanQuery))
            }
            .map { list -> list.map { NoteMapper.toDomain(it) } }
    }

    override suspend fun getNoteById(id: Long): Note? {
        val entity = noteDao.getNoteById(id) ?: return null
        return NoteMapper.toDomain(entity)
    }

    override suspend fun saveNote(note: Note): Long {
        val entity = NoteMapper.toEntity(note)
        return noteDao.insertNote(entity)
    }

    override suspend fun moveToTrash(noteId: Long) {
        noteDao.moveToTrash(noteId)
    }

    override suspend fun restoreFromTrash(noteId: Long) {
        noteDao.restoreFromTrash(noteId)
    }

    override suspend fun deletePermanently(noteId: Long) {
        noteDao.deletePermanently(noteId)
    }

    override suspend fun clearTrash() {
        noteDao.clearTrash()
    }

    override suspend fun togglePin(noteId: Long, isPinned: Boolean) {
        noteDao.setPinned(noteId, isPinned)
    }

    override suspend fun toggleArchive(noteId: Long, isArchived: Boolean) {
        noteDao.setArchived(noteId, isArchived)
    }

    override suspend fun getAllActiveNotesList(): List<Note> {
        return noteDao.getAllActiveNotesList().map { NoteMapper.toDomain(it) }
    }

    override suspend fun importNotes(notes: List<Note>) {
        val entities = notes.map { NoteMapper.toEntity(it.copy(id = 0)) }
        noteDao.insertNotesList(entities)
    }

    override suspend fun batchMoveToTrash(ids: List<Long>) {
        if (ids.isNotEmpty()) {
            noteDao.batchMoveToTrash(ids)
        }
    }

    override suspend fun batchArchive(ids: List<Long>, isArchived: Boolean) {
        if (ids.isNotEmpty()) {
            noteDao.batchSetArchived(ids, isArchived)
        }
    }

    override suspend fun batchTogglePin(ids: List<Long>, isPinned: Boolean) {
        if (ids.isNotEmpty()) {
            noteDao.batchSetPinned(ids, isPinned)
        }
    }

    override suspend fun batchChangeColor(ids: List<Long>, colorIndex: Int) {
        if (ids.isNotEmpty()) {
            noteDao.batchSetColor(ids, colorIndex)
        }
    }

    override suspend fun batchMoveToFolder(ids: List<Long>, folder: String) {
        if (ids.isNotEmpty()) {
            noteDao.batchSetFolder(ids, folder)
        }
    }
}
