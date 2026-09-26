package com.bahoque.gastonomo.core.domain.analysis

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.Movement
import java.math.BigDecimal
import java.math.RoundingMode

/** Una porcion del grafico de gastos por categoria. */
data class CategorySlice(
    val category: Category,
    val total: Money,
    /** Fraccion del total del periodo, entre 0 y 1. Es el ancho de la porcion. */
    val fraction: Float,
    /** El porcentaje redondeado que ensena la fila. */
    val percent: Int,
)

/**
 * El grafico de gastos por categoria de Inicio: la barra apilada con las seis categorias
 * de mas gasto y las filas de debajo.
 *
 * Cada movimiento ya trae su categoria completa, asi que no hay que cruzar nada.
 */
data class CategoryBreakdown(val total: Money, val slices: List<CategorySlice>) {
    companion object {
        /** Cuantas categorias ensena la barra apilada del diseno. */
        const val TOP_SLICES = 6

        /** Las categorias con mas gasto del periodo, de mayor a menor. */
        fun ofExpenses(
            movements: List<Movement>,
            display: CurrencyCode,
            rates: ExchangeRates,
            limit: Int = TOP_SLICES,
        ): CategoryBreakdown {
            val expenses = movements.filterNot { it.kind.isIncome }
            val total = DisplayedAmount.sum(expenses.map { it.money }, display, rates)
            if (total.amount.signum() == 0) return CategoryBreakdown(total, emptyList())
            val slices = expenses
                .groupBy { it.category.id }
                .map { (_, group) -> sliceOf(group, display, rates, total.amount) }
                .sortedByDescending { it.total.amount }
                .take(limit)
            return CategoryBreakdown(total, slices)
        }

        private fun sliceOf(
            group: List<Movement>,
            display: CurrencyCode,
            rates: ExchangeRates,
            total: BigDecimal,
        ): CategorySlice {
            val amount = DisplayedAmount.sum(group.map { it.money }, display, rates)
            val ratio = amount.amount.divide(total, ExchangeRates.PRECISION)
            return CategorySlice(
                category = group.first().category,
                total = amount,
                fraction = ratio.toFloat(),
                percent = ratio.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).toInt(),
            )
        }

        private val HUNDRED = BigDecimal(100)
    }
}
