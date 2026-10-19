package com.example.iptvplayer.data.remote

import com.example.iptvplayer.BuildConfig
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Verifica se há uma versão mais nova do app publicada no GitHub.
 * Cada build gera a tag v1.0.N, e N é o versionCode do APK.
 */
object UpdateChecker {

    private const val REPO = "wbrandaon-cpu/iptv-plater"

    /** Link direto que sempre baixa o APK mais recente. */
    const val DOWNLOAD_URL = "https://github.com/$REPO/releases/latest/download/IPTVPlayer.apk"

    data class UpdateInfo(val versionCode: Int, val versionName: String) {
        val isNewer: Boolean get() = versionCode > BuildConfig.VERSION_CODE
    }

    private val client by lazy {
        Net.client.newBuilder().readTimeout(20, TimeUnit.SECONDS).build()
    }

    /** Retorna a última versão publicada, ou null se não conseguir verificar. */
    suspend fun latest(): UpdateInfo? = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("https://api.github.com/repos/$REPO/releases/latest")
                .header("Accept", "application/vnd.github+json")
                .build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val tag = JsonParser.parseString(body).asJsonObject.get("tag_name")?.asString
                    ?: return@use null
                val code = tag.substringAfterLast('.').toIntOrNull() ?: return@use null
                UpdateInfo(code, tag.removePrefix("v"))
            }
        }.getOrNull()
    }
}
