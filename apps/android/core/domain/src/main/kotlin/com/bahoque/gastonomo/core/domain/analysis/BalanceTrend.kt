package com.bahoque.gastonomo.core.domain.analysis

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.DisplayedAmount
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period
import java.math.BigDecimal
import java.time.LocalDate

/** Un punto de la linea de tendencia: un dia y el balance acumulado hasta el. */
data class TrendPoint(val date: LocalDate, val cumulative: BigDecimal)

/**
 * La tendencia del balance de la tarjeta de Inicio: el balance acumulado dia a dia del
 * periodo, hasta hoy.
 *
 * Acumula **desde cero** el primer dia del periodo y no arrastra saldo anterior, que es lo
 * que hace el prototipo y por eso no hace falta un endpoint de saldo previo.
 */
data class BalanceTrend(val points: List<TrendPoint>) {

    val isEmpty: Boolean get() = points.isEmpty()

    /** El minimo acumulado, para escalar el eje. */
    val minimum: BigDecimal get() = points.minOfOrNull { it.cumulative } ?: BigDecimal.ZERO

    /** El maximo acumulado, para escalar el eje. */
    val maximum: BigDecimal get() = points.maxOfOrNull { it.cumulative } ?: BigDecimal.ZERO

    companion object {
        /** Un punto por dia del periodo, cortando en [today]. */
        @Suppress("LongParameterList") // Cinco datos del dominio, ninguno agrupable.
        fun of(
            movements: List<Movement>,
            period: Period,
            today: LocalDate,
            display: CurrencyCode,
            rates: ExchangeRates,
        ): BalanceTrend {
            val last = if (period.to.isAfter(today)) today else period.to
            if (last.isBefore(period.from)) return BalanceTrend(emptyList())
            val daily = movements
                .filter { !it.occurredOn.isBefore(period.from) && !it.occurredOn.isAfter(last) }
                .groupBy { it.occurredOn }
                .mapValues { (_, group) -> signedTotal(group, display, rates) }
            var running = BigDecimal.ZERO
            val points = mutableListOf<TrendPoint>()
            var day = period.from
            while (!day.isAfter(last)) {
                running += daily[day] ?: BigDecimal.ZERO
                points += TrendPoint(day, running)
                day = day.plusDays(1)
            }
            return BalanceTrend(points)
        }

        private fun signedTotal(
            group: List<Movement>,
            display: CurrencyCode,
            rates: ExchangeRates,
        ): BigDecimal = group.fold(BigDecimal.ZERO) { total, movement ->
            val value = DisplayedAmount.of(movement.money, display, rates)
            if (movement.kind.isIncome) total + value else total - value
        }
    }
}
