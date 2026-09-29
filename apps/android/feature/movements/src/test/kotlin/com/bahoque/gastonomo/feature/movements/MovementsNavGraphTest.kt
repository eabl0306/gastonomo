package com.bahoque.gastonomo.feature.movements

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** Que las dos pantallas se montan de verdad con sus casos de uso y llevan a donde deben. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class MovementsNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the movimientos screen is reachable and loads its data`() {
        compose.renderNavGraph(GastonomoRoute.Movements) { actions ->
            movementsNavGraph(TestMovementsDependencies(), actions)
        }
        compose.onNodeWithText("Movimientos").assertIsDisplayed()
        compose.onNodeWithText("Mercado").assertIsDisplayed()
    }

    @Test
    fun `the quick add button opens the form as an expense`() {
        val recorded = mutableListOf<GastonomoRoute>()
        compose.renderNavGraph(GastonomoRoute.Movements, recorded) { actions ->
            movementsNavGraph(TestMovementsDependencies(), actions)
        }
        compose.onNodeWithContentDescription("Nuevo registro").performClick()
        compose.onNodeWithContentDescription("Nuevo movimiento").performClick()
        assertEquals(GastonomoRoute.NewMovement("out"), recorded.single())
        compose.onNodeWithText("Nuevo movimiento").assertIsDisplayed()
    }

    @Test
    fun `the nuevo movimiento screen is reachable with its kind`() {
        compose.renderNavGraph(GastonomoRoute.NewMovement("in")) { actions ->
            movementsNavGraph(TestMovementsDependencies(), actions)
        }
        compose.onNodeWithText("Nuevo movimiento").assertIsDisplayed()
        compose.onNodeWithText("Salario").assertIsDisplayed()
    }
}
