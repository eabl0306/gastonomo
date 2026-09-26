package com.bahoque.gastonomo.feature.export

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.navigation.GastonomoRoute
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.renderNavGraph
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** El grafo de Exportar: la pantalla existe y se llega a ella. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class ExportNavGraphTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the exportar screen is reachable`() {
        compose.renderNavGraph(GastonomoRoute.Export) { actions ->
            exportNavGraph(FakeExportDependencies.working(Dispatchers.Unconfined), actions)
        }
        compose.onNodeWithText("Exportar datos").assertIsDisplayed()
        compose.onNodeWithText("Formato").assertIsDisplayed()
    }
}
