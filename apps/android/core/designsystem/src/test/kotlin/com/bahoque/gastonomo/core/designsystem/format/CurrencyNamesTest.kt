package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import org.junit.Test
import kotlin.test.assertEquals

/** El nombre de una moneda cambia con el idioma, igual que en el prototipo. */
class CurrencyNamesTest {

    @Test
    fun `it names the currency in spanish`() {
        assertEquals("Peso colombiano", spanish.currencyNames.of(CurrencyCode.COP))
        assertEquals("Euro", spanish.currencyNames.of(CurrencyCode.EUR))
    }

    @Test
    fun `it names the currency in english`() {
        assertEquals("US Dollar", english.currencyNames.of(CurrencyCode.USD))
    }

    @Test
    fun `an unknown code falls back to what the api sent`() {
        val unknown = CurrencyCode("QQQ")
        assertEquals("Moneda de pruebas", spanish.currencyNames.of(unknown, "Moneda de pruebas"))
    }

    private val spanish = GastonomoFormats(language = AppLanguage.SPANISH)
    private val english = GastonomoFormats(language = AppLanguage.ENGLISH)
}
