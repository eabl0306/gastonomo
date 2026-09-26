package com.bahoque.gastonomo.core.domain.loan

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoanSummaryTest {

    @Test
    fun `a loan without payments is pending`() {
        val summary = summaryOf(loan(amount = "150000"))
        assertEquals(LoanStatus.PENDING, summary.status)
        assertEquals(BigDecimal("150000"), summary.outstanding.amount)
        assertEquals(0, summary.percent)
        assertTrue(summary.interest.isZero)
    }

    @Test
    fun `a loan with some payments is partial`() {
        val summary = summaryOf(loan(amount = "150000", payments = listOf("100000")))
        assertEquals(LoanStatus.PARTIAL, summary.status)
        assertEquals(BigDecimal("50000"), summary.outstanding.amount)
        assertEquals(67, summary.percent)
    }

    @Test
    fun `a loan paid to the cent is paid`() {
        val summary = summaryOf(loan(amount = "150000", payments = listOf("100000", "50000")))
        assertEquals(LoanStatus.PAID, summary.status)
        assertTrue(summary.outstanding.isZero)
        assertEquals(100, summary.percent)
    }

    @Test
    fun `what exceeds the loan is interest earned`() {
        val summary = summaryOf(loan(amount = "600000", payments = listOf("600000", "100000")))
        assertEquals(LoanStatus.PAID, summary.status)
        assertEquals(BigDecimal("100000"), summary.interest.amount)
        assertEquals(100, summary.percent)
    }

    @Test
    fun `a remainder smaller than half a unit is rounded to zero in a currency without decimals`() {
        val summary = summaryOf(loan(amount = "150000", payments = listOf("149999.6")))
        assertEquals(LoanStatus.PAID, summary.status)
        assertTrue(summary.outstanding.isZero)
    }

    @Test
    fun `a remainder of a tenth of a cent is rounded to zero in a currency with decimals`() {
        val summary = summaryOf(
            loan(amount = "100", payments = listOf("99.999"), currency = CurrencyCode.USD),
            display = CurrencyCode.USD,
        )
        assertEquals(LoanStatus.PAID, summary.status)
    }

    @Test
    fun `a loan of zero is reported as fully paid instead of dividing by zero`() {
        val summary = summaryOf(loan(amount = "0"))
        assertEquals(100, summary.percent)
    }

    @Test
    fun `the paid status is not open and the others are`() {
        assertFalse(LoanStatus.PAID.isOpen)
        assertTrue(LoanStatus.PARTIAL.isOpen)
        assertTrue(LoanStatus.PENDING.isOpen)
    }

    @Test
    fun `the open loans go first and the paid ones last`() {
        val open = loan(id = "open", amount = "100", openedOn = "2026-01-01")
        val paid = loan(id = "paid", amount = "100", payments = listOf("100"), openedOn = "2026-09-01")
        val ordered = LoanOrder.sorted(listOf(paid, open).map { it to summaryOf(it) })
        assertEquals(listOf("open", "paid"), ordered.map { it.first.id.value })
    }

    @Test
    fun `within the same group the most recent goes first`() {
        val older = loan(id = "older", amount = "100", openedOn = "2026-01-01")
        val newer = loan(id = "newer", amount = "100", openedOn = "2026-09-01")
        val ordered = LoanOrder.sorted(listOf(older, newer).map { it to summaryOf(it) })
        assertEquals(listOf("newer", "older"), ordered.map { it.first.id.value })
    }

    @Test
    fun `the wire values of the direction match the api`() {
        assertEquals(LoanDirection.LENT, LoanDirection.fromWire("lent"))
        assertEquals(LoanDirection.BORROWED, LoanDirection.fromWire("borrowed"))
        assertEquals(null, LoanDirection.fromWire("other"))
    }

    private fun summaryOf(loan: Loan, display: CurrencyCode = CurrencyCode.COP): LoanSummary =
        LoanSummary.of(loan, display, RATES)

    @Suppress("LongParameterList") // Constructor de datos de ejemplo, sin logica.
    private fun loan(
        id: String = "l1",
        amount: String,
        payments: List<String> = emptyList(),
        openedOn: String = "2026-07-02",
        currency: CurrencyCode = CurrencyCode.COP,
    ) = Loan(
        id = LoanId(id),
        person = "Rosa",
        note = "",
        direction = LoanDirection.LENT,
        openedOn = LocalDate.parse(openedOn),
        money = MonetaryAmount.single(BigDecimal(amount), currency),
        payments = payments.mapIndexed { index, value ->
            LoanPayment(
                id = LoanPaymentId("p$index"),
                loanId = LoanId(id),
                paidOn = LocalDate.parse("2026-08-01"),
                label = null,
                money = MonetaryAmount.single(BigDecimal(value), currency),
            )
        },
    )

    private companion object {
        val RATES = ExchangeRates(
            base = CurrencyCode.USD,
            date = LocalDate.of(2026, 9, 22),
            status = RatesStatus.OK,
            rates = mapOf(
                CurrencyCode.USD to BigDecimal("1"),
                CurrencyCode.COP to BigDecimal("3199.41"),
            ),
        )
    }
}
