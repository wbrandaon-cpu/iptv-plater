package com.example.iptvplayer.ui.components

import android.graphics.Color as AndroidColor
import android.graphics.drawable.Drawable
import androidx.core.graphics.drawable.toBitmap
import java.util.concurrent.ConcurrentHashMap

/**
 * Detecta se um logo é escuro (preto ou quase preto), para colocar um fundo
 * branco semitransparente atrás dele e o logo não sumir no tema escuro.
 */
object LogoTone {

    private val cache = ConcurrentHashMap<String, Boolean>()

    fun cached(url: String): Boolean? = cache[url]

    /** Analisa uma miniatura 32x32 do logo; ignora as partes transparentes. */
    fun isDark(url: String, drawable: Drawable): Boolean {
        cache[url]?.let { return it }
        val dark = runCatching {
            val bmp = drawable.toBitmap(32, 32)
            var visible = 0
            var luminanceSum = 0.0
            for (y in 0 until bmp.height) {
                for (x in 0 until bmp.width) {
                    val p = bmp.getPixel(x, y)
                    if (AndroidColor.alpha(p) < 128) continue // parte transparente
                    visible++
                    luminanceSum += (0.299 * AndroidColor.red(p) +
                        0.587 * AndroidColor.green(p) +
                        0.114 * AndroidColor.blue(p)) / 255.0
                }
            }
            visible > 0 && luminanceSum / visible < 0.30
        }.getOrDefault(false)
        cache[url] = dark
        return dark
    }
}
