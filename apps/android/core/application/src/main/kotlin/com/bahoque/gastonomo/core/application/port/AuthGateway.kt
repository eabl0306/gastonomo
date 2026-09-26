package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User

/** Datos del registro. `acceptedTerms` tiene que ser true o la API responde 422. */
data class RegistrationRequest(
    val name: String,
    val email: String,
    val password: String,
    val acceptedTerms: Boolean,
)

/**
 * Autenticacion contra la API. No incluye el refresco: eso lo hace el propio cliente HTTP
 * de forma serializada, porque presentar un refresh ya rotado revoca la familia entera.
 */
interface AuthGateway {

    /** `POST /v1/auth/register`. Siembra las 12 categorias y los ajustes por defecto. */
    suspend fun register(request: RegistrationRequest): Outcome<Session, ApiError>

    /** `POST /v1/auth/login`. */
    suspend fun login(email: String, password: String): Outcome<Session, ApiError>

    /** `POST /v1/auth/logout`. Revoca el refresh token del dispositivo. */
    suspend fun logout(): Outcome<Unit, ApiError>

    /** `POST /v1/auth/password/forgot`. Devuelve 202 exista el correo o no. */
    suspend fun requestPasswordReset(email: String): Outcome<Unit, ApiError>

    /** `POST /v1/auth/password/change`. Revoca los demas dispositivos. */
    suspend fun changePassword(current: String, next: String): Outcome<Unit, ApiError>

    /** `GET /v1/me`. Nombre y correo, para el saludo y el avatar. */
    suspend fun profile(): Outcome<User, ApiError>
}
