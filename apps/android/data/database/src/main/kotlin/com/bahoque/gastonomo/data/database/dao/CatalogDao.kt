package com.bahoque.gastonomo.data.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.CurrencyEntity
import com.bahoque.gastonomo.data.database.entity.ExchangeRateEntity
import com.bahoque.gastonomo.data.database.entity.ExchangeRateSnapshotEntity
import com.bahoque.gastonomo.data.database.entity.ProfileEntity
import com.bahoque.gastonomo.data.database.entity.SettingsEntity
import kotlinx.coroutines.flow.Flow

/** Categorias de la cuenta. Se leen todas de una vez: no hay consulta por identificador. */
@Dao
internal interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY is_default ASC, name COLLATE NOCASE ASC")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories ORDER BY is_default ASC, name COLLATE NOCASE ASC")
    suspend fun all(): List<CategoryEntity>

    @Upsert
    suspend fun upsert(categories: List<CategoryEntity>)

    /** Reemplaza el catalogo entero: es lo que devuelve `GET /v1/categories`, completo. */
    @Transaction
    suspend fun replaceAll(categories: List<CategoryEntity>) {
        clear()
        upsert(categories)
    }

    @Query("DELETE FROM categories")
    suspend fun clear()
}

/** Ajustes de la cuenta y perfil: una fila cada uno. */
@Dao
internal interface AccountDao {

    @Query("SELECT * FROM settings LIMIT 1")
    fun observeSettings(): Flow<SettingsEntity?>

    @Query("SELECT * FROM settings LIMIT 1")
    suspend fun settings(): SettingsEntity?

    @Upsert
    suspend fun upsertSettings(settings: SettingsEntity)

    @Query("SELECT * FROM profile LIMIT 1")
    suspend fun profile(): ProfileEntity?

    @Upsert
    suspend fun upsertProfile(profile: ProfileEntity)

    @Query("DELETE FROM settings")
    suspend fun clearSettings()

    @Query("DELETE FROM profile")
    suspend fun clearProfile()
}

/** Catalogo de monedas y tasas. */
@Dao
internal interface CurrencyDao {

    @Query("SELECT * FROM currencies ORDER BY code ASC")
    suspend fun currencies(): List<CurrencyEntity>

    @Query("SELECT * FROM exchange_rate_snapshots LIMIT 1")
    suspend fun snapshot(): ExchangeRateSnapshotEntity?

    @Query("SELECT * FROM exchange_rates")
    suspend fun rates(): List<ExchangeRateEntity>

    @Upsert
    suspend fun upsertSnapshot(snapshot: ExchangeRateSnapshotEntity)

    @Upsert
    suspend fun upsertRates(rates: List<ExchangeRateEntity>)

    @Upsert
    suspend fun upsertCurrencies(currencies: List<CurrencyEntity>)

    /**
     * Reemplaza la pasada entera. Las tasas viejas se borran antes de poner las nuevas: si una
     * moneda desaparece del catalogo, dejar su tasa colgada haria convertir con un numero que la
     * API ya no da por bueno.
     */
    @Transaction
    suspend fun replaceRates(
        snapshot: ExchangeRateSnapshotEntity,
        rates: List<ExchangeRateEntity>,
    ) {
        clearRates()
        upsertSnapshot(snapshot)
        upsertRates(rates)
    }

    @Query("DELETE FROM exchange_rates")
    suspend fun clearRates()

    @Query("DELETE FROM currencies")
    suspend fun clearCurrencies()
}
