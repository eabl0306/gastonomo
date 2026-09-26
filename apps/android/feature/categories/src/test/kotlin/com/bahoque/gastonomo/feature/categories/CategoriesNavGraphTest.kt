package com.bahoque.gastonomo.feature.categories

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** El grafo de Categorias: la pantalla existe y se llega a ella. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class CategoriesNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the categorias screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.Categories) { actions ->
            categoriesNavGraph(FakeCategoriesDependencies.working(), actions)
        }
        compose.onNodeWithText("Categorías").assertIsDisplayed()
        compose.onNodeWithText("Nueva categoría").assertIsDisplayed()
    }
}
