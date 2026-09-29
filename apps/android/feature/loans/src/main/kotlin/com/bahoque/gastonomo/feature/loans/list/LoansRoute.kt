package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bahoque.gastonomo.feature.loans.LoanCreatedSignal
import com.bahoque.gastonomo.feature.loans.LoansDependencies
import com.bahoque.gastonomo.feature.loans.R
import com.bahoque.gastonomo.feature.loans.moneyContextLoader
import com.bahoque.gastonomo.feature.loans.payment.LoanPaymentOverlay
import com.bahoque.gastonomo.feature.loans.payment.LoanPaymentViewModel

/**
 * Pantalla 8 montada: la lista, el sheet de abono que abre una tarjeta y el toast que
 * confirma el abono.
 *
 * Registrar un abono no navega y cambia el saldo, asi que al guardar se vuelve a pedir la
 * lista: los totales de arriba y el estado de la tarjeta dependen de el.
 */
@Composable
fun LoansRoute(
    dependencies: LoansDependencies,
    createdSignal: LoanCreatedSignal,
    onOpenLoan: (LoanId) -> Unit,
    onNewLoan: () -> Unit,
    onNewMovement: () -> Unit,
    onDictate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val loans: LoansViewModel = viewModel {
        LoansViewModel(dependencies.loadLoans, dependencies.moneyContextLoader())
    }
    val payment: LoanPaymentViewModel = viewModel {
        LoanPaymentViewModel(dependencies.registerLoanPayment, dependencies.clock)
    }
    val state by loans.state.collectAsStateWithLifecycle()
    var toast by rememberSaveable { mutableStateOf<String?>(null) }
    val registered = stringResource(R.string.payment_registered)
    val loanCreated = stringResource(R.string.new_loan_created)
    val formats = GastonomoTheme.formats
    val ticks by createdSignal.ticks.collectAsStateWithLifecycle()
    LaunchedEffect(ticks) {
        if (ticks > 0) {
            toast = loanCreated
            loans.reload()
        }
    }
    Box(modifier = modifier.fillMaxSize()) {
        LoansScreen(
            state = state,
            actions = remember(state.context) {
                LoansActions(
                    onFilter = loans::onFilter,
                    onOpenLoan = { onOpenLoan(it.loan.id) },
                    onRegisterPayment = { payment.open(it.loan, it.summary, state.context) },
                    onNewLoan = onNewLoan,
                    onNewMovement = onNewMovement,
                    onDictate = onDictate,
                    onRetry = loans::reload,
                )
            },
        )
        LoanPaymentOverlay(
            viewModel = payment,
            onSaved = { money ->
                toast = registered.format(formats.amount(money))
                loans.reload()
            },
        )
        toast?.let { message ->
            GastonomoToast(message = message, onHide = { toast = null })
        }
    }
}
