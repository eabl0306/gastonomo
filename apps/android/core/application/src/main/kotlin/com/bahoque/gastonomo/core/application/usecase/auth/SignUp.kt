package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.application.port.RegistrationRequest
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.auth.Session

/**
 * Crea la cuenta y deja la sesion abierta. La API siembra las 12 categorias y los ajustes
 * por defecto, asi que despues de esto ya hay con que trabajar.
 */
class SignUp(
    private val gateway: AuthGateway,
    private val sessionStore: SessionStore,
) {
    suspend operator fun invoke(request: RegistrationRequest): Outcome<Session, ApiError> {
        val normalized = request.copy(name = request.name.trim(), email = request.email.trim())
        val result = gateway.register(normalized)
        if (result is Outcome.Success) sessionStore.save(result.value)
        return result
    }
}
