package com.bahoque.gastonomo.core.application.usecase.loan

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.LoanGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanOrder
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * Los prestamos con su resumen ya calculado y en el orden del diseno: abiertos primero y
 * pagados al final.
 *
 * El periodo no entra aqui: "Me deben" y "Debo" son siempre saldos actuales.
 */
class LoadLoans(private val gateway: LoanGateway) {
    suspend operator fun invoke(
        display: CurrencyCode,
        rates: ExchangeRates,
        direction: LoanDirection? = null,
    ): Outcome<List<Pair<Loan, LoanSummary>>, ApiError> =
        gateway.list(direction).map { loans ->
            LoanOrder.sorted(loans.map { it to LoanSummary.of(it, display, rates) })
        }
}
