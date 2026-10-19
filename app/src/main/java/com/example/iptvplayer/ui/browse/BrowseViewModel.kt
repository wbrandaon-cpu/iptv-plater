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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Categoria selecionada. id = null significa "Todos". */
data class Selection(val id: String?, val name: String)

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

    private var itemsJob: Job? = null

    init {
        loadCategories()
    }

    fun loadCategories() {
        categories = UiState.Loading
        viewModelScope.launch {
            val result = loadState { repository.loadCategories(type) }
            categories = result
            // Primeira abertura: já seleciona a 1ª categoria para mostrar conteúdo ao lado
            if (result is UiState.Success && selection == null) {
                val first = result.data.firstOrNull()
                select(if (first != null) Selection(first.id, first.name) else Selection(null, type.allLabel))
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
