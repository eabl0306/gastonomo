package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf

/** Una API de mentira para los tests del cliente. */
internal class MockApi(
    val sessionStore: SessionStore = FakeSessionStore(SampleAuth.session),
    handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
) {
    /** Todas las peticiones que llegaron, en orden. */
    val requests = mutableListOf<HttpRequestData>()

    private val engine = MockEngine { request ->
        requests += request
        handler(request)
    }

    val client: ApiClient = createApiClient(
        sessionStore = sessionStore,
        config = ApiConfig(baseUrl = BASE_URL, loggingEnabled = false),
        engine = engine,
    )

    companion object {
        const val BASE_URL = "https://api.test"

        /** Una respuesta JSON correcta. */
        fun MockRequestHandleScope.json(body: String, replay: Boolean = false): HttpResponseData =
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = if (replay) {
                    headersOf(CONTENT_TYPE to listOf(JSON), ApiClient.IDEMPOTENT_REPLAY to listOf("true"))
                } else {
                    headersOf(CONTENT_TYPE, JSON)
                },
            )

        /** Un `problem+json` con el estado que pida el test. */
        fun MockRequestHandleScope.problem(status: Int, body: String): HttpResponseData =
            respond(
                content = body,
                status = HttpStatusCode.fromValue(status),
                headers = headersOf(CONTENT_TYPE, PROBLEM_JSON),
            )

        private const val CONTENT_TYPE = "Content-Type"
        private const val JSON = "application/json"
        private const val PROBLEM_JSON = "application/problem+json"
    }
}
