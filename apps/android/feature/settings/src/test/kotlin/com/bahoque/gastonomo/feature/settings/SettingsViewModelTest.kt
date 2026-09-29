package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryGateway
import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.settings.AppLanguage
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import com.bahoque.gastonomo.core.testing.fake.FakeCategoryGateway
import com.bahoque.gastonomo.core.testing.fake.FakeChangeGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSyncStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** El estado y las acciones de Ajustes, con dobles de los casos de uso. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @Before
    fun setUp() = Dispatchers.setMain(StandardTestDispatcher())

    @After
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `it loads the profile, the settings, the catalog and the category count`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(SampleAuth.user, state.user)
        assertEquals(CurrencyCode.COP, state.settings.displayCurrency)
        assertEquals(SampleFinance.currencies.size, state.currencies.size)
        assertEquals(SampleFinance.categories.size, state.categoryCount)
        assertEquals(CloudSyncState.SYNCED, state.cloudSync)
        assertEquals(RatesIndicator.Ready(SampleFinance.rates.date), state.rates)
        assertTrue(!state.loading)
    }

    @Test
    fun `without network the sync indicator is paused and the rates are offline`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.offline())
        advanceUntilIdle()
        assertEquals(CloudSyncState.OFFLINE, viewModel.state.value.cloudSync)
        assertEquals(RatesIndicator.Offline, viewModel.state.value.rates)
    }

    @Test
    fun `changing the currency closes the sheet and announces the new one`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.showSheet(SettingsSheet.CURRENCY)
        viewModel.onSettingChanged(currency = CurrencyCode.USD)
        advanceUntilIdle()
        val state = viewModel.state.value
        assertEquals(CurrencyCode.USD, state.settings.displayCurrency)
        assertNull(state.sheet)
        assertEquals(SettingsMessage.CurrencyChanged(CurrencyCode.USD), state.message)
    }

    @Test
    fun `picking the currency already in use says nothing`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.onSettingChanged(currency = CurrencyCode.COP)
        advanceUntilIdle()
        assertNull(viewModel.state.value.message)
    }

    @Test
    fun `a 503 while saving is told as the catalog not being ready`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.catalogNotReady())
        advanceUntilIdle()
        viewModel.onSettingChanged(currency = CurrencyCode.EUR)
        advanceUntilIdle()
        assertEquals(SettingsMessage.CatalogNotReady, viewModel.state.value.message)
    }

    @Test
    fun `changing the language stores it and closes the sheet`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.showSheet(SettingsSheet.LANGUAGE)
        assertEquals(SettingsSheet.LANGUAGE, viewModel.state.value.sheet)
        viewModel.onSettingChanged(language = AppLanguage.ENGLISH)
        advanceUntilIdle()
        assertEquals(AppLanguage.ENGLISH, viewModel.state.value.settings.language)
        assertNull(viewModel.state.value.sheet)
    }

    /**
     * Al entrar en Ajustes el interruptor del cerrojo se encendia solo, con su animacion, porque
     * el primer valor del flujo llegaba despues de pintar. Lo que se ve no es un dato cargando:
     * es el valor por defecto cambiando a la vista, y pasaba en cada visita.
     *
     * Con los ajustes del telefono ya leidos, el estado inicial trae el bueno: **antes** de
     * avanzar el reloj, o sea antes de que el flujo emita nada.
     */
    @Test
    fun `it opens with the lock the phone already reported, without waiting for the flow`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        dependencies.devicePreferences.set(DeviceSettings(lockEnabled = true))

        val viewModel = SettingsViewModel(dependencies)

        assertTrue(
            viewModel.state.value.device.lockEnabled,
            "la pantalla empezo por el valor por defecto y lo cambio a la vista",
        )
    }

    /**
     * Ajustes ensenaba el cerrojo apagado aunque el telefono lo tuviera puesto.
     *
     * La carga son cinco lecturas de red, y el valor del cerrojo sale de DataStore, asi que llega
     * en ese hueco. La carga escribia el estado entero a partir de la copia con la que empezo, y se
     * llevaba por delante todo lo que hubiera llegado mientras. Lo mismo valia para la cola de
     * pendientes, la red y la marca de resincronizacion a medias.
     *
     * El test de encender el cerrojo que ya habia no lo veia porque miraba el almacen, que estaba
     * bien, y no lo que la pantalla ensena.
     */
    @Test
    fun `the load does not undo what arrived while its reads were in flight`() = runTest {
        val gate = CompletableDeferred<Unit>()
        val dependencies = FakeSettingsDependencies(categoryGateway = SlowCategories(gate))
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()

        dependencies.deviceSettings.setLockEnabled(true)
        advanceUntilIdle()
        assertTrue(viewModel.state.value.device.lockEnabled, "el observador no lo pinto")

        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(
            viewModel.state.value.device.lockEnabled,
            "la carga volvio a escribir el estado de antes y apago el cerrojo en la pantalla",
        )
        assertTrue(!viewModel.state.value.loading)
    }

    @Test
    fun `turning the lock on asks for a pin and stores it`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(true)
        assertEquals(SettingsSheet.PIN, viewModel.state.value.sheet)
        viewModel.onPinChanged("12a345")
        assertEquals("1234", viewModel.state.value.pin)
        viewModel.confirmPin()
        advanceUntilIdle()
        assertEquals(SettingsMessage.LockEnabled, viewModel.state.value.message)
        assertTrue(dependencies.deviceSettings.current.lockEnabled)
        assertTrue(dependencies.localLock.verifyPin("1234"))
    }

    @Test
    fun `a short pin cannot be stored`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(true)
        viewModel.onPinChanged("12")
        viewModel.confirmPin()
        advanceUntilIdle()
        assertEquals(SettingsMessage.SaveFailed, viewModel.state.value.message)
        assertTrue(!dependencies.deviceSettings.current.lockEnabled)
    }

    @Test
    fun `turning the lock off clears the pin without asking`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        viewModel.onLockToggled(false)
        advanceUntilIdle()
        assertEquals(SettingsMessage.LockDisabled, viewModel.state.value.message)
        assertTrue(!dependencies.localLock.hasPin())
    }

    @Test
    fun `signing out revokes the session and tells the caller`() = runTest {
        val dependencies = FakeSettingsDependencies.working()
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()
        var done = false
        viewModel.signOut { done = true }
        advanceUntilIdle()
        assertTrue(done)
        assertTrue(dependencies.authGateway.calls.contains("logout"))
    }

    @Test
    fun `a resync with pending writes does not touch the database`() = runTest {
        val dependencies = FakeSettingsDependencies(
            sync = FakeSyncStore(pending = 3),
            changeGateway = FakeChangeGateway(),
        )
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()

        viewModel.resync()
        advanceUntilIdle()

        // Borrar lo local con cosas sin subir es perder movimientos del usuario.
        assertTrue(dependencies.sync.log.isEmpty())
        assertEquals(ResyncProgress.IDLE, viewModel.state.value.resync)
        assertNull(viewModel.state.value.message, "lo cuenta la hoja, no un toast")
    }

    @Test
    fun `a resync that finishes says so and the row goes back to idle`() = runTest {
        val dependencies = FakeSettingsDependencies(
            changeGateway = FakeChangeGateway(listOf(BATCH)),
        )
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()

        viewModel.resync()
        advanceUntilIdle()

        assertEquals(SettingsMessage.ResyncDone, viewModel.state.value.message)
        assertEquals(ResyncProgress.IDLE, viewModel.state.value.resync)
        assertEquals(listOf("start", "save:c1", "advance:c1", "finish"), dependencies.sync.log)
    }

    @Test
    fun `a resync cut in half leaves the row saying it is unfinished`() = runTest {
        val dependencies = FakeSettingsDependencies(
            changeGateway = FakeChangeGateway(
                batches = listOf(ChangeBatch(cursor = "c1", hasMore = true)),
                failAfter = 1,
            ),
        )
        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()

        viewModel.resync()
        advanceUntilIdle()

        assertEquals(ResyncProgress.UNFINISHED, viewModel.state.value.resync)
        assertEquals(SettingsMessage.ResyncFailed, viewModel.state.value.message)
    }

    @Test
    fun `a resync left unfinished by a previous run is announced on open`() = runTest {
        val dependencies = FakeSettingsDependencies(sync = FakeSyncStore(resyncing = true))

        val viewModel = SettingsViewModel(dependencies)
        advanceUntilIdle()

        assertEquals(ResyncProgress.UNFINISHED, viewModel.state.value.resync)
    }

    @Test
    fun `the message is cleared once the toast has been shown`() = runTest {
        val viewModel = SettingsViewModel(FakeSettingsDependencies.working())
        advanceUntilIdle()
        viewModel.onLockToggled(false)
        advanceUntilIdle()
        viewModel.onMessageShown()
        assertNull(viewModel.state.value.message)
    }

    private companion object {
        val BATCH = ChangeBatch(cursor = "c1", hasMore = false)
    }
}

/**
 * Un catalogo de categorias que se queda esperando. Es la quinta y ultima lectura de la carga,
 * asi que retenerla deja la carga en vuelo y permite que llegue algo por otro camino.
 */
private class SlowCategories(
    private val gate: CompletableDeferred<Unit>,
    private val real: CategoryGateway = FakeCategoryGateway(),
) : CategoryGateway by real {
    override suspend fun list(): Outcome<List<Category>, ApiError> {
        gate.await()
        return real.list()
    }
}
