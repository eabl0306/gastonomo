package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.port.BiometricAvailability
import com.bahoque.gastonomo.core.application.port.BiometricLock
import com.bahoque.gastonomo.core.application.port.BiometricPromptText
import com.bahoque.gastonomo.core.application.port.BiometricResult
import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.domain.settings.DeviceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** Cerrojo local en memoria. Guarda el PIN en claro porque es un doble de test. */
class FakeLocalLock(private var pin: String? = null) : LocalLock {
    override suspend fun hasPin(): Boolean = pin != null

    override suspend fun setPin(pin: String) {
        this.pin = pin
    }

    override suspend fun verifyPin(pin: String): Boolean = this.pin == pin

    override suspend fun clearPin() {
        pin = null
    }
}

/** Huella con la respuesta que decida el test. */
class FakeBiometricLock(
    private val availability: BiometricAvailability = BiometricAvailability.AVAILABLE,
    private val result: BiometricResult = BiometricResult.Succeeded,
) : BiometricLock {
    override suspend fun availability(): BiometricAvailability = availability

    override suspend fun authenticate(text: BiometricPromptText): BiometricResult = result
}

/** Ajustes del dispositivo en memoria. */
class FakeDeviceSettingsStore(initial: DeviceSettings = DeviceSettings()) : DeviceSettingsStore {

    private val state = MutableStateFlow(initial)

    /** El valor actual, para asertar sin recoger el flujo. */
    val current: DeviceSettings get() = state.value

    override val settings: Flow<DeviceSettings> = state

    override suspend fun setLockEnabled(enabled: Boolean) {
        state.value = state.value.copy(lockEnabled = enabled)
    }

    override suspend fun setHideAmounts(hidden: Boolean) {
        state.value = state.value.copy(hideAmounts = hidden)
    }
}
