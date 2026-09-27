package com.bahoque.gastonomo.core.application.usecase.sync

import com.bahoque.gastonomo.core.application.port.NetworkMonitor
import kotlinx.coroutines.flow.Flow

/** Si hay por donde intentar hablar con la API. */
class ObserveConnection(private val monitor: NetworkMonitor) {
    operator fun invoke(): Flow<Boolean> = monitor.connected()
}
