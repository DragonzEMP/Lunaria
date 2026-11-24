package com.lunaria.musicplayer.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun LunariaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val themeColors by ThemeManager.currentTheme.collectAsState()

    val dynamicColorScheme = darkColorScheme(
        primary = themeColors.primaryGlow,
        onPrimary = themeColors.backgroundDark,
        secondary = themeColors.secondaryAccent,
        onSecondary = themeColors.backgroundDark,
        tertiary = NeonPink,
        background = themeColors.backgroundDark,
        onBackground = themeColors.metallicText,
        surface = themeColors.surfaceDark,
        onSurface = themeColors.metallicText,
        surfaceVariant = themeColors.surfaceVariantDark,
        onSurfaceVariant = themeColors.metallicMuted,
        outline = themeColors.copyrightColor
    )

    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = themeColors.backgroundDark.toArgb()
                window.navigationBarColor = themeColors.backgroundDark.toArgb()
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    CompositionLocalProvider(LocalThemeColors provides themeColors) {
        MaterialTheme(
            colorScheme = dynamicColorScheme,
            typography = Typography,
            content = content
        )
    }
}
