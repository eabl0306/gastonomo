package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Ajustes de la cuenta: moneda principal e idioma, y nada mas. El cerrojo con huella o
 * PIN y el ojo que oculta los montos son del dispositivo y viven en [DeviceSettingsStore].
 */
interface SettingsGateway {

    /** `GET /v1/settings`. */
    suspend fun load(): Outcome<UserSettings, ApiError>

    /**
     * `PATCH /v1/settings`. Se puede cambiar una de las dos o las dos.
     *
     * Responde 503 si el catalogo de monedas esta vacio: no es culpa del cliente, es que
     * la tarea de tasas todavia no consiguio su primera pasada. Hay que reintentar.
     */
    suspend fun update(
        displayCurrency: CurrencyCode? = null,
        language: AppLanguage? = null,
    ): Outcome<UserSettings, ApiError>
}
