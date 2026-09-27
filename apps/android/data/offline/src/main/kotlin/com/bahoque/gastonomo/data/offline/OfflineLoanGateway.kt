package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.data.database.local.LocalLoans

/**
 * Prestamos leidos de la base local, con la red solo rellenandola.
 *
 * El estado, el porcentaje y el orden los sigue derivando el cliente sobre lo guardado, que son
 * los cuatro campos crudos: la conversion se hace al pintar, con las tasas del momento.
 */
internal class OfflineLoanGateway(
    private val remote: LoanGateway,
    private val local: LocalLoans,
    private val gate: RefreshGate,
) : LoanGateway {

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> {
        val stored = local.all(direction)
        if (stored.isNotEmpty() || gate.isFresh(LIST_KEY)) {
            gate.inBackground(LIST_KEY) { refreshAll() }
            return Outcome.Success(stored)
        }
        return refreshAll().map { local.all(direction) }
    }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> {
        val stored = local.byId(id)
        if (stored != null) {
            gate.inBackground(id) { refreshOne(id) }
            return Outcome.Success(stored)
        }
        return remote.detail(id).also { it.storeIfSuccess() }
    }

    override suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> {
        val result = remote.create(draft, key)
        if (result is Outcome.Success) {
            local.save(result.value.value)
            // Crear un prestamo crea ademas su movimiento, asi que el periodo tambien cambio.
            gate.forget()
        }
        return result
    }

    override suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> {
        val result = remote.addPayment(id, draft, key)
        if (result is Outcome.Success) {
            gate.forget()
            // El abono llega suelto: el prestamo con su saldo nuevo hay que volver a pedirlo.
            refreshOne(id)
        }
        return result
    }

    private suspend fun refreshAll(): Outcome<Unit, ApiError> = remote.list(null).map { loans ->
        local.replaceAll(loans)
        gate.markFresh(LIST_KEY)
    }

    private suspend fun refreshOne(id: LoanId) {
        remote.detail(id).storeIfSuccess()
    }

    private suspend fun Outcome<Loan, ApiError>.storeIfSuccess() {
        if (this is Outcome.Success) {
            local.save(value)
            gate.markFresh(value.id)
        }
    }

    private companion object {
        /** La lista entera se refresca junta: las pills filtran en local. */
        const val LIST_KEY = "loans"
    }
}
