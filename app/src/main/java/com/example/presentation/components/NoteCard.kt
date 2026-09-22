package com.example.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CheckListItem
import com.example.domain.model.Note
import com.example.domain.model.PageFormat
import kotlinx.serialization.json.Json
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteCard(
    note: Note,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val cardColor = try {
        Color(android.graphics.Color.parseColor(note.colorHex))
    } catch (_: Exception) {
        MaterialTheme.colorScheme.surface
    }

    val isDark = (cardColor.red * 0.299 + cardColor.green * 0.587 + cardColor.blue * 0.114) < 0.5
    val contentColor = if (isDark) Color.White else Color(0xFF1E293B)
    val secondaryColor = if (isDark) Color(0xFFCBD5E1) else Color(0xFF64748B)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = if (isSelected) BorderStroke(2.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (note.isLocked) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = "Защищённая заметка",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Заметка защищена",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = contentColor
                        )
                    }
                } else if (note.title.isNotBlank()) {
                    Text(
                        text = note.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = contentColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                if (isSelectionMode) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() }
                    )
                } else {
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                            contentDescription = if (note.isPinned) "Открепить" else "Закрепить",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else secondaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (note.isLocked) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Нажмите для ввода PIN / биометрии",
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor
                )
            } else if (note.checkListJson.isNotBlank()) {
                val checklist = remember(note.checkListJson) {
                    try {
                        Json.decodeFromString<List<CheckListItem>>(note.checkListJson)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }
                if (checklist.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        checklist.take(4).forEach { item ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.isChecked) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                    contentDescription = null,
                                    tint = if (item.isChecked) secondaryColor.copy(alpha = 0.5f) else secondaryColor,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = if (item.isChecked) secondaryColor.copy(alpha = 0.5f) else contentColor,
                                        textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        if (checklist.size > 4) {
                            Text(
                                text = "+ ещё ${checklist.size - 4} п.",
                                style = MaterialTheme.typography.labelSmall,
                                color = secondaryColor
                            )
                        }
                    }
                } else if (note.content.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = note.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = contentColor,
                        maxLines = 8,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else if (note.content.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor,
                    maxLines = 8,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Tags / Folders / Indicators
            if (note.tags.isNotEmpty() || !note.folder.isNullOrBlank() || note.reminderTime != null || !note.audioUri.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!note.folder.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = note.folder,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (note.format != PageFormat.BLANK) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (note.format) {
                                        PageFormat.BOOK -> Icons.Filled.AutoStories
                                        PageFormat.RULED -> Icons.Filled.FormatAlignJustify
                                        PageFormat.GRID -> Icons.Filled.BorderAll
                                        PageFormat.KRAFT -> Icons.Filled.Style
                                        PageFormat.VINTAGE -> Icons.Filled.Bookmark
                                        PageFormat.MIDNIGHT -> Icons.Filled.DarkMode
                                        PageFormat.BLUEPRINT -> Icons.Filled.Edit
                                        PageFormat.BLANK -> Icons.Filled.Description
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = note.format.title,
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    if (note.reminderTime != null) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Напоминание",
                            tint = secondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    if (!note.audioUri.isNullOrBlank()) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Аудиозапись",
                            tint = secondaryColor,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    note.tags.take(2).forEach { tag ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = secondaryColor.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "#$tag",
                                fontSize = 10.sp,
                                color = secondaryColor,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            val dateStr = SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(note.updatedAt))
            Text(
                text = dateStr,
                fontSize = 10.sp,
                color = secondaryColor
            )
        }
    }
}
