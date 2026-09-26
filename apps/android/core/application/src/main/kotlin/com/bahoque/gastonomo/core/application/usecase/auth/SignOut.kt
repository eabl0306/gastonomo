package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.application.port.SessionStore

/**
 * Cierra la sesion. Revoca el refresh token en la API y borra todo lo local, incluido el
 * PIN: el cerrojo protegia un token que ya no existe.
 *
 * El borrado local se hace pase lo que pase con la llamada: si la API no responde, el
 * usuario igualmente pidio salir.
 */
class SignOut(
    private val gateway: AuthGateway,
    private val sessionStore: SessionStore,
    private val localLock: LocalLock,
) {
    suspend operator fun invoke() {
        gateway.logout()
        sessionStore.clear()
        localLock.clearPin()
    }
}
