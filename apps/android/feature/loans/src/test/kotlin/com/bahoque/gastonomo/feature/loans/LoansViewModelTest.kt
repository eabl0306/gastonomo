package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.feature.loans.list.LoansViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoansViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it loads the loans with their summary and the totals of both sides`() = runTest(dispatcher) {
        val state = viewModel().state

        testScheduler.advanceUntilIdle()

        assertEquals(SampleFinance.loans.size, state.value.rows.size)
        // Solo el parcial de 150.000 con 100.000 abonados sigue abierto del lado que me deben.
        assertEquals(1, state.value.owedToMe.openCount)
        assertEquals(0, BigDecimal("50000").compareTo(state.value.owedToMe.outstanding.amount))
        assertEquals(1, state.value.iOwe.openCount)
        assertEquals(0, BigDecimal("600000").compareTo(state.value.iOwe.outstanding.amount))
        assertNull(state.value.problem)
    }

    @Test
    fun `the open loans come before the paid ones`() = runTest(dispatcher) {
        val state = viewModel().state

        testScheduler.advanceUntilIdle()

        val statuses = state.value.rows.map { it.summary.status.isOpen }
        assertEquals(statuses.sortedByDescending { it }, statuses)
    }

    @Test
    fun `the pills filter the list but not the totals`() = runTest(dispatcher) {
        val model = viewModel()
        testScheduler.advanceUntilIdle()

        model.onFilter(LoanDirection.BORROWED)

        assertEquals(1, model.state.value.visible.size)
        assertTrue(model.state.value.visible.all { it.loan.direction == LoanDirection.BORROWED })
        assertEquals(1, model.state.value.owedToMe.openCount)
    }

    @Test
    fun `a failure leaves a problem and retrying asks again`() = runTest(dispatcher) {
        val dependencies = TestLoansDependencies.failing(ApiError.Network)
        val model = LoansViewModel(dependencies.loadLoans, dependencies.moneyContextLoader())

        testScheduler.advanceUntilIdle()

        assertEquals(LoanProblem.NETWORK, model.state.value.problem)
        assertTrue(model.state.value.rows.isEmpty())

        model.reload()

        assertTrue(model.state.value.loading)
        assertNull(model.state.value.problem)
    }

    private fun viewModel(dependencies: TestLoansDependencies = TestLoansDependencies()) =
        LoansViewModel(dependencies.loadLoans, dependencies.moneyContextLoader())
}
