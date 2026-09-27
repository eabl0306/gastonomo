package com.bahoque.gastonomo.data.offline

import com.bahoque.gastonomo.core.common.DefaultDispatcherProvider
import com.bahoque.gastonomo.core.common.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Decide cuando merece la pena volver a pedirle algo a la red, y lo pide en segundo plano.
 *
 * **No guarda datos.** Los datos estan en la base local, que es la unica fuente de verdad. Lo
 * unico que recuerda es *cuando* se refresco cada cosa por ultima vez, para que volver a una
 * pestana cinco veces seguidas no dispare cinco peticiones identicas.
 *
 * Dos reglas que importan:
 *
 * - **Un refresco que falla no cuenta como refresco.** Asi la lectura siguiente lo reintenta en
 *   vez de esperar al plazo, que es justo lo que hace falta cuando vuelve la red.
 * - **Una escritura lo olvida todo.** Registrar un movimiento cambia tambien el contador de uso
 *   de las categorias y el saldo de un prestamo; dejar cualquiera de esas cosas marcada como
 *   fresca es quedarse ensenando el numero de antes.
 *
 * @param every cuanto vale un refresco. Es un plazo corto: lo unico que evita es la rafaga.
 */
class RefreshGate(
    private val clock: RefreshClock = SystemRefreshClock,
    dispatchers: DispatcherProvider = DefaultDispatcherProvider,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatchers.io),
    private val every: Duration = DEFAULT_WINDOW,
) {

    private val mutex = Mutex()
    private val refreshedAt = mutableMapOf<Any, Long>()
    private val running = mutableSetOf<Any>()

    /** Si [key] se refresco hace poco. */
    suspend fun isFresh(key: Any): Boolean = mutex.withLock { freshLocked(key) }

    /** Deja constancia de que [key] acaba de traerse de la red. */
    suspend fun markFresh(key: Any) {
        mutex.withLock { refreshedAt[key] = clock.nowMillis() }
    }

    /**
     * Lanza [refresh] sin hacer esperar a quien leyo, salvo que [key] este fresca o ya haya un
     * refresco suyo en marcha. Es [refresh] quien decide si marcar la clave como fresca: asi un
     * fallo no la marca.
     */
    suspend fun inBackground(key: Any, refresh: suspend () -> Unit) {
        val start = mutex.withLock { !freshLocked(key) && running.add(key) }
        if (!start) return
        scope.launch {
            try {
                refresh()
            } finally {
                mutex.withLock { running.remove(key) }
            }
        }
    }

    /** Olvida lo refrescado. Lo llama toda escritura y el cambio de cuenta. */
    suspend fun forget() {
        mutex.withLock { refreshedAt.clear() }
    }

    private fun freshLocked(key: Any): Boolean {
        val at = refreshedAt[key] ?: return false
        return clock.nowMillis() - at < every.inWholeMilliseconds
    }

    private companion object {
        /** Corto a proposito: lo unico que evita es pedir lo mismo cinco veces en dos segundos. */
        val DEFAULT_WINDOW = 45.seconds
    }
}

/**
 * El paso del tiempo para el plazo de refresco, monotono.
 *
 * No sale del reloj de pared: si el usuario cambia la hora del telefono, un reloj de pared salta
 * hacia atras y algo marcado "en el futuro" no caducaria nunca.
 */
fun interface RefreshClock {
    fun nowMillis(): Long
}

/** El tiempo desde que arranco la maquina virtual, que nunca va hacia atras. */
object SystemRefreshClock : RefreshClock {

    override fun nowMillis(): Long = System.nanoTime() / NANOS_PER_MILLI

    private const val NANOS_PER_MILLI = 1_000_000L
}
