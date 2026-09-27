package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.data.database.local.LocalAccount
import com.bahoque.gastonomo.data.database.local.LocalCategories
import com.bahoque.gastonomo.data.database.local.LocalCurrencies

/** Categorias leidas de la base local. Escribir va a la red y vuelve a bajar la lista entera. */
internal class OfflineCategoryGateway(
    private val remote: CategoryGateway,
    private val local: LocalCategories,
    private val gate: RefreshGate,
) : CategoryGateway {

    override suspend fun list(): Outcome<List<Category>, ApiError> {
        val stored = local.all()
        if (stored.isNotEmpty() || gate.isFresh(KEY)) {
            gate.inBackground(KEY) { refresh() }
            return Outcome.Success(stored)
        }
        return refresh().map { local.all() }
    }

    override suspend fun create(draft: CategoryDraft): Outcome<Category, ApiError> =
        remote.create(draft).alsoRefreshing()

    override suspend fun update(
        id: CategoryId,
        draft: CategoryDraft,
    ): Outcome<Category, ApiError> = remote.update(id, draft).alsoRefreshing()

    override suspend fun delete(id: CategoryId): Outcome<Unit, ApiError> =
        remote.delete(id).alsoRefreshing()

    private suspend fun refresh(): Outcome<Unit, ApiError> = remote.list().map { categories ->
        local.replaceAll(categories)
        gate.markFresh(KEY)
    }

    /**
     * Borrar una categoria reasigna sus movimientos a Otros y renombrarla cambia como se leen,
     * asi que tras escribir hay que olvidarlo todo, no solo las categorias.
     */
    private suspend fun <V> Outcome<V, ApiError>.alsoRefreshing(): Outcome<V, ApiError> {
        if (this is Outcome.Success) {
            gate.forget()
            refresh()
        }
        return this
    }

    private companion object {
        const val KEY = "categories"
    }
}

/**
 * Ajustes de la cuenta leidos de la base local.
 *
 * Cambiar la moneda principal **no toca los registros**: los importes guardados son los cuatro
 * campos que devolvio la API y la conversion se hace al pintar. Si se guardara convertido, cambiar
 * la moneda obligaria a reescribir la base entera y el registro dejaria de reflejar lo que el
 * usuario teclo.
 */
internal class OfflineSettingsGateway(
    private val remote: SettingsGateway,
    private val local: LocalAccount,
    private val gate: RefreshGate,
) : SettingsGateway {

    override suspend fun load(): Outcome<UserSettings, ApiError> {
        val stored = local.settings()
        if (stored != null) {
            gate.inBackground(KEY) { refresh() }
            return Outcome.Success(stored)
        }
        return refresh()
    }

    override suspend fun update(
        displayCurrency: CurrencyCode?,
        language: AppLanguage?,
    ): Outcome<UserSettings, ApiError> = remote.update(displayCurrency, language).also { result ->
        if (result is Outcome.Success) {
            local.saveSettings(result.value)
            gate.markFresh(KEY)
        }
    }

    private suspend fun refresh(): Outcome<UserSettings, ApiError> =
        remote.load().also { result ->
            if (result is Outcome.Success) {
                local.saveSettings(result.value)
                gate.markFresh(KEY)
            }
        }

    private companion object {
        const val KEY = "settings"
    }
}

/**
 * Catalogo de monedas y tasas leidos de la base local.
 *
 * Sin conexion se sirven las ultimas tasas que se bajaron, que es lo que permite seguir
 * ensenando lo ya guardado en la moneda principal. Registrar en otra moneda es lo que se degrada,
 * y eso es del issue 17.
 */
internal class OfflineCurrencyGateway(
    private val remote: CurrencyGateway,
    private val local: LocalCurrencies,
    private val gate: RefreshGate,
) : CurrencyGateway {

    override suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError> {
        val stored = local.catalog()
        if (stored.isNotEmpty()) {
            gate.inBackground(CATALOG_KEY) { refreshCatalog() }
            return Outcome.Success(stored)
        }
        return refreshCatalog()
    }

    override suspend fun rates(): Outcome<ExchangeRates, ApiError> {
        val stored = local.rates()
        if (stored.status != RatesStatus.UNAVAILABLE) {
            gate.inBackground(RATES_KEY) { refreshRates() }
            return Outcome.Success(stored)
        }
        return refreshRates()
    }

    private suspend fun refreshCatalog(): Outcome<List<CurrencyInfo>, ApiError> =
        remote.catalog().also { result ->
            if (result is Outcome.Success && result.value.isNotEmpty()) {
                local.saveCatalog(result.value)
                gate.markFresh(CATALOG_KEY)
            }
        }

    private suspend fun refreshRates(): Outcome<ExchangeRates, ApiError> =
        remote.rates().also { result ->
            if (result is Outcome.Success) {
                local.saveRates(result.value)
                gate.markFresh(RATES_KEY)
            }
        }

    private companion object {
        const val CATALOG_KEY = "currencies"
        const val RATES_KEY = "rates"
    }
}
