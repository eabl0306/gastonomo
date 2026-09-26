package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.feature.loans.payment.LoanPaymentViewModel
import com.bahoque.gastonomo.feature.loans.payment.PaymentPortion
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
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoanPaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `opening it starts empty, today and in the display currency`() {
        val model = viewModel()

        model.open(SampleFinance.partialLoan, summary(), context())

        assertTrue(model.state.value.isOpen)
        assertEquals("", model.state.value.entry.amountRaw)
        assertEquals(FixedClock.PROTOTYPE_TODAY, model.state.value.date)
        assertEquals(CurrencyCode.COP, model.state.value.entry.currency)
        assertFalse(model.state.value.canSave)
    }

    @Test
    fun `the chips fill the whole balance and the half`() {
        val model = viewModel()
        model.open(SampleFinance.partialLoan, summary(), context())

        model.fill(PaymentPortion.ALL)
        assertEquals("50000", model.state.value.entry.amountRaw)

        model.fill(PaymentPortion.HALF)
        assertEquals("25000", model.state.value.entry.amountRaw)
    }

    @Test
    fun `the chips convert the balance to the currency chosen for the payment`() {
        val model = viewModel()
        model.open(SampleFinance.partialLoan, summary(), context())
        model.editEntry { withCurrency(CurrencyCode.USD) }

        model.fill(PaymentPortion.ALL)

        // 50.000 COP con la tabla del prototipo son unos 15,63 dolares.
        assertEquals("15.63", model.state.value.entry.amountRaw)
    }

    @Test
    fun `a payment bigger than the balance is allowed because that is what makes the interest`() {
        val model = viewModel()
        model.open(SampleFinance.partialLoan, summary(), context())

        model.editEntry { withAmount("900000") }

        assertTrue(model.state.value.canSave)
    }

    @Test
    fun `saving reports the amount as it was typed and closes the sheet`() = runTest(dispatcher) {
        val dependencies = TestLoansDependencies()
        val model = LoanPaymentViewModel(dependencies.registerLoanPayment, dependencies.clock)
        model.open(SampleFinance.partialLoan, summary(), context())
        model.editEntry { withAmount("50000") }

        var saved: Money? = null
        model.save { saved = it }
        testScheduler.advanceUntilIdle()

        assertEquals(CurrencyCode.COP, assertNotNull(saved).currency)
        assertEquals(0, BigDecimal("50000").compareTo(saved.amount))
        assertFalse(model.state.value.isOpen)
        assertEquals(1, dependencies.gateway.keys.size)
    }

    @Test
    fun `a failure keeps the sheet open and the same key`() = runTest(dispatcher) {
        val error = ApiError.Conflict(ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS)
        val dependencies = TestLoansDependencies(FakeLoanGateway(failure = error))
        val model = LoanPaymentViewModel(dependencies.registerLoanPayment, dependencies.clock)
        model.open(SampleFinance.partialLoan, summary(), context())
        model.editEntry { withAmount("50000") }

        model.save { }
        testScheduler.advanceUntilIdle()
        val key = assertNotNull(model.state.value.idempotencyKey)

        model.save { }
        testScheduler.advanceUntilIdle()

        assertTrue(model.state.value.isOpen)
        assertEquals(LoanProblem.IN_PROGRESS, model.state.value.failure?.problem)
        assertEquals(listOf(key, key), dependencies.gateway.keys)
    }

    @Test
    fun `closing the currency sheet keeps everything that was typed`() {
        val model = viewModel()
        model.open(SampleFinance.partialLoan, summary(), context())
        model.editEntry { withAmount("12345") }

        model.editEntry { withCurrencySheet(true) }
        model.editEntry { withCurrencySheet(false) }

        assertFalse(model.state.value.entry.currencySheetOpen)
        assertEquals("12345", model.state.value.entry.amountRaw)
        assertTrue(model.state.value.isOpen)
    }

    @Test
    fun `dismissing it forgets everything`() {
        val model = viewModel()
        model.open(SampleFinance.partialLoan, summary(), context())
        model.editEntry { withAmount("12345") }

        model.dismiss()

        assertFalse(model.state.value.isOpen)
        assertNull(model.state.value.loan)
    }

    private fun viewModel(): LoanPaymentViewModel {
        val dependencies = TestLoansDependencies()
        return LoanPaymentViewModel(dependencies.registerLoanPayment, dependencies.clock)
    }

    private fun summary() =
        LoanSummary.of(SampleFinance.partialLoan, CurrencyCode.COP, SampleFinance.rates)

    private fun context() = MoneyContext(
        display = CurrencyCode.COP,
        rates = SampleFinance.rates,
        currencies = SampleFinance.currencies,
    )
}
