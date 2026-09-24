package com.bahoque.gastonomo.feature.health

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.DispatcherProvider
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.ComponentHealth
import com.bahoque.gastonomo.core.domain.health.HealthReport
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import com.bahoque.gastonomo.core.domain.health.ServiceVersion
import kotlinx.coroutines.CoroutineDispatcher

internal fun healthyReport(): HealthReport = HealthReport(
    version = ServiceVersion("0.1.0"),
    checks = listOf(ComponentHealth("database", HealthStatus.OK, 4)),
)

internal fun degradedReport(): HealthReport = HealthReport(
    version = ServiceVersion("0.1.0"),
    checks = listOf(ComponentHealth("database", HealthStatus.ERROR, 900)),
)

internal fun gatewayReturning(result: Outcome<HealthReport, HealthCheckError>): HealthGateway =
    HealthGateway { result }

/** Todos los dispatchers apuntan al mismo, para que los tests sean deterministas. */
internal class SingleDispatcherProvider(private val dispatcher: CoroutineDispatcher) : DispatcherProvider {
    override val io: CoroutineDispatcher get() = dispatcher
    override val default: CoroutineDispatcher get() = dispatcher
    override val main: CoroutineDispatcher get() = dispatcher
}
