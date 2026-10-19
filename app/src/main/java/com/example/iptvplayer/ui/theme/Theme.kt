package com.example.iptvplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Paleta preto + verde
val Green = Color(0xFF00E676)
val GreenDark = Color(0xFF00A152)
val GreenDeep = Color(0xFF003D1F)
val Black = Color(0xFF000000)
val Panel = Color(0xFF0B0F0C)
val PanelLight = Color(0xFF141B16)

// Cards da tela inicial: verdes vivos no topo, escurecendo para baixo (texto branco legível)
val LiveGradient = listOf(Color(0xFF00C853), Color(0xFF00473A))
val SeriesGradient = listOf(Color(0xFF00A88F), Color(0xFF003B36))
val MoviesGradient = listOf(Color(0xFF4CAF50), Color(0xFF173F1A))

private val DarkColors = darkColorScheme(
    primary = Green,
    onPrimary = Black,
    primaryContainer = GreenDeep,
    onPrimaryContainer = Green,
    secondary = GreenDark,
    onSecondary = Black,
    background = Black,
    onBackground = Color(0xFFE8F5E9),
    surface = Panel,
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = PanelLight,
    onSurfaceVariant = Color(0xFF9EB8A5),
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelLight,
    outline = Color(0xFF2E4A36),
    error = Color(0xFFFF6B6B)
)

/** Tema escuro preto e verde (melhor para assistir vídeo e para TV Box). */
@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
