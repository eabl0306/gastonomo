package com.bahoque.gastonomo.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.bahoque.gastonomo.data.database.dao.AccountDao
import com.bahoque.gastonomo.data.database.dao.CategoryDao
import com.bahoque.gastonomo.data.database.dao.CurrencyDao
import com.bahoque.gastonomo.data.database.dao.LoanDao
import com.bahoque.gastonomo.data.database.dao.MovementDao
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.CurrencyEntity
import com.bahoque.gastonomo.data.database.entity.ExchangeRateEntity
import com.bahoque.gastonomo.data.database.entity.ExchangeRateSnapshotEntity
import com.bahoque.gastonomo.data.database.entity.LoanEntity
import com.bahoque.gastonomo.data.database.entity.LoanPaymentEntity
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import com.bahoque.gastonomo.data.database.entity.ProfileEntity
import com.bahoque.gastonomo.data.database.entity.SettingsEntity

/**
 * La base de datos local. Guarda lo que hoy solo vive en la API para que la aplicacion se pueda
 * abrir sin conexion y ensenarlo todo.
 *
 * **El esquema se exporta y se versiona** (`schemas/`). Empezar sin exportarlo y anadirlo despues
 * significa que la primera actualizacion que cambie una tabla no tiene desde donde migrar y borra
 * los datos de quien ya tenia la aplicacion instalada.
 *
 * No hay `fallbackToDestructiveMigration`. Si algun dia falta una migracion, es mejor que la
 * aplicacion no arranque en desarrollo a que borre el historico de alguien en produccion.
 */
@Database(
    entities = [
        MovementEntity::class,
        LoanEntity::class,
        LoanPaymentEntity::class,
        CategoryEntity::class,
        SettingsEntity::class,
        ProfileEntity::class,
        CurrencyEntity::class,
        ExchangeRateSnapshotEntity::class,
        ExchangeRateEntity::class,
    ],
    version = GastonomoDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
internal abstract class GastonomoDatabase : RoomDatabase() {

    abstract fun movements(): MovementDao

    abstract fun loans(): LoanDao

    abstract fun categories(): CategoryDao

    abstract fun account(): AccountDao

    abstract fun currencies(): CurrencyDao

    companion object {
        /** Sube con cada cambio de esquema, y cada subida trae su migracion y su test. */
        const val VERSION = 1

        /** El fichero, dentro del almacenamiento privado de la aplicacion. */
        const val FILE_NAME = "gastonomo.db"

        fun open(context: Context): GastonomoDatabase =
            Room.databaseBuilder(context, GastonomoDatabase::class.java, FILE_NAME).build()

        /** Sin fichero: lo que se guarda se va con el proceso. Para tests. */
        fun inMemory(context: Context): GastonomoDatabase =
            Room.inMemoryDatabaseBuilder(context, GastonomoDatabase::class.java).build()
    }
}
