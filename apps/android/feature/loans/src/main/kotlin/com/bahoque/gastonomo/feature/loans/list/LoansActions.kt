package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.LoanRow

/** Lo que la lista de prestamos le pide a quien la hospeda. */
@Immutable
data class LoansActions(
    val onFilter: (LoanDirection?) -> Unit,
    val onOpenLoan: (LoanRow) -> Unit,
    val onRegisterPayment: (LoanRow) -> Unit,
    val onNewLoan: () -> Unit,
    val onNewMovement: () -> Unit,
    val onDictate: () -> Unit,
    val onRetry: () -> Unit,
) {
    companion object {
        /** Unas acciones que no hacen nada, para tests y capturas. */
        val NONE = LoansActions(
            onFilter = {},
            onOpenLoan = {},
            onRegisterPayment = {},
            onNewLoan = {},
            onNewMovement = {},
            onDictate = {},
            onRetry = {},
        )
    }
}
