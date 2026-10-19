package com.example.iptvplayer.data.remote

import com.example.iptvplayer.data.remote.dto.AuthResponse
import com.example.iptvplayer.data.remote.dto.CategoryDto
import com.example.iptvplayer.data.remote.dto.LiveStreamDto
import com.example.iptvplayer.data.remote.dto.SeriesDto
import com.example.iptvplayer.data.remote.dto.SeriesInfoResponse
import com.example.iptvplayer.data.remote.dto.VodStreamDto
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Endpoints da API Xtream Codes.
 * Como cada usuário tem um servidor diferente, a URL completa é passada via @Url
 * (ex.: http://host:8080/player_api.php) em vez de um baseUrl fixo.
 */
interface XtreamApi {

    /** Sem "action": devolve user_info + server_info (usado para validar login). */
    @GET
    suspend fun authenticate(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String
    ): AuthResponse

    @GET
    suspend fun getCategories(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String
    ): List<CategoryDto>

    @GET
    suspend fun getLiveStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String?,
        @Query("action") action: String = "get_live_streams"
    ): List<LiveStreamDto>

    @GET
    suspend fun getVodStreams(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String?,
        @Query("action") action: String = "get_vod_streams"
    ): List<VodStreamDto>

    @GET
    suspend fun getSeries(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("category_id") categoryId: String?,
        @Query("action") action: String = "get_series"
    ): List<SeriesDto>

    @GET
    suspend fun getSeriesInfo(
        @Url url: String,
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("series_id") seriesId: String,
        @Query("action") action: String = "get_series_info"
    ): SeriesInfoResponse
}
