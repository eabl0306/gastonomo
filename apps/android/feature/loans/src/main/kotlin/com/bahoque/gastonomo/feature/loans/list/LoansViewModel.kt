package com.bahoque.gastonomo.feature.loans.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.loan.LoanTotals
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.MoneyContext
import com.bahoque.gastonomo.feature.loans.MoneyContextLoader
import com.bahoque.gastonomo.feature.loans.toLoanProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla 8. Carga los prestamos con las tasas de hoy y deja el filtro de las pills en
 * local: las dos tarjetas de arriba tienen que seguir contando los dos lados aunque la
 * lista este filtrada, asi que se piden todos.
 *
 * El periodo no entra: "Te deben" y "Debes" son saldos de hoy, no del mes que se este
 * mirando.
 */
class LoansViewModel(
    private val loadLoans: LoadLoans,
    private val contextLoader: MoneyContextLoader,
) : ViewModel() {

    private val mutable = MutableStateFlow(LoansUiState())

    val state: StateFlow<LoansUiState> = mutable.asStateFlow()

    init {
        reload()
    }

    /** Vuelve a pedir la lista. Es lo que hace "Reintentar" y lo que sigue a un abono. */
    fun reload() {
        mutable.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            val context = contextLoader.load()
            when (val result = loadLoans(context.display, context.rates)) {
                is Outcome.Success -> mutable.update { it.withLoans(result.value, context) }
                is Outcome.Failure -> mutable.update {
                    it.copy(loading = false, problem = result.error.toLoanProblem())
                }
            }
        }
    }

    fun onFilter(direction: LoanDirection?) = mutable.update { it.copy(filter = direction) }

    private fun LoansUiState.withLoans(
        loans: List<Pair<Loan, LoanSummary>>,
        context: MoneyContext,
    ): LoansUiState = copy(
        loading = false,
        problem = null,
        rows = loans.map { (loan, summary) -> LoanRow(loan, summary) },
        owedToMe = LoanTotals.of(loans, LoanDirection.LENT, context.display),
        iOwe = LoanTotals.of(loans, LoanDirection.BORROWED, context.display),
        context = context,
    )
}
