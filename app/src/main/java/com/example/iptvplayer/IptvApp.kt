package com.example.iptvplayer

import android.app.Application
import android.content.Context
import com.example.iptvplayer.data.local.SessionManager
import com.example.iptvplayer.data.remote.ApiClient
import com.example.iptvplayer.data.repository.XtreamRepository

/**
 * Application com um "container" simples de dependências (injeção manual).
 * Em projetos maiores, troque por Hilt/Koin.
 */
class IptvApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(context: Context) {
    val sessionManager = SessionManager(context)
    val repository = XtreamRepository(ApiClient.create())
}
