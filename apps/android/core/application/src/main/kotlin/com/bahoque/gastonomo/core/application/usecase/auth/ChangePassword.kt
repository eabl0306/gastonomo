package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.common.Outcome

/**
 * Cambia la contrasena estando dentro. La API revoca los refresh tokens de los demas
 * dispositivos y deja vivo el de este.
 */
class ChangePassword(private val gateway: AuthGateway) {
    suspend operator fun invoke(current: String, next: String): Outcome<Unit, ApiError> =
        gateway.changePassword(current, next)
}
