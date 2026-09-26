package com.bahoque.gastonomo.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Los ajustes que se quedan en el telefono: el cerrojo local y el ojo de los montos. */
internal class DataStoreDeviceSettingsStore(
    private val dataStore: DataStore<Preferences>,
) : DeviceSettingsStore {

    override val settings: Flow<DeviceSettings> = dataStore.data.map { preferences ->
        DeviceSettings(
            lockEnabled = preferences[SessionKeys.LOCK_ENABLED] ?: false,
            hideAmounts = preferences[SessionKeys.HIDE_AMOUNTS] ?: false,
        )
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[SessionKeys.LOCK_ENABLED] = enabled }
    }

    override suspend fun setHideAmounts(hidden: Boolean) {
        dataStore.edit { it[SessionKeys.HIDE_AMOUNTS] = hidden }
    }
}
