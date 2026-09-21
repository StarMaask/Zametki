package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.NoteDatabase
import com.example.data.preferences.FontSizeScale
import com.example.data.preferences.UserPreferencesManager
import com.example.data.repository.NoteRepositoryImpl
import com.example.presentation.navigation.Screen
import com.example.presentation.screens.archive.ArchiveScreen
import com.example.presentation.screens.editor.NoteEditorScreen
import com.example.presentation.screens.editor.NoteEditorViewModel
import com.example.presentation.screens.notes_list.NotesListScreen
import com.example.presentation.screens.notes_list.NotesListViewModel
import com.example.presentation.screens.search.SearchScreen
import com.example.presentation.screens.settings.SettingsScreen
import com.example.presentation.screens.trash.TrashScreen
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.NotesTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = NoteDatabase.getInstance(applicationContext)
        val repository = NoteRepositoryImpl(database.noteDao())
        val preferencesManager = UserPreferencesManager(applicationContext)

        setContent {
            val themePreset by preferencesManager.themeFlow.collectAsState(initial = AppThemePreset.PURITY)
            val fontSizeScale by preferencesManager.fontSizeFlow.collectAsState(initial = FontSizeScale.NORMAL)
            val isPinEnabled by preferencesManager.isPinEnabledFlow.collectAsState(initial = false)
            val savedPinCode by preferencesManager.pinCodeFlow.collectAsState(initial = "0000")

            var isUnlocked by remember { mutableStateOf(!isPinEnabled) }
            var pinInput by remember { mutableStateOf("") }

            LaunchedEffect(isPinEnabled) {
                if (!isPinEnabled) {
                    isUnlocked = true
                }
            }

            val currentDensity = LocalDensity.current
            val adjustedDensity = remember(currentDensity, fontSizeScale) {
                Density(
                    density = currentDensity.density,
                    fontScale = currentDensity.fontScale * fontSizeScale.scaleMultiplier
                )
            }

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                NotesTheme(preset = themePreset) {
                    if (isPinEnabled && !isUnlocked) {
                        Surface(modifier = Modifier.fillMaxSize()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "Введите PIN-код",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                OutlinedTextField(
                                    value = pinInput,
                                    onValueChange = {
                                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                            pinInput = it
                                            if (it == savedPinCode) {
                                                isUnlocked = true
                                            }
                                        }
                                    },
                                    placeholder = { Text("4 цифры") },
                                    singleLine = true
                                )
                            }
                        }
                    } else {
                        val navController = rememberNavController()

                        LaunchedEffect(Unit) {
                            val openNoteId = intent.getLongExtra("open_note_id", 0L)
                            val widgetNoteId = intent.getLongExtra(com.example.widget.NotesAppWidgetProvider.EXTRA_NOTE_ID, 0L)
                            val widgetAction = intent.getStringExtra(com.example.widget.NotesAppWidgetProvider.EXTRA_ACTION)

                            if (openNoteId > 0L) {
                                navController.navigate(Screen.NoteEditor.createRoute(openNoteId))
                            } else if (widgetNoteId > 0L) {
                                navController.navigate(Screen.NoteEditor.createRoute(widgetNoteId))
                            } else if (widgetAction == com.example.widget.NotesAppWidgetProvider.ACTION_NEW_NOTE) {
                                navController.navigate(Screen.NoteEditor.createRoute(0L))
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = Screen.NotesList.route
                        ) {
                            composable(Screen.NotesList.route) {
                                val viewModel = remember { NotesListViewModel(repository) }
                                NotesListScreen(
                                    viewModel = viewModel,
                                    onNoteClick = { noteId ->
                                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                                    },
                                    onSearchClick = { navController.navigate(Screen.Search.route) },
                                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                    onArchiveClick = { navController.navigate(Screen.Archive.route) },
                                    onTrashClick = { navController.navigate(Screen.Trash.route) }
                                )
                            }

                            composable(
                                route = Screen.NoteEditor.route,
                                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
                            ) { backStackEntry ->
                                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                                val viewModel = remember(noteId) { NoteEditorViewModel(repository, noteId) }
                                NoteEditorScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Search.route) {
                                SearchScreen(
                                    repository = repository,
                                    onNoteClick = { noteId ->
                                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Archive.route) {
                                ArchiveScreen(
                                    repository = repository,
                                    onNoteClick = { noteId ->
                                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                                    },
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Trash.route) {
                                TrashScreen(
                                    repository = repository,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    preferencesManager = preferencesManager,
                                    repository = repository,
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
