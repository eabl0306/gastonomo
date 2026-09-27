package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.ApiClientTest.Probe
import com.bahoque.gastonomo.data.network.MockApi.Companion.json
import com.bahoque.gastonomo.data.network.MockApi.Companion.problem
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El refresco es lo mas delicado de la capa de red: presentar un refresh token ya rotado
 * revoca la familia entera de la cuenta y echa al usuario.
 */
class TokenRefresherTest {

    @Test
    fun `a 401 refreshes once and retries the request`() = runTest {
        var businessCalls = 0
        val store = FakeSessionStore(SampleAuth.session)
        val api = MockApi(sessionStore = store) { request ->
            when {
                request.isRefresh() -> json(REFRESHED)
                businessCalls++ == 0 -> problem(401, """{"status":401}""")
                else -> json("""{"name":"ok"}""")
            }
        }
        val result = api.client.get<Probe>("me")
        assertEquals("ok", (result as Outcome.Success).value.name)
        assertEquals(1, store.refreshCount)
        assertEquals("access-2", store.saved?.accessToken)
    }

    @Test
    fun `the retry goes out with the new token`() = runTest {
        var businessCalls = 0
        val api = MockApi { request ->
            when {
                request.isRefresh() -> json(REFRESHED)
                businessCalls++ == 0 -> problem(401, """{"status":401}""")
                else -> json("""{"name":"ok"}""")
            }
        }
        api.client.get<Probe>("me")
        assertEquals("Bearer access-2", api.requests.last().headers["Authorization"])
    }

    @Test
    fun `two requests failing at once only refresh once`() = runTest {
        val store = FakeSessionStore(SampleAuth.session)
        val api = MockApi(sessionStore = store) { request ->
            when {
                request.isRefresh() -> json(REFRESHED)
                // La API rechaza mientras el token sea el viejo, que es lo que de verdad pasa.
                // Contar peticiones hacia que el test dependiera de en que orden se intercalan las
                // dos, y fallaba de vez en cuando: si la primera reintentaba antes de que la
                // segunda hubiera fallado, su reintento se llevaba el 401 que era de la otra.
                request.carriesStaleToken() -> problem(401, """{"status":401}""")
                else -> json("""{"name":"ok"}""")
            }
        }
        val results = listOf(
            async { api.client.get<Probe>("me") },
            async { api.client.get<Probe>("settings") },
        ).awaitAll()
        assertTrue(results.all { it.isSuccess })
        assertEquals(1, store.refreshCount)
        assertEquals(1, api.requests.count { it.isRefresh() })
    }

    @Test
    fun `a failed refresh clears the session and reports unauthorized`() = runTest {
        val store = FakeSessionStore(SampleAuth.session)
        val api = MockApi(sessionStore = store) { request ->
            if (request.isRefresh()) problem(401, """{"status":401}""") else problem(401, """{"status":401}""")
        }
        val result = api.client.get<Probe>("me")
        assertEquals(ApiError.Unauthorized, (result as Outcome.Failure).error)
        assertNull(store.saved)
    }

    @Test
    fun `an unreadable refresh body also clears the session`() = runTest {
        val store = FakeSessionStore(SampleAuth.session)
        val api = MockApi(sessionStore = store) { request ->
            if (request.isRefresh()) json("not json") else problem(401, """{"status":401}""")
        }
        assertTrue(api.client.get<Probe>("me") is Outcome.Failure)
        assertNull(store.saved)
    }

    @Test
    fun `without a refresh token there is nothing to refresh`() = runTest {
        val store = FakeSessionStore(SESSION_WITHOUT_REFRESH)
        val api = MockApi(sessionStore = store) { problem(401, """{"status":401}""") }
        assertEquals(ApiError.Unauthorized, (api.client.get<Probe>("me") as Outcome.Failure).error)
    }

    private fun HttpRequestData.isRefresh(): Boolean =
        url.encodedPath.endsWith("auth/refresh")

    /** Si la peticion todavia lleva el access token de antes del refresco. */
    private fun HttpRequestData.carriesStaleToken(): Boolean =
        headers[HttpHeaders.Authorization]?.contains(SampleAuth.session.accessToken) == true

    private companion object {
        const val REFRESHED =
            """{"accessToken":"access-2","expiresIn":900,"refreshToken":"refresh-2"}"""

        val SESSION_WITHOUT_REFRESH: Session = SampleAuth.session.copy(refreshToken = "")
    }
}
