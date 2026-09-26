package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.home.HomeUiState
import com.bahoque.gastonomo.feature.home.R
import java.math.BigDecimal

/**
 * La tarjeta de balance: la unica superficie oscura del diseno.
 *
 * El ojo no oculta la tarjeta, solo sustituye cada importe por "{simbolo} ••••••", y la
 * tendencia se sigue dibujando: es una silueta, no una cifra.
 */
@Composable
internal fun BalanceCard(
    state: HomeUiState,
    onToggleHidden: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val formats = GastonomoTheme.formats
    val currency = state.displayCurrency
    val summary = state.content.summary
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        background = GastonomoTheme.colors.ink,
        border = null,
        radius = GastonomoTheme.radii.balanceCard,
        contentPadding = PaddingValues(start = SIDE, end = SIDE, top = SIDE, bottom = BOTTOM),
        contentColor = GastonomoTheme.colors.surface,
    ) {
        CardHeader(state = state, onToggleHidden = onToggleHidden)
        GastonomoAmountText(
            text = if (state.hideAmounts) {
                formats.masked(currency)
            } else {
                formats.balance(summary.net.amount, currency)
            },
            style = GastonomoTheme.typography.balance,
            color = GastonomoTheme.colors.surface,
            modifier = Modifier.padding(top = AMOUNT_GAP),
        )
        Trend(state = state)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = SUB_CARDS_GAP),
            horizontalArrangement = Arrangement.spacedBy(SUB_CARD_GAP),
        ) {
            SubCard(
                label = stringResource(R.string.home_income),
                icon = GastonomoIcons.ARROW_DOWNWARD,
                iconTint = GastonomoTheme.colors.balance.incomeArrow,
                amount = state.shownAmount(summary.income.amount),
            )
            SubCard(
                label = stringResource(R.string.home_expense),
                icon = GastonomoIcons.ARROW_UPWARD,
                iconTint = GastonomoTheme.colors.balance.expenseArrow,
                amount = state.shownAmount(summary.expense.amount),
            )
        }
    }
}

@Composable
private fun CardHeader(state: HomeUiState, onToggleHidden: () -> Unit) {
    val month = GastonomoTheme.formats.dates.monthName(state.selection.month)
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GastonomoText(
            text = if (state.selection.isRange) {
                stringResource(R.string.home_balance_of_range)
            } else {
                stringResource(R.string.home_balance_of_month, month)
            },
            modifier = Modifier.weight(1f),
            style = GastonomoTheme.typography.secondary,
            color = GastonomoTheme.colors.balance.label,
        )
        GastonomoIcon(
            name = if (state.hideAmounts) GastonomoIcons.VISIBILITY_OFF else GastonomoIcons.VISIBILITY,
            modifier = Modifier.pressable(onClick = onToggleHidden, role = Role.Button),
            tint = GastonomoTheme.colors.balance.label,
            contentDescription = stringResource(
                if (state.hideAmounts) R.string.home_show_amounts else R.string.home_hide_amounts,
            ),
        )
    }
}

@Composable
private fun Trend(state: HomeUiState) {
    val points = state.content.trend.points
    if (points.isEmpty()) return
    Column(modifier = Modifier.fillMaxWidth().padding(top = TREND_TOP, bottom = TREND_BOTTOM)) {
        BalanceTrendChart(points = points.map { it.cumulative })
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = LABEL_GAP),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TrendLabel(GastonomoTheme.formats.dates.short(points.first().date))
            TrendLabel(GastonomoTheme.formats.dates.short(points.last().date))
        }
    }
}

@Composable
private fun TrendLabel(text: String) {
    GastonomoText(
        text = text,
        style = GastonomoTheme.typography.trendLabel,
        color = GastonomoTheme.colors.balance.tertiary,
    )
}

@Composable
private fun RowScope.SubCard(label: String, icon: String, iconTint: Color, amount: String) {
    GastonomoCard(
        modifier = Modifier.weight(1f),
        background = GastonomoTheme.colors.balance.subCard,
        border = null,
        radius = GastonomoTheme.radii.subCard,
        contentPadding = PaddingValues(horizontal = SUB_CARD_SIDE, vertical = SUB_CARD_TOP),
        contentColor = GastonomoTheme.colors.surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SUB_CARD_ICON_GAP),
        ) {
            GastonomoIcon(name = icon, size = SUB_CARD_ICON, tint = iconTint)
            GastonomoText(
                text = label,
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.balance.label,
            )
        }
        GastonomoAmountText(
            text = amount,
            style = GastonomoTheme.typography.sectionTitle,
            color = GastonomoTheme.colors.surface,
            modifier = Modifier.padding(top = AMOUNT_GAP),
        )
    }
}

/** El importe tal como se ve: enmascarado cuando el ojo esta cerrado. */
@Composable
private fun HomeUiState.shownAmount(value: BigDecimal): String =
    if (hideAmounts) {
        GastonomoTheme.formats.masked(displayCurrency)
    } else {
        GastonomoTheme.formats.amount(value, displayCurrency)
    }

private val SIDE = 20.dp
private val BOTTOM = 18.dp
private val AMOUNT_GAP = 4.dp
private val TREND_TOP = 14.dp
private val TREND_BOTTOM = 4.dp
private val LABEL_GAP = 2.dp
private val SUB_CARDS_GAP = 16.dp
private val SUB_CARD_GAP = 10.dp
private val SUB_CARD_SIDE = 14.dp
private val SUB_CARD_TOP = 12.dp
private val SUB_CARD_ICON = 16.dp
private val SUB_CARD_ICON_GAP = 4.dp
