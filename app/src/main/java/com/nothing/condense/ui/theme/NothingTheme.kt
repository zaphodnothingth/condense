package com.nothing.condense.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD71920), // Nothing Red
    onPrimary = Color.White,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    background = Color(0xFF000000), // Pure OLED Black
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF141416),
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFF1C1C1E),
    onSurfaceVariant = Color(0xFFA1A1A6)
)

@Composable
fun NothingRainTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
