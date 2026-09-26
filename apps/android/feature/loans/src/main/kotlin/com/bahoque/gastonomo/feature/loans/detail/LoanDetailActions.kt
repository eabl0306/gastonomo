package com.bahoque.gastonomo.feature.loans.detail

import androidx.compose.runtime.Immutable
import com.bahoque.gastonomo.feature.loans.LoanRow

/** Lo que el detalle le pide a quien lo hospeda. */
@Immutable
data class LoanDetailActions(
    val onBack: () -> Unit,
    val onRegisterPayment: (LoanRow) -> Unit,
    val onRetry: () -> Unit,
)
