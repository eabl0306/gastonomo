package com.bahoque.gastonomo.feature.categories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.screenshot.captureGastonomoScreen
import com.bahoque.gastonomo.core.screenshot.screenshotFile
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

/** Las capturas de Categorias y de su sheet. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class CategoriesScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the categories screen renders to a png`() {
        compose.captureGastonomoScreen(SCREEN) {
            CategoriesScreen(state = state(), actions = CategoriesActions.NONE)
        }
        assertTrue(screenshotFile(SCREEN).exists())
    }

    @Test
    fun `the category sheet renders to a png`() {
        compose.captureGastonomoScreen(SHEET) {
            Box(modifier = Modifier.fillMaxSize()) {
                CategorySheet(
                    form = CategoryForm(name = "Viajes", icon = "flight"),
                    onFormChanged = {},
                    onSave = {},
                    onDismiss = {},
                )
            }
        }
        assertTrue(screenshotFile(SHEET).exists())
    }

    private fun state() = CategoriesUiState(
        loading = false,
        categories = FakeCategoriesDependencies.CATALOG,
    )

    private companion object {
        const val SCREEN = "categories"
        const val SHEET = "sheet-category"
    }
}
