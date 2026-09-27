package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.sync.ObserveConnection
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.money.RatesStatus
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import kotlinx.coroutines.flow.first

/**
 * Carga la moneda principal, las tasas y el catalogo de monedas de una vez.
 *
 * Ninguna de las tres cosas es un fallo bloqueante y por eso no devuelve `Outcome`:
 * - sin ajustes se usa la moneda por defecto de una cuenta nueva;
 * - sin tasas (503 mientras la tarea de refresco no consigue su primera pasada) los
 *   importes registrados en la moneda principal se ensenan igual, porque cada registro
 *   guarda su propia equivalencia, y solo los de otra moneda pierden precision;
 * - sin catalogo el sheet de moneda sale vacio, que es lo que dice el contrato para un
 *   arranque en frio, y registrar sigue funcionando.
 */
class MoneyContextLoader(
    private val loadSettings: LoadSettings,
    private val loadExchangeRates: LoadExchangeRates,
    private val loadCurrencyCatalog: LoadCurrencyCatalog,
    private val observeConnection: ObserveConnection,
) {
    suspend fun load(): MoneyContext {
        val settings = loadSettings().valueOrNull() ?: UserSettings()
        val rates = loadExchangeRates().valueOrNull() ?: ExchangeRates.EMPTY
        val online = observeConnection().first()
        return MoneyContext(
            display = settings.displayCurrency,
            rates = rates,
            currencies = loadCurrencyCatalog().valueOrNull().orEmpty(),
            // Un 503 de la API y no tener red acaban en lo mismo para el usuario: no hay tasas de
            // confianza con las que convertir, asi que se registra en la moneda principal.
            canConvert = online && rates.status != RatesStatus.UNAVAILABLE,
        )
    }
}

/** El cargador que sale de las dependencias de esta feature. */
fun LoansDependencies.moneyContextLoader(): MoneyContextLoader = MoneyContextLoader(
    loadSettings = loadSettings,
    loadExchangeRates = loadExchangeRates,
    loadCurrencyCatalog = loadCurrencyCatalog,
    observeConnection = observeConnection,
)
