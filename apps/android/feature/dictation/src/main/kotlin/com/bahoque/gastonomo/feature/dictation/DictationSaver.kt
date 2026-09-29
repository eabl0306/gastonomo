package com.bahoque.gastonomo.feature.dictation

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.port.DictatedEntry
import com.bahoque.gastonomo.core.application.port.DictationDraft
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount

/** Como acabo el guardado de un borrador dictado. */
internal sealed interface DictationSaveResult {

    /**
     * @property isLoan true si se creo un prestamo, que es lo que decide a que pantalla hay que
     *   avisarle de que su lista quedo desfasada.
     * @property announce false cuando la API contesto `Idempotent-Replay: true`: ya existia de un
     *   intento anterior y el aviso ya salio entonces.
     */
    data class Saved(val isLoan: Boolean, val announce: Boolean) : DictationSaveResult

    data class Failed(val problem: DictationProblem) : DictationSaveResult
}

/**
 * Guarda el borrador y lleva la clave de idempotencia.
 *
 * **Un valor por pulsacion de Guardar, el mismo en todos los reintentos de ese intento.** Si se
 * regenerara, un reintento tras un timeout que si llego crearia el registro dos veces, y no hay
 * pantalla para borrarlo.
 *
 * La clave se suelta cuando la creacion sale bien y cuando la API responde 409
 * `idempotency_key_reused`, que significa que el cuerpo cambio.
 */
internal class DictationSaver(private val dependencies: DictationDependencies) {

    private var attemptKey: IdempotencyKey? = null

    suspend fun save(draft: DictationDraft, money: MonetaryAmount): DictationSaveResult {
        val key = attemptKey ?: IdempotencyKey.newKey()
        attemptKey = key
        return when (val entry = draft.entry) {
            is DictatedEntry.Movement -> saveMovement(draft, entry, money, key)
            is DictatedEntry.Loan -> saveLoan(draft, entry, money, key)
        }
    }

    private suspend fun saveMovement(
        draft: DictationDraft,
        entry: DictatedEntry.Movement,
        money: MonetaryAmount,
        key: IdempotencyKey,
    ): DictationSaveResult {
        val movement = MovementDraft(
            occurredOn = draft.occurredOn,
            kind = entry.kind,
            categoryId = entry.category.id,
            money = money,
            note = entry.note.takeIf { it.isNotBlank() },
        )
        return when (val result = dependencies.createMovement(movement, key)) {
            is Outcome.Success -> {
                attemptKey = null
                if (!result.value.isReplay) dependencies.messages.post(messageOf(entry))
                DictationSaveResult.Saved(isLoan = false, announce = !result.value.isReplay)
            }
            is Outcome.Failure -> failureOf(result.error)
        }
    }

    private suspend fun saveLoan(
        draft: DictationDraft,
        entry: DictatedEntry.Loan,
        money: MonetaryAmount,
        key: IdempotencyKey,
    ): DictationSaveResult {
        val loan = LoanDraft(
            person = entry.person,
            direction = entry.direction,
            openedOn = draft.occurredOn,
            money = money,
            note = entry.note.takeIf { it.isNotBlank() },
        )
        return when (val result = dependencies.createLoan(loan, key)) {
            is Outcome.Success -> {
                attemptKey = null
                DictationSaveResult.Saved(isLoan = true, announce = !result.value.isReplay)
            }
            is Outcome.Failure -> failureOf(result.error)
        }
    }

    /**
     * El aviso de un movimiento lo deja el propio guardado, porque sale encima de la pantalla
     * desde la que se dicto y esta ya se cerro. El de un prestamo lo pinta Prestamos con su
     * senal, igual que cuando se crea desde el formulario.
     */
    private fun messageOf(entry: DictatedEntry.Movement) = if (entry.kind.isIncome) {
        TransientMessages.MOVEMENT_SAVED_INCOME
    } else {
        TransientMessages.MOVEMENT_SAVED_EXPENSE
    }

    private fun failureOf(error: ApiError): DictationSaveResult.Failed {
        if (error is ApiError.Conflict && error.code == ConflictCode.IDEMPOTENCY_KEY_REUSED) {
            attemptKey = null
        }
        return DictationSaveResult.Failed(DictationProblem.ofSave(error))
    }
}
