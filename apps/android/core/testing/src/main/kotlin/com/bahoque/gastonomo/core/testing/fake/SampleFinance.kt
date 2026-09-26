package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Los datos de ejemplo del prototipo. Las capturas y los tests usan estos, no unos
 * inventados, para que lo que se audita sea comparable con el diseno entregado.
 */
object SampleFinance {

    val food = category("Alimentación", CategoryKind.OUT, "restaurant", "#cf5f35")
        .copy(movementCount = 12)
    val salary = category("Salario", CategoryKind.IN, "account_balance_wallet", "#2f8a55")
        .copy(movementCount = 3)
    val utilities = category("Servicios", CategoryKind.OUT, "bolt", "#c48f12")
        .copy(movementCount = 6)
    val freelance = category("Freelance", CategoryKind.IN, "laptop_mac", "#2f7fb5")
        .copy(movementCount = 1)

    /** La unica categoria del usuario: el resto son predeterminadas. */
    val pets = category("Mascotas", CategoryKind.OUT, "pets", "#2f7fb5")
        .copy(isDefault = false, movementCount = 1)

    val categories = listOf(food, salary, utilities, freelance, pets)

    /** Un gasto en la moneda principal. */
    val groceries = movement("Mercado", MovementKind.OUT, "2026-09-15", "186400", food)

    /** Un ingreso registrado en dolares, con su equivalencia guardada en pesos. */
    val freelanceIncome = Movement(
        id = MovementId("2"),
        occurredOn = LocalDate.parse("2026-09-05"),
        kind = MovementKind.IN,
        title = "Diseño web · cliente EE. UU.",
        money = MonetaryAmount(
            amount = BigDecimal("140"),
            currency = CurrencyCode.USD,
            base = BigDecimal("450000"),
            baseCurrency = CurrencyCode.COP,
        ),
        category = freelance,
    )

    val monthlySalary = movement("Salario", MovementKind.IN, "2026-09-01", "2800000", salary)
    val internet = movement("Internet y luz", MovementKind.OUT, "2026-09-10", "142000", utilities)

    val movements = listOf(monthlySalary, freelanceIncome, internet, groceries)

    val payment = LoanPayment(
        id = LoanPaymentId("p1"),
        loanId = LoanId("l1"),
        paidOn = LocalDate.parse("2026-09-19"),
        label = null,
        money = MonetaryAmount.single(BigDecimal("100000"), CurrencyCode.COP),
    )

    /** Parcial: 100.000 de 150.000. */
    val partialLoan = Loan(
        id = LoanId("l1"),
        person = "Collar Johana",
        note = "",
        direction = LoanDirection.LENT,
        openedOn = LocalDate.parse("2026-07-02"),
        money = MonetaryAmount.single(BigDecimal("150000"), CurrencyCode.COP),
        payments = listOf(payment),
    )

    /** Pagado con exceso: 700.000 de 600.000, asi que hay intereses ganados. */
    val paidLoan = Loan(
        id = LoanId("l2"),
        person = "Rosa",
        note = "",
        direction = LoanDirection.LENT,
        openedOn = LocalDate.parse("2026-06-28"),
        money = MonetaryAmount.single(BigDecimal("600000"), CurrencyCode.COP),
        payments = listOf(
            payment.copy(id = LoanPaymentId("p2"), loanId = LoanId("l2"), money = cop("600000")),
            payment.copy(
                id = LoanPaymentId("p3"),
                loanId = LoanId("l2"),
                label = "Intereses",
                money = cop("100000"),
            ),
        ),
    )

    /** Pendiente: sin ningun abono, y ademas es dinero que debo. */
    val pendingLoan = Loan(
        id = LoanId("l3"),
        person = "Hermana Evila",
        note = "Teléfono",
        direction = LoanDirection.BORROWED,
        openedOn = LocalDate.parse("2026-06-20"),
        money = MonetaryAmount.single(BigDecimal("600000"), CurrencyCode.COP),
    )

    val loans = listOf(partialLoan, paidLoan, pendingLoan)

    val currencies = listOf(
        CurrencyInfo(CurrencyCode.COP, "Colombian Peso", "$"),
        CurrencyInfo(CurrencyCode.USD, "United States Dollar", "$"),
        CurrencyInfo(CurrencyCode.EUR, "Euro", "€"),
        CurrencyInfo(CurrencyCode.MXN, "Mexican Peso", "$"),
    )

    /** Las tasas de reserva del prototipo, con base USD. */
    val rates = ExchangeRates(
        base = CurrencyCode.USD,
        date = LocalDate.parse("2026-09-22"),
        status = RatesStatus.OK,
        rates = mapOf(
            CurrencyCode.USD to BigDecimal("1"),
            CurrencyCode.COP to BigDecimal("3199.41"),
            CurrencyCode.EUR to BigDecimal("0.87088"),
            CurrencyCode.MXN to BigDecimal("17.2402"),
        ),
    )

    private fun cop(amount: String) = MonetaryAmount.single(BigDecimal(amount), CurrencyCode.COP)

    private fun category(name: String, kind: CategoryKind, icon: String, color: String) = Category(
        id = CategoryId(name.lowercase()),
        name = name,
        kind = kind,
        icon = icon,
        color = color,
        isDefault = true,
    )

    @Suppress("LongParameterList") // Es un constructor de datos de ejemplo, no logica.
    private fun movement(
        title: String,
        kind: MovementKind,
        date: String,
        amount: String,
        category: Category,
    ) = Movement(
        id = MovementId(title),
        occurredOn = LocalDate.parse(date),
        kind = kind,
        title = title,
        money = cop(amount),
        category = category,
    )
}
