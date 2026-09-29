package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.feature.loans.form.LoanPrefill
import com.bahoque.gastonomo.feature.loans.form.NewLoanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NewLoanViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() = Dispatchers.setMain(dispatcher)

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it starts lent, today and in the display currency`() = runTest(dispatcher) {
        val model = viewModel()

        testScheduler.advanceUntilIdle()

        assertEquals(LoanDirection.LENT, model.state.value.direction)
        assertEquals(FixedClock.PROTOTYPE_TODAY, model.state.value.date)
        assertEquals(CurrencyCode.COP, model.state.value.entry.currency)
        assertFalse(model.state.value.canSave)
    }

    @Test
    fun `it can only be saved with an amount and a person`() = runTest(dispatcher) {
        val model = viewModel()
        testScheduler.advanceUntilIdle()

        model.editEntry { withAmount("150000") }
        assertFalse(model.state.value.canSave)

        model.onPerson("Johana")
        assertTrue(model.state.value.canSave)

        model.onPerson("   ")
        assertFalse(model.state.value.canSave)
    }

    @Test
    fun `it sends one post with the four money fields and the person trimmed`() = runTest(dispatcher) {
        val dependencies = TestLoansDependencies()
        val model = NewLoanViewModel(
            dependencies.createLoan,
            dependencies.moneyContextLoader(),
            dependencies.clock,
        )
        testScheduler.advanceUntilIdle()
        model.editEntry { withAmount("150000") }
        model.onPerson("  Johana  ")
        model.onNote("Collar")

        var replay: Boolean? = null
        model.save { replay = it }
        testScheduler.advanceUntilIdle()

        assertEquals(false, replay)
        assertEquals(1, dependencies.gateway.keys.size)
        assertNull(model.state.value.idempotencyKey)
    }

    @Test
    fun `a validation error keeps the key and shows the message of the api next to its field`() =
        runTest(dispatcher) {
            val error = ApiError.Validation(listOf(FieldError("person", "El nombre es obligatorio.")))
            val dependencies = TestLoansDependencies(FakeLoanGateway(failure = error))
            val model = NewLoanViewModel(
                dependencies.createLoan,
                dependencies.moneyContextLoader(),
                dependencies.clock,
            )
            testScheduler.advanceUntilIdle()
            model.editEntry { withAmount("1000") }
            model.onPerson("Johana")

            model.save { }
            testScheduler.advanceUntilIdle()

            val key = assertNotNull(model.state.value.idempotencyKey)
            assertEquals("El nombre es obligatorio.", model.state.value.failure?.person)

            model.save { }
            testScheduler.advanceUntilIdle()

            // El mismo intento reutiliza la clave: un 422 no la quema.
            assertEquals(listOf(key, key), dependencies.gateway.keys)
        }

    @Test
    fun `a reused key conflict burns the key so the next attempt gets a new one`() = runTest(dispatcher) {
        val error = ApiError.Conflict(ConflictCode.IDEMPOTENCY_KEY_REUSED)
        val dependencies = TestLoansDependencies(FakeLoanGateway(failure = error))
        val model = NewLoanViewModel(
            dependencies.createLoan,
            dependencies.moneyContextLoader(),
            dependencies.clock,
        )
        testScheduler.advanceUntilIdle()
        model.editEntry { withAmount("1000") }
        model.onPerson("Johana")

        model.save { }
        testScheduler.advanceUntilIdle()
        assertNull(model.state.value.idempotencyKey)
        assertEquals(LoanProblem.BODY_CHANGED, model.state.value.failure?.problem)

        model.save { }
        testScheduler.advanceUntilIdle()

        assertEquals(2, dependencies.gateway.keys.distinct().size)
    }

    @Test
    fun `an in progress conflict keeps the key so waiting and retrying is safe`() = runTest(dispatcher) {
        val error = ApiError.Conflict(ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS)
        val dependencies = TestLoansDependencies(FakeLoanGateway(failure = error))
        val model = NewLoanViewModel(
            dependencies.createLoan,
            dependencies.moneyContextLoader(),
            dependencies.clock,
        )
        testScheduler.advanceUntilIdle()
        model.editEntry { withAmount("1000") }
        model.onPerson("Johana")

        model.save { }
        testScheduler.advanceUntilIdle()

        assertEquals(LoanProblem.IN_PROGRESS, model.state.value.failure?.problem)
        assertNotNull(model.state.value.idempotencyKey)
    }

    @Test
    fun `changing the currency drops the equivalence the user had typed`() = runTest(dispatcher) {
        val model = viewModel()
        testScheduler.advanceUntilIdle()

        model.editEntry { withAmount("140").withCurrency(CurrencyCode.USD).withRate("4500") }
        assertEquals("4500", model.state.value.entry.rateRaw)

        model.editEntry { withCurrency(CurrencyCode.EUR) }

        assertNull(model.state.value.entry.rateRaw)
    }

    @Test
    fun `a foreign amount saves the suggested equivalence when the user does not touch it`() =
        runTest(dispatcher) {
            val model = viewModel()
            testScheduler.advanceUntilIdle()

            model.editEntry { withAmount("140").withCurrency(CurrencyCode.USD) }
            val suggestion = model.state.value.entry.suggestion(model.state.value.context)

            assertTrue(suggestion.isForeign)
            assertEquals(CurrencyCode.COP, suggestion.money.baseCurrency)
            assertTrue(suggestion.money.base > BigDecimal.ZERO)
        }

    @Test
    fun `coming from the dictation it opens with everything it understood`() = runTest {
        val dependencies = TestLoansDependencies()
        val model = NewLoanViewModel(
            createLoan = dependencies.createLoan,
            contextLoader = dependencies.moneyContextLoader(),
            clock = dependencies.clock,
            prefill = LoanPrefill(
                person = "Rosa",
                direction = LoanDirection.BORROWED,
                amount = "200000",
                currency = CurrencyCode.USD,
                note = "Teléfono",
                openedOn = LocalDate.of(2026, 9, 20),
            ),
        )
        advanceUntilIdle()
        val state = model.state.value
        assertEquals("Rosa", state.person)
        assertEquals(LoanDirection.BORROWED, state.direction)
        assertEquals("200000", state.entry.amountRaw)
        assertEquals(CurrencyCode.USD, state.entry.currency)
        assertEquals("Teléfono", state.note)
        assertEquals(LocalDate.of(2026, 9, 20), state.date)
        assertTrue(state.canSave)
    }

    @Test
    fun `an empty prefill opens the form exactly as the quick add button does`() = runTest {
        val model = viewModel()
        advanceUntilIdle()
        val state = model.state.value
        assertEquals("", state.person)
        assertEquals(LoanDirection.LENT, state.direction)
        assertEquals("", state.entry.amountRaw)
        assertEquals(FixedClock.PROTOTYPE_TODAY, state.date)
    }

    private fun viewModel(): NewLoanViewModel {
        val dependencies = TestLoansDependencies()
        return NewLoanViewModel(
            dependencies.createLoan,
            dependencies.moneyContextLoader(),
            dependencies.clock,
        )
    }
}
