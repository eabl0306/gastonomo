package com.bahoque.gastonomo.core.application.usecase.auth

import com.bahoque.gastonomo.core.application.port.BiometricLock
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult

/** Pide la huella. Los textos del dialogo salen de `strings.xml` y llegan ya traducidos. */
class UnlockWithBiometrics(private val biometricLock: BiometricLock) {
    suspend operator fun invoke(text: BiometricPromptText): BiometricResult =
        biometricLock.authenticate(text)
}
