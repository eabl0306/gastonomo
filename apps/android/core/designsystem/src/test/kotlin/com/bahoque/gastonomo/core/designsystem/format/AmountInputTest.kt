package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * La entrada de importes, que es la replica de `numIn`, `showIn` y `toRaw` del prototipo.
 */
class AmountInputTest {

    private val spanish = GastonomoFormats(AppLanguage.SPANISH).amountInput
    private val english = GastonomoFormats(AppLanguage.ENGLISH).amountInput

    @Test
    fun `only digits get through`() {
        assertEquals("1250", spanish.sanitize("1a2b5c0", COP))
        assertEquals("", spanish.sanitize("abc", COP))
    }

    @Test
    fun `a currency without decimals does not accept a separator`() {
        assertEquals("1250", spanish.sanitize("12,50", COP))
    }

    @Test
    fun `a currency with decimals accepts one separator and no more`() {
        assertEquals("12.50", spanish.sanitize("12,50", USD))
        assertEquals("12.50", spanish.sanitize("12,5,0", USD))
    }

    @Test
    fun `the decimals are cut to those of the currency`() {
        assertEquals("12.50", spanish.sanitize("12,5099", USD))
    }

    @Test
    fun `each language uses its own separator`() {
        assertEquals("12.5", english.sanitize("12.5", USD))
        assertEquals("125", english.sanitize("12,5", USD))
    }

    @Test
    fun `leading zeros are dropped`() {
        assertEquals("120", spanish.sanitize("000120", COP))
        assertEquals("", spanish.sanitize("0000", COP))
    }

    @Test
    fun `a separator without an integer part becomes a zero`() {
        assertEquals("0.5", spanish.sanitize(",5", USD))
    }

    @Test
    fun `the integer part stops at eleven digits`() {
        assertEquals("12345678901", spanish.sanitize("123456789012345", COP))
    }

    @Test
    fun `the displayed value groups thousands with the separator of the language`() {
        assertEquals("1.250.000", spanish.display("1250000"))
        assertEquals("1,250,000", english.display("1250000"))
        assertEquals("140,5", spanish.display("140.5"))
        assertEquals("", spanish.display(""))
    }

    @Test
    fun `a value that is only a separator displays as zero`() {
        assertEquals("0,", spanish.display(".").take(2))
    }

    @Test
    fun `the separator of the language is exposed for the keyboard`() {
        assertEquals(',', spanish.separator())
        assertEquals('.', english.separator())
    }

    @Test
    fun `a raw value is parsed to a decimal and an empty one is zero`() {
        assertEquals(BigDecimal("1250.5"), AmountInput.parse("1250.5"))
        assertEquals(BigDecimal.ZERO, AmountInput.parse(""))
        assertEquals(BigDecimal("12"), AmountInput.parse("12."))
    }

    @Test
    fun `a calculated value becomes a raw value without padding zeros`() {
        assertEquals("450000", AmountInput.toRaw(BigDecimal("450000.004"), COP))
        assertEquals("140.5", AmountInput.toRaw(BigDecimal("140.499"), USD))
        assertEquals("0", AmountInput.toRaw(BigDecimal("0.001"), COP))
        assertEquals("1", AmountInput.toRaw(BigDecimal("1.00"), USD))
    }

    @Test
    fun `the caret stays behind the same digit although a separator moved`() {
        // "1.234" con el cursor detras del 2, que es el segundo digito: "1.2|34".
        assertEquals(3, AmountInput.caretAfter("1.234", 2))
        // Un digito mas convierte el texto en "12.345" y el cursor sigue detras del 2.
        assertEquals(2, AmountInput.caretAfter("12.345", 2))
    }

    @Test
    fun `no digits before the caret puts it at the start and too many at the end`() {
        assertEquals(0, AmountInput.caretAfter("1.234", 0))
        assertEquals(5, AmountInput.caretAfter("1.234", 9))
        assertEquals(0, AmountInput.caretAfter("", 3))
    }

    private companion object {
        val COP = CurrencyCode.COP
        val USD = CurrencyCode.USD
    }
}
