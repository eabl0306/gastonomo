package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanPayment
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.period.Period
import java.time.LocalDate
import java.time.YearMonth

/**
 * Lo que pinta Exportar: el formato, que conjuntos se incluyen, que periodo cubren los
 * movimientos y cuantas filas sale cada conjunto.
 *
 * Los movimientos se piden una sola vez, de todo el historial, y el periodo se filtra en
 * local: "Periodo actual" es un subconjunto de "Todo el historial".
 */
data class ExportUiState(
    val loading: Boolean = true,
    val format: ExportFormat = ExportFormat.XLSX,
    val sets: Set<ExportSet> = ExportSet.entries.toSet(),
    val range: ExportRange = ExportRange.ALL,
    val period: Period = Period.Month(YearMonth.of(INITIAL_YEAR, 1)),
    val today: LocalDate = EXPORT_EPOCH,
    val movements: List<Movement> = emptyList(),
    val loans: List<Loan> = emptyList(),
    val displayCurrency: CurrencyCode = CurrencyCode.COP,
    val rates: ExchangeRates = ExchangeRates.EMPTY,
    val exporting: Boolean = false,
    val payload: ExportPayload? = null,
    val loadFailed: Boolean = false,
    val message: ExportMessage? = null,
) {
    /** Los movimientos que caben en el rango elegido. */
    val movementsInRange: List<Movement>
        get() = when (range) {
            ExportRange.ALL -> movements
            ExportRange.PERIOD -> movements.filter {
                !it.occurredOn.isBefore(period.from) && !it.occurredOn.isAfter(period.to)
            }
        }

    /** Todos los abonos de todos los prestamos, con el prestamo al que pertenecen. */
    val payments: List<Pair<Loan, LoanPayment>>
        get() = loans.flatMap { loan -> loan.payments.map { loan to it } }

    /** Cuantas filas sale un conjunto. Es el "{n} filas" de cada checkbox. */
    fun rowsOf(set: ExportSet): Int = when (set) {
        ExportSet.MOVEMENTS -> movementsInRange.size
        ExportSet.LOANS -> loans.size
        ExportSet.PAYMENTS -> payments.size
    }

    /** Los conjuntos marcados, siempre en el orden del diseno. */
    val selectedSets: List<ExportSet> get() = ExportSet.entries.filter { it in sets }

    /** Se puede exportar con al menos un conjunto marcado y sin otra exportacion en curso. */
    val canExport: Boolean get() = sets.isNotEmpty() && !exporting && !loading
}

/**
 * Un ano cualquiera para el periodo inicial. No se ensena nunca: el ViewModel lo sustituye
 * por el mes de hoy en cuanto lee el reloj.
 */
private const val INITIAL_YEAR = 2026

/** Los dos formatos del diseno. */
enum class ExportFormat {
    /** Un libro con una hoja por conjunto. */
    XLSX,

    /** Un archivo de texto por conjunto. */
    CSV,
}

/**
 * Los tres conjuntos exportables, en el orden del diseno.
 *
 * [slug] es lo que va en el nombre del archivo CSV, `gastonomo_{slug}_{fecha}.csv`, con los
 * mismos nombres que usa el prototipo.
 */
enum class ExportSet(val slug: String) {
    MOVEMENTS("movs"),
    LOANS("loans"),
    PAYMENTS("abonos"),
}

/** Que movimientos entran: todos o solo los del periodo. */
enum class ExportRange {
    ALL,
    PERIOD,
}

/** Lo que Exportar cuenta por el toast. */
sealed interface ExportMessage {

    /** True si es un fallo: el toast cambia el icono en vez de mentir con un check. */
    val isError: Boolean get() = false

    /** "Exportacion lista: {n} filas". */
    data class Ready(val rows: Int) : ExportMessage

    /** No se pudo escribir o compartir el archivo. */
    data object Failed : ExportMessage {
        override val isError: Boolean get() = true
    }
}
