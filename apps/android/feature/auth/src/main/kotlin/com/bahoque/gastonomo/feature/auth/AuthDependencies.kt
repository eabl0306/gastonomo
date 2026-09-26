package com.bahoque.gastonomo.feature.auth

import com.bahoque.gastonomo.core.application.usecase.auth.ObserveSignedIn
import com.bahoque.gastonomo.core.application.usecase.auth.ObserveStoredUser
import com.bahoque.gastonomo.core.application.usecase.auth.RequestPasswordReset
import com.bahoque.gastonomo.core.application.usecase.auth.SignIn
import com.bahoque.gastonomo.core.application.usecase.auth.SignOut
import com.bahoque.gastonomo.core.application.usecase.auth.SignUp
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithBiometrics
import com.bahoque.gastonomo.core.application.usecase.auth.UnlockWithPin
import com.bahoque.gastonomo.core.application.usecase.settings.ObserveDeviceSettings

/**
 * Lo que necesitan las pantallas de este modulo. Lo implementa la raiz de composicion de
 * la aplicacion, en su propio fichero: este modulo no conoce a ningun adaptador.
 *
 * Si una pantalla nueva necesita otro caso de uso, se anade aqui y se rellena en
 * `AuthWiring.kt` de `:app`. Son los dos unicos ficheros que hay que tocar, y los dos son
 * de este modulo o suyos, asi que nadie mas los esta editando.
 */
interface AuthDependencies {
    val signIn: SignIn

    val signUp: SignUp

    val requestPasswordReset: RequestPasswordReset

    val unlockWithPin: UnlockWithPin

    val unlockWithBiometrics: UnlockWithBiometrics

    /** Se usa al agotar los intentos de PIN: borra la sesion guardada en el dispositivo. */
    val signOut: SignOut

    val observeSignedIn: ObserveSignedIn

    /** El usuario guardado, para el saludo y el avatar del desbloqueo. No pasa por la red. */
    val observeStoredUser: ObserveStoredUser

    val observeDeviceSettings: ObserveDeviceSettings
}
