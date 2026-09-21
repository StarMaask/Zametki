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
    val isListeningSpeech: Boolean = false,
    val isCheckedItemsList: Boolean = false,
    val checkListItems: List<com.example.domain.model.CheckListItem> = emptyList(),
    val imageUris: List<String> = emptyList(),
    val isLocked: Boolean = false,
    val isAiProcessing: Boolean = false
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
            val parsedList = if (note.isCheckedItemsList) {
                parseCheckList(note.content)
            } else emptyList()

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
                    isSaved = true,
                    isCheckedItemsList = note.isCheckedItemsList,
                    checkListItems = parsedList,
                    imageUris = note.imageUris,
                    isLocked = note.isLocked
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

    fun toggleCheckListMode() {
        val current = _uiState.value
        val newMode = !current.isCheckedItemsList
        if (newMode) {
            val items = parseCheckList(current.content)
            val finalItems = if (items.isEmpty()) listOf(com.example.domain.model.CheckListItem(text = "")) else items
            _uiState.update {
                it.copy(
                    isCheckedItemsList = true,
                    checkListItems = finalItems,
                    content = serializeCheckList(finalItems),
                    isSaved = false
                )
            }
        } else {
            val plainText = current.checkListItems.joinToString("\n") { it.text }
            _uiState.update {
                it.copy(
                    isCheckedItemsList = false,
                    content = plainText,
                    characterCount = plainText.length,
                    wordCount = calculateWords(plainText),
                    isSaved = false
                )
            }
        }
        scheduleAutoSave()
    }

    fun addCheckListItem() {
        val current = _uiState.value.checkListItems
        val updated = current + com.example.domain.model.CheckListItem(text = "")
        val content = serializeCheckList(updated)
        _uiState.update {
            it.copy(checkListItems = updated, content = content, isSaved = false)
        }
        scheduleAutoSave()
    }

    fun toggleCheckItem(index: Int, isChecked: Boolean) {
        val current = _uiState.value.checkListItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(isDone = isChecked)
            val content = serializeCheckList(current)
            _uiState.update {
                it.copy(checkListItems = current, content = content, isSaved = false)
            }
            scheduleAutoSave()
        }
    }

    fun updateCheckItemText(index: Int, text: String) {
        val current = _uiState.value.checkListItems.toMutableList()
        if (index in current.indices) {
            current[index] = current[index].copy(text = text)
            val content = serializeCheckList(current)
            _uiState.update {
                it.copy(checkListItems = current, content = content, isSaved = false)
            }
            scheduleAutoSave()
        }
    }

    fun removeCheckItem(index: Int) {
        val current = _uiState.value.checkListItems.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            val content = serializeCheckList(current)
            _uiState.update {
                it.copy(checkListItems = current, content = content, isSaved = false)
            }
            scheduleAutoSave()
        }
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
        val current = _uiState.value
        if (current.isCheckedItemsList) {
            val updated = current.checkListItems + com.example.domain.model.CheckListItem(text = recognizedText)
            val content = serializeCheckList(updated)
            _uiState.update { it.copy(checkListItems = updated, content = content, isSaved = false) }
        } else {
            val next = if (current.content.isBlank()) recognizedText else "${current.content} $recognizedText"
            onContentChange(next)
        }
    }

    fun moveToTrash(onComplete: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.id
            if (id > 0) {
                repository.moveToTrash(id)
            }
            onComplete()
        }
    }

    fun toggleArchive(onComplete: () -> Unit) {
        viewModelScope.launch {
            val id = _uiState.value.id
            val nextArchived = !_uiState.value.isArchived
            if (id > 0) {
                repository.toggleArchive(id, nextArchived)
            }
            _uiState.update { it.copy(isArchived = nextArchived) }
            onComplete()
        }
    }

    fun getShareText(): String {
        val s = _uiState.value
        val titlePart = if (s.title.isNotBlank()) "${s.title}\n\n" else ""
        val bodyPart = if (s.isCheckedItemsList) {
            s.checkListItems.joinToString("\n") { item ->
                val mark = if (item.isDone) "☑️ " else "⬜ "
                mark + item.text
            }
        } else {
            s.content
        }
        val tagsPart = if (s.tags.isNotEmpty()) "\n\n" + s.tags.joinToString(" ") { "#$it" } else ""
        return "$titlePart$bodyPart$tagsPart".trim()
    }

    private fun scheduleAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            delay(1500)
            saveNow()
        }
    }

    fun saveNow() {
        viewModelScope.launch {
            val s = _uiState.value
            val effectiveContent = if (s.isCheckedItemsList) {
                serializeCheckList(s.checkListItems)
            } else {
                s.content.trim()
            }

            if (s.title.isBlank() && effectiveContent.isBlank() && s.tags.isEmpty()) {
                _uiState.update { it.copy(isSaving = false, isSaved = true) }
                return@launch
            }

            val note = Note(
                id = s.id,
                title = s.title.trim(),
                content = effectiveContent,
                colorIndex = s.colorIndex,
                isPinned = s.isPinned,
                isArchived = s.isArchived,
                reminderTime = s.reminderTime,
                tags = s.tags,
                isCheckedItemsList = s.isCheckedItemsList,
                imageUris = s.imageUris,
                isLocked = s.isLocked,
                updatedAt = System.currentTimeMillis()
            )

            val savedId = repository.saveNote(note)
            _uiState.update {
                it.copy(
                    id = if (s.id == 0L) savedId else s.id,
                    content = effectiveContent,
                    isSaving = false,
                    isSaved = true
                )
            }
        }
    }

    fun addImageUri(uriString: String) {
        val current = _uiState.value.imageUris
        if (!current.contains(uriString)) {
            _uiState.update { it.copy(imageUris = current + uriString, isSaved = false) }
            scheduleAutoSave()
        }
    }

    fun removeImageUri(uriString: String) {
        val current = _uiState.value.imageUris
        _uiState.update { it.copy(imageUris = current - uriString, isSaved = false) }
        scheduleAutoSave()
    }

    fun toggleLock() {
        val next = !_uiState.value.isLocked
        _uiState.update { it.copy(isLocked = next, isSaved = false) }
        scheduleAutoSave()
    }

    fun generateAiSummary() {
        val s = _uiState.value
        val textToSummarize = if (s.isCheckedItemsList) {
            s.checkListItems.joinToString(", ") { it.text }
        } else {
            s.content
        }
        if (textToSummarize.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }
            delay(600) // имитация интеллектуального анализа текста
            val sentences = textToSummarize.split(".", "\n").map { it.trim() }.filter { it.length > 5 }
            val summary = if (sentences.size <= 2) {
                "📌 Кратко: ${textToSummarize.take(80)}..."
            } else {
                "📌 Главное:\n• " + sentences.take(3).joinToString("\n• ")
            }

            if (s.isCheckedItemsList) {
                val updatedItems = s.checkListItems + com.example.domain.model.CheckListItem(text = summary, isDone = false)
                _uiState.update {
                    it.copy(
                        checkListItems = updatedItems,
                        content = serializeCheckList(updatedItems),
                        isAiProcessing = false,
                        isSaved = false
                    )
                }
            } else {
                val nextContent = "${s.content.trim()}\n\n$summary".trim()
                onContentChange(nextContent)
                _uiState.update { it.copy(isAiProcessing = false) }
            }
            scheduleAutoSave()
        }
    }

    fun generateChecklistFromText() {
        val s = _uiState.value
        if (s.content.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isAiProcessing = true) }
            delay(500)
            val lines = s.content.lines().map { it.trim() }.filter { it.isNotBlank() }
            val newChecklist = lines.flatMap { line ->
                // Делим по запятым или точкам если это длинный список
                if (line.contains(",") && line.length > 30) {
                    line.split(",").map { it.trim().removePrefix("-").removePrefix("•").trim() }.filter { it.isNotBlank() }
                } else {
                    listOf(line.removePrefix("-").removePrefix("•").trim())
                }
            }.map { com.example.domain.model.CheckListItem(text = it, isDone = false) }

            val finalList = if (newChecklist.isEmpty()) listOf(com.example.domain.model.CheckListItem(text = "")) else newChecklist
            _uiState.update {
                it.copy(
                    isCheckedItemsList = true,
                    checkListItems = finalList,
                    content = serializeCheckList(finalList),
                    isAiProcessing = false,
                    isSaved = false
                )
            }
            scheduleAutoSave()
        }
    }

    private fun parseCheckList(content: String): List<com.example.domain.model.CheckListItem> {
        if (content.isBlank()) return listOf(com.example.domain.model.CheckListItem(text = ""))
        return content.lines().filter { it.isNotBlank() }.map { line ->
            when {
                line.startsWith("[x] ") -> com.example.domain.model.CheckListItem(text = line.removePrefix("[x] "), isDone = true)
                line.startsWith("[X] ") -> com.example.domain.model.CheckListItem(text = line.removePrefix("[X] "), isDone = true)
                line.startsWith("[ ] ") -> com.example.domain.model.CheckListItem(text = line.removePrefix("[ ] "), isDone = false)
                else -> com.example.domain.model.CheckListItem(text = line, isDone = false)
            }
        }.ifEmpty { listOf(com.example.domain.model.CheckListItem(text = "")) }
    }

    private fun serializeCheckList(items: List<com.example.domain.model.CheckListItem>): String {
        return items.joinToString("\n") { item ->
            val prefix = if (item.isDone) "[x] " else "[ ] "
            prefix + item.text
        }
    }

    private fun calculateWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split("\\s+".toRegex()).size
    }
}
