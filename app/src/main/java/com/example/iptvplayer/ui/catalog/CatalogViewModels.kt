package com.example.iptvplayer.ui.catalog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.model.CategoryUi
import com.example.iptvplayer.data.model.ContentItem
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.data.repository.XtreamRepository
import com.example.iptvplayer.util.UiState
import com.example.iptvplayer.util.loadState
import kotlinx.coroutines.launch

class CategoriesViewModel(
    private val repository: XtreamRepository,
    private val type: ContentType
) : ViewModel() {

    var state by mutableStateOf<UiState<List<CategoryUi>>>(UiState.Loading)
        private set

    init { load() }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch { state = loadState { repository.loadCategories(type) } }
    }
}

class ContentViewModel(
    private val repository: XtreamRepository,
    private val type: ContentType,
    private val categoryId: String?
) : ViewModel() {

    var state by mutableStateOf<UiState<List<ContentItem>>>(UiState.Loading)
        private set

    init { load() }

    fun load() {
        state = UiState.Loading
        viewModelScope.launch { state = loadState { repository.loadItems(type, categoryId) } }
    }
}
