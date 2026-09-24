package com.bahoque.gastonomo.core.domain.health

/**
 * Salud de una dependencia concreta del servicio, por ejemplo la base de datos.
 */
data class ComponentHealth(
    val name: String,
    val status: HealthStatus,
    val latencyMs: Long,
) {
    init {
        require(name.isNotBlank()) { "Component name must not be blank" }
        require(latencyMs >= 0) { "Component latency must not be negative, was $latencyMs" }
        require(status != HealthStatus.DEGRADED) { "DEGRADED only describes a whole report" }
    }

    val isHealthy: Boolean get() = status.isHealthy
}
