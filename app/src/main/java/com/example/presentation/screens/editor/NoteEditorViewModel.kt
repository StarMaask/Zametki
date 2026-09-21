package com.example.presentation.screens.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.Note
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NoteEditorUiState(
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val colorIndex: Int = 0,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val reminderTime: Long? = null,
    val tags: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = true,
    val characterCount: Int = 0,
    val wordCount: Int = 0,
    val isListeningSpeech: Boolean = false
)

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState())
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: Job? = null

    init {
        if (initialNoteId > 0) {
            loadNote(initialNoteId)
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(id) ?: return@launch
            _uiState.update {
                it.copy(
                    id = note.id,
                    title = note.title,
                    content = note.content,
                    colorIndex = note.colorIndex,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    reminderTime = note.reminderTime,
                    tags = note.tags,
                    characterCount = note.content.length,
                    wordCount = calculateWords(note.content),
                    isSaved = true
                )
            }
        }
    }

    fun onTitleChange(newTitle: String) {
        _uiState.update { it.copy(title = newTitle, isSaved = false) }
        scheduleAutoSave()
    }

    fun onContentChange(newContent: String) {
        _uiState.update {
            it.copy(
                content = newContent,
                characterCount = newContent.length,
                wordCount = calculateWords(newContent),
                isSaved = false
            )
        }
        scheduleAutoSave()
    }

    fun onColorChange(newColorIndex: Int) {
        _uiState.update { it.copy(colorIndex = newColorIndex, isSaved = false) }
        scheduleAutoSave()
    }

    fun togglePin() {
        val nextState = !_uiState.value.isPinned
        _uiState.update { it.copy(isPinned = nextState, isSaved = false) }
        scheduleAutoSave()
    }

    fun addTag(tag: String) {
        val cleanTag = tag.trim().replace("#", "")
        if (cleanTag.isNotEmpty() && !_uiState.value.tags.contains(cleanTag)) {
            val updated = _uiState.value.tags + cleanTag
            _uiState.update { it.copy(tags = updated, isSaved = false) }
            scheduleAutoSave()
        }
    }

    fun removeTag(tag: String) {
        val updated = _uiState.value.tags - tag
        _uiState.update { it.copy(tags = updated, isSaved = false) }
        scheduleAutoSave()
    }

    fun setReminder(timeMs: Long?) {
        _uiState.update { it.copy(reminderTime = timeMs, isSaved = false) }
        scheduleAutoSave()
    }

    fun appendSpeechText(recognizedText: String) {
        val current = _uiState.value.content
        val next = if (current.isBlank()) recognizedText else "$current $recognizedText"
        onContentChange(next)
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            delay(1500) // Автосохранение через 1.5 сек после остановки ввода
            saveNow()
        }
    }

    fun saveNow() {
        viewModelScope.launch {
            val s = _uiState.value
            // Не сохраняем абсолютно пустую новую заметку
            if (s.title.isBlank() && s.content.isBlank() && s.tags.isEmpty()) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
                return@launch
            }

            val note = Note(
                id = s.id,
                title = s.title.trim(),
                content = s.content.trim(),
                colorIndex = s.colorIndex,
                isPinned = s.isPinned,
                isArchived = s.isArchived,
                reminderTime = s.reminderTime,
                tags = s.tags,
                updatedAt = System.currentTimeMillis()
            )

            val savedId = repository.saveNote(note)
            _uiState.update {
                it.copy(
                    id = if (s.id == 0L) savedId else s.id,
                    isSaving = false,
                    isSaved = true
                )
            }
        }
    }

    private fun calculateWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }
}
