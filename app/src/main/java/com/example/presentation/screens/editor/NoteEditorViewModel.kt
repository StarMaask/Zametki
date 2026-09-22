package com.example.presentation.screens.editor

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.domain.model.CheckListItem
import com.example.domain.model.Note
import com.example.domain.model.NoteTemplate
import com.example.domain.model.PageFormat
import com.example.domain.repository.NoteRepository
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

data class NoteEditorUiState(
    val noteId: Long = 0L,
    val title: String = "",
    val content: String = "",
    val colorHex: String = "#FFFFFF",
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isLocked: Boolean = false,
    val reminderTime: Long? = null,
    val tags: List<String> = emptyList(),
    val checkList: List<CheckListItem> = emptyList(),
    val imageUris: List<String> = emptyList(),
    val audioUri: String? = null,
    val folder: String? = null,
    val availableFolders: List<String> = emptyList(),
    val isChecklistMode: Boolean = false,
    val sortCompletedToEnd: Boolean = true,
    val isMarkdownPreview: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isSaved: Boolean = false,
    val pageFormat: PageFormat = PageFormat.BOOK,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val wordCount: Int
        get() = if (content.isBlank()) 0 else content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size

    val charCount: Int
        get() = content.length

    val readingTimeFormatted: String
        get() {
            val words = wordCount
            return when {
                words == 0 -> "0 мин"
                words < 100 -> "< 1 мин"
                else -> "~${maxOf(1, (words + 90) / 180)} мин"
            }
        }

    fun toDomainNote(): Note {
        val checklistJson = if (checkList.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(checkList) else ""
        val imageUrisJson = if (imageUris.isNotEmpty()) kotlinx.serialization.json.Json.encodeToString(imageUris) else ""
        return Note(
            id = noteId,
            title = title,
            content = content,
            colorHex = colorHex,
            isPinned = isPinned,
            isArchived = isArchived,
            isDeleted = false,
            createdAt = createdAt,
            updatedAt = updatedAt,
            reminderTime = reminderTime,
            tags = tags,
            checkListJson = checklistJson,
            imageUrisJson = imageUrisJson,
            audioUri = audioUri,
            folder = folder,
            isLocked = isLocked,
            pageFormat = pageFormat.name
        )
    }
}

class NoteEditorViewModel(
    private val repository: NoteRepository,
    private val initialNoteId: Long,
    initialTemplate: NoteTemplate? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(NoteEditorUiState(noteId = initialNoteId))
    val uiState: StateFlow<NoteEditorUiState> = _uiState.asStateFlow()

    private val undoStack = mutableListOf<String>()
    private val redoStack = mutableListOf<String>()
    private var lastRecordedText: String = ""

    init {
        loadFolders()
        if (initialNoteId > 0) {
            loadNote(initialNoteId)
        } else if (initialTemplate != null) {
            applyTemplate(initialTemplate)
        }
    }

    private fun loadFolders() {
        viewModelScope.launch {
            repository.getAllFolders().collect { folders ->
                _uiState.update { it.copy(availableFolders = folders) }
            }
        }
    }

    private fun loadNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNoteById(id) ?: return@launch
            val parsedChecklist = try {
                if (note.checkListJson.isNotBlank()) Json.decodeFromString<List<CheckListItem>>(note.checkListJson) else emptyList()
            } catch (_: Exception) { emptyList() }

            val parsedImages = try {
                if (note.imageUrisJson.isNotBlank()) Json.decodeFromString<List<String>>(note.imageUrisJson) else emptyList()
            } catch (_: Exception) { emptyList() }

            _uiState.update {
                it.copy(
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    colorHex = note.colorHex,
                    isPinned = note.isPinned,
                    isArchived = note.isArchived,
                    reminderTime = note.reminderTime,
                    tags = note.tags,
                    checkList = parsedChecklist,
                    imageUris = parsedImages,
                    audioUri = note.audioUri,
                    folder = note.folder,
                    isLocked = note.isLocked,
                    isChecklistMode = parsedChecklist.isNotEmpty(),
                    pageFormat = try { PageFormat.valueOf(note.pageFormat) } catch (_: Exception) { PageFormat.BOOK },
                    createdAt = note.createdAt,
                    updatedAt = note.updatedAt
                )
            }
            lastRecordedText = note.content
        }
    }

    fun onPageFormatChange(format: PageFormat) {
        _uiState.update { it.copy(pageFormat = format) }
    }

    fun onTitleChange(title: String) { _uiState.update { it.copy(title = title) } }

    fun onContentChange(content: String) {
        val previous = _uiState.value.content
        if (previous != content) {
            undoStack.add(previous)
            redoStack.clear()
            _uiState.update {
                it.copy(
                    content = content,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val current = _uiState.value.content
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(current)
            _uiState.update {
                it.copy(
                    content = previous,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val current = _uiState.value.content
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(current)
            _uiState.update {
                it.copy(
                    content = next,
                    canUndo = undoStack.isNotEmpty(),
                    canRedo = redoStack.isNotEmpty()
                )
            }
        }
    }

    fun toggleMarkdownPreview() {
        _uiState.update { it.copy(isMarkdownPreview = !it.isMarkdownPreview) }
    }

    fun toggleLock() {
        _uiState.update { it.copy(isLocked = !it.isLocked) }
    }

    fun onColorChange(colorHex: String) { _uiState.update { it.copy(colorHex = colorHex) } }
    fun togglePin() { _uiState.update { it.copy(isPinned = !it.isPinned) } }
    fun onFolderChange(folder: String?) { _uiState.update { it.copy(folder = folder) } }
    fun onAudioUriChange(uri: String?) { _uiState.update { it.copy(audioUri = uri) } }
    fun onReminderChange(time: Long?) { _uiState.update { it.copy(reminderTime = time) } }

    fun appendFormatting(prefix: String, suffix: String = "") {
        _uiState.update { current ->
            val currentText = current.content
            val newText = if (currentText.isEmpty()) {
                "$prefix$suffix"
            } else if (currentText.endsWith("\n")) {
                "$currentText$prefix$suffix"
            } else {
                "$currentText\n$prefix$suffix"
            }
            current.copy(content = newText)
        }
    }

    fun toggleArchive(onCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            val nextArchived = !state.isArchived
            _uiState.update { it.copy(isArchived = nextArchived) }
            if (state.noteId > 0) {
                val existing = repository.getNoteById(state.noteId)
                if (existing != null) {
                    repository.updateNote(existing.copy(isArchived = nextArchived, updatedAt = System.currentTimeMillis()))
                }
            }
            onCompleted()
        }
    }

    fun addTag(tag: String) {
        val clean = tag.trim().removePrefix("#")
        if (clean.isNotBlank() && !_uiState.value.tags.contains(clean)) {
            _uiState.update { it.copy(tags = it.tags + clean) }
        }
    }

    fun removeTag(tag: String) {
        _uiState.update { it.copy(tags = it.tags - tag) }
    }

    fun addImage(uri: String) {
        _uiState.update { it.copy(imageUris = it.imageUris + uri) }
    }

    fun removeImage(uri: String) {
        _uiState.update { it.copy(imageUris = it.imageUris - uri) }
    }

    fun toggleChecklistMode() {
        _uiState.update { current ->
            val nextMode = !current.isChecklistMode
            val items = if (nextMode && current.checkList.isEmpty()) {
                current.content.lines().filter { it.isNotBlank() }.map {
                    CheckListItem(id = System.currentTimeMillis().toString() + "_" + it.hashCode(), text = it)
                }
            } else current.checkList
            current.copy(isChecklistMode = nextMode, checkList = items)
        }
    }

    fun addChecklistItem(text: String) {
        if (text.isNotBlank()) {
            val item = CheckListItem(id = System.currentTimeMillis().toString(), text = text)
            _uiState.update { it.copy(checkList = it.checkList + item) }
        }
    }

    fun applyTemplate(template: NoteTemplate) {
        if (template == NoteTemplate.BLANK) return
        val items = template.checklistItems.mapIndexed { index, text ->
            CheckListItem(id = "${System.currentTimeMillis()}_$index", text = text, isChecked = false)
        }
        _uiState.update { current ->
            current.copy(
                title = if (current.title.isBlank()) template.defaultTitle else current.title,
                colorHex = template.colorHex,
                folder = template.defaultFolder ?: current.folder,
                tags = (current.tags + template.defaultTags).distinct(),
                checkList = items,
                isChecklistMode = items.isNotEmpty()
            )
        }
    }

    fun toggleSortCompletedToEnd() {
        _uiState.update { current ->
            val next = !current.sortCompletedToEnd
            val sortedList = if (next) {
                current.checkList.sortedBy { it.isChecked }
            } else current.checkList
            current.copy(sortCompletedToEnd = next, checkList = sortedList)
        }
    }

    fun sortChecklistCompletedItems() {
        _uiState.update { current ->
            current.copy(checkList = current.checkList.sortedBy { it.isChecked })
        }
    }

    fun toggleChecklistItem(id: String) {
        _uiState.update { current ->
            val updated = current.checkList.map {
                if (it.id == id) it.copy(isChecked = !it.isChecked) else it
            }
            val finalChecklist = if (current.sortCompletedToEnd) {
                updated.sortedBy { it.isChecked }
            } else {
                updated
            }
            current.copy(checkList = finalChecklist)
        }
    }

    fun removeChecklistItem(id: String) {
        _uiState.update { current ->
            current.copy(checkList = current.checkList.filter { it.id != id })
        }
    }

    fun saveNote(context: Context? = null, onSaved: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val state = _uiState.value
            if (state.title.isBlank() && state.content.isBlank() && state.checkList.isEmpty() && state.imageUris.isEmpty() && state.audioUri == null) {
                return@launch
            }

            val checklistJson = if (state.checkList.isNotEmpty()) Json.encodeToString(state.checkList) else ""
            val imageUrisJson = if (state.imageUris.isNotEmpty()) Json.encodeToString(state.imageUris) else ""

            val note = Note(
                id = state.noteId,
                title = state.title,
                content = state.content,
                colorHex = state.colorHex,
                isPinned = state.isPinned,
                isArchived = state.isArchived,
                isDeleted = false,
                updatedAt = System.currentTimeMillis(),
                reminderTime = state.reminderTime,
                tags = state.tags,
                checkListJson = checklistJson,
                imageUrisJson = imageUrisJson,
                audioUri = state.audioUri,
                folder = state.folder,
                isLocked = state.isLocked
            )

            val savedId = if (state.noteId > 0) {
                repository.updateNote(note)
                state.noteId
            } else {
                repository.insertNote(note)
            }

            // Sync with system AlarmManager
            if (context != null) {
                val reminderTime = state.reminderTime
                if (reminderTime != null && reminderTime > System.currentTimeMillis()) {
                    ReminderScheduler.scheduleReminder(
                        context = context,
                        noteId = savedId,
                        title = state.title,
                        content = state.content,
                        triggerAtMillis = reminderTime
                    )
                } else if (reminderTime == null) {
                    ReminderScheduler.cancelReminder(context, savedId)
                }
                com.example.widget.NotesAppWidgetProvider.notifyDataChanged(context)
            }

            _uiState.update { it.copy(noteId = savedId, isSaved = true) }
            onSaved(savedId)
        }
    }

    fun moveToTrash(context: Context? = null, onCompleted: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.noteId
            if (id > 0) {
                val existing = repository.getNoteById(id)
                if (existing != null) {
                    repository.updateNote(existing.copy(isDeleted = true, updatedAt = System.currentTimeMillis()))
                }
                if (context != null) {
                    ReminderScheduler.cancelReminder(context, id)
                    com.example.widget.NotesAppWidgetProvider.notifyDataChanged(context)
                }
            }
            onCompleted()
        }
    }
}
