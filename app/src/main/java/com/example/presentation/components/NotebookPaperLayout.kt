package com.example.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BorderAll
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FormatAlignJustify
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.PageFormat

// Ink & Paper Palette
object NotebookPalette {
    val BookPaper = Color(0xFFFBF8F1)
    val BookSpineDark = Color(0xFF4A3528)
    val BookSpineMid = Color(0xFF5D4037)
    val BookSpineLight = Color(0xFF795548)
    val BookBookmark = Color(0xFFC62828)
    val BookInk = Color(0xFF2C2420)
    val BookPlaceholder = Color(0xFF786F68)

    val RuledPaper = Color(0xFFFCFBF7)
    val RuledLine = Color(0xFFD6E2EE)
    val RuledMargin = Color(0xFFEF5350)
    val RuledInk = Color(0xFF1B365D)
    val RuledPlaceholder = Color(0xFF6B7280)

    val GridPaper = Color(0xFFFBFBFB)
    val GridLine = Color(0xFFD8E2EC)
    val GridMargin = Color(0xFFEF5350)
    val GridInk = Color(0xFF1E293B)
    val GridPlaceholder = Color(0xFF64748B)

    val BlankInk = Color(0xFF18181B)
    val BlankPlaceholder = Color(0xFF71717A)

    val DarkInk = Color(0xFFF8FAFC)
    val DarkPlaceholder = Color(0xFF94A3B8)
}

fun getPaperColor(format: PageFormat, customColorHex: String): Color {
    return when (format) {
        PageFormat.BOOK -> NotebookPalette.BookPaper
        PageFormat.RULED -> NotebookPalette.RuledPaper
        PageFormat.GRID -> NotebookPalette.GridPaper
        PageFormat.BLANK -> {
            try {
                Color(android.graphics.Color.parseColor(customColorHex))
            } catch (_: Exception) {
                Color(0xFFFAF9F6)
            }
        }
    }
}

fun getInkColor(format: PageFormat, paperColor: Color): Color {
    val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(paperColor.hashCode())
    return if (luminance > 0.45) {
        when (format) {
            PageFormat.BOOK -> NotebookPalette.BookInk
            PageFormat.RULED -> NotebookPalette.RuledInk
            PageFormat.GRID -> NotebookPalette.GridInk
            PageFormat.BLANK -> NotebookPalette.BlankInk
        }
    } else {
        NotebookPalette.DarkInk
    }
}

fun getPlaceholderColor(format: PageFormat, paperColor: Color): Color {
    val luminance = androidx.core.graphics.ColorUtils.calculateLuminance(paperColor.hashCode())
    return if (luminance > 0.45) {
        when (format) {
            PageFormat.BOOK -> NotebookPalette.BookPlaceholder
            PageFormat.RULED -> NotebookPalette.RuledPlaceholder
            PageFormat.GRID -> NotebookPalette.GridPlaceholder
            PageFormat.BLANK -> NotebookPalette.BlankPlaceholder
        }
    } else {
        NotebookPalette.DarkPlaceholder
    }
}

fun getFontFamily(format: PageFormat): FontFamily {
    return when (format) {
        PageFormat.BOOK -> FontFamily.Serif
        else -> FontFamily.Default
    }
}

@Composable
fun BookBookmarkRibbon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 36.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(w, 0f)
            lineTo(w, h)
            lineTo(w / 2f, h - 8.dp.toPx())
            lineTo(0f, h)
            close()
        }
        drawPath(
            path = path,
            color = NotebookPalette.BookBookmark
        )
    }
}

@Composable
fun SpiralRings(
    modifier: Modifier = Modifier,
    count: Int = 14
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.SpaceEvenly,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        repeat(count) {
            Box(
                modifier = Modifier
                    .size(width = 14.dp, height = 6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF78909C),
                                Color(0xFFCFD8DC),
                                Color(0xFF546E7A)
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun NotebookPaperCanvas(
    format: PageFormat,
    paperColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(paperColor)
            .drawBehind {
                val canvasWidth = size.width
                val canvasHeight = size.height

                when (format) {
                    PageFormat.BOOK -> {
                        // 1. Left Book Spine Gradient
                        val spineWidth = 14.dp.toPx()
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    NotebookPalette.BookSpineDark,
                                    NotebookPalette.BookSpineMid,
                                    NotebookPalette.BookSpineLight
                                ),
                                startX = 0f,
                                endX = spineWidth
                            ),
                            topLeft = Offset.Zero,
                            size = Size(spineWidth, canvasHeight)
                        )

                        // 2. Spine shadow curvature onto the book page
                        drawRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0x35000000),
                                    Color(0x15000000),
                                    Color.Transparent
                                ),
                                startX = spineWidth,
                                endX = spineWidth + 24.dp.toPx()
                            ),
                            topLeft = Offset(spineWidth, 0f),
                            size = Size(24.dp.toPx(), canvasHeight)
                        )

                        // 3. Subtle page perimeter deckle border
                        drawRect(
                            color = Color(0x12000000),
                            topLeft = Offset.Zero,
                            size = Size(canvasWidth, canvasHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }

                    PageFormat.RULED -> {
                        // 1. Horizontal ruled lines
                        val lineSpacing = 32.dp.toPx()
                        var y = lineSpacing
                        while (y < canvasHeight) {
                            drawLine(
                                color = NotebookPalette.RuledLine,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1.dp.toPx()
                            )
                            y += lineSpacing
                        }

                        // 2. Left Red Margin Line
                        val marginX = 48.dp.toPx()
                        drawLine(
                            color = NotebookPalette.RuledMargin,
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, canvasHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    PageFormat.GRID -> {
                        val gridSize = 22.dp.toPx()

                        // Horizontal lines
                        var y = gridSize
                        while (y < canvasHeight) {
                            drawLine(
                                color = NotebookPalette.GridLine,
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 0.8.dp.toPx()
                            )
                            y += gridSize
                        }

                        // Vertical lines
                        var x = gridSize
                        while (x < canvasWidth) {
                            drawLine(
                                color = NotebookPalette.GridLine,
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 0.8.dp.toPx()
                            )
                            x += gridSize
                        }

                        // Left Red Margin Line
                        val marginX = 48.dp.toPx()
                        drawLine(
                            color = NotebookPalette.GridMargin,
                            start = Offset(marginX, 0f),
                            end = Offset(marginX, canvasHeight),
                            strokeWidth = 1.5.dp.toPx()
                        )
                    }

                    PageFormat.BLANK -> {
                        // Subtle clean border
                        drawRect(
                            color = Color(0x15000000),
                            topLeft = Offset.Zero,
                            size = Size(canvasWidth, canvasHeight),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                        )
                    }
                }
            }
    ) {
        // Decorative top bookmark for Book format
        if (format == PageFormat.BOOK) {
            BookBookmarkRibbon(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 20.dp)
            )
        }

        // Spiral rings for ruled / grid formats
        if (format == PageFormat.RULED || format == PageFormat.GRID) {
            SpiralRings(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 6.dp)
            )
        }

        content()
    }
}

@Composable
fun PageFormatSelectorDialog(
    currentFormat: PageFormat,
    onDismissRequest: () -> Unit,
    onFormatSelect: (PageFormat) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Filled.AutoStories,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = "Формат страницы",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                PageFormat.values().forEach { format ->
                    val isSelected = format == currentFormat
                    val icon: ImageVector = when (format) {
                        PageFormat.BOOK -> Icons.Filled.AutoStories
                        PageFormat.RULED -> Icons.Filled.FormatAlignJustify
                        PageFormat.GRID -> Icons.Filled.BorderAll
                        PageFormat.BLANK -> Icons.Filled.Description
                    }

                    Card(
                        onClick = { onFormatSelect(format) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            }
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = format.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = format.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("Закрыть")
            }
        }
    )
}
