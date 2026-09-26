package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.period.Period
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import org.junit.Test
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Las tres tablas del diseno, columna a columna. */
class ExportTablesTest {

    private val state = loadedExportState()
    private val tables = ExportTables(spanishLabels, CurrencyCode.COP, SampleFinance.rates)

    @Test
    fun `the movement columns are the ones in the design`() {
        val table = tables.of(ExportSet.MOVEMENTS, state)
        assertEquals(MOVEMENT_HEADERS, table.headers)
    }

    @Test
    fun `an expense is exported as a negative number`() {
        val row = rowOf(ExportSet.MOVEMENTS, "Mercado")
        assertEquals("-186400", row[ORIGINAL].text())
        assertEquals("-186400", row[SHOWN].text())
        assertEquals("Gasto", row[KIND].text())
    }

    @Test
    fun `an income keeps its sign`() {
        val row = rowOf(ExportSet.MOVEMENTS, "Salario")
        assertEquals("2800000", row[SHOWN].text())
        assertEquals("Ingreso", row[KIND].text())
    }

    @Test
    fun `a record in another currency keeps its own amount and its equivalent`() {
        val row = rowOf(ExportSet.MOVEMENTS, "Diseño web · cliente EE. UU.")
        assertEquals("140", row[ORIGINAL].text())
        assertEquals("USD", row[CURRENCY].text())
        assertEquals("450000", row[SHOWN].text())
    }

    @Test
    fun `movements are exported oldest first`() {
        val dates = tables.of(ExportSet.MOVEMENTS, state).rows.map { it.first().text() }
        assertEquals(dates.sorted(), dates)
    }

    @Test
    fun `a loan carries its direction, what is paid and what is left`() {
        val row = tables.of(ExportSet.LOANS, state).rows.first { it[1].text() == "Collar Johana" }
        assertEquals("Me debe", row[2].text())
        assertEquals("100000", row[7].text())
        assertEquals("50000", row[8].text())
        assertEquals("Parcial", row[9].text())
    }

    @Test
    fun `a loan with no payments is pending`() {
        val row = tables.of(ExportSet.LOANS, state).rows.first { it[1].text() == "Hermana Evila" }
        assertEquals("Debo", row[2].text())
        assertEquals("Pendiente", row[9].text())
    }

    @Test
    fun `a payment without a concept says just abono`() {
        val rows = tables.of(ExportSet.PAYMENTS, state).rows
        assertTrue(rows.any { it[2].text() == "Abono" })
        assertTrue(rows.any { it[2].text() == "Intereses" })
    }

    @Test
    fun `the period only leaves the movements of the month`() {
        val everything = state.movementsInRange.size
        val period = state.copy(range = ExportRange.PERIOD).movementsInRange.size
        assertEquals(everything, period)
        val narrow = state.copy(
            range = ExportRange.PERIOD,
            period = Period.Range(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20)),
        )
        assertEquals(2, narrow.movementsInRange.size)
    }

    private fun rowOf(set: ExportSet, title: String) =
        tables.of(set, state).rows.first { it[TITLE].text() == title }

    private companion object {
        const val TITLE = 3
        const val KIND = 1
        const val ORIGINAL = 4
        const val CURRENCY = 5
        const val SHOWN = 6
    }
}
