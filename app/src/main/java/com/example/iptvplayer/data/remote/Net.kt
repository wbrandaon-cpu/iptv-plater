package com.example.iptvplayer.data.remote

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import java.net.Inet4Address
import java.net.InetAddress
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP único do app (API, vídeo e imagens), ajustado para redes móveis.
 *
 * Problemas comuns no 4G/5G que isto resolve:
 *  - Redes móveis brasileiras usam IPv6. Quando o servidor IPTV anuncia um
 *    endereço IPv6 que não funciona, o app ficava preso tentando ele até o
 *    tempo esgotar. Agora o IPv4 é tentado primeiro (e o IPv6 continua como
 *    alternativa, inclusive em redes só-IPv6).
 *  - Ao trocar de Wi-Fi para 4G (ou o contrário), conexões antigas "mortas"
 *    eram reaproveitadas e travavam. Agora elas são descartadas na troca.
 *  - Tempo de conexão maior, porque a rede móvel demora mais para "acordar".
 */
object Net {

    const val USER_AGENT = "IPTVPlayer/1.0 (Linux; Android)"

    /** IPv4 primeiro, IPv6 depois. */
    private val ipv4FirstDns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            Dns.SYSTEM.lookup(hostname).sortedBy { if (it is Inet4Address) 0 else 1 }
    }

    private val userAgent = Interceptor { chain ->
        chain.proceed(chain.request().newBuilder().header("User-Agent", USER_AGENT).build())
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(ipv4FirstDns)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS) // listas de VOD podem ter dezenas de MB
            .writeTimeout(20, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .followRedirects(true)
            .followSslRedirects(true)
            .addInterceptor(userAgent)
            .build()
    }

    /** Descarta conexões antigas sempre que a rede do aparelho muda. */
    fun watchNetworkChanges(context: Context) {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return
        runCatching {
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = client.connectionPool.evictAll()
                override fun onLost(network: Network) = client.connectionPool.evictAll()
            })
        }
    }
}
