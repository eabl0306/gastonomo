package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanStatus
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LoanDetailViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it loads the loan with its balance and its payments`() = runTest(dispatcher) {
        val model = viewModel(SampleFinance.partialLoan.id)

        testScheduler.advanceUntilIdle()

        val row = assertNotNull(model.state.value.row)
        assertEquals(LoanStatus.PARTIAL, row.summary.status)
        assertEquals(0, BigDecimal("50000").compareTo(row.summary.outstanding.amount))
        assertEquals(1, row.loan.payments.size)
        assertNull(model.state.value.problem)
    }

    @Test
    fun `a paid loan that was overpaid shows the interest`() = runTest(dispatcher) {
        val model = viewModel(SampleFinance.paidLoan.id)

        testScheduler.advanceUntilIdle()

        val row = assertNotNull(model.state.value.row)
        assertEquals(LoanStatus.PAID, row.summary.status)
        assertEquals(0, BigDecimal("100000").compareTo(row.summary.interest.amount))
    }

    @Test
    fun `the payments come from the most recent to the oldest`() = runTest(dispatcher) {
        val model = viewModel(SampleFinance.paidLoan.id)

        testScheduler.advanceUntilIdle()

        val dates = assertNotNull(model.state.value.row).loan.payments.map { it.paidOn }
        assertEquals(dates.sortedDescending(), dates)
    }

    @Test
    fun `a loan of another account is a not found problem, never a forbidden one`() = runTest(dispatcher) {
        val dependencies = TestLoansDependencies()
        val model = LoanDetailViewModel(
            loanId = LoanId("does-not-exist"),
            loadLoanDetail = dependencies.loadLoanDetail,
            contextLoader = dependencies.moneyContextLoader(),
        )

        testScheduler.advanceUntilIdle()

        assertEquals(LoanProblem.NOT_FOUND, model.state.value.problem)
        assertNull(model.state.value.row)
    }

    @Test
    fun `a network failure can be retried`() = runTest(dispatcher) {
        val dependencies = TestLoansDependencies.failing(ApiError.Network)
        val model = LoanDetailViewModel(
            loanId = SampleFinance.partialLoan.id,
            loadLoanDetail = dependencies.loadLoanDetail,
            contextLoader = dependencies.moneyContextLoader(),
        )

        testScheduler.advanceUntilIdle()
        assertEquals(LoanProblem.NETWORK, model.state.value.problem)

        model.reload()

        assertNull(model.state.value.problem)
    }

    private fun viewModel(id: LoanId): LoanDetailViewModel {
        val dependencies = TestLoansDependencies()
        return LoanDetailViewModel(id, dependencies.loadLoanDetail, dependencies.moneyContextLoader())
    }
}
