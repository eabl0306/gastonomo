package com.bahoque.gastonomo.core.application.health.port

/**
 * Fallos que el puerto de salud puede devolver. No son excepciones: forman parte
 * del contrato del puerto y obligan a tratarlos.
 */
sealed interface HealthCheckError {

    /** No se pudo contactar con la API. */
    data class Unreachable(val detail: String) : HealthCheckError

    /** La API contesto con un codigo que no expone un informe de salud. */
    data class UnexpectedStatus(val code: Int) : HealthCheckError

    /** La respuesta no respeta el contrato de /health. */
    data class MalformedResponse(val detail: String) : HealthCheckError
}
