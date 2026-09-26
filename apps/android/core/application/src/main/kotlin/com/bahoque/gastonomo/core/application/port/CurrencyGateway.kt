package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * Catalogo de monedas y tasas. Los clientes **no llaman a Frankfurter**: lo hace la API y
 * sirve la respuesta guardada.
 *
 * En arranque en frio [catalog] devuelve la lista vacia con un 200 y [rates] responde 503.
 * Registrar movimientos funciona igual; lo unico que sale vacio es el sheet de moneda.
 */
interface CurrencyGateway {

    /** `GET /v1/currencies`. Ya filtrado: sin metales ni XDR y solo con tasa. */
    suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError>

    /** `GET /v1/exchange-rates`. Base USD. 503 si no hay ninguna pasada guardada. */
    suspend fun rates(): Outcome<ExchangeRates, ApiError>
}
