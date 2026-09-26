package com.bahoque.gastonomo.core.application.usecase.loan

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * El detalle de un prestamo con sus abonos, del mas reciente al mas antiguo, y su resumen.
 * Un prestamo de otra cuenta responde 404, nunca 403.
 */
class LoadLoanDetail(private val gateway: LoanGateway) {
    suspend operator fun invoke(
        id: LoanId,
        display: CurrencyCode,
        rates: ExchangeRates,
    ): Outcome<Pair<Loan, LoanSummary>, ApiError> = gateway.detail(id).map { loan ->
        val ordered = loan.copy(payments = loan.payments.sortedByDescending { it.paidOn })
        ordered to LoanSummary.of(ordered, display, rates)
    }
}
