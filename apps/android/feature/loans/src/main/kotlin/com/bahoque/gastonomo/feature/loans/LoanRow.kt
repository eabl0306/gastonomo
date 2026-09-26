package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanSummary

/**
 * Un prestamo con su resumen ya calculado. Es lo que pinta una tarjeta de la lista y lo que
 * pinta el detalle: el saldo, el estado y el porcentaje dependen de la moneda principal del
 * momento, asi que viajan juntos y nadie los vuelve a calcular.
 */
@Immutable
data class LoanRow(val loan: Loan, val summary: LoanSummary)
