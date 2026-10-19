package com.example.iptvplayer.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.iptvplayer.AppContainer
import com.example.iptvplayer.data.model.ContentType
import com.example.iptvplayer.ui.browse.BrowseScreen
import com.example.iptvplayer.ui.home.HomeScreen
import com.example.iptvplayer.ui.login.LoginScreen
import com.example.iptvplayer.ui.player.PlayerScreen
import com.example.iptvplayer.ui.series.SeriesDetailScreen
import kotlinx.coroutines.flow.first

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val BROWSE = "browse/{type}"
    const val SERIES = "series/{seriesId}?name={name}"
    const val PLAYER = "player/{type}/{id}?ext={ext}&title={title}"

    fun browse(type: ContentType) = "browse/${type.name}"

    fun series(seriesId: String, name: String) =
        "series/${Uri.encode(seriesId)}?name=${Uri.encode(name)}"

    fun player(type: ContentType, id: String, ext: String?, title: String) =
        "player/${type.name}/${Uri.encode(id)}?ext=${Uri.encode(ext.orEmpty())}&title=${Uri.encode(title)}"
}

@Composable
fun AppNavHost(container: AppContainer) {
    // Lê as credenciais salvas uma única vez para decidir a tela inicial.
    val startDestination by produceState<String?>(initialValue = null) {
        val saved = container.sessionManager.credentials.first()
        container.repository.credentials = saved
        value = if (saved != null) Routes.HOME else Routes.LOGIN
    }

    val start = startDestination
    if (start == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = start) {

        composable(Routes.LOGIN) {
            LoginScreen(
                container = container,
                onLoggedIn = {
                    nav.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                }
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                container = container,
                onOpen = { type -> nav.navigate(Routes.browse(type)) },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(
            Routes.BROWSE,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { entry ->
            val type = ContentType.valueOf(entry.arguments?.getString("type")!!)
            BrowseScreen(
                container = container,
                type = type,
                onBack = { nav.popBackStack() },
                onItemClick = { item ->
                    if (item.type == ContentType.SERIES) {
                        nav.navigate(Routes.series(item.id, item.name))
                    } else {
                        nav.navigate(Routes.player(item.type, item.id, item.extension, item.name))
                    }
                }
            )
        }

        composable(
            Routes.SERIES,
            arguments = listOf(
                navArgument("seriesId") { type = NavType.StringType },
                navArgument("name") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val args = entry.arguments!!
            SeriesDetailScreen(
                container = container,
                seriesId = args.getString("seriesId")!!,
                fallbackTitle = args.getString("name").orEmpty(),
                onBack = { nav.popBackStack() },
                onEpisodeClick = { episode, seriesName ->
                    nav.navigate(
                        Routes.player(
                            ContentType.SERIES, episode.id, episode.extension,
                            "$seriesName - ${episode.title}"
                        )
                    )
                }
            )
        }

        composable(
            Routes.PLAYER,
            arguments = listOf(
                navArgument("type") { type = NavType.StringType },
                navArgument("id") { type = NavType.StringType },
                navArgument("ext") { type = NavType.StringType; defaultValue = "" },
                navArgument("title") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            val args = entry.arguments!!
            val type = ContentType.valueOf(args.getString("type")!!)
            val urls = remember(entry) {
                runCatching {
                    container.repository.streamUrls(
                        type = type,
                        id = args.getString("id")!!,
                        extension = args.getString("ext")?.ifBlank { null }
                    )
                }.getOrDefault(emptyList())
            }
            PlayerScreen(
                urls = urls,
                title = args.getString("title").orEmpty(),
                isLive = type == ContentType.LIVE,
                onBack = { nav.popBackStack() }
            )
        }
    }
}
