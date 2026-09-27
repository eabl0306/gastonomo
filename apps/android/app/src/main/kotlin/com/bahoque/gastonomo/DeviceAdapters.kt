package com.bahoque.gastonomo

import com.bahoque.gastonomo.core.application.port.BiometricLock
import com.bahoque.gastonomo.core.application.port.Clock
import com.bahoque.gastonomo.core.application.port.DeviceSettingsStore
import com.bahoque.gastonomo.core.application.port.LocalLock
import com.bahoque.gastonomo.core.application.port.NetworkMonitor
import com.bahoque.gastonomo.core.application.port.SessionStore

/** Los adaptadores que hablan con el dispositivo: almacenamiento, cerrojo y reloj. */
internal data class DeviceAdapters(
    val sessionStore: SessionStore,
    val localLock: LocalLock,
    val deviceSettings: DeviceSettingsStore,
    val biometricLock: BiometricLock,
    val clock: Clock,

    /** Si hay por donde salir a internet. No dice si la API responde: eso se sabe intentandolo. */
    val network: NetworkMonitor,
)
