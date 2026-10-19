package com.example.iptvplayer.ui.home

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.iptvplayer.data.model.Credentials
import com.example.iptvplayer.data.remote.Net
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

private val AUTH_OK = Regex(""""auth"\s*:\s*"?1""")

/** Resultado de uma etapa do teste. ok = null enquanto está rodando. */
data class TestStep(val title: String, val ok: Boolean?, val detail: String)

/**
 * Teste de conexão: mostra em qual etapa a conexão com o servidor falha.
 * Útil para entender por que funciona no Wi-Fi e não no 4G/5G (ou o contrário).
 * Não exibe usuário nem senha.
 */
@Composable
fun ConnectionTestDialog(credentials: Credentials?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val steps = remember { mutableStateListOf<TestStep>() }
    var running by remember { mutableStateOf(true) }
    var conclusion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        conclusion = runConnectionTest(context, credentials) { steps.add(it) }
        running = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Teste de conexão") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                steps.forEach { step -> StepRow(step) }
                if (running) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Testando...")
                    }
                }
                conclusion?.let {
                    Text(it, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Fechar") } }
    )
}

@Composable
private fun StepRow(step: TestStep) {
    Row {
        Text(
            when (step.ok) { true -> "✔"; false -> "✘"; null -> "•" },
            color = when (step.ok) { true -> MaterialTheme.colorScheme.primary; false -> Color(0xFFFF6B6B); null -> Color.Gray },
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp)
        )
        Column {
            Text(step.title, fontWeight = FontWeight.SemiBold)
            Text(step.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Executa as etapas em ordem e devolve uma conclusão em linguagem simples. */
private suspend fun runConnectionTest(
    context: Context,
    credentials: Credentials?,
    report: (TestStep) -> Unit
): String = withContext(Dispatchers.IO) {

    // 1) Tipo de rede
    val cm = context.getSystemService(ConnectivityManager::class.java)
    val caps = cm?.getNetworkCapabilities(cm.activeNetwork)
    val link = cm?.getLinkProperties(cm.activeNetwork)
    val netType = when {
        caps == null -> "Sem conexão"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Dados móveis (4G/5G)"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Cabo de rede"
        else -> "Outra"
    }
    val addrs = link?.linkAddresses?.map { it.address }.orEmpty()
    val ipInfo = buildList {
        if (addrs.any { it is Inet4Address }) add("IPv4")
        if (addrs.any { it is Inet6Address && !it.isLinkLocalAddress }) add("IPv6")
    }.joinToString(" + ").ifEmpty { "sem endereço" }
    report(TestStep("Rede do aparelho", caps != null, "$netType • $ipInfo"))
    if (caps == null) return@withContext "O aparelho está sem internet. Ative o Wi-Fi ou os dados móveis."

    // 2) Internet em geral
    val internetOk = runCatching {
        Net.client.newCall(Request.Builder().url("https://connectivitycheck.gstatic.com/generate_204").build())
            .execute().use { it.code == 204 || it.isSuccessful }
    }.getOrDefault(false)
    report(TestStep("Acesso à internet", internetOk, if (internetOk) "OK" else "Não foi possível acessar a internet"))
    if (!internetOk) return@withContext "A internet desta rede não está respondendo. " +
        "Se estiver nos dados móveis, confira se há sinal/franquia e se o app tem permissão de usar dados móveis."

    if (credentials == null) return@withContext "Faça login para testar o servidor."
    val uri = Uri.parse(credentials.serverUrl)
    val host = uri.host.orEmpty()
    val port = if (uri.port > 0) uri.port else if (uri.scheme == "https") 443 else 80

    // 3) DNS do servidor
    val resolved = runCatching { InetAddress.getAllByName(host).toList() }
    val ips = resolved.getOrNull().orEmpty()
    val dnsDetail = if (ips.isNotEmpty()) {
        "${ips.count { it is Inet4Address }} endereço(s) IPv4, ${ips.count { it is Inet6Address }} IPv6"
    } else {
        "Nome do servidor não encontrado nesta rede"
    }
    report(TestStep("Encontrar o servidor (DNS)", ips.isNotEmpty(), dnsDetail))
    if (ips.isEmpty()) return@withContext "Esta rede não encontrou o endereço do servidor da sua lista. " +
        "Isso costuma acontecer quando a operadora bloqueia o servidor. Teste em outra rede e, " +
        "se só falhar nesta, fale com o seu provedor da lista."

    // 4) Conexão na porta do servidor (IPv4 primeiro, depois IPv6)
    val ordered = ips.sortedBy { if (it is Inet4Address) 0 else 1 }
    var connectedVia: String? = null
    var elapsed = 0L
    for (ip in ordered) {
        val start = System.currentTimeMillis()
        val ok = runCatching { Socket().use { it.connect(InetSocketAddress(ip, port), 8_000) } }.isSuccess
        if (ok) {
            elapsed = System.currentTimeMillis() - start
            connectedVia = if (ip is Inet4Address) "IPv4" else "IPv6"
            break
        }
    }
    report(
        TestStep(
            "Conectar na porta $port",
            connectedVia != null,
            connectedVia?.let { "OK via $it em ${elapsed} ms" } ?: "Sem resposta na porta $port"
        )
    )
    if (connectedVia == null) return@withContext "O servidor foi encontrado, mas não aceita conexão nesta rede " +
        "(porta $port). Se funciona no Wi-Fi, a operadora pode estar bloqueando esse servidor ou essa porta. " +
        "Fale com o seu provedor da lista."

    // 5) Login na API
    val apiUrl = "${credentials.serverUrl}/player_api.php".toHttpUrlOrNull()?.newBuilder()
        ?.addQueryParameter("username", credentials.username)
        ?.addQueryParameter("password", credentials.password)
        ?.build()
    val start = System.currentTimeMillis()
    val apiResult = runCatching {
        Net.client.newCall(Request.Builder().url(apiUrl!!).build()).execute().use { r ->
            val body = r.body?.string().orEmpty()
            r.code to AUTH_OK.containsMatchIn(body)
        }
    }
    val apiMs = System.currentTimeMillis() - start
    val (code, auth) = apiResult.getOrDefault(-1 to false)
    report(
        TestStep(
            "Login no servidor",
            auth,
            when {
                code == -1 -> "Sem resposta (${apiResult.exceptionOrNull()?.javaClass?.simpleName})"
                auth -> "OK (HTTP $code) em $apiMs ms"
                else -> "Servidor respondeu HTTP $code, mas recusou o login"
            }
        )
    )
    when {
        auth && apiMs > 5_000 -> "Tudo funcionando, mas esta rede está lenta para chegar ao servidor ($apiMs ms). " +
            "Os canais podem demorar um pouco para abrir."
        auth -> "Tudo funcionando nesta rede ($netType)."
        code == -1 -> "A conexão com o servidor caiu durante o login. Tente de novo; se persistir só nesta rede, " +
            "a operadora pode estar interferindo na conexão."
        else -> "O servidor respondeu, mas não aceitou usuário/senha. Confira os dados com o seu provedor."
    }
}
