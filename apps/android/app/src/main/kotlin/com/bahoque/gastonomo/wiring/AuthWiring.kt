package com.bahoque.gastonomo.wiring

import com.bahoque.gastonomo.UseCases
import com.bahoque.gastonomo.feature.auth.AuthDependencies

/**
 * Lo que `:feature:auth` necesita, sacado del grafo de la aplicacion.
 *
 * Este fichero es solo de esa feature: anadir un caso de uso aqui no toca nada de las
 * demas.
 */
internal fun authDependencies(useCases: UseCases): AuthDependencies =
    object : AuthDependencies {
        override val signIn = useCases.signIn
        override val signUp = useCases.signUp
        override val requestPasswordReset = useCases.requestPasswordReset
        override val unlockWithPin = useCases.unlockWithPin
        override val unlockWithBiometrics = useCases.unlockWithBiometrics
        override val signOut = useCases.signOut
        override val observeSignedIn = useCases.observeSignedIn
        override val observeStoredUser = useCases.observeStoredUser
        override val observeDeviceSettings = useCases.observeDeviceSettings
    }
