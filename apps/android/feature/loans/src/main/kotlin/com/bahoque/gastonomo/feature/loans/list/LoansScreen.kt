package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.EmptyState
import com.bahoque.gastonomo.core.designsystem.component.FilterPill
import com.bahoque.gastonomo.core.designsystem.component.FilterPillRow
import com.bahoque.gastonomo.core.designsystem.component.GastonomoFab
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.feature.loans.LoanProblemState
import com.bahoque.gastonomo.feature.loans.R

/**
 * Pantalla 8. Prestamos.
 *
 * Los abiertos van primero y los pagados al final: ese orden lo da el dominio
 * ([com.bahoque.gastonomo.core.domain.loan.LoanOrder]) y aqui no se vuelve a ordenar.
 */
@Composable
fun LoansScreen(
    state: LoansUiState,
    actions: LoansActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GastonomoScreen {
            GastonomoText(
                text = stringResource(R.string.loans_title),
                style = GastonomoTheme.typography.screenTitle,
                color = GastonomoTheme.colors.ink,
                modifier = Modifier.padding(top = TITLE_TOP, bottom = TITLE_BOTTOM),
            )
            LoanTotalsCards(owedToMe = state.owedToMe, iOwe = state.iOwe)
            Filters(
                selected = state.filter,
                onFilter = actions.onFilter,
                modifier = Modifier.padding(top = GastonomoTheme.dimens.cardGapLoose),
            )
            LoanList(state = state, actions = actions)
        }
        GastonomoFab(
            text = stringResource(R.string.loans_fab),
            onClick = actions.onNewLoan,
            icon = GastonomoIcons.ADD,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = GastonomoTheme.dimens.screenPadding, bottom = FAB_BOTTOM),
        )
    }
}

@Composable
private fun Filters(
    selected: LoanDirection?,
    onFilter: (LoanDirection?) -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterPillRow(modifier = modifier) {
        FilterPill(
            label = stringResource(GastonomoStrings.filterAll),
            selected = selected == null,
            onClick = { onFilter(null) },
        )
        FilterPill(
            label = stringResource(GastonomoStrings.directionOwedToMe),
            selected = selected == LoanDirection.LENT,
            onClick = { onFilter(LoanDirection.LENT) },
        )
        FilterPill(
            label = stringResource(GastonomoStrings.directionIOwe),
            selected = selected == LoanDirection.BORROWED,
            onClick = { onFilter(LoanDirection.BORROWED) },
        )
    }
}

@Composable
private fun LoanList(state: LoansUiState, actions: LoansActions) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            top = GastonomoTheme.dimens.cardGapLoose,
            bottom = LIST_BOTTOM,
        ),
        verticalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGapTight),
    ) {
        items(state.visible, key = { it.loan.id.value }) { row ->
            LoanCard(
                row = row,
                onOpen = { actions.onOpenLoan(row) },
                onRegisterPayment = { actions.onRegisterPayment(row) },
            )
        }
        if (state.problem != null) {
            item { LoanProblemState(problem = state.problem, onRetry = actions.onRetry) }
        }
        if (state.isEmpty) {
            item {
                EmptyState(
                    icon = GastonomoIcons.HANDSHAKE,
                    message = stringResource(R.string.loans_empty),
                )
            }
        }
    }
}

/** El hueco que deja el FAB por encima de la tab bar, que la pinta `:app`. */
private val FAB_BOTTOM = 14.dp
private val TITLE_TOP = 12.dp
private val TITLE_BOTTOM = 16.dp
private val LIST_BOTTOM = 96.dp
