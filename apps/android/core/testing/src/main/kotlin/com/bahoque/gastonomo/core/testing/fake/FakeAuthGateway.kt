package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User

/**
 * Autenticacion programable. Cada respuesta se fija en el constructor y las llamadas
 * quedan registradas.
 *
 * ```
 * val gateway = FakeAuthGateway(loginResult = Outcome.Failure(ApiError.InvalidCredentials))
 * ```
 */
class FakeAuthGateway(
    private val sessionResult: Outcome<Session, ApiError> = Outcome.Success(SampleAuth.session),
    private val unitResult: Outcome<Unit, ApiError> = Outcome.Success(Unit),
    private val profileResult: Outcome<User, ApiError> = Outcome.Success(SampleAuth.user),
) : AuthGateway {

    /** Las operaciones invocadas, en orden. */
    val calls = mutableListOf<String>()

    /** La ultima peticion de registro. */
    var lastRegistration: RegistrationRequest? = null
        private set

    /** Las credenciales del ultimo login. */
    var lastCredentials: Pair<String, String>? = null
        private set

    override suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError> {
        calls += "register"
        lastRegistration = request
        return sessionResult
    }

    override suspend fun login(email: String, password: String): Outcome<Session, ApiError> {
        calls += "login"
        lastCredentials = email to password
        return sessionResult
    }

    override suspend fun logout(): Outcome<Unit, ApiError> {
        calls += "logout"
        return unitResult
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError> {
        calls += "forgot:$email"
        return unitResult
    }

    override suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError> {
        calls += "change"
        return unitResult
    }

    override suspend fun profile(): Outcome<User, ApiError> {
        calls += "profile"
        return profileResult
    }
}
