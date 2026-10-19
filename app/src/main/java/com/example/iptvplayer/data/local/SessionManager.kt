package com.example.iptvplayer.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.iptvplayer.data.model.Credentials
import com.example.iptvplayer.data.model.SortOrder
import com.example.iptvplayer.data.model.ViewMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "iptv_session")

/**
 * Persiste as credenciais com Jetpack DataStore para o usuário não precisar
 * digitar tudo de novo a cada abertura do app.
 */
class SessionManager(private val context: Context) {

    private object Keys {
        val SERVER = stringPreferencesKey("server_url")
        val USERNAME = stringPreferencesKey("username")
        val PASSWORD = stringPreferencesKey("password")
        val EXP_DATE = stringPreferencesKey("exp_date")
        val VIEW_MODE = stringPreferencesKey("view_mode")
        val SORT_ORDER = stringPreferencesKey("sort_order")
    }

    private val prefs: Flow<Preferences> = context.dataStore.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }

    /** Credenciais salvas, ou null se o usuário não estiver logado. */
    val credentials: Flow<Credentials?> = prefs.map { p ->
        val server = p[Keys.SERVER]
        val user = p[Keys.USERNAME]
        val pass = p[Keys.PASSWORD]
        if (server.isNullOrBlank() || user.isNullOrBlank() || pass == null) null
        else Credentials(server, user, pass)
    }

    /** Último servidor usado (mantido mesmo após logout, para preencher o login). */
    val lastServer: Flow<String?> = prefs.map { it[Keys.SERVER] }

    /** Data de expiração da conta (timestamp Unix em segundos, como vem da API). */
    val expDate: Flow<String?> = prefs.map { it[Keys.EXP_DATE] }

    /** Exibição escolhida (grade ou lista), lembrada entre aberturas do app. */
    val viewMode: Flow<ViewMode> = prefs.map { p ->
        p[Keys.VIEW_MODE]?.let { runCatching { ViewMode.valueOf(it) }.getOrNull() } ?: ViewMode.GRID
    }

    val sortOrder: Flow<SortOrder> = prefs.map { p ->
        p[Keys.SORT_ORDER]?.let { runCatching { SortOrder.valueOf(it) }.getOrNull() } ?: SortOrder.DEFAULT
    }

    suspend fun setViewMode(mode: ViewMode) {
        context.dataStore.edit { it[Keys.VIEW_MODE] = mode.name }
    }

    suspend fun setSortOrder(order: SortOrder) {
        context.dataStore.edit { it[Keys.SORT_ORDER] = order.name }
    }

    suspend fun save(credentials: Credentials, expDate: String?) {
        context.dataStore.edit { p ->
            p[Keys.SERVER] = credentials.serverUrl
            p[Keys.USERNAME] = credentials.username
            p[Keys.PASSWORD] = credentials.password
            if (expDate != null) p[Keys.EXP_DATE] = expDate else p.remove(Keys.EXP_DATE)
        }
    }

    suspend fun logout() {
        context.dataStore.edit { p ->
            p.remove(Keys.USERNAME)
            p.remove(Keys.PASSWORD)
            p.remove(Keys.EXP_DATE)
        }
    }
}
