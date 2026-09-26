package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.CurrencyDecimals
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.Movement
import java.math.BigDecimal
import java.math.RoundingMode

/** Una celda de la exportacion: texto o numero. En Excel el numero va como numero. */
sealed interface ExportCell {
    data class Text(val value: String) : ExportCell
    data class Number(val value: BigDecimal) : ExportCell
}

/** Una hoja o un archivo: su nombre, sus encabezados y sus filas. */
data class ExportTable(
    val name: String,
    val headers: List<String>,
    val rows: List<List<ExportCell>>,
)

/**
 * Construye las tres tablas del diseno. Es logica pura: no toca red, ni ficheros, ni
 * recursos, y por eso se puede probar fila a fila.
 *
 * Los importes salen en dos columnas, como todo en la aplicacion: el original tal como se
 * tecleo y su valor en la moneda principal de ahora mismo, que sale de [DisplayedAmount].
 * **Los gastos van en negativo**, igual que en el prototipo.
 */
class ExportTables(
    private val labels: ExportLabels,
    private val displayCurrency: CurrencyCode,
    private val rates: ExchangeRates,
) {

    /** La tabla de un conjunto, con los datos que se le pasan. */
    fun of(set: ExportSet, state: ExportUiState): ExportTable = when (set) {
        ExportSet.MOVEMENTS -> movements(state.movementsInRange)
        ExportSet.LOANS -> loans(state.loans)
        ExportSet.PAYMENTS -> payments(state.payments)
    }

    private fun movements(movements: List<Movement>): ExportTable = ExportTable(
        name = labels.movements.name,
        headers = labels.movements.headers,
        rows = movements.sortedBy { it.occurredOn }.map { movement ->
            val sign = if (movement.kind.isIncome) BigDecimal.ONE else MINUS_ONE
            listOf(
                ExportCell.Text(movement.occurredOn.toString()),
                ExportCell.Text(labels.kindOf(movement.kind)),
                ExportCell.Text(movement.category.name),
                ExportCell.Text(movement.title),
                ExportCell.Number(movement.money.amount * sign),
                ExportCell.Text(movement.money.currency.value),
                ExportCell.Number(shown(movement.money) * sign),
            )
        },
    )

    private fun loans(loans: List<Loan>): ExportTable = ExportTable(
        name = labels.loans.name,
        headers = labels.loans.headers,
        rows = loans.map { loan ->
            val summary = LoanSummary.of(loan, displayCurrency, rates)
            listOf(
                ExportCell.Text(loan.openedOn.toString()),
                ExportCell.Text(loan.person),
                ExportCell.Text(labels.directionOf(loan.direction)),
                ExportCell.Text(loan.note),
                ExportCell.Number(loan.money.amount),
                ExportCell.Text(loan.money.currency.value),
                ExportCell.Number(shown(loan.money)),
                ExportCell.Number(rounded(summary.paid.amount)),
                ExportCell.Number(rounded(summary.outstanding.amount)),
                ExportCell.Text(labels.statusOf(summary.status)),
            )
        },
    )

    private fun payments(payments: List<Pair<Loan, LoanPayment>>): ExportTable = ExportTable(
        name = labels.payments.name,
        headers = labels.payments.headers,
        rows = payments.sortedBy { (_, payment) -> payment.paidOn }.map { (loan, payment) ->
            listOf(
                ExportCell.Text(payment.paidOn.toString()),
                ExportCell.Text(loan.person),
                ExportCell.Text(payment.label?.takeIf { it.isNotBlank() } ?: labels.payment),
                ExportCell.Number(payment.money.amount),
                ExportCell.Text(payment.money.currency.value),
                ExportCell.Number(shown(payment.money)),
            )
        },
    )

    private fun shown(money: MonetaryAmount): BigDecimal =
        rounded(DisplayedAmount.of(money, displayCurrency, rates))

    /** Se redondea a los decimales de la moneda principal, como hace `rd` del prototipo. */
    private fun rounded(value: BigDecimal): BigDecimal =
        value.setScale(CurrencyDecimals.of(displayCurrency), RoundingMode.HALF_UP)

    private companion object {
        val MINUS_ONE: BigDecimal = BigDecimal(-1)
    }
}
