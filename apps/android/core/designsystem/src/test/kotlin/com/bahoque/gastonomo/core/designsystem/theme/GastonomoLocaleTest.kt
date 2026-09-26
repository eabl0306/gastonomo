package com.bahoque.gastonomo.core.designsystem.theme

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.R
import com.bahoque.gastonomo.core.designsystem.component.GastonomoText
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * El idioma se elige en Ajustes, no en el telefono. El dispositivo esta en es-CO en los dos
 * tests: lo que cambia los textos es el parametro del tema, no el idioma del sistema.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class GastonomoLocaleTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `spanish keeps the strings of the device`() {
        render(AppLanguage.SPANISH)
        compose.onNodeWithText("Moneda principal").assertIsDisplayed()
    }

    @Test
    fun `english switches the strings although the device is in spanish`() {
        render(AppLanguage.ENGLISH)
        compose.onNodeWithText("Main currency").assertIsDisplayed()
    }

    @Test
    fun `the language also decides how an amount is written`() {
        render(AppLanguage.ENGLISH)
        compose.onNodeWithText("1,234.56", substring = true).assertIsDisplayed()
    }

    private fun render(language: AppLanguage) {
        compose.setContent {
            GastonomoTheme(language = language, displayCurrency = CurrencyCode.USD) {
                Column {
                    GastonomoText(stringResource(R.string.gastonomo_currency_title_settings))
                    GastonomoText(
                        GastonomoTheme.formats.amount("1234.56".toBigDecimal(), CurrencyCode.USD),
                    )
                }
            }
        }
    }
}
