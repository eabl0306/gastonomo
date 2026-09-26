package com.bahoque.gastonomo.feature.loans.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.format.loanCountLabel
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanTotals
import com.bahoque.gastonomo.feature.loans.R

/**
 * Las dos tarjetas de arriba de Prestamos: "Te deben" en ink con texto blanco y "Debes" en
 * blanco con el monto en ambar.
 *
 * No las afecta el filtro de las pills ni el periodo: son los saldos de hoy de los dos
 * lados.
 */
@Composable
fun LoanTotalsCards(
    owedToMe: LoanTotals,
    iOwe: LoanTotals,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(GastonomoTheme.dimens.cardGapTight),
    ) {
        TotalCard(
            label = stringResource(R.string.loans_owed_to_me),
            totals = owedToMe,
            background = GastonomoTheme.colors.ink,
            border = null,
            labelColor = GastonomoTheme.colors.balance.label,
            amountColor = GastonomoTheme.colors.surface,
            countColor = GastonomoTheme.colors.balance.tertiary,
        )
        TotalCard(
            label = stringResource(R.string.loans_i_owe),
            totals = iOwe,
            background = GastonomoTheme.colors.surface,
            border = GastonomoTheme.colors.line,
            labelColor = GastonomoTheme.colors.muted,
            amountColor = GastonomoTheme.colors.amber,
            countColor = GastonomoTheme.colors.muted,
        )
    }
}

@Composable
private fun RowScope.TotalCard(
    label: String,
    totals: LoanTotals,
    background: Color,
    border: Color?,
    labelColor: Color,
    amountColor: Color,
    countColor: Color,
) {
    GastonomoCard(
        modifier = Modifier.weight(1f),
        background = background,
        border = border,
        contentPadding = PaddingValues(CARD_PADDING),
    ) {
        Column {
            GastonomoText(
                text = label,
                style = GastonomoTheme.typography.secondarySmall,
                color = labelColor,
                maxLines = 1,
            )
            GastonomoAmountText(
                text = GastonomoTheme.formats.amount(totals.outstanding),
                style = GastonomoTheme.typography.sheetTitle,
                color = amountColor,
                modifier = Modifier.padding(top = AMOUNT_GAP),
            )
            GastonomoText(
                text = loanCountLabel(totals.openCount),
                style = GastonomoTheme.typography.secondarySmall,
                color = countColor,
                maxLines = 1,
                modifier = Modifier.padding(top = COUNT_GAP),
            )
        }
    }
}

private val CARD_PADDING = 16.dp
private val AMOUNT_GAP = 4.dp
private val COUNT_GAP = 2.dp
