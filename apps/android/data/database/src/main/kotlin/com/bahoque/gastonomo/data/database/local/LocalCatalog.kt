package com.bahoque.gastonomo.data.database.local

import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.data.database.dao.AccountDao
import com.bahoque.gastonomo.data.database.dao.CategoryDao
import com.bahoque.gastonomo.data.database.dao.CurrencyDao
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.CurrencyEntity
import com.bahoque.gastonomo.data.database.entity.ProfileEntity
import com.bahoque.gastonomo.data.database.entity.SettingsEntity
import com.bahoque.gastonomo.data.database.entity.rateRowsOf
import com.bahoque.gastonomo.data.database.entity.ratesOf
import com.bahoque.gastonomo.data.database.entity.snapshotOf

/** Las categorias guardadas. `GET /v1/categories` las trae todas, asi que se reemplazan todas. */
class LocalCategories internal constructor(private val dao: CategoryDao) {

    suspend fun all(): List<Category> = dao.all().map { it.toDomain() }

    suspend fun replaceAll(categories: List<Category>) {
        dao.replaceAll(categories.map(CategoryEntity::of))
    }
}

/** Los ajustes de la cuenta y el perfil: una fila cada uno. */
class LocalAccount internal constructor(private val dao: AccountDao) {

    suspend fun settings(): UserSettings? = dao.settings()?.toDomain()

    suspend fun saveSettings(settings: UserSettings) {
        dao.upsertSettings(SettingsEntity.of(settings))
    }

    suspend fun profile(): User? = dao.profile()?.toDomain()

    suspend fun saveProfile(user: User) {
        dao.upsertProfile(ProfileEntity.of(user))
    }
}

/**
 * El catalogo de monedas y las tasas.
 *
 * [rates] devuelve [ExchangeRates.EMPTY] cuando no hay ninguna pasada guardada, que es el mismo
 * valor con el que arranca la aplicacion en frio: quien lo recibe ya sabe tratarlo.
 */
class LocalCurrencies internal constructor(private val dao: CurrencyDao) {

    suspend fun catalog(): List<CurrencyInfo> = dao.currencies().map { it.toDomain() }

    suspend fun saveCatalog(currencies: List<CurrencyInfo>) {
        dao.upsertCurrencies(currencies.map(CurrencyEntity::of))
    }

    suspend fun rates(): ExchangeRates = ratesOf(dao.snapshot(), dao.rates())

    suspend fun saveRates(rates: ExchangeRates) {
        dao.replaceRates(snapshotOf(rates), rateRowsOf(rates))
    }
}
