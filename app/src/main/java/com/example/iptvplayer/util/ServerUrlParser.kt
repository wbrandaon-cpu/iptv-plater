package com.example.iptvplayer.util

import android.net.Uri

data class ParsedServer(
    val baseUrl: String,
    val username: String?,
    val password: String?
)

/**
 * Normaliza o que o usuário digitou no campo "URL do Servidor".
 *
 * Aceita:
 *  - servidor.com:8080                    -> http://servidor.com:8080
 *  - http://servidor.com:8080/            -> http://servidor.com:8080
 *  - link M3U completo do provedor:
 *    http://servidor.com:8080/get.php?username=joao&password=123&type=m3u_plus
 *    -> extrai host, usuário e senha automaticamente.
 */
object ServerUrlParser {

    fun parse(input: String): ParsedServer? {
        var raw = input.trim()
        if (raw.isEmpty()) return null
        if (!raw.startsWith("http://", ignoreCase = true) &&
            !raw.startsWith("https://", ignoreCase = true)
        ) {
            raw = "http://$raw"
        }

        val uri = Uri.parse(raw)
        if (uri.host.isNullOrBlank()) return null
        val scheme = uri.scheme?.lowercase() ?: "http"
        val authority = uri.encodedAuthority ?: return null

        // Remove o arquivo .php final (get.php, player_api.php, xmltv.php...)
        val segments = uri.pathSegments.toMutableList()
        if (segments.lastOrNull()?.endsWith(".php", ignoreCase = true) == true) {
            segments.removeAt(segments.lastIndex)
        }
        val path = if (segments.isEmpty()) "" else "/" + segments.joinToString("/")

        val user = runCatching { uri.getQueryParameter("username") }.getOrNull()
        val pass = runCatching { uri.getQueryParameter("password") }.getOrNull()

        return ParsedServer(
            baseUrl = "$scheme://$authority$path",
            username = user?.takeIf { it.isNotBlank() },
            password = pass?.takeIf { it.isNotBlank() }
        )
    }
}
