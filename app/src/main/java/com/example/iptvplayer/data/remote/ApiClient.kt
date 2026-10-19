package com.example.iptvplayer.data.remote

import com.example.iptvplayer.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    /** Alguns painéis bloqueiam o User-Agent padrão do OkHttp; usamos um próprio. */
    const val USER_AGENT = "IPTVPlayer/1.0 (Linux; Android)"

    fun create(): XtreamApi {
        val userAgent = Interceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }

        // Atenção: o log BASIC mostra a URL (com usuário/senha). Só ativo em debug.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        val client = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // listas de VOD podem ter dezenas de MB
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(userAgent)
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder().setLenient().create()

        return Retrofit.Builder()
            // baseUrl é obrigatório no Retrofit, mas todas as chamadas usam @Url absoluto.
            .baseUrl("http://localhost/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(XtreamApi::class.java)
    }
}
