package com.bahoque.gastonomo.core.application.usecase.movement

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.movement.Movement

/**
 * Crea un movimiento. Recibe la clave de idempotencia desde fuera **a proposito**: la
 * clave pertenece al intento del usuario, no a la llamada, asi que la crea el ViewModel al
 * pulsar Guardar y la repite en cada reintento.
 *
 * Si el resultado llega con `isReplay` a true, esto era un reintento de algo ya creado: no
 * hay que volver a ensenar el toast.
 */
class CreateMovement(private val gateway: MovementGateway) {
    suspend operator fun invoke(
        draft: MovementDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Movement>, ApiError> = gateway.create(draft, key)
}
