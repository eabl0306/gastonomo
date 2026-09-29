package com.bahoque.gastonomo.core.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.SCREEN_HEIGHT
import com.bahoque.gastonomo.core.screenshot.captureGastonomoTabScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Las pestanas de la aplicacion y el armazon con el que se capturan.
 *
 * Existe por lo que paso: la aplicacion montaba su armazon y las capturas montaban otro, asi que
 * las imagenes ensenaban 78 dp de contenido que en el telefono tapa la tab bar. Ahora las dos
 * salen del mismo sitio, y esto lo vigila.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class TabItemsTest {

    @get:Rule
    val compose = createComposeRule()

    /** Una pestana, un destino, en el mismo orden y con su icono. */
    @Test
    fun `hay una pestana por destino principal, en orden`() {
        compose.setContent {
            assertEquals(
                TopLevelDestination.entries.map { it.icon },
                tabItems().map { it.icon },
            )
            assertEquals(TopLevelDestination.entries.size, tabItems().size)
        }
    }

    /**
     * La captura de una pantalla con pestanas trae la barra, y le quita alto al contenido.
     *
     * Sin esto se vuelve a lo de antes sin que nada se queje: una captura que da a la pantalla
     * los 844 dp enteros y ensena filas que el usuario no ve.
     */
    @Test
    fun `la captura con pestanas mete la barra y acorta el contenido`() {
        compose.captureGastonomoTabScreen("armazon-con-pestanas", TopLevelDestination.MOVEMENTS) {
            Box(modifier = Modifier.testTag(CONTENT).fillMaxSize())
        }

        for (label in listOf("Inicio", "Movimientos", "Préstamos", "Ajustes")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        // Sin numeros a mano: la barra ocupa sitio propio y queda FUERA del contenido, que es
        // justo lo que no pasaba cuando la captura le daba la pantalla entera a la pantalla.
        val content = compose.onNodeWithTag(CONTENT).getUnclippedBoundsInRoot()
        val tab = compose.onNodeWithText("Inicio").getUnclippedBoundsInRoot()
        assertTrue(content.bottom < SCREEN_HEIGHT.dp, "el contenido llega hasta ${content.bottom}")
        assertTrue(tab.top >= content.bottom, "la pestana empieza en ${tab.top}")
    }
}

private const val CONTENT = "contenido"
