package com.bahoque.gastonomo.core.domain.analysis

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.Movement

/**
 * Ingresos, gastos y neto del periodo. Alimenta la tarjeta de balance de Inicio y el
 * resumen de tres columnas de Movimientos.
 *
 * Ojo con el resumen de tres columnas: se calcula sobre el periodo **sin filtrar**, asi
 * que hay que pedir la lista sin `kind` y filtrar las pills en local. Si se pide
 * `kind=out` no se puede pintar la columna de Ingresos.
 */
data class PeriodSummary(val income: Money, val expense: Money, val net: Money) {
    companion object {
        /** Suma los movimientos del periodo en [display] con las tasas de hoy. */
        fun of(
            movements: List<Movement>,
            display: CurrencyCode,
            rates: ExchangeRates,
        ): PeriodSummary {
            val (incomes, expenses) = movements.partition { it.kind.isIncome }
            val income = DisplayedAmount.sum(incomes.map { it.money }, display, rates)
            val expense = DisplayedAmount.sum(expenses.map { it.money }, display, rates)
            return PeriodSummary(income, expense, income - expense)
        }
    }
}
