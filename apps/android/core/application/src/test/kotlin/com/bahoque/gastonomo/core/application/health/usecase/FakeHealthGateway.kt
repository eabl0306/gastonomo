package com.bahoque.gastonomo.core.application.health.usecase

import com.bahoque.gastonomo.core.application.health.port.HealthCheckError
import com.bahoque.gastonomo.core.application.health.port.HealthGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.HealthReport

/** Doble del puerto: devuelve siempre el resultado que le configuren. */
class FakeHealthGateway(
    private val result: Outcome<HealthReport, HealthCheckError>,
) : HealthGateway {

    var calls: Int = 0
        private set

    override suspend fun fetchHealth(): Outcome<HealthReport, HealthCheckError> {
        calls += 1
        return result
    }
}
