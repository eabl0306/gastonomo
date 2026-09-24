package com.bahoque.gastonomo.core.common

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Puerto para los dispatchers de corrutinas. Permite que los tests usen dispatchers
 * deterministas sin que el nucleo dependa de Android ni de kotlinx-coroutines-test.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
    val main: CoroutineDispatcher
}

/** Implementacion de produccion. Los dispatchers se resuelven de forma perezosa. */
object DefaultDispatcherProvider : DispatcherProvider {
    override val io: CoroutineDispatcher get() = Dispatchers.IO
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val main: CoroutineDispatcher get() = Dispatchers.Main
}
