package com.bahoque.gastonomo.data.health

import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout

private const val CONNECT_TIMEOUT_MS = 3_000L
private const val REQUEST_TIMEOUT_MS = 5_000L

/**
 * Unica forma de construir el adaptador. La URL base sale de BuildConfig.API_BASE_URL,
 * y el engine se puede sustituir desde los tests.
 */
fun createHealthGateway(
    baseUrl: String = BuildConfig.API_BASE_URL,
    engine: HttpClientEngine = OkHttp.create(),
): HealthGateway = KtorHealthGateway(httpClient = createHttpClient(engine), baseUrl = baseUrl.trimEnd('/'))

private fun createHttpClient(engine: HttpClientEngine): HttpClient = HttpClient(engine) {
    expectSuccess = false
    install(HttpTimeout) {
        connectTimeoutMillis = CONNECT_TIMEOUT_MS
        requestTimeoutMillis = REQUEST_TIMEOUT_MS
    }
}
