package com.example.iptvplayer.data.remote

import com.example.iptvplayer.data.remote.dto.ServerInfoDto
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Portas/endereços alternativos do servidor IPTV.
 *
 * O painel Xtream informa no login (server_info) em quais portas ele atende:
 * a porta HTTP ("port") e a porta HTTPS ("https_port"), às vezes com outro
 * endereço ("url"). Quando a porta principal não responde na rede atual
 * (ex.: operadora móvel não deixa conectar na porta 80), o app tenta essas
 * alternativas automaticamente — para a API, os vídeos e as imagens — e passa
 * a usar a que funcionou.
 *
 * Só usa endereços que o próprio servidor anunciou (ou a porta HTTPS padrão do
 * mesmo servidor). Se o servidor não atender em outra porta, não há o que fazer
 * pelo app.
 */
object ServerFailover {

    @Volatile private var primary: HttpUrl? = null
    @Volatile private var alternatives: List<HttpUrl> = emptyList()
    /** Último endereço que funcionou (usado primeiro nas próximas requisições). */
    @Volatile private var active: HttpUrl? = null

    /** Endereços em uso (principal + alternativas), para o teste de conexão. */
    val allBases: List<HttpUrl>
        get() = listOfNotNull(primary) + alternatives

    fun configure(primaryBase: String, alternativeBases: List<String>) {
        primary = primaryBase.toHttpUrlOrNull()
        alternatives = alternativeBases.mapNotNull { it.toHttpUrlOrNull() }
            .filterNot { primary != null && sameServer(it, primary!!) }
            .distinctBy { "${it.scheme}://${it.host}:${it.port}" }
        active = null
    }

    fun clear() {
        primary = null
        alternatives = emptyList()
        active = null
    }

    /**
     * Monta a lista de alternativas a partir do server_info do login.
     * Sempre inclui a porta HTTPS padrão (443) do mesmo servidor como última opção.
     */
    fun alternativesFrom(primaryBase: String, info: ServerInfoDto?): List<String> {
        val base = primaryBase.toHttpUrlOrNull() ?: return emptyList()
        val hosts = listOfNotNull(base.host, info?.url?.trim()?.takeIf { it.isNotEmpty() }).distinct()
        val httpsPort = info?.httpsPort?.toIntOrNull()?.takeIf { it in 1..65535 }
        val httpPort = info?.port?.toIntOrNull()?.takeIf { it in 1..65535 }
        val out = mutableListOf<String>()
        for (h in hosts) {
            if (httpsPort != null) out += "https://$h:$httpsPort"
            if (httpPort != null) out += "http://$h:$httpPort"
        }
        out += "https://${base.host}"
        return out.distinct()
    }

    private fun sameServer(a: HttpUrl, b: HttpUrl) =
        a.scheme == b.scheme && a.host == b.host && a.port == b.port

    private fun isOurServer(url: HttpUrl): Boolean =
        allBases.any { sameServer(url, it) }

    /** Página HTML (ou 451) no lugar da API ou do vídeo: não é o servidor IPTV respondendo. */
    private fun looksIntercepted(url: HttpUrl, response: Response): Boolean {
        if (response.code == 451) return true
        val path = url.encodedPath.lowercase()
        val expectsData = path.endsWith(".php") || path.endsWith(".ts") || path.endsWith(".m3u8") ||
            path.contains("/live/") || path.contains("/movie/") || path.contains("/series/")
        val isHtml = response.header("Content-Type")?.contains("text/html", ignoreCase = true) == true
        return expectsData && isHtml
    }

    val interceptor = Interceptor { chain ->
        val request = chain.request()
        val p = primary
        if (p == null || alternatives.isEmpty() || !isOurServer(request.url)) {
            return@Interceptor chain.proceed(request)
        }

        // Ordem: o que funcionou por último, depois o principal, depois as alternativas
        val bases = (listOfNotNull(active) + p + alternatives).distinctBy { "${it.scheme}://${it.host}:${it.port}" }
        var lastError: IOException? = null
        for (base in bases) {
            val url = request.url.newBuilder()
                .scheme(base.scheme)
                .host(base.host)
                .port(base.port)
                .build()
            try {
                val response: Response = chain
                    .withConnectTimeout(8, TimeUnit.SECONDS) // não esperar muito por porta bloqueada
                    .proceed(request.newBuilder().url(url).build())
                if (looksIntercepted(request.url, response) && base != bases.last()) {
                    // Ex.: a rede devolveu uma página HTML no lugar da API/vídeo
                    response.close()
                    lastError = IOException("Resposta inesperada em ${base.port}")
                    continue
                }
                active = base
                return@Interceptor response
            } catch (e: IOException) {
                if (chain.call().isCanceled()) throw e
                lastError = e
            }
        }
        throw lastError ?: IOException("Servidor indisponível")
    }
}
