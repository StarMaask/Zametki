package com.example.presentation.screens.notes_list

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Note
import com.example.domain.model.NotesViewMode
import com.example.presentation.components.FilterBottomSheet
import com.example.presentation.components.NoteCard
import com.example.presentation.components.PulsingFab
import com.example.ui.theme.NoteTagColors
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesListScreen(
    viewModel: NotesListViewModel,
    onNoteClick: (Long) -> Unit,
    onCreateNoteClick: () -> Unit,
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onTrashClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showMenu by remember { mutableStateOf(false) }

    var showPinDialog by remember { mutableStateOf(false) }
    var pendingNoteId by remember { mutableStateOf<Long?>(null) }
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    // Диалоги для массовых операций
    var showBatchFolderDialog by remember { mutableStateOf(false) }
    var batchFolderInput by remember { mutableStateOf("") }
    var showBatchColorDialog by remember { mutableStateOf(false) }

    val isPinEnabled by (viewModel.preferencesManager?.isPinEnabledFlow ?: flowOf(false)).collectAsState(initial = false)
    val actualPin by (viewModel.preferencesManager?.pinCodeFlow ?: flowOf("0000")).collectAsState(initial = "0000")

    val onNoteCardClick = { note: Note ->
        if (state.isSelectionMode) {
            viewModel.toggleNoteSelection(note.id)
        } else if (note.isLocked && isPinEnabled) {
            pendingNoteId = note.id
            enteredPin = ""
            pinError = false
            showPinDialog = true
        } else {
            onNoteClick(note.id)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            if (state.isSelectionMode) {
                // TopAppBar в режиме множественного выбора
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { viewModel.clearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Отмена")
                        }
                    },
                    title = {
                        Text(
                            text = "${state.selectedNoteIds.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            if (state.selectedNoteIds.size == state.notes.size) {
                                viewModel.clearSelection()
                            } else {
                                viewModel.selectAll(state.notes.map { it.id })
                            }
                        }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Выбрать все")
                        }
                        IconButton(onClick = { showBatchFolderDialog = true }) {
                            Icon(Icons.Default.Folder, contentDescription = "Папка")
                        }
                        IconButton(onClick = { showBatchColorDialog = true }) {
                            Icon(Icons.Default.Palette, contentDescription = "Цвет")
                        }
                        IconButton(onClick = { viewModel.archiveSelected() }) {
                            Icon(Icons.Default.Archive, contentDescription = "В архив")
                        }
                        IconButton(onClick = { viewModel.deleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "Заметки",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${state.notes.size} заметок",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = onSearchClick) {
                            Icon(Icons.Default.Search, contentDescription = "Поиск")
                        }
                        IconButton(onClick = { viewModel.toggleViewMode() }) {
                            Icon(
                                imageVector = if (state.viewMode == NotesViewMode.STAGGERED_GRID) {
                                    Icons.Default.ViewAgenda
                                } else {
                                    Icons.Default.GridView
                                },
                                contentDescription = "Вид списка"
                            )
                        }
                        IconButton(onClick = { viewModel.setFilterSheetOpen(true) }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Фильтры")
                        }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = "Ещё")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Выбрать заметки") },
                                    leadingIcon = { Icon(Icons.Default.SelectAll, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        if (state.notes.isNotEmpty()) {
                                            viewModel.toggleNoteSelection(state.notes.first().id)
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Архив") },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onArchiveClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Корзина") },
                                    leadingIcon = { Icon(Icons.Default.DeleteOutline, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onTrashClick()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Настройки") },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                                    onClick = {
                                        showMenu = false
                                        onSettingsClick()
                                    }
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            }
        },
        floatingActionButton = {
            if (!state.isSelectionMode) {
                PulsingFab(onClick = onCreateNoteClick)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Горизонтальные фильтры по папкам (если есть папки)
            if (state.allFolders.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedFolder == null,
                        onClick = { viewModel.onFolderSelect(null) },
                        label = { Text("Все папки") }
                    )
                    state.allFolders.forEach { folder ->
                        FilterChip(
                            selected = state.selectedFolder == folder,
                            onClick = {
                                viewModel.onFolderSelect(
                                    if (state.selectedFolder == folder) null else folder
                                )
                            },
                            label = { Text("📁 $folder") }
                        )
                    }
                }
            }

            // Горизонтальные быстрые чипсы по тегам
            val allTags = remember(state.notes) {
                state.notes.flatMap { it.tags }.distinct()
            }

            if (allTags.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.selectedTagFilter == null,
                        onClick = { viewModel.onTagFilterSelect(null) },
                        label = { Text("Все теги") }
                    )
                    allTags.forEach { tag ->
                        FilterChip(
                            selected = state.selectedTagFilter == tag,
                            onClick = {
                                viewModel.onTagFilterSelect(
                                    if (state.selectedTagFilter == tag) null else tag
                                )
                            },
                            label = { Text("#$tag") }
                        )
                    }
                }
            }

            // Список заметок или пустое состояние
            if (state.notes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Здесь будут ваши заметки",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нажмите + чтобы записать первую мысль",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            } else {
                when (state.viewMode) {
                    NotesViewMode.STAGGERED_GRID -> {
                        LazyVerticalStaggeredGrid(
                            columns = StaggeredGridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalItemSpacing = 10.dp,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.notes, key = { it.id }) { note ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { 40 }
                                ) {
                                    NoteCard(
                                        note = note,
                                        onClick = { onNoteCardClick(note) },
                                        onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                        isSelected = state.selectedNoteIds.contains(note.id),
                                        isSelectionMode = state.isSelectionMode
                                    )
                                }
                            }
                        }
                    }
                    NotesViewMode.LINEAR_LIST, NotesViewMode.COMPACT -> {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(state.notes, key = { it.id }) { note ->
                                NoteCard(
                                    note = note,
                                    onClick = { onNoteCardClick(note) },
                                    onLongClick = { viewModel.toggleNoteSelection(note.id) },
                                    isSelected = state.selectedNoteIds.contains(note.id),
                                    isSelectionMode = state.isSelectionMode
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Диалог ввода PIN-кода для защищенных заметок
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pendingNoteId = null
            },
            title = { Text("Заметка защищена") },
            text = {
                Column {
                    Text(
                        text = "Введите PIN-код для открытия заметки:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = enteredPin,
                        onValueChange = {
                            enteredPin = it.take(6)
                            pinError = false
                        },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        isError = pinError,
                        label = { Text("PIN-код") },
                        supportingText = if (pinError) {
                            { Text("Неверный PIN-код", color = MaterialTheme.colorScheme.error) }
                        } else null,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (enteredPin == actualPin) {
                            val id = pendingNoteId
                            showPinDialog = false
                            pendingNoteId = null
                            if (id != null) {
                                onNoteClick(id)
                            }
                        } else {
                            pinError = true
                        }
                    }
                ) {
                    Text("Открыть")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPinDialog = false
                        pendingNoteId = null
                    }
                ) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог выбора цвета для выделенных заметок
    if (showBatchColorDialog) {
        AlertDialog(
            onDismissRequest = { showBatchColorDialog = false },
            title = { Text("Выбрать цвет") },
            text = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteTagColors.list.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable {
                                    viewModel.changeColorSelected(index)
                                    showBatchColorDialog = false
                                }
                        )
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBatchColorDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог перемещения в папку для выделенных заметок
    if (showBatchFolderDialog) {
        AlertDialog(
            onDismissRequest = { showBatchFolderDialog = false },
            title = { Text("Переместить в папку") },
            text = {
                Column {
                    OutlinedTextField(
                        value = batchFolderInput,
                        onValueChange = { batchFolderInput = it },
                        label = { Text("Название папки") },
                        placeholder = { Text("например, Работа или Личное") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (state.allFolders.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("Существующие папки:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            state.allFolders.forEach { f ->
                                FilterChip(
                                    selected = batchFolderInput == f,
                                    onClick = { batchFolderInput = f },
                                    label = { Text(f) }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.moveSelectedToFolder(batchFolderInput.trim())
                        showBatchFolderDialog = false
                        batchFolderInput = ""
                    }
                ) {
                    Text("Переместить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchFolderDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Нижняя шторка фильтров
    if (state.isFilterSheetOpen) {
        FilterBottomSheet(
            filterState = state.filterState,
            folders = state.allFolders,
            onFilterChange = { newFilters ->
                viewModel.updateFilters(newFilters)
            },
            onDismiss = {
                viewModel.setFilterSheetOpen(false)
            }
        )
    }
}
