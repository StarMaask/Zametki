package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.model.CheckListItem
import com.example.domain.model.Note
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ShareExportUtil {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())

    private fun parseChecklist(jsonStr: String): List<CheckListItem> {
        return try {
            if (jsonStr.isNotBlank()) Json.decodeFromString<List<CheckListItem>>(jsonStr) else emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun buildNoteShareText(note: Note): String {
        val sb = StringBuilder()
        val title = if (note.title.isNotBlank()) note.title else "Без названия"
        sb.append("📝 ").append(title).append("\n")

        val folder = note.folder
        if (!folder.isNullOrBlank()) {
            sb.append("📁 Папка: ").append(folder).append("\n")
        }

        sb.append("📅 ").append(dateFormat.format(Date(note.updatedAt))).append("\n")

        if (note.tags.isNotEmpty()) {
            sb.append("🏷️ ").append(note.tags.joinToString(" ") { "#$it" }).append("\n")
        }

        sb.append("\n")

        val checklist = parseChecklist(note.checkListJson)
        if (checklist.isNotEmpty()) {
            for (item in checklist) {
                val checkMark = if (item.isChecked) "✓" else "☐"
                sb.append("$checkMark ${item.text}\n")
            }
            if (note.content.isNotBlank()) {
                sb.append("\n").append(note.content).append("\n")
            }
        } else {
            sb.append(note.content)
        }

        return sb.toString().trim()
    }

    fun shareAsText(context: Context, note: Note) {
        val text = buildNoteShareText(note)
        val title = if (note.title.isNotBlank()) note.title else "Заметка"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, "Поделиться заметкой"))
    }

    fun shareAsTxtFile(context: Context, note: Note) {
        try {
            val text = buildNoteShareText(note)
            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = note.title.replace(Regex("[^a-zA-Zа-яА-Я0-9_]"), "_").take(20).ifBlank { "note" }
            val file = File(exportDir, "${cleanTitle}_${System.currentTimeMillis()}.txt")
            file.writeText(text)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Экспорт в TXT"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка создания файла TXT: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun shareAsPdf(context: Context, note: Note) {
        try {
            val pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width (points)
            val pageHeight = 842 // A4 standard height (points)
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 20f
                isFakeBoldText = true
                isAntiAlias = true
            }

            val metaPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 11f
                isAntiAlias = true
            }

            val bodyPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 13f
                isAntiAlias = true
            }

            val linePaint = Paint().apply {
                color = Color.LTGRAY
                strokeWidth = 1f
            }

            var currentY = 50f
            val margin = 50f
            val contentWidth = (pageWidth - margin * 2).toInt()

            // Header Title
            val titleText = if (note.title.isNotBlank()) note.title else "Без названия"
            val titleLayout = StaticLayout.Builder.obtain(titleText, 0, titleText.length, titlePaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(margin, currentY)
            titleLayout.draw(canvas)
            canvas.restore()
            currentY += titleLayout.height + 12f

            // Metadata (date, folder, tags)
            val metaBuilder = StringBuilder()
            metaBuilder.append(dateFormat.format(Date(note.updatedAt)))
            val folder = note.folder
            if (!folder.isNullOrBlank()) {
                metaBuilder.append("  •  Папка: ").append(folder)
            }
            if (note.tags.isNotEmpty()) {
                metaBuilder.append("  •  Теги: ").append(note.tags.joinToString(", "))
            }
            val metaString = metaBuilder.toString()
            val metaLayout = StaticLayout.Builder.obtain(metaString, 0, metaString.length, metaPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(margin, currentY)
            metaLayout.draw(canvas)
            canvas.restore()
            currentY += metaLayout.height + 16f

            // Separator line
            canvas.drawLine(margin, currentY, pageWidth - margin, currentY, linePaint)
            currentY += 20f

            // Body Content
            val checklist = parseChecklist(note.checkListJson)
            val bodyText = if (checklist.isNotEmpty()) {
                val clText = checklist.joinToString("\n") { item ->
                    val mark = if (item.isChecked) "[✓]" else "[  ]"
                    "$mark ${item.text}"
                }
                if (note.content.isNotBlank()) "$clText\n\n${note.content}" else clText
            } else {
                note.content
            }

            val bodyLayout = StaticLayout.Builder.obtain(bodyText, 0, bodyText.length, bodyPaint, contentWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .build()
            canvas.save()
            canvas.translate(margin, currentY)
            bodyLayout.draw(canvas)
            canvas.restore()

            pdfDocument.finishPage(page)

            val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val cleanTitle = note.title.replace(Regex("[^a-zA-Zа-яА-Я0-9_]"), "_").take(20).ifBlank { "note" }
            val pdfFile = File(exportDir, "${cleanTitle}_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(pdfFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", pdfFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, titleText)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Экспорт в PDF"))
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Ошибка создания PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
