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
    outlineVariant = NexoraDarkOutline.copy(alpha = 0.62f),
    surfaceContainer = NexoraDarkSurface,
    surfaceContainerHigh = NexoraDarkSurfaceHigh,
    inverseSurface = NexoraLightSurface,
    inverseOnSurface = NexoraLightOnSurface,
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
    outlineVariant = NexoraLightOutline.copy(alpha = 0.62f),
    surfaceContainer = NexoraLightSurface,
    surfaceContainerHigh = NexoraLightSurfaceHigh,
    inverseSurface = NexoraInk,
    inverseOnSurface = Color.White,
)

private val AmoledColors = DarkColors.copy(
    background = NexoraAmoled,
    surface = Color(0xFF050505),
    surfaceVariant = Color(0xFF111111),
    surfaceContainer = Color(0xFF050505),
    surfaceContainerHigh = Color(0xFF141414),
    outline = Color(0xFF2A2A2A)
)

private val NexoraTypography = Typography(
    headlineLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp
    ),
    headlineSmall = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 21.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
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
        fontSize = 12.sp,
        lineHeight = 16.sp
    )
)

private val NexoraShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(18.dp)
)

object NexoraSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
}

object NexoraRadius {
    val control = 8.dp
    val card = 12.dp
    val sheet = 16.dp
}

enum class NexoraThemeMode {
    System,
    Light,
    Dark,
    Amoled
}

@Composable
fun NexoraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeMode: NexoraThemeMode = NexoraThemeMode.System,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        NexoraThemeMode.Light -> LightColors
        NexoraThemeMode.Dark -> DarkColors
        NexoraThemeMode.Amoled -> AmoledColors
        NexoraThemeMode.System -> if (darkTheme) DarkColors else LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NexoraTypography,
        shapes = NexoraShapes,
        content = content
    )
}
