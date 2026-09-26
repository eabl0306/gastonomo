package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** El sheet de moneda, que comparten Ajustes y los tres formularios de dinero. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class CurrencySheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `from settings it explains that existing records do not change`() {
        render(CurrencySheetOrigin.Settings)
        compose.onNodeWithText("Moneda principal").assertIsDisplayed()
        compose.onNodeWithText("FRECUENTES").assertIsDisplayed()
        compose.onNodeWithText("Peso colombiano").assertIsDisplayed()
    }

    @Test
    fun `from a form it says which currency is the default one`() {
        render(CurrencySheetOrigin.Record)
        compose.onNodeWithText("Moneda del registro").assertIsDisplayed()
    }

    @Test
    fun `the footer counts the whole catalogue`() {
        render(CurrencySheetOrigin.Settings)
        compose.onNodeWithText("4 monedas · tasas de Frankfurter").assertIsDisplayed()
    }

    @Test
    fun `while searching the frequent section is hidden`() {
        render(CurrencySheetOrigin.Settings, query = "eur")
        compose.onNodeWithText("Euro").assertIsDisplayed()
        compose.onNodeWithText("FRECUENTES").assertDoesNotExist()
    }

    @Test
    fun `a search with no matches says so`() {
        render(CurrencySheetOrigin.Settings, query = "zzz")
        compose.onNodeWithText("Sin resultados.").assertIsDisplayed()
    }

    @Test
    fun `picking a row reports its code`() {
        val picked = mutableListOf<CurrencyCode>()
        render(CurrencySheetOrigin.Settings, onSelect = { picked += it })
        compose.onNodeWithText("Euro").performClick()
        assertEquals(listOf(CurrencyCode.EUR), picked)
    }

    @Test
    fun `it filters by code and by name, ignoring case`() {
        assertEquals(listOf(CurrencyCode.USD), filterCurrencies(CATALOG, "usd").map { it.code })
        assertEquals(listOf(CurrencyCode.EUR), filterCurrencies(CATALOG, " EuRo ").map { it.code })
        assertEquals(CATALOG, filterCurrencies(CATALOG, "  "))
    }

    private fun render(
        origin: CurrencySheetOrigin,
        query: String = "",
        onSelect: (CurrencyCode) -> Unit = {},
    ) {
        compose.setContent {
            GastonomoTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    CurrencySheet(
                        origin = origin,
                        currencies = CATALOG,
                        selected = CurrencyCode.COP,
                        query = query,
                        onQueryChange = {},
                        onSelect = onSelect,
                        onDismiss = {},
                    )
                }
            }
        }
    }

    private companion object {
        val CATALOG = listOf(
            CurrencyInfo(CurrencyCode.COP, "Colombian Peso", "$"),
            CurrencyInfo(CurrencyCode.USD, "United States Dollar", "$"),
            CurrencyInfo(CurrencyCode.EUR, "Euro", "€"),
            CurrencyInfo(CurrencyCode.MXN, "Mexican Peso", "$"),
        )
    }
}
