package com.bahoque.gastonomo.core.application.usecase.loan

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan

/**
 * Crea un prestamo. La API crea tambien su movimiento en la misma escritura: gasto si
 * preste, ingreso si me prestaron, con titulo "Prestamo - {persona}".
 *
 * Necesita `Idempotency-Key` porque son dos registros de dinero de una vez.
 */
class CreateLoan(private val gateway: LoanGateway) {
    suspend operator fun invoke(
        draft: LoanDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<Loan>, ApiError> = gateway.create(draft, key)
}
