package com.bahoque.gastonomo.data.health

import com.bahoque.gastonomo.core.domain.health.ComponentHealth
import com.bahoque.gastonomo.core.domain.health.HealthReport
import com.bahoque.gastonomo.core.domain.health.HealthStatus
import com.bahoque.gastonomo.core.domain.health.ServiceVersion

/**
 * Traduce el DTO al dominio. Devuelve null cuando la respuesta no respeta el contrato,
 * en vez de lanzar: quien llama decide como reportar el fallo.
 */
internal fun HealthReportDto.toDomain(): HealthReport? {
    val serviceVersion = ServiceVersion.parseOrNull(version)
    val components = checks.map { it.toDomain() }
    if (serviceVersion == null || components.any { it == null }) return null
    return HealthReport(version = serviceVersion, checks = components.filterNotNull())
}

internal fun ComponentHealthDto.toDomain(): ComponentHealth? {
    val parsed = HealthStatus.fromWire(status)
    val usable = parsed != null && parsed != HealthStatus.DEGRADED && name.isNotBlank() && latencyMs >= 0
    return if (usable) ComponentHealth(name = name, status = requireNotNull(parsed), latencyMs = latencyMs) else null
}
