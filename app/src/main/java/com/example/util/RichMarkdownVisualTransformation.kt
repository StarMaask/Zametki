package com.example.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

class RichMarkdownVisualTransformation(
    private val baseTextColor: Color
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val tagColor = baseTextColor.copy(alpha = 0.35f)
        val tagStyle = SpanStyle(color = tagColor, fontSize = 11.sp)

        val annotated = buildAnnotatedString {
            append(raw)

            // Default base style
            addStyle(SpanStyle(color = baseTextColor), 0, raw.length)

            // 1. Line-level styling (Headings #, Quotes >)
            val lines = raw.split("\n")
            var lineStart = 0
            for (line in lines) {
                val trimmed = line.trimStart()
                if (trimmed.startsWith("### ")) {
                    val prefixLen = line.indexOf("### ") + 4
                    addStyle(tagStyle, lineStart, lineStart + prefixLen)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp), lineStart + prefixLen, lineStart + line.length)
                } else if (trimmed.startsWith("## ")) {
                    val prefixLen = line.indexOf("## ") + 3
                    addStyle(tagStyle, lineStart, lineStart + prefixLen)
                    addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = 19.sp), lineStart + prefixLen, lineStart + line.length)
                } else if (trimmed.startsWith("# ")) {
                    val prefixLen = line.indexOf("# ") + 2
                    addStyle(tagStyle, lineStart, lineStart + prefixLen)
                    addStyle(SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 21.sp), lineStart + prefixLen, lineStart + line.length)
                } else if (trimmed.startsWith("> ")) {
                    val prefixLen = line.indexOf("> ") + 2
                    addStyle(tagStyle, lineStart, lineStart + prefixLen)
                    addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = baseTextColor.copy(alpha = 0.85f)), lineStart + prefixLen, lineStart + line.length)
                }
                lineStart += line.length + 1
            }

            // 2. Bold (**text**)
            val boldRegex = Regex("\\*\\*(.+?)\\*\\*")
            boldRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 2)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first + 2, range.last - 1)
                addStyle(tagStyle, range.last - 1, range.last + 1)
            }

            // 2b. Bold (<b>text</b> or <strong>text</strong>)
            Regex("<(b|strong)>(.+?)</\\1>", RegexOption.IGNORE_CASE).findAll(raw).forEach { match ->
                val tagLen = match.groupValues[1].length + 2
                val closeTagLen = match.groupValues[1].length + 3
                val range = match.range
                addStyle(tagStyle, range.first, range.first + tagLen)
                addStyle(SpanStyle(fontWeight = FontWeight.Bold), range.first + tagLen, range.last - closeTagLen + 1)
                addStyle(tagStyle, range.last - closeTagLen + 1, range.last + 1)
            }

            // 3. Italic (*text*) - avoid matching within **
            val italicRegex = Regex("(?<!\\*)\\*([^*\\n]+?)\\*(?!\\*)")
            italicRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 1)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), range.first + 1, range.last)
                addStyle(tagStyle, range.last, range.last + 1)
            }

            // 3b. Italic (<i>text</i> or <em>text</em>)
            Regex("<(i|em)>(.+?)</\\1>", RegexOption.IGNORE_CASE).findAll(raw).forEach { match ->
                val tagLen = match.groupValues[1].length + 2
                val closeTagLen = match.groupValues[1].length + 3
                val range = match.range
                addStyle(tagStyle, range.first, range.first + tagLen)
                addStyle(SpanStyle(fontStyle = FontStyle.Italic), range.first + tagLen, range.last - closeTagLen + 1)
                addStyle(tagStyle, range.last - closeTagLen + 1, range.last + 1)
            }

            // 4. Strikethrough (~~text~~ or <s>text</s> or <del>text</del>)
            val strikeRegex = Regex("~~(.+?)~~")
            strikeRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 2)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.first + 2, range.last - 1)
                addStyle(tagStyle, range.last - 1, range.last + 1)
            }
            Regex("<(s|del)>(.+?)</\\1>", RegexOption.IGNORE_CASE).findAll(raw).forEach { match ->
                val tagLen = match.groupValues[1].length + 2
                val closeTagLen = match.groupValues[1].length + 3
                val range = match.range
                addStyle(tagStyle, range.first, range.first + tagLen)
                addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), range.first + tagLen, range.last - closeTagLen + 1)
                addStyle(tagStyle, range.last - closeTagLen + 1, range.last + 1)
            }

            // 5. Underline (<u>text</u> or __text__)
            val underlineTagRegex = Regex("<u>(.+?)</u>", RegexOption.IGNORE_CASE)
            underlineTagRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 3)
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), range.first + 3, range.last - 3)
                addStyle(tagStyle, range.last - 3, range.last + 1)
            }
            val underlineUnderscoreRegex = Regex("__(.+?)__")
            underlineUnderscoreRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 2)
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), range.first + 2, range.last - 1)
                addStyle(tagStyle, range.last - 1, range.last + 1)
            }

            // 6. Highlight (==text== or <mark>text</mark>)
            val highlightRegex = Regex("==(.+?)==")
            highlightRegex.findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 2)
                addStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF1E293B)), range.first + 2, range.last - 1)
                addStyle(tagStyle, range.last - 1, range.last + 1)
            }
            Regex("<mark>(.+?)</mark>", RegexOption.IGNORE_CASE).findAll(raw).forEach { match ->
                val range = match.range
                addStyle(tagStyle, range.first, range.first + 6)
                addStyle(SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF1E293B)), range.first + 6, range.last - 6)
                addStyle(tagStyle, range.last - 6, range.last + 1)
            }

            // 7. Custom Text Color ([color=#HEX]text[/color])
            val colorRegex = Regex("\\[color=(#[0-9a-fA-F]{6})\\](.*?)\\[/color\\]")
            colorRegex.findAll(raw).forEach { match ->
                val hex = match.groupValues[1]
                val openTagLen = 8 + hex.length // "[color=#123456]" is 15 chars
                val closeTagLen = 8 // "[/color]"
                val range = match.range
                try {
                    val parsedColor = Color(android.graphics.Color.parseColor(hex))
                    addStyle(tagStyle, range.first, range.first + openTagLen)
                    addStyle(SpanStyle(color = parsedColor), range.first + openTagLen, range.last - closeTagLen + 1)
                    addStyle(tagStyle, range.last - closeTagLen + 1, range.last + 1)
                } catch (_: Exception) {}
            }
            // 7b. HTML Color (<font color="#HEX">text</font>)
            val fontColorRegex = Regex("<font color=\"(#[0-9a-fA-F]{6})\">(.*?)</font>", RegexOption.IGNORE_CASE)
            fontColorRegex.findAll(raw).forEach { match ->
                val hex = match.groupValues[1]
                val openTagLen = 14 + hex.length // '<font color="#123456">'
                val closeTagLen = 7 // "</font>"
                val range = match.range
                try {
                    val parsedColor = Color(android.graphics.Color.parseColor(hex))
                    addStyle(tagStyle, range.first, range.first + openTagLen)
                    addStyle(SpanStyle(color = parsedColor), range.first + openTagLen, range.last - closeTagLen + 1)
                    addStyle(tagStyle, range.last - closeTagLen + 1, range.last + 1)
                } catch (_: Exception) {}
            }
        }

        return TransformedText(annotated, OffsetMapping.Identity)
    }
}
