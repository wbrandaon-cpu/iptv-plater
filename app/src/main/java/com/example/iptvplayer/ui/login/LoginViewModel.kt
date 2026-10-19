package com.example.iptvplayer.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.local.SessionManager
import com.example.iptvplayer.data.model.Credentials
import com.example.iptvplayer.data.repository.XtreamRepository
import com.example.iptvplayer.util.ServerUrlParser
import com.example.iptvplayer.util.toUserMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

data class LoginUiState(
    val server: String = "",
    val username: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

class LoginViewModel(
    private val repository: XtreamRepository,
    private val session: SessionManager
) : ViewModel() {

    var state by mutableStateOf(LoginUiState())
        private set

    init {
        // Preenche com o último servidor usado (útil após logout)
        viewModelScope.launch {
            val last = session.lastServer.first()
            if (!last.isNullOrBlank() && state.server.isEmpty()) {
                state = state.copy(server = last)
            }
        }
    }

    fun onServerChange(value: String) {
        // Se o usuário colar o link M3U completo, extraímos usuário e senha dele.
        val parsed = if (value.contains("username=", ignoreCase = true)) ServerUrlParser.parse(value) else null
        state = state.copy(
            server = value,
            username = parsed?.username ?: state.username,
            password = parsed?.password ?: state.password,
            error = null
        )
    }

    fun onUsernameChange(value: String) {
        state = state.copy(username = value, error = null)
    }

    fun onPasswordChange(value: String) {
        state = state.copy(password = value, error = null)
    }

    fun login(onSuccess: () -> Unit) {
        if (state.isLoading) return

        val parsed = ServerUrlParser.parse(state.server)
        if (parsed == null) {
            state = state.copy(error = "Informe uma URL de servidor válida.")
            return
        }
        val user = state.username.trim()
        val pass = state.password.trim()
        if (user.isEmpty() || pass.isEmpty()) {
            state = state.copy(error = "Preencha usuário e senha.")
            return
        }

        state = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                val credentials = Credentials(parsed.baseUrl, user, pass)
                val info = repository.login(credentials)
                session.save(credentials, info.expDate)
                state = state.copy(isLoading = false)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.toUserMessage())
            }
        }
    }
}
