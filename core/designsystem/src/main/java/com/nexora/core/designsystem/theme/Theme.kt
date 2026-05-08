package com.nexora.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = NexoraBlue,
    secondary = NexoraAccent,
    background = NexoraSurface,
    surface = NexoraSurface,
    onPrimary = Color.White,
    onSecondary = Color.Black
)

private val LightColors = lightColorScheme(
    primary = NexoraBlueDark,
    secondary = NexoraAccent,
    background = NexoraSurfaceLight,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
