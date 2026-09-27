package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment

/**
 * Prestamos con cache en memoria delante.
 *
 * La lista y el detalle van en caches separadas porque se piden desde sitios distintos, pero
 * las dos se invalidan juntas: un abono cambia el detalle del prestamo y tambien su saldo en
 * la lista.
 *
 * El estado, el porcentaje y el orden los sigue derivando el cliente sobre lo guardado, que
 * es la respuesta cruda de la API.
 */
internal class CachedLoanGateway(
    private val delegate: LoanGateway,
    private val caches: GatewayCaches,
) : LoanGateway {

    override suspend fun list(direction: LoanDirection?): Outcome<List<Loan>, ApiError> =
        caches.loans.read(direction) { delegate.list(direction) }

    override suspend fun detail(id: LoanId): Outcome<Loan, ApiError> =
        caches.loanDetail.read(id) { delegate.detail(id) }

    override suspend fun create(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> {
        val result = delegate.create(draft, key)
        if (result is Outcome.Success) caches.invalidateAfterMoneyWrite()
        return result
    }

    override suspend fun addPayment(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> {
        val result = delegate.addPayment(id, draft, key)
        if (result is Outcome.Success) caches.invalidateAfterMoneyWrite()
        return result
    }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedLoanGateway(gateway: LoanGateway, caches: GatewayCaches): LoanGateway =
    CachedLoanGateway(gateway, caches)
