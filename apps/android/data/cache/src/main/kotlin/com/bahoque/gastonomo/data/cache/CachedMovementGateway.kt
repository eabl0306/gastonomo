package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.time.LocalDate

/**
 * Movimientos con cache en memoria delante. La pantalla no se entera: recibe el mismo puerto.
 *
 * Los importes se guardan **como los devuelve la API**, con sus cuatro campos y sin convertir.
 * Cachear un importe ya convertido lo ataria a la moneda principal y a las tasas del momento
 * en que se pidio, y cambiar la moneda en Ajustes dejaria de cambiar lo que se ve.
 */
internal class CachedMovementGateway(
    private val delegate: MovementGateway,
    private val caches: GatewayCaches,
) : MovementGateway {

    override suspend fun list(
        from: LocalDate,
        to: LocalDate,
        kind: MovementKind?,
        cursor: String?,
        limit: Int?,
    ): Outcome<MovementPage, ApiError> =
        caches.movements.read(MovementQuery(from, to, kind, cursor, limit)) {
            delegate.list(from, to, kind, cursor, limit)
        }

    override suspend fun create(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> {
        val result = delegate.create(draft, key)
        // Solo si salio bien: un 422 no cambio nada en el servidor, asi que tirar la cache
        // seria obligar a una recarga sin motivo.
        if (result is Outcome.Success) caches.invalidateAfterMoneyWrite()
        return result
    }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedMovementGateway(gateway: MovementGateway, caches: GatewayCaches): MovementGateway =
    CachedMovementGateway(gateway, caches)
