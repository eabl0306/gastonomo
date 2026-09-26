package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.CurrencyDecimals
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Lo que la tarjeta y el detalle de un prestamo necesitan y la API no da: saldo,
 * abonado, estado, porcentaje e intereses ganados.
 *
 * @property outstanding saldo pendiente, ya redondeado a cero cuando cabe dentro del
 *   epsilon de la moneda.
 * @property interest el excedente cuando lo abonado se pasa del monto.
 */
data class LoanSummary(
    val total: Money,
    val paid: Money,
    val outstanding: Money,
    val interest: Money,
    val status: LoanStatus,
    val percent: Int,
) {
    companion object {

        /** Porcentaje maximo que ensena la barra, aunque lo abonado se pase del monto. */
        const val MAX_PERCENT = 100

        /**
         * Calcula el resumen en [display] con las tasas de hoy.
         *
         * El epsilon es 0,005 en las monedas con decimales y 0,5 en las que no tienen, el
         * mismo del prototipo: sin el, una conversion deja un saldo de 0,0001 y un
         * prestamo pagado sale como Parcial para siempre.
         */
        fun of(loan: Loan, display: CurrencyCode, rates: ExchangeRates): LoanSummary {
            val total = DisplayedAmount.money(loan.money, display, rates)
            val paid = DisplayedAmount.sum(loan.payments.map { it.money }, display, rates)
            val epsilon = epsilonOf(display)
            val remainder = total.amount - paid.amount
            val outstanding = if (remainder < epsilon) BigDecimal.ZERO else remainder
            val surplus = paid.amount - total.amount
            val interest = if (surplus > epsilon) surplus else BigDecimal.ZERO
            return LoanSummary(
                total = total,
                paid = paid,
                outstanding = Money(outstanding, display),
                interest = Money(interest, display),
                status = statusOf(outstanding, paid.amount),
                percent = percentOf(paid.amount, total.amount),
            )
        }

        private fun epsilonOf(display: CurrencyCode): BigDecimal =
            if (CurrencyDecimals.hasNoFraction(display)) HALF_UNIT else HALF_CENT

        private fun statusOf(outstanding: BigDecimal, paid: BigDecimal): LoanStatus = when {
            outstanding.signum() == 0 -> LoanStatus.PAID
            paid.signum() > 0 -> LoanStatus.PARTIAL
            else -> LoanStatus.PENDING
        }

        private fun percentOf(paid: BigDecimal, total: BigDecimal): Int {
            if (total.signum() == 0) return MAX_PERCENT
            val ratio = paid.multiply(HUNDRED, ExchangeRates.PRECISION)
                .divide(total, ExchangeRates.PRECISION)
                .setScale(0, RoundingMode.HALF_UP)
                .toInt()
            return minOf(MAX_PERCENT, ratio)
        }

        private val HALF_CENT = BigDecimal("0.005")
        private val HALF_UNIT = BigDecimal("0.5")
        private val HUNDRED = BigDecimal(MAX_PERCENT)
    }
}
