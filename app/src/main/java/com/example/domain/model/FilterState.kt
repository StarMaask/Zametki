package com.example.domain.model

/**
 * Модель фильтра и сортировки для списка заметок
 */
enum class NoteSortOrder(val title: String) {
    BY_UPDATED("По дате изменения"),
    BY_CREATED("По дате создания"),
    BY_TITLE("По названию"),
    BY_COLOR("По цвету")
}

enum class NotesViewMode {
    STAGGERED_GRID,
    LINEAR_LIST,
    COMPACT
}

enum class DateFilter(val title: String) {
    ALL("Все"),
    TODAY("Сегодня"),
    WEEK("Эта неделя"),
    MONTH("Этот месяц")
}

data class FilterState(
    val selectedTags: Set<String> = emptySet(),
    val selectedColors: Set<Int> = emptySet(),
    val dateFilter: DateFilter = DateFilter.ALL,
    val onlyWithReminder: Boolean = false,
    val onlyPinned: Boolean = false,
    val sortOrder: NoteSortOrder = NoteSortOrder.BY_UPDATED
)
