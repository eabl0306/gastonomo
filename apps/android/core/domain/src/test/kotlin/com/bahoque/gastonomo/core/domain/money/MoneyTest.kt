package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MoneyTest {

    @Test
    fun `a currency code is three uppercase letters`() {
        assertEquals("COP", CurrencyCode("COP").value)
        assertFailsWith<IllegalArgumentException> { CurrencyCode("cop") }
        assertFailsWith<IllegalArgumentException> { CurrencyCode("COPP") }
    }

    @Test
    fun `parsing a currency code normalises case and spacing`() {
        assertEquals(CurrencyCode.USD, CurrencyCode.parseOrNull(" usd "))
        assertNull(CurrencyCode.parseOrNull("12"))
    }

    @Test
    fun `a currency code prints as its value`() {
        assertEquals("EUR", CurrencyCode.EUR.toString())
    }

    @Test
    fun `the currencies without a fraction show no decimals`() {
        assertEquals(0, CurrencyDecimals.of(CurrencyCode.COP))
        assertEquals(0, CurrencyDecimals.of(CurrencyCode("JPY")))
        assertEquals(2, CurrencyDecimals.of(CurrencyCode.USD))
        assertTrue(CurrencyDecimals.hasNoFraction(CurrencyCode.COP))
        assertFalse(CurrencyDecimals.hasNoFraction(CurrencyCode.EUR))
    }

    @Test
    fun `money adds and subtracts within the same currency`() {
        val ten = Money(BigDecimal("10"), CurrencyCode.USD)
        val four = Money(BigDecimal("4"), CurrencyCode.USD)
        assertEquals(BigDecimal("14"), (ten + four).amount)
        assertEquals(BigDecimal("6"), (ten - four).amount)
        assertEquals(BigDecimal("-10"), (-ten).amount)
        assertEquals(BigDecimal("10"), (-ten).abs().amount)
    }

    @Test
    fun `mixing currencies is a programming error`() {
        val pesos = Money(BigDecimal("10"), CurrencyCode.COP)
        val dollars = Money(BigDecimal("10"), CurrencyCode.USD)
        assertFailsWith<IllegalArgumentException> { pesos + dollars }
    }

    @Test
    fun `money compares and reports its sign`() {
        val zero = Money.zero(CurrencyCode.COP)
        val negative = Money(BigDecimal("-1"), CurrencyCode.COP)
        assertTrue(zero.isZero)
        assertTrue(negative.isNegative)
        assertTrue(negative < zero)
    }

    @Test
    fun `rounding uses the decimals of the currency`() {
        assertEquals(BigDecimal("1235"), Money(BigDecimal("1234.5"), CurrencyCode.COP).rounded().amount)
        assertEquals(BigDecimal("1.23"), Money(BigDecimal("1.234"), CurrencyCode.USD).rounded().amount)
    }

    @Test
    fun `money is parsed from the string the api sends`() {
        assertEquals(BigDecimal("186400.0000"), Money.parse("186400.0000", CurrencyCode.COP).amount)
        assertNull(Money.parseOrNull("not a number", CurrencyCode.COP))
    }

    @Test
    fun `a single currency amount is its own equivalent`() {
        val amount = MonetaryAmount.single(BigDecimal("500"), CurrencyCode.COP)
        assertEquals(amount.amount, amount.base)
        assertEquals(amount.currency, amount.baseCurrency)
        assertFalse(amount.isForeignTo(CurrencyCode.COP))
        assertTrue(amount.isForeignTo(CurrencyCode.USD))
    }

    @Test
    fun `a monetary amount exposes its original and its equivalent`() {
        val amount = MonetaryAmount.of(
            original = Money(BigDecimal("140"), CurrencyCode.USD),
            equivalent = Money(BigDecimal("450000"), CurrencyCode.COP),
        )
        assertEquals(Money(BigDecimal("140"), CurrencyCode.USD), amount.original)
        assertEquals(Money(BigDecimal("450000"), CurrencyCode.COP), amount.equivalent)
    }
}
