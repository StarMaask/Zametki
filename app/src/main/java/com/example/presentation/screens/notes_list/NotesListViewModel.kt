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
    val isFilterSheetOpen: Boolean = false,
    val lastDeletedNote: Note? = null
)

class NotesListViewModel(
    private val repository: NoteRepository,
    val preferencesManager: UserPreferencesManager? = null
) : ViewModel() {

    private val _filterState = MutableStateFlow(FilterState())
    private val _viewMode = MutableStateFlow(NotesViewMode.STAGGERED_GRID)
    private val _selectedTag = MutableStateFlow<String?>(null)
    private val _isFilterSheetOpen = MutableStateFlow(false)
    private val _lastDeletedNote = MutableStateFlow<Note?>(null)

    val uiState: StateFlow<NotesListUiState> = combine(
        repository.getActiveNotes(),
        _filterState,
        _viewMode,
        _selectedTag,
        _isFilterSheetOpen
    ) { notes, filters, viewMode, tag, sheetOpen ->
        val filtered = notes.filter { note ->
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

            matchesTag && matchesSelectedTags && matchesColors && matchesReminder && matchesPinned && matchesDate
        }.sortedWith { n1, n2 ->
            // Сначала закрепленные
            if (n1.isPinned != n2.isPinned) {
                if (n1.isPinned) -1 else 1
            } else {
                when (filters.sortOrder) {
                    NoteSortOrder.BY_UPDATED -> n2.updatedAt.compareTo(n1.updatedAt)
                    NoteSortOrder.BY_CREATED -> n2.createdAt.compareTo(n1.createdAt)
                    NoteSortOrder.BY_TITLE -> n1.title.compareTo(n2.title, ignoreCase = true)
                    NoteSortOrder.BY_COLOR -> n1.colorIndex.compareTo(n2.colorIndex)
                }
            }
        }

        NotesListUiState(
            notes = filtered,
            filterState = filters,
            viewMode = viewMode,
            selectedTagFilter = tag,
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
