package com.bahoque.gastonomo.data.health

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.errorOrNull
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class KtorHealthGatewayTest {

    @Test
    fun `maps a healthy payload to the domain`() = runTest {
        val report = gatewayReturning(HttpStatusCode.OK, HealthResponses.HEALTHY).fetchHealth().valueOrNull()

        assertEquals(HealthStatus.OK, report?.status)
        assertEquals("0.1.0", report?.version?.value)
        assertEquals(HealthStatus.OK, report?.componentNamed("database")?.status)
        assertEquals(3L, report?.componentNamed("database")?.latencyMs)
    }

    @Test
    fun `a 503 with a body is still a report`() = runTest {
        val gateway = gatewayReturning(HttpStatusCode.ServiceUnavailable, HealthResponses.DEGRADED)

        val report = gateway.fetchHealth().valueOrNull()

        assertEquals(HealthStatus.DEGRADED, report?.status)
        assertEquals(listOf("database"), report?.failingChecks?.map { it.name })
    }

    @Test
    fun `an unexpected status code has no report`() = runTest {
        val gateway = gatewayReturning(HttpStatusCode.InternalServerError, "")

        val error = assertIs<HealthCheckError.UnexpectedStatus>(gateway.fetchHealth().errorOrNull())

        assertEquals(HttpStatusCode.InternalServerError.value, error.code)
    }

    @Test
    fun `a body that is not json is malformed`() = runTest {
        val gateway = gatewayReturning(HttpStatusCode.OK, HealthResponses.NOT_JSON)

        assertIs<HealthCheckError.MalformedResponse>(gateway.fetchHealth().errorOrNull())
    }

    @Test
    fun `an unknown component status is malformed`() = runTest {
        val gateway = gatewayReturning(HttpStatusCode.OK, HealthResponses.UNKNOWN_COMPONENT_STATUS)

        assertIs<HealthCheckError.MalformedResponse>(gateway.fetchHealth().errorOrNull())
    }

    @Test
    fun `a version that is not semver is malformed`() = runTest {
        val gateway = gatewayReturning(HttpStatusCode.OK, HealthResponses.BAD_VERSION)

        assertIs<HealthCheckError.MalformedResponse>(gateway.fetchHealth().errorOrNull())
    }

    @Test
    fun `a network failure is reported as unreachable`() = runTest {
        val engine = MockEngine { throw IOException("connection refused") }
        val gateway = createHealthGateway(baseUrl = BASE_URL, engine = engine)

        val error = assertIs<HealthCheckError.Unreachable>(gateway.fetchHealth().errorOrNull())

        assertEquals("connection refused", error.detail)
    }

    @Test
    fun `the readiness endpoint of the shared contract is the one called`() = runTest {
        val engine = MockEngine { respond(HealthResponses.HEALTHY, HttpStatusCode.OK, JSON_HEADERS) }
        createHealthGateway(baseUrl = "$BASE_URL/", engine = engine).fetchHealth()

        assertEquals("$BASE_URL/health/ready", engine.requestHistory.single().url.toString())
    }

    private fun gatewayReturning(status: HttpStatusCode, body: String): HealthGateway {
        val engine = MockEngine { respond(body, status, JSON_HEADERS) }
        return createHealthGateway(baseUrl = BASE_URL, engine = engine)
    }

    private companion object {
        const val BASE_URL = "http://localhost:8080"
        val JSON_HEADERS = headersOf(HttpHeaders.ContentType, "application/json")
    }
}
