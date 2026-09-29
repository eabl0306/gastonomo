package com.bahoque.gastonomo.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheet
import com.bahoque.gastonomo.core.designsystem.component.CurrencySheetOrigin
import com.bahoque.gastonomo.core.designsystem.component.LanguageSheet
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.navigation.TopLevelDestination
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.captureGastonomoTabScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertTrue

/** Las capturas de Ajustes y de los dos sheets que abre. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class SettingsScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the settings screen renders to a png`() {
        compose.captureGastonomoTabScreen(SETTINGS, TopLevelDestination.SETTINGS) {
            SettingsScreen(state = state(), actions = SettingsActions.NONE, version = "0.1.0")
        }
        assertTrue(screenshotFile(SETTINGS).exists())
    }

    @Test
    fun `the currency sheet renders to a png`() {
        compose.captureGastonomoScreen(CURRENCY) {
            Box(modifier = Modifier.fillMaxSize()) {
                CurrencySheet(
                    origin = CurrencySheetOrigin.Settings,
                    currencies = SampleFinance.currencies,
                    selected = CurrencyCode.COP,
                    query = "",
                    onQueryChange = {},
                    onSelect = {},
                    onDismiss = {},
                )
            }
        }
        assertTrue(screenshotFile(CURRENCY).exists())
    }

    @Test
    fun `the language sheet renders to a png`() {
        compose.captureGastonomoScreen(LANGUAGE) {
            Box(modifier = Modifier.fillMaxSize()) {
                LanguageSheet(selected = AppLanguage.SPANISH, onSelect = {}, onDismiss = {})
            }
        }
        assertTrue(screenshotFile(LANGUAGE).exists())
    }

    @Test
    fun `the pin sheet renders to a png`() {
        compose.captureGastonomoScreen(PIN) {
            Box(modifier = Modifier.fillMaxSize()) {
                PinSheet(pin = "12", onPinChanged = {}, onConfirm = {}, onDismiss = {})
            }
        }
        assertTrue(screenshotFile(PIN).exists())
    }

    private fun state() = SettingsUiState(
        loading = false,
        user = SampleAuth.user,
        settings = UserSettings(),
        categoryCount = SampleFinance.categories.size,
        currencies = SampleFinance.currencies,
        rates = RatesIndicator.Ready(LocalDate.of(2026, 9, 22)),
    )

    private companion object {
        const val SETTINGS = "settings"
        const val CURRENCY = "sheet-currency"
        const val LANGUAGE = "sheet-language"
        const val PIN = "sheet-pin"
    }
}
