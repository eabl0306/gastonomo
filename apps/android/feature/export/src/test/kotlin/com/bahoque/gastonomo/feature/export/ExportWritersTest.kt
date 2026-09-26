package com.bahoque.gastonomo.feature.export

import org.junit.Test
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** El CSV y el xlsx que genera el cliente, comprobados sobre el archivo de verdad. */
class ExportWritersTest {

    @Test
    fun `the csv carries the bom, the semicolons and the crlf of the design`() {
        val bytes = CsvWriter.write(table)
        val text = String(bytes, Charsets.UTF_8)
        assertTrue(text.startsWith("﻿"))
        assertEquals(
            listOf("﻿Fecha;Tipo;Monto", "2026-09-15;Gasto;-186400", ""),
            text.split("\r\n"),
        )
    }

    @Test
    fun `a value with a semicolon, a quote or a newline is quoted`() {
        val quoted = CsvWriter.write(
            ExportTable(
                name = "x",
                headers = listOf("a"),
                rows = listOf(
                    listOf(ExportCell.Text("uno;dos")),
                    listOf(ExportCell.Text("di \"hola\"")),
                    listOf(ExportCell.Text("dos\nlineas")),
                ),
            ),
        )
        val lines = String(quoted, Charsets.UTF_8).removePrefix("﻿").split("\r\n")
        assertEquals("\"uno;dos\"", lines[1])
        assertEquals("\"di \"\"hola\"\"\"", lines[2])
        assertTrue(lines[3].startsWith("\"dos"))
    }

    @Test
    fun `the xlsx is a zip with one sheet per set`() {
        val entries = entriesOf(XlsxWriter.write(listOf(table, table.copy(name = "Préstamos"))))
        assertTrue("[Content_Types].xml" in entries)
        assertTrue("xl/workbook.xml" in entries)
        assertTrue("xl/styles.xml" in entries)
        assertTrue("xl/worksheets/sheet1.xml" in entries)
        assertTrue("xl/worksheets/sheet2.xml" in entries)
    }

    @Test
    fun `every cell of the sheet lands where it has to`() {
        val sheet = fileOf(XlsxWriter.write(listOf(table)), "xl/worksheets/sheet1.xml")
        assertTrue("<c r=\"A1\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Fecha" in sheet)
        assertTrue("<c r=\"C1\" s=\"1\" t=\"inlineStr\"><is><t xml:space=\"preserve\">Monto" in sheet)
        assertTrue("<c r=\"A2\" t=\"inlineStr\"><is><t xml:space=\"preserve\">2026-09-15" in sheet)
        assertTrue("<c r=\"C2\"><v>-186400</v></c>" in sheet)
    }

    @Test
    fun `the workbook names its sheets`() {
        val workbook = fileOf(XlsxWriter.write(listOf(table)), "xl/workbook.xml")
        assertTrue("name=\"Movimientos\"" in workbook)
    }

    @Test
    fun `the header row is the only one in bold`() {
        val sheet = fileOf(XlsxWriter.write(listOf(table)), "xl/worksheets/sheet1.xml")
        assertEquals(table.headers.size, Regex("s=\"1\"").findAll(sheet).count())
    }

    @Test
    fun `the text that would break the xml is escaped`() {
        val sheet = fileOf(
            XlsxWriter.write(
                listOf(table.copy(rows = listOf(listOf(ExportCell.Text("Pan & <té>"))))),
            ),
            "xl/worksheets/sheet1.xml",
        )
        assertTrue("Pan &amp; &lt;té&gt;" in sheet)
    }

    @Test
    fun `column letters go past the alphabet`() {
        assertEquals("A", XlsxWriter.columnName(0))
        assertEquals("Z", XlsxWriter.columnName(25))
        assertEquals("AA", XlsxWriter.columnName(26))
        assertEquals("AB", XlsxWriter.columnName(27))
    }

    @Test
    fun `a sheet name is cut and cleaned for excel`() {
        assertEquals("Movimientos", XlsxWriter.sheetName("Movimientos"))
        assertEquals("ab", XlsxWriter.sheetName("a[b]"))
        assertEquals(31, XlsxWriter.sheetName("x".repeat(40)).length)
    }

    private fun entriesOf(bytes: ByteArray): List<String> {
        val names = mutableListOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                names += entry.name
                entry = zip.nextEntry
            }
        }
        return names
    }

    private fun fileOf(bytes: ByteArray, name: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) return String(zip.readBytes(), Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        error("The generated file has no $name")
    }

    private val table = ExportTable(
        name = "Movimientos",
        headers = listOf("Fecha", "Tipo", "Monto"),
        rows = listOf(
            listOf(
                ExportCell.Text("2026-09-15"),
                ExportCell.Text("Gasto"),
                ExportCell.Number(BigDecimal("-186400")),
            ),
        ),
    )
}
