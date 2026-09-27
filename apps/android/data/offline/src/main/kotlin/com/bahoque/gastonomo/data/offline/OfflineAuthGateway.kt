package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.data.database.local.LocalAccount
import com.bahoque.gastonomo.data.database.local.LocalStore

/**
 * El perfil leido de la base local y, sobre todo, **el vaciado de la base en cada cambio de
 * cuenta**.
 *
 * Esta aqui y no en un caso de uso porque entrar, registrarse y salir pasan los tres por este
 * puerto: asi no hay manera de anadir un camino nuevo de sesion que se olvide de limpiar. Sin
 * esto, quien entra despues de que otro cierre sesion en el mismo telefono veria los movimientos
 * del anterior, y ahora ademas en disco.
 *
 * Se limpia **antes** de la llamada, no despues: si el intento falla, tampoco queda nada de la
 * cuenta anterior.
 */
internal class OfflineAuthGateway(
    private val remote: AuthGateway,
    private val store: LocalStore,
    private val local: LocalAccount,
    private val gate: RefreshGate,
) : AuthGateway {

    override suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError> {
        wipe()
        return remote.register(request)
    }

    override suspend fun login(email: String, password: String): Outcome<Session, ApiError> {
        wipe()
        return remote.login(email, password)
    }

    override suspend fun logout(): Outcome<Unit, ApiError> {
        wipe()
        return remote.logout()
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError> =
        remote.requestPasswordReset(email)

    /** Revoca los demas dispositivos, no este, asi que lo guardado aqui sigue siendo mio. */
    override suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError> =
        remote.changePassword(current, next)

    override suspend fun profile(): Outcome<User, ApiError> {
        val stored = local.profile()
        if (stored != null) {
            gate.inBackground(KEY) { refresh() }
            return Outcome.Success(stored)
        }
        return refresh()
    }

    private suspend fun refresh(): Outcome<User, ApiError> = remote.profile().also { result ->
        if (result is Outcome.Success) {
            local.saveProfile(result.value)
            gate.markFresh(KEY)
        }
    }

    private suspend fun wipe() {
        store.clear()
        gate.forget()
    }

    private companion object {
        const val KEY = "profile"
    }
}
