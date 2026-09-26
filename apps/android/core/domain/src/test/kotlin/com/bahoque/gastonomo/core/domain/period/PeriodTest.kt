package com.bahoque.gastonomo.core.domain.period

import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PeriodTest {

    @Test
    fun `a month covers from its first day to its last`() {
        val month = Period.Month(YearMonth.of(2026, 9))
        assertEquals(LocalDate.of(2026, 9, 1), month.from)
        assertEquals(LocalDate.of(2026, 9, 30), month.to)
    }

    @Test
    fun `the current month is the one of today`() {
        assertEquals(Period.Month(YearMonth.of(2026, 9)), Period.currentMonth(TODAY))
    }

    @Test
    fun `a range starts before it ends`() {
        assertFailsWith<IllegalArgumentException> {
            Period.Range(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 1))
        }
    }

    @Test
    fun `a range of one day is valid`() {
        val range = Period.Range(TODAY, TODAY)
        assertEquals(TODAY, range.from)
        assertEquals(TODAY, range.to)
    }

    @Test
    fun `this month goes from the first to the last day`() {
        val range = PeriodPreset.THIS_MONTH.rangeOn(TODAY)
        assertEquals(LocalDate.of(2026, 9, 1), range.from)
        assertEquals(LocalDate.of(2026, 9, 30), range.to)
    }

    @Test
    fun `last month is the whole previous month`() {
        val range = PeriodPreset.LAST_MONTH.rangeOn(TODAY)
        assertEquals(LocalDate.of(2026, 8, 1), range.from)
        assertEquals(LocalDate.of(2026, 8, 31), range.to)
    }

    @Test
    fun `the last thirty days end today`() {
        val range = PeriodPreset.LAST_30_DAYS.rangeOn(TODAY)
        assertEquals(LocalDate.of(2026, 8, 24), range.from)
        assertEquals(TODAY, range.to)
    }

    @Test
    fun `the last three months end today`() {
        val range = PeriodPreset.LAST_3_MONTHS.rangeOn(TODAY)
        assertEquals(LocalDate.of(2026, 6, 22), range.from)
        assertEquals(TODAY, range.to)
    }

    @Test
    fun `this year starts on the first of january`() {
        val range = PeriodPreset.THIS_YEAR.rangeOn(TODAY)
        assertEquals(LocalDate.of(2026, 1, 1), range.from)
        assertEquals(TODAY, range.to)
    }

    private companion object {
        /** El "hoy" del prototipo. */
        val TODAY: LocalDate = LocalDate.of(2026, 9, 22)
    }
}
