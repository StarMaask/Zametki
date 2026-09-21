package com.example.presentation.screens.notes_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.model.DateFilter
import com.example.domain.model.FilterState
import com.example.domain.model.Note
import com.example.domain.model.NoteSortOrder
import com.example.domain.model.NotesViewMode
import com.example.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotesListUiState(
    val notes: List<Note> = emptyList(),
    val filterState: FilterState = FilterState(),
    val viewMode: NotesViewMode = NotesViewMode.STAGGERED_GRID,
    val selectedTagFilter: String? = null,
    val selectedFolder: String? = null,
    val allFolders: List<String> = emptyList(),
    val selectedNoteIds: Set<Long> = emptySet(),
    val isFilterSheetOpen: Boolean = false,
    val lastDeletedNote: Note? = null
) {
    val isSelectionMode: Boolean get() = selectedNoteIds.isNotEmpty()
}

class NotesListViewModel(
    private val repository: NoteRepository,
    val preferencesManager: UserPreferencesManager? = null
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private val _viewMode = MutableStateFlow(NotesViewMode.STAGGERED_GRID)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _selectedFolder = MutableStateFlow<String?>(null)
    private val _selectedNoteIds = MutableStateFlow<Set<Long>>(emptySet())
    private val _isFilterSheetOpen = MutableStateFlow(false)
    private val _lastDeletedNote = MutableStateFlow<Note?>(null)

    val uiState: StateFlow<NotesListUiState> = combine(
        repository.getActiveNotes(),
        _filterState,
        _viewMode,
        _selectedTag,
        _selectedFolder,
        _selectedNoteIds,
        _isFilterSheetOpen
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val notes = args[0] as List<Note>
        val filters = args[1] as FilterState
        val viewMode = args[2] as NotesViewMode
        val tag = args[3] as String?
        val folder = args[4] as String?
        @Suppress("UNCHECKED_CAST")
        val selectedIds = args[5] as Set<Long>
        val sheetOpen = args[6] as Boolean

        val folders = notes.map { it.folder.trim() }.filter { it.isNotBlank() }.distinct().sorted()

        val filtered = notes.filter { note ->
            val matchesFolder = if (folder != null) {
                if (folder == "NO_FOLDER") note.folder.isBlank() else note.folder == folder
            } else true
            val matchesTag = if (tag != null) note.tags.contains(tag) else true
            val matchesSelectedTags = if (filters.selectedTags.isNotEmpty()) {
                filters.selectedTags.any { note.tags.contains(it) }
            } else true
            val matchesColors = if (filters.selectedColors.isNotEmpty()) {
                filters.selectedColors.contains(note.colorIndex)
            } else true
            val matchesReminder = if (filters.onlyWithReminder) {
                note.reminderTime != null
            } else true
            val matchesPinned = if (filters.onlyPinned) {
                note.isPinned
            } else true

            val matchesDate = when (filters.dateFilter) {
                DateFilter.ALL -> true
                DateFilter.TODAY -> {
                    val startOfDay = System.currentTimeMillis() - (System.currentTimeMillis() % 86400000L)
                    note.updatedAt >= startOfDay
                }
                DateFilter.WEEK -> {
                    val weekAgo = System.currentTimeMillis() - 7 * 86400000L
                    note.updatedAt >= weekAgo
                }
                DateFilter.MONTH -> {
                    val monthAgo = System.currentTimeMillis() - 30L * 86400000L
                    note.updatedAt >= monthAgo
                }
            }

            matchesFolder && matchesTag && matchesSelectedTags && matchesColors && matchesReminder && matchesPinned && matchesDate
        }.sortedWith { n1, n2 ->
            // Сначала закрепленные (если сортировка не по дате в обратном порядке)
            if (n1.isPinned != n2.isPinned) {
                if (n1.isPinned) -1 else 1
            } else {
                when (filters.sortOrder) {
                    NoteSortOrder.BY_UPDATED -> n2.updatedAt.compareTo(n1.updatedAt)
                    NoteSortOrder.BY_UPDATED_ASC -> n1.updatedAt.compareTo(n2.updatedAt)
                    NoteSortOrder.BY_CREATED -> n2.createdAt.compareTo(n1.createdAt)
                    NoteSortOrder.BY_TITLE_ASC -> n1.title.compareTo(n2.title, ignoreCase = true)
                    NoteSortOrder.BY_TITLE_DESC -> n2.title.compareTo(n1.title, ignoreCase = true)
                    NoteSortOrder.BY_COLOR -> n1.colorIndex.compareTo(n2.colorIndex)
                }
            }
        }

        NotesListUiState(
            notes = filtered,
            filterState = filters,
            viewMode = viewMode,
            selectedTagFilter = tag,
            selectedFolder = folder,
            allFolders = folders,
            selectedNoteIds = selectedIds,
            isFilterSheetOpen = sheetOpen,
            lastDeletedNote = _lastDeletedNote.value
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NotesListUiState()
    )

    fun onTagFilterSelect(tag: String?) {
        _selectedTag.value = tag
    }

    fun onFolderSelect(folder: String?) {
        _selectedFolder.value = folder
    }

    fun toggleViewMode() {
        _viewMode.value = when (_viewMode.value) {
            NotesViewMode.STAGGERED_GRID -> NotesViewMode.LINEAR_LIST
            NotesViewMode.LINEAR_LIST -> NotesViewMode.COMPACT
            NotesViewMode.COMPACT -> NotesViewMode.STAGGERED_GRID
        }
    }

    fun setFilterSheetOpen(isOpen: Boolean) {
        _isFilterSheetOpen.value = isOpen
    }

    fun updateFilters(newFilters: FilterState) {
        _filterState.value = newFilters
    }

    fun setSortOrder(sortOrder: NoteSortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = sortOrder)
    }

    // Множественный выбор (Multi-selection)
    fun toggleNoteSelection(noteId: Long) {
        val current = _selectedNoteIds.value.toMutableSet()
        if (current.contains(noteId)) {
            current.remove(noteId)
        } else {
            current.add(noteId)
        }
        _selectedNoteIds.value = current
    }

    fun selectAll(noteIds: List<Long>) {
        _selectedNoteIds.value = noteIds.toSet()
    }

    fun clearSelection() {
        _selectedNoteIds.value = emptySet()
    }

    fun deleteSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.batchMoveToTrash(ids)
                clearSelection()
            }
        }
    }

    fun archiveSelected() {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.batchArchive(ids, true)
                clearSelection()
            }
        }
    }

    fun pinSelected(pin: Boolean) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.batchTogglePin(ids, pin)
                clearSelection()
            }
        }
    }

    fun changeColorSelected(colorIndex: Int) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.batchChangeColor(ids, colorIndex)
                clearSelection()
            }
        }
    }

    fun moveSelectedToFolder(folder: String) {
        val ids = _selectedNoteIds.value.toList()
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.batchMoveToFolder(ids, folder)
                clearSelection()
            }
        }
    }

    // Одиночные операции
    fun deleteNote(note: Note) {
        viewModelScope.launch {
            _lastDeletedNote.value = note
            repository.moveToTrash(note.id)
        }
    }

    fun undoDelete() {
        val noteToRestore = _lastDeletedNote.value ?: return
        viewModelScope.launch {
            repository.restoreFromTrash(noteToRestore.id)
            _lastDeletedNote.value = null
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.togglePin(note.id, !note.isPinned)
        }
    }

    fun toggleArchive(note: Note) {
        viewModelScope.launch {
            repository.toggleArchive(note.id, true)
        }
    }
}
