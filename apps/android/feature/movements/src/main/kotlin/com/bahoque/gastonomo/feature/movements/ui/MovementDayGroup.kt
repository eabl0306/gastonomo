package com.bahoque.gastonomo.feature.movements.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.MovementRow
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.analysis.DayGroup
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.feature.movements.MovementsUiState

/**
 * Un grupo de dia de la lista: el encabezado con "Hoy", "Ayer" o el dia de la semana y el neto
 * del dia a la derecha, y debajo una tarjeta blanca con sus movimientos.
 */
@Composable
internal fun MovementDayGroup(
    group: DayGroup,
    state: MovementsUiState,
    modifier: Modifier = Modifier,
) {
    val formats = GastonomoTheme.formats
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = HEADER_SIDE, vertical = HEADER_VERTICAL),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GastonomoText(
                text = formats.dates.dayGroup(
                    date = group.date,
                    today = state.today,
                    todayLabel = stringResource(GastonomoStrings.dayToday),
                    yesterdayLabel = stringResource(GastonomoStrings.dayYesterday),
                ),
                style = GastonomoTheme.typography.labelStrong,
                color = GastonomoTheme.colors.muted,
            )
            GastonomoAmountText(
                text = formats.net(group.net.amount, state.displayCurrency),
                style = GastonomoTheme.typography.labelStrong,
                color = GastonomoTheme.colors.muted,
            )
        }
        GastonomoCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GastonomoTheme.radii.card,
            contentPadding = PaddingValues(),
        ) {
            group.movements.forEachIndexed { index, movement ->
                GroupRow(
                    movement = movement,
                    state = state,
                    showDivider = index != group.movements.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun GroupRow(movement: Movement, state: MovementsUiState, showDivider: Boolean) {
    val formats = GastonomoTheme.formats
    val shown = DisplayedAmount.of(movement.money, state.displayCurrency, state.rates)
    MovementRow(
        title = movement.title,
        subtitle = subtitleOf(movement, state),
        categoryIcon = movement.category.icon,
        categoryColor = categoryColorOf(movement.category.color),
        amount = formats.signed(shown, movement.kind.isIncome, state.displayCurrency),
        amountColor = if (movement.kind.isIncome) {
            GastonomoTheme.colors.green
        } else {
            GastonomoTheme.colors.ink
        },
        showDivider = showDivider,
        pending = movement.pending,
    )
}

/** La categoria y, si el registro se hizo en otra moneda, su importe original. */
@Composable
private fun subtitleOf(movement: Movement, state: MovementsUiState): String {
    if (!movement.money.isForeignTo(state.displayCurrency)) return movement.category.name
    return movement.category.name + SEPARATOR + GastonomoTheme.formats.amount(movement.money.original)
}

private const val SEPARATOR = " · "

private val HEADER_SIDE = 4.dp
private val HEADER_VERTICAL = 8.dp
