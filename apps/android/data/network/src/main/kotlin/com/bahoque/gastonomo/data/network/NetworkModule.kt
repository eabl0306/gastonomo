package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.port.SessionStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Construye el cliente de la API. Es el unico sitio donde se configuran el JSON, los
 * tiempos de espera y el registro de trafico.
 *
 * ```
 * // en AppContainer
 * val apiClient = createApiClient(sessionStore)
 * ```
 *
 * @param engine se puede sustituir por `MockEngine` en un test sin tocar nada mas.
 */
fun createApiClient(
    sessionStore: SessionStore,
    config: ApiConfig = ApiConfig(),
    engine: HttpClientEngine? = null,
): ApiClient {
    val http = buildHttpClient(config, engine)
    return ApiClient(http, config, TokenRefresher(http, config, sessionStore))
}

/**
 * El JSON de la API. `ignoreUnknownKeys` porque un campo nuevo en una respuesta no puede
 * tumbar una version publicada de la aplicacion.
 */
internal val ApiJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    encodeDefaults = false
}

private fun buildHttpClient(config: ApiConfig, engine: HttpClientEngine?): HttpClient {
    val configure: io.ktor.client.HttpClientConfig<*>.() -> Unit = {
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) {
            connectTimeoutMillis = CONNECT_TIMEOUT_MILLIS
            requestTimeoutMillis = REQUEST_TIMEOUT_MILLIS
            socketTimeoutMillis = REQUEST_TIMEOUT_MILLIS
        }
        if (config.loggingEnabled) {
            // Solo en debug: en release esto imprimiria los tokens en el log.
            install(Logging) { level = LogLevel.HEADERS }
        }
        // La API responde 401, 404 y 422 con cuerpos que hay que leer, asi que un estado de
        // error no puede lanzar excepcion.
        expectSuccess = false
    }
    return if (engine == null) HttpClient(OkHttp, configure) else HttpClient(engine, configure)
}

private const val CONNECT_TIMEOUT_MILLIS = 10_000L
private const val REQUEST_TIMEOUT_MILLIS = 30_000L
