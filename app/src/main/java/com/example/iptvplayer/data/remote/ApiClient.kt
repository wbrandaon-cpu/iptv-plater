package com.example.iptvplayer.data.remote

import com.example.iptvplayer.BuildConfig
import com.google.gson.GsonBuilder
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    /** Alguns painéis bloqueiam o User-Agent padrão do OkHttp; usamos um próprio. */
    const val USER_AGENT = Net.USER_AGENT

    fun create(): XtreamApi {
        // Atenção: o log BASIC mostra a URL (com usuário/senha). Só ativo em debug.
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }

        // Mesmo cliente do resto do app (IPv4 primeiro, timeouts para 4G/5G)
        val client = Net.client.newBuilder()
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
