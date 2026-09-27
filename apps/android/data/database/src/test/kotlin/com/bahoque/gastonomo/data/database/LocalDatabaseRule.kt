package com.bahoque.gastonomo.data.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import org.junit.rules.ExternalResource

/**
 * Una base de datos en memoria por test, con las claves ajenas activas.
 *
 * En memoria y no en fichero porque lo que se prueba aqui es el esquema y las consultas, no la
 * persistencia entre arranques: eso lo cubre el test de migracion en cuanto haya una version 2.
 */
internal class LocalDatabaseRule : ExternalResource() {

    lateinit var database: GastonomoDatabase
        private set

    override fun before() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, GastonomoDatabase::class.java).build()
    }

    override fun after() {
        database.close()
    }
}
