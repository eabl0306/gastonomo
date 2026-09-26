package com.bahoque.gastonomo.feature.loans.detail

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.feature.loans.LoanProblem
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.MoneyContext

/**
 * Pantalla 9. El detalle de un prestamo con sus abonos.
 *
 * @property row null mientras carga o si la lectura fallo. Los abonos llegan dentro del
 *   prestamo y ya ordenados del mas reciente al mas antiguo por el caso de uso.
 */
@Immutable
data class LoanDetailUiState(
    val loading: Boolean = true,
    val problem: LoanProblem? = null,
    val row: LoanRow? = null,
    val context: MoneyContext = MoneyContext(),
)
