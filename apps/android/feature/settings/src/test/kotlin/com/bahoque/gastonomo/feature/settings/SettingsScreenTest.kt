package com.bahoque.gastonomo.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.time.LocalDate
import kotlin.test.assertEquals

/** Lo que Ajustes ensena en cada estado, renderizado en la JVM. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class SettingsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows the profile, the three groups and the footer`() {
        setContent(loadedState())
        compose.onNodeWithText("Camilo Rojas").assertIsDisplayed()
        compose.onNodeWithText("camilo@correo.com").assertIsDisplayed()
        compose.onNodeWithText("CUENTA").assertIsDisplayed()
        compose.onNodeWithText("GENERAL").assertIsDisplayed()
        compose.onNodeWithText("DATOS").assertIsDisplayed()
        compose.onNodeWithText("Gastonomo 9.9.9 · Finanzas personales")
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `the currency row shows the code and the localized name`() {
        setContent(loadedState())
        compose.onNodeWithText("COP · Peso colombiano").assertIsDisplayed()
    }

    @Test
    fun `the rates row shows the frankfurter date when they are fresh`() {
        setContent(loadedState())
        compose.onNodeWithText("Frankfurter · 22 sept").assertIsDisplayed()
    }

    @Test
    fun `the rates row says offline when there are no fresh rates`() {
        setContent(loadedState().copy(rates = RatesIndicator.Offline))
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
    }

    @Test
    fun `the sync row says there is no connection when there is none`() {
        setContent(loadedState().copy(online = false))
        compose.onNodeWithText("Sin conexión").assertIsDisplayed()
    }

    @Test
    fun `the sync row says how many are waiting, instead of pretending all is well`() {
        setContent(loadedState().copy(online = false, pendingCount = 2))
        compose.onNodeWithText("2 sin enviar").assertIsDisplayed()
    }

    @Test
    fun `with network and a queue it says it is syncing`() {
        setContent(loadedState().copy(pendingCount = 1))
        compose.onNodeWithText("Sincronizando…").assertIsDisplayed()
    }

    @Test
    fun `the categories row shows how many there are`() {
        setContent(loadedState().copy(categoryCount = 13))
        compose.onNodeWithText("13").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `tapping the currency row asks to open its sheet`() {
        var opened = 0
        setContent(loadedState(), SettingsActions({ opened++ }, {}, {}, {}, {}, {}, {}))
        compose.onNodeWithText("Moneda").performScrollTo().performClick()
        assertEquals(1, opened)
    }

    private fun setContent(
        state: SettingsUiState,
        actions: SettingsActions = SettingsActions.NONE,
    ) {
        compose.setContent {
            GastonomoTheme {
                SettingsScreen(state = state, actions = actions, version = VERSION)
            }
        }
    }

    private fun loadedState() = SettingsUiState(
        loading = false,
        user = SampleAuth.user,
        settings = UserSettings(displayCurrency = CurrencyCode.COP),
        categoryCount = SampleFinance.categories.size,
        currencies = SampleFinance.currencies,
        rates = RatesIndicator.Ready(LocalDate.of(2026, 9, 22)),
    )

    private companion object {
        const val VERSION = "9.9.9"
    }
}
