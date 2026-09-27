package com.bahoque.gastonomo.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * El esquema exportado, leido tal cual se versiona.
 *
 * Lo que comprueba es la regla 1 de `CLAUDE.md` en el sitio donde se rompe callada: **ninguna
 * columna de dinero puede ser `REAL`**. Un `REAL` es un `double`, y un importe en un double parece
 * correcto hasta que un dia un saldo no cuadra. Lo mismo con las fechas de registro: si alguna
 * acabara en `INTEGER` seria un epoch, y un epoch mueve el movimiento de dia segun donde este el
 * usuario.
 *
 * Se lee el JSON y no las clases porque es el JSON lo que describe la tabla de verdad.
 */
// Corre bajo Robolectric por org.json: el del android.jar de los tests unitarios devuelve null
// en vez de leer nada, y el test pasaria sin haber mirado el esquema.
@RunWith(AndroidJUnit4::class)
class ExportedSchemaTest {

    @Test
    fun `no column is REAL in any version ever exported`() {
        // Todas las versiones, no solo la ultima: una que hubiera guardado dinero en REAL seguiria
        // siendo de donde migra quien todavia la tenga.
        val offenders = (1..GastonomoDatabase.VERSION).flatMap { version ->
            columnsOf(schemaOf(version)).filter { it.affinity == "REAL" }
        }

        assertTrue(offenders.isEmpty(), "columnas REAL: ${offenders.map { "${it.table}.${it.name}" }}")
    }

    @Test
    fun `every money column is TEXT`() {
        val money = columns().filter { it.name in MONEY_COLUMNS }

        assertTrue(money.isNotEmpty(), "no se encontro ninguna columna de dinero")
        money.forEach { assertEquals("TEXT", it.affinity, "${it.table}.${it.name}") }
    }

    @Test
    fun `the dates of record are TEXT, not an epoch`() {
        val dates = columns().filter { it.name in DATE_COLUMNS }

        assertTrue(dates.isNotEmpty(), "no se encontro ninguna columna de fecha")
        dates.forEach { assertEquals("TEXT", it.affinity, "${it.table}.${it.name}") }
    }

    @Test
    fun `the version of the file is the one the code declares`() {
        assertEquals(GastonomoDatabase.VERSION, schema().getInt("version"))
    }

    @Test
    fun `the indexes the screens need are there`() {
        val indexed = tables().flatMap { table ->
            val name = table.getString("tableName")
            val indices = table.optJSONArray("indices") ?: return@flatMap emptyList()
            (0 until indices.length()).map { name to indices.getJSONObject(it).getString("createSql") }
        }

        INDEXED_COLUMNS.forEach { (table, column) ->
            assertTrue(
                indexed.any { it.first == table && it.second.contains("`$column`") },
                "falta el indice de $table.$column",
            )
        }
    }

    private fun columns(): List<Column> = columnsOf(schema())

    private fun columnsOf(schema: JSONObject): List<Column> = tablesOf(schema).flatMap { table ->
        val name = table.getString("tableName")
        val fields = table.getJSONArray("fields")
        (0 until fields.length()).map { index ->
            val field = fields.getJSONObject(index)
            Column(name, field.getString("columnName"), field.getString("affinity"))
        }
    }

    private fun tables(): List<JSONObject> = tablesOf(schema())

    private fun tablesOf(schema: JSONObject): List<JSONObject> {
        val entities = schema.getJSONArray("entities")
        return (0 until entities.length()).map { entities.getJSONObject(it) }
    }

    private fun schema(): JSONObject = schemaOf(GastonomoDatabase.VERSION)

    private fun schemaOf(version: Int): JSONObject {
        val file = File("$SCHEMA_DIR/$version.json")
        if (!file.exists()) fail("no existe ${file.path}; el esquema tiene que estar versionado")
        return JSONObject(file.readText()).getJSONObject("database")
    }

    private data class Column(val table: String, val name: String, val affinity: String)

    private companion object {
        const val SCHEMA_DIR = "schemas/com.bahoque.gastonomo.data.database.GastonomoDatabase"

        val MONEY_COLUMNS = setOf("amount", "base", "rate")
        val DATE_COLUMNS = setOf("occurred_on", "opened_on", "paid_on", "date")

        /** Lo que las pantallas piden de forma directa. */
        val INDEXED_COLUMNS = listOf(
            "movements" to "occurred_on",
            "loans" to "direction",
            "loan_payments" to "loan_id",
        )
    }
}
