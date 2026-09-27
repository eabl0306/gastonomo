package com.bahoque.gastonomo.feature.settings

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
import com.bahoque.gastonomo.core.application.usecase.sync.ObservePendingWrites
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

    /** Solo para el conteo de la fila "Categorias": la pantalla de Categorias es otra feature. */
    val loadCategories: LoadCategories

    val observePendingWrites: ObservePendingWrites

    val correctPendingWrite: CorrectPendingWrite

    val discardPendingWrite: DiscardPendingWrite

    val retryPendingWrites: RetryPendingWrites
}
