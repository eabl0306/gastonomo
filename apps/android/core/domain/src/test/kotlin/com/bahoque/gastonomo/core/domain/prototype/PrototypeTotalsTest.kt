package com.bahoque.gastonomo.core.domain.prototype

import com.bahoque.gastonomo.core.domain.analysis.PeriodSummary
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanStatus
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.loan.LoanTotals
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

/**
 * Los numeros que pinta el prototipo con sus datos semilla, clavados uno a uno.
 *
 * Es la comprobacion de aceptacion del rediseno: si el dominio deja de dar estos valores,
 * la aplicacion ya no ensena lo mismo que el diseno aprobado, y ninguna otra prueba lo
 * nota porque todas las demas usan datos inventados.
 *
 * Todo se calcula en COP, la moneda principal del prototipo, y la unica fila en otra
 * moneda (140 USD con equivalencia de 450.000 COP) entra por su equivalencia guardada,
 * que es la segunda rama de `disp`. Por eso la tabla de tasas puede estar vacia: si se
 * usara una tasa de hoy, este test cambiaria de resultado cada dia.
 */
class PrototypeTotalsTest {

    private val display = PrototypeSeed.COP
    private val noRates = ExchangeRates.EMPTY

    @Test
    fun `september shows the balance, income and expense of the prototype`() {
        val summary = PeriodSummary.of(PrototypeSeed.september(), display, noRates)

        assertAmount("3350000", summary.income.amount)
        assertAmount("1732400", summary.expense.amount)
        assertAmount("1617600", summary.net.amount)
    }

    @Test
    fun `the lent card shows 1190000 across four open loans`() {
        val totals = LoanTotals.of(rows(), LoanDirection.LENT, display)

        assertAmount("1190000", totals.outstanding.amount)
        assertEquals(4, totals.openCount)
    }

    @Test
    fun `each loan of the prototype gets its own outstanding balance`() {
        val outstanding = rows().associate { (loan, summary) ->
            loan.person to summary.outstanding.amount
        }

        assertAmount("50000", outstanding.getValue("Collar Johana"))
        assertAmount("630000", outstanding.getValue("Nevera"))
        assertAmount("400000", outstanding.getValue("Hermana Evila"))
        assertAmount("110000", outstanding.getValue("Mama"))
        assertAmount("0", outstanding.getValue("Rosa"))
    }

    @Test
    fun `the overpaid loan is paid and shows the excess as earned interest`() {
        val rosa = rows().first { (loan, _) -> loan.person == "Rosa" }.second

        assertEquals(LoanStatus.PAID, rosa.status)
        // Abonado 700.000 sobre 600.000: el excedente es lo que la tarjeta ensena.
        assertAmount("100000", rosa.interest.amount)
        assertEquals(LoanSummary.MAX_PERCENT, rosa.percent)
    }

    @Test
    fun `the foreign row enters the totals by its stored equivalent`() {
        val foreign = PrototypeSeed.september().single { it.money.currency == PrototypeSeed.USD }
        val onlyForeign = PeriodSummary.of(listOf(foreign), display, noRates)

        // 140 USD se suman como los 450.000 COP que se guardaron, no con la tasa de hoy.
        assertAmount("450000", onlyForeign.income.amount)
    }

    private fun rows() = PrototypeSeed.loans.map { it to LoanSummary.of(it, display, noRates) }

    /**
     * Compara por valor y no por escala: `3.35E+6` y `3350000` son el mismo importe y
     * `BigDecimal.equals` los da por distintos.
     */
    private fun assertAmount(expected: String, actual: BigDecimal) {
        assertTrue(
            "esperaba $expected y llego ${actual.toPlainString()}",
            BigDecimal(expected).compareTo(actual) == 0,
        )
    }
}
