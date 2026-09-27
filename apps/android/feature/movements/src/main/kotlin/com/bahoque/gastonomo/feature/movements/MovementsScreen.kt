package com.bahoque.gastonomo.feature.movements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.application.state.MessageKey
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.DateRangeSheet
import com.bahoque.gastonomo.core.designsystem.component.EmptyState
import com.bahoque.gastonomo.core.designsystem.component.FilterPill
import com.bahoque.gastonomo.core.designsystem.component.FilterPillRow
import com.bahoque.gastonomo.core.designsystem.component.GastonomoFab
import com.bahoque.gastonomo.core.designsystem.component.GastonomoLoadingBlock
import com.bahoque.gastonomo.core.designsystem.component.GastonomoNoteLine
import com.bahoque.gastonomo.core.designsystem.component.GastonomoRetryBlock
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoSheetHost
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.component.PeriodPickerRow
import com.bahoque.gastonomo.core.designsystem.format.periodLabel
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.movements.ui.MovementDayGroup
import com.bahoque.gastonomo.feature.movements.ui.PeriodSummaryCard

/**
 * Movimientos, la pantalla 6: el resumen del periodo, las tres pills y la lista agrupada por
 * dia, del dia mas reciente al mas antiguo.
 */
@Composable
fun MovementsScreen(
    state: MovementsUiState,
    actions: MovementsActions,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        GastonomoScreen {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                GastonomoText(
                    text = stringResource(R.string.movements_title),
                    modifier = Modifier.padding(top = TITLE_TOP, bottom = TITLE_BOTTOM),
                    style = GastonomoTheme.typography.screenTitle,
                    color = GastonomoTheme.colors.ink,
                )
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
                PeriodSummaryCard(state = state)
                Spacer(Modifier.height(SUMMARY_GAP))
                Pills(filter = state.filter, onFilter = actions.onFilter)
                Groups(state = state, actions = actions)
                Spacer(Modifier.height(BOTTOM_GAP))
            }
        }
        GastonomoFab(
            text = stringResource(R.string.movements_new),
            onClick = actions.onNewMovement,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = FAB_SIDE, bottom = FAB_BOTTOM),
        )
        Overlays(state = state, actions = actions)
    }
}

@Composable
private fun Pills(filter: MovementFilter, onFilter: (MovementFilter) -> Unit) {
    FilterPillRow(modifier = Modifier.fillMaxWidth()) {
        FilterPill(
            label = stringResource(GastonomoStrings.filterAll),
            selected = filter == MovementFilter.ALL,
            onClick = { onFilter(MovementFilter.ALL) },
        )
        FilterPill(
            label = stringResource(R.string.movements_income),
            selected = filter == MovementFilter.INCOME,
            onClick = { onFilter(MovementFilter.INCOME) },
        )
        FilterPill(
            label = stringResource(R.string.movements_expense),
            selected = filter == MovementFilter.EXPENSE,
            onClick = { onFilter(MovementFilter.EXPENSE) },
        )
    }
}

@Composable
private fun Groups(state: MovementsUiState, actions: MovementsActions) {
    when {
        state.error != null -> GastonomoRetryBlock(
            message = stringResource(errorTextOf(state.error)),
            onRetry = actions.onRetry,
        )
        state.loading && state.groups.isEmpty() -> GastonomoLoadingBlock()
        state.groups.isEmpty() -> EmptyState(
            icon = GastonomoIcons.RECEIPT_LONG,
            message = stringResource(R.string.movements_empty),
            modifier = Modifier.padding(vertical = EMPTY_PADDING),
        )
        else -> Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(GROUP_GAP),
        ) {
            state.groups.forEach { group ->
                MovementDayGroup(group = group, state = state)
            }
        }
    }
}

/** El sheet de rango y el toast, que van encima de la pantalla. */
@Composable
private fun Overlays(state: MovementsUiState, actions: MovementsActions) {
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

/** El texto de cada fallo. Los mensajes comunes ya estan en el sistema de diseno. */
internal fun errorTextOf(error: MovementsError): Int = when (error) {
    MovementsError.NETWORK -> GastonomoStrings.errorNetwork
    MovementsError.SESSION_EXPIRED -> GastonomoStrings.errorSessionExpired
    MovementsError.IN_PROGRESS -> GastonomoStrings.errorInProgress
    MovementsError.BODY_CHANGED -> GastonomoStrings.errorBodyChanged
    MovementsError.CONFLICT -> R.string.movements_error_conflict
    MovementsError.UNEXPECTED -> GastonomoStrings.errorUnexpected
}

/** El toast que dejo el formulario al guardar y volver atras. */
@Composable
private fun toastTextOf(key: MessageKey): String? = when (key) {
    TransientMessages.MOVEMENT_SAVED_INCOME -> stringResource(R.string.movements_toast_income_saved)
    TransientMessages.MOVEMENT_SAVED_EXPENSE -> stringResource(R.string.movements_toast_expense_saved)
    else -> null
}

private val TITLE_TOP = 4.dp
private val TITLE_BOTTOM = 16.dp
private val PICKER_GAP = 12.dp
private val SUMMARY_GAP = 14.dp
private val EMPTY_PADDING = 40.dp
private val GROUP_GAP = 10.dp
private val BOTTOM_GAP = 96.dp
private val FAB_SIDE = 20.dp

/**
 * El diseno pone el FAB a 92 del borde inferior de la pantalla, contando la tab bar por debajo.
 * Aqui la tab bar es hermana del contenido y no lo tapa, asi que la distancia es la que queda:
 * 92 menos los 78 de la barra.
 */
private val FAB_BOTTOM = 14.dp
