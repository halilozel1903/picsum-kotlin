package io.github.halilozel1903.picsum.sample

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF2E5AAC),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9E2FF),
    onPrimaryContainer = Color(0xFF001A43),
    secondary = Color(0xFF575E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCE2F9),
    onSecondaryContainer = Color(0xFF141B2C),
    tertiary = Color(0xFFB4561F),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBCA),
    onTertiaryContainer = Color(0xFF341000),
    background = Color(0xFFF7F7F5),
    onBackground = Color(0xFF1A1B1E),
    surface = Color(0xFFF7F7F5),
    onSurface = Color(0xFF1A1B1E),
    surfaceVariant = Color(0xFFE3E3E0),
    onSurfaceVariant = Color(0xFF45464A),
    surfaceContainerLow = Color(0xFFF1F1EF),
    surfaceContainer = Color(0xFFECECE9),
    surfaceContainerHigh = Color(0xFFE6E6E3),
    outline = Color(0xFF75767A),
    outlineVariant = Color(0xFFC6C6C9),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFAFC6FF),
    onPrimary = Color(0xFF002E6B),
    primaryContainer = Color(0xFF0D4392),
    onPrimaryContainer = Color(0xFFD9E2FF),
    secondary = Color(0xFFC0C6DC),
    onSecondary = Color(0xFF2A3042),
    secondaryContainer = Color(0xFF404659),
    onSecondaryContainer = Color(0xFFDCE2F9),
    tertiary = Color(0xFFFFB68F),
    onTertiary = Color(0xFF552000),
    tertiaryContainer = Color(0xFF8E3F0A),
    onTertiaryContainer = Color(0xFFFFDBCA),
    background = Color(0xFF111214),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF111214),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF2B2C30),
    onSurfaceVariant = Color(0xFFC6C6CA),
    surfaceContainerLow = Color(0xFF17181B),
    surfaceContainer = Color(0xFF1C1D20),
    surfaceContainerHigh = Color(0xFF26272A),
    outline = Color(0xFF8F9094),
    outlineVariant = Color(0xFF45464A),
)

@Composable
fun GalleryTheme(dark: Boolean, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, content = content)
}
