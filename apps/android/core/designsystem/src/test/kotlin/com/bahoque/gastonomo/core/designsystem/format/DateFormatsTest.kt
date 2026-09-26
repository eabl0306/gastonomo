package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

/** Los nombres de meses y dias son los del prototipo, letra por letra. */
class DateFormatsTest {

    private val spanish = DateFormats(AppLanguage.SPANISH)
    private val english = DateFormats(AppLanguage.ENGLISH)

    @Test
    fun `a long date is the day, the short month and the year`() {
        assertEquals("22 sept 2026", spanish.long(TODAY))
        assertEquals("22 Sep 2026", english.long(TODAY))
    }

    @Test
    fun `a short date drops the year`() {
        assertEquals("22 sept", spanish.short(TODAY))
        assertEquals("1 ene", spanish.short(LocalDate.of(2026, 1, 1)))
    }

    @Test
    fun `the month picker capitalises the long month`() {
        assertEquals("Septiembre 2026", spanish.monthAndYear(YearMonth.of(2026, 9)))
        assertEquals("Diciembre 2026", spanish.monthAndYear(YearMonth.of(2026, 12)))
        assertEquals("September 2026", english.monthAndYear(YearMonth.of(2026, 9)))
    }

    @Test
    fun `the balance card uses the month inside a sentence`() {
        assertEquals("septiembre", spanish.monthName(YearMonth.of(2026, 9)))
        assertEquals("September", english.monthName(YearMonth.of(2026, 9)))
    }

    @Test
    fun `the home header is the weekday and the short date`() {
        assertEquals("Martes, 22 sept", spanish.weekdayWithDate(TODAY))
        assertEquals("Tuesday, 22 Sep", english.weekdayWithDate(TODAY))
    }

    @Test
    fun `a sunday is the first weekday of the list`() {
        assertEquals("Domingo, 20 sept", spanish.weekdayWithDate(LocalDate.of(2026, 9, 20)))
    }

    @Test
    fun `the day group says today, yesterday or the weekday`() {
        assertEquals("Hoy", spanish.dayGroup(TODAY, TODAY, "Hoy", "Ayer"))
        assertEquals("Ayer", spanish.dayGroup(TODAY.minusDays(1), TODAY, "Hoy", "Ayer"))
        assertEquals(
            "Sábado 19 sept",
            spanish.dayGroup(LocalDate.of(2026, 9, 19), TODAY, "Hoy", "Ayer"),
        )
    }

    @Test
    fun `an active range shows both ends`() {
        assertEquals(
            "22 ago – 22 sept 2026",
            spanish.range(LocalDate.of(2026, 8, 22), TODAY),
        )
    }

    private companion object {
        val TODAY: LocalDate = LocalDate.of(2026, 9, 22)
    }
}
