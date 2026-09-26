package com.bahoque.gastonomo.core.application.usecase.movement

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period

/**
 * Los ultimos movimientos del periodo, que es lo que ensena "Ultimos movimientos" de
 * Inicio. Pide `limit=4` y ya vienen ordenados del mas reciente al mas antiguo.
 */
class LoadRecentMovements(private val gateway: MovementGateway) {
    suspend operator fun invoke(
        period: Period,
        limit: Int = RECENT_LIMIT,
    ): Outcome<List<Movement>, ApiError> =
        gateway.list(period.from, period.to, null, null, limit).map { it.items }

    companion object {
        /** Los cuatro que ensena Inicio. */
        const val RECENT_LIMIT = 4
    }
}
