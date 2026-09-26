package com.bahoque.gastonomo.core.application.port

import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.flow.Flow

/**
 * Donde vive la sesion en el dispositivo.
 *
 * El refresh token se guarda **cifrado**: es la llave de la cuenta durante 30 dias. El
 * access token puede vivir en memoria, porque caduca en 15 minutos y se vuelve a pedir.
 */
interface SessionStore {

    /** Emite true mientras haya refresh token guardado. Es lo que decide la pantalla inicial. */
    val isSignedIn: Flow<Boolean>

    /** El usuario guardado, para pintar el saludo antes de que llegue `GET /v1/me`. */
    val user: Flow<User?>

    /** Guarda la sesion recien creada por register o login. */
    suspend fun save(session: Session)

    /** Sustituye el par de tokens despues de un refresco. El viejo ya no vale. */
    suspend fun replaceTokens(tokens: TokenPair)

    /** El access token vigente, o null si no hay o ya caduco. */
    suspend fun accessToken(): String?

    /** El refresh token guardado, o null si no hay sesion. */
    suspend fun refreshToken(): String?

    /** Borra todo. Se llama al cerrar sesion y cuando un refresco falla. */
    suspend fun clear()
}
