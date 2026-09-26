package com.bahoque.gastonomo.core.designsystem.format

import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import java.time.LocalDate
import java.time.YearMonth
import kotlin.test.Test
import kotlin.test.assertEquals

class PeriodLabelsTest {

    @Test
    fun `a month reads as its name and year`() {
        val label = GastonomoFormats().periodLabel(Period.Month(YearMonth.of(2026, 9)))
        assertEquals("Septiembre 2026", label)
    }

    @Test
    fun `a range reads from day to day`() {
        val period = Period.Range(LocalDate.of(2026, 8, 22), LocalDate.of(2026, 9, 22))
        assertEquals("22 ago – 22 sept 2026", GastonomoFormats().periodLabel(period))
    }

    @Test
    fun `in english the month keeps the english name`() {
        val formats = GastonomoFormats(language = AppLanguage.ENGLISH)
        assertEquals("September 2026", formats.periodLabel(Period.Month(YearMonth.of(2026, 9))))
    }
}
