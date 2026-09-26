package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.SessionStore
import kotlinx.coroutines.flow.Flow

/**
 * Si hay sesion guardada. Es lo que decide la pantalla inicial: Login si no hay, PIN si
 * hay sesion y cerrojo, e Inicio si hay sesion sin cerrojo.
 */
class ObserveSignedIn(private val sessionStore: SessionStore) {
    operator fun invoke(): Flow<Boolean> = sessionStore.isSignedIn
}
