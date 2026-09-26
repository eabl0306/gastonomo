package com.bahoque.gastonomo.feature.loans.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.AppBarNavigation
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.loans.LoanProblemState
import com.bahoque.gastonomo.feature.loans.LoanRow
import com.bahoque.gastonomo.feature.loans.R

/**
 * Pantalla 9. Detalle del prestamo.
 *
 * "Registrar abono" solo sale si queda saldo, y abre el sheet encima sin navegar.
 */
@Composable
fun LoanDetailScreen(
    state: LoanDetailUiState,
    actions: LoanDetailActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        GastonomoAppBar(
            title = stringResource(R.string.loan_detail_title),
            onNavigate = actions.onBack,
            navigation = AppBarNavigation.Back,
        )
        GastonomoScreen {
            state.problem?.let { LoanProblemState(problem = it, onRetry = actions.onRetry) }
            state.row?.let { row -> Content(row = row, state = state, actions = actions) }
        }
    }
}

@Composable
private fun Content(row: LoanRow, state: LoanDetailUiState, actions: LoanDetailActions) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = TOP_PADDING, bottom = BOTTOM_PADDING),
    ) {
        LoanSummaryCard(row = row)
        if (row.summary.status.isOpen) {
            PrimaryButton(
                text = stringResource(R.string.loan_register_payment),
                onClick = { actions.onRegisterPayment(row) },
                icon = GastonomoIcons.ADD_CARD,
                modifier = Modifier.padding(top = GastonomoTheme.dimens.cardGapLoose),
            )
        }
        GastonomoText(
            text = stringResource(R.string.loan_detail_payments),
            style = GastonomoTheme.typography.sectionTitle,
            color = GastonomoTheme.colors.ink,
            modifier = Modifier.padding(top = SECTION_TOP, bottom = SECTION_BOTTOM),
        )
        LoanPaymentList(
            payments = row.loan.payments,
            display = state.context.display,
            rates = state.context.rates,
        )
    }
}

private val TOP_PADDING = 4.dp
private val BOTTOM_PADDING = 24.dp
private val SECTION_TOP = 26.dp
private val SECTION_BOTTOM = 10.dp
