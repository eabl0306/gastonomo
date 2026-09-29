package com.bahoque.gastonomo.feature.movements

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.state.RangeDraft
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.navigation.TopLevelDestination
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoTabScreen
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
        compose.captureGastonomoTabScreen(SCREENSHOT, TopLevelDestination.MOVEMENTS) {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        val file = screenshotFile(SCREENSHOT)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        // Dos veces: el titulo de la pantalla y la etiqueta de su pestana. Con la captura
        // dentro del armazon, la barra entra en el arbol y "Movimientos" deja de ser unico.
        compose.onAllNodesWithText("Movimientos").assertCountEquals(2)
        compose.onNodeWithText("$ 3.250.000").assertIsDisplayed()
        compose.onNodeWithText("$ 328.400").assertIsDisplayed()
        compose.onNodeWithText("+$ 2.921.600").assertIsDisplayed()
    }

    @Test
    fun `the days are grouped and named`() {
        compose.captureGastonomoTabScreen("movements-groups", TopLevelDestination.MOVEMENTS) {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        compose.onNodeWithText("Martes 15 sept").assertIsDisplayed()
        compose.onNodeWithText("Mercado").assertIsDisplayed()
        compose.onNodeWithText("Alimentación").assertIsDisplayed()
    }

    @Test
    fun `a movement in another currency shows what was typed`() {
        compose.captureGastonomoTabScreen("movements-foreign", TopLevelDestination.MOVEMENTS) {
            MovementsScreen(MovementsSamples.list, MovementsActions.NONE)
        }
        compose.onNodeWithText("Freelance · US$ 140,00").assertIsDisplayed()
    }

    @Test
    fun `the pills ask for a filter`() {
        val chosen = mutableListOf<MovementFilter>()
        compose.captureGastonomoTabScreen("movements-pills", TopLevelDestination.MOVEMENTS) {
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
        compose.captureGastonomoTabScreen("movements-empty", TopLevelDestination.MOVEMENTS) {
            MovementsScreen(MovementsSamples.emptyList, MovementsActions.NONE)
        }
        compose.onNodeWithText("No hay movimientos en este periodo.").assertIsDisplayed()
    }

    @Test
    fun `an error offers to try again`() {
        var retried = 0
        compose.captureGastonomoTabScreen("movements-error", TopLevelDestination.MOVEMENTS) {
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
    fun `the quick add button offers the three ways of registering`() {
        var movements = 0
        var loans = 0
        var dictations = 0
        compose.captureGastonomoTabScreen("movements-fab", TopLevelDestination.MOVEMENTS) {
            MovementsScreen(
                state = MovementsSamples.list,
                actions = MovementsActions.NONE.copy(
                    onNewMovement = { movements++ },
                    onNewLoan = { loans++ },
                    onDictate = { dictations++ },
                ),
            )
        }
        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Dictar").performClick()
        assertEquals(1, movements)
        assertEquals(0, loans)
        assertEquals(1, dictations)
    }

    @Test
    fun `the toast of a saved movement is shown over the list`() {
        compose.captureGastonomoTabScreen("movements-toast", TopLevelDestination.MOVEMENTS) {
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
        compose.captureGastonomoTabScreen("movements-range-sheet", TopLevelDestination.MOVEMENTS) {
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
