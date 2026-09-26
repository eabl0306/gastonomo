package com.bahoque.gastonomo.feature.home

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

/** Que Inicio se monta de verdad con sus casos de uso y lleva a donde tiene que llevar. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class HomeNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the inicio screen is reachable and loads its data`() {
        compose.renderNavGraph(GastonomoRoute.Home) { actions ->
            homeNavGraph(TestHomeDependencies(), actions)
        }
        compose.onNodeWithText("Hola, Camilo").assertIsDisplayed()
        compose.onNodeWithText("Últimos movimientos").assertIsDisplayed()
    }

    @Test
    fun `the expense quick action opens the form as an expense`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Home, recorded) { actions ->
            homeNavGraph(TestHomeDependencies(), actions)
            // El destino es de otra feature, que este modulo no puede ver: aqui basta con
            // que exista para que la navegacion se pueda comprobar.
            composable<GastonomoRoute.NewMovement> { }
        }
        compose.onNodeWithText("Gasto").performClick()
        assertEquals(GastonomoRoute.NewMovement("out"), recorded.single())
    }

    @Test
    fun `see all switches to the movements tab`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Home, recorded) { actions ->
            homeNavGraph(TestHomeDependencies(), actions)
        }
        // Esta abajo del todo: hay que llevarla a la vista antes de poder tocarla.
        compose.onNodeWithText("Ver todos").performScrollTo().performClick()
        assertEquals(GastonomoRoute.Movements, recorded.single())
    }
}
