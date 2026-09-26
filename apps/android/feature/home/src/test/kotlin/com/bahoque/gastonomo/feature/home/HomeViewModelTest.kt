package com.bahoque.gastonomo.feature.home

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.state.TransientMessages
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import java.math.BigDecimal
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeViewModelTest {

    @get:Rule
    val dispatcher = MainDispatcherRule()

    @Test
    fun `it loads the period and fills the balance card`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(BigDecimal("3250000"), state.content.summary.income.amount)
        assertEquals(BigDecimal("328400"), state.content.summary.expense.amount)
        assertEquals(BigDecimal("2921600"), state.content.summary.net.amount)
    }

    @Test
    fun `the trend has one point per day up to today`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        val points = viewModel.state.value.content.trend.points
        assertEquals(22, points.size)
        assertEquals(BigDecimal("2921600"), points.last().cumulative)
    }

    @Test
    fun `the categories of the period feed the chart`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        val breakdown = viewModel.state.value.content.breakdown
        assertEquals(2, breakdown.slices.size)
        assertEquals("Alimentación", breakdown.slices.first().category.name)
    }

    @Test
    fun `only the loans with an outstanding balance are counted`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        val content = viewModel.state.value.content
        assertEquals(1, content.owedToMe.count)
        assertEquals(BigDecimal("50000"), content.owedToMe.outstanding)
        assertEquals(1, content.iOwe.count)
        assertEquals(BigDecimal("600000"), content.iOwe.outstanding)
    }

    @Test
    fun `it greets the profile that the api returns`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        assertEquals("Camilo", viewModel.state.value.user?.firstName)
        assertEquals("CR", viewModel.state.value.user?.initials)
    }

    @Test
    fun `changing the month reloads and is seen in the state`() = runTest {
        val dependencies = TestHomeDependencies()
        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()
        viewModel.period.previousMonth()
        advanceUntilIdle()
        assertEquals(Period.Month(YearMonth.of(2026, 8)), viewModel.state.value.selection.period)
        assertEquals(
            viewModel.state.value.selection,
            dependencies.periodFilter.selection.value,
        )
    }

    @Test
    fun `applying a range is seen by the screen`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies())
        advanceUntilIdle()
        viewModel.period.openRangeSheet()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.rangeSheetOpen)
        viewModel.period.selectPreset(PeriodPreset.LAST_3_MONTHS)
        viewModel.period.applyRange()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.selection.isRange)
        assertNull(viewModel.state.value.rangeDraft)
    }

    @Test
    fun `a network failure leaves an error that retry clears`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies(movementsFailure = ApiError.Network))
        advanceUntilIdle()
        assertEquals(HomeError.NETWORK, viewModel.state.value.error)
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(HomeError.NETWORK, viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun `an expired session is told apart from a network failure`() = runTest {
        val viewModel = HomeViewModel(TestHomeDependencies(movementsFailure = ApiError.Unauthorized))
        advanceUntilIdle()
        assertEquals(HomeError.SESSION_EXPIRED, viewModel.state.value.error)
    }

    @Test
    fun `rates that are not ready yet do not break the screen`() = runTest {
        val viewModel = HomeViewModel(
            TestHomeDependencies(ratesFailure = ApiError.ServiceUnavailable),
        )
        advanceUntilIdle()
        val state = viewModel.state.value
        assertTrue(state.ratesUnavailable)
        assertNull(state.error)
        assertEquals(4, state.content.recent.size)
    }

    @Test
    fun `the eye is a device setting and comes back through the store`() = runTest {
        val dependencies = TestHomeDependencies()
        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()
        assertFalse(viewModel.state.value.hideAmounts)
        viewModel.onToggleHiddenAmounts()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.hideAmounts)
        assertTrue(dependencies.device.current.hideAmounts)
    }

    @Test
    fun `a message left by a form is shown once`() = runTest {
        val dependencies = TestHomeDependencies()
        val viewModel = HomeViewModel(dependencies)
        advanceUntilIdle()
        dependencies.messages.post(TransientMessages.MOVEMENT_SAVED_EXPENSE)
        advanceUntilIdle()
        assertEquals(TransientMessages.MOVEMENT_SAVED_EXPENSE, viewModel.state.value.toast)
        viewModel.onToastHidden()
        advanceUntilIdle()
        assertNull(viewModel.state.value.toast)
    }
}
