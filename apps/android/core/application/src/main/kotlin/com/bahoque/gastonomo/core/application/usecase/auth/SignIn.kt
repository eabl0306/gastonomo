package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session

/** Entra con correo y contrasena y guarda la sesion cifrada en el dispositivo. */
class SignIn(
    private val gateway: AuthGateway,
    private val sessionStore: SessionStore,
) {
    suspend operator fun invoke(email: String, password: String): Outcome<Session, ApiError> {
        val result = gateway.login(email.trim(), password)
        if (result is Outcome.Success) sessionStore.save(result.value)
        return result
    }
}
