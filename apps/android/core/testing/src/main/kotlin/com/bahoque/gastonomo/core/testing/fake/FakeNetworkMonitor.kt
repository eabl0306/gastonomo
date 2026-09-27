package com.bahoque.gastonomo.core.testing.fake

import com.bahoque.gastonomo.core.application.port.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** La conectividad, programable. Por defecto hay red, que es el caso de casi todos los tests. */
class FakeNetworkMonitor(connected: Boolean = true) : NetworkMonitor {

    private val mutable = MutableStateFlow(connected)

    override fun connected(): Flow<Boolean> = mutable.asStateFlow()

    /** Enciende o apaga la red en mitad de un test. */
    fun set(connected: Boolean) {
        mutable.value = connected
    }
}
