package com.bahoque.gastonomo.feature.export

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Un `.xlsx` de verdad, escrito a mano con `java.util.zip`.
 *
 * Un xlsx es un zip con unos pocos XML dentro, y eso es exactamente lo que se genera aqui:
 * el prototipo produce un `.xls` de SpreadsheetML, que Excel abre con un aviso y Google
 * Sheets no abre. La alternativa era Apache POI, que son varios megabytes de biblioteca y
 * metodos que Android no trae, para escribir cinco ficheros de texto.
 *
 * El texto va como `inlineStr`, sin tabla de cadenas compartidas: es mas largo en bytes y
 * mucho mas corto en codigo, y una exportacion personal no llega al tamano en el que eso
 * importa.
 */
object XlsxWriter {

    /** El tipo MIME de un libro de Excel moderno. */
    const val MIME_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

    fun write(tables: List<ExportTable>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            zip.put("[Content_Types].xml", contentTypes(tables.size))
            zip.put("_rels/.rels", ROOT_RELS)
            zip.put("xl/workbook.xml", workbook(tables))
            zip.put("xl/_rels/workbook.xml.rels", workbookRels(tables.size))
            zip.put("xl/styles.xml", STYLES)
            tables.forEachIndexed { index, table ->
                zip.put("xl/worksheets/sheet${index + 1}.xml", sheet(table))
            }
        }
        return output.toByteArray()
    }

    private fun ZipOutputStream.put(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray(Charsets.UTF_8))
        closeEntry()
    }

    private fun contentTypes(sheets: Int): String = buildString {
        append(HEADER)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"$RELS_TYPE\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"$WORKBOOK_TYPE\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"$STYLES_TYPE\"/>")
        repeat(sheets) {
            append(
                "<Override PartName=\"/xl/worksheets/sheet${it + 1}.xml\" " +
                    "ContentType=\"$SHEET_TYPE\"/>",
            )
        }
        append("</Types>")
    }

    private fun workbook(tables: List<ExportTable>): String = buildString {
        append(HEADER)
        append("<workbook xmlns=\"$MAIN_NS\" xmlns:r=\"$RELS_NS\"><sheets>")
        tables.forEachIndexed { index, table ->
            append(
                "<sheet name=\"${escape(sheetName(table.name))}\" " +
                    "sheetId=\"${index + 1}\" r:id=\"rId${index + 1}\"/>",
            )
        }
        append("</sheets></workbook>")
    }

    private fun workbookRels(sheets: Int): String = buildString {
        append(HEADER)
        append("<Relationships xmlns=\"$PACKAGE_RELS_NS\">")
        repeat(sheets) {
            append(
                "<Relationship Id=\"rId${it + 1}\" Type=\"$SHEET_REL\" " +
                    "Target=\"worksheets/sheet${it + 1}.xml\"/>",
            )
        }
        append("<Relationship Id=\"rId${sheets + 1}\" Type=\"$STYLES_REL\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun sheet(table: ExportTable): String = buildString {
        append(HEADER)
        append("<worksheet xmlns=\"$MAIN_NS\"><sheetData>")
        append(row(1, table.headers.map { ExportCell.Text(it) }, bold = true))
        table.rows.forEachIndexed { index, cells -> append(row(index + 2, cells, bold = false)) }
        append("</sheetData></worksheet>")
    }

    private fun row(number: Int, cells: List<ExportCell>, bold: Boolean): String = buildString {
        append("<row r=\"$number\">")
        cells.forEachIndexed { index, cell ->
            val reference = columnName(index) + number
            val style = if (bold) " s=\"1\"" else ""
            when (cell) {
                is ExportCell.Number ->
                    append("<c r=\"$reference\"$style><v>${cell.text()}</v></c>")
                is ExportCell.Text -> append(
                    "<c r=\"$reference\"$style t=\"inlineStr\"><is><t xml:space=\"preserve\">" +
                        "${escape(cell.value)}</t></is></c>",
                )
            }
        }
        append("</row>")
    }

    /** A, B, ... Z, AA, AB. Ninguna tabla del diseno pasa de la J, pero la regla es general. */
    internal fun columnName(index: Int): String {
        var rest = index
        val name = StringBuilder()
        do {
            name.insert(0, 'A' + rest % LETTERS)
            rest = rest / LETTERS - 1
        } while (rest >= 0)
        return name.toString()
    }

    /** Excel no admite mas de 31 caracteres ni estos signos en el nombre de una hoja. */
    internal fun sheetName(name: String): String =
        name.filterNot { it in FORBIDDEN }.take(MAX_SHEET_NAME)

    private fun escape(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    private const val LETTERS = 26
    private const val MAX_SHEET_NAME = 31
    private const val FORBIDDEN = "[]:*?/\\"
    private const val HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
    private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val RELS_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val PACKAGE_RELS_NS = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val RELS_TYPE = "application/vnd.openxmlformats-package.relationships+xml"
    private const val WORKBOOK_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"
    private const val SHEET_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"
    private const val STYLES_TYPE =
        "application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"
    private const val SHEET_REL = "$RELS_NS/worksheet"
    private const val STYLES_REL = "$RELS_NS/styles"
    private const val ROOT_RELS = "$HEADER<Relationships xmlns=\"$PACKAGE_RELS_NS\">" +
        "<Relationship Id=\"rId1\" Type=\"$RELS_NS/officeDocument\" Target=\"xl/workbook.xml\"/>" +
        "</Relationships>"

    /** Dos fuentes: la normal y la negrita de los encabezados, que es el estilo 1. */
    private const val STYLES = "$HEADER<styleSheet xmlns=\"$MAIN_NS\">" +
        "<fonts count=\"2\"><font/><font><b/></font></fonts>" +
        "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>" +
        "<fill><patternFill patternType=\"gray125\"/></fill></fills>" +
        "<borders count=\"1\"><border/></borders>" +
        "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/>" +
        "</cellStyleXfs>" +
        "<cellXfs count=\"2\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +
        "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +
        "</cellXfs></styleSheet>"
}
