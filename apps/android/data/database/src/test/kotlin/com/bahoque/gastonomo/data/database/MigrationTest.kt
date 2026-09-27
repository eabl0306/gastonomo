package com.bahoque.gastonomo.data.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.assertEquals

/**
 * Que subir de version no se lleve por delante lo que el usuario ya tenia.
 *
 * Es el test que justifica exportar el esquema. Sin el, la primera actualizacion que cambiara una
 * tabla borraria el historico de quien ya tuviera la aplicacion instalada, y nadie se enteraria
 * hasta que pasara en un telefono de verdad.
 *
 * La base de la version anterior se levanta **desde su esquema exportado**, no desde el codigo:
 * el codigo de la version 1 ya no existe, y reconstruirla a mano seria probar contra una copia que
 * se puede quedar vieja. `MigrationTestHelper` hace justo esto, pero lee el esquema de los assets
 * de un APK de instrumentacion, y aqui no hay emulador.
 */
@RunWith(AndroidJUnit4::class)
class MigrationTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val file = File(context.cacheDir, "migration-test.db")

    @After
    fun clean() {
        file.delete()
    }

    @Test
    fun `from 1 to 2 the movements survive and arrive marked as already sent`() = runTest {
        createVersion(1) { db ->
            db.execSQL(INSERT_MOVEMENT)
        }

        val database = openWithMigrations()
        val movement = try {
            database.movements().between(DAY, DAY).single()
        } finally {
            database.close()
        }

        // El importe sigue con sus cuatro decimales y el movimiento queda como lo que es: subido.
        assertEquals("186400.0000", movement.money.amount.toPlainString())
        assertEquals(false, movement.pending)
    }

    @Test
    fun `from 1 to 2 the queue starts empty`() = runTest {
        createVersion(1) { }

        val database = openWithMigrations()
        val pending = try {
            database.pending().count()
        } finally {
            database.close()
        }

        assertEquals(0, pending)
    }

    /** Abrir con Room ejecuta la migracion y **valida el esquema resultante** contra el esperado. */
    private fun openWithMigrations(): GastonomoDatabase =
        Room.databaseBuilder(context, GastonomoDatabase::class.java, file.absolutePath)
            .addGastonomoMigrations()
            .build()

    /** Levanta el fichero tal como lo dejaria la version [version] de la aplicacion. */
    private fun createVersion(version: Int, seed: (SQLiteDatabase) -> Unit) {
        file.delete()
        val schema = schemaOf(version)
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            val entities = schema.getJSONArray("entities")
            (0 until entities.length()).forEach { index ->
                val entity = entities.getJSONObject(index)
                val table = entity.getString("tableName")
                db.execSQL(entity.getString("createSql").replace(TABLE_NAME, table))
                val indices = entity.optJSONArray("indices") ?: return@forEach
                (0 until indices.length()).forEach { at ->
                    db.execSQL(indices.getJSONObject(at).getString("createSql").replace(TABLE_NAME, table))
                }
            }
            // Room reconoce la version por estas dos cosas, asi que sin ellas creeria que la base
            // esta vacia y la crearia de cero en vez de migrarla.
            db.execSQL(CREATE_MASTER)
            db.execSQL(INSERT_MASTER, arrayOf(schema.getString("identityHash")))
            db.version = version
            seed(db)
        }
    }

    private fun schemaOf(version: Int): JSONObject =
        JSONObject(File("$SCHEMA_DIR/$version.json").readText()).getJSONObject("database")

    private companion object {
        const val SCHEMA_DIR = "schemas/com.bahoque.gastonomo.data.database.GastonomoDatabase"
        const val TABLE_NAME = "\${TABLE_NAME}"

        val DAY: java.time.LocalDate = java.time.LocalDate.parse("2026-09-15")

        const val CREATE_MASTER =
            "CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY, identity_hash TEXT)"
        const val INSERT_MASTER =
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash) VALUES(42, ?)"

        val INSERT_MOVEMENT = """
            INSERT INTO movements (id, occurred_on, kind, title, amount, currency, base,
                base_currency, category_id, category_name, category_kind, category_icon,
                category_color, category_is_default, category_movement_count, loan_id,
                loan_payment_id)
            VALUES ('m1', '2026-09-15', 'out', 'Mercado', '186400.0000', 'COP', '186400.0000',
                'COP', 'food', 'Alimentacion', 'out', 'restaurant', '#cf5f35', 1, 12, null, null)
        """.trimIndent()
    }
}
