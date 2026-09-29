package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/**
 * El chip de filtro activo va en ink, y el inactivo en blanco. No lo comprobaba nada, y el
 * fondo es justo lo que se rompio.
 *
 * **Lo que este test NO cubre, y conviene saberlo.** El fallo de verdad solo aparecia al
 * seleccionar el chip **con el dedo apoyado**: mientras el dedo esta abajo corre la animacion
 * de pulsado, el chip se recomponia en cada fotograma y el fondo se quedaba con el color de
 * antes, texto blanco sobre blanco. El toque sostenido esta reproducido aqui abajo, pero
 * Robolectric pinta igual con el fallo puesto y sin el: se comprobo poniendo el codigo viejo,
 * y este test seguia verde. Lo que se rompia vive en las capas de dibujo del dispositivo, no
 * en el render de la JVM.
 *
 * Asi que esto vale como lo que es, una comprobacion del estilo del chip. El arreglo se
 * verifico en un telefono, pulsacion sostenida de 250 a 1500 ms, en Movimientos y en
 * Prestamos. Si algun dia hay emulador en la CI, este es el test que hay que volver a mirar.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class FilterPillTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the selected chip is dark and the one next to it is not`() {
        var selected by mutableStateOf(false)
        compose.setContent {
            GastonomoTheme {
                Box(
                    modifier = Modifier
                        .background(GastonomoTheme.colors.bgApp)
                        .padding(PADDING),
                ) {
                    FilterPill(label = LABEL, selected = selected, onClick = { selected = true })
                }
            }
        }

        assertTrue(backgroundOfPill() > LIGHT, "sin seleccionar tiene que ir en claro")

        holdAndRelease()

        assertTrue(backgroundOfPill() < DARK, "seleccionado tiene que ir en ink")
    }

    /**
     * Un punto del interior a media altura, lejos del texto: ahi solo puede haber fondo.
     */
    private fun backgroundOfPill(): Float {
        val pill = compose.onNodeWithText(LABEL).captureToImage().toPixelMap()
        return pill[pill.width / 12, pill.height / 2].luminance()
    }

    /** Un toque con el dedo apoyado: baja, se queda un rato, y sube. */
    private fun holdAndRelease() {
        compose.mainClock.autoAdvance = false
        compose.onNodeWithText(LABEL).performTouchInput { down(center) }
        compose.mainClock.advanceTimeBy(HELD_MILLIS)
        compose.onNodeWithText(LABEL).performTouchInput { up() }
        compose.mainClock.advanceTimeBy(SETTLE_MILLIS)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private companion object {
        const val LABEL = "Me deben"
        const val HELD_MILLIS = 300L
        const val SETTLE_MILLIS = 2000L

        /** Ink queda muy por debajo; surface, muy por encima. */
        const val DARK = 0.2f
        const val LIGHT = 0.8f

        val PADDING = 8.dp
    }
}
