package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// ПАЛИТРА ТЕМ: 3 АЛЬТЕРНАТИВНЫХ ТЕМЫ + СЕМАНТИЧЕСКИЕ ЦВЕТА
// ============================================================================

// --- 1. Тема "Чистота" (Purity): Светлая, бело-серо-синяя ---
// Обоснование: Ощущение свежести, чистого листа блокнота, высокой читаемости.
val PurityPrimary = Color(0xFF1E5BB8)
val PurityOnPrimary = Color(0xFFFFFFFF)
val PurityPrimaryContainer = Color(0xFFD8E6FF)
val PurityOnPrimaryContainer = Color(0xFF001B3F)

val PuritySecondary = Color(0xFF4A607A)
val PurityOnSecondary = Color(0xFFFFFFFF)
val PuritySecondaryContainer = Color(0xFFD0E4FF)
val PurityOnSecondaryContainer = Color(0xFF061D33)

val PurityBackground = Color(0xFFF7F9FC)
val PurityOnBackground = Color(0xFF191C20)
val PuritySurface = Color(0xFFFFFFFF)
val PurityOnSurface = Color(0xFF191C20)
val PuritySurfaceVariant = Color(0xFFE2E8F0)
val PurityOnSurfaceVariant = Color(0xFF43474E)
val PurityOutline = Color(0xFFCBD5E1)

// --- 2. Тема "Глубина" (Depth): Тёмная, угольно-фиолетовая ---
// Обоснование: Премиальный ночной режим, не утомляет глаза, акцентный глубокий фиолетовый.
val DepthPrimary = Color(0xFFBB86FC)
val DepthOnPrimary = Color(0xFF280057)
val DepthPrimaryContainer = Color(0xFF4A2885)
val DepthOnPrimaryContainer = Color(0xFFEADBFF)

val DepthSecondary = Color(0xFFC7BCE0)
val DepthOnSecondary = Color(0xFF302B44)
val DepthSecondaryContainer = Color(0xFF47415B)
val DepthOnSecondaryContainer = Color(0xFFE4D8FD)

val DepthBackground = Color(0xFF110E18)
val DepthOnBackground = Color(0xFFE6E1E8)
val DepthSurface = Color(0xFF1A1624)
val DepthOnSurface = Color(0xFFE6E1E8)
val DepthSurfaceVariant = Color(0xFF282334)
val DepthOnSurfaceVariant = Color(0xFFCAC4D0)
val DepthOutline = Color(0xFF413B50)

// --- 3. Тема "Тепло" (Warmth): Бежево-терракотовая, уютная ---
// Обоснование: Теплый аналоговый крафт, как винтажная бумага и теплый вечерний свет.
val WarmthPrimary = Color(0xFFB85D36)
val WarmthOnPrimary = Color(0xFFFFFFFF)
val WarmthPrimaryContainer = Color(0xFFFFDBCE)
val WarmthOnPrimaryContainer = Color(0xFF3B1202)

val WarmthSecondary = Color(0xFF74594E)
val WarmthOnSecondary = Color(0xFFFFFFFF)
val WarmthSecondaryContainer = Color(0xFFFFDBCF)
val WarmthOnSecondaryContainer = Color(0xFF2B170F)

val WarmthBackground = Color(0xFFFBF8F4)
val WarmthOnBackground = Color(0xFF201A18)
val WarmthSurface = Color(0xFFFFFBF7)
val WarmthOnSurface = Color(0xFF201A18)
val WarmthSurfaceVariant = Color(0xFFF3ECE6)
val WarmthOnSurfaceVariant = Color(0xFF51443F)
val WarmthOutline = Color(0xFFD8C7BF)

// --- 8 ПРЕДУСТАНОВЛЕННЫХ ЦВЕТОВЫХ МЕТОК ДЛЯ ЗАМЕТОК ---
// Подобраны для комфортной идентификации и гармонии с карточками
object NoteTagColors {
    val Blue = Color(0xFF3B82F6)       // Голубой / Важное
    val Indigo = Color(0xFF6366F1)     // Индиго / Мысли
    val Emerald = Color(0xFF10B981)    // Изумрудный / Работа
    val Amber = Color(0xFFF59E0B)      // Янтарный / Срочно
    val Rose = Color(0xFFF43F5E)       // Розовый / Идеи
    val Purple = Color(0xFF8B5CF6)     // Фиолетовый / Личное
    val Teal = Color(0xFF14B8A6)       // Бирюзовый / Учеба
    val Orange = Color(0xFFF97316)     // Оранжевый / Покупки

    val list = listOf(
        Blue, Indigo, Emerald, Amber,
        Rose, Purple, Teal, Orange
    )
}
