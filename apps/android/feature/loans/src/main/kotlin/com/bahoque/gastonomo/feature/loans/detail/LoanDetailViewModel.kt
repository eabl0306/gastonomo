package com.bahoque.gastonomo.feature.loans.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoanDetail
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.MoneyContextLoader
import com.bahoque.gastonomo.feature.loans.toLoanProblem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Pantalla 9. Pide el prestamo y calcula su resumen con las tasas de hoy.
 *
 * No hay endpoint de abonos: llegan dentro del prestamo, asi que registrar uno obliga a
 * volver a pedir el detalle entero.
 */
class LoanDetailViewModel(
    private val loanId: LoanId,
    private val loadLoanDetail: LoadLoanDetail,
    private val contextLoader: MoneyContextLoader,
) : ViewModel() {

    private val mutable = MutableStateFlow(LoanDetailUiState())

    val state: StateFlow<LoanDetailUiState> = mutable.asStateFlow()

    init {
        reload()
    }

    /** Vuelve a pedir el prestamo. Es lo que hace "Reintentar" y lo que sigue a un abono. */
    fun reload() {
        mutable.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            val context = contextLoader.load()
            when (val result = loadLoanDetail(loanId, context.display, context.rates)) {
                is Outcome.Success -> mutable.value = LoanDetailUiState(
                    loading = false,
                    row = LoanRow(result.value.first, result.value.second),
                    context = context,
                )
                is Outcome.Failure -> mutable.update {
                    it.copy(loading = false, problem = result.error.toLoanProblem())
                }
            }
        }
    }
}
