package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.common.Outcome
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Lo que promete la cache, comprobado en las dos direcciones: que no repite una peticion
 * cuando no debe, y que si sale a la red cuando toca.
 */
// runCurrent es experimental todavia, y hace falta: deja correr la revalidacion que quedo
// encolada en segundo plano. advanceUntilIdle no sirve, porque desde la version 1.10 no
// ejecuta el trabajo del backgroundScope.
@OptIn(ExperimentalCoroutinesApi::class)
class MemoryCacheTest {

    @Test
    fun `the first read goes to the network and the second does not`() = runTest {
        val source = CountingSource()
        val cache = cacheOf()

        assertEquals(Outcome.Success(FIRST), cache.read(KEY, source::fetch))
        assertEquals(Outcome.Success(FIRST), cache.read(KEY, source::fetch))

        assertEquals(1, source.calls)
    }

    @Test
    fun `every key is stored on its own`() = runTest {
        val source = CountingSource()
        val cache = cacheOf()

        cache.read(KEY, source::fetch)
        cache.read("another", source::fetch)

        assertEquals(2, source.calls)
    }

    @Test
    fun `past the window it serves what it has and revalidates behind`() = runTest {
        val source = CountingSource()
        val ticker = TestTicker()
        val cache = cacheOf(ticker)
        cache.read(KEY, source::fetch)

        ticker.advance(STALE)
        source.value = SECOND
        // Lo que se sirve es todavia lo viejo: la vista pinta ya, sin esperar a la red.
        assertEquals(Outcome.Success(FIRST), cache.read(KEY, source::fetch))

        runCurrent()
        assertEquals(2, source.calls)
        // Y la lectura siguiente ya tiene lo nuevo, sin haber pedido nada mas.
        assertEquals(Outcome.Success(SECOND), cache.read(KEY, source::fetch))
        assertEquals(2, source.calls)
    }

    @Test
    fun `two stale reads at once launch a single revalidation`() = runTest {
        val source = CountingSource()
        val ticker = TestTicker()
        val cache = cacheOf(ticker)
        cache.read(KEY, source::fetch)
        ticker.advance(STALE)

        cache.read(KEY, source::fetch)
        cache.read(KEY, source::fetch)
        runCurrent()

        assertEquals(2, source.calls)
    }

    @Test
    fun `past the hard limit it waits for the network`() = runTest {
        val source = CountingSource()
        val ticker = TestTicker()
        val cache = cacheOf(ticker)
        cache.read(KEY, source::fetch)

        ticker.advance(TOO_OLD)
        source.value = SECOND

        // Ensenar datos de hace horas como si fueran de ahora es peor que esperar.
        assertEquals(Outcome.Success(SECOND), cache.read(KEY, source::fetch))
        assertEquals(2, source.calls)
    }

    @Test
    fun `a failure is not stored`() = runTest {
        val source = CountingSource()
        val cache = cacheOf()
        source.failure = ApiError.Network

        assertEquals(Outcome.Failure(ApiError.Network), cache.read(KEY, source::fetch))
        source.failure = null
        assertEquals(Outcome.Success(FIRST), cache.read(KEY, source::fetch))

        assertEquals(2, source.calls)
    }

    @Test
    fun `a failed revalidation does not wipe what was there`() = runTest {
        val source = CountingSource()
        val ticker = TestTicker()
        val cache = cacheOf(ticker)
        cache.read(KEY, source::fetch)

        ticker.advance(STALE)
        source.failure = ApiError.Network
        cache.read(KEY, source::fetch)
        runCurrent()

        // Sigue habiendo algo viejo que servir, que es mejor que una pantalla de error.
        assertEquals(Outcome.Success(FIRST), cache.read(KEY, source::fetch))
    }

    @Test
    fun `invalidating forces the next read to ask again`() = runTest {
        val source = CountingSource()
        val cache = cacheOf()
        cache.read(KEY, source::fetch)

        cache.invalidate()
        cache.read(KEY, source::fetch)

        assertEquals(2, source.calls)
    }

    @Test
    fun `a response that lands after an invalidation is thrown away`() = runTest {
        val source = CountingSource()
        val cache = cacheOf()

        // La peticion arranca, y mientras esta en vuelo el usuario registra un movimiento.
        val result = cache.read(KEY) {
            cache.invalidate()
            source.fetch()
        }

        assertEquals(Outcome.Success(FIRST), result)
        // Lo que devolvio no se guardo, asi que la lectura siguiente vuelve a la red.
        cache.read(KEY, source::fetch)
        assertEquals(2, source.calls)
    }

    @Test
    fun `the cache does not grow past its cap`() = runTest {
        val source = CountingSource()
        val cache = cacheOf(policy = policyOf(maxEntries = 2))

        cache.read("a", source::fetch)
        cache.read("b", source::fetch)
        cache.read("c", source::fetch)

        assertEquals(2, cache.size())
        // La que se fue es la primera que se guardo.
        cache.read("a", source::fetch)
        assertEquals(4, source.calls)
    }

    private fun policyOf(maxEntries: Int = MANY) =
        CachePolicy(fresh = FRESH, maxStale = MAX_STALE, maxEntries = maxEntries)

    private fun TestScope.cacheOf(
        ticker: TestTicker = TestTicker(),
        policy: CachePolicy = policyOf(),
    ): MemoryCache<String, String> = MemoryCache(policy, ticker, backgroundScope)

    private class CountingSource {
        var calls: Int = 0
            private set
        var value: String = FIRST
        var failure: ApiError? = null

        suspend fun fetch(): Outcome<String, ApiError> {
            calls++
            return failure?.let { Outcome.Failure(it) } ?: Outcome.Success(value)
        }
    }

    private companion object {
        const val KEY = "key"
        const val FIRST = "one"
        const val SECOND = "two"
        const val MANY = 32
        val FRESH = 45.seconds
        val MAX_STALE = 5.minutes

        /** Suficiente para pasar de fresco a rancio sin pasarse del plazo maximo. */
        val STALE = 1.minutes

        /** Suficiente para pasarse del plazo maximo. */
        val TOO_OLD = 10.minutes
    }
}
