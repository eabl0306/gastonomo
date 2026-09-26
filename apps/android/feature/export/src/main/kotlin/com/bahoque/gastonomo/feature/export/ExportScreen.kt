package com.bahoque.gastonomo.feature.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAppBar
import com.bahoque.gastonomo.core.designsystem.component.GastonomoEmptyStateRetry
import com.bahoque.gastonomo.core.designsystem.component.GastonomoScreen
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.PrimaryButton
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/**
 * La pantalla 14: el formato, que conjuntos se incluyen, el periodo de los movimientos, el
 * resumen del archivo y el boton de exportar.
 *
 * El boton queda deshabilitado sin ningun conjunto marcado, y mientras se cargan los datos:
 * exportar un historial a medio leer daria un archivo incompleto sin avisar.
 */
@Composable
fun ExportScreen(
    state: ExportUiState,
    actions: ExportActions,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        GastonomoAppBar(
            title = stringResource(R.string.export_title),
            onNavigate = actions.onBack,
        )
        GastonomoScreen(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
            ) {
                SectionTitle(stringResource(R.string.export_format))
                FormatCards(selected = state.format, onSelect = actions.onFormat)
                SectionTitle(stringResource(R.string.export_include))
                SetRows(state = state, onToggle = actions.onToggleSet)
                SectionTitle(stringResource(R.string.export_period))
                RangePills(state = state, onSelect = actions.onRange)
                SummaryBox(state = state, modifier = Modifier.padding(top = BOX_GAP))
                if (state.loadFailed) {
                    GastonomoEmptyStateRetry(
                        icon = GastonomoIcons.DESCRIPTION,
                        message = stringResource(R.string.export_error_load),
                        onRetry = actions.onRetry,
                    )
                }
            }
        }
        PrimaryButton(
            text = stringResource(state.format.buttonRes()),
            onClick = actions.onExport,
            enabled = state.canExport,
            icon = GastonomoIcons.DOWNLOAD,
            modifier = Modifier.padding(
                start = GastonomoTheme.dimens.screenPadding,
                end = GastonomoTheme.dimens.screenPadding,
                top = BUTTON_TOP,
                bottom = BUTTON_BOTTOM,
            ),
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    GastonomoText(
        text = text,
        style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
        color = GastonomoTheme.colors.ink,
        modifier = Modifier.padding(top = TITLE_TOP),
    )
}

private val ITEM_GAP = 10.dp
private val TITLE_TOP = 12.dp
private val BOX_GAP = 12.dp
private val BUTTON_TOP = 12.dp
private val BUTTON_BOTTOM = 24.dp
