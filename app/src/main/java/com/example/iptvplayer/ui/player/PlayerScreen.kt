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
import com.example.iptvplayer.ui.components.MobileDataHint
import androidx.compose.runtime.rememberCoroutineScope
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val scope = rememberCoroutineScope()
    var reconnectAttempts by remember { mutableIntStateOf(0) }

    val player = remember {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(ApiClient.USER_AGENT)
            .setAllowCrossProtocolRedirects(true) // servidores IPTV redirecionam muito (http <-> https)
            .setConnectTimeoutMs(10_000)
            .setReadTimeoutMs(20_000)

        // MPEG-TS: começa a exibir sem esperar um quadro-chave completo (troca de canal mais rápida)
        val extractors = DefaultExtractorsFactory().setTsExtractorFlags(
            DefaultTsPayloadReaderFactory.FLAG_ALLOW_NON_IDR_KEYFRAMES or
                DefaultTsPayloadReaderFactory.FLAG_DETECT_ACCESS_UNITS
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(httpFactory, extractors)
            // HLS ao vivo (fallback): fica perto do "ao vivo" e acelera levemente para alcançar
            .setLiveTargetOffsetMs(3_000)
            .setLiveMinSpeed(0.97f)
            .setLiveMaxSpeed(1.05f)

        // Buffer curto no ao vivo = imagem aparece rápido e com pouco atraso.
        // Filmes/séries usam buffer maior para não travar.
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ if (isLive) 2_000 else 15_000,
                /* maxBufferMs = */ if (isLive) 15_000 else 50_000,
                /* bufferForPlaybackMs = */ if (isLive) 500 else 1_500,
                /* bufferForPlaybackAfterRebufferMs = */ if (isLive) 1_500 else 3_000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        ExoPlayer.Builder(context)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
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
            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> reconnectAttempts = 0
                    // Ao vivo nunca deveria "terminar": se o servidor cortou, reconecta
                    Player.STATE_ENDED -> if (isLive) {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    else -> Unit
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val networkError = error.errorCode in NETWORK_ERRORS
                when {
                    // Live atrasou demais em relação à janela do HLS: volta para o "ao vivo"
                    error.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW -> {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    // Queda de conexão (ex.: troca Wi-Fi <-> 4G): reconecta sozinho
                    networkError && reconnectAttempts < MAX_RECONNECTS -> {
                        reconnectAttempts++
                        scope.launch {
                            delay(1_000L * reconnectAttempts)
                            player.seekToDefaultPosition()
                            player.prepare()
                        }
                    }
                    // Tenta o próximo formato (ex.: .ts -> .m3u8)
                    urlIndex < urls.lastIndex -> {
                        reconnectAttempts = 0
                        urlIndex++
                    }
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
                // Ao vivo: fecha a conexão em segundo plano (libera a conexão da conta)
                // e, ao voltar, reabre já no ponto "ao vivo", sem atraso acumulado.
                Lifecycle.Event.ON_STOP -> if (isLive) player.stop() else player.pause()
                Lifecycle.Event.ON_START -> if (errorMessage == null) {
                    if (isLive) {
                        player.seekToDefaultPosition()
                        player.prepare()
                    }
                    player.play()
                }
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
                MobileDataHint()
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onBack) { Text("Voltar", color = Color.White) }
                    if (urls.isNotEmpty()) {
                        Button(onClick = {
                            errorMessage = null
                            reconnectAttempts = 0
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

private const val MAX_RECONNECTS = 3

private val NETWORK_ERRORS = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_TIMEOUT
)

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
