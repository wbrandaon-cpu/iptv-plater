package com.example.iptvplayer.data.model

/** Credenciais do servidor Xtream Codes. serverUrl já normalizado, ex.: http://host:8080 */
data class Credentials(
    val serverUrl: String,
    val username: String,
    val password: String
)

enum class ContentType(
    val title: String,
    val allLabel: String,
    val categoriesAction: String
) {
    LIVE("TV AO VIVO", "Todos os canais", "get_live_categories"),
    SERIES("SÉRIES", "Todas as séries", "get_series_categories"),
    MOVIE("FILMES", "Todos os filmes", "get_vod_categories");
}

/** Categoria/gênero retornado pela API. */
data class CategoryUi(val id: String, val name: String)

/** Item genérico exibido na grade (canal, filme ou série). */
data class ContentItem(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val extension: String?,
    val type: ContentType,
    /** Posição original no servidor (ordem "Padrão"). */
    val position: Int = 0,
    /** Data em que foi adicionado (Unix segundos), para ordenar por "Mais recentes". */
    val added: Long? = null
)

enum class ViewMode { GRID, LIST }

enum class SortOrder(val label: String) {
    DEFAULT("Padrão do servidor"),
    NAME_ASC("Nome (A → Z)"),
    NAME_DESC("Nome (Z → A)"),
    NEWEST("Mais recentes"),
    OLDEST("Mais antigos");
}

data class SeriesDetailUi(
    val name: String,
    val cover: String?,
    val plot: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val seasons: List<Season>
)

data class Season(val number: String, val episodes: List<EpisodeUi>)

data class EpisodeUi(
    val id: String,
    val number: String,
    val title: String,
    val extension: String,
    val image: String?,
    val duration: String?
)
