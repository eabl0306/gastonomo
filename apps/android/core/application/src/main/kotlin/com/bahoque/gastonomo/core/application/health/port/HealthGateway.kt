package com.bahoque.gastonomo.core.application.health.port

import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.health.HealthReport

/**
 * Puerto driven. El nucleo pide el informe de salud sin saber si viene de HTTP,
 * de una cache o de un doble de test.
 */
fun interface HealthGateway {
    suspend fun fetchHealth(): Outcome<HealthReport, HealthCheckError>
}
