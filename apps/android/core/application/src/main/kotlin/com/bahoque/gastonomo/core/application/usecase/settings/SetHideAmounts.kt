package com.bahoque.gastonomo.core.application.usecase.settings

import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore

/** El ojo de la tarjeta de balance. Es estado del dispositivo, no de la cuenta. */
class SetHideAmounts(private val store: DeviceSettingsStore) {
    suspend operator fun invoke(hidden: Boolean) = store.setHideAmounts(hidden)
}
