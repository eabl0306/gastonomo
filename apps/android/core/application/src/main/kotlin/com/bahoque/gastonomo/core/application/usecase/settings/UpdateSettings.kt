package com.bahoque.gastonomo.core.application.usecase.settings

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.application.state.UserPreferences
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.valueOrNull
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Cambia la moneda principal, el idioma o los dos.
 *
 * Cambiar la moneda **no modifica ningun registro guardado**: solo cambia en que moneda se
 * ensena todo y cual se usa por defecto en los registros nuevos.
 *
 * Lo que responde la API se publica en [preferences], que es lo que hace que el cambio se vea
 * en el resto de la aplicacion y no solo en Ajustes.
 *
 * Puede responder 503 si el catalogo de monedas esta vacio. No es un fallo del cliente: la
 * tarea de tasas todavia no consiguio su primera pasada y hay que reintentar.
 */
class UpdateSettings(
    private val gateway: SettingsGateway,
    private val preferences: UserPreferences = UserPreferences(),
) {
    suspend operator fun invoke(
        displayCurrency: CurrencyCode? = null,
        language: AppLanguage? = null,
    ): Outcome<UserSettings, ApiError> = gateway.update(displayCurrency, language)
        .also { result -> result.valueOrNull()?.let(preferences::set) }
}
