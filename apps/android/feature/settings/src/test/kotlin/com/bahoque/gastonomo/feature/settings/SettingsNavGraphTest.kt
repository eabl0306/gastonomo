package com.bahoque.gastonomo.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.compose.composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** El grafo de Ajustes: se llega a la pantalla y sus filas navegan a donde deben. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class SettingsNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the ajustes screen is reachable`() {
        render()
        compose.onNodeWithText("Ajustes").assertIsDisplayed()
    }

    @Test
    fun `the categories row opens categories`() {
        val recorded = render()
        compose.onNodeWithText("Categorías").performScrollTo().performClick()
        assertEquals(listOf(GastonomoRoute.Categories), recorded)
    }

    @Test
    fun `the export row opens the export screen`() {
        val recorded = render()
        compose.onNodeWithText("Exportar datos").performScrollTo().performClick()
        assertEquals(listOf(GastonomoRoute.Export), recorded)
    }

    @Test
    fun `signing out replaces the stack with the login screen`() {
        val recorded = render()
        compose.onNodeWithText("Cerrar sesión").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(listOf(GastonomoRoute.Login), recorded)
    }

    private fun render(): List<GastonomoRoute> {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Settings, recorded) { actions ->
            settingsNavGraph(FakeSettingsDependencies.working(), actions)
            // El arnes navega de verdad, asi que las pantallas de las otras features
            // tienen que existir en el grafo aunque aqui no pinten nada.
            composable<GastonomoRoute.Categories> {}
            composable<GastonomoRoute.Export> {}
        }
        return recorded
    }
}
