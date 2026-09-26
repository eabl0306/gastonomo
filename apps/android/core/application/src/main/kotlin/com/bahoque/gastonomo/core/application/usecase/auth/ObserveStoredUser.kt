package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.flow.Flow

/**
 * El usuario que ya esta guardado en el dispositivo, sin llamar a la API.
 *
 * Lo necesita la pantalla de desbloqueo: el saludo y las iniciales del avatar se pintan
 * con la sesion guardada, porque el PIN protege un secreto local y esa pantalla tiene que
 * funcionar sin red. `LoadProfile` no sirve para esto: hace `GET /v1/me`.
 */
class ObserveStoredUser(private val sessionStore: SessionStore) {
    operator fun invoke(): Flow<User?> = sessionStore.user
}
