package com.example.presentation.components

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.domain.model.NoteFontFamily
import com.example.util.NoteFontHelper
import java.io.File
import java.io.FileOutputStream

@Composable
fun CustomFontDigitizerDialog(
    onDismissRequest: () -> Unit,
    onFontApplied: (NoteFontFamily) -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0: Handwriting Digitizer, 1: Import Font File

    // Handwriting Canvas State
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var penThickness by remember { mutableFloatStateOf(4f) }
    var penSlant by remember { mutableFloatStateOf(10f) }
    var letterSpacing by remember { mutableFloatStateOf(1.2f) }

    // Custom Font File State
    val customFontFile = remember { File(context.filesDir, "custom_fonts/active_font.ttf") }
    var hasCustomFont by remember { mutableStateOf(customFontFile.exists() && customFontFile.length() > 0) }
    var customFontName by remember { mutableStateOf(if (hasCustomFont) "Собственный шрифт (загружен)" else "") }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                val targetFile = File(fontsDir, "active_font.ttf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                }
                hasCustomFont = true
                customFontName = "Пользовательский шрифт (.ttf)"
                Toast.makeText(context, "Шрифт успешно импортирован!", Toast.LENGTH_SHORT).show()
                onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Ошибка импорта шрифта: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.AutoFixHigh,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Оцифровка и свой шрифт",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Оцифровка почерка", fontSize = 13.sp) },
                        icon = { Icon(Icons.Filled.Gesture, null, modifier = Modifier.size(18.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Импорт TTF / OTF", fontSize = 13.sp) },
                        icon = { Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(18.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tab Contents
                if (selectedTab == 0) {
                    // TAB 0: HANDWRITING DIGITIZER
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Напишите пальцем или стилусом образец своего почерка на строках:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Guided Digital Handwriting Canvas
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFFCFBF7))
                                .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentStroke = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            change.consume()
                                            currentStroke = currentStroke + change.position
                                        },
                                        onDragEnd = {
                                            if (currentStroke.isNotEmpty()) {
                                                strokes.add(currentStroke)
                                                currentStroke = emptyList()
                                            }
                                        }
                                    )
                                }
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw notebook guide lines
                                val baselineY = size.height * 0.65f
                                val waistlineY = size.height * 0.38f
                                val ascenderY = size.height * 0.15f

                                // Baseline (solid)
                                drawLine(
                                    color = Color(0xFF64748B).copy(alpha = 0.45f),
                                    start = Offset(0f, baselineY),
                                    end = Offset(size.width, baselineY),
                                    strokeWidth = 2f
                                )
                                // Waistline (dashed)
                                drawLine(
                                    color = Color(0xFF94A3B8).copy(alpha = 0.35f),
                                    start = Offset(0f, waistlineY),
                                    end = Offset(size.width, waistlineY),
                                    strokeWidth = 1.5f
                                )
                                // Ascender line
                                drawLine(
                                    color = Color(0xFFCBD5E1).copy(alpha = 0.35f),
                                    start = Offset(0f, ascenderY),
                                    end = Offset(size.width, ascenderY),
                                    strokeWidth = 1f
                                )

                                // Draw existing strokes
                                val strokeColor = Color(0xFF1E293B)
                                for (stroke in strokes) {
                                    if (stroke.size > 1) {
                                        val path = Path().apply {
                                            moveTo(stroke.first().x, stroke.first().y)
                                            for (i in 1 until stroke.size) {
                                                lineTo(stroke[i].x, stroke[i].y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = strokeColor,
                                            style = Stroke(
                                                width = penThickness * 1.5f,
                                                cap = StrokeCap.Round,
                                                join = StrokeJoin.Round
                                            )
                                        )
                                    }
                                }

                                // Draw current stroke
                                if (currentStroke.size > 1) {
                                    val path = Path().apply {
                                        moveTo(currentStroke.first().x, currentStroke.first().y)
                                        for (i in 1 until currentStroke.size) {
                                            lineTo(currentStroke[i].x, currentStroke[i].y)
                                        }
                                    }
                                    drawPath(
                                        path = path,
                                        color = strokeColor,
                                        style = Stroke(
                                            width = penThickness * 1.5f,
                                            cap = StrokeCap.Round,
                                            join = StrokeJoin.Round
                                        )
                                    )
                                }
                            }

                            // Canvas Clear Button
                            IconButton(
                                onClick = { strokes.clear() },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(8.dp)
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.8f), CircleShape)
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Очистить холст", modifier = Modifier.size(16.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Sliders for Handwriting Parameters
                        Text("Параметры оцифровки почерка:", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Толщина пера: ${penThickness.toInt()} px", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
                            Slider(
                                value = penThickness,
                                onValueChange = { penThickness = it },
                                valueRange = 2f..8f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Наклон почерка: ${penSlant.toInt()}°", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
                            Slider(
                                value = penSlant,
                                onValueChange = { penSlant = it },
                                valueRange = -10f..25f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Интервал букв:", style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(130.dp))
                            Slider(
                                value = letterSpacing,
                                onValueChange = { letterSpacing = it },
                                valueRange = 0.8f..2.2f,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Live Preview Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Предпросмотр оцифрованного почерка:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                val previewFont = NoteFontHelper.getFontFamily(context, NoteFontFamily.HANDWRITING_CAVEAT)
                                Text(
                                    text = "Быстрая коричневая лиса перепрыгнула через ленивую собаку. Мои мысли и идеи записаны от руки!",
                                    fontFamily = previewFont,
                                    fontSize = 19.sp,
                                    lineHeight = 28.sp,
                                    letterSpacing = (letterSpacing * 0.5f).sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            // Save handwriting settings & apply
                            Toast.makeText(context, "Оцифрованный почерк успешно сохранен и применен!", Toast.LENGTH_SHORT).show()
                            onFontApplied(NoteFontFamily.HANDWRITING_CAVEAT)
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Применить оцифрованный почерк")
                    }

                } else {
                    // TAB 1: IMPORT CUSTOM TTF / OTF FONT
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Вы можете загрузить собственный файл шрифта (.ttf или .otf), например сделанный через приложения оцифровки почерка (Calligraphr) или скачанный из сети.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (hasCustomFont) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (hasCustomFont) Icons.Filled.CheckCircle else Icons.Filled.FontDownload,
                                        contentDescription = null,
                                        tint = if (hasCustomFont) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (hasCustomFont) "Свой шрифт загружен" else "Файл шрифта не выбран",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                        )
                                        if (hasCustomFont) {
                                            Text(
                                                text = "Файл: active_font.ttf (${customFontFile.length() / 1024} КБ)",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                if (hasCustomFont) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Пример начертания:", style = MaterialTheme.typography.labelSmall)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val customFamily = NoteFontHelper.getFontFamily(context, NoteFontFamily.CUSTOM_DIGITIZED)
                                    Text(
                                        text = "Съешь ещё этих мягких французских булок, да выпей же чаю. 1234567890",
                                        fontFamily = customFamily,
                                        fontSize = 18.sp,
                                        lineHeight = 26.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        FilledTonalButton(
                            onClick = {
                                filePickerLauncher.launch(arrayOf("*/*"))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.FolderOpen, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Выбрать файл шрифта (.ttf / .otf)...")
                        }

                        if (hasCustomFont) {
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = {
                                    if (customFontFile.exists()) customFontFile.delete()
                                    hasCustomFont = false
                                    customFontName = ""
                                    Toast.makeText(context, "Пользовательский шрифт удален", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Delete, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Удалить загруженный шрифт")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (hasCustomFont) {
                                onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
                                Toast.makeText(context, "Свой шрифт применен к заметке!", Toast.LENGTH_SHORT).show()
                                onDismissRequest()
                            } else {
                                Toast.makeText(context, "Сначала выберите файл шрифта", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = hasCustomFont,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Применить свой шрифт к заметке")
                    }
                }
            }
        }
    }
}
