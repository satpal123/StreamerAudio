package com.player.streameraudio.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val Purple = Color(0xFF9147FF)
private val DarkScheme = darkColorScheme(
    primary = Color(0xFFBE9BFF), onPrimary = Color(0xFF2D0068),
    primaryContainer = Color(0xFF39225C), onPrimaryContainer = Color(0xFFEBDDFF),
    secondary = Color(0xFFC9B8E8), secondaryContainer = Color(0xFF332A40),
    tertiary = Color(0xFFFFA7C4), background = Color(0xFF0E0D12),
    surface = Color(0xFF0E0D12), surfaceVariant = Color(0xFF49454F),
    outline = Color(0xFF958E9D), error = Color(0xFFFFB4AB)
)
private val LightScheme = lightColorScheme(
    primary = Purple, onPrimary = Color.White,
    primaryContainer = Color(0xFFEBDDFF), onPrimaryContainer = Color(0xFF2B0052),
    secondary = Color(0xFF675A73), secondaryContainer = Color(0xFFEEE0F7),
    tertiary = Color(0xFF8C4960), background = Color(0xFFFFF8FF),
    surface = Color(0xFFFFF8FF), surfaceVariant = Color(0xFFE9E0EA),
    outline = Color(0xFF7B747E)
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(28.dp)
)

@Composable
fun StreamerAudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        shapes = AppShapes,
        content = content
    )
}
