package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ConversionSuggestionTest {

    @Test
    fun `in the display currency there is no conversion box`() {
        val suggestion = suggestionOf("100000", CurrencyCode.COP)
        assertFalse(suggestion.isForeign)
        assertEquals(BigDecimal("100000"), suggestion.base)
        assertTrue(suggestion.isValid)
    }

    @Test
    fun `in the display currency the equivalent is the amount itself`() {
        val money = suggestionOf("100000", CurrencyCode.COP).money
        assertEquals(money.currency, money.baseCurrency)
        assertEquals(0, money.amount.compareTo(money.base))
    }

    @Test
    fun `another currency suggests the amount times the rate`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD)
        assertTrue(suggestion.isForeign)
        assertFalse(suggestion.isEdited)
        assertEquals(0, suggestion.suggestedBase?.compareTo(BigDecimal("447917.4")))
    }

    @Test
    fun `an edited equivalent wins over the suggested one`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("450000"))
        assertTrue(suggestion.isEdited)
        assertEquals(BigDecimal("450000"), suggestion.base)
        assertEquals(0, suggestion.ownRate?.compareTo(BigDecimal("3214.2857142857142857")))
    }

    @Test
    fun `the saved record keeps both the original amount and the equivalent`() {
        val money = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("450000")).money
        assertEquals(CurrencyCode.USD, money.currency)
        assertEquals(CurrencyCode.COP, money.baseCurrency)
        assertEquals(0, money.amount.compareTo(BigDecimal("140")))
        assertEquals(0, money.base.compareTo(BigDecimal("450000")))
    }

    @Test
    fun `without a rate there is no suggestion and the user has to type it`() {
        val suggestion = ConversionSuggestion.of(
            entered = Money(BigDecimal("50"), CurrencyCode("GBP")),
            display = CurrencyCode.COP,
            rates = RATES,
        )
        assertNull(suggestion.suggestedRate)
        assertNull(suggestion.suggestedBase)
        assertFalse(suggestion.isValid)
    }

    @Test
    fun `a zero amount is never valid`() {
        assertFalse(suggestionOf("0", CurrencyCode.COP).isValid)
        assertFalse(suggestionOf("", CurrencyCode.COP).isValid)
    }

    @Test
    fun `a zero equivalent is never valid`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal.ZERO)
        assertFalse(suggestion.isValid)
        assertNull(suggestion.ownRate)
    }

    private fun suggestionOf(
        amount: String,
        currency: CurrencyCode,
        edited: BigDecimal? = null,
    ) = ConversionSuggestion.of(
        entered = Money(amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, currency),
        display = CurrencyCode.COP,
        rates = RATES,
        editedBase = edited,
    )

    private companion object {
        val RATES = ExchangeRates(
            base = CurrencyCode.USD,
            status = RatesStatus.OK,
            rates = mapOf(
                CurrencyCode.USD to BigDecimal.ONE,
                CurrencyCode.COP to BigDecimal("3199.41"),
            ),
        )
    }
}
