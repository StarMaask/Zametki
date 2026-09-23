package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class FontSizeScale(val title: String, val scale: Float) {
    SMALL("Компактный", 0.85f),
    NORMAL("Обычный", 1.0f),
    LARGE("Крупный", 1.18f);

    val scaleMultiplier: Float get() = scale
}

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("app_theme")
    private val KEY_FONT_SIZE = stringPreferencesKey("font_size")
    private val KEY_PIN_CODE = stringPreferencesKey("pin_code")
    private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")
    private val KEY_BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
    private val KEY_IS_GRID_LAYOUT = booleanPreferencesKey("is_grid_layout")
    private val KEY_CUSTOM_FONT_PATH = stringPreferencesKey("custom_font_path")

    private val syncPrefs = context.getSharedPreferences("user_settings_sync", Context.MODE_PRIVATE)

    fun isGridLayoutSync(): Boolean {
        return syncPrefs.getBoolean("is_grid_layout", true)
    }

    val fontSizeFlow: Flow<FontSizeScale> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_FONT_SIZE] ?: FontSizeScale.NORMAL.name
        try {
            FontSizeScale.valueOf(name)
        } catch (_: Exception) {
            FontSizeScale.NORMAL
        }
    }

    val pinCodeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_CODE] ?: "0000"
    }

    val isPinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_ENABLED] ?: false
    }

    val isBiometricEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_BIOMETRIC_ENABLED] ?: false
    }

    val themeFlow: Flow<AppThemePreset> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME] ?: AppThemePreset.PURITY.name
        try {
            AppThemePreset.valueOf(name)
        } catch (_: Exception) {
            AppThemePreset.PURITY
        }
    }

    val isGridLayoutFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_IS_GRID_LAYOUT] ?: true
    }

    val customFontPathFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_FONT_PATH]
    }

    suspend fun setTheme(theme: AppThemePreset) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setFontSize(scale: FontSizeScale) {
        context.dataStore.edit { prefs ->
            prefs[KEY_FONT_SIZE] = scale.name
        }
    }

    suspend fun setPinCode(pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_CODE] = pin
        }
    }

    suspend fun setPinEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PIN_ENABLED] = enabled
        }
    }

    suspend fun setBiometricEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BIOMETRIC_ENABLED] = enabled
        }
    }

    suspend fun setGridLayout(isGrid: Boolean) {
        syncPrefs.edit().putBoolean("is_grid_layout", isGrid).apply()
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_GRID_LAYOUT] = isGrid
        }
    }

    suspend fun setCustomFontPath(path: String?) {
        context.dataStore.edit { prefs ->
            if (path == null) {
                prefs.remove(KEY_CUSTOM_FONT_PATH)
            } else {
                prefs[KEY_CUSTOM_FONT_PATH] = path
            }
        }
    }
}
