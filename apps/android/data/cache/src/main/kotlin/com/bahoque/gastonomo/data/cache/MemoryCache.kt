package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Respuestas de la API guardadas en memoria, vivas mientras viva el proceso.
 *
 * No es persistencia: al cerrar la aplicacion no queda nada. Lo unico que hace es no repetir
 * una peticion que se acaba de hacer, que es lo que pasaba al salir de una pantalla y volver a
 * ella. Los plazos estan en [CachePolicy].
 *
 * Cuatro cosas que conviene saber antes de tocarla:
 *
 * - **Un fallo no se guarda**, asi que el boton de reintentar siempre sale a la red.
 * - **Un fallo tampoco borra lo que habia.** Un corte de red no puede dejar la cache peor de
 *   como estaba: la entrada se queda con su edad y la siguiente lectura lo reintenta.
 * - **La peticion corre en el contexto de quien lee**, no en un hilo aparte. Asi
 *   `collectLatest` sigue cancelando la carga del mes anterior cuando el usuario pulsa la
 *   flecha del selector dos veces seguidas. La unica excepcion es la revalidacion, que es a
 *   proposito independiente de quien la disparo.
 * - **Una respuesta que llega despues de una invalidacion se tira.** Si no, una revalidacion
 *   lanzada justo antes de registrar un movimiento volveria a guardar la lista de antes, y la
 *   pantalla tardaria un plazo entero en ensenar lo que el usuario acaba de escribir.
 *
 * Es provisional: cuando exista una base de datos local como unica fuente de verdad, esta
 * clase se retira entera. Ver el issue 14 del repositorio.
 */
class MemoryCache<K, V : Any>(
    private val policy: CachePolicy,
    private val ticker: CacheTicker,
    private val scope: CoroutineScope,
) {

    private val mutex = Mutex()
    private val entries = LinkedHashMap<K, Entry<V>>()

    /** Las claves con una revalidacion en marcha, para no lanzar dos de la misma. */
    private val revalidating = mutableSetOf<K>()

    /** Sube con cada invalidacion. Es lo que delata a una respuesta que llego tarde. */
    private var generation = 0L

    /** Lo guardado si todavia sirve; si no, lo que devuelva [fetch]. */
    suspend fun read(key: K, fetch: suspend () -> Outcome<V, ApiError>): Outcome<V, ApiError> {
        val hit = hitOrNull(key) ?: return fetchAndStore(key, fetch)
        if (hit.revalidate) revalidate(key, fetch)
        return Outcome.Success(hit.value)
    }

    /** Tira lo guardado. La llama cada escritura que puede haber cambiado estos datos. */
    suspend fun invalidate() {
        mutex.withLock {
            entries.clear()
            generation++
        }
    }

    /** Cuantas entradas hay guardadas. Para los tests y para depurar. */
    suspend fun size(): Int = mutex.withLock { entries.size }

    private suspend fun hitOrNull(key: K): Hit<V>? = mutex.withLock {
        val entry = entries[key] ?: return@withLock null
        val age = ticker.nowMillis() - entry.storedAt
        when {
            age < policy.fresh.inWholeMilliseconds -> Hit(entry.value, revalidate = false)
            // El reclamo entra en el mismo cerrojo que la lectura: dos pantallas que piden lo
            // mismo a la vez no pueden lanzar dos revalidaciones de la misma clave.
            age < policy.maxStale.inWholeMilliseconds ->
                Hit(entry.value, revalidate = revalidating.add(key))
            else -> null
        }
    }

    private suspend fun fetchAndStore(
        key: K,
        fetch: suspend () -> Outcome<V, ApiError>,
    ): Outcome<V, ApiError> {
        val startedAt = mutex.withLock { generation }
        val result = fetch()
        if (result is Outcome.Success) store(key, result.value, startedAt)
        return result
    }

    private suspend fun store(key: K, value: V, expected: Long) = mutex.withLock {
        if (expected != generation) return@withLock
        // Se borra antes de poner para que la clave se vaya al final del orden de insercion:
        // asi la primera del mapa es siempre la que se guardo hace mas tiempo.
        entries.remove(key)
        entries[key] = Entry(value, ticker.nowMillis())
        while (entries.size > policy.maxEntries) {
            entries.remove(entries.keys.first())
        }
    }

    /** Refresca en segundo plano, sin hacer esperar a quien leyo. */
    private fun revalidate(key: K, fetch: suspend () -> Outcome<V, ApiError>) {
        scope.launch {
            try {
                fetchAndStore(key, fetch)
            } finally {
                mutex.withLock { revalidating.remove(key) }
            }
        }
    }

    private data class Entry<V : Any>(val value: V, val storedAt: Long)

    private data class Hit<V : Any>(val value: V, val revalidate: Boolean)
}
