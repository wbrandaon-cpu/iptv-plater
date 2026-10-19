package com.example.iptvplayer.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.model.ContentType
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpen: (ContentType) -> Unit,
    onLoggedOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val credentials by container.sessionManager.credentials.collectAsState(initial = null)
    val expDate by container.sessionManager.expDate.collectAsState(initial = null)
    var confirmLogout by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("IPTV Player", fontWeight = FontWeight.Bold)
                        val subtitle = buildString {
                            credentials?.let { append("Olá, ${it.username}") }
                            formatExpiration(expDate)?.let { append("  •  $it") }
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { confirmLogout = true }) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            val isWide = maxWidth > maxHeight
            if (isWide) {
                // Paisagem / TV: cards lado a lado
                Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    entries.forEach { e ->
                        DashboardCard(e, Modifier.weight(1f).fillMaxHeight()) { onOpen(e.type) }
                    }
                }
            } else {
                // Retrato: cards empilhados
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    entries.forEach { e ->
                        DashboardCard(e, Modifier.weight(1f).fillMaxWidth()) { onOpen(e.type) }
                    }
                }
            }
        }
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
private fun DashboardCard(entry: DashboardEntry, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(entry.gradient)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(entry.icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    entry.type.title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

/** exp_date vem como timestamp Unix (segundos) ou null para contas sem vencimento. */
private fun formatExpiration(expDate: String?): String? {
    val seconds = expDate?.toLongOrNull() ?: return null
    val fmt = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR"))
    return "Válido até ${fmt.format(Date(seconds * 1000))}"
}
