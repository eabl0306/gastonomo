package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.core.application.port.SyncStore
import com.bahoque.gastonomo.core.application.usecase.sync.ResyncAccount
import com.bahoque.gastonomo.feature.settings.SettingsDependencies

/**
 * Lo que `:feature:settings` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun settingsDependencies(
    useCases: UseCases,
    resync: ResyncAccount,
    sync: SyncStore,
): SettingsDependencies =
    object : SettingsDependencies {
        override val loadSettings = useCases.loadSettings
        override val updateSettings = useCases.updateSettings
        override val loadProfile = useCases.loadProfile
        override val loadCurrencyCatalog = useCases.loadCurrencyCatalog
        override val loadExchangeRates = useCases.loadExchangeRates
        override val signOut = useCases.signOut
        override val changePassword = useCases.changePassword
        override val setUpLocalLock = useCases.setUpLocalLock
        override val observeDeviceSettings = useCases.observeDeviceSettings
        override val loadCategories = useCases.loadCategories
        override val observePendingWrites = useCases.observePendingWrites
        override val correctPendingWrite = useCases.correctPendingWrite
        override val discardPendingWrite = useCases.discardPendingWrite
        override val retryPendingWrites = useCases.retryPendingWrites
        override val observeConnection = useCases.observeConnection
        override val resyncAccount = resync
        override val syncStore = sync
    }
