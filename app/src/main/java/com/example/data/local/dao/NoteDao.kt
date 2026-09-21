package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun getActiveNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 1 ORDER BY updatedAt DESC")
    fun getTrashNotesFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE isDeleted = 0 AND reminderTime IS NOT NULL ORDER BY reminderTime ASC")
    fun getNotesWithRemindersFlow(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id LIMIT 1")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity): Long

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Delete
    suspend fun deleteNote(note: NoteEntity)

    // Soft delete / Move to trash
    @Query("UPDATE notes SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun moveToTrash(noteId: Long, updatedAt: Long = System.currentTimeMillis())

    // Restore from trash
    @Query("UPDATE notes SET isDeleted = 0, isArchived = 0, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun restoreFromTrash(noteId: Long, updatedAt: Long = System.currentTimeMillis())

    // Permanent delete
    @Query("DELETE FROM notes WHERE id = :noteId")
    suspend fun deletePermanently(noteId: Long)

    @Query("DELETE FROM notes WHERE isDeleted = 1")
    suspend fun clearTrash()

    // Pin toggle
    @Query("UPDATE notes SET isPinned = :isPinned, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun setPinned(noteId: Long, isPinned: Boolean, updatedAt: Long = System.currentTimeMillis())

    // Archive toggle
    @Query("UPDATE notes SET isArchived = :isArchived, updatedAt = :updatedAt WHERE id = :noteId")
    suspend fun setArchived(noteId: Long, isArchived: Boolean, updatedAt: Long = System.currentTimeMillis())

    // FTS4 полнотекстовый поиск по MATCH
    @Query("""
        SELECT notes.* FROM notes
        JOIN notes_fts ON notes.id = notes_fts.rowid
        WHERE notes.isDeleted = 0 
          AND notes_fts MATCH :query
        ORDER BY notes.isPinned DESC, notes.updatedAt DESC
    """)
    fun searchNotesFts(query: String): Flow<List<NoteEntity>>

    // Запасной LIKE поиск (для частичных совпадений без MATCH)
    @Query("""
        SELECT * FROM notes 
        WHERE isDeleted = 0 
          AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%' OR tagsJson LIKE '%' || :query || '%')
        ORDER BY isPinned DESC, updatedAt DESC
    """)
    fun searchNotesLike(query: String): Flow<List<NoteEntity>>
}
