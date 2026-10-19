package com.example.iptvplayer.ui.browse

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.Star
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.logos.ChannelLogos
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import com.example.iptvplayer.data.model.ContentItem
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.data.model.SortOrder
import com.example.iptvplayer.data.model.ViewMode
import com.example.iptvplayer.ui.components.AppTopBar
import com.example.iptvplayer.ui.components.EmptyView
import com.example.iptvplayer.ui.components.ErrorView
import com.example.iptvplayer.ui.components.LoadingView
import com.example.iptvplayer.ui.components.SearchField
import com.example.iptvplayer.ui.components.focusHighlight
import com.example.iptvplayer.util.UiState

/**
 * Tela de navegação em dois painéis:
 *  - esquerda: categorias/gêneros
 *  - direita: canais, filmes ou séries da categoria selecionada (grade ou lista)
 */
@Composable
fun BrowseScreen(
    container: AppContainer,
    type: ContentType,
    onBack: () -> Unit,
    onItemClick: (ContentItem) -> Unit,
    vm: BrowseViewModel = viewModel(key = "browse_$type") {
        BrowseViewModel(container.repository, container.sessionManager, type)
    }
) {
    val viewMode by vm.viewMode.collectAsState()
    val sortOrder by vm.sortOrder.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            AppTopBar(
                title = type.title,
                subtitle = vm.selection?.name,
                onBack = onBack,
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar lista")
                    }
                    IconButton(onClick = {
                        vm.setViewMode(if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID)
                    }) {
                        Icon(
                            if (viewMode == ViewMode.GRID) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                            contentDescription = if (viewMode == ViewMode.GRID) "Ver em lista" else "Ver em grade"
                        )
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Exibição e ordenação")
                        }
                        ViewOptionsMenu(
                            expanded = menuOpen,
                            viewMode = viewMode,
                            sortOrder = sortOrder,
                            onDismiss = { menuOpen = false },
                            onViewMode = { vm.setViewMode(it); menuOpen = false },
                            onSortOrder = { vm.setSortOrder(it); menuOpen = false }
                        )
                    }
                }
            )
        }
    ) { padding ->
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val compact = maxWidth < 600.dp
            val paneWidth: Dp = if (compact) 128.dp else 280.dp

            Row(Modifier.fillMaxSize()) {
                CategoryPane(
                    vm = vm,
                    compact = compact,
                    modifier = Modifier
                        .width(paneWidth)
                        .fillMaxHeight()
                        .background(MaterialTheme.colorScheme.surface)
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outline)
                ContentPane(
                    vm = vm,
                    viewMode = viewMode,
                    sortOrder = sortOrder,
                    compact = compact,
                    onItemClick = onItemClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                )
            }
        }
    }
}

@Composable
private fun ViewOptionsMenu(
    expanded: Boolean,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    onDismiss: () -> Unit,
    onViewMode: (ViewMode) -> Unit,
    onSortOrder: (SortOrder) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        MenuHeader("Exibir como")
        DropdownMenuItem(
            text = { Text("Grade") },
            leadingIcon = { Icon(Icons.Default.GridView, null) },
            trailingIcon = { if (viewMode == ViewMode.GRID) CheckIcon() },
            onClick = { onViewMode(ViewMode.GRID) }
        )
        DropdownMenuItem(
            text = { Text("Lista") },
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ViewList, null) },
            trailingIcon = { if (viewMode == ViewMode.LIST) CheckIcon() },
            onClick = { onViewMode(ViewMode.LIST) }
        )
        HorizontalDivider()
        MenuHeader("Ordenar por")
        SortOrder.entries.forEach { order ->
            DropdownMenuItem(
                text = { Text(order.label) },
                trailingIcon = { if (order == sortOrder) CheckIcon() },
                onClick = { onSortOrder(order) }
            )
        }
    }
}

@Composable
private fun MenuHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun CheckIcon() {
    Icon(Icons.Default.Check, contentDescription = "Selecionado", tint = MaterialTheme.colorScheme.primary)
}

// ------------------------------------------------------------ Categorias

@Composable
private fun CategoryPane(vm: BrowseViewModel, compact: Boolean, modifier: Modifier) {
    val favoritesCount = vm.favorites.collectAsState().value.size
    Box(modifier) {
        when (val s = vm.categories) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(28.dp))
            }
            is UiState.Error -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(s.message, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.size(8.dp))
                Button(onClick = vm::loadCategories) { Text("Tentar") }
            }
            is UiState.Success -> LazyColumn(
                contentPadding = PaddingValues(vertical = 6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    CategoryEntry(
                        name = "★ $FAVORITES_LABEL ($favoritesCount)",
                        selected = vm.selection?.isFavorites == true,
                        compact = compact
                    ) { vm.select(Selection(FAVORITES_ID, FAVORITES_LABEL)) }
                }
                item {
                    CategoryEntry(
                        name = vm.type.allLabel,
                        selected = vm.selection?.id == null && vm.selection != null,
                        compact = compact
                    ) { vm.select(Selection(null, vm.type.allLabel)) }
                }
                items(s.data) { category ->
                    CategoryEntry(
                        name = category.name,
                        selected = vm.selection?.id == category.id,
                        compact = compact
                    ) { vm.select(Selection(category.id, category.name)) }
                }
            }
        }
    }
}

@Composable
private fun CategoryEntry(name: String, selected: Boolean, compact: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(8.dp)
    Surface(
        onClick = onClick,
        shape = shape,
        color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .focusHighlight(shape)
    ) {
        Text(
            name,
            style = if (compact) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = if (compact) 10.dp else 14.dp)
        )
    }
}

// ------------------------------------------------------------ Conteúdo

@Composable
private fun ContentPane(
    vm: BrowseViewModel,
    viewMode: ViewMode,
    sortOrder: SortOrder,
    compact: Boolean,
    onItemClick: (ContentItem) -> Unit,
    modifier: Modifier
) {
    var query by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    val favorites by vm.favorites.collectAsState()
    val favoriteIds = remember(favorites) { favorites.map { it.id }.toSet() }
    val showingFavorites = vm.selection?.isFavorites == true

    // Dois toques (ou segurar) no item: adiciona/remove dos favoritos
    val onToggleFavorite: (ContentItem) -> Unit = { item ->
        vm.toggleFavorite(item) { added ->
            Toast.makeText(
                context,
                if (added) "★ \"${item.name}\" adicionado aos favoritos"
                else "\"${item.name}\" removido dos favoritos",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Column(modifier) {
        SearchField(query, { query = it }, "Buscar nesta categoria")

        val source: UiState<List<ContentItem>> = if (showingFavorites) UiState.Success(favorites) else vm.items
        when (val s = source) {
            UiState.Loading -> LoadingView(
                if (vm.type == ContentType.LIVE) "Carregando canais..." else "Carregando..."
            )
            is UiState.Error -> ErrorView(s.message, vm::loadItems)
            is UiState.Success -> {
                val shown = remember(s.data, query, sortOrder) {
                    val q = query.trim()
                    val filtered = if (q.isEmpty()) s.data else s.data.filter { it.name.contains(q, ignoreCase = true) }
                    filtered.sortedFor(sortOrder)
                }
                Text(
                    "${shown.size} itens  •  ${sortOrder.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 2.dp)
                )
                when {
                    shown.isEmpty() -> EmptyView(
                        when {
                            showingFavorites && s.data.isEmpty() ->
                                "Nenhum favorito ainda.\n\nDê dois toques (ou segure) em um item para adicioná-lo aqui."
                            s.data.isEmpty() -> "Esta categoria está vazia."
                            else -> "Nada encontrado."
                        }
                    )
                    // key(...) recria a lista ao trocar de categoria/ordem, voltando ao topo
                    viewMode == ViewMode.GRID -> key(vm.selection, sortOrder) {
                        val isLive = vm.type == ContentType.LIVE
                        val minSize = when {
                            isLive && compact -> 96.dp
                            isLive -> 140.dp
                            compact -> 92.dp
                            else -> 120.dp
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = minSize),
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(shown, contentType = { it.type }) { item ->
                                PosterCard(
                                    item = item,
                                    isFavorite = item.id in favoriteIds,
                                    onClick = { onItemClick(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }
                        }
                    }
                    else -> key(vm.selection, sortOrder) {
                        LazyColumn(
                            contentPadding = PaddingValues(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(shown, contentType = { it.type }) { item ->
                                ListRow(
                                    item = item,
                                    isFavorite = item.id in favoriteIds,
                                    onClick = { onItemClick(item) },
                                    onToggleFavorite = { onToggleFavorite(item) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PosterCard(
    item: ContentItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isLive = item.type == ContentType.LIVE
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .padding(5.dp)
            .focusHighlight(shape)
            .clip(shape)
            .favoriteClickable(onClick, onToggleFavorite)
    ) {
        Column {
            Box {
                Thumbnail(
                    item,
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(if (isLive) 16f / 11f else 2f / 3f)
                )
                if (isFavorite) FavoriteBadge(Modifier.align(Alignment.TopEnd).padding(4.dp))
            }
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

@Composable
private fun ListRow(
    item: ContentItem,
    isFavorite: Boolean,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val isLive = item.type == ContentType.LIVE
    val shape = RoundedCornerShape(10.dp)
    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 3.dp)
            .focusHighlight(shape)
            .clip(shape)
            .favoriteClickable(onClick, onToggleFavorite)
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Thumbnail(
                item,
                Modifier
                    .width(if (isLive) 72.dp else 46.dp)
                    .aspectRatio(if (isLive) 16f / 11f else 2f / 3f)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                item.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isFavorite) FavoriteBadge()
        }
    }
}

/** Toque: abre. Dois toques ou segurar (útil no controle da TV Box): favorita. */
@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.favoriteClickable(onClick: () -> Unit, onToggleFavorite: () -> Unit): Modifier =
    combinedClickable(
        onClick = onClick,
        onDoubleClick = onToggleFavorite,
        onLongClick = onToggleFavorite
    )

@Composable
private fun FavoriteBadge(modifier: Modifier = Modifier) {
    Icon(
        Icons.Rounded.Star,
        contentDescription = "Favorito",
        tint = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .size(22.dp)
            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
            .padding(2.dp)
    )
}

@Composable
private fun Thumbnail(item: ContentItem, modifier: Modifier) {
    val isLive = item.type == ContentType.LIVE

    // Canais: tenta o logo oficial colorido (banco iptv-org) e o logo do provedor.
    // Se o nome bateu exatamente, o oficial vem primeiro; senão, o do provedor.
    val candidates = remember(item.id, item.imageUrl, item.name) {
        if (!isLive) listOfNotNull(item.imageUrl)
        else {
            val official = ChannelLogos.find(item.name)
            val ordered = if (official?.exact == true) listOf(official.url, item.imageUrl)
            else listOf(item.imageUrl, official?.url)
            ordered.filterNotNull().filter { it.isNotBlank() }.distinct()
        }
    }
    var index by remember(candidates) { mutableIntStateOf(0) }
    val url = candidates.getOrNull(index)

    // Logos de canais: fundo transparente (o logo aparece direto sobre o card).
    // Capas de filmes/séries mantêm um fundo escuro enquanto carregam.
    val background = if (isLive) Color.Transparent else Color(0xFF0F1A12)

    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(background),
        contentAlignment = Alignment.Center
    ) {
        if (url == null) {
            NamePlaceholder(item.name, isLive)
        } else {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(url)
                    .crossfade(true)
                    .build(),
                contentDescription = item.name,
                contentScale = if (isLive) ContentScale.Fit else ContentScale.Crop,
                loading = { NamePlaceholder(item.name, isLive) },
                error = {
                    // Falhou: tenta a próxima opção de logo, ou mostra as iniciais
                    if (index < candidates.lastIndex) {
                        LaunchedEffect(index) { index++ }
                    }
                    NamePlaceholder(item.name, isLive)
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(if (isLive) 8.dp else 0.dp)
            )
        }
    }
}

/** Sem logo: mostra as iniciais do canal em verde (em vez de um ícone apagado). */
@Composable
private fun NamePlaceholder(name: String, isLive: Boolean) {
    val initials = remember(name) {
        ChannelLogos.tokens(name).take(2).joinToString("") { it.take(1) }.uppercase()
            .ifEmpty { name.take(2).uppercase() }
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            initials,
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold
        )
    }
}
