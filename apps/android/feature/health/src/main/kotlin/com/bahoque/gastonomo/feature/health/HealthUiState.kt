package com.bahoque.gastonomo.feature.health

import com.bahoque.gastonomo.core.domain.health.HealthReport

/** Lo unico que la pantalla necesita saber. */
sealed interface HealthUiState {

    data object Loading : HealthUiState

    data class Ready(val report: HealthReport) : HealthUiState

    data class Failed(val reason: HealthFailureReason) : HealthUiState
}

/** Motivo del fallo en terminos de UI. No expone detalles del transporte. */
enum class HealthFailureReason {
    UNREACHABLE,
    UNEXPECTED_RESPONSE,
}
