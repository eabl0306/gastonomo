package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.Flow

/**
 * Los ajustes que no se sincronizan: el cerrojo local y el ojo que oculta los montos.
 *
 * Tambien guarda una copia de la moneda y el idioma de la cuenta, para poder pintar la
 * primera pantalla con los ajustes correctos antes de que responda `GET /v1/settings`.
 */
interface DeviceSettingsStore {

    val settings: Flow<DeviceSettings>

    suspend fun setLockEnabled(enabled: Boolean)

    suspend fun setHideAmounts(hidden: Boolean)
}
