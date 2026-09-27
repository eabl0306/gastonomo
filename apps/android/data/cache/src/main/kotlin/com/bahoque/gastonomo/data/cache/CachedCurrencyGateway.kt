package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * Catalogo de monedas y tasas con cache en memoria delante, y con plazos largos: la API las
 * refresca a las 06:00 y a las 18:00 UTC, asi que pedirlas mas de una vez cada media hora no
 * cambia ni un numero.
 *
 * El arranque en frio no se cachea, porque un 503 nunca se guarda: mientras la tarea de tasas
 * no consiga su primera pasada, cada intento vuelve a salir a la red.
 */
internal class CachedCurrencyGateway(
    private val delegate: CurrencyGateway,
    private val caches: GatewayCaches,
) : CurrencyGateway {

    override suspend fun catalog(): Outcome<List<CurrencyInfo>, ApiError> =
        caches.currencies.read(Unit) { delegate.catalog() }

    override suspend fun rates(): Outcome<ExchangeRates, ApiError> =
        caches.rates.read(Unit) { delegate.rates() }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedCurrencyGateway(gateway: CurrencyGateway, caches: GatewayCaches): CurrencyGateway =
    CachedCurrencyGateway(gateway, caches)
