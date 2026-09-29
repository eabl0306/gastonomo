package com.bahoque.gastonomo.feature.settings

import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.core.application.state.DevicePreferences
import com.bahoque.gastonomo.core.application.state.UserPreferences
import com.bahoque.gastonomo.core.application.usecase.auth.ChangePassword
import com.bahoque.gastonomo.core.application.usecase.auth.LoadProfile
import com.bahoque.gastonomo.core.application.usecase.auth.SetUpLocalLock
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.category.LoadCategories
import com.bahoque.gastonomo.core.application.usecase.currency.LoadCurrencyCatalog
import com.bahoque.gastonomo.core.application.usecase.currency.LoadExchangeRates
import com.bahoque.gastonomo.core.application.usecase.settings.LoadSettings
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings
import com.bahoque.gastonomo.core.application.usecase.settings.UpdateSettings
import com.bahoque.gastonomo.core.application.usecase.sync.CorrectPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.DiscardPendingWrite
import com.bahoque.gastonomo.core.application.usecase.sync.ObserveConnection
import com.bahoque.gastonomo.core.application.usecase.sync.ObservePendingWrites
import com.bahoque.gastonomo.core.application.usecase.sync.ResyncAccount
import com.bahoque.gastonomo.core.application.usecase.sync.RetryPendingWrites

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `SettingsWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface SettingsDependencies {
    val loadSettings: LoadSettings

    val updateSettings: UpdateSettings

    val loadProfile: LoadProfile

    val loadCurrencyCatalog: LoadCurrencyCatalog

    val loadExchangeRates: LoadExchangeRates

    val signOut: SignOut

    val changePassword: ChangePassword

    val setUpLocalLock: SetUpLocalLock

    val observeDeviceSettings: ObserveDeviceSettings

    /**
     * Lo que ya se sabe al abrir la pantalla, para no empezar por los valores por defecto y
     * cambiarlos a la vista. El cerrojo sale de aqui, no del primer valor del flujo.
     */
    val devicePreferences: DevicePreferences

    /** Lo mismo para la moneda principal y el idioma, que el tema ya tiene leidos. */
    val preferences: UserPreferences

    /** Solo para el conteo de la fila "Categorias": la pantalla de Categorias es otra feature. */
    val loadCategories: LoadCategories

    val observePendingWrites: ObservePendingWrites

    val correctPendingWrite: CorrectPendingWrite

    val discardPendingWrite: DiscardPendingWrite

    val retryPendingWrites: RetryPendingWrites

    val observeConnection: ObserveConnection

    /** La red de seguridad: borra lo local y lo baja otra vez. */
    val resyncAccount: ResyncAccount

    /** Si quedo una resincronizacion a medias, para decirlo en la fila. */
    val syncStore: SyncStore
}
