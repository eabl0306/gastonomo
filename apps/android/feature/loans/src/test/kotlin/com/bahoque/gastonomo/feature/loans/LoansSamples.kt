package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanOrder
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.loan.LoanTotals
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.feature.loans.detail.LoanDetailUiState
import com.bahoque.gastonomo.feature.loans.form.NewLoanUiState
import com.bahoque.gastonomo.feature.loans.list.LoansUiState
import com.bahoque.gastonomo.feature.loans.payment.PaymentUiState

/**
 * Estados de ejemplo con los datos del prototipo, para las capturas y los tests de pantalla.
 *
 * Salen de [SampleFinance] y no de datos inventados, asi que una captura se puede comparar
 * con el diseno entregado tal cual.
 */
internal object LoansSamples {

    val context = MoneyContext(
        display = CurrencyCode.COP,
        rates = SampleFinance.rates,
        currencies = SampleFinance.currencies,
    )

    private val ordered = LoanOrder.sorted(
        SampleFinance.loans.map { it to LoanSummary.of(it, context.display, context.rates) },
    )

    val rows = ordered.map { (loan, summary) -> LoanRow(loan, summary) }

    val partial = rows.first { it.loan.id == SampleFinance.partialLoan.id }

    val paidWithInterest = rows.first { it.loan.id == SampleFinance.paidLoan.id }

    val loans = LoansUiState(
        loading = false,
        rows = rows,
        owedToMe = LoanTotals.of(ordered, LoanDirection.LENT, context.display),
        iOwe = LoanTotals.of(ordered, LoanDirection.BORROWED, context.display),
        context = context,
    )

    val emptyLoans = LoansUiState(loading = false, context = context)

    val loansProblem = LoansUiState(loading = false, problem = LoanProblem.NETWORK, context = context)

    val detail = LoanDetailUiState(loading = false, row = withNote(), context = context)

    val paidDetail = LoanDetailUiState(loading = false, row = paidWithInterest, context = context)

    val newLoan = NewLoanUiState(
        direction = LoanDirection.LENT,
        person = "Johana",
        note = "Collar",
        date = FixedClock.PROTOTYPE_TODAY,
        entry = MoneyEntryState(amountRaw = "150000", currency = CurrencyCode.COP),
        context = context,
    )

    val newLoanInDollars = newLoan.copy(
        direction = LoanDirection.BORROWED,
        entry = MoneyEntryState(amountRaw = "140", currency = CurrencyCode.USD),
    )

    val payment = PaymentUiState(
        loan = partial.loan,
        outstanding = partial.summary.outstanding,
        entry = MoneyEntryState(amountRaw = "25000", currency = CurrencyCode.COP),
        date = FixedClock.PROTOTYPE_TODAY,
        context = context,
    )

    /** El detalle del diseno lleva nota, y el parcial de ejemplo no la trae. */
    private fun withNote(): LoanRow {
        val loan = partial.loan.copy(note = "Collar")
        return LoanRow(loan, LoanSummary.of(loan, context.display, context.rates))
    }
}
