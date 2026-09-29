package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewMovementViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `it opens with the kind it was asked for and the account currency`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(MovementKind.OUT, state.kind)
        assertEquals(CurrencyCode.COP, state.currency)
        assertEquals(state.today, state.date)
        assertFalse(state.loading)
    }

    @Test
    fun `it only offers the categories of the kind`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        assertTrue(viewModel.state.value.offeredCategories.all { it.kind.accepts(MovementKind.OUT) })
        viewModel.onKind(MovementKind.IN)
        assertTrue(viewModel.state.value.offeredCategories.any { it.name == "Salario" })
    }

    @Test
    fun `changing the kind forgets the chosen category`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onCategory(SampleFinance.food.id)
        assertEquals(SampleFinance.food.id, viewModel.state.value.selectedCategoryId)
        viewModel.onKind(MovementKind.IN)
        assertNull(viewModel.state.value.selectedCategoryId)
    }

    @Test
    fun `saving needs an amount and a category`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        assertFalse(viewModel.state.value.canSave)
        viewModel.onAmount("186400")
        assertFalse(viewModel.state.value.canSave)
        viewModel.onCategory(SampleFinance.food.id)
        assertTrue(viewModel.state.value.canSave)
    }

    @Test
    fun `another currency asks for a rate and works out the equivalent itself`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onAmount("140")
        viewModel.onCurrency(CurrencyCode.USD)
        val suggestion = viewModel.state.value.suggestion
        assertTrue(suggestion.isForeign)
        // La tasa vigente, y la equivalencia que sale de multiplicarla por el importe.
        assertEquals(0, suggestion.suggestedRate?.compareTo(BigDecimal("3199.41")))
        assertEquals(0, suggestion.base.compareTo(BigDecimal("447917.4")))
        // Con la tasa del usuario la equivalencia se recalcula sola: ya no la teclea nadie.
        viewModel.onRate("4500")
        assertEquals(0, viewModel.state.value.suggestion.base.compareTo(BigDecimal("630000")))
    }

    @Test
    fun `changing the currency throws the edited rate away`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onAmount("140")
        viewModel.onCurrency(CurrencyCode.USD)
        viewModel.onRate("4500")
        viewModel.onCurrency(CurrencyCode.EUR)
        assertNull(viewModel.state.value.rateRaw)
    }

    @Test
    fun `saving announces it and lets the screen go back`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = filledViewModel(dependencies)
        viewModel.onSave()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saved)
        assertEquals(TransientMessages.MOVEMENT_SAVED_EXPENSE, dependencies.messages.message.value)
        assertEquals(1, dependencies.movementGateway.keys.size)
    }

    @Test
    fun `a replay does not announce the movement twice`() = runTest {
        val dependencies = TestMovementsDependencies(replay = true)
        val viewModel = filledViewModel(dependencies)
        viewModel.onSave()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.saved)
        assertNull(dependencies.messages.message.value)
    }

    @Test
    fun `a rejected body keeps the same key and shows the message of the api`() = runTest {
        val message = "El importe tiene que ser mayor que cero."
        val dependencies = TestMovementsDependencies(
            createFailure = ApiError.Validation(listOf(FieldError("amount", message))),
        )
        val viewModel = filledViewModel(dependencies)
        viewModel.onSave()
        advanceUntilIdle()
        viewModel.onCategory(SampleFinance.food.id)
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(message, viewModel.state.value.messageFor("amount"))
        assertEquals(1, dependencies.movementGateway.keys.distinct().size)
        assertEquals(2, dependencies.movementGateway.keys.size)
    }

    @Test
    fun `a reused key is thrown away so the next attempt gets a new one`() = runTest {
        val dependencies = TestMovementsDependencies(
            createFailure = ApiError.Conflict(ConflictCode.IDEMPOTENCY_KEY_REUSED),
        )
        val viewModel = filledViewModel(dependencies)
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(MovementsError.BODY_CHANGED, viewModel.state.value.error)
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(2, dependencies.movementGateway.keys.distinct().size)
    }

    @Test
    fun `a save already in flight keeps the key and says to wait`() = runTest {
        val dependencies = TestMovementsDependencies(
            createFailure = ApiError.Conflict(ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS),
        )
        val viewModel = filledViewModel(dependencies)
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(MovementsError.IN_PROGRESS, viewModel.state.value.error)
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals(1, dependencies.movementGateway.keys.distinct().size)
    }

    @Test
    fun `the currency sheet opens and closes`() = runTest {
        val viewModel = viewModelOf(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onCurrencySheet(true)
        viewModel.onCurrencyQuery("eur")
        assertTrue(viewModel.state.value.currencySheetOpen)
        assertEquals("eur", viewModel.state.value.currencyQuery)
        viewModel.onCurrencySheet(false)
        assertFalse(viewModel.state.value.currencySheetOpen)
    }

    @Test
    fun `the note travels with the movement`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = filledViewModel(dependencies)
        viewModel.onNote("Mercado de la semana")
        viewModel.onSave()
        advanceUntilIdle()
        assertEquals("Mercado de la semana", dependencies.movementGateway.lastDraft?.note)
    }

    private fun viewModelOf(dependencies: TestMovementsDependencies) =
        NewMovementViewModel(dependencies, MovementKind.OUT)

    /** Un formulario listo para guardar: importe y categoria puestos. */
    private suspend fun filledViewModel(
        dependencies: TestMovementsDependencies,
    ): NewMovementViewModel = viewModelOf(dependencies).also {
        kotlinx.coroutines.yield()
        it.onAmount("186400")
        it.onCategory(SampleFinance.food.id)
    }
}
