package com.bahoque.gastonomo.feature.loans.payment

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.feature.loans.MoneyEntryActions
import java.time.LocalDate

/** Lo que el sheet de abono le pide al ViewModel. */
@Immutable
data class PaymentActions(
    val entry: MoneyEntryActions,
    val onFill: (PaymentPortion) -> Unit,
    val onDate: (LocalDate) -> Unit,
    val onSave: () -> Unit,
    val onDismiss: () -> Unit,
)
