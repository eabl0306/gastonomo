package com.bahoque.gastonomo.core.application.usecase.settings

import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.Flow

/** El estado del cerrojo local y del ojo que oculta los montos. No se sincronizan. */
class ObserveDeviceSettings(private val store: DeviceSettingsStore) {
    operator fun invoke(): Flow<DeviceSettings> = store.settings
}
