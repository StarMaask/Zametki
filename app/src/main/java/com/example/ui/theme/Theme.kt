package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Доступные пресеты темы оформления
 */
enum class AppThemePreset(val title: String) {
    PURITY("Чистота"),   // Светлая бело-серо-синяя
    DEPTH("Глубина"),    // Тёмная угольно-фиолетовая
    WARMTH("Тепло"),     // Бежево-терракотовая
    SYSTEM("Как в системе") // Системная + Material You
}

// 1. Схема "Чистота" (Светлая)
private val PurityColorScheme = lightColorScheme(
    primary = PurityPrimary,
    onPrimary = PurityOnPrimary,
    primaryContainer = PurityPrimaryContainer,
    onPrimaryContainer = PurityOnPrimaryContainer,
    secondary = PuritySecondary,
    onSecondary = PurityOnSecondary,
    secondaryContainer = PuritySecondaryContainer,
    onSecondaryContainer = PurityOnSecondaryContainer,
    background = PurityBackground,
    onBackground = PurityOnBackground,
    surface = PuritySurface,
    onSurface = PurityOnSurface,
    surfaceVariant = PuritySurfaceVariant,
    onSurfaceVariant = PurityOnSurfaceVariant,
    outline = PurityOutline
)

// 2. Схема "Глубина" (Тёмная)
private val DepthColorScheme = darkColorScheme(
    primary = DepthPrimary,
    onPrimary = DepthOnPrimary,
    primaryContainer = DepthPrimaryContainer,
    onPrimaryContainer = DepthOnPrimaryContainer,
    secondary = DepthSecondary,
    onSecondary = DepthOnSecondary,
    secondaryContainer = DepthSecondaryContainer,
    onSecondaryContainer = DepthOnSecondaryContainer,
    background = DepthBackground,
    onBackground = DepthOnBackground,
    surface = DepthSurface,
    onSurface = DepthOnSurface,
    surfaceVariant = DepthSurfaceVariant,
    onSurfaceVariant = DepthOnSurfaceVariant,
    outline = DepthOutline
)

// 3. Схема "Тепло" (Уютная бежевая)
private val WarmthColorScheme = lightColorScheme(
    primary = WarmthPrimary,
    onPrimary = WarmthOnPrimary,
    primaryContainer = WarmthPrimaryContainer,
    onPrimaryContainer = WarmthOnPrimaryContainer,
    secondary = WarmthSecondary,
    onSecondary = WarmthOnSecondary,
    secondaryContainer = WarmthSecondaryContainer,
    onSecondaryContainer = WarmthOnSecondaryContainer,
    background = WarmthBackground,
    onBackground = WarmthOnBackground,
    surface = WarmthSurface,
    onSurface = WarmthOnSurface,
    surfaceVariant = WarmthSurfaceVariant,
    onSurfaceVariant = WarmthOnSurfaceVariant,
    outline = WarmthOutline
)

@Composable
fun NoteAppTheme(
    themePreset: AppThemePreset = AppThemePreset.PURITY,
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current

    val colorScheme: ColorScheme = when (themePreset) {
        AppThemePreset.PURITY -> PurityColorScheme
        AppThemePreset.DEPTH -> DepthColorScheme
        AppThemePreset.WARMTH -> WarmthColorScheme
        AppThemePreset.SYSTEM -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            } else {
                if (isDarkTheme) DepthColorScheme else PurityColorScheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
