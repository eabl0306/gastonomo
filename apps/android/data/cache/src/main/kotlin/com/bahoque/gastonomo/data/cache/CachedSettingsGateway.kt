package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.SettingsGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.UserSettings

/**
 * Ajustes de la cuenta con cache en memoria delante.
 *
 * Cambiar la moneda principal invalida **solo los ajustes**, no los registros. Los importes
 * guardados son los cuatro campos que devolvio la API y la conversion la hace el cliente al
 * pintar, asi que la lista de movimientos que ya esta en memoria sigue siendo valida: cambia
 * como se muestra, no lo que se guardo.
 */
internal class CachedSettingsGateway(
    private val delegate: SettingsGateway,
    private val caches: GatewayCaches,
) : SettingsGateway {

    override suspend fun load(): Outcome<UserSettings, ApiError> =
        caches.settings.read(Unit) { delegate.load() }

    override suspend fun update(
        displayCurrency: CurrencyCode?,
        language: AppLanguage?,
    ): Outcome<UserSettings, ApiError> {
        val result = delegate.update(displayCurrency, language)
        if (result is Outcome.Success) caches.settings.invalidate()
        return result
    }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedSettingsGateway(gateway: SettingsGateway, caches: GatewayCaches): SettingsGateway =
    CachedSettingsGateway(gateway, caches)
