package com.bahoque.gastonomo.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.bahoque.gastonomo.core.application.port.SessionStore
import com.bahoque.gastonomo.core.domain.auth.Session
import com.bahoque.gastonomo.core.domain.auth.TokenPair
import com.bahoque.gastonomo.core.domain.user.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * La sesion en DataStore, con los dos tokens cifrados por el Keystore.
 *
 * El access token tambien se cifra y se guarda con su caducidad, en vez de vivir solo en
 * memoria: asi al volver a abrir la aplicacion no hace falta gastar un refresco, y un
 * refresco de mas es justo lo que no conviene con tokens que rotan.
 */
internal class DataStoreSessionStore(
    private val dataStore: DataStore<Preferences>,
    private val cipher: KeystoreCipher = KeystoreCipher(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : SessionStore {

    override val isSignedIn: Flow<Boolean> =
        dataStore.data.map { it[SessionKeys.REFRESH_TOKEN] != null }

    override val user: Flow<User?> = dataStore.data.map { preferences ->
        val id = preferences[SessionKeys.USER_ID] ?: return@map null
        User(
            id = id,
            name = preferences[SessionKeys.USER_NAME].orEmpty(),
            email = preferences[SessionKeys.USER_EMAIL].orEmpty(),
        )
    }

    override suspend fun save(session: Session) {
        dataStore.edit { preferences ->
            preferences[SessionKeys.USER_ID] = session.user.id
            preferences[SessionKeys.USER_NAME] = session.user.name
            preferences[SessionKeys.USER_EMAIL] = session.user.email
            writeTokens(preferences, session.accessToken, session.expiresInSeconds, session.refreshToken)
        }
    }

    override suspend fun replaceTokens(tokens: TokenPair) {
        dataStore.edit { preferences ->
            writeTokens(preferences, tokens.accessToken, tokens.expiresInSeconds, tokens.refreshToken)
        }
    }

    override suspend fun accessToken(): String? {
        val preferences = dataStore.data.first()
        val expiresAt = preferences[SessionKeys.ACCESS_TOKEN_EXPIRES_AT]
        val valid = expiresAt != null && expiresAt > nowMillis()
        return if (valid) preferences[SessionKeys.ACCESS_TOKEN]?.let(cipher::decrypt) else null
    }

    override suspend fun refreshToken(): String? =
        dataStore.data.first()[SessionKeys.REFRESH_TOKEN]?.let(cipher::decrypt)

    override suspend fun clear() {
        dataStore.edit { preferences ->
            SESSION_KEYS.forEach { preferences.remove(it) }
            preferences.remove(SessionKeys.ACCESS_TOKEN_EXPIRES_AT)
        }
    }

    private fun writeTokens(
        preferences: androidx.datastore.preferences.core.MutablePreferences,
        accessToken: String,
        expiresInSeconds: Long,
        refreshToken: String,
    ) {
        preferences[SessionKeys.ACCESS_TOKEN] = cipher.encrypt(accessToken)
        preferences[SessionKeys.REFRESH_TOKEN] = cipher.encrypt(refreshToken)
        preferences[SessionKeys.ACCESS_TOKEN_EXPIRES_AT] =
            nowMillis() + (expiresInSeconds - EXPIRY_MARGIN_SECONDS) * MILLIS_PER_SECOND
    }

    private companion object {
        /**
         * Se da el token por caducado 30 segundos antes. Sin margen, una peticion que sale
         * con el token justo en el limite llega ya caducada y provoca un refresco que se
         * podia haber hecho antes y sin prisa.
         */
        const val EXPIRY_MARGIN_SECONDS = 30L
        const val MILLIS_PER_SECOND = 1_000L

        val SESSION_KEYS = listOf(
            SessionKeys.ACCESS_TOKEN,
            SessionKeys.REFRESH_TOKEN,
            SessionKeys.USER_ID,
            SessionKeys.USER_NAME,
            SessionKeys.USER_EMAIL,
        )
    }
}
