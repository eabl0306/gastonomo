package com.bahoque.gastonomo.feature.categories

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.screenshot.GASTONOMO_QUALIFIERS_ES
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

/** Lo que Categorias ensena, renderizado en la JVM. */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = GASTONOMO_QUALIFIERS_ES)
class CategoriesScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `it shows my categories and the default ones`() {
        setContent(loaded())
        compose.onNodeWithText("MIS CATEGORÍAS").assertIsDisplayed()
        compose.onNodeWithText("Mascotas").assertIsDisplayed()
        compose.onNodeWithText("PREDETERMINADAS").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a category with one movement is counted in singular`() {
        setContent(loaded())
        compose.onNodeWithText("1 movimiento").assertIsDisplayed()
    }

    @Test
    fun `a category with no movements says so`() {
        setContent(loaded())
        compose.onNodeWithText("Sin movimientos").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `the default rows cannot be edited`() {
        setContent(loaded())
        compose.onAllNodesWithContentDescription("No se puede editar")
            .assertCountEquals(loaded().defaults.size)
    }

    @Test
    fun `the edit button opens the sheet for that category`() {
        val opened = mutableListOf<Category>()
        setContent(loaded(), CategoriesActions({}, {}, { it?.let(opened::add) }, {}, {}))
        compose.onNodeWithContentDescription("Editar").performClick()
        assertEquals(listOf(SampleFinance.pets), opened)
    }

    @Test
    fun `the delete button reports the category`() {
        val deleted = mutableListOf<Category>()
        setContent(loaded(), CategoriesActions({}, {}, {}, { deleted += it }, {}))
        compose.onNodeWithContentDescription("Eliminar").performClick()
        assertEquals(listOf(SampleFinance.pets), deleted)
    }

    @Test
    fun `a failed load offers to try again`() {
        var retried = 0
        setContent(
            loaded().copy(loadFailed = true),
            CategoriesActions({}, {}, {}, {}, { retried++ }),
        )
        compose.onNodeWithText("No se pudieron cargar las categorías.").assertIsDisplayed()
        compose.onNodeWithText("Reintentar").performClick()
        assertEquals(1, retried)
    }

    @Test
    fun `the income tab shows the income categories`() {
        setContent(loaded().copy(tab = MovementKind.IN))
        compose.onNodeWithText("Salario").assertIsDisplayed()
    }

    private fun setContent(
        state: CategoriesUiState,
        actions: CategoriesActions = CategoriesActions.NONE,
    ) {
        compose.setContent {
            GastonomoTheme { CategoriesScreen(state = state, actions = actions) }
        }
    }

    private fun loaded() = CategoriesUiState(
        loading = false,
        categories = FakeCategoriesDependencies.CATALOG,
    )
}
