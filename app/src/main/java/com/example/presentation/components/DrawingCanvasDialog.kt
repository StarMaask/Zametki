package com.example.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.io.File
import java.io.FileOutputStream

@Composable
fun DrawingCanvasDialog(
    onDismiss: () -> Unit,
    onSaveDrawing: (String) -> Unit
) {
    val context = LocalContext.current
    val lines = remember { mutableStateListOf<List<Offset>>() }
    var currentLine by remember { mutableStateOf<List<Offset>>(emptyList()) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    TextButton(onClick = { lines.clear(); currentLine = emptyList() }) {
                        Text("Очистить")
                    }
                    Button(onClick = {
                        val bitmap = Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(android.graphics.Color.WHITE)
                        val paint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            strokeWidth = 8f
                            style = Paint.Style.STROKE
                            isAntiAlias = true
                            strokeCap = Paint.Cap.ROUND
                            strokeJoin = Paint.Join.ROUND
                        }

                        val allLines = lines + listOf(currentLine)
                        for (line in allLines) {
                            if (line.size < 2) continue
                            val path = Path().apply {
                                moveTo(line[0].x, line[0].y)
                                for (i in 1 until line.size) {
                                    lineTo(line[i].x, line[i].y)
                                }
                            }
                            canvas.drawPath(path, paint)
                        }

                        val file = File(context.cacheDir, "drawing_${System.currentTimeMillis()}.png")
                        FileOutputStream(file).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
                        }
                        onSaveDrawing(file.absolutePath)
                    }) {
                        Text("Сохранить")
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White)
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    currentLine = listOf(offset)
                                },
                                onDragEnd = {
                                    lines.add(currentLine)
                                    currentLine = emptyList()
                                },
                                onDrag = { change, _ ->
                                    currentLine = currentLine + change.position
                                }
                            )
                        }
                ) {
                    ComposeCanvas(modifier = Modifier.fillMaxSize()) {
                        for (line in lines) {
                            for (i in 0 until line.size - 1) {
                                drawLine(
                                    color = Color.Black,
                                    start = line[i],
                                    end = line[i + 1],
                                    strokeWidth = 6f
                                )
                            }
                        }
                        for (i in 0 until currentLine.size - 1) {
                            drawLine(
                                color = Color.Black,
                                start = currentLine[i],
                                end = currentLine[i + 1],
                                strokeWidth = 6f
                            )
                        }
                    }
                }
            }
        }
    }
}
