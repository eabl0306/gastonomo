package com.bahoque.gastonomo.feature.movements.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.movements.MovementsUiState
import com.bahoque.gastonomo.feature.movements.R

/**
 * El resumen de tres columnas: Ingresos en verde, Gastos en rojo y el Neto en ink.
 *
 * Se calcula sobre el periodo **sin filtrar**: las pills de abajo no lo tocan, asi que cambiar
 * de pill no cambia estas tres cifras.
 */
@Composable
internal fun PeriodSummaryCard(
    state: MovementsUiState,
    modifier: Modifier = Modifier,
) {
    val formats = GastonomoTheme.formats
    val currency = state.displayCurrency
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        radius = GastonomoTheme.radii.smallCard,
        contentPadding = PaddingValues(horizontal = SIDE_PADDING, vertical = VERTICAL_PADDING),
    ) {
        // IntrinsicSize.Min da a la fila el alto de su columna mas alta, que es lo que los dos
        // divisores necesitan para poder estirarse: sin el se quedan sin alto.
        Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            SummaryColumn(
                label = stringResource(R.string.movements_income),
                amount = formats.amount(state.summary.income.amount, currency),
                color = GastonomoTheme.colors.green,
            )
            ColumnDivider()
            SummaryColumn(
                label = stringResource(R.string.movements_expense),
                amount = formats.amount(state.summary.expense.amount, currency),
                color = GastonomoTheme.colors.red,
            )
            ColumnDivider()
            SummaryColumn(
                label = stringResource(R.string.movements_net),
                amount = formats.net(state.summary.net.amount, currency),
                color = GastonomoTheme.colors.ink,
            )
        }
    }
}

@Composable
private fun RowScope.SummaryColumn(label: String, amount: String, color: Color) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        GastonomoText(
            text = label,
            style = GastonomoTheme.typography.trendLabel,
            color = GastonomoTheme.colors.muted,
        )
        GastonomoAmountText(
            text = amount,
            modifier = Modifier.padding(top = AMOUNT_GAP),
            style = GastonomoTheme.typography.sliceAmount,
            color = color,
        )
    }
}

@Composable
private fun ColumnDivider() {
    Box(
        modifier = Modifier
            .width(GastonomoTheme.dimens.divider)
            .fillMaxHeight()
            .background(GastonomoTheme.colors.divider),
    )
}

private val SIDE_PADDING = 4.dp
private val VERTICAL_PADDING = 12.dp
private val AMOUNT_GAP = 3.dp
