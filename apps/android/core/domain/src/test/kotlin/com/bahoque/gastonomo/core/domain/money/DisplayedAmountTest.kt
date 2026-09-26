package com.bahoque.gastonomo.core.domain.money

import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Las cuatro ramas de la regla central de multimoneda, una por test. Es la funcion `disp`
 * del prototipo y de ella cuelgan todos los totales de la aplicacion.
 */
class DisplayedAmountTest {

    @Test
    fun `when the record is already in the shown currency it uses its own amount`() {
        val record = MonetaryAmount(
            amount = BigDecimal("140"),
            currency = USD,
            base = BigDecimal("450000"),
            baseCurrency = COP,
        )
        assertEquals(BigDecimal("140"), DisplayedAmount.of(record, USD, RATES))
    }

    @Test
    fun `when the stored equivalent is in the shown currency it wins over today rates`() {
        // 450000 no es 140 por la tasa de hoy: es la tasa que el usuario escribio a mano, y
        // se respeta.
        val record = MonetaryAmount(
            amount = BigDecimal("140"),
            currency = USD,
            base = BigDecimal("450000"),
            baseCurrency = COP,
        )
        assertEquals(BigDecimal("450000"), DisplayedAmount.of(record, COP, RATES))
    }

    @Test
    fun `otherwise it converts the stored equivalent with today rates`() {
        val record = MonetaryAmount(
            amount = BigDecimal("140"),
            currency = USD,
            base = BigDecimal("450000"),
            baseCurrency = COP,
        )
        // 450000 COP a EUR: 450000 * 0.87088 / 3199.41
        val shown = DisplayedAmount.of(record, EUR, RATES)
        assertEquals(0, shown.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(BigDecimal("122.49")))
    }

    @Test
    fun `without a rate for the stored equivalent it converts the original amount`() {
        val record = MonetaryAmount(
            amount = BigDecimal("100"),
            currency = USD,
            base = BigDecimal("1000"),
            baseCurrency = CurrencyCode("CLP"),
        )
        // No hay tasa de CLP, pero si de USD: 100 USD a EUR son 87.088.
        assertEquals(0, DisplayedAmount.of(record, EUR, RATES).compareTo(BigDecimal("87.088")))
    }

    @Test
    fun `with no rate at all it falls back to the stored equivalent`() {
        val record = MonetaryAmount(
            amount = BigDecimal("100"),
            currency = CurrencyCode("CLP"),
            base = BigDecimal("1000"),
            baseCurrency = CurrencyCode("CLP"),
        )
        assertEquals(BigDecimal("1000"), DisplayedAmount.of(record, EUR, ExchangeRates.EMPTY))
    }

    @Test
    fun `converting to the same currency changes nothing`() {
        assertEquals(BigDecimal("5"), RATES.convert(BigDecimal("5"), COP, COP))
    }

    @Test
    fun `a missing rate makes the conversion fail instead of guessing`() {
        assertNull(RATES.convert(BigDecimal("5"), CurrencyCode("VND"), COP))
        assertNull(RATES.convert(BigDecimal("5"), COP, CurrencyCode("VND")))
    }

    @Test
    fun `the table says which currencies it covers`() {
        assertTrue(RATES.covers(COP))
        assertTrue(!RATES.covers(CurrencyCode("VND")))
        assertEquals(RatesStatus.OK, RATES.status)
        assertEquals(LocalDate.of(2026, 9, 22), RATES.date)
    }

    @Test
    fun `the rate of one unit is what the conversion box shows`() {
        assertEquals(0, RATES.rateOf(USD, COP)?.compareTo(BigDecimal("3199.41")))
    }

    @Test
    fun `summing records converts each one before adding`() {
        val pesos = MonetaryAmount.single(BigDecimal("1000"), COP)
        val dollars = MonetaryAmount.single(BigDecimal("1"), USD)
        val total = DisplayedAmount.sum(listOf(pesos, dollars), COP, RATES)
        assertEquals(COP, total.currency)
        assertEquals(0, total.amount.compareTo(BigDecimal("4199.41")))
    }

    @Test
    fun `money wraps the shown value with the shown currency`() {
        val record = MonetaryAmount.single(BigDecimal("10"), USD)
        assertEquals(Money(BigDecimal("10"), USD), DisplayedAmount.money(record, USD, RATES))
    }

    private companion object {
        val COP = CurrencyCode.COP
        val USD = CurrencyCode.USD
        val EUR = CurrencyCode.EUR

        val RATES = ExchangeRates(
            base = USD,
            date = LocalDate.of(2026, 9, 22),
            status = RatesStatus.OK,
            rates = mapOf(
                USD to BigDecimal("1"),
                COP to BigDecimal("3199.41"),
                EUR to BigDecimal("0.87088"),
            ),
        )
    }
}
