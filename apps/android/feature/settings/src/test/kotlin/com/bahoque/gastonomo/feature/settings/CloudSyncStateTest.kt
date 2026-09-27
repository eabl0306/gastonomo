package com.bahoque.gastonomo.feature.settings

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo que dice la fila "Sincronizacion en la nube".
 *
 * Antes era un texto fijo que decia "Al dia" pasara lo que pasara, y eso es lo que destapo que la
 * aplicacion tenia que funcionar sin conexion. Ahora sale de dos cosas que el usuario no puede ver
 * por su cuenta: cuantas escrituras siguen aqui y si hay red.
 */
class CloudSyncStateTest {

    @Test
    fun `with nothing pending and the api responding it is up to date`() {
        assertEquals(CloudSyncState.SYNCED, state().cloudSync)
    }

    @Test
    fun `with something pending and network it is syncing`() {
        assertEquals(CloudSyncState.SYNCING, state(pending = 2).cloudSync)
    }

    @Test
    fun `with something pending and no network it says how many are waiting`() {
        // Es lo que el usuario querria saber antes de desinstalar la aplicacion o cambiar de
        // telefono, asi que no se puede quedar escondido detras de un "al dia".
        assertEquals(CloudSyncState.PENDING, state(pending = 2, online = false).cloudSync)
    }

    @Test
    fun `with no network and nothing pending it just says there is no connection`() {
        assertEquals(CloudSyncState.OFFLINE, state(online = false).cloudSync)
    }

    @Test
    fun `a read that could not reach the api also counts as no connection`() {
        assertEquals(CloudSyncState.OFFLINE, state(reachedApi = false).cloudSync)
    }

    @Test
    fun `converting needs network and rates that are actually there`() {
        assertTrue(state().canConvert)
        assertFalse(state(online = false).canConvert)
        // Un 503 de la API y no tener red acaban en lo mismo: no hay tasas de confianza.
        assertFalse(state(rates = RatesIndicator.Offline).canConvert)
    }

    private fun state(
        pending: Int = 0,
        online: Boolean = true,
        reachedApi: Boolean = true,
        rates: RatesIndicator = RatesIndicator.Ready(null),
    ) = SettingsUiState(
        pendingCount = pending,
        online = online,
        reachedApi = reachedApi,
        rates = rates,
    )
}
