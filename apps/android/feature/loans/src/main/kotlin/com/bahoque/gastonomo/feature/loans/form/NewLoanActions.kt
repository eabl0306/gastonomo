package com.bahoque.gastonomo.feature.loans.form

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.MoneyEntryActions
import java.time.LocalDate

/** Lo que el formulario le pide al ViewModel. */
@Immutable
data class NewLoanActions(
    val entry: MoneyEntryActions,
    val onDirection: (LoanDirection) -> Unit,
    val onPerson: (String) -> Unit,
    val onNote: (String) -> Unit,
    val onDate: (LocalDate) -> Unit,
    val onSave: () -> Unit,
    val onClose: () -> Unit,
)
