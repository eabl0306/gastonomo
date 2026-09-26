package com.bahoque.gastonomo.feature.export

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bahoque.gastonomo.core.designsystem.component.GastonomoToast
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * Exportar con su ViewModel.
 *
 * El ViewModel genera los bytes y esta pantalla los escribe y los pasa a la hoja de
 * compartir: es lo unico que necesita un Context, asi que es lo unico que vive aqui.
 */
@Composable
fun ExportRoute(
    dependencies: ExportDependencies,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel = viewModel { ExportViewModel(dependencies) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val labels = exportLabels(state.displayCurrency.value)
    val actions = remember(viewModel, onBack, labels) {
        ExportActions(
            onBack = onBack,
            onFormat = viewModel::onFormatSelected,
            onToggleSet = viewModel::onSetToggled,
            onRange = viewModel::onRangeSelected,
            onExport = { viewModel.onExport(labels) },
            onRetry = viewModel::load,
        )
    }
    Box(modifier = modifier.fillMaxSize()) {
        ExportScreen(state = state, actions = actions)
        ShareWhenReady(state = state, viewModel = viewModel)
        state.message?.let { message ->
            GastonomoToast(
                message = message.text(),
                onHide = viewModel::onMessageShown,
                icon = if (message.isError) GastonomoIcons.INFO else GastonomoIcons.CHECK_CIRCLE,
                iconTint = if (message.isError) {
                    GastonomoTheme.colors.amber
                } else {
                    GastonomoTheme.colors.lime
                },
            )
        }
    }
}

/** Escribe los archivos en la cache y abre la hoja de compartir en cuanto estan listos. */
@Composable
private fun ShareWhenReady(state: ExportUiState, viewModel: ExportViewModel) {
    val context = LocalContext.current
    val title = stringResource(R.string.export_share)
    val payload = state.payload
    LaunchedEffect(payload) {
        if (payload == null) return@LaunchedEffect
        val intent = ExportSharing.intentFor(context, payload, title)
        if (intent == null) {
            viewModel.onPayloadFailed()
            return@LaunchedEffect
        }
        context.startActivity(intent)
        viewModel.onPayloadDelivered(payload.rows)
    }
}

/** El texto de cada aviso. Se resuelve aqui: el ViewModel no conoce los recursos. */
@Composable
private fun ExportMessage.text(): String = when (this) {
    is ExportMessage.Ready -> pluralStringResource(R.plurals.export_ready, rows, rows)
    ExportMessage.Failed -> stringResource(R.string.export_error)
}
