package com.bahoque.gastonomo.core.domain.health

/**
 * Estado de salud tal y como lo publica la API. Un componente solo puede estar OK o ERROR.
 * Un informe completo solo puede estar OK o DEGRADED.
 */
enum class HealthStatus(val wireValue: String) {
    OK("ok"),
    DEGRADED("degraded"),
    ERROR("error"),
    ;

    val isHealthy: Boolean get() = this == OK

    companion object {
        /** Traduce el valor del contrato JSON. Devuelve null si no lo reconoce. */
        fun fromWire(value: String): HealthStatus? =
            entries.firstOrNull { it.wireValue == value.trim().lowercase() }
    }
}
