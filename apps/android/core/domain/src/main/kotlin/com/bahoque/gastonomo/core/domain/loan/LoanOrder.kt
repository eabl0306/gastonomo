package com.bahoque.gastonomo.core.domain.loan

/**
 * Orden de la lista de prestamos: los abiertos primero y los pagados al final. Lo deriva
 * el cliente porque depende de la moneda principal del momento.
 */
object LoanOrder {

    /** Ordena por estado y, dentro de cada grupo, del mas reciente al mas antiguo. */
    fun sorted(loans: List<Pair<Loan, LoanSummary>>): List<Pair<Loan, LoanSummary>> =
        loans.sortedWith(
            compareBy<Pair<Loan, LoanSummary>> { if (it.second.status.isOpen) 0 else 1 }
                .thenByDescending { it.first.openedOn }
                .thenByDescending { it.first.id.value },
        )
}
