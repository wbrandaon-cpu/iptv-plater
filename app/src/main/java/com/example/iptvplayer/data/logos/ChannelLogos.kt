package com.example.iptvplayer.data.logos

import android.content.Context
import com.google.gson.JsonParser
import java.text.Normalizer

/**
 * Logos oficiais (coloridos) dos canais, vindos do banco público iptv-org.
 *
 * O mapa "nome normalizado -> URL do logo" é gerado na compilação pelo script
 * tools/build_logo_map.py e embutido no APK (assets/channel_logos.json),
 * então não gasta internet do celular.
 *
 * IMPORTANTE: tokens() precisa seguir exatamente a mesma regra de normalize()
 * do script Python, senão os nomes não batem.
 */
object ChannelLogos {

    data class Match(val url: String, val exact: Boolean)

    @Volatile
    private var map: Map<String, String> = emptyMap()

    private const val QUALITY =
        """fhd|hd|sd|uhd|fullhd|4k|8k|h265|h264|hevc|1080p?|720p?|480p?|2160p?|\d+fps|fps|hdr|alt|backup|vip|br|pt|us|uk"""
    private val MARKS = Regex("""\p{Mn}+""")
    private val BRACKETS = Regex("""\[[^\]]*\]|\([^)]*\)|\{[^}]*\}""")
    private val PREFIX = Regex("""^\s*[a-z]{2,3}\s*[:|]\s*""")
    private val NON_ALNUM = Regex("""[^a-z0-9]+""")
    private val QUALITY_RE = Regex("""(?<![a-z0-9])($QUALITY)(?![a-z0-9])""")

    /** Carrega o mapa do APK. Chamado uma vez ao abrir o app (em segundo plano). */
    fun load(context: Context) {
        if (map.isNotEmpty()) return
        map = runCatching {
            context.assets.open("channel_logos.json").bufferedReader().use { reader ->
                JsonParser.parseReader(reader).asJsonObject.entrySet()
                    .associate { (k, v) -> k to v.asString }
            }
        }.getOrDefault(emptyMap())
    }

    fun tokens(name: String): List<String> {
        var s = name.lowercase()
        s = Normalizer.normalize(s, Normalizer.Form.NFD).replace(MARKS, "")
        s = BRACKETS.replace(s, " ")
        s = PREFIX.replace(s, " ")
        s = NON_ALNUM.replace(s, " ")
        s = QUALITY_RE.replace(s, " ")
        return s.split(' ').filter { it.isNotBlank() }
    }

    /**
     * Procura o logo pelo nome do canal. Tenta o nome completo e, se não achar,
     * vai tirando palavras do fim ("Globo SP" -> "Globo").
     * exact = true quando bateu o nome completo (alta confiança).
     */
    fun find(channelName: String): Match? {
        val m = map
        if (m.isEmpty()) return null
        val t = tokens(channelName)
        for (n in t.size downTo 1) {
            val key = t.take(n).joinToString("")
            if (key.length < 3) continue
            for (candidate in listOf(key, "tv$key", "${key}tv")) {
                m[candidate]?.let { return Match(it, exact = n == t.size) }
            }
        }
        return null
    }
}
