package com.bahoque.gastonomo.core.application.state

import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.period.PeriodPreset
import com.bahoque.gastonomo.core.testing.fake.FixedClock
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeriodFilterTest {

    @Test
    fun `it starts on the month of today`() {
        val filter = PeriodFilter.startingOn(TODAY)
        assertEquals(Period.Month(YearMonth.of(2026, 9)), filter.selection.value.period)
        assertFalse(filter.selection.value.isRange)
    }

    @Test
    fun `the same filter is seen by everyone who holds it`() {
        val filter = PeriodFilter.startingOn(TODAY)
        val editor = PeriodFilterEditor(filter, CLOCK)
        editor.previousMonth()
        assertEquals(Period.Month(YearMonth.of(2026, 8)), filter.selection.value.period)
    }

    @Test
    fun `the month selector does not go past the current month`() {
        val filter = PeriodFilter.startingOn(TODAY)
        PeriodFilterEditor(filter, CLOCK).nextMonthOrClearRange()
        assertEquals(Period.Month(YearMonth.of(2026, 9)), filter.selection.value.period)
        assertFalse(filter.selection.value.canGoForward(TODAY))
    }

    @Test
    fun `going back and forward again returns to the same month`() {
        val filter = PeriodFilter.startingOn(TODAY)
        val editor = PeriodFilterEditor(filter, CLOCK)
        editor.previousMonth()
        assertTrue(filter.selection.value.canGoForward(TODAY))
        editor.nextMonthOrClearRange()
        assertEquals(Period.Month(YearMonth.of(2026, 9)), filter.selection.value.period)
    }

    @Test
    fun `with a range active the arrow returns to the current month`() {
        val filter = PeriodFilter.startingOn(TODAY)
        val editor = PeriodFilterEditor(filter, CLOCK)
        editor.openRangeSheet()
        editor.selectPreset(PeriodPreset.LAST_3_MONTHS)
        editor.applyRange()
        assertTrue(filter.selection.value.isRange)
        editor.nextMonthOrClearRange()
        assertEquals(Period.Month(YearMonth.of(2026, 9)), filter.selection.value.period)
    }

    @Test
    fun `a preset fills the draft and applying it changes the period`() {
        val filter = PeriodFilter.startingOn(TODAY)
        val editor = PeriodFilterEditor(filter, CLOCK)
        editor.openRangeSheet()
        editor.selectPreset(PeriodPreset.LAST_MONTH)
        assertEquals(PeriodPreset.LAST_MONTH, editor.draft.value?.preset)
        editor.applyRange()
        assertNull(editor.draft.value)
        assertEquals(Period.Range(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 31)), filter.selection.value.period)
    }

    @Test
    fun `editing a date by hand deselects the preset`() {
        val editor = PeriodFilterEditor(PeriodFilter.startingOn(TODAY), CLOCK)
        editor.openRangeSheet()
        editor.selectPreset(PeriodPreset.THIS_YEAR)
        editor.editFrom(LocalDate.of(2026, 3, 1))
        assertNull(editor.draft.value?.preset)
        assertEquals(LocalDate.of(2026, 3, 1), editor.draft.value?.from)
        editor.editTo(LocalDate.of(2026, 4, 1))
        assertEquals(LocalDate.of(2026, 4, 1), editor.draft.value?.to)
    }

    @Test
    fun `a range that ends before it starts is not applied`() {
        val filter = PeriodFilter.startingOn(TODAY)
        val editor = PeriodFilterEditor(filter, CLOCK)
        editor.openRangeSheet()
        editor.editFrom(LocalDate.of(2026, 9, 20))
        editor.editTo(LocalDate.of(2026, 9, 10))
        editor.applyRange()
        assertFalse(filter.selection.value.isRange)
    }

    @Test
    fun `closing the sheet throws the draft away`() {
        val editor = PeriodFilterEditor(PeriodFilter.startingOn(TODAY), CLOCK)
        editor.openRangeSheet()
        editor.closeRangeSheet()
        assertNull(editor.draft.value)
        editor.applyRange()
        assertNull(editor.draft.value)
    }

    @Test
    fun `editing a date with the sheet closed does nothing`() {
        val editor = PeriodFilterEditor(PeriodFilter.startingOn(TODAY), CLOCK)
        editor.editFrom(LocalDate.of(2026, 1, 1))
        assertNull(editor.draft.value)
    }

    @Test
    fun `the month of a range is the month it starts in`() {
        val selection = PeriodSelection(
            Period.Range(LocalDate.of(2026, 8, 22), LocalDate.of(2026, 9, 22)),
        )
        assertEquals(YearMonth.of(2026, 8), selection.month)
        assertEquals(YearMonth.of(2026, 7), selection.previousMonth().month)
    }

    private companion object {
        val TODAY: LocalDate = FixedClock.PROTOTYPE_TODAY
        val CLOCK = FixedClock()
    }
}
