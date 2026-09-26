package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Sesion en memoria. `saved` deja comprobar que un caso de uso guardo lo que tocaba. */
class FakeSessionStore(initial: Session? = null) : SessionStore {

    private val state = MutableStateFlow(initial)

    /** La ultima sesion guardada, o null si se limpio. */
    val saved: Session? get() = state.value

    /** Cuantas veces se sustituyeron los tokens. Lo usan los tests de refresco. */
    var refreshCount: Int = 0
        private set

    override val isSignedIn: Flow<Boolean> = state.map { it != null }

    override val user: Flow<User?> = state.map { it?.user }

    override suspend fun save(session: Session) {
        state.value = session
    }

    override suspend fun replaceTokens(tokens: TokenPair) {
        refreshCount++
        state.value = state.value?.copy(
            accessToken = tokens.accessToken,
            expiresInSeconds = tokens.expiresInSeconds,
            refreshToken = tokens.refreshToken,
        )
    }

    override suspend fun accessToken(): String? = state.value?.accessToken

    override suspend fun refreshToken(): String? = state.value?.refreshToken

    override suspend fun clear() {
        state.value = null
    }
}
