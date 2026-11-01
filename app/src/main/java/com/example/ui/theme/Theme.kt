package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = AmpGold,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF382900),
    onPrimaryContainer = Color(0xFFFFD54F),
    secondary = PhosphorCyan,
    onSecondary = Color.Black,
    tertiary = TubeAmber,
    background = PedalboardDark,
    onBackground = Color(0xFFE0E3EB),
    surface = ChassisSurface,
    onSurface = Color(0xFFE0E3EB),
    surfaceVariant = Color(0xFF22262E),
    onSurfaceVariant = Color(0xFFB0BEC5),
    error = TubeRed
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
