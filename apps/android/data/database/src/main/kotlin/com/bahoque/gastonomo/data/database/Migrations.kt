package com.bahoque.gastonomo.data.database

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * Las migraciones del esquema local, una por cada subida de version.
 *
 * No hay `fallbackToDestructiveMigration` en ninguna parte: si algun dia falta una migracion es
 * mejor que la aplicacion no arranque en desarrollo a que borre el historico de alguien en
 * produccion. Cada entrada de esta lista tiene su test, que la ejecuta sobre una base de la
 * version anterior con datos dentro.
 */
internal val MIGRATIONS: List<Migration> = listOf(MigrationFrom1To2)

/** Las anade una a una. Un `addMigrations(*MIGRATIONS)` copiaria el array en cada llamada. */
internal fun <T : RoomDatabase> RoomDatabase.Builder<T>.addGastonomoMigrations():
    RoomDatabase.Builder<T> = MIGRATIONS.fold(this) { builder, migration ->
    builder.addMigrations(migration)
}

/**
 * Version 2: las escrituras que el usuario hizo sin conexion.
 *
 * Anade la marca de pendiente a los tres registros de dinero y la cola de subida. Las columnas
 * nuevas llevan `DEFAULT 0`, asi que lo que ya estaba guardado queda como lo que es: subido.
 */
internal object MigrationFrom1To2 : Migration(1, 2) {

    override fun migrate(connection: SQLiteConnection) {
        PENDING_COLUMNS.forEach { table ->
            connection.execSQL("ALTER TABLE `$table` ADD COLUMN `pending` INTEGER NOT NULL DEFAULT 0")
        }
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `pending_writes` (`id` TEXT NOT NULL, `kind` TEXT NOT NULL, " +
                "`payload` TEXT NOT NULL, `idempotency_key` TEXT NOT NULL, `loan_id` TEXT, " +
                "`depends_on` TEXT, `queued_at` INTEGER NOT NULL, `attempts` INTEGER NOT NULL, " +
                "`failure` TEXT, PRIMARY KEY(`id`))",
        )
        connection.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_pending_writes_queued_at` " +
                "ON `pending_writes` (`queued_at`)",
        )
    }

    private val PENDING_COLUMNS = listOf("movements", "loans", "loan_payments")
}
