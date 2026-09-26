package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.Money

/**
 * Las dos tarjetas de arriba de Prestamos, "Te deben" y "Debes": el saldo pendiente total
 * de una direccion y cuantos prestamos siguen abiertos.
 *
 * Solo cuentan los abiertos. Un prestamo pagado no aporta saldo ni suma al conteo, asi que
 * la tarjeta dice "Nada pendiente" aunque haya prestamos en la lista.
 *
 * No depende del periodo: "Me deben" y "Debo" son siempre saldos de hoy.
 */
data class LoanTotals(val outstanding: Money, val openCount: Int) {
    companion object {

        /** Ninguno abierto. Es lo que ensenan las tarjetas mientras la lista carga. */
        fun none(display: CurrencyCode): LoanTotals = LoanTotals(Money.zero(display), 0)

        /** Suma los abiertos de [direction] en la moneda principal [display]. */
        fun of(
            rows: List<Pair<Loan, LoanSummary>>,
            direction: LoanDirection,
            display: CurrencyCode,
        ): LoanTotals {
            val open = rows.filter { (loan, summary) ->
                loan.direction == direction && summary.status.isOpen
            }
            val total = open.fold(Money.zero(display)) { sum, (_, summary) ->
                sum + summary.outstanding
            }
            return LoanTotals(outstanding = total, openCount = open.size)
        }
    }
}
