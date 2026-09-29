package com.bahoque.gastonomo.core.application.usecase.settings

import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.application.state.DevicePreferences
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

/**
 * El estado del cerrojo local y del ojo que oculta los montos. No se sincronizan.
 *
 * Cada emision se publica ademas en [DevicePreferences], para que una pantalla que llegue
 * despues empiece con el valor ya leido en vez de con el por defecto. Va aqui y no en quien
 * observa porque asi no hay que acordarse en el siguiente sitio que lo haga.
 */
class ObserveDeviceSettings(
    private val store: DeviceSettingsStore,
    private val preferences: DevicePreferences = DevicePreferences(),
) {
    operator fun invoke(): Flow<DeviceSettings> = store.settings.onEach(preferences::set)
}
