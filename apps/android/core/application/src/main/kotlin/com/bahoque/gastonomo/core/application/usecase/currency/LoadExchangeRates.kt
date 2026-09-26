package com.bahoque.gastonomo.core.application.usecase.currency

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CurrencyGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.ExchangeRates

/**
 * Las tasas vigentes, con base USD. Todas las conversiones de la aplicacion salen de aqui.
 *
 * Responde 503 mientras la tarea de refresco no consiga su primera pasada. No es un fallo:
 * hay que reintentar. Con `status` a `stale` las tasas sirven igual y Ajustes pinta
 * "Sin conexion".
 */
class LoadExchangeRates(private val gateway: CurrencyGateway) {
    suspend operator fun invoke(): Outcome<ExchangeRates, ApiError> = gateway.rates()
}
