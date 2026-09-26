package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.application.port.LocalLock

/**
 * Enciende o apaga el cerrojo local del toggle de Ajustes. Encenderlo exige un PIN;
 * apagarlo lo borra, porque un cerrojo apagado con PIN guardado es un secreto sin dueno.
 */
class SetUpLocalLock(
    private val localLock: LocalLock,
    private val deviceSettings: DeviceSettingsStore,
) {
    suspend operator fun invoke(enabled: Boolean, pin: String? = null): Boolean {
        if (!enabled) {
            localLock.clearPin()
            deviceSettings.setLockEnabled(false)
            return true
        }
        if (pin == null || pin.length != LocalLock.PIN_LENGTH) return false
        localLock.setPin(pin)
        deviceSettings.setLockEnabled(true)
        return true
    }
}
