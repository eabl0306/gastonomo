package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.common.Outcome

/**
 * Pide el enlace de recuperacion. La API responde 202 exista el correo o no, asi que la
 * pantalla siempre pasa al estado "Revisa tu correo": responder distinto delataria quien
 * tiene cuenta. El enlace caduca en 30 minutos.
 */
class RequestPasswordReset(private val gateway: AuthGateway) {
    suspend operator fun invoke(email: String): Outcome<Unit, ApiError> =
        gateway.requestPasswordReset(email.trim())
}
