package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/** Como acabo un intento de guardar. */
sealed interface SaveResult {

    /**
     * @property announce false cuando la API contesto con `Idempotent-Replay: true`: el
     *   movimiento ya existia de un intento anterior y el toast ya salio entonces.
     */
    data class Saved(val announce: Boolean) : SaveResult

    data class Failed(
        val error: MovementsError,
        val fieldErrors: List<FieldError> = emptyList(),
    ) : SaveResult
}

/**
 * Guarda el movimiento y lleva la clave de idempotencia, que es la parte que hay que hacer bien:
 * **un valor por pulsacion de Guardar, el mismo en todos los reintentos de ese intento**. Si se
 * regenerara, un reintento crearia un segundo movimiento y el diseno no tiene pantalla para
 * borrarlo.
 *
 * La clave se suelta cuando la creacion sale bien y cuando la API responde 409
 * `idempotency_key_reused`, que significa que el cuerpo cambio. Un 422 **no** la quema: el
 * usuario corrige el importe y reintenta con la misma.
 */
internal class MovementSaver(private val dependencies: MovementsDependencies) {

    private var attemptKey: IdempotencyKey? = null

    suspend fun save(draft: MovementDraft): SaveResult {
        val key = attemptKey ?: IdempotencyKey.newKey()
        attemptKey = key
        return when (val result = dependencies.createMovement(draft, key)) {
            is Outcome.Success -> {
                attemptKey = null
                // Un reintento de algo ya creado no vuelve a avisar: el toast ya salio.
                if (!result.value.isReplay) {
                    dependencies.messages.post(messageOf(draft.kind))
                }
                SaveResult.Saved(announce = !result.value.isReplay)
            }
            is Outcome.Failure -> failureOf(result.error)
        }
    }

    private fun messageOf(kind: MovementKind) = if (kind.isIncome) {
        TransientMessages.MOVEMENT_SAVED_INCOME
    } else {
        TransientMessages.MOVEMENT_SAVED_EXPENSE
    }

    private fun failureOf(error: ApiError): SaveResult.Failed {
        if (error is ApiError.Conflict && error.code == ConflictCode.IDEMPOTENCY_KEY_REUSED) {
            attemptKey = null
        }
        return SaveResult.Failed(
            error = movementsErrorOf(error),
            fieldErrors = (error as? ApiError.Validation)?.fields.orEmpty(),
        )
    }
}
