package com.example.iptvplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.iptvplayer.BuildConfig

/**
 * O app é gerado em duas versões (dois aplicativos separados):
 *  - "verde": preto + verde
 *  - "rosa":  preto + rosa
 * A cor vem do BuildConfig.THEME_COLOR, definido em app/build.gradle.kts.
 */
private val IsPink = BuildConfig.THEME_COLOR == "rosa"

val Black = Color(0xFF000000)

// Cor de destaque
val Accent = if (IsPink) Color(0xFFFF4FA3) else Color(0xFF00E676)
val AccentDark = if (IsPink) Color(0xFFC2185B) else Color(0xFF00A152)
val AccentDeep = if (IsPink) Color(0xFF4A0E2B) else Color(0xFF003D1F)

// Fundos dos painéis (levemente tingidos com a cor do app)
val Panel = if (IsPink) Color(0xFF110A0E) else Color(0xFF0B0F0C)
val PanelLight = if (IsPink) Color(0xFF1E1218) else Color(0xFF141B16)
val ThumbBackground = if (IsPink) Color(0xFF1C0F16) else Color(0xFF0F1A12)
val ShortcutBackground = if (IsPink) Color(0xFF6B2A4B) else Color(0xFF2E5E45)

// Cards da tela inicial: cor viva no topo, escurecendo para baixo (texto branco legível)
val LiveGradient =
    if (IsPink) listOf(Color(0xFFFF4FA3), Color(0xFF5C0F35)) else listOf(Color(0xFF00C853), Color(0xFF00473A))
val SeriesGradient =
    if (IsPink) listOf(Color(0xFFE0569E), Color(0xFF3F0D2A)) else listOf(Color(0xFF00A88F), Color(0xFF003B36))
val MoviesGradient =
    if (IsPink) listOf(Color(0xFFF06292), Color(0xFF4A1430)) else listOf(Color(0xFF4CAF50), Color(0xFF173F1A))

private val DarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = Black,
    primaryContainer = AccentDeep,
    onPrimaryContainer = Accent,
    secondary = AccentDark,
    onSecondary = Black,
    background = Black,
    onBackground = if (IsPink) Color(0xFFFCE4EC) else Color(0xFFE8F5E9),
    surface = Panel,
    onSurface = if (IsPink) Color(0xFFFCE4EC) else Color(0xFFE8F5E9),
    surfaceVariant = PanelLight,
    onSurfaceVariant = if (IsPink) Color(0xFFC9A3B6) else Color(0xFF9EB8A5),
    surfaceContainer = Panel,
    surfaceContainerHigh = PanelLight,
    outline = if (IsPink) Color(0xFF5A2E44) else Color(0xFF2E4A36),
    error = Color(0xFFFF6B6B)
)

/** Tema escuro com a cor da versão (verde ou rosa). */
@Composable
fun IptvTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
