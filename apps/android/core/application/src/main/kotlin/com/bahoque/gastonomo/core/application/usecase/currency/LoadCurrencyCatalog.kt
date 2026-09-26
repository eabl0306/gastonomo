package com.bahoque.gastonomo.core.application.usecase.currency

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.currency.CurrencyInfo

/**
 * El catalogo del sheet de moneda, ya ordenado por codigo. La API lo entrega filtrado: sin
 * metales ni XDR y solo con las monedas que tienen tasa.
 *
 * En arranque en frio devuelve la lista vacia con un 200. El sheet sale vacio, pero
 * registrar movimientos sigue funcionando.
 */
class LoadCurrencyCatalog(private val gateway: CurrencyGateway) {
    suspend operator fun invoke(): Outcome<List<CurrencyInfo>, ApiError> =
        gateway.catalog().map { catalog -> catalog.sortedBy { it.code.value } }
}
