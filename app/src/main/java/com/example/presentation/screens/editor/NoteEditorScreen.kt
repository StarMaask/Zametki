package com.example.presentation.screens.editor

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.presentation.components.AudioNotePlayer
import com.example.presentation.components.AudioRecordDialog
import com.example.presentation.components.DrawingCanvasDialog
import com.example.presentation.components.FullscreenImageViewerDialog
import com.example.ui.theme.NoteTagColors
import com.example.util.ImageStorageUtil
import com.example.util.ReminderScheduler
import com.example.util.ShareExportUtil
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit,
    onSpeechClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()

    var showTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }
    var showFolderDialog by remember { mutableStateOf(false) }
    var folderInput by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showAiMenu by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    var showDrawingDialog by remember { mutableStateOf(false) }
    var showAudioRecordDialog by remember { mutableStateOf(false) }
    var selectedFullscreenImage by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val savedPath = ImageStorageUtil.saveImageToInternalStorage(context, uri)
            if (savedPath != null) {
                viewModel.addImageUri(savedPath)
            }
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAudioRecordDialog = true
        } else {
            Toast.makeText(context, "Для записи аудио требуется разрешение микрофона", Toast.LENGTH_SHORT).show()
        }
    }

    // Извлечение ссылок из текста
    val detectedUrls = remember(state.content) {
        val urlRegex = Regex("""(https?://[^\s]+|www\.[^\s]+)""", RegexOption.IGNORE_CASE)
        urlRegex.findAll(state.content).map { it.value }.distinct().take(5).toList()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (state.isSaving) "Сохранение..." else if (state.isAiProcessing) "ИИ думает..." else "Сохранено",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.saveNow()
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка блокировки PIN-кодом
                    IconButton(onClick = { viewModel.toggleLock() }) {
                        Icon(
                            imageVector = if (state.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Защита заметки",
                            tint = if (state.isLocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    // Кнопка напоминания
                    IconButton(onClick = { showReminderDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = "Напоминание",
                            tint = if (state.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    // Кнопка закрепить
                    IconButton(onClick = { viewModel.togglePin() }) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = "Закрепить",
                            tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    // Поделиться и Экспорт меню
                    Box {
                        IconButton(onClick = { showShareMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Поделиться / Экспорт")
                        }
                        DropdownMenu(
                            expanded = showShareMenu,
                            onDismissRequest = { showShareMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Отправить текст") },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    ShareExportUtil.shareAsText(context, viewModel.getCurrentNote())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспорт в PDF документ") },
                                leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    ShareExportUtil.shareAsPdf(context, viewModel.getCurrentNote())
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Экспорт в файл .TXT") },
                                leadingIcon = { Icon(Icons.Default.TextSnippet, contentDescription = null) },
                                onClick = {
                                    showShareMenu = false
                                    ShareExportUtil.shareAsTxtFile(context, viewModel.getCurrentNote())
                                }
                            )
                        }
                    }
                    // Меню три точки
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Дополнительно")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(if (state.folder.isNotBlank()) "Папка: ${state.folder}" else "Выбрать папку") },
                                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) },
                                onClick = {
                                    showMenu = false
                                    folderInput = state.folder
                                    showFolderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(if (state.isArchived) "Вернуть из архива" else "В архив") },
                                leadingIcon = {
                                    Icon(
                                        if (state.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.toggleArchive {
                                        onBack()
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("В корзину", color = MaterialTheme.colorScheme.error) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                },
                                onClick = {
                                    showMenu = false
                                    viewModel.moveToTrash {
                                        onBack()
                                    }
                                }
                            )
                        }
                    }
                    // Сохранить
                    IconButton(onClick = { viewModel.saveNow() }) {
                        Icon(Icons.Default.Check, contentDescription = "Готово")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        // Переключатель Чек-лист / Текст
                        IconButton(onClick = { viewModel.toggleCheckListMode() }) {
                            Icon(
                                imageVector = if (state.isCheckedItemsList) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                                contentDescription = "Список задач",
                                tint = if (state.isCheckedItemsList) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!state.isCheckedItemsList) {
                            IconButton(onClick = {
                                viewModel.onContentChange(state.content + "\n• ")
                            }) {
                                Icon(Icons.Default.FormatListBulleted, contentDescription = "Список")
                            }
                            IconButton(onClick = {
                                viewModel.onContentChange(state.content + " **жирный** ")
                            }) {
                                Icon(Icons.Default.FormatBold, contentDescription = "Жирный")
                            }
                            IconButton(onClick = {
                                viewModel.onContentChange(state.content + " *курсив* ")
                            }) {
                                Icon(Icons.Default.FormatItalic, contentDescription = "Курсив")
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        // Рисование от руки
                        IconButton(onClick = { showDrawingDialog = true }) {
                            Icon(
                                Icons.Default.Brush,
                                contentDescription = "Рисование",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Прикрепить фото
                        IconButton(onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }) {
                            Icon(
                                Icons.Default.AddPhotoAlternate,
                                contentDescription = "Прикрепить фото",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Голосовая запись аудио / диктофон
                        IconButton(onClick = {
                            val permission = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                showAudioRecordDialog = true
                            } else {
                                audioPermissionLauncher.launch(permission)
                            }
                        }) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = "Записать аудио",
                                tint = if (state.audioUri != null) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                            )
                        }

                        // ИИ помощник
                        Box {
                            IconButton(onClick = { showAiMenu = true }) {
                                if (state.isAiProcessing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = "Умный помощник ИИ",
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }
                            DropdownMenu(
                                expanded = showAiMenu,
                                onDismissRequest = { showAiMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("✨ Краткая выжимка (TL;DR)") },
                                    onClick = {
                                        showAiMenu = false
                                        viewModel.generateAiSummary()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("✨ Превратить в To-Do чек-лист") },
                                    onClick = {
                                        showAiMenu = false
                                        viewModel.generateChecklistFromText()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            // Напоминание бейдж (если установлено)
            if (state.reminderTime != null) {
                val formattedReminder = SimpleDateFormat("dd MMM, HH:mm", Locale("ru")).format(Date(state.reminderTime!!))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Напоминание: $formattedReminder",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Удалить напоминание",
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    viewModel.setReminder(null)
                                    if (state.id > 0) {
                                        ReminderScheduler.cancelReminder(context, state.id)
                                    }
                                },
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            // Папка и Цветовая палитра
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Чип папки
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f))
                        .clickable {
                            folderInput = state.folder
                            showFolderDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        Icons.Default.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (state.folder.isNotBlank()) state.folder else "Без папки",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Палитра цветовых меток
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NoteTagColors.list.forEachIndexed { index, color ->
                        val isSelected = state.colorIndex == index
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { viewModel.onColorChange(index) }
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Встроенный аудиоплеер (если прикреплена аудиозапись)
            if (state.audioUri != null) {
                AudioNotePlayer(
                    audioPath = state.audioUri!!,
                    onDeleteAudio = { viewModel.setAudioUri(null) },
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            // Поле ввода заголовка
            BasicTextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChange(it) },
                textStyle = MaterialTheme.typography.displaySmall.copy(
                    color = MaterialTheme.colorScheme.onBackground
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (state.title.isEmpty()) {
                        Text(
                            text = "Заголовок",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                        )
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Чипсы тегов
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                state.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { viewModel.removeTag(tag) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "#$tag",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Удалить тег",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Кнопка добавить тег
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .clickable { showTagDialog = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+ тег",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Галерея прикрепленных фото и рисунков
            if (state.imageUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(state.imageUris) { imgPath ->
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                                .clickable { selectedFullscreenImage = imgPath }
                        ) {
                            AsyncImage(
                                model = File(imgPath),
                                contentDescription = "Прикрепленное фото",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            IconButton(
                                onClick = { viewModel.removeImageUri(imgPath) },
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(2.dp)
                                    .align(Alignment.TopEnd)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить фото",
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Интеллектуальное обнаружение ссылок (Link Detection)
            if (detectedUrls.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    detectedUrls.forEach { url ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .clickable {
                                    val safeUrl = if (!url.startsWith("http://") && !url.startsWith("https://")) "https://$url" else url
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(safeUrl)))
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.OpenInBrowser,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = url.take(30) + if (url.length > 30) "..." else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Тело заметки: либо To-Do чек-лист, либо свободный текст
            if (state.isCheckedItemsList) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.checkListItems.forEachIndexed { index, item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Checkbox(
                                checked = item.isDone,
                                onCheckedChange = { isChecked ->
                                    viewModel.toggleCheckItem(index, isChecked)
                                }
                            )
                            BasicTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    viewModel.updateCheckItemText(index, newText)
                                },
                                textStyle = MaterialTheme.typography.bodyLarge.copy(
                                    color = if (item.isDone) {
                                        MaterialTheme.colorScheme.outline
                                    } else {
                                        MaterialTheme.colorScheme.onBackground
                                    },
                                    textDecoration = if (item.isDone) TextDecoration.LineThrough else null
                                ),
                                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 4.dp),
                                decorationBox = { inner ->
                                    if (item.text.isEmpty()) {
                                        Text(
                                            text = "Пункт задачи...",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    }
                                    inner()
                                }
                            )
                            IconButton(
                                onClick = { viewModel.removeCheckItem(index) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Удалить пункт",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { viewModel.addCheckListItem() }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Добавить пункт")
                    }
                }
            } else {
                BasicTextField(
                    value = state.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (state.content.isEmpty()) {
                            Text(
                                text = "Начните писать или надиктуйте голосом...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                            )
                        }
                        innerTextField()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(380.dp)
                )
            }
        }
    }

    // Диалог папки
    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Папка заметки") },
            text = {
                Column {
                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { folderInput = it },
                        label = { Text("Название папки") },
                        placeholder = { Text("например, Работа, Идеи, Учёба") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    if (state.folder.isNotBlank()) {
                        TextButton(
                            onClick = {
                                viewModel.setFolder("")
                                showFolderDialog = false
                            }
                        ) {
                            Text("Убрать из папки", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.setFolder(folderInput.trim())
                    showFolderDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог скетча / рисования
    if (showDrawingDialog) {
        DrawingCanvasDialog(
            onDismiss = { showDrawingDialog = false },
            onSaveDrawing = { savedDrawingPath ->
                viewModel.addImageUri(savedDrawingPath)
            }
        )
    }

    // Диалог записи аудио
    if (showAudioRecordDialog) {
        AudioRecordDialog(
            onDismiss = { showAudioRecordDialog = false },
            onAudioRecorded = { audioPath ->
                viewModel.setAudioUri(audioPath)
            }
        )
    }

    // Полноэкранный просмотрщик изображений
    if (selectedFullscreenImage != null) {
        FullscreenImageViewerDialog(
            imageUri = selectedFullscreenImage!!,
            onDismiss = { selectedFullscreenImage = null },
            onDeleteImage = {
                viewModel.removeImageUri(selectedFullscreenImage!!)
                selectedFullscreenImage = null
            }
        )
    }

    // Диалог добавления тега
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Новый тег") },
            text = {
                OutlinedTextField(
                    value = newTagInput,
                    onValueChange = { newTagInput = it },
                    placeholder = { Text("Название тега") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newTagInput.isNotBlank()) {
                        viewModel.addTag(newTagInput)
                        newTagInput = ""
                        showTagDialog = false
                    }
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог установки напоминания
    if (showReminderDialog) {
        AlertDialog(
            onDismissRequest = { showReminderDialog = false },
            title = { Text("Напоминание") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val triggerSchedule = { timeMs: Long ->
                        viewModel.setReminder(timeMs)
                        val noteId = if (state.id > 0) state.id else System.currentTimeMillis()
                        ReminderScheduler.scheduleReminder(
                            context = context,
                            noteId = noteId,
                            title = state.title.ifBlank { "Напоминание" },
                            content = if (state.isCheckedItemsList) state.checkListItems.joinToString(", ") { it.text } else state.content,
                            triggerAtMillis = timeMs
                        )
                        showReminderDialog = false
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                set(Calendar.HOUR_OF_DAY, 18)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            triggerSchedule(cal.timeInMillis)
                        }
                    ) {
                        Text("Сегодня вечером (18:00)", modifier = Modifier.fillMaxWidth())
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            triggerSchedule(cal.timeInMillis)
                        }
                    ) {
                        Text("Завтра утром (09:00)", modifier = Modifier.fillMaxWidth())
                    }

                    TextButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            val cal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 14)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                            }
                            triggerSchedule(cal.timeInMillis)
                        }
                    ) {
                        Text("Завтра днем (14:00)", modifier = Modifier.fillMaxWidth())
                    }

                    if (state.reminderTime != null) {
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                viewModel.setReminder(null)
                                if (state.id > 0) {
                                    ReminderScheduler.cancelReminder(context, state.id)
                                }
                                showReminderDialog = false
                            }
                        ) {
                            Text("Удалить напоминание", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showReminderDialog = false }) {
                    Text("Закрыть")
                }
            }
        )
    }
}
