package com.bahoque.gastonomo.data.cache

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/** Los limites de la politica, que son los que hacen que la cache no mienta. */
class CachePolicyTest {

    @Test
    fun `the stale window cannot be shorter than the fresh one`() {
        assertFailsWith<IllegalArgumentException> {
            CachePolicy(fresh = 5.minutes, maxStale = 1.minutes, maxEntries = 4)
        }
    }

    @Test
    fun `a cache without room for anything makes no sense`() {
        assertFailsWith<IllegalArgumentException> {
            CachePolicy(fresh = 1.seconds, maxStale = 1.minutes, maxEntries = 0)
        }
    }

    @Test
    fun `what the user writes expires much sooner than the catalog`() {
        assertTrue(CachePolicy.WRITEABLE.fresh < CachePolicy.CATALOG.fresh)
        assertTrue(CachePolicy.WRITEABLE.maxEntries > CachePolicy.CATALOG.maxEntries)
    }

    @Test
    fun `the elapsed ticker never goes backwards`() {
        val first = ElapsedTicker.nowMillis()
        val second = ElapsedTicker.nowMillis()

        assertTrue(second >= first)
    }

    @Test
    fun `the test ticker moves only when the test says so`() {
        val ticker = TestTicker()
        val start = ticker.nowMillis()

        ticker.advance(1.minutes)

        assertEquals(start + 1.minutes.inWholeMilliseconds, ticker.nowMillis())
    }
}
