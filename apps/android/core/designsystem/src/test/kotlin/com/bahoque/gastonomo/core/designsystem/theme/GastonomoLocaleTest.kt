package com.bahoque.gastonomo.core.designsystem.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.SideEffect
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

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

    /**
     * Poner el idioma sustituye `LocalContext` por un contexto de configuracion, y `LocalActivity`
     * no se provee en ningun sitio: se **deriva** de `LocalContext`. Sin volver a fijarlo, toda
     * pantalla dentro del tema veia null y se quedaba sin actividad, que es como la de Exportar
     * dejo de poder abrir la hoja de compartir.
     */
    @Test
    fun `the activity survives the language substitution`() {
        var activity: Any? = null
        var seen = false
        compose.setContent {
            GastonomoTheme(language = AppLanguage.SPANISH, displayCurrency = CurrencyCode.COP) {
                val current = LocalActivity.current
                SideEffect {
                    activity = current
                    seen = true
                }
            }
        }
        compose.waitForIdle()
        assertTrue(seen, "el contenido del tema no se compuso")
        assertNotNull(
            activity,
            "LocalActivity se calcula a partir de LocalContext, y el tema lo sustituye: hay que " +
                "volver a fijarlo o nadie dentro de la aplicacion puede abrir nada del sistema",
        )
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
