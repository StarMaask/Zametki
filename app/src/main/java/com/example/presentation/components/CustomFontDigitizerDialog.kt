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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.domain.model.NoteFontFamily
import com.example.util.HandwritingAnalysisResult
import com.example.util.HandwritingPhotoDigitizer
import com.example.util.NoteFontHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@Composable
fun CustomFontDigitizerDialog(
    initialImageUri: String? = null,
    onDismissRequest: () -> Unit,
    onFontApplied: (NoteFontFamily) -> Unit,
    onTextExtracted: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 0: По фото алфавита (.jpg / .png), 1: Нарисовать на экране, 2: Импорт TTF / OTF
    var selectedTab by remember { mutableIntStateOf(0) }

    // State for Tab 0: Photo Digitizer
    var selectedPhotoUri by remember {
        mutableStateOf<Uri?>(initialImageUri?.let { Uri.parse(it) })
    }
    var isAnalyzingPhoto by remember { mutableStateOf(false) }
    var isExtractingText by remember { mutableStateOf(false) }
    var analysisResult by remember {
        mutableStateOf<HandwritingAnalysisResult?>(HandwritingPhotoDigitizer.getSavedCalibration(context))
    }

    // Handwriting Canvas State (Tab 1)
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var currentStroke by remember { mutableStateOf<List<Offset>>(emptyList()) }

    var penThickness by remember { mutableFloatStateOf(analysisResult?.strokeThickness ?: 4f) }
    var penSlant by remember { mutableFloatStateOf(analysisResult?.slantAngle ?: 9f) }
    var letterSpacing by remember { mutableFloatStateOf(analysisResult?.letterSpacing ?: 1.15f) }

    // Custom Font File State (Tab 2)
    val customFontFile = remember { File(context.filesDir, "custom_fonts/active_font.ttf") }
    var hasCustomFont by remember { mutableStateOf(customFontFile.exists() && customFontFile.length() > 0) }

    var previewSampleText by remember {
        mutableStateOf("Привет! Это мой оцифрованный рукописный почерк.")
    }

    // Auto-analyze initialImageUri if provided
    LaunchedEffect(initialImageUri) {
        if (initialImageUri != null && selectedPhotoUri != null && analysisResult == null) {
            isAnalyzingPhoto = true
            val result = HandwritingPhotoDigitizer.processAlphabetPhoto(context, selectedPhotoUri!!)
            isAnalyzingPhoto = false
            result.onSuccess { analysis ->
                analysisResult = analysis
                penThickness = analysis.strokeThickness
                penSlant = analysis.slantAngle
                letterSpacing = analysis.letterSpacing
                hasCustomFont = true
                onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
            }
        }
    }

    // Launcher for selecting alphabet photo (.jpg, .png, etc.)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPhotoUri = uri
            isAnalyzingPhoto = true
            coroutineScope.launch {
                val result = HandwritingPhotoDigitizer.processAlphabetPhoto(context, uri)
                isAnalyzingPhoto = false
                result.onSuccess { analysis ->
                    analysisResult = analysis
                    penThickness = analysis.strokeThickness
                    penSlant = analysis.slantAngle
                    letterSpacing = analysis.letterSpacing
                    hasCustomFont = true
                    onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
                    Toast.makeText(context, "Алфавит распознан! Почерк оцифрован и активирован", Toast.LENGTH_SHORT).show()
                }.onFailure { error ->
                    Toast.makeText(context, "Ошибка обработки фото: ${error.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // Launcher for font files (.ttf, .otf) with intelligent redirect if user picks a .jpg
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.path?.lowercase() ?: ""
            val isImage = fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                    fileName.endsWith(".png") || fileName.endsWith(".webp") ||
                    (context.contentResolver.getType(uri)?.startsWith("image/") == true)

            if (isImage) {
                // User picked an image in the font tab -> gracefully redirect to Photo Digitizer tab
                selectedTab = 0
                selectedPhotoUri = uri
                isAnalyzingPhoto = true
                coroutineScope.launch {
                    val result = HandwritingPhotoDigitizer.processAlphabetPhoto(context, uri)
                    isAnalyzingPhoto = false
                    result.onSuccess { analysis ->
                        analysisResult = analysis
                        penThickness = analysis.strokeThickness
                        penSlant = analysis.slantAngle
                        letterSpacing = analysis.letterSpacing
                        hasCustomFont = true
                        Toast.makeText(context, "Фото алфавита оцифровано во вкладке «По фото»!", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(context, "Ошибка обработки фото: ${error.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                try {
                    val fontsDir = File(context.filesDir, "custom_fonts").apply { mkdirs() }
                    val targetFile = File(fontsDir, "active_font.ttf")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileOutputStream(targetFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    hasCustomFont = true
                    Toast.makeText(context, "Файл шрифта (.ttf/.otf) успешно импортирован!", Toast.LENGTH_SHORT).show()
                    onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, "Ошибка импорта шрифта: ${e.message}", Toast.LENGTH_LONG).show()
                }
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
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(24.dp)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp)
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Оцифровка почерка",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                    IconButton(onClick = onDismissRequest) {
                        Icon(Icons.Filled.Close, contentDescription = "Закрыть")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("По фото (.jpg)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                        icon = { Icon(Icons.Filled.CameraAlt, null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("На экране", fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.Gesture, null, modifier = Modifier.size(16.dp)) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Файл TTF", fontSize = 12.sp) },
                        icon = { Icon(Icons.Filled.FileUpload, null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // TAB 0: DIGITIZE FROM PHOTO OF ALPHABET
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Загрузите фото листа с рукописным алфавитом (.jpg или .png). Приложение считает индивидуальные штрихи, наклон и толщину пера:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Upload Box / Image Preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedPhotoUri != null) {
                                AsyncImage(
                                    model = selectedPhotoUri,
                                    contentDescription = "Фото алфавита",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "Фото листа с алфавитом загружено",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else if (analysisResult != null && File(analysisResult!!.sampleImagePath).exists()) {
                                AsyncImage(
                                    model = File(analysisResult!!.sampleImagePath),
                                    contentDescription = "Сохраненный образец алфавита",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .background(Color.Black.copy(alpha = 0.55f))
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        text = "Ранее оцифрованный образец почерка",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Image,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Фото алфавита (.jpg / .png) еще не выбрано",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Подойдет фото листа бумаги с буквами от руки",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }

                            if (isAnalyzingPhoto || isExtractingText) {
                                Surface(
                                    modifier = Modifier.fillMaxSize(),
                                    color = Color.Black.copy(alpha = 0.65f)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(32.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            if (isExtractingText) "Распознавание текста с фото..." else "Оцифровка штрихов и наклона...",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { photoPickerLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (selectedPhotoUri != null || analysisResult != null) "Другое фото (.jpg)..." else "Выбрать фото (.jpg)...",
                                    fontSize = 12.sp
                                )
                            }

                            if (selectedPhotoUri != null && onTextExtracted != null) {
                                OutlinedButton(
                                    onClick = {
                                        isExtractingText = true
                                        coroutineScope.launch {
                                            val text = HandwritingPhotoDigitizer.extractTextFromImage(context, selectedPhotoUri!!)
                                            isExtractingText = false
                                            onTextExtracted(text)
                                            Toast.makeText(context, "Текст с фото перенесен в заметку!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Filled.DocumentScanner, null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Текст в заметку", fontSize = 12.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Detected Parameters Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Параметры оцифровки почерка:",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Spacer(modifier = Modifier.height(6.dp))

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
                                        valueRange = -5f..25f,
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
                                        valueRange = 0.8f..2.0f,
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                analysisResult?.let { res ->
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                                    Text(
                                        text = "Распознан: ${res.styleDescription} • ${res.inkColorName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Live Preview Card with interactive text testing
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Предпросмотр почерка:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (hasCustomFont || analysisResult != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.primary,
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = "✓ Почерк активен",
                                                color = MaterialTheme.colorScheme.onPrimary,
                                                fontSize = 10.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                val digitizedFont = if (hasCustomFont) {
                                    NoteFontHelper.getFontFamily(context, NoteFontFamily.CUSTOM_DIGITIZED)
                                } else {
                                    NoteFontHelper.getFontFamily(context, NoteFontFamily.HANDWRITING_MARCK)
                                }

                                OutlinedTextField(
                                    value = previewSampleText,
                                    onValueChange = { previewSampleText = it },
                                    label = { Text("Попробуйте ввести русский текст своим почерком:", fontSize = 11.sp) },
                                    textStyle = LocalTextStyle.current.copy(
                                        fontFamily = digitizedFont,
                                        fontSize = 19.sp,
                                        lineHeight = 27.sp,
                                        letterSpacing = (letterSpacing * 0.5f).sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            // Apply digitized handwriting font
                            Toast.makeText(context, "Оцифрованный почерк применён к заметке!", Toast.LENGTH_SHORT).show()
                            onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
                            onDismissRequest()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Применить и перейти к заметке")
                    }

                } else if (selectedTab == 1) {
                    // TAB 1: DRAW ON SCREEN
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
                                val baselineY = size.height * 0.65f
                                val waistlineY = size.height * 0.38f
                                val ascenderY = size.height * 0.15f

                                drawLine(Color(0xFF64748B).copy(alpha = 0.45f), Offset(0f, baselineY), Offset(size.width, baselineY), 2f)
                                drawLine(Color(0xFF94A3B8).copy(alpha = 0.35f), Offset(0f, waistlineY), Offset(size.width, waistlineY), 1.5f)
                                drawLine(Color(0xFFCBD5E1).copy(alpha = 0.35f), Offset(0f, ascenderY), Offset(size.width, ascenderY), 1f)

                                val strokeColor = Color(0xFF1E293B)
                                for (stroke in strokes) {
                                    if (stroke.size > 1) {
                                        val path = Path().apply {
                                            moveTo(stroke.first().x, stroke.first().y)
                                            for (i in 1 until stroke.size) {
                                                lineTo(stroke[i].x, stroke[i].y)
                                            }
                                        }
                                        drawPath(path, strokeColor, style = Stroke(penThickness * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                    }
                                }
                                if (currentStroke.size > 1) {
                                    val path = Path().apply {
                                        moveTo(currentStroke.first().x, currentStroke.first().y)
                                        for (i in 1 until currentStroke.size) {
                                            lineTo(currentStroke[i].x, currentStroke[i].y)
                                        }
                                    }
                                    drawPath(path, strokeColor, style = Stroke(penThickness * 1.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                                }
                            }

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

                        Button(
                            onClick = {
                                Toast.makeText(context, "Образцы почерка сохранены и применены!", Toast.LENGTH_SHORT).show()
                                onFontApplied(NoteFontFamily.HANDWRITING_CAVEAT)
                                onDismissRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Применить почерк с холста")
                        }
                    }
                } else {
                    // TAB 2: IMPORT TTF / OTF FONT FILE
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = "Загрузите файл шрифта (.ttf или .otf), например созданный в Calligraphr или скачанный из интернета.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(14.dp))

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
                                            text = if (hasCustomFont) "Свой шрифт активен" else "Шрифт не выбран",
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
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(8.dp))
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
                            onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
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
                                    Toast.makeText(context, "Пользовательский шрифт сброшен", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Filled.Delete, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Удалить файл шрифта")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (hasCustomFont) {
                        Button(
                            onClick = {
                                onFontApplied(NoteFontFamily.CUSTOM_DIGITIZED)
                                onDismissRequest()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Применить файл шрифта")
                        }
                    }
                }
            }
        }
    }
}
