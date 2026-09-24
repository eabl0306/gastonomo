package com.bahoque.gastonomo.data.health

import kotlinx.serialization.Serializable

/**
 * Espejo exacto del JSON que publica la API. Vive separado del dominio para que un cambio
 * en el contrato no obligue a tocar el nucleo.
 */
@Serializable
internal data class HealthReportDto(
    val status: String,
    val version: String,
    val checks: List<ComponentHealthDto> = emptyList(),
)

@Serializable
internal data class ComponentHealthDto(
    val name: String,
    val status: String,
    val latencyMs: Long = 0,
)
