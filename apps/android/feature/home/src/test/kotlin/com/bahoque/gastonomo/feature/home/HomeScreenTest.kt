package com.bahoque.gastonomo.feature.home

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Inicio con los datos del prototipo: lo que se ve y lo que pasa al tocarlo. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class HomeScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it renders the whole screen to a png`() {
        compose.captureGastonomoScreen(SCREENSHOT) { HomeScreen(HomeSamples.loaded, HomeActions.NONE) }
        val file = screenshotFile(SCREENSHOT)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        compose.onNodeWithText("Hola, Camilo").assertIsDisplayed()
        compose.onNodeWithText("Martes, 22 sept").assertIsDisplayed()
        compose.onNodeWithText("Balance de septiembre").assertIsDisplayed()
        compose.onNodeWithText("$ 2.921.600").assertIsDisplayed()
    }

    @Test
    fun `the eye masks every amount`() {
        var toggled = 0
        compose.captureGastonomoScreen("home-hidden") {
            HomeScreen(
                state = HomeSamples.loaded.copy(hideAmounts = true),
                actions = HomeActions.NONE.copy(onToggleHiddenAmounts = { toggled++ }),
            )
        }
        compose.onAllNodesWithText("$ ••••••").onFirst().assertIsDisplayed()
        compose.onNodeWithContentDescription("Mostrar los montos").performClick()
        assertEquals(1, toggled)
    }

    @Test
    fun `an empty period shows every empty state`() {
        compose.captureGastonomoScreen("home-empty") {
            HomeScreen(HomeSamples.empty, HomeActions.NONE)
        }
        compose.onNodeWithText("Sin gastos en este periodo.").assertIsDisplayed()
        compose.onNodeWithText("Sin movimientos en este periodo.").assertIsDisplayed()
        compose.onAllNodesWithText("Nada pendiente").onFirst().assertIsDisplayed()
    }

    @Test
    fun `a quick action opens the form with its kind`() {
        val opened = mutableListOf<MovementKind>()
        compose.captureGastonomoScreen("home-quick-actions") {
            HomeScreen(
                state = HomeSamples.loaded,
                actions = HomeActions.NONE.copy(onNewMovement = { opened += it }),
            )
        }
        compose.onNodeWithText("Ingreso").performClick()
        compose.onNodeWithText("Gasto").performClick()
        assertEquals(listOf(MovementKind.IN, MovementKind.OUT), opened)
    }

    @Test
    fun `an error offers to try again`() {
        var retried = 0
        compose.captureGastonomoScreen("home-error") {
            HomeScreen(
                state = HomeSamples.empty.copy(error = HomeError.NETWORK),
                actions = HomeActions.NONE.copy(onRetry = { retried++ }),
            )
        }
        compose.onNodeWithText("No hay conexión. Inténtalo de nuevo.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `the toast of a saved movement is shown over the screen`() {
        compose.captureGastonomoScreen("home-toast") {
            HomeScreen(
                state = HomeSamples.loaded.copy(toast = TransientMessages.MOVEMENT_SAVED_EXPENSE),
                actions = HomeActions.NONE,
            )
        }
        compose.onNodeWithText("Gasto guardado").assertIsDisplayed()
    }

    @Test
    fun `the range sheet opens over the screen`() {
        compose.captureGastonomoScreen("home-range-sheet") {
            HomeScreen(
                state = HomeSamples.loaded.copy(
                    rangeDraft = com.bahoque.gastonomo.core.application.state.RangeDraft(
                        from = HomeSamples.loaded.selection.period.from,
                        to = HomeSamples.loaded.today,
                    ),
                ),
                actions = HomeActions.NONE,
            )
        }
        compose.onNodeWithText("Rango de fechas").assertIsDisplayed()
        compose.onNodeWithText("Este mes").assertIsDisplayed()
    }

    private companion object {
        const val SCREENSHOT = "home"
    }
}
