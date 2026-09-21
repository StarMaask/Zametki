package com.example.presentation.navigation

/**
 * Направления навигации в приложении
 */
sealed class Screen(val route: String) {
    data object NotesList : Screen("notes_list")
    data object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long) = "note_editor/$noteId"
    }
    data object Search : Screen("search")
    data object Reminders : Screen("reminders")
    data object Archive : Screen("archive")
    data object Trash : Screen("trash")
    data object Tags : Screen("tags")
    data object Settings : Screen("settings")
    data object Onboarding : Screen("onboarding")
}
