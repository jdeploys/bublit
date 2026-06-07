package com.bublit.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF25686F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFBDECF1),
    onPrimaryContainer = Color(0xFF082F35),
    secondary = Color(0xFF7A5B2E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDDB0),
    onSecondaryContainer = Color(0xFF2B1700),
    tertiary = Color(0xFF7C5265),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD8E6),
    onTertiaryContainer = Color(0xFF321020),
    background = Color(0xFFF7F7F2),
    onBackground = Color(0xFF1D1D1B),
    surface = Color(0xFFFFFBF7),
    onSurface = Color(0xFF1D1D1B),
    surfaceVariant = Color(0xFFE0E4DF),
    onSurfaceVariant = Color(0xFF434842),
    outline = Color(0xFF73796F),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9DD7DD),
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFFBDECF1),
    secondary = Color(0xFFEFC18A),
    onSecondary = Color(0xFF452B08),
    secondaryContainer = Color(0xFF5F421C),
    onSecondaryContainer = Color(0xFFFFDDB0),
    tertiary = Color(0xFFF0B7CD),
    onTertiary = Color(0xFF4A2535),
    tertiaryContainer = Color(0xFF623B4C),
    onTertiaryContainer = Color(0xFFFFD8E6),
    background = Color(0xFF131413),
    onBackground = Color(0xFFE5E3DE),
    surface = Color(0xFF1B1C1A),
    onSurface = Color(0xFFE5E3DE),
    surfaceVariant = Color(0xFF434842),
    onSurfaceVariant = Color(0xFFC3C8BE),
    outline = Color(0xFF8D9388),
)

@Composable
fun BublitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
