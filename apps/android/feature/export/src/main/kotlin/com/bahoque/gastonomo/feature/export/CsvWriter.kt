package com.bahoque.gastonomo.feature.export

import java.math.BigDecimal

/**
 * El CSV del diseno: UTF-8 **con BOM**, separador `;`, saltos CRLF y comillas en los
 * valores que llevan `;`, comilla doble o salto de linea.
 *
 * El BOM y el punto y coma son lo que hace que Excel abra el archivo bien de doble clic en
 * un Windows en castellano; sin ellos las tildes salen rotas y todo cae en una columna.
 *
 * Los numeros van con punto decimal, sin separador de miles y sin ceros de relleno, igual
 * que en el prototipo: es un archivo de datos, no algo que se lea tal cual.
 */
object CsvWriter {

    /** El tipo MIME con el que se comparte un CSV. */
    const val MIME_TYPE = "text/csv"

    fun write(table: ExportTable): ByteArray {
        val text = buildString {
            append(BOM)
            appendRow(table.headers.map { ExportCell.Text(it) })
            table.rows.forEach { appendRow(it) }
        }
        return text.toByteArray(Charsets.UTF_8)
    }

    private fun StringBuilder.appendRow(cells: List<ExportCell>) {
        append(cells.joinToString(SEPARATOR) { escape(it.text()) })
        append(NEWLINE)
    }

    private fun escape(value: String): String {
        val needsQuotes = value.any { it == ';' || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return '"' + value.replace("\"", "\"\"") + '"'
    }

    private const val SEPARATOR = ";"
    private const val NEWLINE = "\r\n"
    private const val BOM = "\uFEFF"
}

/** El texto de una celda: los numeros sin notacion cientifica ni ceros de relleno. */
internal fun ExportCell.text(): String = when (this) {
    is ExportCell.Text -> value
    is ExportCell.Number -> value.plain()
}

private fun BigDecimal.plain(): String =
    if (signum() == 0) "0" else stripTrailingZeros().toPlainString()
