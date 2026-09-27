package com.bahoque.gastonomo.feature.settings.pending

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.feature.settings.SettingsDependencies

/** Une el ViewModel de registros sin enviar con su pantalla. */
@Composable
internal fun PendingWritesRoute(
    dependencies: SettingsDependencies,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: PendingWritesViewModel = viewModel {
        PendingWritesViewModel(
            observe = dependencies.observePendingWrites,
            correct = dependencies.correctPendingWrite,
            discard = dependencies.discardPendingWrite,
            retry = dependencies.retryPendingWrites,
        )
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Abrir la pantalla ya intenta subir. Tocar la fila de Ajustes es, entonces, forzar un
    // intento: esperar a que el sistema decida no es una respuesta cuando lo que el usuario
    // quiere saber es si su gasto esta a salvo.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.onRetry() }
    val actions = remember(viewModel) {
        PendingWritesActions(
            onBack = onBack,
            onEdit = viewModel::onEdit,
            onAmount = viewModel::onAmount,
            onCancelEdit = viewModel::onCancelEdit,
            onSave = viewModel::onSave,
            onAskDiscard = viewModel::onAskDiscard,
            onCancelDiscard = viewModel::onCancelDiscard,
            onConfirmDiscard = viewModel::onConfirmDiscard,
            onRetry = viewModel::onRetry,
        )
    }
    PendingWritesScreen(state = state, actions = actions, modifier = modifier)
}
