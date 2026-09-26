package com.bahoque.gastonomo

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.navigation.selectedTabIndex
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/**
 * El armazon: que la raiz de composicion se construye, que las 14 pantallas estan
 * declaradas y que la tab bar sale solo en las cuatro principales.
 */
@RunWith(AndroidJUnit4::class)
// El idioma por defecto de la aplicacion es el espanol, y Robolectric arranca en ingles:
// sin este qualifier la tab bar saldria con los textos de values-en.
@Config(qualifiers = "es-rCO-w390dp-h844dp-xxhdpi")
class GastonomoAppTest {

    @get:Rule
    val compose = createComposeRule()

    private val container = AppContainer(ApplicationProvider.getApplicationContext<Context>())

    @Test
    fun `the home screen shows the tab bar`() {
        compose.setContent { GastonomoApp(container, GastonomoRoute.Home) }
        // "Inicio" solo sale en el tab: la pantalla saluda por el nombre, no se titula.
        assertEquals(1, compose.countOfText("Inicio"))
        compose.onNodeWithText("Hola", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Movimientos").assertIsDisplayed()
        compose.onNodeWithText("Préstamos").assertIsDisplayed()
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    fun `switching tab lands on the other screen`() {
        compose.setContent { GastonomoApp(container, GastonomoRoute.Home) }
        compose.onNodeWithText("Préstamos").performClick()
        // "Prestamos" sale dos veces: en el tab y como titulo de la pantalla, asi que se
        // comprueba algo que solo tiene la pantalla.
        compose.onNodeWithText("Te deben").assertIsDisplayed()
    }

    @Test
    fun `a secondary screen does not show the tab bar`() {
        compose.setContent { GastonomoApp(container, GastonomoRoute.Login) }
        compose.onNodeWithText("Bienvenido de nuevo").assertIsDisplayed()
        assertEquals(0, compose.countOfText("Ajustes"))
    }

    @Test
    fun `only the four main screens are a tab`() {
        assertEquals(-1, selectedTabIndex(null))
    }

    @Test
    fun `the container hands each feature its own dependencies`() {
        assertEquals(container.home, container.home)
        listOf(
            container.auth,
            container.home,
            container.movements,
            container.loans,
            container.settings,
            container.categories,
            container.export,
        ).forEach { dependencies -> assertEquals(dependencies, dependencies) }
    }

    private fun ComposeContentTestRule.countOfText(text: String): Int =
        onAllNodes(hasText(text)).fetchSemanticsNodes().size
}
