package com.bahoque.gastonomo.feature.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.application.state.MessageKey
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.DateRangeSheet
import com.bahoque.gastonomo.core.designsystem.component.GastonomoLoadingBlock
import com.bahoque.gastonomo.core.designsystem.component.GastonomoNoteLine
import com.bahoque.gastonomo.core.designsystem.component.GastonomoRetryBlock
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.component.PeriodPickerRow
import com.bahoque.gastonomo.core.designsystem.format.periodLabel
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.home.ui.BalanceCard
import com.bahoque.gastonomo.feature.home.ui.ExpensesByCategoryCard
import com.bahoque.gastonomo.feature.home.ui.HomeHeader
import com.bahoque.gastonomo.feature.home.ui.LoanTotalCards
import com.bahoque.gastonomo.feature.home.ui.QuickActions
import com.bahoque.gastonomo.feature.home.ui.RecentMovementsCard

/**
 * Inicio, la pantalla 5 del rediseno: saludo, filtro de periodo, balance con su tendencia,
 * acciones rapidas, prestamos, gastos por categoria y ultimos movimientos.
 *
 * Solo recibe estado y acciones, asi que se puede capturar y probar sin ViewModel.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GastonomoScreen {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(TOP_GAP))
                HomeHeader(
                    today = state.today,
                    user = state.user,
                    onOpenSettings = actions.onOpenSettings,
                )
                Spacer(Modifier.height(HEADER_GAP))
                PeriodPickerRow(
                    label = GastonomoTheme.formats.periodLabel(state.selection.period),
                    isRange = state.selection.isRange,
                    canGoForward = state.selection.canGoForward(state.today),
                    onPrevious = actions.onPreviousMonth,
                    onNextOrClear = actions.onNextOrClearRange,
                    onOpenRange = actions.onOpenRangeSheet,
                )
                if (state.ratesUnavailable) {
                    GastonomoNoteLine(stringResource(GastonomoStrings.errorRatesUnavailable))
                }
                Spacer(Modifier.height(PICKER_GAP))
                when {
                    state.error != null -> GastonomoRetryBlock(
                        message = stringResource(errorTextOf(state.error)),
                        onRetry = actions.onRetry,
                    )
                    state.loading && !state.everLoaded -> GastonomoLoadingBlock()
                    else -> HomeContentBlocks(state = state, actions = actions)
                }
                Spacer(Modifier.height(BOTTOM_GAP))
            }
        }
        Overlays(state = state, actions = actions)
    }
}

/** El sheet de rango y el toast, que van encima de la pantalla. */
@Composable
private fun Overlays(state: HomeUiState, actions: HomeActions) {
    GastonomoSheetHost(state.rangeDraft) { draft ->
        DateRangeSheet(
            from = draft.from,
            to = draft.to,
            selectedPreset = draft.preset,
            onPreset = actions.onPreset,
            onFrom = actions.onRangeFrom,
            onTo = actions.onRangeTo,
            onApply = actions.onApplyRange,
            onDismiss = actions.onCloseRangeSheet,
        )
    }
    state.toast?.let { key ->
        toastTextOf(key)?.let { message ->
            GastonomoToast(message = message, onHide = actions.onToastHidden)
        }
    }
}

@Composable
private fun HomeContentBlocks(state: HomeUiState, actions: HomeActions) {
    BalanceCard(state = state, onToggleHidden = actions.onToggleHiddenAmounts)
    Spacer(Modifier.height(BLOCK_GAP))
    QuickActions(
        onNewMovement = actions.onNewMovement,
        onNewLoan = actions.onNewLoan,
        onOpenLoans = actions.onOpenLoans,
    )
    Spacer(Modifier.height(BLOCK_GAP))
    LoanTotalCards(state = state, onOpenLoans = actions.onOpenLoans)
    Spacer(Modifier.height(BLOCK_GAP))
    ExpensesByCategoryCard(state = state)
    Spacer(Modifier.height(SECTION_GAP))
    RecentMovementsCard(state = state, onSeeAll = actions.onOpenMovements)
}

/** El texto de cada fallo. Los mensajes comunes ya estan en el sistema de diseno. */
private fun errorTextOf(error: HomeError): Int = when (error) {
    HomeError.NETWORK -> GastonomoStrings.errorNetwork
    HomeError.SESSION_EXPIRED -> GastonomoStrings.errorSessionExpired
    HomeError.UNEXPECTED -> GastonomoStrings.errorUnexpected
}

/**
 * El toast que dejo un formulario al guardar y volver atras. Una clave que Inicio no conoce
 * no se pinta: sera de otra pantalla.
 */
@Composable
private fun toastTextOf(key: MessageKey): String? = when (key) {
    TransientMessages.MOVEMENT_SAVED_INCOME -> stringResource(R.string.home_toast_income_saved)
    TransientMessages.MOVEMENT_SAVED_EXPENSE -> stringResource(R.string.home_toast_expense_saved)
    else -> null
}

private val TOP_GAP = 8.dp
private val HEADER_GAP = 18.dp
private val PICKER_GAP = 14.dp
private val BLOCK_GAP = 14.dp
private val SECTION_GAP = 24.dp
private val BOTTOM_GAP = 28.dp
