package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.format.GastonomoFormats
import com.bahoque.gastonomo.core.designsystem.format.periodLabel
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.ConversionSuggestion
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Los componentes compartidos que anadio la tanda de Inicio y Movimientos: el filtro de fechas,
 * la caja de conversion, el monto de un formulario y la fila de un movimiento.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class PeriodComponentsTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the shared components render to a png`() {
        compose.captureGastonomoScreen(SCREENSHOT_NAME) { Gallery() }
        val file = screenshotFile(SCREENSHOT_NAME)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        compose.onNodeWithText("Septiembre 2026").assertIsDisplayed()
        compose.onNodeWithText("Mercado").assertIsDisplayed()
    }

    @Test
    fun `the conversion box explains the suggested rate`() {
        compose.captureGastonomoScreen("conversion-box-suggested") {
            ConversionBox(
                suggestion = suggestion(edited = null),
                baseRaw = null,
                onBaseChange = {},
                onUseSuggested = {},
            )
        }
        compose.onNodeWithText("Equivale a (COP)").assertIsDisplayed()
        compose.onNodeWithText(SUGGESTED_HELP).assertIsDisplayed()
    }

    @Test
    fun `an edited equivalent offers the suggested one back`() {
        var reset = false
        compose.captureGastonomoScreen("conversion-box-edited") {
            ConversionBox(
                suggestion = suggestion(edited = BigDecimal("450000")),
                baseRaw = "450000",
                onBaseChange = {},
                onUseSuggested = { reset = true },
            )
        }
        compose.onNodeWithText(OWN_HELP).assertIsDisplayed()
        compose.onNodeWithText("Usar sugerida").performClick()
        assertTrue(reset)
    }

    @Test
    fun `the range sheet lists the five presets`() {
        var applied = false
        compose.captureGastonomoScreen("date-range-sheet") {
            DateRangeSheet(
                from = LocalDate.of(2026, 8, 22),
                to = LocalDate.of(2026, 9, 22),
                selectedPreset = PeriodPreset.LAST_30_DAYS,
                onPreset = {},
                onFrom = {},
                onTo = {},
                onApply = { applied = true },
                onDismiss = {},
            )
        }
        compose.onNodeWithText("Rango de fechas").assertIsDisplayed()
        compose.onNodeWithText("Últimos 3 meses").assertIsDisplayed()
        compose.onNodeWithText("22 ago 2026").assertIsDisplayed()
        compose.onNodeWithText("Aplicar rango").performClick()
        assertTrue(applied)
    }

    @Test
    fun `the month arrows only move when they can`() {
        var forward = 0
        compose.captureGastonomoScreen("period-picker-blocked") {
            PeriodPickerRow(
                label = GastonomoFormats().periodLabel(Period.Month(YearMonth.of(2026, 9))),
                isRange = false,
                canGoForward = false,
                onPrevious = {},
                onNextOrClear = { forward++ },
                onOpenRange = {},
            )
        }
        compose.onNodeWithText("Septiembre 2026").assertIsDisplayed()
        assertEquals(0, forward)
    }

    @Test
    fun `with a range active the picker changes its three parts`() {
        var cleared = 0
        compose.captureGastonomoScreen("period-picker-range") {
            PeriodPickerRow(
                label = GastonomoFormats().periodLabel(
                    Period.Range(LocalDate.of(2026, 8, 22), LocalDate.of(2026, 9, 22)),
                ),
                isRange = true,
                canGoForward = false,
                onPrevious = {},
                onNextOrClear = { cleared++ },
                onOpenRange = {},
            )
        }
        compose.onNodeWithText("22 ago – 22 sept 2026").assertIsDisplayed()
        compose.onNodeWithContentDescription("Volver al mes actual").performClick()
        assertEquals(1, cleared)
    }

    @Composable
    private fun Gallery() {
        Column(
            modifier = Modifier.fillMaxWidth().padding(GastonomoTheme.dimens.screenPadding),
            verticalArrangement = Arrangement.spacedBy(ITEM_GAP),
        ) {
            PeriodPickerRow(
                label = GastonomoFormats().periodLabel(Period.Month(YearMonth.of(2026, 9))),
                isRange = false,
                canGoForward = true,
                onPrevious = {},
                onNextOrClear = {},
                onOpenRange = {},
            )
            AmountInputField(
                raw = "186400",
                onRawChange = {},
                currency = CurrencyCode.COP,
                color = GastonomoTheme.colors.red,
            )
            CurrencyChip(currency = CurrencyCode.USD, onClick = {})
            ConversionBox(
                suggestion = suggestion(edited = null),
                baseRaw = null,
                onBaseChange = {},
                onUseSuggested = {},
            )
            GastonomoDateField(label = "Fecha", value = LocalDate.of(2026, 9, 22), onValueChange = {})
            GastonomoCard {
                MovementRow(
                    title = "Mercado",
                    subtitle = "Alimentación · 15 sept",
                    categoryIcon = "restaurant",
                    categoryColor = GastonomoTheme.colors.red,
                    amount = "− $ 186.400",
                    amountColor = GastonomoTheme.colors.ink,
                    showDivider = false,
                )
            }
        }
    }

    private fun suggestion(edited: BigDecimal?) = ConversionSuggestion.of(
        entered = Money(BigDecimal("140"), CurrencyCode.USD),
        display = CurrencyCode.COP,
        rates = RATES,
        editedBase = edited,
    )

    private companion object {
        val ITEM_GAP = 12.dp

        const val SCREENSHOT_NAME = "design-system-period"
        const val SUGGESTED_HELP =
            "Tasa sugerida por Frankfurter: 1 USD = 3.199,41 COP. Puedes cambiarla."
        const val OWN_HELP = "Tu tasa: 1 USD = 3.214,29 COP · sugerida 3.199,41"

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
