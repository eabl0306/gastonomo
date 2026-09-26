package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.AuthGateway
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.user.User

/** El perfil de la cuenta: nombre y correo, para el saludo, el avatar y Ajustes. */
class LoadProfile(private val gateway: AuthGateway) {
    suspend operator fun invoke(): Outcome<User, ApiError> = gateway.profile()
}
