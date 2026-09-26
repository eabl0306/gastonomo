package com.bahoque.gastonomo.feature.movements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.state.RangeDraft
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** La lista de Movimientos con los datos del prototipo. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class MovementsScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it renders the whole screen to a png`() {
        compose.captureGastonomoScreen(SCREENSHOT) {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        val file = screenshotFile(SCREENSHOT)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        compose.onNodeWithText("Movimientos").assertIsDisplayed()
        compose.onNodeWithText("$ 3.250.000").assertIsDisplayed()
        compose.onNodeWithText("$ 328.400").assertIsDisplayed()
        compose.onNodeWithText("+$ 2.921.600").assertIsDisplayed()
    }

    @Test
    fun `the days are grouped and named`() {
        compose.captureGastonomoScreen("movements-groups") {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        compose.onNodeWithText("Martes 15 sept").assertIsDisplayed()
        compose.onNodeWithText("Mercado").assertIsDisplayed()
        compose.onNodeWithText("Alimentación").assertIsDisplayed()
    }

    @Test
    fun `a movement in another currency shows what was typed`() {
        compose.captureGastonomoScreen("movements-foreign") {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        compose.onNodeWithText("Freelance · US$ 140,00").assertIsDisplayed()
    }

    @Test
    fun `the pills ask for a filter`() {
        val chosen = mutableListOf<MovementFilter>()
        compose.captureGastonomoScreen("movements-pills") {
            MovementsScreen(
                state = MovementsSamples.list.copy(filter = MovementFilter.EXPENSE),
                actions = MovementsActions.NONE.copy(onFilter = { chosen += it }),
            )
        }
        compose.onNode(hasText("Ingresos") and hasClickAction()).performClick()
        assertEquals(MovementFilter.INCOME, chosen.single())
    }

    @Test
    fun `an empty period says so`() {
        compose.captureGastonomoScreen("movements-empty") {
            MovementsScreen(MovementsSamples.emptyList, MovementsActions.NONE)
        }
        compose.onNodeWithText("No hay movimientos en este periodo.").assertIsDisplayed()
    }

    @Test
    fun `an error offers to try again`() {
        var retried = 0
        compose.captureGastonomoScreen("movements-error") {
            MovementsScreen(
                state = MovementsSamples.emptyList.copy(error = MovementsError.NETWORK),
                actions = MovementsActions.NONE.copy(onRetry = { retried++ }),
            )
        }
        compose.onNodeWithText("No hay conexión. Inténtalo de nuevo.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `the fab opens the form`() {
        var opened = 0
        compose.captureGastonomoScreen("movements-fab") {
            MovementsScreen(
                state = MovementsSamples.list,
                actions = MovementsActions.NONE.copy(onNewMovement = { opened++ }),
            )
        }
        compose.onNodeWithText("Nuevo").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun `the toast of a saved movement is shown over the list`() {
        compose.captureGastonomoScreen("movements-toast") {
            MovementsScreen(
                state = MovementsSamples.list.copy(
                    toast = TransientMessages.MOVEMENT_SAVED_INCOME,
                ),
                actions = MovementsActions.NONE,
            )
        }
        compose.onNodeWithText("Ingreso guardado").assertIsDisplayed()
    }

    @Test
    fun `the range sheet opens over the list`() {
        compose.captureGastonomoScreen("movements-range-sheet") {
            MovementsScreen(
                state = MovementsSamples.list.copy(
                    rangeDraft = RangeDraft(
                        from = MovementsSamples.list.selection.period.from,
                        to = MovementsSamples.list.today,
                    ),
                ),
                actions = MovementsActions.NONE,
            )
        }
        compose.onNodeWithText("Rango de fechas").assertIsDisplayed()
    }

    private companion object {
        const val SCREENSHOT = "movements"
    }
}
