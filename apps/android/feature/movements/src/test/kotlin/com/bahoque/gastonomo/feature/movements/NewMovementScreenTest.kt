package com.bahoque.gastonomo.feature.movements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** El formulario de Nuevo movimiento. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class NewMovementScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it renders the whole form to a png`() {
        compose.captureGastonomoScreen(SCREENSHOT) {
            NewMovementScreen(MovementsSamples.filledForm, NewMovementActions.NONE)
        }
        val file = screenshotFile(SCREENSHOT)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        compose.onNodeWithText("Nuevo movimiento").assertIsDisplayed()
        compose.onNodeWithText("186.400").assertIsDisplayed()
        compose.onNodeWithText("Categoría").assertIsDisplayed()
        compose.onNodeWithText("Guardar movimiento").assertIsDisplayed()
    }

    @Test
    fun `an empty form only offers the expense categories`() {
        compose.captureGastonomoScreen("new-movement-empty") {
            NewMovementScreen(MovementsSamples.form, NewMovementActions.NONE)
        }
        compose.onNodeWithText("Alimentación").assertIsDisplayed()
        compose.onNodeWithText("Servicios").assertIsDisplayed()
        compose.onNodeWithText("Ej. Mercado de la semana").assertIsDisplayed()
    }

    @Test
    fun `switching to income asks for the other kind`() {
        val kinds = mutableListOf<MovementKind>()
        compose.captureGastonomoScreen("new-movement-kind") {
            NewMovementScreen(
                state = MovementsSamples.form,
                actions = NewMovementActions.NONE.copy(onKind = { kinds += it }),
            )
        }
        compose.onNode(hasText("Ingreso") and hasClickAction()).performClick()
        assertEquals(MovementKind.IN, kinds.single())
    }

    @Test
    fun `another currency shows the conversion box`() {
        compose.captureGastonomoScreen("new-movement-foreign") {
            NewMovementScreen(MovementsSamples.foreignForm, NewMovementActions.NONE)
        }
        compose.onNodeWithText("Equivale a (COP)").assertIsDisplayed()
        compose.onNodeWithText("447.917").assertIsDisplayed()
    }

    @Test
    fun `the chip opens the currency sheet`() {
        var opened = false
        compose.captureGastonomoScreen("new-movement-chip") {
            NewMovementScreen(
                state = MovementsSamples.form,
                actions = NewMovementActions.NONE.copy(onCurrencySheet = { opened = it }),
            )
        }
        compose.onNodeWithText("COP · Peso colombiano").performClick()
        assertTrue(opened)
    }

    @Test
    fun `the currency sheet lists what the api sent`() {
        compose.captureGastonomoScreen("new-movement-currency-sheet") {
            NewMovementScreen(
                state = MovementsSamples.form.copy(currencySheetOpen = true),
                actions = NewMovementActions.NONE,
            )
        }
        compose.onNodeWithText("Moneda del registro").assertIsDisplayed()
        compose.onNodeWithText("USD").assertIsDisplayed()
    }

    @Test
    fun `a rejected amount shows the message of the api next to the button`() {
        compose.captureGastonomoScreen("new-movement-error") {
            NewMovementScreen(
                state = MovementsSamples.filledForm.copy(
                    fieldErrors = listOf(FieldError("amount", "El importe tiene que ser mayor que cero.")),
                ),
                actions = NewMovementActions.NONE,
            )
        }
        compose.onNodeWithText("El importe tiene que ser mayor que cero.").assertIsDisplayed()
    }

    @Test
    fun `saving is asked for once`() {
        var saved = 0
        compose.captureGastonomoScreen("new-movement-save") {
            NewMovementScreen(
                state = MovementsSamples.filledForm,
                actions = NewMovementActions.NONE.copy(onSave = { saved++ }),
            )
        }
        compose.onNodeWithText("Guardar movimiento").performClick()
        assertEquals(1, saved)
    }

    @Test
    fun `the close button leaves the form`() {
        var closed = 0
        compose.captureGastonomoScreen("new-movement-close") {
            NewMovementScreen(
                state = MovementsSamples.form,
                actions = NewMovementActions.NONE.copy(onClose = { closed++ }),
            )
        }
        compose.onNodeWithContentDescription("Cerrar").performClick()
        assertEquals(1, closed)
    }

    @Test
    fun `the chosen category is the one in the state`() {
        compose.captureGastonomoScreen("new-movement-category") {
            NewMovementScreen(
                state = MovementsSamples.form.copy(selectedCategoryId = SampleFinance.food.id),
                actions = NewMovementActions.NONE,
            )
        }
        compose.onNodeWithText("Alimentación").assertIsDisplayed()
    }

    private companion object {
        const val SCREENSHOT = "new-movement"
    }
}
