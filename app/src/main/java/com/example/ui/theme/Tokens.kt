package com.example.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

/**
 * Дизайн-токены приложения заметок:
 * - Скругления (12, 16, 24 dp)
 * - Отступы по сетке 8dp
 * - Тени и возвышения (Elevation)
 * - Спецификации длительности и кривых анимаций
 */
object NoteDimens {
    // Сетка отступов (8-point grid)
    val spacingXSmall = 4.dp
    val spacingSmall = 8.dp
    val spacingMedium = 16.dp
    val spacingLarge = 24.dp
    val spacingXLarge = 32.dp

    // Скругления (Radius tokens)
    val radiusSmall = 12.dp   // Мелкие элементы: chips, теги, кнопки действий
    val radiusMedium = 16.dp  // Карточки заметок, диалоги
    val radiusLarge = 24.dp   // BottomSheet, FAB, контейнер редактора
    val radiusFull = 100.dp   // Pill-формы (поисковая строка, аватарки)

    // Формы
    val shapeSmall = RoundedCornerShape(radiusSmall)
    val shapeMedium = RoundedCornerShape(radiusMedium)
    val shapeLarge = RoundedCornerShape(radiusLarge)
    val shapePill = RoundedCornerShape(radiusFull)

    // Возвышения (Elevation)
    val elevationNone = 0.dp
    val elevationLow = 2.dp    // Карточка в покое
    val elevationMedium = 6.dp // Карточка в фокусе / перетаскивании
    val elevationHigh = 12.dp  // FAB, BottomSheet
}

/**
 * Спецификации анимаций интерфейса с точным временем и кривыми
 */
object NoteAnimationSpecs {
    // Длительности (ms)
    const val DURATION_FAST = 150        // Микро-взаимодействия (тап, checkbox)
    const val DURATION_NORMAL = 280      // Раскрытие/закрытие элементов, появление карточек
    const val DURATION_EXPAND = 350      // Переход со списка в редактор, FAB expand
    const val DURATION_THEME_CROSSFADE = 400 // Плавное переключение темы

    // Кривые Безье (Easing)
    val EmphasizedEasing: Easing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f) // Material 3 Emphasized
    val StandardEasing: Easing = FastOutSlowInEasing
}
