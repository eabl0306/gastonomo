package com.bahoque.gastonomo.data.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.common.map
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User
import com.bahoque.gastonomo.data.network.ApiClient

/**
 * Autenticacion contra la API.
 *
 * El refresco no esta aqui: lo hace el propio [ApiClient] de forma serializada, porque
 * presentar un refresh token ya rotado revoca la familia entera de la cuenta.
 */
internal class KtorAuthGateway(
    private val client: ApiClient,
    private val sessionStore: SessionStore,
) : AuthGateway {

    override suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError> =
        client.post<SessionDto>(
            path = "auth/register",
            body = RegisterRequestDto(
                name = request.name,
                email = request.email,
                password = request.password,
                acceptedTerms = request.acceptedTerms,
            ),
            anonymous = true,
        ).map { it.toDomain() }

    override suspend fun login(email: String, password: String): Outcome<Session, ApiError> =
        client.post<SessionDto>("auth/login", LoginRequestDto(email, password), anonymous = true)
            .map { it.toDomain() }

    override suspend fun logout(): Outcome<Unit, ApiError> {
        val refreshToken = sessionStore.refreshToken() ?: return Outcome.Success(Unit)
        return client.postEmpty("auth/logout", LogoutRequestDto(refreshToken))
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError> =
        client.postEmpty("auth/password/forgot", ForgotPasswordRequestDto(email), anonymous = true)

    override suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError> =
        client.postEmpty(
            path = "auth/password/change",
            body = ChangePasswordRequestDto(current, next, sessionStore.refreshToken()),
        )

    override suspend fun profile(): Outcome<User, ApiError> =
        client.get<UserDto>("me").map { it.toDomain() }
}

/** La fabrica que llama el contenedor de la aplicacion. */
fun createAuthGateway(client: ApiClient, sessionStore: SessionStore): AuthGateway =
    KtorAuthGateway(client, sessionStore)
