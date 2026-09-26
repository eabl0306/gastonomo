package com.bahoque.gastonomo.core.domain.analysis

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.Money
import com.bahoque.gastonomo.core.domain.movement.Movement
import java.time.LocalDate

/** Un grupo de dia de la lista de Movimientos, con su neto a la derecha del encabezado. */
data class DayGroup(val date: LocalDate, val net: Money, val movements: List<Movement>)

/** Agrupa los movimientos por dia, del mas reciente al mas antiguo. */
object DayGrouping {
    fun of(
        movements: List<Movement>,
        display: CurrencyCode,
        rates: ExchangeRates,
    ): List<DayGroup> = movements
        .groupBy { it.occurredOn }
        .toSortedMap(compareByDescending { it })
        .map { (date, group) ->
            val summary = PeriodSummary.of(group, display, rates)
            DayGroup(date = date, net = summary.net, movements = group)
        }
}

/** Suma de una lista de movimientos en la moneda que se ensena. */
fun List<Movement>.totalIn(display: CurrencyCode, rates: ExchangeRates): Money =
    DisplayedAmount.sum(map { it.money }, display, rates)
