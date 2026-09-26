package com.bahoque.gastonomo.feature.export

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.bahoque.gastonomo.core.designsystem.GastonomoStrings
import com.bahoque.gastonomo.core.designsystem.icon.GastonomoIcons
import com.bahoque.gastonomo.core.designsystem.theme.GastonomoTheme

/** El titulo, el icono y el color de cada formato, que el diseno fija uno a uno. */
internal fun ExportFormat.titleRes(): Int = when (this) {
    ExportFormat.XLSX -> R.string.export_format_xlsx
    ExportFormat.CSV -> R.string.export_format_csv
}

internal fun ExportFormat.hintRes(): Int = when (this) {
    ExportFormat.XLSX -> R.string.export_format_xlsx_hint
    ExportFormat.CSV -> R.string.export_format_csv_hint
}

internal fun ExportFormat.icon(): String = when (this) {
    ExportFormat.XLSX -> GastonomoIcons.TABLE_VIEW
    ExportFormat.CSV -> GastonomoIcons.DATA_OBJECT
}

@Composable
internal fun ExportFormat.color(): Color = when (this) {
    ExportFormat.XLSX -> GastonomoTheme.colors.green
    ExportFormat.CSV -> GastonomoTheme.colors.csvGrey
}

internal fun ExportFormat.buttonRes(): Int = when (this) {
    ExportFormat.XLSX -> R.string.export_do_xlsx
    ExportFormat.CSV -> R.string.export_do_csv
}

internal fun ExportSet.titleRes(): Int = when (this) {
    ExportSet.MOVEMENTS -> R.string.export_set_movements
    ExportSet.LOANS -> R.string.export_set_loans
    ExportSet.PAYMENTS -> R.string.export_set_payments
}

internal fun ExportSet.icon(): String = when (this) {
    ExportSet.MOVEMENTS -> GastonomoIcons.SWAP_VERT
    ExportSet.LOANS -> GastonomoIcons.HANDSHAKE
    ExportSet.PAYMENTS -> GastonomoIcons.ADD_CARD
}

/** El nombre del archivo que ensena la caja de resumen. */
@Composable
internal fun fileNameOf(state: ExportUiState): String {
    val sets = state.selectedSets
    return when {
        sets.isEmpty() -> stringResource(R.string.export_pick_one)
        state.format == ExportFormat.XLSX -> state.workbookFileName()
        sets.size == 1 -> state.csvFileName(sets.first())
        else -> pluralStringResource(R.plurals.export_files_csv, sets.size, sets.size)
    }
}

/** La nota que explica que lleva el archivo. */
@Composable
internal fun noteOf(state: ExportUiState): String {
    val sets = state.selectedSets
    val names = sets.map { stringResource(it.titleRes()) }.joinToString(SET_SEPARATOR)
    return when {
        sets.isEmpty() -> ""
        state.format == ExportFormat.XLSX ->
            pluralStringResource(R.plurals.export_note_xlsx, sets.size, sets.size, names)
        sets.size == 1 -> stringResource(R.string.export_note_csv_one)
        else -> stringResource(R.string.export_note_csv_many)
    }
}

/** Los textos que acaban dentro del archivo, resueltos con los recursos de la pantalla. */
@Composable
internal fun exportLabels(displayCurrency: String): ExportLabels = ExportLabels(
    movements = ExportTableLabels(
        name = stringResource(R.string.export_set_movements),
        headers = listOf(
            stringResource(R.string.export_column_date),
            stringResource(R.string.export_column_type),
            stringResource(R.string.export_column_category),
            stringResource(R.string.export_column_description),
            stringResource(R.string.export_column_original_amount),
            stringResource(R.string.export_column_currency),
            stringResource(R.string.export_column_amount, displayCurrency),
        ),
    ),
    loans = ExportTableLabels(
        name = stringResource(R.string.export_set_loans),
        headers = listOf(
            stringResource(R.string.export_column_date),
            stringResource(R.string.export_column_person),
            stringResource(R.string.export_column_direction),
            stringResource(R.string.export_column_note),
            stringResource(R.string.export_column_original_amount),
            stringResource(R.string.export_column_currency),
            stringResource(R.string.export_column_amount, displayCurrency),
            stringResource(R.string.export_column_paid, displayCurrency),
            stringResource(R.string.export_column_outstanding, displayCurrency),
            stringResource(R.string.export_column_status),
        ),
    ),
    payments = ExportTableLabels(
        name = stringResource(R.string.export_set_payments),
        headers = listOf(
            stringResource(R.string.export_column_date),
            stringResource(R.string.export_column_person),
            stringResource(R.string.export_column_label),
            stringResource(R.string.export_column_original_amount),
            stringResource(R.string.export_column_currency),
            stringResource(R.string.export_column_amount, displayCurrency),
        ),
    ),
    income = stringResource(GastonomoStrings.kindIncome),
    expense = stringResource(GastonomoStrings.kindExpense),
    owedToMe = stringResource(GastonomoStrings.directionOwedToMe),
    iOwe = stringResource(GastonomoStrings.directionIOwe),
    paid = stringResource(GastonomoStrings.loanStatusPaid),
    partial = stringResource(GastonomoStrings.loanStatusPartial),
    pending = stringResource(GastonomoStrings.loanStatusPending),
    payment = stringResource(R.string.export_payment),
)

private const val SET_SEPARATOR = ", "
