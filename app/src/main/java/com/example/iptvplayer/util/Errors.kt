package com.example.iptvplayer.util

import com.google.gson.JsonParseException
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/** Exceção com mensagem já pronta para mostrar ao usuário. */
class AppException(override val message: String, cause: Throwable? = null) : Exception(message, cause)

/** Converte exceções técnicas em mensagens amigáveis em português. */
fun Throwable.toUserMessage(): String = when (this) {
    is AppException -> message
    is UnknownHostException -> "Servidor não encontrado. Verifique a URL e sua conexão."
    is ConnectException -> "Não foi possível conectar ao servidor."
    is SocketTimeoutException -> "O servidor demorou demais para responder."
    is SSLException -> "Erro de SSL. Tente usar http:// em vez de https://."
    is HttpException -> when (code()) {
        401, 403 -> "Acesso negado: usuário ou senha inválidos."
        404 -> "API não encontrada. Verifique a URL do servidor."
        else -> "Erro do servidor (HTTP ${code()})."
    }
    is JsonParseException, is IllegalStateException -> "Resposta inesperada do servidor."
    is IllegalArgumentException -> "URL do servidor inválida."
    else -> message ?: "Erro desconhecido."
}
