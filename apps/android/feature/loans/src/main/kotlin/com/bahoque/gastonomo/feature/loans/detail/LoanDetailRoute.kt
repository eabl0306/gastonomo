package com.bahoque.gastonomo.feature.loans.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.feature.loans.LoansDependencies
import com.bahoque.gastonomo.feature.loans.R
import com.bahoque.gastonomo.feature.loans.moneyContextLoader
import com.bahoque.gastonomo.feature.loans.payment.LoanPaymentOverlay
import com.bahoque.gastonomo.feature.loans.payment.LoanPaymentViewModel

/**
 * Pantalla 9 montada, con el sheet de abono encima y el toast que lo confirma.
 *
 * Los abonos llegan dentro del prestamo y no hay endpoint para pedirlos sueltos, asi que al
 * registrar uno se vuelve a pedir el detalle completo.
 */
@Composable
fun LoanDetailRoute(
    dependencies: LoansDependencies,
    loanId: LoanId,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail: LoanDetailViewModel = viewModel {
        LoanDetailViewModel(loanId, dependencies.loadLoanDetail, dependencies.moneyContextLoader())
    }
    val payment: LoanPaymentViewModel = viewModel {
        LoanPaymentViewModel(dependencies.registerLoanPayment, dependencies.clock)
    }
    val state by detail.state.collectAsStateWithLifecycle()
    var toast by rememberSaveable { mutableStateOf<String?>(null) }
    val registered = stringResource(R.string.payment_registered)
    val formats = GastonomoTheme.formats
    Box(modifier = modifier.fillMaxSize()) {
        LoanDetailScreen(
            state = state,
            actions = remember(state.context) {
                LoanDetailActions(
                    onBack = onBack,
                    onRegisterPayment = { payment.open(it.loan, it.summary, state.context) },
                    onRetry = detail::reload,
                )
            },
        )
        LoanPaymentOverlay(
            viewModel = payment,
            onSaved = { money ->
                toast = registered.format(formats.amount(money))
                detail.reload()
            },
        )
        toast?.let { message ->
            GastonomoToast(message = message, onHide = { toast = null })
        }
    }
}
