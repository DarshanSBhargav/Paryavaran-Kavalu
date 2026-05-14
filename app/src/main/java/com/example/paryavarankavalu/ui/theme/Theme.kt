package com.example.paryavarankavalu.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = EcoLightGreen,
    secondary = EcoSand,
    tertiary = EcoAccent,
    background = Color(0xFF1B1C18),
    surface = Color(0xFF1B1C18),
    onPrimary = EcoDarkGreen,
    onSecondary = EcoEarth,
    onTertiary = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = EcoGreen,
    secondary = EcoEarth,
    tertiary = EcoAccent,
    background = Background,
    surface = White,
    onPrimary = White,
    onSecondary = White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF1B1C18),
    onSurface = Color(0xFF1B1C18),
)

@Composable
fun ParyavaranKavaluTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
