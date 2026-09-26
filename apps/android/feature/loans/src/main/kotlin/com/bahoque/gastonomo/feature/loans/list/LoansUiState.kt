package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanTotals
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.feature.loans.LoanProblem
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.MoneyContext

/**
 * Pantalla 8. La lista de prestamos con sus dos tarjetas de totales y sus pills.
 *
 * Las pills filtran solo la lista: las tarjetas de arriba siguen contando los dos lados,
 * asi que el estado guarda todos los prestamos y el filtro se aplica al pintar.
 *
 * @property filter null es la pill "Todos".
 */
@Immutable
data class LoansUiState(
    val loading: Boolean = true,
    val problem: LoanProblem? = null,
    val rows: List<LoanRow> = emptyList(),
    val filter: LoanDirection? = null,
    val owedToMe: LoanTotals = LoanTotals.none(CurrencyCode.COP),
    val iOwe: LoanTotals = LoanTotals.none(CurrencyCode.COP),
    val context: MoneyContext = MoneyContext(),
) {
    /** Las tarjetas que se ensenan con el filtro puesto. */
    val visible: List<LoanRow>
        get() = rows.filter { filter == null || it.loan.direction == filter }

    /** El estado vacio sale cuando ya se cargo, no hubo fallo y el filtro no deja nada. */
    val isEmpty: Boolean get() = !loading && problem == null && visible.isEmpty()
}
