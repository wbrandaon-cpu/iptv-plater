package com.example.iptvplayer.ui.components

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign

/** true quando o aparelho está navegando pelos dados móveis (3G/4G/5G). */
fun Context.isOnMobileData(): Boolean {
    val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
    val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
    return caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
}

/**
 * Dica exibida junto com erros quando o celular está nos dados móveis.
 * O app não tem nenhuma restrição a Wi-Fi; se só falha no 4G, a causa costuma
 * estar no aparelho (dados móveis bloqueados para o app) ou na operadora.
 */
@Composable
fun MobileDataHint(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val onMobile = remember { context.isOnMobileData() }
    if (!onMobile) return
    Text(
        "Você está nos dados móveis. Se funciona no Wi-Fi e não aqui, confira em " +
            "Configurações > Apps > IPTV Player > Dados móveis se o uso está permitido " +
            "(e se a Economia de dados está desligada para o app).",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
