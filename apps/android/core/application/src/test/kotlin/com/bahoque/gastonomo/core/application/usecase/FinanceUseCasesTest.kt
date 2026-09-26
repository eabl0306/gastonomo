package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.MovementPage
import com.bahoque.gastonomo.core.application.usecase.loan.CreateLoan
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoanDetail
import com.bahoque.gastonomo.core.application.usecase.loan.LoadLoans
import com.bahoque.gastonomo.core.application.usecase.loan.RegisterLoanPayment
import com.bahoque.gastonomo.core.application.usecase.movement.CreateMovement
import com.bahoque.gastonomo.core.application.usecase.movement.LoadPeriodMovements
import com.bahoque.gastonomo.core.application.usecase.movement.LoadRecentMovements
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanStatus
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.testing.fake.FakeLoanGateway
import com.bahoque.gastonomo.core.testing.fake.FakeMovementGateway
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FinanceUseCasesTest {

    @Test
    fun `loading a period exhausts the cursor`() = runTest {
        val gateway = FakeMovementGateway(
            pages = listOf(
                MovementPage(listOf(SampleFinance.groceries), "cursor-1"),
                MovementPage(listOf(SampleFinance.internet), "cursor-2"),
                MovementPage(listOf(SampleFinance.monthlySalary), null),
            ),
        )
        val result = LoadPeriodMovements(gateway)(SEPTEMBER)
        assertEquals(3, (result as Outcome.Success).value.size)
        assertEquals(3, gateway.listCalls)
    }

    @Test
    fun `a single page needs one call`() = runTest {
        val gateway = FakeMovementGateway(pages = listOf(MovementPage(SampleFinance.movements, null)))
        LoadPeriodMovements(gateway)(SEPTEMBER)
        assertEquals(1, gateway.listCalls)
    }

    @Test
    fun `a failure while paginating stops the walk`() = runTest {
        val gateway = FakeMovementGateway(failure = ApiError.Network)
        assertFalse(LoadPeriodMovements(gateway)(SEPTEMBER).isSuccess)
    }

    @Test
    fun `the home screen asks for the four most recent`() = runTest {
        val gateway = FakeMovementGateway(pages = listOf(MovementPage(SampleFinance.movements, null)))
        val result = LoadRecentMovements(gateway)(SEPTEMBER)
        assertEquals(SampleFinance.movements, (result as Outcome.Success).value)
        assertEquals(LoadRecentMovements.RECENT_LIMIT, 4)
    }

    @Test
    fun `a failed recent load is reported`() = runTest {
        val gateway = FakeMovementGateway(failure = ApiError.Unauthorized)
        assertFalse(LoadRecentMovements(gateway)(SEPTEMBER).isSuccess)
    }

    @Test
    fun `creating a movement passes the key of the attempt through`() = runTest {
        val gateway = FakeMovementGateway()
        val key = IdempotencyKey("attempt-1")
        CreateMovement(gateway)(MOVEMENT_DRAFT, key)
        assertEquals(listOf(key), gateway.keys)
        assertEquals(MOVEMENT_DRAFT, gateway.lastDraft)
    }

    @Test
    fun `a retry that replays does not look like a new creation`() = runTest {
        val gateway = FakeMovementGateway(replay = true)
        val result = CreateMovement(gateway)(MOVEMENT_DRAFT, IdempotencyKey("attempt-1"))
        assertTrue((result as Outcome.Success).value.isReplay)
    }

    @Test
    fun `loading loans adds the summary and puts the open ones first`() = runTest {
        val result = LoadLoans(FakeLoanGateway())(CurrencyCode.COP, SampleFinance.rates)
        val loans = (result as Outcome.Success).value
        assertEquals(
            listOf(LoanStatus.PARTIAL, LoanStatus.PENDING, LoanStatus.PAID),
            loans.map { it.second.status },
        )
    }

    @Test
    fun `loading loans can filter by direction`() = runTest {
        val gateway = FakeLoanGateway()
        val result = LoadLoans(gateway)(CurrencyCode.COP, SampleFinance.rates, LoanDirection.BORROWED)
        assertEquals(LoanDirection.BORROWED, gateway.lastDirection)
        assertEquals(1, (result as Outcome.Success).value.size)
    }

    @Test
    fun `a failed loan list is reported`() = runTest {
        val gateway = FakeLoanGateway(failure = ApiError.Network)
        assertFalse(LoadLoans(gateway)(CurrencyCode.COP, SampleFinance.rates).isSuccess)
    }

    @Test
    fun `the detail orders the payments from the most recent`() = runTest {
        val result = LoadLoanDetail(FakeLoanGateway())(
            id = SampleFinance.paidLoan.id,
            display = CurrencyCode.COP,
            rates = SampleFinance.rates,
        )
        val (loan, summary) = (result as Outcome.Success).value
        assertEquals(2, loan.payments.size)
        assertEquals(LoanStatus.PAID, summary.status)
    }

    @Test
    fun `a loan of another account is not found`() = runTest {
        val result = LoadLoanDetail(FakeLoanGateway())(
            id = LoanId("unknown"),
            display = CurrencyCode.COP,
            rates = SampleFinance.rates,
        )
        assertEquals(ApiError.NotFound, (result as Outcome.Failure).error)
    }

    @Test
    fun `creating a loan and a payment carry their idempotency key`() = runTest {
        val gateway = FakeLoanGateway()
        CreateLoan(gateway)(LOAN_DRAFT, IdempotencyKey("loan-1"))
        RegisterLoanPayment(gateway)(
            id = SampleFinance.partialLoan.id,
            draft = PAYMENT_DRAFT,
            key = IdempotencyKey("payment-1"),
        )
        assertEquals(listOf("loan-1", "payment-1"), gateway.keys.map { it.value })
    }

    @Test
    fun `a failed creation is reported`() = runTest {
        val gateway = FakeLoanGateway(failure = ApiError.Conflict("idempotency_key_reused"))
        assertFalse(CreateLoan(gateway)(LOAN_DRAFT, IdempotencyKey("loan-1")).isSuccess)
        assertFalse(
            RegisterLoanPayment(gateway)(
                LoanId("l1"),
                PAYMENT_DRAFT,
                IdempotencyKey("payment-1"),
            ).isSuccess,
        )
    }

    private companion object {
        val SEPTEMBER = Period.Month(YearMonth.of(2026, 9))
        val MONEY = MonetaryAmount.single(BigDecimal("186400"), CurrencyCode.COP)

        val MOVEMENT_DRAFT = MovementDraft(
            occurredOn = LocalDate.of(2026, 9, 15),
            kind = MovementKind.OUT,
            categoryId = SampleFinance.food.id,
            money = MONEY,
            note = "Mercado",
        )

        val LOAN_DRAFT = LoanDraft(
            person = "Rosa",
            direction = LoanDirection.LENT,
            openedOn = LocalDate.of(2026, 9, 15),
            money = MONEY,
        )

        val PAYMENT_DRAFT = LoanPaymentDraft(paidOn = LocalDate.of(2026, 9, 19), money = MONEY)
    }
}
