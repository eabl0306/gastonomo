package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LoanTotalsTest {

    @Test
    fun `only open loans add to the balance and to the count`() {
        val totals = LoanTotals.of(rows(), LoanDirection.LENT, COP)

        assertEquals(2, totals.openCount)
        assertEquals(0, BigDecimal("150000").compareTo(totals.outstanding.amount))
    }

    @Test
    fun `a direction without open loans is zero and counts nothing`() {
        val totals = LoanTotals.of(rows(), LoanDirection.BORROWED, COP)

        assertEquals(0, totals.openCount)
        assertEquals(COP, totals.outstanding.currency)
        assertEquals(0, BigDecimal.ZERO.compareTo(totals.outstanding.amount))
    }

    @Test
    fun `an empty list is zero in the display currency`() {
        val totals = LoanTotals.of(emptyList(), LoanDirection.LENT, CurrencyCode.USD)

        assertEquals(CurrencyCode.USD, totals.outstanding.currency)
        assertEquals(0, totals.openCount)
    }

    private fun rows(): List<Pair<Loan, LoanSummary>> = listOf(
        loan("1", "100000", paid = null),
        loan("2", "50000", paid = null),
        loan("3", "80000", paid = "80000"),
    ).map { it to LoanSummary.of(it, COP, ExchangeRates.EMPTY) }

    private fun loan(id: String, amount: String, paid: String?) = Loan(
        id = LoanId(id),
        person = "Persona $id",
        note = "",
        direction = LoanDirection.LENT,
        openedOn = LocalDate.of(2026, 9, 1),
        money = MonetaryAmount.single(BigDecimal(amount), COP),
        payments = paid?.let {
            listOf(
                LoanPayment(
                    id = LoanPaymentId("p$id"),
                    loanId = LoanId(id),
                    paidOn = LocalDate.of(2026, 9, 2),
                    label = null,
                    money = MonetaryAmount.single(BigDecimal(it), COP),
                ),
            )
        }.orEmpty(),
    )

    private companion object {
        val COP = CurrencyCode.COP
    }
}
