package com.example.iptvplayer.ui.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.ui.components.BackTopBar
import com.example.iptvplayer.ui.components.EmptyView
import com.example.iptvplayer.ui.components.ErrorView
import com.example.iptvplayer.ui.components.LoadingView
import com.example.iptvplayer.ui.components.SearchField
import com.example.iptvplayer.util.UiState

/**
 * Lista as categorias/gêneros de TV ao vivo, filmes ou séries.
 * O primeiro item ("Todos...") abre todo o conteúdo daquele tipo.
 */
@Composable
fun CategoriesScreen(
    container: AppContainer,
    type: ContentType,
    onBack: () -> Unit,
    onCategoryClick: (categoryId: String?, name: String) -> Unit,
    vm: CategoriesViewModel = viewModel(key = "categories_$type") {
        CategoriesViewModel(container.repository, type)
    }
) {
    var query by rememberSaveable { mutableStateOf("") }

    Scaffold(topBar = { BackTopBar(type.title, onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when (val s = vm.state) {
                UiState.Loading -> LoadingView("Carregando categorias...")
                is UiState.Error -> ErrorView(s.message, vm::load)
                is UiState.Success -> {
                    val filtered = remember(s.data, query) {
                        if (query.isBlank()) s.data
                        else s.data.filter { it.name.contains(query.trim(), ignoreCase = true) }
                    }
                    SearchField(query, { query = it }, "Buscar categoria")

                    if (s.data.isEmpty()) {
                        EmptyView("Nenhuma categoria disponível no seu plano.")
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 260.dp),
                            contentPadding = PaddingValues(12.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            if (query.isBlank()) {
                                item {
                                    CategoryRow(type.allLabel, Icons.Default.Apps, highlighted = true) {
                                        onCategoryClick(null, type.allLabel)
                                    }
                                }
                            }
                            items(filtered) { category ->
                                CategoryRow(category.name, Icons.Default.Folder) {
                                    onCategoryClick(category.id, category.name)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryRow(
    name: String,
    icon: ImageVector,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (highlighted) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Text(
                name,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (highlighted) FontWeight.Bold else FontWeight.Normal
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}
