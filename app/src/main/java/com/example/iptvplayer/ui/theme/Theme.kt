package com.example.iptvplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val AccentRed = Color(0xFFE50914)
val LiveGradient = listOf(Color(0xFFE50914), Color(0xFF8E0E00))
val SeriesGradient = listOf(Color(0xFF6A11CB), Color(0xFF2575FC))
val MoviesGradient = listOf(Color(0xFFF7971E), Color(0xFFD35400))

private val DarkColors = darkColorScheme(
    primary = AccentRed,
    onPrimary = Color.White,
    secondary = Color(0xFF2575FC),
    background = Color(0xFF0F0F12),
    onBackground = Color(0xFFECECEC),
    surface = Color(0xFF17171C),
    onSurface = Color(0xFFECECEC),
    surfaceVariant = Color(0xFF24242B),
    onSurfaceVariant = Color(0xFFB8B8C2),
    error = Color(0xFFFF6B6B)
)

/** App de IPTV: tema escuro sempre (melhor para assistir vídeo). */
@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
