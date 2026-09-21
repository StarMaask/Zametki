package com.example.presentation.screens.settings

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.preferences.FontSizeScale
import com.example.data.preferences.UserPreferencesManager
import com.example.domain.repository.NoteRepository
import com.example.ui.theme.AppThemePreset
import com.example.util.BackupRestoreUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    preferencesManager: UserPreferencesManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    repository: NoteRepository? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentTheme by preferencesManager.themeFlow.collectAsState(initial = AppThemePreset.PURITY)
    val currentFontSize by preferencesManager.fontSizeFlow.collectAsState(initial = FontSizeScale.NORMAL)
    val isPinEnabled by preferencesManager.isPinEnabledFlow.collectAsState(initial = false)
    val currentPin by preferencesManager.pinCodeFlow.collectAsState(initial = "0000")

    var showPinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Внешний вид
            Text(
                text = "Внешний вид",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Text(
                            text = "Цветовая тема",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    AppThemePreset.entries.forEach { preset ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setTheme(preset)
                                    }
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = preset.title,
                                style = MaterialTheme.typography.bodyLarge
                            )
                            RadioButton(
                                selected = currentTheme == preset,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setTheme(preset)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Размер текста
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Размер шрифта",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "Масштаб текста в приложении",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    FontSizeScale.entries.forEach { scale ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setFontSize(scale)
                                    }
                                }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = scale.title,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            RadioButton(
                                selected = currentFontSize == scale,
                                onClick = {
                                    scope.launch(Dispatchers.IO) {
                                        preferencesManager.setFontSize(scale)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Безопасность и PIN-код
            Text(
                text = "Безопасность",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Column {
                                Text(
                                    text = "Защита PIN-кодом",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = if (isPinEnabled) "Включена" else "Отключена",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                        Switch(
                            checked = isPinEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch(Dispatchers.IO) {
                                    preferencesManager.setPinEnabled(enabled)
                                }
                            }
                        )
                    }

                    if (isPinEnabled) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                newPinInput = ""
                                pinError = false
                                showPinDialog = true
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Изменить PIN-код (сейчас: ****)")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Резервное копирование и восстановление
            if (repository != null) {
                Text(
                    text = "Резервное копирование",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(12.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.padding(start = 8.dp))
                            Text(
                                text = "Локальный бэкап заметок (JSON)",
                                style = MaterialTheme.typography.titleMedium
                            )
                        }

                        Text(
                            text = "Вы можете экспортировать все свои заметки в безопасный JSON файл или импортировать ранее сохраненную копию.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    scope.launch {
                                        val notes = withContext(Dispatchers.IO) {
                                            repository.getAllActiveNotesList()
                                        }
                                        if (notes.isEmpty()) {
                                            Toast.makeText(context, "Нет заметок для экспорта", Toast.LENGTH_SHORT).show()
                                        } else {
                                            val jsonString = BackupRestoreUtil.exportToJson(notes)
                                            shareJsonBackup(context, jsonString)
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Spacer(modifier = Modifier.padding(start = 6.dp))
                                Text("Экспорт")
                            }

                            Button(
                                onClick = {
                                    importJsonText = ""
                                    showImportDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = null)
                                Spacer(modifier = Modifier.padding(start = 6.dp))
                                Text("Импорт")
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // О приложении
            Text(
                text = "О приложении",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Заметки v1.0",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Локальное, приватное и быстрое хранилище заметок с Room FTS4, напоминаниями, фото и Material 3.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // Диалог смены PIN-кода
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Установка PIN-кода") },
            text = {
                Column {
                    Text(
                        text = "Введите 4 цифры для защиты ваших заметок:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                newPinInput = it
                                pinError = false
                            }
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        isError = pinError,
                        label = { Text("PIN-код (4 цифры)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (pinError) {
                        Text(
                            text = "PIN должен состоять ровно из 4 цифр",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (newPinInput.length == 4) {
                        scope.launch(Dispatchers.IO) {
                            preferencesManager.setPinCode(newPinInput)
                            preferencesManager.setPinEnabled(true)
                        }
                        Toast.makeText(context, "PIN-код успешно обновлен", Toast.LENGTH_SHORT).show()
                        showPinDialog = false
                    } else {
                        pinError = true
                    }
                }) {
                    Text("Сохранить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    // Диалог импорта JSON
    if (showImportDialog && repository != null) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Импорт заметок") },
            text = {
                Column {
                    Text(
                        text = "Вставьте JSON содержимое резервной копии ниже:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        label = { Text("JSON данные") },
                        maxLines = 10
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    if (importJsonText.isNotBlank()) {
                        scope.launch {
                            val importedNotes = BackupRestoreUtil.importFromJson(importJsonText)
                            if (importedNotes.isNotEmpty()) {
                                withContext(Dispatchers.IO) {
                                    repository.importNotes(importedNotes)
                                }
                                Toast.makeText(context, "Импортировано заметок: ${importedNotes.size}", Toast.LENGTH_SHORT).show()
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "Не удалось распознать формат JSON", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text("Импортировать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
}

private fun shareJsonBackup(context: Context, json: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_SUBJECT, "Notes_Backup.json")
        putExtra(Intent.EXTRA_TEXT, json)
    }
    context.startActivity(Intent.createChooser(intent, "Экспорт резервной копии"))
}
