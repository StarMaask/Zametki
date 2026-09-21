package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.local.NoteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotesAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_notes_layout)

        // PendingIntent для открытия главного экрана
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

        // PendingIntent для создания новой заметки
        val newNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_ACTION, ACTION_NEW_NOTE)
        }
        val newNotePendingIntent = PendingIntent.getActivity(
            context,
            1,
            newNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_btn_add, newNotePendingIntent)

        // Загрузка заметок из базы данных в фоновом потоке
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = NoteDatabase.getInstance(context)
                val notes = db.noteDao().getAllActiveNotesList()
                    .filter { !it.isArchived }
                    .sortedWith(compareByDescending<com.example.data.local.entity.NoteEntity> { it.isPinned }.thenByDescending { it.updatedAt })

                withContext(Dispatchers.Main) {
                    views.setTextViewText(R.id.widget_count, "${notes.size} зам.")

                    if (notes.isEmpty()) {
                        views.setViewVisibility(R.id.widget_empty_text, View.VISIBLE)
                        views.setViewVisibility(R.id.widget_item_1, View.GONE)
                        views.setViewVisibility(R.id.widget_item_2, View.GONE)
                        views.setViewVisibility(R.id.widget_item_3, View.GONE)
                    } else {
                        views.setViewVisibility(R.id.widget_empty_text, View.GONE)

                        val itemLayouts = listOf(
                            Triple(R.id.widget_item_1, R.id.widget_item_1_title, R.id.widget_item_1_content),
                            Triple(R.id.widget_item_2, R.id.widget_item_2_title, R.id.widget_item_2_content),
                            Triple(R.id.widget_item_3, R.id.widget_item_3_title, R.id.widget_item_3_content)
                        )

                        for (i in itemLayouts.indices) {
                            val (containerId, titleId, contentId) = itemLayouts[i]
                            if (i < notes.size) {
                                val note = notes[i]
                                views.setViewVisibility(containerId, View.VISIBLE)
                                val title = if (note.title.isNotBlank()) note.title else "Без названия"
                                val displayTitle = if (note.isPinned) "📌 $title" else title
                                views.setTextViewText(titleId, displayTitle)

                                val contentText = if (note.isLocked) {
                                    "🔒 Заметка защищена"
                                } else if (note.content.isNotBlank()) {
                                    note.content.replace("\n", " ")
                                } else {
                                    "Пустая заметка"
                                }
                                views.setTextViewText(contentId, contentText)

                                val noteIntent = Intent(context, MainActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    putExtra(EXTRA_ACTION, ACTION_OPEN_NOTE)
                                    putExtra(EXTRA_NOTE_ID, note.id)
                                }
                                val notePendingIntent = PendingIntent.getActivity(
                                    context,
                                    (100 + i),
                                    noteIntent,
                                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                )
                                views.setOnClickPendingIntent(containerId, notePendingIntent)
                            } else {
                                views.setViewVisibility(containerId, View.GONE)
                            }
                        }
                    }

                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } catch (_: Exception) {
                // Ignore any db errors gracefully
            }
        }
    }

    companion object {
        const val EXTRA_ACTION = "com.example.widget.EXTRA_ACTION"
        const val ACTION_NEW_NOTE = "ACTION_NEW_NOTE"
        const val ACTION_OPEN_NOTE = "ACTION_OPEN_NOTE"
        const val EXTRA_NOTE_ID = "EXTRA_NOTE_ID"

        fun notifyDataChanged(context: Context) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, NotesAppWidgetProvider::class.java)
            val ids = appWidgetManager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                val intent = Intent(context, NotesAppWidgetProvider::class.java).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                }
                context.sendBroadcast(intent)
            }
        }
    }
}
