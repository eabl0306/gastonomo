package com.bahoque.gastonomo.feature.loans

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.component.BadgeColors
import com.bahoque.gastonomo.core.designsystem.component.statusColorsOf
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanStatus
import com.bahoque.gastonomo.core.domain.loan.LoanSummary

/**
 * Lo que la tarjeta, el detalle y el sheet pintan de un prestamo, resuelto una sola vez.
 *
 * Las tres pantallas ensenan lo mismo con distinto tamano, asi que la decision de que
 * texto y que color toca esta aqui y no repartida por los composables. Es la traduccion
 * literal de `info()` del prototipo, con [LoanSummary] ya calculado por el dominio.
 */
@Immutable
data class LoanVisuals(
    val avatarBackground: Color,
    val avatarContent: Color,
    val highlightLabel: String,
    val highlightColor: Color,
    val barColor: Color,
    val badge: BadgeColors,
    val statusLabel: String,
    val directionLabel: String,
)

/**
 * El bloque destacado de un prestamo abierto es su saldo; el de uno pagado es el monto, o
 * los intereses cuando lo abonado se paso.
 */
@Composable
fun loanVisualsOf(direction: LoanDirection, summary: LoanSummary): LoanVisuals {
    val colors = GastonomoTheme.colors
    val lent = direction == LoanDirection.LENT
    val settled = summary.status == LoanStatus.PAID
    val withInterest = settled && !summary.interest.isZero
    return LoanVisuals(
        avatarBackground = if (lent) colors.blueBg else colors.amberBg,
        avatarContent = if (lent) colors.blue else colors.amber,
        highlightLabel = stringResource(highlightLabelOf(settled, withInterest)),
        highlightColor = when {
            withInterest -> colors.amber
            settled -> colors.green
            lent -> colors.blue
            else -> colors.amber
        },
        barColor = if (settled) colors.green else colors.ink,
        badge = statusColorsOf(summary.status),
        statusLabel = stringResource(statusLabelOf(summary.status)),
        directionLabel = stringResource(if (lent) R.string.loan_owes_me else R.string.loan_i_owe),
    )
}

/**
 * El importe que acompana a [LoanVisuals.highlightLabel]: el saldo, los intereses o el
 * monto original, en ese orden. Sale aparte porque necesita el formato de dinero.
 */
@Composable
fun highlightAmountOf(summary: LoanSummary): String {
    val formats = GastonomoTheme.formats
    val settled = summary.status == LoanStatus.PAID
    return when {
        !settled -> formats.amount(summary.outstanding)
        !summary.interest.isZero -> formats.amount(summary.interest)
        else -> formats.amount(summary.total)
    }
}

/** La fraccion de la barra de progreso, entre 0 y 1. */
fun progressOf(summary: LoanSummary): Float =
    summary.percent.toFloat() / LoanSummary.MAX_PERCENT

private fun highlightLabelOf(settled: Boolean, withInterest: Boolean): Int = when {
    withInterest -> R.string.loan_interest_earned
    settled -> R.string.loan_paid_in_full
    else -> R.string.loan_outstanding
}

private fun statusLabelOf(status: LoanStatus): Int = when (status) {
    LoanStatus.PAID -> GastonomoStrings.loanStatusPaid
    LoanStatus.PARTIAL -> GastonomoStrings.loanStatusPartial
    LoanStatus.PENDING -> GastonomoStrings.loanStatusPending
}
