package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.settings.SettingsDependencies

/**
 * Lo que `:feature:settings` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun settingsDependencies(useCases: UseCases): SettingsDependencies =
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
    }
