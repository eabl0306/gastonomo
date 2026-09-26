package com.bahoque.gastonomo.feature.loans

import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.money.ExchangeRates
import com.bahoque.gastonomo.core.domain.settings.UserSettings

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
) {
    suspend fun load(): MoneyContext {
        val settings = loadSettings().valueOrNull() ?: UserSettings()
        return MoneyContext(
            display = settings.displayCurrency,
            rates = loadExchangeRates().valueOrNull() ?: ExchangeRates.EMPTY,
            currencies = loadCurrencyCatalog().valueOrNull().orEmpty(),
        )
    }
}

/** El cargador que sale de las dependencias de esta feature. */
fun LoansDependencies.moneyContextLoader(): MoneyContextLoader = MoneyContextLoader(
    loadSettings = loadSettings,
    loadExchangeRates = loadExchangeRates,
    loadCurrencyCatalog = loadCurrencyCatalog,
)
