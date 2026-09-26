package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.user.User

/** El usuario del prototipo, para que los tests y las capturas hablen del mismo. */
object SampleAuth {

    val user = User(
        id = "11111111-1111-1111-1111-111111111111",
        name = "Camilo Rojas",
        email = "camilo@correo.com",
    )

    val session = Session(
        accessToken = "access-token",
        expiresInSeconds = ACCESS_TOKEN_SECONDS,
        refreshToken = "refresh-token",
        user = user,
    )

    /** Los 15 minutos que dura el access token de la API. */
    private const val ACCESS_TOKEN_SECONDS = 900L
}
