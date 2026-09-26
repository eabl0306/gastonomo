package com.bahoque.gastonomo.feature.export

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.loan.Loan
import com.bahoque.gastonomo.core.domain.loan.LoanSummary
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.movement.Movement
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Las transformaciones de estado de Exportar, fuera del ViewModel: son funciones puras y
 * se leen, y se prueban, sin montar el ViewModel entero.
 */

internal fun ExportUiState.withSettings(
    result: Outcome<UserSettings, ApiError>,
): ExportUiState = when (result) {
    is Outcome.Success -> copy(displayCurrency = result.value.displayCurrency)
    is Outcome.Failure -> copy(loadFailed = true)
}

/**
 * Sin tasas no se puede convertir, pero si exportar: los registros que ya estan en la
 * moneda principal salen bien igualmente. Se marca el fallo y se sigue.
 */
internal fun ExportUiState.withRates(
    result: Outcome<ExchangeRates, ApiError>,
): ExportUiState = when (result) {
    is Outcome.Success -> copy(rates = result.value)
    is Outcome.Failure -> copy(loadFailed = true)
}

/** El caso de uso devuelve cada prestamo con su resumen; aqui solo hace falta el prestamo. */
internal fun ExportUiState.withLoans(
    result: Outcome<List<Pair<Loan, LoanSummary>>, ApiError>,
): ExportUiState = when (result) {
    is Outcome.Success -> copy(loans = result.value.map { (loan, _) -> loan })
    is Outcome.Failure -> copy(loadFailed = true)
}

internal fun ExportUiState.withMovements(
    result: Outcome<List<Movement>, ApiError>,
): ExportUiState = when (result) {
    is Outcome.Success -> copy(movements = result.value)
    is Outcome.Failure -> copy(loadFailed = true)
}

/**
 * Los archivos que hay que generar con lo que esta marcado: un libro con una hoja por
 * conjunto, o un CSV por conjunto.
 */
internal fun ExportUiState.buildPayload(labels: ExportLabels): ExportPayload {
    val builder = ExportTables(labels, displayCurrency, rates)
    val tables = selectedSets.map { it to builder.of(it, this) }
    val rows = tables.sumOf { (_, table) -> table.rows.size }
    val documents = when (format) {
        ExportFormat.XLSX -> listOf(
            ExportDocument(
                fileName = workbookFileName(),
                mimeType = XlsxWriter.MIME_TYPE,
                bytes = XlsxWriter.write(tables.map { (_, table) -> table }),
            ),
        )
        ExportFormat.CSV -> tables.map { (set, table) ->
            ExportDocument(
                fileName = csvFileName(set),
                mimeType = CsvWriter.MIME_TYPE,
                bytes = CsvWriter.write(table),
            )
        }
    }
    return ExportPayload(documents = documents, rows = rows)
}

/** `gastonomo_{YYYY-MM-DD}.xlsx`. */
internal fun ExportUiState.workbookFileName(): String = "$PREFIX$today.xlsx"

/** `gastonomo_{conjunto}_{YYYY-MM-DD}.csv`. */
internal fun ExportUiState.csvFileName(set: ExportSet): String =
    "$PREFIX${set.slug}_$today.csv"

private const val PREFIX = "gastonomo_"
