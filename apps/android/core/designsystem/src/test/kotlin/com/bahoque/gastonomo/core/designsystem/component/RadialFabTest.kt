package com.bahoque.gastonomo.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** El boton de registro rapido y su menu en arco. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class RadialFabTest {

    @get:Rule
    val compose = createComposeRule()

    private val chosen = mutableListOf<String>()

    private fun options() = listOf(
        RadialOption(GastonomoIcons.SWAP_VERT, "Nuevo movimiento") { chosen += "movimiento" },
        RadialOption(GastonomoIcons.HANDSHAKE, "Nuevo préstamo") { chosen += "prestamo" },
        RadialOption(GastonomoIcons.MIC, "Dictar") { chosen += "dictar" },
    )

    private fun render(name: String) {
        compose.captureGastonomoScreen(name) {
            Box(
                modifier = Modifier.fillMaxSize().background(GastonomoTheme.colors.bgApp),
            ) {
                GastonomoRadialFab(
                    options = options(),
                    closeLabel = "Cerrar",
                    openLabel = "Nuevo registro",
                )
            }
        }
    }

    @Test
    fun `cerrado ensena un solo boton`() {
        render("radial-fab-cerrado")

        compose.onNodeWithContentDescription("Nuevo registro").assertIsDisplayed()
    }

    /**
     * Un toque limpio deja el menu abierto: es lo que convierte el gesto de deslizar en un menu
     * de dos toques para quien no lo conoce.
     */
    @Test
    fun `un toque abre el menu y deja elegir con un segundo toque`() {
        render("radial-fab-cerrado-2")

        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        // La captura va DESPUES del toque: el arco solo existe con el menu abierto, y
        // capturar al montar dejaria una imagen del boton cerrado con nombre de abierto.
        compose.onRoot().captureRoboImage(screenshotFile("radial-fab-abierto").path)
        compose.onNodeWithContentDescription("Dictar").assertIsDisplayed()
        compose.onNodeWithContentDescription("Dictar").performClick()

        assertEquals(listOf("dictar"), chosen)
    }

    /** Las tres opciones estan, y cada una lleva a la suya. */
    @Test
    fun `cada opcion ejecuta la accion que le toca`() {
        render("radial-fab-opciones")

        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Nuevo préstamo").performClick()

        assertEquals(listOf("movimiento", "prestamo"), chosen)
    }
}
