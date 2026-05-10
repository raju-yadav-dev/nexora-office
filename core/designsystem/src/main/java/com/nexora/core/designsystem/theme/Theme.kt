package com.nexora.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DarkColors = darkColorScheme(
    primary = NexoraPrimary,
    onPrimary = Color.White,
    primaryContainer = NexoraPrimaryVariant,
    onPrimaryContainer = Color.White,
    secondary = NexoraSecondary,
    onSecondary = Color.Black,
    secondaryContainer = NexoraSecondary.copy(alpha = 0.18f),
    onSecondaryContainer = Color.White,
    error = NexoraError,
    background = NexoraDarkBackground,
    onBackground = NexoraDarkOnSurface,
    surface = NexoraDarkSurface,
    onSurface = NexoraDarkOnSurface,
    surfaceVariant = NexoraDarkSurfaceHigh,
    onSurfaceVariant = NexoraDarkMuted,
    outline = NexoraDarkOutline,
)

private val LightColors = lightColorScheme(
    primary = NexoraPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE4FF),
    onPrimaryContainer = NexoraPrimaryVariant,
    secondary = NexoraSecondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD8F7EA),
    onSecondaryContainer = Color(0xFF075E46),
    error = NexoraError,
    background = NexoraLightBackground,
    onBackground = NexoraLightOnSurface,
    surface = NexoraLightSurface,
    onSurface = NexoraLightOnSurface,
    surfaceVariant = NexoraLightSurfaceHigh,
    onSurfaceVariant = NexoraLightMuted,
    outline = NexoraLightOutline,
)

private val NexoraTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 19.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 17.sp
    ),
    labelMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)

private val NexoraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(22.dp)
)

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
