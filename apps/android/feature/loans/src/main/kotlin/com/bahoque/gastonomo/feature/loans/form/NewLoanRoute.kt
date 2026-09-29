package com.bahoque.gastonomo.feature.loans.form

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheet
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheetOrigin
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.feature.loans.LoansDependencies
import com.bahoque.gastonomo.feature.loans.moneyContextLoader
import com.bahoque.gastonomo.feature.loans.moneyEntryActions

/**
 * Pantalla 10 montada. El sheet de moneda se abre encima del formulario y al elegir o cerrar
 * se vuelve a el sin perder nada de lo escrito.
 *
 * @param onCreated se llama al crear el prestamo. Quien hospeda vuelve atras y pinta el
 *   toast; [replay] a true significa que era un reintento y el exito ya se pinto.
 */
@Composable
fun NewLoanRoute(
    dependencies: LoansDependencies,
    onCreated: (replay: Boolean) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    prefill: LoanPrefill = LoanPrefill.NONE,
) {
    val form: NewLoanViewModel = viewModel {
        NewLoanViewModel(
            createLoan = dependencies.createLoan,
            contextLoader = dependencies.moneyContextLoader(),
            clock = dependencies.clock,
            prefill = prefill,
        )
    }
    val state by form.state.collectAsStateWithLifecycle()
    Box(modifier = modifier.fillMaxSize()) {
        NewLoanScreen(
            state = state,
            actions = remember(form) {
                NewLoanActions(
                    entry = moneyEntryActions(form::editEntry),
                    onDirection = form::onDirection,
                    onPerson = form::onPerson,
                    onNote = form::onNote,
                    onDate = form::onDate,
                    onSave = { form.save(onCreated) },
                    onClose = onClose,
                )
            },
        )
        GastonomoSheetHost(visible = state.entry.currencySheetOpen) {
            CurrencySheet(
                origin = CurrencySheetOrigin.Record,
                currencies = state.context.currencies,
                selected = state.entry.currency,
                query = state.entry.currencyQuery,
                onQueryChange = { query -> form.editEntry { withQuery(query) } },
                onSelect = { code -> form.editEntry { withCurrency(code) } },
                onDismiss = { form.editEntry { withCurrencySheet(false) } },
            )
        }
    }
}
