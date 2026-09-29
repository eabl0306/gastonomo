package com.bahoque.gastonomo.feature.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.domain.movement.MovementKind

/**
 * Une el ViewModel de Inicio con su pantalla. Es lo unico que el grafo de navegacion llama, y
 * lo que decide a donde va cada toque: la pantalla no conoce ninguna ruta.
 */
@Composable
internal fun HomeRoute(
    dependencies: HomeDependencies,
    onNewMovement: (MovementKind) -> Unit,
    onNewLoan: () -> Unit,
    onDictate: () -> Unit,
    onOpenLoans: () -> Unit,
    onOpenMovements: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.factory(dependencies))
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Volver de un formulario no recrea esta entrada de la pila, asi que sin esto lo que se
    // acaba de registrar no saldria.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onResumed() }
    val actions = remember(viewModel) {
        HomeActions(
            onPreviousMonth = viewModel.period::previousMonth,
            onNextOrClearRange = viewModel.period::nextMonthOrClearRange,
            onOpenRangeSheet = viewModel.period::openRangeSheet,
            onCloseRangeSheet = viewModel.period::closeRangeSheet,
            onPreset = viewModel.period::selectPreset,
            onRangeFrom = viewModel.period::editFrom,
            onRangeTo = viewModel.period::editTo,
            onApplyRange = viewModel.period::applyRange,
            onToggleHiddenAmounts = viewModel::onToggleHiddenAmounts,
            onRetry = viewModel::onRetry,
            onToastHidden = viewModel::onToastHidden,
            onNewMovement = onNewMovement,
            onNewLoan = onNewLoan,
            onDictate = onDictate,
            onOpenLoans = onOpenLoans,
            onOpenMovements = onOpenMovements,
            onOpenSettings = onOpenSettings,
        )
    }
    HomeScreen(state = state, actions = actions)
}
