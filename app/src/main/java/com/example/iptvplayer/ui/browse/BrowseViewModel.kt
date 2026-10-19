package com.example.iptvplayer.ui.browse

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.local.SessionManager
import com.example.iptvplayer.data.model.CategoryUi
import com.example.iptvplayer.data.model.ContentItem
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.data.model.SortOrder
import com.example.iptvplayer.data.model.ViewMode
import com.example.iptvplayer.data.repository.XtreamRepository
import com.example.iptvplayer.util.UiState
import com.example.iptvplayer.util.loadState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categoria selecionada. id = null significa "Todos"; FAVORITES_ID, os favoritos. */
data class Selection(val id: String?, val name: String) {
    val isFavorites: Boolean get() = id == FAVORITES_ID
}

const val FAVORITES_ID = "__favoritos__"
const val FAVORITES_LABEL = "FAVORITOS"

/**
 * Estado da tela de navegação: categorias à esquerda e conteúdo da
 * categoria selecionada à direita.
 */
class BrowseViewModel(
    private val repository: XtreamRepository,
    private val session: SessionManager,
    val type: ContentType
) : ViewModel() {

    var categories by mutableStateOf<UiState<List<CategoryUi>>>(UiState.Loading)
        private set

    var selection by mutableStateOf<Selection?>(null)
        private set

    var items by mutableStateOf<UiState<List<ContentItem>>>(UiState.Loading)
        private set

    val viewMode: StateFlow<ViewMode> =
        session.viewMode.stateIn(viewModelScope, SharingStarted.Eagerly, ViewMode.GRID)

    val sortOrder: StateFlow<SortOrder> =
        session.sortOrder.stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.DEFAULT)

    /** Favoritos só deste tipo (canais, filmes ou séries), atualizados na hora. */
    val favorites: StateFlow<List<ContentItem>> = session.favorites
        .map { list -> list.filter { it.type == type } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var itemsJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        categories = UiState.Loading
        viewModelScope.launch {
            val result = loadState { repository.loadCategories(type) }
            categories = result
            // Primeira abertura: abre os FAVORITOS (se houver) ou a 1ª categoria
            if (result is UiState.Success && selection == null) {
                val hasFavorites = session.favorites.first().any { it.type == type }
                val first = result.data.firstOrNull()
                select(
                    when {
                        hasFavorites -> Selection(FAVORITES_ID, FAVORITES_LABEL)
                        first != null -> Selection(first.id, first.name)
                        else -> Selection(null, type.allLabel)
                    }
                )
            }
        }
    }

    fun select(newSelection: Selection) {
        selection = newSelection
        loadItems()
    }

    fun loadItems() {
        val current = selection ?: return
        itemsJob?.cancel()
        // Favoritos vêm do aparelho (StateFlow acima), não do servidor
        if (current.isFavorites) {
            items = UiState.Success(emptyList())
            return
        }
        items = UiState.Loading
        itemsJob = viewModelScope.launch {
            items = loadState { repository.loadItems(type, current.id) }
        }
    }

    /** Botão "Atualizar": busca categorias e conteúdo de novo no servidor. */
    fun refresh() {
        repository.clearCache()
        loadCategories()
        loadItems()
    }

    /** Dois toques no item: adiciona/remove dos favoritos. */
    fun toggleFavorite(item: ContentItem, onResult: (added: Boolean) -> Unit) {
        viewModelScope.launch { onResult(session.toggleFavorite(item)) }
    }

    fun setViewMode(mode: ViewMode) {
        viewModelScope.launch { session.setViewMode(mode) }
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { session.setSortOrder(order) }
    }
}

/** Aplica a ordenação escolhida. */
fun List<ContentItem>.sortedFor(order: SortOrder): List<ContentItem> = when (order) {
    SortOrder.DEFAULT -> sortedBy { it.position }
    SortOrder.NAME_ASC -> sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortOrder.NAME_DESC -> sortedWith(compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name })
    SortOrder.NEWEST -> sortedByDescending { it.added ?: 0L }
    SortOrder.OLDEST -> sortedBy { it.added ?: Long.MAX_VALUE }
}
