package com.example.iptvplayer.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.BuildConfig
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.data.remote.UpdateChecker
import com.example.iptvplayer.ui.components.AppTopBar
import com.example.iptvplayer.ui.components.focusHighlight
import com.example.iptvplayer.ui.theme.LiveGradient
import com.example.iptvplayer.ui.theme.MoviesGradient
import com.example.iptvplayer.ui.theme.SeriesGradient
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DashboardEntry(
    val type: ContentType,
    val icon: ImageVector,
    val gradient: List<Color>
)

private val entries = listOf(
    DashboardEntry(ContentType.LIVE, Icons.Rounded.LiveTv, LiveGradient),
    DashboardEntry(ContentType.SERIES, Icons.Rounded.VideoLibrary, SeriesGradient),
    DashboardEntry(ContentType.MOVIE, Icons.Rounded.Movie, MoviesGradient)
)

@Composable
fun HomeScreen(
    container: AppContainer,
    onOpen: (ContentType) -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val snackbar = remember { SnackbarHostState() }
    val credentials by container.sessionManager.credentials.collectAsState(initial = null)
    val expDate by container.sessionManager.expDate.collectAsState(initial = null)
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    var showConnectionTest by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateChecker.UpdateInfo?>(null) }

    // Verifica atualização do app ao abrir a tela inicial (silencioso)
    LaunchedEffect(Unit) {
        update = UpdateChecker.latest()?.takeIf { it.isNewer }
    }

    val subtitle = buildString {
        credentials?.let { append("Olá, ${it.username}") }
        formatExpiration(expDate)?.let { append("  •  $it") }
        append("  •  v${BuildConfig.VERSION_NAME}")
    }

    val checkUpdate: () -> Unit = {
        scope.launch { snackbar.showSnackbar("Verificando atualizações...") }
        scope.launch {
            val latest = UpdateChecker.latest()
            snackbar.currentSnackbarData?.dismiss()
            when {
                latest == null -> snackbar.showSnackbar("Não foi possível verificar agora.")
                latest.isNewer -> update = latest
                else -> snackbar.showSnackbar("Você já está na versão mais recente (v${BuildConfig.VERSION_NAME}).")
            }
        }
    }
    val refreshLists: () -> Unit = {
        container.repository.clearCache()
        scope.launch { snackbar.showSnackbar("Pronto! As listas serão baixadas de novo do servidor.") }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            AppTopBar(
                title = "IPTV Player",
                subtitle = subtitle,
                actions = {
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            update?.let { info ->
                UpdateBanner(info) { uriHandler.openUri(UpdateChecker.DOWNLOAD_URL) }
            }

            BoxWithConstraints(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                val gap = 14.dp
                val isWide = maxWidth > maxHeight

                val live = @Composable { m: Modifier ->
                    DashboardCard(entries[0], big = true, modifier = m) { onOpen(ContentType.LIVE) }
                }
                val movies = @Composable { m: Modifier ->
                    DashboardCard(entries[2], big = false, modifier = m) { onOpen(ContentType.MOVIE) }
                }
                val series = @Composable { m: Modifier ->
                    DashboardCard(entries[1], big = false, modifier = m) { onOpen(ContentType.SERIES) }
                }
                val shortcuts = @Composable { m: Modifier ->
                    Row(m, horizontalArrangement = Arrangement.spacedBy(gap)) {
                        ShortcutButton("ATUALIZAR\nLISTAS", Icons.Default.Refresh, Modifier.weight(1f).fillMaxHeight(), refreshLists)
                        ShortcutButton("ATUALIZAR\nAPP", Icons.Default.SystemUpdate, Modifier.weight(1f).fillMaxHeight(), checkUpdate)
                        ShortcutButton("TESTAR\nCONEXÃO", Icons.Default.NetworkCheck, Modifier.weight(1f).fillMaxHeight()) { showConnectionTest = true }
                        ShortcutButton("SAIR", Icons.AutoMirrored.Filled.Logout, Modifier.weight(1f).fillMaxHeight()) { confirmLogout = true }
                    }
                }

                if (isWide) {
                    // Paisagem / TV Box: TV AO VIVO grande à esquerda;
                    // FILMES e SÉRIES à direita e atalhos menores embaixo deles.
                    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        live(Modifier.weight(1f).fillMaxHeight())
                        Column(Modifier.weight(1.45f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                            Row(Modifier.fillMaxWidth().weight(3f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                                movies(Modifier.weight(1f).fillMaxHeight())
                                series(Modifier.weight(1f).fillMaxHeight())
                            }
                            shortcuts(Modifier.fillMaxWidth().weight(1f))
                        }
                    }
                } else {
                    // Retrato (celular em pé): TV AO VIVO em cima, FILMES e SÉRIES lado a lado, atalhos embaixo
                    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(gap)) {
                        live(Modifier.fillMaxWidth().weight(1.2f))
                        Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(gap)) {
                            movies(Modifier.weight(1f).fillMaxHeight())
                            series(Modifier.weight(1f).fillMaxHeight())
                        }
                        shortcuts(Modifier.fillMaxWidth().height(72.dp))
                    }
                }
            }
        }
    }

    if (showConnectionTest) {
        ConnectionTestDialog(credentials) { showConnectionTest = false }
    }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("Sair da conta?") },
            text = { Text("Você precisará digitar usuário e senha novamente.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    scope.launch {
                        container.sessionManager.logout()
                        container.repository.logout()
                        onLoggedOut()
                    }
                }) { Text("Sair") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun UpdateBanner(info: UpdateChecker.UpdateInfo, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .focusHighlight(shape)
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.SystemUpdate, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Nova versão v${info.versionName} disponível", fontWeight = FontWeight.Bold)
                Text("Toque para baixar e instalar a atualização", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun DashboardCard(entry: DashboardEntry, big: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Surface(onClick = onClick, modifier = modifier.focusHighlight(shape), shape = shape) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(entry.gradient)),
            contentAlignment = Alignment.Center
        ) {
            // Tudo é proporcional ao tamanho do card: ícone e nome preenchem o espaço
            val density = LocalDensity.current
            val side = minOf(maxWidth, maxHeight)
            val circle = side * (if (big) 0.58f else 0.55f)
            val iconSize = circle * 0.62f
            val label = entry.type.title
            // Fonte limitada pela largura (para caber o nome) e pela altura do card
            val byWidth = maxWidth * 0.86f / (label.length * 0.68f)
            val byHeight = maxHeight * 0.16f
            val fontSize = with(density) { minOf(byWidth, byHeight).coerceAtLeast(9.dp).toSp() }
            val spacing = maxHeight * 0.05f

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Ícone dentro de um círculo com contorno, como no modelo
                Box(
                    Modifier
                        .size(circle)
                        .border(maxOf(3.dp, circle * 0.04f), Color.White, CircleShape)
                        .background(Color.White.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        entry.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(iconSize)
                    )
                }
                Spacer(Modifier.height(spacing))
                Text(
                    label,
                    color = Color.White,
                    fontSize = fontSize,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/** Botões menores da linha de baixo (atalhos): ícone e texto preenchem o botão. */
@Composable
private fun ShortcutButton(label: String, icon: ImageVector, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = ShortcutColor,
        contentColor = Color.White,
        modifier = modifier.focusHighlight(shape)
    ) {
        BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val density = LocalDensity.current
            val lines = label.lines()
            val longest = lines.maxOf { it.length }
            val iconSize = maxHeight * 0.5f
            // Espaço para o texto: largura total menos o ícone e margens
            val textWidth = (maxWidth - iconSize - 28.dp).coerceAtLeast(40.dp)
            val byWidth = textWidth / (longest * 0.66f)
            val byHeight = maxHeight * 0.78f / (lines.size * 1.15f)
            val fontSize = with(density) { minOf(byWidth, byHeight).coerceAtLeast(9.dp).toSp() }

            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    fontSize = fontSize,
                    lineHeight = fontSize * 1.1f,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = lines.size,
                    softWrap = false
                )
            }
        }
    }
}

private val ShortcutColor = Color(0xFF2E5E45)

/** exp_date vem como timestamp Unix (segundos) ou null para contas sem vencimento. */
private fun formatExpiration(expDate: String?): String? {
    val seconds = expDate?.toLongOrNull() ?: return null
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return "Válido até ${fmt.format(Date(seconds * 1000))}"
}
