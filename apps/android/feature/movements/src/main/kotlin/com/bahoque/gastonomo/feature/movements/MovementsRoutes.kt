package com.bahoque.gastonomo.feature.movements

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/** Une el ViewModel de la lista de Movimientos con su pantalla. */
@Composable
internal fun MovementsRoute(
    dependencies: MovementsDependencies,
    onNewMovement: () -> Unit,
) {
    val viewModel: MovementsViewModel = viewModel(factory = MovementsViewModel.factory(dependencies))
    val state by viewModel.state.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        MovementsActions(
            onPreviousMonth = viewModel.period::previousMonth,
            onNextOrClearRange = viewModel.period::nextMonthOrClearRange,
            onOpenRangeSheet = viewModel.period::openRangeSheet,
            onCloseRangeSheet = viewModel.period::closeRangeSheet,
            onPreset = viewModel.period::selectPreset,
            onRangeFrom = viewModel.period::editFrom,
            onRangeTo = viewModel.period::editTo,
            onApplyRange = viewModel.period::applyRange,
            onFilter = viewModel::onFilter,
            onRetry = viewModel::onRetry,
            onToastHidden = viewModel::onToastHidden,
            onNewMovement = onNewMovement,
        )
    }
    MovementsScreen(state = state, actions = actions)
}

/**
 * Une el ViewModel del formulario con su pantalla.
 *
 * Al guardar bien el ViewModel marca `saved` y aqui se vuelve atras: el toast lo pinta la
 * pantalla de destino, que es donde lo pone el diseno.
 */
@Composable
internal fun NewMovementRoute(
    dependencies: MovementsDependencies,
    kind: MovementKind,
    onClose: () -> Unit,
) {
    val viewModel: NewMovementViewModel = viewModel(
        factory = NewMovementViewModel.factory(dependencies, kind),
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(state.saved) {
        if (state.saved) onClose()
    }
    val actions = remember(viewModel) {
        NewMovementActions(
            onKind = viewModel::onKind,
            onAmount = viewModel::onAmount,
            onBase = viewModel::onBase,
            onCategory = viewModel::onCategory,
            onDate = viewModel::onDate,
            onNote = viewModel::onNote,
            onCurrencySheet = viewModel::onCurrencySheet,
            onCurrencyQuery = viewModel::onCurrencyQuery,
            onCurrency = viewModel::onCurrency,
            onSave = viewModel::onSave,
            onClose = onClose,
        )
    }
    NewMovementScreen(state = state, actions = actions)
}
