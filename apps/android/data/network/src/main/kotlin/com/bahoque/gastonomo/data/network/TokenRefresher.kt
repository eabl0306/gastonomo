package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Refresca el par de tokens, y lo hace **de uno en uno**.
 *
 * Esto es lo mas delicado de la capa de red. `POST /v1/auth/refresh` rota: el token usado
 * queda revocado y apunta al nuevo. Presentar un token ya rotado significa, para la API,
 * que alguien tiene una copia, asi que **revoca la familia entera** y echa al usuario.
 *
 * Si dos peticiones fallan con 401 a la vez y las dos refrescan, la segunda presenta un
 * token que la primera ya rotó y la aplicacion se auto expulsa. El mutex lo impide: solo
 * una refresca, la otra espera y se encuentra el token nuevo ya guardado.
 *
 * Si el refresco falla, se limpia la sesion: el refresh token guardado ya no sirve para
 * nada y guardarlo solo invita a volver a intentarlo y perder la familia.
 */
internal class TokenRefresher(
    private val client: HttpClient,
    private val config: ApiConfig,
    private val sessionStore: SessionStore,
) {
    private val mutex = Mutex()

    /**
     * Un access token valido, refrescando si hace falta.
     *
     * @param rejectedToken el token que la API acaba de rechazar con un 401, o null en la
     *   primera llamada. Si mientras se esperaba el mutex otra peticion ya refresco, el
     *   token guardado sera distinto del rechazado y se usa sin gastar otro refresco.
     */
    suspend fun accessToken(rejectedToken: String? = null): String? = mutex.withLock {
        val current = sessionStore.accessToken()
        if (current != null && current != rejectedToken) return@withLock current
        refreshLocked()
    }

    private suspend fun refreshLocked(): String? {
        val refreshToken = sessionStore.refreshToken() ?: return null
        val body = rotate(refreshToken)
        if (body == null) {
            // El refresh token guardado ya no sirve. Dejarlo solo invita a presentarlo otra
            // vez, y eso es lo que revoca la familia entera de la cuenta.
            sessionStore.clear()
            return null
        }
        sessionStore.replaceTokens(TokenPair(body.accessToken, body.expiresIn, body.refreshToken))
        return body.accessToken
    }

    /** Pide el par nuevo. Devuelve null si la peticion fallo o el cuerpo no se pudo leer. */
    private suspend fun rotate(refreshToken: String): RefreshResponse? {
        val response = runCatching { requestRefresh(refreshToken) }.getOrNull()
        if (response == null || !response.status.isSuccess()) return null
        return runCatching { response.body<RefreshResponse>() }.getOrNull()
    }

    private suspend fun requestRefresh(refreshToken: String): HttpResponse =
        client.post("${config.baseUrl}/${ApiConfig.VERSION_PATH}/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(RefreshRequest(refreshToken))
        }
}

@Serializable
private data class RefreshRequest(@SerialName("refreshToken") val refreshToken: String)

@Serializable
private data class RefreshResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("expiresIn") val expiresIn: Long,
    @SerialName("refreshToken") val refreshToken: String,
)
