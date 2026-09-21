package com.example.presentation.navigation

sealed class Screen(val route: String) {
    object NotesList : Screen("notes_list")
    object NoteEditor : Screen("note_editor/{noteId}") {
        fun createRoute(noteId: Long): String = "note_editor/$noteId"
    }
    object Search : Screen("search")
    object Archive : Screen("archive")
    object Trash : Screen("trash")
    object Settings : Screen("settings")
}
