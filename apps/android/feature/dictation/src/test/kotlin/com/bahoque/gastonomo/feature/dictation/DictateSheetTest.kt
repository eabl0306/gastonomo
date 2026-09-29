package com.bahoque.gastonomo.feature.dictation

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Las cuatro fases de la hoja de dictado. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class DictateSheetTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `listening renders the microphone and what it heard`() {
        compose.captureGastonomoScreen(LISTENING) {
            DictateSheet(DictationSamples.listening, DictateActions.NONE)
        }
        val file = screenshotFile(LISTENING)
        assertTrue(file.exists(), "Roborazzi did not write ${file.absolutePath}")
        compose.onNodeWithText("Dictar registro").assertIsDisplayed()
        compose.onNodeWithText("Almorcé por 45 mil").assertIsDisplayed()
        compose.onNodeWithText("Toca para terminar o mantén presionado mientras hablas")
            .assertIsDisplayed()
    }

    @Test
    fun `with nothing heard yet it says it is listening`() {
        compose.captureGastonomoScreen("dictation-hearing") {
            DictateSheet(DictationSamples.hearing, DictateActions.NONE)
        }
        compose.onNodeWithText("Te escucho…").assertIsDisplayed()
    }

    @Test
    fun `tapping the microphone asks to finish, it does not cancel`() {
        var finished = 0
        compose.captureGastonomoScreen("dictation-stop") {
            DictateSheet(
                state = DictationSamples.listening,
                actions = DictateActions.NONE.copy(onStopListening = { finished++ }),
            )
        }
        compose.onNodeWithContentDescription("Terminar de dictar").performClick()
        assertEquals(1, finished)
    }

    @Test
    fun `without a microphone it explains why and offers the field`() {
        compose.captureGastonomoScreen("dictation-typing") {
            DictateSheet(DictationSamples.typing, DictateActions.NONE)
        }
        compose.onNodeWithText(
            "Este dispositivo no permite dictado por voz. " +
                "Escribe el registro y lo interpretamos igual.",
        ).assertIsDisplayed()
        compose.onNodeWithText("Interpretar").assertIsDisplayed()
        compose.onNodeWithText("Prueba con").assertIsDisplayed()
    }

    @Test
    fun `interpreting shows what was said`() {
        compose.captureGastonomoScreen("dictation-processing") {
            DictateSheet(DictationSamples.processing, DictateActions.NONE)
        }
        compose.onNodeWithText("Interpretando…").assertIsDisplayed()
        compose.onNodeWithText("“Almorcé por 45 mil”").assertIsDisplayed()
    }

    @Test
    fun `the review card of an expense renders with its amount and category`() {
        compose.captureGastonomoScreen(REVIEW) {
            DictateSheet(DictationSamples.review, DictateActions.NONE)
        }
        assertTrue(screenshotFile(REVIEW).exists())
        compose.onNodeWithText("Revisa el registro").assertIsDisplayed()
        compose.onNodeWithText("Gasto").assertIsDisplayed()
        compose.onNodeWithText("$ 45.000").assertIsDisplayed()
        compose.onNodeWithText("Alimentación").assertIsDisplayed()
        compose.onNodeWithText("28 sept 2026").assertIsDisplayed()
        compose.onNodeWithText("Guardar").assertIsDisplayed()
        compose.onNodeWithText("Editar").assertIsDisplayed()
        compose.onNodeWithText("Dictar de nuevo").assertIsDisplayed()
    }

    @Test
    fun `editing asks for the full form, it does not save anything`() {
        var edits = 0
        var saves = 0
        compose.captureGastonomoScreen("dictation-edit") {
            DictateSheet(
                state = DictationSamples.review,
                actions = DictateActions.NONE.copy(onEdit = { edits++ }, onSave = { saves++ }),
            )
        }
        compose.onNodeWithText("Editar").performClick()
        assertEquals(1, edits)
        assertEquals(0, saves)
    }

    @Test
    fun `a dictated loan says which way it goes and with whom`() {
        compose.captureGastonomoScreen("dictation-review-loan") {
            DictateSheet(DictationSamples.reviewLoan, DictateActions.NONE)
        }
        compose.onNodeWithText("Préstamo: te deben").assertIsDisplayed()
        compose.onNodeWithText("Rosa").assertIsDisplayed()
    }

    @Test
    fun `another currency shows the amount as dictated and its equivalent below`() {
        compose.captureGastonomoScreen("dictation-review-foreign") {
            DictateSheet(DictationSamples.reviewForeign, DictateActions.NONE)
        }
        compose.onNodeWithText("US$ 140,00").assertIsDisplayed()
        compose.onNodeWithText("$ 447.917").assertIsDisplayed()
    }

    @Test
    fun `without an amount it says how to fix it`() {
        compose.captureGastonomoScreen("dictation-review-no-amount") {
            DictateSheet(DictationSamples.reviewNoAmount, DictateActions.NONE)
        }
        compose.onNodeWithText("Sin importe").assertIsDisplayed()
        compose.onNodeWithText(
            "No entendimos el importe. Dicta de nuevo diciéndolo completo.",
        ).assertIsDisplayed()
    }

    @Test
    fun `without a rate it says to dictate in the main currency`() {
        compose.captureGastonomoScreen("dictation-review-no-rate") {
            DictateSheet(DictationSamples.reviewNoRate, DictateActions.NONE)
        }
        compose.onNodeWithText(
            "No tenemos la equivalencia de esta moneda ahora mismo. " +
                "Dicta el importe en tu moneda principal.",
        ).assertIsDisplayed()
    }

    private companion object {
        const val LISTENING = "dictation-listening"
        const val REVIEW = "dictation-review"
    }
}
