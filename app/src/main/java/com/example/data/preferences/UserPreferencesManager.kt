package com.example.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.AppThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class UserPreferencesManager(private val context: Context) {

    private val KEY_THEME = stringPreferencesKey("app_theme")
    private val KEY_VIEW_MODE = stringPreferencesKey("view_mode")
    private val KEY_HAPTIC = booleanPreferencesKey("haptic_feedback")
    private val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    private val KEY_PIN_CODE = stringPreferencesKey("pin_code")
    private val KEY_PIN_ENABLED = booleanPreferencesKey("pin_enabled")

    val pinCodeFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_CODE] ?: "0000"
    }

    val isPinEnabledFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PIN_ENABLED] ?: false
    }

    val themeFlow: Flow<AppThemePreset> = context.dataStore.data.map { prefs ->
        val name = prefs[KEY_THEME] ?: AppThemePreset.PURITY.name
        try {
            AppThemePreset.valueOf(name)
        } catch (_: Exception) {
            AppThemePreset.PURITY
        }
    }

    val isFirstLaunchFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_FIRST_LAUNCH] ?: true
    }

    suspend fun setTheme(theme: AppThemePreset) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }

    suspend fun setOnboardingFinished() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
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
}
