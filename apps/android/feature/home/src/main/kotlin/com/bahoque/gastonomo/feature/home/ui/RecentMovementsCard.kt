package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.MovementRow
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.designsystem.theme.categoryColorOf
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.feature.home.HomeUiState
import com.bahoque.gastonomo.feature.home.R

/**
 * "Ultimos movimientos": los cuatro mas recientes del periodo, con "Ver todos" al lado del
 * titulo.
 *
 * El subtitulo de la fila lleva la categoria y la fecha, y ademas el importe original cuando
 * el registro se hizo en otra moneda: es el unico sitio donde se ve lo que teclo el usuario.
 */
@Composable
internal fun RecentMovementsCard(
    state: HomeUiState,
    onSeeAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = TITLE_GAP),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            GastonomoText(
                text = stringResource(R.string.home_recent_movements),
                style = GastonomoTheme.typography.sectionTitle,
                color = GastonomoTheme.colors.ink,
            )
            GastonomoText(
                text = stringResource(GastonomoStrings.actionSeeAll),
                modifier = Modifier.pressable(onClick = onSeeAll, role = Role.Button),
                style = GastonomoTheme.typography.labelStrong,
                color = GastonomoTheme.colors.muted,
            )
        }
        GastonomoCard(
            modifier = Modifier.fillMaxWidth(),
            radius = GastonomoTheme.radii.loanCard,
            contentPadding = PaddingValues(vertical = CARD_PADDING),
        ) {
            if (state.content.recent.isEmpty()) {
                GastonomoText(
                    text = stringResource(R.string.home_no_movements),
                    modifier = Modifier.padding(horizontal = ROW_PADDING, vertical = EMPTY_PADDING),
                    style = GastonomoTheme.typography.bodySmall,
                    color = GastonomoTheme.colors.muted,
                )
            }
            state.content.recent.forEachIndexed { index, movement ->
                RecentRow(
                    movement = movement,
                    state = state,
                    showDivider = index != state.content.recent.lastIndex,
                )
            }
        }
    }
}

@Composable
private fun RecentRow(movement: Movement, state: HomeUiState, showDivider: Boolean) {
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

/** "{categoria} · {fecha}" y, si el registro esta en otra moneda, su importe original. */
@Composable
private fun subtitleOf(movement: Movement, state: HomeUiState): String {
    val formats = GastonomoTheme.formats
    val head = movement.category.name + SEPARATOR + formats.dates.short(movement.occurredOn)
    if (!movement.money.isForeignTo(state.displayCurrency)) return head
    return head + SEPARATOR + formats.amount(movement.money.original)
}

private const val SEPARATOR = " · "

private val TITLE_GAP = 8.dp
private val CARD_PADDING = 4.dp
private val ROW_PADDING = 16.dp
private val EMPTY_PADDING = 12.dp
