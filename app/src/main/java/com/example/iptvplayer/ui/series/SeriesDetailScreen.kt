package com.example.iptvplayer.ui.series

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.model.EpisodeUi
import com.example.iptvplayer.data.model.SeriesDetailUi
import com.example.iptvplayer.data.repository.XtreamRepository
import com.example.iptvplayer.ui.components.AppTopBar
import com.example.iptvplayer.ui.components.EmptyView
import com.example.iptvplayer.ui.components.ErrorView
import com.example.iptvplayer.ui.components.LoadingView
import com.example.iptvplayer.util.UiState
import com.example.iptvplayer.util.loadState
import kotlinx.coroutines.launch

class SeriesDetailViewModel(
    private val repository: XtreamRepository,
    private val seriesId: String
) : ViewModel() {

    var state by mutableStateOf<UiState<SeriesDetailUi>>(UiState.Loading)
        private set

    init { load() }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch { state = loadState { repository.loadSeriesInfo(seriesId) } }
    }
}

/** Detalhes da série: capa, sinopse, abas de temporadas e lista de episódios. */
@Composable
fun SeriesDetailScreen(
    container: AppContainer,
    seriesId: String,
    fallbackTitle: String,
    onBack: () -> Unit,
    onEpisodeClick: (episode: EpisodeUi, seriesName: String) -> Unit,
    vm: SeriesDetailViewModel = viewModel(key = "series_$seriesId") {
        SeriesDetailViewModel(container.repository, seriesId)
    }
) {
    var selectedSeason by rememberSaveable { mutableIntStateOf(0) }

    val current = vm.state
    val title = if (current is UiState.Success) current.data.name.ifBlank { fallbackTitle } else fallbackTitle

    Scaffold(topBar = { AppTopBar(title = title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = vm.state) {
                UiState.Loading -> LoadingView("Carregando episódios...")
                is UiState.Error -> ErrorView(s.message, vm::load)
                is UiState.Success -> {
                    val series = s.data
                    val seasonIndex = selectedSeason.coerceIn(0, (series.seasons.size - 1).coerceAtLeast(0))

                    LazyColumn(Modifier.fillMaxSize()) {
                        item { SeriesHeader(series) }

                        if (series.seasons.isEmpty()) {
                            item { EmptyView("Nenhum episódio disponível.") }
                        } else {
                            item {
                                ScrollableTabRow(selectedTabIndex = seasonIndex, edgePadding = 12.dp) {
                                    series.seasons.forEachIndexed { index, season ->
                                        Tab(
                                            selected = index == seasonIndex,
                                            onClick = { selectedSeason = index },
                                            text = { Text("Temporada ${season.number}") }
                                        )
                                    }
                                }
                            }
                            items(series.seasons[seasonIndex].episodes) { episode ->
                                EpisodeRow(episode) { onEpisodeClick(episode, series.name.ifBlank { fallbackTitle }) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeriesHeader(series: SeriesDetailUi) {
    val fallback = rememberVectorPainter(Icons.Rounded.VideoLibrary)
    Row(Modifier.padding(16.dp)) {
        AsyncImage(
            model = series.cover,
            contentDescription = series.name,
            placeholder = fallback,
            error = fallback,
            fallback = fallback,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .width(120.dp)
                .aspectRatio(2f / 3f)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Spacer(Modifier.width(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(series.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            val meta = listOfNotNull(series.releaseDate, series.genre, series.rating?.let { "★ $it" })
            if (meta.isNotEmpty()) {
                Text(
                    meta.joinToString("  •  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            series.plot?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: EpisodeUi, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = episode.image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(120.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "E${episode.number}  ${episode.title}",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.SemiBold
                )
                episode.duration?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Rounded.PlayCircle, contentDescription = "Assistir", tint = MaterialTheme.colorScheme.primary)
        }
    }
}
