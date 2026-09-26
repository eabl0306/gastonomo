package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * El formato de dinero y de tasas, que es donde mas silenciosamente se rompen las cosas.
 *
 * Los espacios de estos asertos son espacios normales: el formateador pone uno entre el
 * simbolo y el numero, y el agrupador del idioma pone punto o coma segun toque.
 */
class GastonomoFormatsTest {

    private val spanish = GastonomoFormats(AppLanguage.SPANISH, CurrencyCode.COP)
    private val english = GastonomoFormats(AppLanguage.ENGLISH, CurrencyCode.USD)

    @Test
    fun `pesos show no decimals and group with a dot`() {
        assertEquals("$ 186.400", spanish.amount(BigDecimal("186400")))
        assertEquals("$ 1.250.000", spanish.amount(BigDecimal("1250000")))
    }

    @Test
    fun `dollars show two decimals and the US prefix`() {
        assertEquals("US$ 140,00", spanish.amount(BigDecimal("140"), CurrencyCode.USD))
        assertEquals("US$ 140.00", english.amount(BigDecimal("140"), CurrencyCode.USD))
    }

    @Test
    fun `another currency whose symbol is a dollar takes two letters of its code`() {
        assertEquals("MX$ 250,00", spanish.amount(BigDecimal("250"), CurrencyCode.MXN))
    }

    @Test
    fun `a currency with its own symbol keeps it`() {
        assertEquals("€ 99,90", spanish.amount(BigDecimal("99.9"), CurrencyCode.EUR))
    }

    @Test
    fun `a currency the catalog does not know falls back to its code`() {
        assertEquals("NOK 10,00", spanish.amount(BigDecimal("10"), CurrencyCode("NOK")))
    }

    @Test
    fun `the amount is always absolute and the sign goes in front`() {
        assertEquals("$ 186.400", spanish.amount(BigDecimal("-186400")))
        assertEquals("+$ 2.800.000", spanish.signed(BigDecimal("2800000"), isIncome = true))
        assertEquals("−$ 186.400", spanish.signed(BigDecimal("186400"), isIncome = false))
    }

    @Test
    fun `an expense uses the typographic minus and never the hyphen`() {
        val expense = spanish.signed(BigDecimal("1"), isIncome = false)
        assertEquals('−', expense.first())
    }

    @Test
    fun `a net is positive or negative depending on its sign`() {
        assertEquals("+$ 100", spanish.net(BigDecimal("100")))
        assertEquals("−$ 100", spanish.net(BigDecimal("-100")))
        assertEquals("+$ 0", spanish.net(BigDecimal.ZERO))
    }

    @Test
    fun `a balance only shows the minus when it is negative`() {
        assertEquals("$ 100", spanish.balance(BigDecimal("100")))
        assertEquals("−$ 100", spanish.balance(BigDecimal("-100")))
    }

    @Test
    fun `the masked balance keeps the symbol`() {
        assertEquals("$ ••••••", spanish.masked())
    }

    @Test
    fun `a money value carries its own currency`() {
        assertEquals("US$ 12,50", spanish.amount(Money(BigDecimal("12.5"), CurrencyCode.USD)))
    }

    @Test
    fun `a rate below one shows six decimals`() {
        assertEquals("0,87088", spanish.rate(BigDecimal("0.87088")))
        assertEquals("0,000123", spanish.rate(BigDecimal("0.00012345")))
    }

    @Test
    fun `a rate below one hundred shows up to four decimals`() {
        assertEquals("17,2402", spanish.rate(BigDecimal("17.2402")))
        assertEquals("1", spanish.rate(BigDecimal.ONE))
    }

    @Test
    fun `a rate of one hundred or more shows up to two decimals`() {
        assertEquals("3.199,41", spanish.rate(BigDecimal("3199.41")))
    }

    @Test
    fun `the symbol can be asked for on its own`() {
        assertEquals("US$", spanish.symbolOf(CurrencyCode.USD))
        assertEquals("$", spanish.symbolOf(CurrencyCode.COP))
    }

    @Test
    fun `the api catalog replaces the fallback symbols`() {
        val krone = CurrencyInfo(CurrencyCode("NOK"), "Norwegian Krone", "kr")
        val catalog = CurrencySymbols.DEFAULT.withCatalog(listOf(krone))
        val formats = spanish.withSymbols(catalog)
        assertEquals("kr 10,00", formats.amount(BigDecimal("10"), CurrencyCode("NOK")))
    }

    @Test
    fun `an empty catalog leaves the symbols as they were`() {
        val untouched = CurrencySymbols.DEFAULT.withCatalog(emptyList())
        assertEquals(CurrencySymbols.DEFAULT.of(CurrencyCode.COP), untouched.of(CurrencyCode.COP))
    }
}
