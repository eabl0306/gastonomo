package com.bahoque.gastonomo.core.domain.period

import java.time.LocalDate
import java.time.YearMonth

/**
 * El filtro de fechas que comparten Inicio y Movimientos: o un mes, o un rango. Decide
 * el `from` y el `to` de `GET /v1/movements` y de que periodo se calculan el balance, la
 * tendencia, los gastos por categoria y el resumen de tres columnas.
 *
 * No afecta a los prestamos: "Me deben" y "Debo" son siempre saldos actuales.
 */
sealed interface Period {

    /** Primer dia del periodo, inclusive. */
    val from: LocalDate

    /** Ultimo dia del periodo, inclusive. */
    val to: LocalDate

    /** Un mes natural completo. Es el estado por defecto. */
    data class Month(val month: YearMonth) : Period {
        override val from: LocalDate get() = month.atDay(1)
        override val to: LocalDate get() = month.atEndOfMonth()
    }

    /** Un rango elegido a mano en el sheet de rango. */
    data class Range(override val from: LocalDate, override val to: LocalDate) : Period {
        init {
            require(!from.isAfter(to)) { "A range starts before it ends" }
        }
    }

    companion object {
        /** El mes al que pertenece [today]. */
        fun currentMonth(today: LocalDate): Period = Month(YearMonth.from(today))
    }
}

/**
 * Los cinco presets del sheet de rango, en el orden en el que salen. El rango se calcula
 * contra el dia de hoy, que en produccion es la fecha real.
 */
enum class PeriodPreset {
    THIS_MONTH,
    LAST_MONTH,
    LAST_30_DAYS,
    LAST_3_MONTHS,
    THIS_YEAR,
    ;

    /** El rango que este preset produce con [today] como dia de hoy. */
    fun rangeOn(today: LocalDate): Period.Range {
        val month = YearMonth.from(today)
        return when (this) {
            THIS_MONTH -> Period.Range(month.atDay(1), month.atEndOfMonth())
            LAST_MONTH -> month.minusMonths(1).let { Period.Range(it.atDay(1), it.atEndOfMonth()) }
            LAST_30_DAYS -> Period.Range(today.minusDays(DAYS_IN_A_MONTH), today)
            LAST_3_MONTHS -> Period.Range(today.minusMonths(QUARTER_MONTHS), today)
            THIS_YEAR -> Period.Range(today.withDayOfYear(1), today)
        }
    }

    private companion object {
        const val DAYS_IN_A_MONTH = 29L
        const val QUARTER_MONTHS = 3L
    }
}
