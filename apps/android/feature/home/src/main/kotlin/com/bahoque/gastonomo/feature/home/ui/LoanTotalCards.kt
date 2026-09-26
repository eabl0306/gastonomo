package com.bahoque.gastonomo.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
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
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.pressable
import com.bahoque.gastonomo.core.designsystem.format.loanCountLabel
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.feature.home.HomeUiState
import com.bahoque.gastonomo.feature.home.LoanTotal

/**
 * "Me deben" y "Debo": el saldo pendiente total y el conteo de prestamos abiertos.
 *
 * No dependen del periodo, a diferencia de todo lo que hay encima en la pantalla: son saldos
 * actuales. Las dos llevan a Prestamos.
 */
@Composable
internal fun LoanTotalCards(
    state: HomeUiState,
    onOpenLoans: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GAP),
    ) {
        LoanTotalCard(
            label = stringResource(GastonomoStrings.directionOwedToMe),
            total = state.content.owedToMe,
            amountColor = GastonomoTheme.colors.blue,
            state = state,
            onClick = onOpenLoans,
        )
        LoanTotalCard(
            label = stringResource(GastonomoStrings.directionIOwe),
            total = state.content.iOwe,
            amountColor = GastonomoTheme.colors.amber,
            state = state,
            onClick = onOpenLoans,
        )
    }
}

@Composable
private fun RowScope.LoanTotalCard(
    label: String,
    total: LoanTotal,
    amountColor: Color,
    state: HomeUiState,
    onClick: () -> Unit,
) {
    val formats = GastonomoTheme.formats
    GastonomoCard(
        modifier = Modifier
            .weight(1f)
            .pressable(onClick = onClick, role = Role.Button),
        radius = GastonomoTheme.radii.smallCard,
        contentPadding = PaddingValues(CONTENT_PADDING),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GastonomoText(
                text = label,
                modifier = Modifier.weight(1f),
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.muted,
            )
            GastonomoIcon(
                name = GastonomoIcons.ARROW_OUTWARD,
                size = ICON_SIZE,
                tint = GastonomoTheme.colors.ink,
            )
        }
        GastonomoAmountText(
            text = if (state.hideAmounts) {
                formats.masked(state.displayCurrency)
            } else {
                formats.amount(total.outstanding, state.displayCurrency)
            },
            style = GastonomoTheme.typography.loanTotal,
            color = amountColor,
            modifier = Modifier.padding(top = AMOUNT_GAP),
        )
        GastonomoText(
            text = loanCountLabel(total.count),
            modifier = Modifier.padding(top = COUNT_GAP),
            style = GastonomoTheme.typography.secondarySmall,
            color = GastonomoTheme.colors.muted,
            maxLines = 1,
        )
    }
}

private val GAP = 10.dp
private val CONTENT_PADDING = 14.dp
private val ICON_SIZE = 16.dp
private val AMOUNT_GAP = 6.dp
private val COUNT_GAP = 2.dp
