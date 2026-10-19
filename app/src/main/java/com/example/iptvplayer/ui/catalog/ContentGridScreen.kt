package com.example.iptvplayer.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.model.ContentItem
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.ui.components.BackTopBar
import com.example.iptvplayer.ui.components.EmptyView
import com.example.iptvplayer.ui.components.ErrorView
import com.example.iptvplayer.ui.components.LoadingView
import com.example.iptvplayer.ui.components.SearchField
import com.example.iptvplayer.util.UiState

/** Grade com logos dos canais ou capas de filmes/séries de uma categoria. */
@Composable
fun ContentGridScreen(
    container: AppContainer,
    type: ContentType,
    categoryId: String?,
    title: String,
    onBack: () -> Unit,
    onItemClick: (ContentItem) -> Unit,
    vm: ContentViewModel = viewModel(key = "content_${type}_$categoryId") {
        ContentViewModel(container.repository, type, categoryId)
    }
) {
    var query by rememberSaveable { mutableStateOf("") }
    val isLive = type == ContentType.LIVE

    Scaffold(topBar = { BackTopBar(title.ifBlank { type.title }, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = vm.state) {
                UiState.Loading -> LoadingView(if (isLive) "Carregando canais..." else "Carregando catálogo...")
                is UiState.Error -> ErrorView(s.message, vm::load)
                is UiState.Success -> {
                    val filtered = remember(s.data, query) {
                        if (query.isBlank()) s.data
                        else s.data.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    }
                    SearchField(query, { query = it }, "Buscar em ${s.data.size} itens")

                    if (filtered.isEmpty()) {
                        EmptyView(if (s.data.isEmpty()) "Esta categoria está vazia." else "Nada encontrado.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = if (isLive) 130.dp else 115.dp),
                            contentPadding = PaddingValues(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Sem "key": alguns painéis repetem IDs, o que derrubaria o LazyGrid.
                            items(filtered, contentType = { it.type }) { item ->
                                PosterCard(item) { onItemClick(item) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(item: ContentItem, onClick: () -> Unit) {
    val isLive = item.type == ContentType.LIVE
    val context = LocalContext.current
    val fallback = rememberVectorPainter(if (isLive) Icons.Rounded.LiveTv else Icons.Rounded.Movie)

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.padding(6.dp)
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(item.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                placeholder = fallback,
                error = fallback,
                fallback = fallback,
                // Logos de canais: inteiros (Fit). Capas: preenchendo (Crop) em 2:3.
                contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(if (isLive) 16f / 10f else 2f / 3f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(if (isLive) 12.dp else 0.dp)
            )
            Text(
                item.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
            )
        }
    }
}
