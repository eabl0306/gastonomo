package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpMethod

/**
 * El cliente que usan todos los adaptadores de datos. Hace tres cosas que ninguno de ellos
 * tiene que repetir:
 *
 * 1. Pone el `Bearer` y, ante un 401, refresca **una sola vez** y reintenta. Los refrescos
 *    van serializados en [TokenRefresher], porque presentar un refresh token ya rotado
 *    revoca la familia entera.
 * 2. Traduce `problem+json` a [ApiError], con los `errors[]` por campo.
 * 3. Manda `Idempotency-Key` y lee la cabecera `Idempotent-Replay`.
 *
 * Un adaptador lo usa asi:
 * ```
 * client.get<MovementListDto>("movements") { parameter("from", from.toString()) }
 * client.post<MovementDto, MovementRequest>("movements", body, key)
 * ```
 */
class ApiClient internal constructor(
    private val http: HttpClient,
    private val config: ApiConfig,
    private val refresher: TokenRefresher,
) {

    /** Un GET autenticado. */
    suspend inline fun <reified T> get(
        path: String,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Outcome<T, ApiError> = call(HttpMethod.Get, path, null, null, false, block).decode()

    /**
     * Un POST sin idempotencia: el que no crea dinero.
     *
     * @param anonymous true en login, registro y recuperacion de contrasena. Esas rutas no
     *   llevan Bearer y su 401 significa "credenciales incorrectas", no "sesion caducada":
     *   refrescar ahi taparia el error que la pantalla tiene que ensenar.
     */
    suspend inline fun <reified T> post(
        path: String,
        body: Any?,
        anonymous: Boolean = false,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Outcome<T, ApiError> = call(HttpMethod.Post, path, body, null, anonymous, block).decode()

    /** Un PATCH autenticado. */
    suspend inline fun <reified T> patch(
        path: String,
        body: Any?,
        noinline block: HttpRequestBuilder.() -> Unit = {},
    ): Outcome<T, ApiError> = call(HttpMethod.Patch, path, body, null, false, block).decode()

    /** Un DELETE autenticado. Responde 204 sin cuerpo. */
    suspend fun delete(path: String): Outcome<Unit, ApiError> =
        call(HttpMethod.Delete, path, null, null, false) {}.map { }

    /** Un POST sin cuerpo de respuesta, como logout o los de contrasena. */
    suspend fun postEmpty(
        path: String,
        body: Any? = null,
        anonymous: Boolean = false,
    ): Outcome<Unit, ApiError> = call(HttpMethod.Post, path, body, null, anonymous) {}.map { }

    /**
     * Uno de los tres POST que crean dinero. Manda la clave y devuelve si la respuesta era
     * un reintento: con `isReplay` a true **no hay que volver a pintar el exito**.
     */
    suspend inline fun <reified T> postIdempotent(
        path: String,
        body: Any?,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<T>, ApiError> = call(HttpMethod.Post, path, body, key, false) {}
        .let { outcome ->
            when (outcome) {
                is Outcome.Failure -> outcome
                is Outcome.Success -> Outcome.Success(
                    IdempotentResult(outcome.value.body<T>(), outcome.value.isReplay()),
                )
            }
        }

    /** Ejecuta la peticion, refresca ante un 401 y traduce el error. Es el nucleo de todo. */
    @Suppress("LongParameterList") // Es la firma interna de una peticion HTTP completa.
    @PublishedApi
    internal suspend fun call(
        method: HttpMethod,
        path: String,
        body: Any?,
        idempotencyKey: IdempotencyKey?,
        anonymous: Boolean,
        block: HttpRequestBuilder.() -> Unit,
    ): Outcome<HttpResponse, ApiError> {
        val token = if (anonymous) null else refresher.accessToken()
        val first = execute(method, path, body, idempotencyKey, token, block)
            ?: return Outcome.Failure(ApiError.Network)
        // Si no se mando Bearer, el 401 es la respuesta del propio endpoint (credenciales
        // incorrectas en login) y no una sesion caducada: refrescar aqui taparia el error
        // que la pantalla tiene que ensenar.
        val expired = first.status.value == STATUS_UNAUTHORIZED && token != null
        return if (expired) retry(method, path, body, idempotencyKey, token, block) else first.toOutcome()
    }

    /** Refresca una sola vez y repite la peticion. Si el refresco falla, se acabo la sesion. */
    @Suppress("LongParameterList") // Repite la firma de [call] para poder reintentar igual.
    private suspend fun retry(
        method: HttpMethod,
        path: String,
        body: Any?,
        idempotencyKey: IdempotencyKey?,
        rejected: String?,
        block: HttpRequestBuilder.() -> Unit,
    ): Outcome<HttpResponse, ApiError> {
        val refreshed = refresher.accessToken(rejectedToken = rejected)
            ?: return Outcome.Failure(ApiError.Unauthorized)
        val response = execute(method, path, body, idempotencyKey, refreshed, block)
        return response?.toOutcome() ?: Outcome.Failure(ApiError.Network)
    }

    @Suppress("LongParameterList") // Cada parametro es una parte de la peticion.
    private suspend fun execute(
        method: HttpMethod,
        path: String,
        body: Any?,
        idempotencyKey: IdempotencyKey?,
        token: String?,
        block: HttpRequestBuilder.() -> Unit,
    ): HttpResponse? = runCatching {
        http.request(config.urlOf(path)) {
            this.method = method
            token?.let { header(AUTHORIZATION, "$BEARER $it") }
            idempotencyKey?.let { header(IDEMPOTENCY_KEY, it.value) }
            body?.let { setJsonBody(it) }
            block()
        }
    }.getOrNull()

    companion object {
        internal const val AUTHORIZATION = "Authorization"
        internal const val BEARER = "Bearer"

        /** La cabecera de los tres POST que crean dinero. */
        const val IDEMPOTENCY_KEY = "Idempotency-Key"

        /** La cabecera con la que la API avisa de que esto es un reintento. */
        const val IDEMPOTENT_REPLAY = "Idempotent-Replay"
    }
}
