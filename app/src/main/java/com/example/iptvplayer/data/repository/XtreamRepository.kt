package com.example.iptvplayer.data.repository

import android.net.Uri
import com.example.iptvplayer.data.model.CategoryUi
import com.example.iptvplayer.data.model.ContentItem
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.data.model.Credentials
import com.example.iptvplayer.data.model.EpisodeUi
import com.example.iptvplayer.data.model.Season
import com.example.iptvplayer.data.model.SeriesDetailUi
import com.example.iptvplayer.data.remote.XtreamApi
import com.example.iptvplayer.data.remote.dto.EpisodeDto
import com.example.iptvplayer.data.remote.dto.SeriesInfoDto
import com.example.iptvplayer.data.remote.dto.UserInfoDto
import com.example.iptvplayer.util.AppException
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.util.concurrent.ConcurrentHashMap

/**
 * Fonte única de dados do app: fala com a API Xtream Codes, mantém a sessão
 * em memória e guarda um cache simples das listas já baixadas.
 */
class XtreamRepository(private val api: XtreamApi) {

    private val gson = Gson()
    private val cache = ConcurrentHashMap<String, Any>()

    @Volatile
    var credentials: Credentials? = null
        set(value) {
            field = value
            cache.clear()
        }

    private val creds: Credentials
        get() = credentials ?: throw AppException("Sessão expirada. Faça login novamente.")

    private fun apiUrl(c: Credentials) = "${c.serverUrl}/player_api.php"

    // ------------------------------------------------------------------ Login

    /** Valida usuário/senha no servidor. Lança AppException em caso de falha. */
    suspend fun login(c: Credentials): UserInfoDto {
        val response = api.authenticate(apiUrl(c), c.username, c.password)
        val info = response.userInfo
            ?: throw AppException("O servidor não respondeu como um painel Xtream Codes.")
        if (info.auth != "1") throw AppException("Usuário ou senha inválidos.")
        val status = info.status
        if (status != null && !status.equals("Active", ignoreCase = true)) {
            throw AppException("Conta com status \"$status\". Contate seu provedor.")
        }
        credentials = c
        return info
    }

    fun logout() {
        credentials = null
    }

    // ------------------------------------------------------------- Catálogo

    suspend fun loadCategories(type: ContentType): List<CategoryUi> = cached("cat_$type") {
        val c = creds
        api.getCategories(apiUrl(c), c.username, c.password, type.categoriesAction)
            .mapNotNull { dto ->
                val id = dto.id ?: return@mapNotNull null
                CategoryUi(id, dto.name?.trim().orEmpty().ifEmpty { "Sem nome" })
            }
    }

    /** categoryId = null carrega todo o conteúdo do tipo. */
    suspend fun loadItems(type: ContentType, categoryId: String?): List<ContentItem> =
        cached("items_${type}_${categoryId ?: "all"}") {
            val c = creds
            val url = apiUrl(c)
            when (type) {
                ContentType.LIVE -> api.getLiveStreams(url, c.username, c.password, categoryId)
                    .mapNotNull { s ->
                        val id = s.streamId ?: return@mapNotNull null
                        ContentItem(id, s.name.clean(), s.icon.nullIfBlank(), "m3u8", type)
                    }

                ContentType.MOVIE -> api.getVodStreams(url, c.username, c.password, categoryId)
                    .mapNotNull { s ->
                        val id = s.streamId ?: return@mapNotNull null
                        ContentItem(id, s.name.clean(), s.icon.nullIfBlank(), s.extension.nullIfBlank() ?: "mp4", type)
                    }

                ContentType.SERIES -> api.getSeries(url, c.username, c.password, categoryId)
                    .mapNotNull { s ->
                        val id = s.seriesId ?: return@mapNotNull null
                        ContentItem(id, s.name.clean(), s.cover.nullIfBlank(), null, type)
                    }
            }
        }

    suspend fun loadSeriesInfo(seriesId: String): SeriesDetailUi = cached("series_$seriesId") {
        val c = creds
        val response = api.getSeriesInfo(apiUrl(c), c.username, c.password, seriesId)
        val info = response.info?.takeIf { it.isJsonObject }
            ?.let { gson.fromJson(it, SeriesInfoDto::class.java) }

        SeriesDetailUi(
            name = info?.name.clean(),
            cover = info?.cover.nullIfBlank(),
            plot = info?.plot.nullIfBlank(),
            genre = info?.genre.nullIfBlank(),
            releaseDate = info?.releaseDate.nullIfBlank(),
            rating = info?.rating.nullIfBlank(),
            seasons = parseSeasons(response.episodes)
        )
    }

    // ------------------------------------------------------------ Streams

    /**
     * Monta as URLs de reprodução no padrão Xtream Codes.
     * Para TV ao vivo devolve HLS (.m3u8) e, como fallback, MPEG-TS (.ts):
     * alguns servidores só entregam um dos dois.
     */
    fun streamUrls(type: ContentType, id: String, extension: String?): List<String> {
        val c = creds
        val user = Uri.encode(c.username)
        val pass = Uri.encode(c.password)
        val s = c.serverUrl
        return when (type) {
            ContentType.LIVE -> listOf(
                "$s/live/$user/$pass/$id.m3u8",
                "$s/live/$user/$pass/$id.ts"
            )
            ContentType.MOVIE -> listOf("$s/movie/$user/$pass/$id.${extension ?: "mp4"}")
            ContentType.SERIES -> listOf("$s/series/$user/$pass/$id.${extension ?: "mp4"}")
        }
    }

    // ------------------------------------------------------------ Helpers

    /**
     * "episodes" costuma vir como objeto { "1": [...], "2": [...] }, mas há
     * painéis que mandam array de arrays. Tratamos os dois formatos.
     */
    private fun parseSeasons(element: JsonElement?): List<Season> {
        if (element == null || element.isJsonNull) return emptyList()
        val all = mutableListOf<Pair<String, EpisodeDto>>()

        fun addArray(seasonKey: String?, arr: JsonElement) {
            if (!arr.isJsonArray) return
            gson.fromJson(arr, Array<EpisodeDto>::class.java).forEach { ep ->
                all += (seasonKey ?: ep.season ?: "1") to ep
            }
        }

        when {
            element.isJsonObject -> element.asJsonObject.entrySet()
                .forEach { (season, arr) -> addArray(season, arr) }
            element.isJsonArray -> element.asJsonArray.forEach { inner ->
                if (inner.isJsonArray) addArray(null, inner)
                else if (inner.isJsonObject) {
                    val ep = gson.fromJson(inner, EpisodeDto::class.java)
                    all += (ep.season ?: "1") to ep
                }
            }
        }

        return all
            .filter { it.second.id != null }
            .groupBy({ it.first }, { it.second })
            .map { (season, eps) ->
                Season(
                    number = season,
                    episodes = eps.map { it.toUi() }
                        .sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
                )
            }
            .sortedBy { it.number.toIntOrNull() ?: Int.MAX_VALUE }
    }

    private fun EpisodeDto.toUi(): EpisodeUi {
        val infoObj: JsonObject? = info?.takeIf { it.isJsonObject }?.asJsonObject
        return EpisodeUi(
            id = id!!,
            number = episodeNum.orEmpty(),
            title = title.clean().ifEmpty { "Episódio ${episodeNum.orEmpty()}" },
            extension = extension.nullIfBlank() ?: "mp4",
            image = infoObj?.stringOrNull("movie_image"),
            duration = infoObj?.stringOrNull("duration")
        )
    }

    private fun JsonObject.stringOrNull(key: String): String? {
        val el = get(key) ?: return null
        return if (el.isJsonPrimitive) el.asString.nullIfBlank() else null
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T : Any> cached(key: String, loader: suspend () -> T): T {
        cache[key]?.let { return it as T }
        return loader().also { cache[key] = it }
    }

    private fun String?.clean(): String = this?.trim().orEmpty()
    private fun String?.nullIfBlank(): String? = this?.trim()?.takeIf { it.isNotEmpty() }
}
