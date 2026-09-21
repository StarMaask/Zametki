package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.example.presentation.screens.archive.ArchiveViewModel
import com.example.presentation.screens.editor.NoteEditorScreen
import com.example.presentation.screens.editor.NoteEditorViewModel
import com.example.presentation.screens.notes_list.NotesListScreen
import com.example.presentation.screens.notes_list.NotesListViewModel
import com.example.presentation.screens.search.SearchScreen
import com.example.presentation.screens.search.SearchViewModel
import com.example.presentation.screens.settings.SettingsScreen
import com.example.presentation.screens.trash.TrashScreen
import com.example.presentation.screens.trash.TrashViewModel
import com.example.ui.theme.AppThemePreset
import com.example.ui.theme.NoteAppTheme
import com.example.widget.NotesAppWidgetProvider
import java.util.Locale

class MainActivity : ComponentActivity() {

    private lateinit var database: NoteDatabase
    private lateinit var repository: NoteRepositoryImpl
    private lateinit var preferencesManager: UserPreferencesManager
    private var speechRecognizer: SpeechRecognizer? = null
    private var activeEditorViewModel: NoteEditorViewModel? = null

    private val speechPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSpeechInput()
        } else {
            Toast.makeText(this, "Требуется доступ к микрофону для голосового ввода", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        database = NoteDatabase.getInstance(this)
        repository = NoteRepositoryImpl(database.noteDao())
        preferencesManager = UserPreferencesManager(this)

        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        }

        setContent {
            val currentTheme by preferencesManager.themeFlow.collectAsState(initial = AppThemePreset.PURITY)
            val currentFontSize by preferencesManager.fontSizeFlow.collectAsState(initial = FontSizeScale.NORMAL)

            val currentDensity = LocalDensity.current
            val adjustedDensity = Density(
                density = currentDensity.density,
                fontScale = currentDensity.fontScale * currentFontSize.scaleMultiplier
            )

            CompositionLocalProvider(LocalDensity provides adjustedDensity) {
                NoteAppTheme(themePreset = currentTheme) {
                    Crossfade(targetState = currentTheme, label = "theme_crossfade") { _ ->
                        AppNavigation()
                    }
                }
            }
        }
    }

    @Composable
    private fun AppNavigation() {
        val navController = rememberNavController()

        val widgetAction = intent?.getStringExtra(NotesAppWidgetProvider.EXTRA_ACTION)
        val widgetNoteId = intent?.getLongExtra(NotesAppWidgetProvider.EXTRA_NOTE_ID, 0L) ?: 0L

        androidx.compose.runtime.LaunchedEffect(widgetAction, widgetNoteId) {
            if (widgetAction == NotesAppWidgetProvider.ACTION_NEW_NOTE) {
                intent?.removeExtra(NotesAppWidgetProvider.EXTRA_ACTION)
                navController.navigate(Screen.NoteEditor.createRoute(0L))
            } else if (widgetAction == NotesAppWidgetProvider.ACTION_OPEN_NOTE && widgetNoteId > 0L) {
                intent?.removeExtra(NotesAppWidgetProvider.EXTRA_ACTION)
                intent?.removeExtra(NotesAppWidgetProvider.EXTRA_NOTE_ID)
                navController.navigate(Screen.NoteEditor.createRoute(widgetNoteId))
            }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.NotesList.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.NotesList.route) {
                val listViewModel: NotesListViewModel = viewModel {
                    NotesListViewModel(repository, preferencesManager)
                }

                NotesListScreen(
                    viewModel = listViewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onCreateNoteClick = {
                        navController.navigate(Screen.NoteEditor.createRoute(0L))
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search.route)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    },
                    onArchiveClick = {
                        navController.navigate(Screen.Archive.route)
                    },
                    onTrashClick = {
                        navController.navigate(Screen.Trash.route)
                    }
                )
            }

            composable(
                route = Screen.NoteEditor.route,
                arguments = listOf(navArgument("noteId") { type = NavType.LongType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId") ?: 0L
                val editorViewModel: NoteEditorViewModel = viewModel {
                    NoteEditorViewModel(repository, noteId)
                }
                activeEditorViewModel = editorViewModel

                NoteEditorScreen(
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() },
                    onSpeechClick = { checkAndStartSpeech() }
                )
            }

            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = viewModel {
                    SearchViewModel(repository)
                }

                SearchScreen(
                    viewModel = searchViewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Archive.route) {
                val archiveViewModel: ArchiveViewModel = viewModel {
                    ArchiveViewModel(repository)
                }

                ArchiveScreen(
                    viewModel = archiveViewModel,
                    onNoteClick = { noteId ->
                        navController.navigate(Screen.NoteEditor.createRoute(noteId))
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Trash.route) {
                val trashViewModel: TrashViewModel = viewModel {
                    TrashViewModel(repository)
                }

                TrashScreen(
                    viewModel = trashViewModel,
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

    private fun checkAndStartSpeech() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSpeechInput()
        } else {
            speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startSpeechInput() {
        if (speechRecognizer == null) {
            Toast.makeText(this, "Голосовой ввод недоступен на этом устройстве", Toast.LENGTH_SHORT).show()
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale("ru").toString())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Диктуйте заметку...")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) {
                Toast.makeText(this@MainActivity, "Ошибка распознавания речи", Toast.LENGTH_SHORT).show()
            }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    activeEditorViewModel?.appendSpeechText(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
        Toast.makeText(this, "Слушаю...", Toast.LENGTH_SHORT).show()
    }

    override fun onStop() {
        super.onStop()
        NotesAppWidgetProvider.notifyDataChanged(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}
