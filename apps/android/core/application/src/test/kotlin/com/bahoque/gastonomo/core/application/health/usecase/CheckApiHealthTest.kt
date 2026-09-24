package com.bahoque.gastonomo.core.application.health.usecase

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.errorOrNull
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.health.ComponentHealth
import com.bahoque.gastonomo.core.domain.health.HealthReport
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import com.bahoque.gastonomo.core.domain.health.ServiceVersion
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class CheckApiHealthTest {

    @Test
    fun `returns the report when the contract is honoured`() = runTest {
        val report = reportWith(ComponentHealth("database", HealthStatus.OK, 4))
        val gateway = FakeHealthGateway(Outcome.Success(report))

        val result = CheckApiHealth(gateway)()

        assertEquals(report, result.valueOrNull())
        assertEquals(1, gateway.calls)
    }

    @Test
    fun `a degraded report is still a successful outcome`() = runTest {
        val report = reportWith(ComponentHealth("database", HealthStatus.ERROR, 900))
        val gateway = FakeHealthGateway(Outcome.Success(report))

        val result = CheckApiHealth(gateway)()

        assertEquals(HealthStatus.DEGRADED, result.valueOrNull()?.status)
    }

    @Test
    fun `a report without the database check breaks the contract`() = runTest {
        val report = reportWith(ComponentHealth("cache", HealthStatus.OK, 1))
        val gateway = FakeHealthGateway(Outcome.Success(report))

        val result = CheckApiHealth(gateway)()

        val error = assertIs<HealthCheckError.MalformedResponse>(result.errorOrNull())
        assertEquals("missing 'database' check", error.detail)
    }

    @Test
    fun `gateway failures are propagated untouched`() = runTest {
        val failure = HealthCheckError.Unreachable("connection refused")
        val gateway = FakeHealthGateway(Outcome.Failure(failure))

        val result = CheckApiHealth(gateway)()

        assertEquals(failure, result.errorOrNull())
    }

    private fun reportWith(vararg checks: ComponentHealth) =
        HealthReport(ServiceVersion("0.1.0"), checks.toList())
}
