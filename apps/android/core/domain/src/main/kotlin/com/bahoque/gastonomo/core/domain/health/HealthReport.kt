package com.bahoque.gastonomo.core.domain.health

/**
 * Informe de salud del servicio. La regla del dominio es unica y vive aqui:
 * un solo componente en error degrada el informe completo.
 */
data class HealthReport(
    val version: ServiceVersion,
    val checks: List<ComponentHealth>,
) {
    val status: HealthStatus
        get() = if (checks.all { it.isHealthy }) HealthStatus.OK else HealthStatus.DEGRADED

    val isHealthy: Boolean get() = status.isHealthy

    /** Componentes que estan fallando, en el orden en que los publica la API. */
    val failingChecks: List<ComponentHealth> get() = checks.filterNot { it.isHealthy }

    fun componentNamed(name: String): ComponentHealth? = checks.firstOrNull { it.name == name }
}
