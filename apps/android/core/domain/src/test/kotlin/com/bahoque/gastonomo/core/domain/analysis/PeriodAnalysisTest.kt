package com.bahoque.gastonomo.core.domain.analysis

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.domain.period.Period
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PeriodAnalysisTest {

    @Test
    fun `the summary splits income from expense and gives the net`() {
        val summary = PeriodSummary.of(MOVEMENTS, COP, RATES)
        assertEquals(BigDecimal("2800000"), summary.income.amount)
        assertEquals(BigDecimal("330000"), summary.expense.amount)
        assertEquals(BigDecimal("2470000"), summary.net.amount)
    }

    @Test
    fun `an empty period has a net of zero`() {
        val summary = PeriodSummary.of(emptyList(), COP, RATES)
        assertTrue(summary.net.isZero)
    }

    @Test
    fun `the breakdown ranks the categories by expense`() {
        val breakdown = CategoryBreakdown.ofExpenses(MOVEMENTS, COP, RATES)
        assertEquals(listOf("Vivienda", "Alimentación"), breakdown.slices.map { it.category.name })
        assertEquals(BigDecimal("330000"), breakdown.total.amount)
        assertEquals(61, breakdown.slices.first().percent)
    }

    @Test
    fun `the breakdown of a period without expenses is empty`() {
        val incomeOnly = MOVEMENTS.filter { it.kind.isIncome }
        assertTrue(CategoryBreakdown.ofExpenses(incomeOnly, COP, RATES).slices.isEmpty())
    }

    @Test
    fun `the breakdown keeps at most the requested number of slices`() {
        val breakdown = CategoryBreakdown.ofExpenses(MOVEMENTS, COP, RATES, limit = 1)
        assertEquals(1, breakdown.slices.size)
    }

    @Test
    fun `the trend accumulates day by day from zero and stops at today`() {
        val trend = BalanceTrend.of(MOVEMENTS, SEPTEMBER, TODAY, COP, RATES)
        assertEquals(22, trend.points.size)
        assertEquals(LocalDate.of(2026, 9, 1), trend.points.first().date)
        assertEquals(TODAY, trend.points.last().date)
        assertEquals(BigDecimal("2600000"), trend.points.first().cumulative)
        assertEquals(BigDecimal("2470000"), trend.points.last().cumulative)
    }

    @Test
    fun `the trend of a period that has not started yet is empty`() {
        val future = Period.Month(YearMonth.of(2026, 12))
        assertTrue(BalanceTrend.of(MOVEMENTS, future, TODAY, COP, RATES).isEmpty)
    }

    @Test
    fun `the trend reports its extremes so the chart can be scaled`() {
        val trend = BalanceTrend.of(MOVEMENTS, SEPTEMBER, TODAY, COP, RATES)
        assertEquals(BigDecimal("2470000"), trend.minimum)
        assertEquals(BigDecimal("2600000"), trend.maximum)
    }

    @Test
    fun `an empty trend reports zero extremes`() {
        val trend = BalanceTrend(emptyList())
        assertEquals(BigDecimal.ZERO, trend.minimum)
        assertEquals(BigDecimal.ZERO, trend.maximum)
    }

    @Test
    fun `the movements are grouped by day from the most recent`() {
        val groups = DayGrouping.of(MOVEMENTS, COP, RATES)
        assertEquals(
            listOf(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 1)),
            groups.map { it.date },
        )
        assertEquals(BigDecimal("-130000"), groups.first().net.amount)
        assertEquals(2, groups.last().movements.size)
    }

    @Test
    fun `a list of movements can be totalled in the shown currency`() {
        assertEquals(BigDecimal("3130000"), MOVEMENTS.totalIn(COP, RATES).amount)
    }

    private companion object {
        val COP = CurrencyCode.COP
        val TODAY: LocalDate = LocalDate.of(2026, 9, 22)
        val SEPTEMBER = Period.Month(YearMonth.of(2026, 9))

        val RATES = ExchangeRates(
            base = CurrencyCode.USD,
            date = TODAY,
            status = RatesStatus.OK,
            rates = mapOf(
                CurrencyCode.USD to BigDecimal("1"),
                COP to BigDecimal("3199.41"),
            ),
        )

        val HOUSING = category("Vivienda", "home", "#7a52c7")
        val FOOD = category("Alimentación", "restaurant", "#cf5f35")
        val SALARY = Category(
            id = CategoryId("salario"),
            name = "Salario",
            kind = CategoryKind.IN,
            icon = "account_balance_wallet",
            color = "#2f8a55",
            isDefault = true,
        )

        val MOVEMENTS = listOf(
            movement("1", MovementKind.IN, "2026-09-01", "2800000", SALARY),
            movement("2", MovementKind.OUT, "2026-09-01", "200000", HOUSING),
            movement("3", MovementKind.OUT, "2026-09-15", "130000", FOOD),
        )

        fun category(name: String, icon: String, color: String) = Category(
            id = CategoryId(name.lowercase()),
            name = name,
            kind = CategoryKind.OUT,
            icon = icon,
            color = color,
            isDefault = true,
        )

        @Suppress("LongParameterList") // Constructor de datos de ejemplo, sin logica.
        fun movement(
            id: String,
            kind: MovementKind,
            date: String,
            amount: String,
            category: Category,
        ) = Movement(
            id = MovementId(id),
            occurredOn = LocalDate.parse(date),
            kind = kind,
            title = category.name,
            money = MonetaryAmount.single(BigDecimal(amount), COP),
            category = category,
        )
    }
}
