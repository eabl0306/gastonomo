package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User

/**
 * Cachea el perfil y, sobre todo, **vacia la cache en cada cambio de cuenta**.
 *
 * Esta aqui y no en un caso de uso porque entrar, registrarse y salir pasan los tres por este
 * puerto: asi no hay manera de anadir un camino nuevo de sesion que se olvide de limpiar. Sin
 * esto, quien entra despues de que otro cierre sesion en el mismo proceso veria los
 * movimientos del anterior.
 *
 * Se limpia **antes** de la llamada, no despues: si el intento falla, tampoco queda nada de la
 * cuenta anterior en memoria.
 */
internal class CachedAuthGateway(
    private val delegate: AuthGateway,
    private val caches: GatewayCaches,
) : AuthGateway {

    override suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError> {
        caches.clear()
        return delegate.register(request)
    }

    override suspend fun login(email: String, password: String): Outcome<Session, ApiError> {
        caches.clear()
        return delegate.login(email, password)
    }

    override suspend fun logout(): Outcome<Unit, ApiError> {
        caches.clear()
        return delegate.logout()
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError> =
        delegate.requestPasswordReset(email)

    /** Revoca los demas dispositivos, no este, asi que lo que hay en memoria sigue siendo mio. */
    override suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError> =
        delegate.changePassword(current, next)

    override suspend fun profile(): Outcome<User, ApiError> =
        caches.profile.read(Unit) { delegate.profile() }
}

/** Envuelve [gateway] con la cache en memoria. La llama la raiz de composicion. */
fun cachedAuthGateway(gateway: AuthGateway, caches: GatewayCaches): AuthGateway =
    CachedAuthGateway(gateway, caches)
