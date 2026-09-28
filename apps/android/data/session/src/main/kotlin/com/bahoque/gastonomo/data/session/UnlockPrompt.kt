package com.bahoque.gastonomo.data.session

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import com.bahoque.gastonomo.core.application.port.BiometricPromptText

/**
 * Lo que se acepta para desbloquear: la biometria del dispositivo, y **nada mas**.
 *
 * Aqui estaba tambien `DEVICE_CREDENTIAL`, y era un fallo doble.
 *
 * El de cara: `PromptInfo` no admite texto de boton negativo si se permite la credencial del
 * dispositivo, asi que construirlo lanzaba `IllegalArgumentException` y la aplicacion se
 * cerraba al tocar la huella.
 *
 * El de fondo, que es el que importa: el boton dice "Usar PIN", y ese PIN es el de cuatro
 * digitos de Gastonomo, el del teclado que hay debajo. Permitir la credencial del dispositivo
 * dejaba entrar con el patron del movil, que no es el cerrojo de la aplicacion y se salta su
 * limite de cinco intentos.
 */
internal const val UNLOCK_AUTHENTICATORS = BiometricManager.Authenticators.BIOMETRIC_WEAK

/**
 * El dialogo del sistema.
 *
 * Esta fuera de [AndroidBiometricLock] a proposito: construirlo no necesita sensor ni
 * actividad, asi que es la parte del cerrojo que si se puede ejecutar y comprobar en la JVM.
 * `PromptInfo.Builder.build()` valida la combinacion de textos y autenticadores, y ahi es
 * donde se rompia.
 */
internal fun unlockPromptInfo(text: BiometricPromptText): BiometricPrompt.PromptInfo =
    BiometricPrompt.PromptInfo.Builder()
        .setTitle(text.title)
        .setSubtitle(text.subtitle)
        .setNegativeButtonText(text.negativeButton)
        .setAllowedAuthenticators(UNLOCK_AUTHENTICATORS)
        .build()
