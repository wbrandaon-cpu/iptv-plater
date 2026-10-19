package com.example.iptvplayer.data.remote.dto

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

/*
 * DTOs da API Xtream Codes (player_api.php).
 * Todos os campos são String? de propósito: cada painel devolve tipos diferentes
 * (o mesmo campo pode vir como número, string ou até vazio). O Gson converte
 * números em String sem problemas, e campos nulos são filtrados no repositório.
 */

data class AuthResponse(
    @SerializedName("user_info") val userInfo: UserInfoDto?,
    @SerializedName("server_info") val serverInfo: ServerInfoDto?
)

data class UserInfoDto(
    @SerializedName("username") val username: String?,
    @SerializedName("auth") val auth: String?,
    @SerializedName("status") val status: String?,
    @SerializedName("exp_date") val expDate: String?,
    @SerializedName("max_connections") val maxConnections: String?
)

data class ServerInfoDto(
    @SerializedName("url") val url: String?,
    @SerializedName("port") val port: String?,
    @SerializedName("https_port") val httpsPort: String?,
    @SerializedName("server_protocol") val protocol: String?
)

data class CategoryDto(
    @SerializedName("category_id") val id: String?,
    @SerializedName("category_name") val name: String?
)

data class LiveStreamDto(
    @SerializedName("stream_id") val streamId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_icon") val icon: String?,
    @SerializedName("category_id") val categoryId: String?,
    @SerializedName("num") val num: String?,
    @SerializedName("added") val added: String?
)

data class VodStreamDto(
    @SerializedName("stream_id") val streamId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("stream_icon") val icon: String?,
    @SerializedName("container_extension") val extension: String?,
    @SerializedName("rating") val rating: String?,
    @SerializedName("num") val num: String?,
    @SerializedName("added") val added: String?
)

data class SeriesDto(
    @SerializedName("series_id") val seriesId: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("num") val num: String?,
    @SerializedName("last_modified") val lastModified: String?
)

/**
 * "info" e "episodes" ficam como JsonElement porque alguns painéis devolvem
 * [] (array vazio) em vez de {} — o que quebraria o parse automático.
 */
data class SeriesInfoResponse(
    @SerializedName("info") val info: JsonElement?,
    @SerializedName("episodes") val episodes: JsonElement?
)

data class SeriesInfoDto(
    @SerializedName("name") val name: String?,
    @SerializedName("cover") val cover: String?,
    @SerializedName("plot") val plot: String?,
    @SerializedName("genre") val genre: String?,
    @SerializedName("releaseDate") val releaseDate: String?,
    @SerializedName("rating") val rating: String?
)

data class EpisodeDto(
    @SerializedName("id") val id: String?,
    @SerializedName("episode_num") val episodeNum: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("container_extension") val extension: String?,
    @SerializedName("season") val season: String?,
    @SerializedName("info") val info: JsonElement?
)

/** Favorito salvo no aparelho (JSON no DataStore). */
data class FavoriteDto(
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("ext") val extension: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("added") val added: Long?
)
