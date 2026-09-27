package com.bahoque.gastonomo.feature.movements

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MovementsViewModelTest {

    @Test
    fun `the loading block only shows before the first load, so the month does not blink`() = runTest {
        val viewModel = MovementsViewModel(TestMovementsDependencies())
        assertFalse(viewModel.state.value.everLoaded, "la primera vez si hay bloque de carga")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.everLoaded)

        viewModel.period.previousMonth()
        runCurrent()

        // Aqui estaba el parpadeo: la condicion de antes miraba si la lista estaba vacia, asi
        // que en una cuenta vacia cada recarga cambiaba la pantalla por el bloque de carga.
        assertTrue(viewModel.state.value.everLoaded, "recargar no devuelve el bloque de carga")
        advanceUntilIdle()
        assertTrue(viewModel.state.value.everLoaded)
    }

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `it groups the period by day, newest first`() = runTest {
        val viewModel = MovementsViewModel(TestMovementsDependencies())
        advanceUntilIdle()
        val groups = viewModel.state.value.groups
        assertEquals(4, groups.size)
        assertEquals(LocalDate.of(2026, 9, 15), groups.first().date)
        assertEquals(LocalDate.of(2026, 9, 1), groups.last().date)
    }

    @Test
    fun `the summary is the whole period, not the filtered list`() = runTest {
        val viewModel = MovementsViewModel(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onFilter(MovementFilter.EXPENSE)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(BigDecimal("3250000"), state.summary.income.amount)
        assertEquals(BigDecimal("328400"), state.summary.expense.amount)
        assertEquals(2, state.groups.size)
    }

    @Test
    fun `the pills filter what is shown and nothing else`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = MovementsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onFilter(MovementFilter.INCOME)
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.groups.size)
        viewModel.onFilter(MovementFilter.ALL)
        advanceUntilIdle()
        assertEquals(4, viewModel.state.value.groups.size)
    }

    @Test
    fun `the day net adds up its own movements`() = runTest {
        val viewModel = MovementsViewModel(TestMovementsDependencies())
        advanceUntilIdle()
        val salaryDay = viewModel.state.value.groups.last()
        assertEquals(BigDecimal("2800000"), salaryDay.net.amount)
    }

    @Test
    fun `the period is shared with home`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = MovementsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.period.openRangeSheet()
        viewModel.period.selectPreset(PeriodPreset.LAST_MONTH)
        viewModel.period.applyRange()
        advanceUntilIdle()
        assertTrue(dependencies.periodFilter.selection.value.isRange)
        assertTrue(viewModel.state.value.selection.isRange)
    }

    @Test
    fun `a network failure leaves an error and stops loading`() = runTest {
        val viewModel = MovementsViewModel(
            TestMovementsDependencies(movementsFailure = ApiError.Network),
        )
        advanceUntilIdle()
        assertEquals(MovementsError.NETWORK, viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
        assertTrue(viewModel.state.value.groups.isEmpty())
    }

    @Test
    fun `retrying asks again`() = runTest {
        val viewModel = MovementsViewModel(TestMovementsDependencies())
        advanceUntilIdle()
        viewModel.onRetry()
        advanceUntilIdle()
        assertNull(viewModel.state.value.error)
    }

    @Test
    fun `rates that are not ready yet only add a warning`() = runTest {
        val viewModel = MovementsViewModel(
            TestMovementsDependencies(ratesFailure = ApiError.ServiceUnavailable),
        )
        advanceUntilIdle()
        assertTrue(viewModel.state.value.ratesUnavailable)
        assertNull(viewModel.state.value.error)
        assertEquals(4, viewModel.state.value.groups.size)
    }

    @Test
    fun `the toast left by the form is shown once`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = MovementsViewModel(dependencies)
        advanceUntilIdle()
        dependencies.messages.post(TransientMessages.MOVEMENT_SAVED_INCOME)
        advanceUntilIdle()
        assertEquals(TransientMessages.MOVEMENT_SAVED_INCOME, viewModel.state.value.toast)
        viewModel.onToastHidden()
        advanceUntilIdle()
        assertNull(viewModel.state.value.toast)
    }

    @Test
    fun `the first time the screen shows there is a single load`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = MovementsViewModel(dependencies)

        viewModel.onResumed()
        advanceUntilIdle()

        assertEquals(1, dependencies.listCalls)
    }

    @Test
    fun `coming back to the screen loads again, so what was just saved shows up`() = runTest {
        val dependencies = TestMovementsDependencies()
        val viewModel = MovementsViewModel(dependencies)
        viewModel.onResumed()
        advanceUntilIdle()

        // El formulario guardo y volvio atras: esta entrada de la pila nunca se destruyo.
        viewModel.onResumed()
        advanceUntilIdle()

        assertEquals(2, dependencies.listCalls)
    }
}
