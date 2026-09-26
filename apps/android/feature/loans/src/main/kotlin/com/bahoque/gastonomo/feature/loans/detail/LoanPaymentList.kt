package com.bahoque.gastonomo.feature.loans.detail

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bahoque.gastonomo.core.designsystem.component.GastonomoAmountText
import com.bahoque.gastonomo.core.designsystem.component.GastonomoCard
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.designsystem.component.IconTile
import com.bahoque.gastonomo.core.designsystem.component.RowDivider
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcon
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.feature.loans.R

/**
 * El historial de abonos, del mas reciente al mas antiguo. El orden lo da el caso de uso.
 *
 * Cada abono lleva su fecha y, si se registro en otra moneda, su monto original detras.
 */
@Composable
fun LoanPaymentList(
    payments: List<LoanPayment>,
    display: CurrencyCode,
    rates: ExchangeRates,
    modifier: Modifier = Modifier,
) {
    GastonomoCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = CARD_PADDING_X, vertical = CARD_PADDING_Y),
    ) {
        if (payments.isEmpty()) {
            GastonomoText(
                text = stringResource(R.string.loan_detail_payments_empty),
                style = GastonomoTheme.typography.bodySmall,
                color = GastonomoTheme.colors.muted,
                modifier = Modifier.padding(vertical = EMPTY_PADDING),
            )
            return@GastonomoCard
        }
        payments.forEachIndexed { index, payment ->
            PaymentRow(payment = payment, display = display, rates = rates)
            if (index != payments.lastIndex) {
                RowDivider(horizontalPadding = 0.dp)
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: LoanPayment, display: CurrencyCode, rates: ExchangeRates) {
    val formats = GastonomoTheme.formats
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = ROW_PADDING),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(ROW_GAP),
    ) {
        IconTile(
            background = GastonomoTheme.colors.greenBg,
            size = TILE_SIZE,
            radius = TILE_SIZE / 2,
        ) {
            GastonomoIcon(
                GastonomoIcons.CHECK,
                size = ICON_SIZE,
                tint = GastonomoTheme.colors.green,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            GastonomoText(
                text = labelOf(payment),
                style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = GastonomoTheme.colors.ink,
                maxLines = 1,
            )
            GastonomoText(
                text = dateOf(payment, display),
                style = GastonomoTheme.typography.secondarySmall,
                color = GastonomoTheme.colors.muted,
                maxLines = 1,
                modifier = Modifier.padding(top = LABEL_GAP),
            )
        }
        GastonomoAmountText(
            text = formats.amount(DisplayedAmount.money(payment.money, display, rates)),
            style = GastonomoTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
            color = GastonomoTheme.colors.ink,
        )
    }
}

/** "Abono" o "Abono - {concepto}". */
@Composable
private fun labelOf(payment: LoanPayment): String {
    val label = payment.label?.takeIf { it.isNotBlank() } ?: return stringResource(R.string.loan_detail_payment)
    return stringResource(R.string.loan_detail_payment_labeled, label)
}

/** La fecha, con el monto original detras si el abono se registro en otra moneda. */
@Composable
private fun dateOf(payment: LoanPayment, display: CurrencyCode): String {
    val formats = GastonomoTheme.formats
    val date = formats.dates.long(payment.paidOn)
    if (!payment.money.isForeignTo(display)) return date
    return date + stringResource(R.string.loan_separator) + formats.amount(payment.money.original)
}

private val CARD_PADDING_X = 16.dp
private val CARD_PADDING_Y = 4.dp
private val EMPTY_PADDING = 16.dp
private val ROW_PADDING = 12.dp
private val ROW_GAP = 12.dp
private val TILE_SIZE = 32.dp
private val ICON_SIZE = 18.dp
private val LABEL_GAP = 2.dp
