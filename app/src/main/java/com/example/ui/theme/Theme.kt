package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SoundDeckCyan,
    secondary = SoundDeckCyanDark,
    tertiary = SoundDeckAmber,
    background = SoundDeckBackground,
    surface = SoundDeckSurface,
    surfaceVariant = SoundDeckSurfaceVariant,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    error = SoundDeckRed
)

@Composable
fun SoundDeckTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}

