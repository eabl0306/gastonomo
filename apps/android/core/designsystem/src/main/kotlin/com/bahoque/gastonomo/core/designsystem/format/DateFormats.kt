package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.time.LocalDate
import java.time.YearMonth

/**
 * Las cinco formas en las que el diseno ensena una fecha. Los nombres salen de
 * [CalendarNames], asi que son identicos a los del prototipo en los dos idiomas.
 */
class DateFormats internal constructor(private val language: AppLanguage) {

    private val names = CalendarNames.of(language)

    /** "22 sept 2026". La fecha de creacion de un prestamo y la de un abono. */
    fun long(date: LocalDate): String =
        "${date.dayOfMonth} ${names.monthsShort[date.monthValue - 1]} ${date.year}"

    /** "22 sept". La fecha dentro del subtitulo de una fila de movimiento. */
    fun short(date: LocalDate): String =
        "${date.dayOfMonth} ${names.monthsShort[date.monthValue - 1]}"

    /** "Septiembre 2026". El label del selector de mes. */
    fun monthAndYear(month: YearMonth): String =
        capitalize(names.monthsLong[month.monthValue - 1]) + " " + month.year

    /**
     * El mes dentro de una frase: "Balance de septiembre" en castellano y "September
     * balance" en ingles.
     *
     * Va en minuscula en castellano porque asi lo escribe el prototipo y asi se escribe un
     * mes dentro de una frase; en ingles el mes es un nombre propio y lleva mayuscula. El
     * selector de mes es otra cosa: ahi el mes abre la etiqueta y usa [monthAndYear].
     */
    fun monthName(month: YearMonth): String {
        val name = names.monthsLong[month.monthValue - 1]
        return if (language == AppLanguage.ENGLISH) capitalize(name) else name
    }

    /** "Martes, 22 sept". La fecha del header de Inicio. */
    fun weekdayWithDate(date: LocalDate): String =
        capitalize(weekdayOf(date)) + ", " + short(date)

    /**
     * El encabezado de un grupo de dia en Movimientos: "Hoy", "Ayer" o
     * "Martes 22 sept".
     */
    fun dayGroup(date: LocalDate, today: LocalDate, todayLabel: String, yesterdayLabel: String): String = when {
        date == today -> todayLabel
        date == today.minusDays(1) -> yesterdayLabel
        else -> capitalize(weekdayOf(date)) + " " + short(date)
    }

    /** "22 ago – 22 sept 2026". El label del selector cuando hay un rango activo. */
    fun range(from: LocalDate, to: LocalDate): String = "${short(from)} – ${long(to)}"

    private fun weekdayOf(date: LocalDate): String =
        names.weekdaysLong[date.dayOfWeek.value % DAYS_IN_A_WEEK]

    private fun capitalize(text: String): String =
        text.replaceFirstChar { it.uppercase() }

    private companion object {
        const val DAYS_IN_A_WEEK = 7
    }
}
