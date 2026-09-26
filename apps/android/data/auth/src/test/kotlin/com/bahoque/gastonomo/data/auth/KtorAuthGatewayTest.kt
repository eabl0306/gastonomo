package com.bahoque.gastonomo.data.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.ApiConfig
import com.bahoque.gastonomo.data.network.createApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class KtorAuthGatewayTest {

    @Test
    fun `a login maps the session the api sends`() = runTest {
        val gateway = gatewayOf { respondJson(SESSION_BODY) }
        val session = (gateway.login("camilo@correo.com", "secret") as Outcome.Success).value
        assertEquals("access-token", session.accessToken)
        assertEquals("refresh-token", session.refreshToken)
        assertEquals(900, session.expiresInSeconds)
        assertEquals("Camilo Rojas", session.user.name)
    }

    @Test
    fun `a wrong login is invalid credentials and not an expired session`() = runTest {
        val gateway = gatewayOf {
            respond(
                content = """{"type":"https://gastonomo.app/problems/invalid-credentials"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf("Content-Type", "application/problem+json"),
            )
        }
        val error = (gateway.login("camilo@correo.com", "wrong") as Outcome.Failure).error
        assertEquals(ApiError.InvalidCredentials, error)
    }

    @Test
    fun `a registration sends the accepted terms`() = runTest {
        var body = ""
        val gateway = gatewayOf { request ->
            body = request.bodyText()
            respondJson(SESSION_BODY)
        }
        gateway.register(RegistrationRequest("Camilo", "c@c.com", "secret123", true))
        assertTrue(body.contains("\"acceptedTerms\":true"))
    }

    @Test
    fun `a logout sends the refresh token of the device`() = runTest {
        var body = ""
        val gateway = gatewayOf { request ->
            body = request.bodyText()
            respond("", HttpStatusCode.NoContent)
        }
        assertTrue(gateway.logout().isSuccess)
        assertTrue(body.contains("refresh-token"))
    }

    @Test
    fun `logging out without a session does not call the api`() = runTest {
        var called = false
        val gateway = gatewayOf(store = FakeSessionStore()) {
            called = true
            respond("", HttpStatusCode.NoContent)
        }
        assertTrue(gateway.logout().isSuccess)
        assertTrue(!called)
    }

    @Test
    fun `forgetting the password always succeeds`() = runTest {
        val gateway = gatewayOf { respond("", HttpStatusCode.Accepted) }
        assertTrue(gateway.requestPasswordReset("nobody@nowhere.com").isSuccess)
    }

    @Test
    fun `changing the password keeps the current device signed in`() = runTest {
        var body = ""
        val gateway = gatewayOf { request ->
            body = request.bodyText()
            respond("", HttpStatusCode.NoContent)
        }
        gateway.changePassword("old", "new")
        assertTrue(body.contains("refresh-token"))
    }

    @Test
    fun `the profile maps name and email`() = runTest {
        val gateway = gatewayOf {
            respondJson("""{"id":"1","name":"Camilo Rojas","email":"camilo@correo.com"}""")
        }
        val user = (gateway.profile() as Outcome.Success).value
        assertEquals("CR", user.initials)
    }

    private fun gatewayOf(
        store: FakeSessionStore = FakeSessionStore(SampleAuth.session),
        handler: MockHandler,
    ) = createAuthGateway(
        client = createApiClient(
            sessionStore = store,
            config = ApiConfig(baseUrl = "https://api.test", loggingEnabled = false),
            engine = MockEngine { handler(it) },
        ),
        sessionStore = store,
    )

    private fun HttpRequestData.bodyText(): String = (body as? TextContent)?.text.orEmpty()

    private companion object {
        const val SESSION_BODY = """{"accessToken":"access-token","expiresIn":900,
            "refreshToken":"refresh-token",
            "user":{"id":"1","name":"Camilo Rojas","email":"camilo@correo.com"}}"""

        fun MockRequestHandleScope.respondJson(body: String) =
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    }
}

/** Lo que el MockEngine hace con cada peticion. El tipo completo no cabe en una linea. */
private typealias MockHandler =
    suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
