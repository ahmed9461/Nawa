package com.ahmed9461.nawa.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val Purple = Color(0xFF6F63FF)
private val DarkBackground = Color(0xFF0B1020)
private val DarkSurface = Color(0xFF121A2D)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB9B3FF),
    onPrimary = Color(0xFF201A69),
    primaryContainer = Color(0xFF39318A),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = Color(0xFF202A40),
)

private val LightColors = lightColorScheme(
    primary = Purple,
    primaryContainer = Color(0xFFE5E1FF),
    background = Color(0xFFF7F8FC),
    surface = Color.White,
    surfaceVariant = Color(0xFFEFF1F7),
)

@Composable
fun NawaTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val view = LocalView.current

    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !dark
        }
    }

    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        content = content,
    )
}
