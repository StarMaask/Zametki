package com.example.presentation.screens.editor

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.example.domain.model.NoteTemplate
import com.example.presentation.components.AudioPlaybackCard
import com.example.presentation.components.AudioRecordDialog
import com.example.presentation.components.DrawingCanvasDialog
import com.example.presentation.components.NoteInfoDialog
import com.example.presentation.components.NoteTemplateDialog
import com.example.presentation.components.TooltipIconButton
import com.example.ui.theme.NoteColors
import com.example.util.ShareExportUtil
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    viewModel: NoteEditorViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showColorPicker by remember { mutableStateOf(false) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var showDrawingDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showFolderDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var showFormatMenu by remember { mutableStateOf(false) }
    var showOrganizeMenu by remember { mutableStateOf(false) }
    var showTopMenu by remember { mutableStateOf(false) }
    var showTemplateDialog by remember { mutableStateOf(false) }

    var newTagInput by remember { mutableStateOf("") }
    var folderInput by remember { mutableStateOf("") }
    var checkItemInput by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.addImage(it.toString()) }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showAudioDialog = true
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(state.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.background
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.noteId == 0L) "Новая заметка" else "Заметка",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TooltipIconButton(
                        onClick = {
                            viewModel.saveNote(context)
                            onBack()
                        },
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        tooltip = "Сохранить и вернуться",
                        contentDescription = "Назад"
                    )
                },
                actions = {
                    // Pin Button
                    TooltipIconButton(
                        onClick = { viewModel.togglePin() },
                        icon = if (state.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        tooltip = if (state.isPinned) "Открепить заметку" else "Закрепить вверху списка",
                        tint = if (state.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    // Reminder Button
                    TooltipIconButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            val calendar = Calendar.getInstance()
                            DatePickerDialog(context, { _, year, month, dayOfMonth ->
                                TimePickerDialog(context, { _, hourOfDay, minute ->
                                    val reminderCal = Calendar.getInstance().apply {
                                        set(year, month, dayOfMonth, hourOfDay, minute, 0)
                                    }
                                    viewModel.onReminderChange(reminderCal.timeInMillis)
                                }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        icon = if (state.reminderTime != null) Icons.Filled.NotificationsActive else Icons.Filled.NotificationsNone,
                        tooltip = if (state.reminderTime != null) "Изменить напоминание" else "Установить напоминание",
                        tint = if (state.reminderTime != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )

                    // Overflow Menu
                    Box {
                        TooltipIconButton(
                            onClick = { showTopMenu = true },
                            icon = Icons.Filled.MoreVert,
                            tooltip = "Дополнительные действия"
                        )
                        DropdownMenu(
                            expanded = showTopMenu,
                            onDismissRequest = { showTopMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Информация о заметке")
                                        Text("Статистика и дата изменения", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Info, null) },
                                onClick = {
                                    showTopMenu = false
                                    showInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Поделиться текстом")
                                        Text("Отправить текст через мессенджеры", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Share, null) },
                                onClick = {
                                    showTopMenu = false
                                    ShareExportUtil.shareAsText(context, state.toDomainNote())
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Применить шаблон")
                                        Text("Заполнить структуру (план, покупки, встреча)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.AutoAwesome, null) },
                                onClick = {
                                    showTopMenu = false
                                    showTemplateDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Экспорт в PDF")
                                        Text("Создать документ с заголовком и датой", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.PictureAsPdf, null) },
                                onClick = {
                                    showTopMenu = false
                                    ShareExportUtil.shareAsPdf(context, state.toDomainNote())
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Экспорт в TXT")
                                        Text("Сохранить как текстовый файл", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Description, null) },
                                onClick = {
                                    showTopMenu = false
                                    ShareExportUtil.shareAsTxtFile(context, state.toDomainNote())
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(if (state.isLocked) "Снять защиту PIN-кодом" else "Защитить PIN-кодом")
                                        Text(if (state.isLocked) "Заметка будет доступна без ввода PIN" else "Скрывать содержимое до ввода PIN", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(if (state.isLocked) Icons.Filled.LockOpen else Icons.Filled.Lock, null) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.toggleLock()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text(if (state.isArchived) "Из архива" else "В архив")
                                        Text(if (state.isArchived) "Вернуть в общий список" else "Скрыть с главного экрана", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Archive, null) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.toggleArchive { onBack() }
                                }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Удалить в корзину", color = MaterialTheme.colorScheme.error)
                                        Text("Можно восстановить позже", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                                onClick = {
                                    showTopMenu = false
                                    viewModel.moveToTrash(context) { onBack() }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = backgroundColor)
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. ВСТАВКА (Insert Dropdown Menu)
                    Box {
                        TooltipIconButton(
                            onClick = { showInsertMenu = true },
                            icon = Icons.Filled.AddCircleOutline,
                            tooltip = "Вставить медиа или список",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        DropdownMenu(
                            expanded = showInsertMenu,
                            onDismissRequest = { showInsertMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Чек-лист задач")
                                        Text("Создать список дел с галочками", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.CheckBox, null) },
                                onClick = {
                                    showInsertMenu = false
                                    viewModel.toggleChecklistMode()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Фото / Изображение")
                                        Text("Прикрепить картинку из галереи", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Image, null) },
                                onClick = {
                                    showInsertMenu = false
                                    imagePickerLauncher.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Рисунок от руки")
                                        Text("Холст для эскизов и схем", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Brush, null) },
                                onClick = {
                                    showInsertMenu = false
                                    showDrawingDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Голосовая запись")
                                        Text("Записать аудио на диктофон", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Mic, null) },
                                onClick = {
                                    showInsertMenu = false
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                        showAudioDialog = true
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            )
                        }
                    }

                    // 2. ФОРМАТИРОВАНИЕ (Formatting Dropdown Menu)
                    Box {
                        TooltipIconButton(
                            onClick = { showFormatMenu = true },
                            icon = Icons.Filled.TextFormat,
                            tooltip = "Форматирование текста",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenu(
                            expanded = showFormatMenu,
                            onDismissRequest = { showFormatMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Жирный текст (**)")
                                        Text("Выделение важного", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatBold, null) },
                                onClick = {
                                    showFormatMenu = false
                                    viewModel.appendFormatting("**", "**")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Курсивный текст (*)")
                                        Text("Наклонный шрифт", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatItalic, null) },
                                onClick = {
                                    showFormatMenu = false
                                    viewModel.appendFormatting("*", "*")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Заголовок (# )")
                                        Text("Крупный заголовок раздела", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Title, null) },
                                onClick = {
                                    showFormatMenu = false
                                    viewModel.appendFormatting("# ")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Маркированный список (• )")
                                        Text("Элемент перечисления", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.FormatListBulleted, null) },
                                onClick = {
                                    showFormatMenu = false
                                    viewModel.appendFormatting("• ")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Цитата (> )")
                                        Text("Блок цитирования", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.FormatQuote, null) },
                                onClick = {
                                    showFormatMenu = false
                                    viewModel.appendFormatting("> ")
                                }
                            )
                        }
                    }

                    // 3. ПАЛИТРА ЦВЕТОВ (Color Row Toggle)
                    TooltipIconButton(
                        onClick = { showColorPicker = !showColorPicker },
                        icon = Icons.Filled.Palette,
                        tooltip = "Цвет фона заметки",
                        tint = if (showColorPicker) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // 4. ОРГАНИЗАЦИЯ (Organize Dropdown Menu: Folder & Tags)
                    Box {
                        TooltipIconButton(
                            onClick = { showOrganizeMenu = true },
                            icon = Icons.Filled.FolderOpen,
                            tooltip = "Папка и теги",
                            tint = if (state.folder != null || state.tags.isNotEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenu(
                            expanded = showOrganizeMenu,
                            onDismissRequest = { showOrganizeMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Папка")
                                        Text(if (state.folder != null) "Текущая: ${state.folder}" else "Назначить папку", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Folder, null) },
                                onClick = {
                                    showOrganizeMenu = false
                                    folderInput = state.folder ?: ""
                                    showFolderDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Column {
                                        Text("Добавить тег")
                                        Text("Теги для быстрой фильтрации", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Label, null) },
                                onClick = {
                                    showOrganizeMenu = false
                                    showTagDialog = true
                                }
                            )
                        }
                    }
                }
            }
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Color Selector Bar if toggled
            if (showColorPicker) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "Выберите пастельный оттенок заметки:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(NoteColors) { hex ->
                                val color = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.LightGray }
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .clickable { viewModel.onColorChange(hex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (state.colorHex.equals(hex, ignoreCase = true)) {
                                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(10.dp)) {}
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Folder, Reminder, Tags Chip Row
            if (!state.folder.isNullOrBlank() || state.reminderTime != null || state.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (!state.folder.isNullOrBlank()) {
                        SuggestionChip(
                            onClick = { folderInput = state.folder ?: ""; showFolderDialog = true },
                            label = { Text(state.folder!!) },
                            icon = { Icon(Icons.Filled.Folder, contentDescription = "Папка", modifier = Modifier.size(14.dp)) }
                        )
                    }

                    if (state.reminderTime != null) {
                        val reminderStr = SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(state.reminderTime!!))
                        InputChip(
                            selected = true,
                            onClick = { viewModel.onReminderChange(null) },
                            label = { Text(reminderStr) },
                            leadingIcon = { Icon(Icons.Filled.NotificationsActive, contentDescription = "Напоминание", modifier = Modifier.size(14.dp)) },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Отключить напоминание", modifier = Modifier.size(12.dp)) }
                        )
                    }

                    state.tags.forEach { tag ->
                        InputChip(
                            selected = false,
                            onClick = { viewModel.removeTag(tag) },
                            label = { Text("#$tag") },
                            trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Удалить тег", modifier = Modifier.size(12.dp)) }
                        )
                    }
                }
            }

            // Attached Images Row
            if (state.imageUris.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    items(state.imageUris) { uri ->
                        Box(modifier = Modifier.size(130.dp).clip(RoundedCornerShape(12.dp))) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Прикрепленное изображение",
                                modifier = Modifier.fillMaxSize()
                            )
                            IconButton(
                                onClick = { viewModel.removeImage(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = "Удалить фото", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // Attached Audio Card
            if (!state.audioUri.isNullOrBlank()) {
                AudioPlaybackCard(
                    audioUri = state.audioUri!!,
                    onDelete = { viewModel.onAudioUriChange(null) }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Title Field
            TextField(
                value = state.title,
                onValueChange = { viewModel.onTitleChange(it) },
                placeholder = { Text("Заголовок заметки", fontSize = 22.sp, fontWeight = FontWeight.Bold) },
                textStyle = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Checklist mode or Regular content field
            if (state.isChecklistMode) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val completedCount = state.checkList.count { it.isChecked }
                            val totalCount = state.checkList.size
                            Column {
                                Text(
                                    text = "Список дел ($completedCount/$totalCount)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = if (state.sortCompletedToEnd) "Выполненные переносятся вниз" else "Без автосортировки",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TooltipIconButton(
                                    onClick = { viewModel.toggleSortCompletedToEnd() },
                                    icon = Icons.Filled.SwapVert,
                                    tooltip = if (state.sortCompletedToEnd) "Отключить сортировку завершённых вниз" else "Переносить выполненные в конец",
                                    tint = if (state.sortCompletedToEnd) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                TextButton(onClick = { viewModel.toggleChecklistMode() }) {
                                    Text("В текст")
                                }
                            }
                        }

                        state.checkList.forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = item.isChecked,
                                    onCheckedChange = { viewModel.toggleChecklistItem(item.id) }
                                )
                                Text(
                                    text = item.text,
                                    modifier = Modifier.weight(1f).padding(start = 6.dp),
                                    style = if (item.isChecked) {
                                        MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                            textDecoration = TextDecoration.LineThrough
                                        )
                                    } else {
                                        MaterialTheme.typography.bodyLarge
                                    }
                                )
                                TooltipIconButton(
                                    onClick = { viewModel.removeChecklistItem(item.id) },
                                    icon = Icons.Filled.Close,
                                    tooltip = "Удалить пункт списка",
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = checkItemInput,
                                onValueChange = { checkItemInput = it },
                                placeholder = { Text("Новый пункт списка...") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (checkItemInput.isNotBlank()) {
                                        viewModel.addChecklistItem(checkItemInput.trim())
                                        checkItemInput = ""
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = "Добавить пункт")
                            }
                        }
                    }
                }
            } else {
                TextField(
                    value = state.content,
                    onValueChange = { viewModel.onContentChange(it) },
                    placeholder = { Text("Текст заметки...") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 250.dp)
                )
            }
        }
    }

    // Dialogs
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            title = { Text("Добавить тег") },
            text = {
                Column {
                    Text(
                        text = "Теги начинаются со знака # и помогают сортировать похожие заметки.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Например: работа, рецепты") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addTag(newTagInput)
                    newTagInput = ""
                    showTagDialog = false
                }) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showFolderDialog) {
        AlertDialog(
            onDismissRequest = { showFolderDialog = false },
            title = { Text("Папка заметки") },
            text = {
                Column {
                    Text(
                        text = "Введите название новой папки или выберите из существующих:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = folderInput,
                        onValueChange = { folderInput = it },
                        placeholder = { Text("Имя папки") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (state.availableFolders.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Существующие папки:", style = MaterialTheme.typography.labelSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(state.availableFolders) { f ->
                                AssistChip(
                                    onClick = { folderInput = f },
                                    label = { Text(f) }
                                )
                            }
                        }
                    }
                    if (!state.folder.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        TextButton(onClick = {
                            viewModel.onFolderChange(null)
                            showFolderDialog = false
                        }) {
                            Text("Убрать из папки", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.onFolderChange(if (folderInput.isBlank()) null else folderInput.trim())
                    showFolderDialog = false
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFolderDialog = false }) { Text("Отмена") }
            }
        )
    }

    if (showInfoDialog) {
        val completedCount = state.checkList.count { it.isChecked }
        NoteInfoDialog(
            wordCount = state.wordCount,
            charCount = state.charCount,
            createdAt = state.createdAt,
            updatedAt = state.updatedAt,
            completedChecklistItems = completedCount,
            totalChecklistItems = state.checkList.size,
            onDismiss = { showInfoDialog = false }
        )
    }

    if (showAudioDialog) {
        AudioRecordDialog(
            onDismiss = { showAudioDialog = false },
            onRecordingFinished = { path ->
                viewModel.onAudioUriChange(path)
                showAudioDialog = false
            }
        )
    }

    if (showDrawingDialog) {
        DrawingCanvasDialog(
            onDismiss = { showDrawingDialog = false },
            onSaveDrawing = { path ->
                viewModel.addImage(path)
                showDrawingDialog = false
            }
        )
    }

    if (showTemplateDialog) {
        NoteTemplateDialog(
            onDismissRequest = { showTemplateDialog = false },
            onTemplateSelect = { template ->
                showTemplateDialog = false
                viewModel.applyTemplate(template)
            }
        )
    }
}
