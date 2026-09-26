package com.bahoque.gastonomo.feature.loans.payment

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheet
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheetOrigin
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.feature.loans.moneyEntryActions

/**
 * El sheet de abono y, encima de el, el sheet de moneda. Lo montan igual las dos pantallas
 * de prestamos, asi que el cableado esta aqui una sola vez.
 *
 * Al elegir una moneda o cerrar el sheet de moneda se vuelve al de abono con todo lo escrito
 * en su sitio: el sheet de abono nunca se desmonta, solo queda debajo.
 *
 * @param onSaved se llama con el importe tal como se teclo, y solo cuando la API creo el
 *   abono: un reintento idempotente no vuelve a pintar el toast.
 */
@Composable
fun LoanPaymentOverlay(
    viewModel: LoanPaymentViewModel,
    onSaved: (Money) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.isOpen) return
    RegisterPaymentSheet(
        state = state,
        actions = PaymentActions(
            entry = moneyEntryActions(viewModel::editEntry),
            onFill = viewModel::fill,
            onDate = viewModel::onDate,
            onSave = { viewModel.save(onSaved) },
            onDismiss = viewModel::dismiss,
        ),
        modifier = modifier,
    )
    if (state.entry.currencySheetOpen) {
        CurrencySheet(
            origin = CurrencySheetOrigin.Record,
            currencies = state.context.currencies,
            selected = state.entry.currency,
            query = state.entry.currencyQuery,
            onQueryChange = { query -> viewModel.editEntry { withQuery(query) } },
            onSelect = { code -> viewModel.editEntry { withCurrency(code) } },
            onDismiss = { viewModel.editEntry { withCurrencySheet(false) } },
        )
    }
}
