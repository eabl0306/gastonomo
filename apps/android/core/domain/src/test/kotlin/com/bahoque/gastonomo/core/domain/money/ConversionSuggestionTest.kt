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
    fun `another currency works out the equivalent with the rate in force`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD)
        assertTrue(suggestion.isForeign)
        assertFalse(suggestion.isEdited)
        assertEquals(0, suggestion.rate?.compareTo(BigDecimal("3199.41")))
        assertEquals(0, suggestion.base.compareTo(BigDecimal("447917.4")))
    }

    @Test
    fun `an edited rate wins over the suggested one and moves the equivalent`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("4500"))
        assertTrue(suggestion.isEdited)
        assertEquals(0, suggestion.rate?.compareTo(BigDecimal("4500")))
        assertEquals(0, suggestion.base.compareTo(BigDecimal("630000")))
    }

    @Test
    fun `the saved record keeps both the original amount and the equivalent`() {
        val money = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("4500")).money
        assertEquals(CurrencyCode.USD, money.currency)
        assertEquals(CurrencyCode.COP, money.baseCurrency)
        assertEquals(0, money.amount.compareTo(BigDecimal("140")))
        assertEquals(0, money.base.compareTo(BigDecimal("630000")))
    }

    @Test
    fun `without a rate there is no suggestion and the user has to type it`() {
        val suggestion = ConversionSuggestion.of(
            entered = Money(BigDecimal("50"), CurrencyCode("GBP")),
            display = CurrencyCode.COP,
            rates = RATES,
        )
        assertNull(suggestion.suggestedRate)
        assertNull(suggestion.rate)
        assertEquals(0, suggestion.base.signum())
        assertFalse(suggestion.isValid)
    }

    @Test
    fun `a zero amount is never valid`() {
        assertFalse(suggestionOf("0", CurrencyCode.COP).isValid)
        assertFalse(suggestionOf("", CurrencyCode.COP).isValid)
    }

    @Test
    fun `a zero rate is never valid`() {
        val suggestion = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal.ZERO)
        assertEquals(0, suggestion.base.signum())
        assertFalse(suggestion.isValid)
    }

    /**
     * La tasa manda y la equivalencia se deriva. Con moneda sin fraccion, como el peso, los
     * decimales de la tasa son lo unico que separa un registro correcto de uno redondeado a lo
     * bruto: 140 por 3.199,41 no es lo mismo que 140 por 3.199.
     */
    @Test
    fun `the decimals of the rate reach the equivalent`() {
        val exact = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("3199.41"))
        val rounded = suggestionOf("140", CurrencyCode.USD, edited = BigDecimal("3199"))
        assertEquals(0, exact.base.compareTo(BigDecimal("447917.4")))
        assertEquals(0, rounded.base.compareTo(BigDecimal("447860")))
    }

    private fun suggestionOf(
        amount: String,
        currency: CurrencyCode,
        edited: BigDecimal? = null,
    ) = ConversionSuggestion.of(
        entered = Money(amount.toBigDecimalOrNull() ?: BigDecimal.ZERO, currency),
        display = CurrencyCode.COP,
        rates = RATES,
        editedRate = edited,
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
