package com.example.iptvplayer

import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import com.example.iptvplayer.data.local.SessionManager
import com.example.iptvplayer.data.logos.ChannelLogos
import com.example.iptvplayer.data.remote.ApiClient
import com.example.iptvplayer.data.remote.Net
import com.example.iptvplayer.data.repository.XtreamRepository

/**
 * Application com um "container" simples de dependências (injeção manual).
 * Em projetos maiores, troque por Hilt/Koin.
 */
class IptvApp : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        // Carrega o mapa de logos oficiais em segundo plano
        Thread { ChannelLogos.load(this) }.start()
        // Limpa conexões antigas ao trocar entre Wi-Fi e dados móveis
        Net.watchNetworkChanges(this)
    }

    /**
     * Carregador de imagens (logos e capas):
     *  - entende logos em SVG (comuns em logos oficiais);
     *  - usa User-Agent próprio (alguns servidores bloqueiam o padrão);
     *  - guarda os logos em cache mesmo quando o servidor pede para não guardar,
     *    então a lista abre rápido nas próximas vezes.
     */
    override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
        .components { add(SvgDecoder.Factory()) }
        .okHttpClient {
            Net.client.newBuilder()
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            // Wikimedia (fonte de vários logos) pede um User-Agent com contato
                            .header("User-Agent", "IPTVPlayer/1.0 (Android; +https://github.com/wbrandaon-cpu/iptv-plater)")
                            .build()
                    )
                }
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.resolve("image_cache"))
                .maxSizeBytes(150L * 1024 * 1024)
                .build()
        }
        .respectCacheHeaders(false)
        .crossfade(true)
        .build()
}

class AppContainer(context: Context) {
    val sessionManager = SessionManager(context)
    val repository = XtreamRepository(ApiClient.create())
}
