package com.bahoque.gastonomo.data.network

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.MockApi.Companion.json
import com.bahoque.gastonomo.data.network.MockApi.Companion.problem
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ApiClientTest {

    @Test
    fun `a get carries the bearer token`() = runTest {
        val api = MockApi { json("""{"name":"ok"}""") }
        val result = api.client.get<Probe>("me")
        assertEquals("ok", (result as Outcome.Success).value.name)
        assertEquals(
            "Bearer ${SampleAuth.session.accessToken}",
            api.requests.single().headers["Authorization"],
        )
    }

    @Test
    fun `the path carries the version prefix`() = runTest {
        val api = MockApi { json("""{"name":"ok"}""") }
        api.client.get<Probe>("movements")
        assertEquals("/v1/movements", api.requests.single().url.encodedPath)
    }

    @Test
    fun `a validation error arrives with its fields in spanish`() = runTest {
        val api = MockApi {
            problem(
                status = 422,
                body = """{"status":422,"detail":"Revisa los campos marcados.",
                    "errors":[{"field":"amount","message":"El importe tiene que ser mayor que cero."}]}""",
            )
        }
        val error = (api.client.get<Probe>("movements") as Outcome.Failure).error
        val validation = error as ApiError.Validation
        assertEquals("El importe tiene que ser mayor que cero.", validation.messageFor("amount"))
        assertEquals("Revisa los campos marcados.", validation.detail)
    }

    @Test
    fun `a conflict arrives with its code`() = runTest {
        val api = MockApi {
            problem(409, """{"status":409,"code":"${ConflictCode.IDEMPOTENCY_KEY_REUSED}"}""")
        }
        val error = (api.client.get<Probe>("movements") as Outcome.Failure).error
        assertEquals(ConflictCode.IDEMPOTENCY_KEY_REUSED, (error as ApiError.Conflict).code)
    }

    @Test
    fun `a resource of another account is not found`() = runTest {
        val api = MockApi { problem(404, """{"status":404}""") }
        assertEquals(ApiError.NotFound, (api.client.get<Probe>("loans/x") as Outcome.Failure).error)
    }

    @Test
    fun `a 503 is reported as unavailable so the screen can retry`() = runTest {
        val api = MockApi { problem(503, """{"status":503}""") }
        assertEquals(
            ApiError.ServiceUnavailable,
            (api.client.get<Probe>("exchange-rates") as Outcome.Failure).error,
        )
    }

    @Test
    fun `a rate limit carries the retry delay`() = runTest {
        val api = MockApi { problem(429, """{"status":429}""") }
        val error = (api.client.get<Probe>("auth/login") as Outcome.Failure).error
        assertTrue(error is ApiError.TooManyRequests)
    }

    @Test
    fun `a wrong login is told apart from an expired session`() = runTest {
        val credentials = MockApi(sessionStore = FakeSessionStore()) {
            problem(401, """{"type":"https://gastonomo.app/problems/invalid-credentials"}""")
        }
        assertEquals(
            ApiError.InvalidCredentials,
            (credentials.client.get<Probe>("auth/login") as Outcome.Failure).error,
        )
    }

    @Test
    fun `an unreadable body is an unexpected error and not a crash`() = runTest {
        val api = MockApi { json("not json") }
        val error = (api.client.get<Probe>("me") as Outcome.Failure).error
        assertTrue(error is ApiError.Unexpected)
    }

    @Test
    fun `a transport failure is reported as no network`() = runTest {
        val api = MockApi { throw java.io.IOException("offline") }
        assertEquals(ApiError.Network, (api.client.get<Probe>("me") as Outcome.Failure).error)
    }

    @Test
    fun `a delete answers without a body`() = runTest {
        val api = MockApi { respondError(HttpStatusCode.NoContent) }
        assertTrue(api.client.delete("categories/x").isSuccess)
    }

    @Test
    fun `an idempotent post sends the key and reports a replay`() = runTest {
        val api = MockApi { json("""{"name":"created"}""", replay = true) }
        val result = api.client.postIdempotent<Probe>("movements", Probe("x"), IdempotencyKey("k1"))
        val value = (result as Outcome.Success).value
        assertTrue(value.isReplay)
        assertEquals("k1", api.requests.single().headers[ApiClient.IDEMPOTENCY_KEY])
    }

    @Test
    fun `a first attempt is not a replay`() = runTest {
        val api = MockApi { json("""{"name":"created"}""") }
        val result = api.client.postIdempotent<Probe>("movements", Probe("x"), IdempotencyKey("k1"))
        assertTrue(!(result as Outcome.Success).value.isReplay)
    }

    @Test
    fun `an idempotent post that fails does not pretend to have created anything`() = runTest {
        val api = MockApi { problem(422, """{"status":422}""") }
        val result = api.client.postIdempotent<Probe>("movements", Probe("x"), IdempotencyKey("k1"))
        assertTrue(result is Outcome.Failure)
    }

    @Test
    fun `without a session there is no authorization header`() = runTest {
        val api = MockApi(sessionStore = FakeSessionStore()) { json("""{"name":"ok"}""") }
        api.client.post<Probe>("auth/login", Probe("x"))
        assertNull(api.requests.single().headers["Authorization"])
    }

    @Serializable
    internal data class Probe(@SerialName("name") val name: String)
}
