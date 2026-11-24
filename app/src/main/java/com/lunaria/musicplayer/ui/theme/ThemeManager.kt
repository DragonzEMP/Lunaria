package com.lunaria.musicplayer.ui.theme

import android.content.Context
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

val Context.themeDataStore: DataStore<Preferences> by preferencesDataStore(name = "lunaria_theme_prefs")

data class LunariaPalette(
    val id: String,
    val title: String,
    val primaryGlow: Color,
    val secondaryAccent: Color
)

data class ThemeColors(
    val paletteName: String,
    val primaryGlow: Color,
    val secondaryAccent: Color,
    val backgroundDark: Color = Color(0xFF080A15),
    val surfaceDark: Color = Color(0xFF0F1424),
    val surfaceVariantDark: Color = Color(0xFF161E36),
    val metallicText: Color = Color(0xFFC2D1F0),
    val metallicMuted: Color = Color(0xFF8899B7),
    val copyrightColor: Color = Color(0xFF4F5970)
)

val DefaultThemeColors = ThemeColors(
    paletteName = "Celestial Neon",
    primaryGlow = Color(0xFF9C47FC),
    secondaryAccent = Color(0xFF00F0FF)
)

val LocalThemeColors = staticCompositionLocalOf { DefaultThemeColors }

object ThemeManager {

    private val KEY_THEME_NAME = stringPreferencesKey("theme_name")
    private val KEY_PRIMARY_GLOW = intPreferencesKey("primary_glow")
    private val KEY_SECONDARY_ACCENT = intPreferencesKey("secondary_accent")
    private val KEY_EQ_WARNING_SHOWN_COUNT = intPreferencesKey("eq_warning_shown_count")

    val Presets = listOf(
        LunariaPalette("celestial", "Celestial Neon", Color(0xFF9C47FC), Color(0xFF00F0FF)),
        LunariaPalette("cyberpunk", "Cyberpunk", Color(0xFFFF007F), Color(0xFFFFE600)),
        LunariaPalette("matrix", "Matrix Green", Color(0xFF00FF66), Color(0xFF39FF14)),
        LunariaPalette("solar", "Solar Flare", Color(0xFFFF2A2A), Color(0xFFFFB800)),
        LunariaPalette("vaporwave", "Vaporwave", Color(0xFFB967FF), Color(0xFF01CDFE)),
        LunariaPalette("aurora", "Aurora Mint", Color(0xFF05D9E8), Color(0xFF00FFA3))
    )

    private val _currentTheme = MutableStateFlow(DefaultThemeColors)
    val currentTheme: StateFlow<ThemeColors> = _currentTheme.asStateFlow()

    private val _eqWarningShownCount = MutableStateFlow(0)
    val eqWarningShownCount: StateFlow<Int> = _eqWarningShownCount.asStateFlow()

    fun incrementEqWarningCount() {
        val newCount = _eqWarningShownCount.value + 1
        _eqWarningShownCount.value = newCount
        scope.launch {
            try {
                dataStore?.edit { prefs ->
                    prefs[KEY_EQ_WARNING_SHOWN_COUNT] = newCount
                }
            } catch (_: Exception) {}
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO)
    private var dataStore: DataStore<Preferences>? = null

    fun initialize(context: Context) {
        val ds = context.applicationContext.themeDataStore
        dataStore = ds
        scope.launch {
            try {
                val prefs = ds.data.firstOrNull()
                val savedName = prefs?.get(KEY_THEME_NAME) ?: "Celestial Neon"
                val savedPrimaryInt = prefs?.get(KEY_PRIMARY_GLOW)
                val savedSecondaryInt = prefs?.get(KEY_SECONDARY_ACCENT)

                val primary = if (savedPrimaryInt != null) Color(savedPrimaryInt) else DefaultThemeColors.primaryGlow
                val secondary = if (savedSecondaryInt != null) Color(savedSecondaryInt) else DefaultThemeColors.secondaryAccent

                val warningCount = prefs?.get(KEY_EQ_WARNING_SHOWN_COUNT) ?: 0
                _eqWarningShownCount.value = warningCount

                _currentTheme.value = DefaultThemeColors.copy(
                    paletteName = savedName,
                    primaryGlow = primary,
                    secondaryAccent = secondary
                )
            } catch (_: Exception) {}
        }
    }

    fun applyPalette(palette: LunariaPalette) {
        applyCustomColors(palette.title, palette.primaryGlow, palette.secondaryAccent)
    }

    fun applyCustomColors(name: String, primary: Color, secondary: Color) {
        val updated = _currentTheme.value.copy(
            paletteName = name,
            primaryGlow = primary,
            secondaryAccent = secondary
        )
        _currentTheme.value = updated

        // Persist asynchronously in DataStore
        scope.launch {
            try {
                dataStore?.edit { prefs ->
                    prefs[KEY_THEME_NAME] = name
                    prefs[KEY_PRIMARY_GLOW] = primary.toArgb()
                    prefs[KEY_SECONDARY_ACCENT] = secondary.toArgb()
                }
            } catch (_: Exception) {}
        }
    }
}

