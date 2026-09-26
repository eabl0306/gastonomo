package com.bahoque.gastonomo.feature.categories

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El estado de Categorias, con dobles de los tres casos de uso. */
@OptIn(ExperimentalCoroutinesApi::class)
class CategoriesViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it splits the list into mine and defaults for the active tab`() = runTest {
        val viewModel = loaded()
        val state = viewModel.state.value
        assertEquals(listOf("Mascotas"), state.mine.map { it.name })
        assertTrue(state.defaults.any { it.name == "Alimentación" })
        assertTrue(state.defaults.none { it.name == "Salario" })
    }

    @Test
    fun `a both category shows up in the two tabs`() = runTest {
        val viewModel = loaded()
        assertTrue(viewModel.state.value.defaults.any { it.name == "Otros" })
        viewModel.onTabSelected(MovementKind.IN)
        assertTrue(viewModel.state.value.defaults.any { it.name == "Otros" })
    }

    @Test
    fun `each tab counts what it can show`() = runTest {
        val viewModel = loaded()
        val state = viewModel.state.value
        assertEquals(state.categories.count { it.kind.accepts(MovementKind.OUT) }, state.countOf(MovementKind.OUT))
        assertEquals(state.categories.count { it.kind.accepts(MovementKind.IN) }, state.countOf(MovementKind.IN))
    }

    @Test
    fun `a new category opens the sheet with the type of the active tab`() = runTest {
        val viewModel = loaded()
        viewModel.onTabSelected(MovementKind.IN)
        viewModel.onOpenSheet(null)
        val form = requireNotNull(viewModel.state.value.form)
        assertEquals(MovementKind.IN, form.kind)
        assertTrue(!form.isEditing)
    }

    @Test
    fun `editing one loads its name, icon and colour`() = runTest {
        val viewModel = loaded()
        viewModel.onOpenSheet(SampleFinance.pets)
        val form = requireNotNull(viewModel.state.value.form)
        assertEquals("Mascotas", form.name)
        assertEquals("pets", form.icon)
        assertTrue(form.isEditing)
    }

    @Test
    fun `a name that already exists cannot be saved`() = runTest {
        val viewModel = loaded()
        viewModel.onOpenSheet(null)
        viewModel.onFormChanged(requireNotNull(viewModel.state.value.form).copy(name = " alimentación "))
        val form = requireNotNull(viewModel.state.value.form)
        assertTrue(form.duplicate)
        assertTrue(!form.canSave)
    }

    @Test
    fun `renaming a category to its own name is not a duplicate`() = runTest {
        val viewModel = loaded()
        viewModel.onOpenSheet(SampleFinance.pets)
        viewModel.onFormChanged(requireNotNull(viewModel.state.value.form).copy(name = "Mascotas"))
        assertTrue(!requireNotNull(viewModel.state.value.form).duplicate)
    }

    @Test
    fun `the name is cut at the maximum the api accepts`() = runTest {
        val viewModel = loaded()
        viewModel.onOpenSheet(null)
        val long = "x".repeat(40)
        viewModel.onFormChanged(requireNotNull(viewModel.state.value.form).copy(name = long))
        assertEquals(
            Category.MAX_NAME_LENGTH,
            requireNotNull(viewModel.state.value.form).name.length,
        )
    }

    @Test
    fun `saving closes the sheet, jumps to its tab and announces it`() = runTest {
        val dependencies = FakeCategoriesDependencies.working()
        val viewModel = loaded(dependencies)
        viewModel.onOpenSheet(null)
        viewModel.onFormChanged(
            requireNotNull(viewModel.state.value.form).copy(name = "Viajes", kind = MovementKind.IN),
        )
        viewModel.onSave()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertNull(state.form)
        assertEquals(MovementKind.IN, state.tab)
        assertEquals(CategoriesMessage.Created("Viajes"), state.message)
        assertEquals("Viajes", dependencies.gateway.lastDraft?.name)
    }

    @Test
    fun `a 409 for a taken name is marked on the form and not on a toast`() = runTest {
        val viewModel = loaded(FakeCategoriesDependencies.nameTaken())
        viewModel.onOpenSheet(null)
        viewModel.onFormChanged(requireNotNull(viewModel.state.value.form).copy(name = "Viajes"))
        viewModel.onSave()
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(requireNotNull(state.form).duplicate)
        assertNull(state.message)
    }

    @Test
    fun `a 409 for a default category is told on a toast`() = runTest {
        val viewModel = loaded(FakeCategoriesDependencies.isDefault())
        viewModel.onOpenSheet(null)
        viewModel.onFormChanged(requireNotNull(viewModel.state.value.form).copy(name = "Viajes"))
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(CategoriesMessage.IsDefault, viewModel.state.value.message)
    }

    @Test
    fun `deleting one announces it and reloads`() = runTest {
        val dependencies = FakeCategoriesDependencies.working()
        val viewModel = loaded(dependencies)
        viewModel.onDelete(SampleFinance.pets)
        advanceUntilIdle()
        assertEquals(CategoriesMessage.Deleted("Mascotas"), viewModel.state.value.message)
        assertEquals(listOf(SampleFinance.pets.id), dependencies.gateway.deleted)
    }

    @Test
    fun `a failed load offers to try again`() = runTest {
        val viewModel = loaded(FakeCategoriesDependencies.offline())
        assertTrue(viewModel.state.value.loadFailed)
        assertTrue(!viewModel.state.value.loading)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.loaded(
        dependencies: FakeCategoriesDependencies = FakeCategoriesDependencies.working(),
    ): CategoriesViewModel {
        val viewModel = CategoriesViewModel(dependencies)
        advanceUntilIdle()
        return viewModel
    }
}
