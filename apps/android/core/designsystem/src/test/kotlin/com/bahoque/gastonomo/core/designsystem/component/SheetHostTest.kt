package com.bahoque.gastonomo.core.designsystem.component

import android.content.Context
import android.provider.Settings
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo que hace el host de los sheets, que es lo que no se ve en una captura: que la hoja siga
 * pintada mientras baja, que arrastrarla la cierre y que un arrastre corto no.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class SheetHostTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun restoreAnimations() {
        setTransitionScale(1f)
    }

    @Test
    fun `it counts the sheet that is open, so the frame can dim the tab bar`() {
        val open = mutableStateOf(false)
        val sheets = OpenSheets()
        compose.setContent {
            GastonomoTheme {
                CompositionLocalProvider(LocalOpenSheets provides sheets) {
                    GastonomoSheetHost(visible = open.value) { Text("hoja") }
                }
            }
        }
        compose.waitForIdle()
        assertEquals(0, sheets.count)

        compose.runOnUiThread { open.value = true }
        compose.waitForIdle()
        assertEquals(1, sheets.count, "con la hoja abierta la tab bar tiene que ir velada")

        // Baja al cerrar, no al terminar la animacion: si no, el velo de la barra se quedaria
        // puesto un cuarto de segundo despues de que la hoja se haya ido.
        compose.runOnUiThread { open.value = false }
        compose.waitForIdle()
        assertEquals(0, sheets.count)
    }

    @Test
    fun `closing slides the sheet down instead of cutting it`() {
        val open = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            GastonomoTheme { Sheet(visible = open.value, onDismiss = {}) }
        }
        compose.mainClock.advanceTimeByFrame()
        val resting = compose.onNodeWithText(CONTENT).getBoundsInRoot().top

        compose.hide(open)
        compose.frames(MIDWAY_FRAMES)

        // Sigue pintada y mas abajo que antes: esta bajando, no se ha cortado.
        val leaving = compose.onNodeWithText(CONTENT).getBoundsInRoot().top
        assertTrue(leaving > resting, "la hoja no se movio: $leaving frente a $resting")
    }

    @Test
    fun `and once it is down it is gone`() {
        val open = mutableStateOf(true)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            GastonomoTheme { Sheet(visible = open.value, onDismiss = {}) }
        }
        compose.mainClock.advanceTimeByFrame()

        compose.hide(open)
        compose.frames(WELL_PAST_FRAMES)

        compose.onNodeWithText(CONTENT).assertDoesNotExist()
    }

    @Test
    fun `the content it was showing survives the value going away`() {
        val value = mutableStateOf<String?>(CONTENT)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            GastonomoTheme {
                GastonomoSheetHost(value.value) { text ->
                    GastonomoBottomSheet(onDismiss = {}) { GastonomoText(text) }
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()

        compose.close(value)
        compose.frames(MIDWAY_FRAMES)

        // Si no se retuviera, lo que bajaria seria una hoja vacia.
        compose.onNodeWithText(CONTENT).assertExists()
    }

    @Test
    fun `dragging the sheet down closes it`() {
        var dismissed = false
        compose.setContent {
            GastonomoTheme { Sheet(visible = true, onDismiss = { dismissed = true }) }
        }

        compose.onNodeWithText(CONTENT).performTouchInput {
            swipeDown(startY = 0f, endY = LONG_DRAG, durationMillis = QUICK)
        }

        assertTrue(dismissed)
    }

    @Test
    fun `a short drag leaves it open`() {
        var dismissed = false
        compose.setContent {
            GastonomoTheme { Sheet(visible = true, onDismiss = { dismissed = true }) }
        }

        compose.onNodeWithText(CONTENT).performTouchInput {
            swipeDown(startY = 0f, endY = SHORT_DRAG, durationMillis = SLOW)
        }

        assertFalse(dismissed)
        compose.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    @Test
    fun `with the animations off the sheet is just there`() {
        setTransitionScale(0f)
        compose.setContent {
            GastonomoTheme { Sheet(visible = true, onDismiss = {}) }
        }

        compose.onNodeWithText(CONTENT).assertIsDisplayed()
    }

    /**
     * Cierra la hoja y deja que la composicion se enganche a la animacion. Con el reloj en manual
     * hay que recomponer a mano: sin el `waitForIdle`, avanzar el reloj no llega a ver el cambio.
     */
    private fun <T> ComposeContentTestRule.close(state: MutableState<T?>) {
        runOnUiThread { state.value = null }
        waitForIdle()
    }

    private fun ComposeContentTestRule.hide(state: MutableState<Boolean>) {
        runOnUiThread { state.value = false }
        waitForIdle()
    }

    private fun ComposeContentTestRule.frames(count: Int) {
        repeat(count) { mainClock.advanceTimeByFrame() }
    }

    @Composable
    private fun Sheet(visible: Boolean, onDismiss: () -> Unit) {
        GastonomoSheetHost(visible = visible) {
            GastonomoBottomSheet(onDismiss = onDismiss) { GastonomoText(CONTENT) }
        }
    }

    private fun setTransitionScale(scale: Float) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        Settings.Global.putFloat(
            context.contentResolver,
            Settings.Global.TRANSITION_ANIMATION_SCALE,
            scale,
        )
    }

    private companion object {
        const val CONTENT = "Contenido de la hoja"

        /** Fotogramas suficientes para que la salida haya avanzado y no para que acabe. */
        const val MIDWAY_FRAMES = 4

        /** Mas fotogramas que cualquier duracion del sistema de diseno. */
        const val WELL_PAST_FRAMES = 60

        /** Pixeles: a xxhdpi son 200 dp, por encima del recorrido que cierra. */
        const val LONG_DRAG = 600f

        /** Pixeles: unos 20 dp, muy por debajo. */
        const val SHORT_DRAG = 60f

        const val QUICK = 100L
        const val SLOW = 400L
    }
}
