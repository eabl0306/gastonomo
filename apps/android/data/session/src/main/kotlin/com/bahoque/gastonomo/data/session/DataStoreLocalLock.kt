package com.bahoque.gastonomo.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.bahoque.gastonomo.core.application.port.LocalLock
import kotlinx.coroutines.flow.first

/** El PIN del cerrojo local, guardado hasheado con su sal. */
internal class DataStoreLocalLock(
    private val dataStore: DataStore<Preferences>,
    private val hasher: PinHasher = PinHasher(),
) : LocalLock {

    override suspend fun hasPin(): Boolean =
        dataStore.data.first()[SessionKeys.PIN_HASH] != null

    override suspend fun setPin(pin: String) {
        val salt = hasher.newSalt()
        dataStore.edit { preferences ->
            preferences[SessionKeys.PIN_SALT] = salt
            preferences[SessionKeys.PIN_HASH] = hasher.hash(pin, salt)
        }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val preferences = dataStore.data.first()
        val salt = preferences[SessionKeys.PIN_SALT]
        val stored = preferences[SessionKeys.PIN_HASH]
        return salt != null && stored != null && hasher.hash(pin, salt) == stored
    }

    override suspend fun clearPin() {
        dataStore.edit { preferences ->
            preferences.remove(SessionKeys.PIN_HASH)
            preferences.remove(SessionKeys.PIN_SALT)
        }
    }
}
