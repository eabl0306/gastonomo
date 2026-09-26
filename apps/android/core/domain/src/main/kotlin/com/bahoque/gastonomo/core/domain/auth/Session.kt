package com.bahoque.gastonomo.core.domain.auth

import com.bahoque.gastonomo.core.domain.user.User

/**
 * Lo que devuelven `register` y `login`. El access token es un JWT de 15 minutos y puede
 * vivir en memoria; el refresh es opaco, de 30 dias, y hay que guardarlo cifrado.
 *
 * @property expiresInSeconds vida del access token, tal como la manda la API.
 */
data class Session(
    val accessToken: String,
    val expiresInSeconds: Long,
    val refreshToken: String,
    val user: User,
)

/**
 * El par de tokens sin el usuario, que es lo que devuelve un refresco.
 *
 * `POST /v1/auth/refresh` ROTA: hay que guardar el nuevo y tirar el viejo. Presentar uno
 * ya rotado revoca la familia entera y echa al usuario.
 */
data class TokenPair(
    val accessToken: String,
    val expiresInSeconds: Long,
    val refreshToken: String,
)
