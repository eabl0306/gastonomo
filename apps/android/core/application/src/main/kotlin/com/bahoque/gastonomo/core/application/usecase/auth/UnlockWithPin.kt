package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.LocalLock

/**
 * Comprueba el PIN del cerrojo local. No habla con la API: lo que desbloquea es el refresh
 * token que ya esta guardado en este dispositivo.
 */
class UnlockWithPin(private val localLock: LocalLock) {
    suspend operator fun invoke(pin: String): Boolean =
        pin.length == LocalLock.PIN_LENGTH && localLock.verifyPin(pin)
}
