package com.bahoque.gastonomo.core.application.usecase.loan

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.IdempotentResult
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment

/**
 * Registra un abono. La API crea tambien su movimiento: ingreso si preste, gasto si me
 * prestaron, con titulo "Abono - {persona}".
 *
 * Un abono puede pasarse del saldo y no se rechaza: el excedente se ensena como intereses
 * ganados.
 */
class RegisterLoanPayment(private val gateway: LoanGateway) {
    suspend operator fun invoke(
        id: LoanId,
        draft: LoanPaymentDraft,
        key: IdempotencyKey,
    ): Outcome<IdempotentResult<LoanPayment>, ApiError> = gateway.addPayment(id, draft, key)
}
