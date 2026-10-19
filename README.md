# IPTV Player (Kotlin + Jetpack Compose + Media3)

App Android para listas IPTV usando a **API Xtream Codes** (`player_api.php`).

## Como abrir
1. Android Studio Ladybug (2024.2) ou mais novo → **File > Open** → selecione a pasta `IPTVPlayer`.
2. Aguarde o Gradle sincronizar (o Studio gera o `gradlew` automaticamente).
3. Rode em um celular, emulador ou Android TV (minSdk 24 / Android 7.0).

## Estrutura

```
app/src/main/java/com/example/iptvplayer/
├── IptvApp.kt                    # Application + container de dependências
├── MainActivity.kt               # Activity única (Compose)
├── data/
│   ├── local/SessionManager.kt   # DataStore: salva servidor/usuário/senha
│   ├── model/Models.kt           # Modelos de UI (ContentType, ContentItem...)
│   ├── remote/
│   │   ├── ApiClient.kt          # Retrofit + OkHttp
│   │   ├── XtreamApi.kt          # Endpoints Xtream Codes
│   │   └── dto/XtreamDtos.kt     # JSON da API
│   └── repository/XtreamRepository.kt  # Login, catálogo, URLs de stream, cache
├── util/                         # UiState, erros amigáveis, parser de URL
└── ui/
    ├── navigation/AppNavHost.kt  # Rotas
    ├── login/                    # Tela de login
    ├── home/                     # Dashboard (TV AO VIVO / SÉRIES / FILMES)
    ├── catalog/                  # Categorias + grade de conteúdo
    ├── series/                   # Temporadas e episódios
    ├── player/PlayerScreen.kt    # Media3/ExoPlayer em tela cheia
    ├── components/               # Componentes reutilizáveis
    └── theme/                    # Tema escuro
```

## Endpoints usados
| Ação | URL |
|---|---|
| Login | `{servidor}/player_api.php?username=U&password=P` |
| Categorias | `...&action=get_live_categories` / `get_vod_categories` / `get_series_categories` |
| Conteúdo | `...&action=get_live_streams` / `get_vod_streams` / `get_series` (`&category_id=X`) |
| Episódios | `...&action=get_series_info&series_id=X` |
| Stream ao vivo | `{servidor}/live/U/P/{id}.m3u8` (fallback `.ts`) |
| Filme | `{servidor}/movie/U/P/{id}.{ext}` |
| Episódio | `{servidor}/series/U/P/{id}.{ext}` |

## Observações
- No campo "URL do Servidor" o usuário pode colar o **link M3U completo**
  (`get.php?username=...&password=...`): usuário e senha são preenchidos sozinhos.
- `network_security_config.xml` libera HTTP, pois quase todo servidor IPTV não usa HTTPS.
- A senha fica salva em texto puro no DataStore (área privada do app). Para mais
  segurança, criptografe com Android Keystore antes de salvar.
- Áudio AC3/E-AC3/DTS pode não tocar em alguns aparelhos. Nesse caso adicione a
  extensão FFmpeg do Media3 (`media3-decoder-ffmpeg`, precisa ser compilada).
- Versões de dependências: combinação estável e testada; o Android Studio vai sugerir
  atualizações, que podem ser aplicadas normalmente.
- Use apenas com listas e conteúdos que você tem direito de acessar.
