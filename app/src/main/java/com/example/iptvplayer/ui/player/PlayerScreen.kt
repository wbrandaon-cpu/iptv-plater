package com.example.iptvplayer.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.iptvplayer.data.remote.ApiClient
import com.example.iptvplayer.ui.components.ClockText

/**
 * Player em tela cheia com Media3/ExoPlayer.
 *
 * @param urls lista de URLs candidatas; se a primeira falhar (ex.: servidor sem HLS),
 *             tenta automaticamente a próxima (ex.: MPEG-TS).
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    urls: List<String>,
    title: String,
    isLive: Boolean,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var urlIndex by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(if (urls.isEmpty()) "Sessão inválida. Faça login novamente." else null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var resizeMode by remember { mutableIntStateOf(AspectRatioFrameLayout.RESIZE_MODE_FIT) }

    val player = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ApiClient.USER_AGENT)
            .setAllowCrossProtocolRedirects(true) // servidores IPTV redirecionam muito (http <-> https)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(30_000)

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(httpFactory))
            .build()
            .apply { playWhenReady = true }
    }

    // (Re)prepara o player sempre que a URL candidata mudar
    LaunchedEffect(urlIndex) {
        val url = urls.getOrNull(urlIndex) ?: return@LaunchedEffect
        val item = MediaItem.Builder()
            .setUri(url)
            .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
            .apply { if (url.endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
            .build()
        player.setMediaItem(item)
        player.prepare()
    }

    // Tratamento de erros + liberação do player
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                when {
                    // Live atrasou demais em relação à janela do HLS: volta para o "ao vivo"
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    // Tenta o próximo formato (ex.: .m3u8 -> .ts)
                    urlIndex < urls.lastIndex -> urlIndex++
                    else -> errorMessage = friendlyPlaybackError(error)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    // Pausa quando o app vai para segundo plano e retoma ao voltar
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (errorMessage == null) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Tela cheia (imersivo) + paisagem enquanto o player estiver aberto
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, it.decorView) }
        val previousOrientation = activity?.requestedOrientation
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
        onDispose {
            controller?.show(WindowInsetsCompat.Type.systemBars())
            if (activity != null && previousOrientation != null) {
                activity.requestedOrientation = previousOrientation
            }
        }
    }

    BackHandler(onBack = onBack)

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                    keepScreenOn = true
                    // TV Box: o PlayerView precisa de foco para responder ao controle remoto
                    isFocusable = true
                    post { requestFocus() }
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // Em canais ao vivo não faz sentido avançar/voltar
                    setShowFastForwardButton(!isLive)
                    setShowRewindButton(!isLive)
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            controlsVisible = visibility == android.view.View.VISIBLE
                        }
                    )
                }
            },
            update = { view -> view.resizeMode = resizeMode },
            modifier = Modifier.fillMaxSize()
        )

        // Barra superior com título, mostrada junto com os controles do player
        if (controlsVisible && errorMessage == null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                }
                Text(
                    title,
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                ClockText(color = Color.White, modifier = Modifier.padding(horizontal = 12.dp))
                OutlinedButton(onClick = {
                    resizeMode = when (resizeMode) {
                        AspectRatioFrameLayout.RESIZE_MODE_FIT -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                        else -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                }) {
                    Text(
                        when (resizeMode) {
                            AspectRatioFrameLayout.RESIZE_MODE_ZOOM -> "Zoom"
                            AspectRatioFrameLayout.RESIZE_MODE_FILL -> "Esticar"
                            else -> "Ajustar"
                        },
                        color = Color.White
                    )
                }
            }
        }

        // Overlay de erro com opção de tentar de novo
        errorMessage?.let { msg ->
            Column(
                Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.85f), MaterialTheme.shapes.medium)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(msg, color = Color.White, textAlign = TextAlign.Center)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { Text("Voltar", color = Color.White) }
                    if (urls.isNotEmpty()) {
                        Button(onClick = {
                            errorMessage = null
                            if (urlIndex == 0) {
                                player.prepare() // mesma URL: só reprepara
                            } else {
                                urlIndex = 0 // dispara o LaunchedEffect com a 1ª URL
                            }
                        }) { Text("Tentar novamente") }
                    }
                }
            }
        }
    }
}

private fun friendlyPlaybackError(error: PlaybackException): String = when (error.errorCode) {
    PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
        "O servidor recusou o stream. Verifique se sua conta permite mais uma conexão simultânea."
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
        "Falha de conexão com o servidor de vídeo."
    PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
    PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
    PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
    PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ->
        "Formato de vídeo não suportado por este aparelho."
    else -> "Não foi possível reproduzir este conteúdo (${error.errorCodeName})."
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
