package com.bahoque.gastonomo.core.application.usecase

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.application.state.ResyncResult
import com.bahoque.gastonomo.core.application.usecase.sync.ResyncAccount
import com.bahoque.gastonomo.core.application.usecase.sync.SyncAccount
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.testing.fake.FakeChangeGateway
import com.bahoque.gastonomo.core.testing.fake.FakeSyncStore
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SyncUseCasesTest {

    @Test
    fun `it asks from the cursor it has and pages until there is nothing left`() = runTest {
        val gateway = FakeChangeGateway(
            listOf(
                ChangeBatch(cursor = "c1", hasMore = true, categories = listOf(SampleFinance.pets)),
                ChangeBatch(cursor = "c2", hasMore = false),
            ),
        )
        val store = FakeSyncStore(cursor = "c0")

        val result = SyncAccount(gateway, store)()

        assertEquals(2, (result as Outcome.Success<Int>).value)
        assertEquals(listOf<String?>("c0", "c1"), gateway.asked)
        assertEquals("c2", store.cursor())
    }

    @Test
    fun `the cursor only moves after the batch is written`() = runTest {
        val gateway = FakeChangeGateway(
            listOf(ChangeBatch(cursor = "c1", hasMore = false)),
        )
        val store = FakeSyncStore()

        SyncAccount(gateway, store)()

        // El orden es la regla: escribir y solo entonces avanzar. Al contrario, un fallo al
        // escribir perderia el lote para siempre porque nadie lo vuelve a mandar.
        assertEquals(listOf("save:c1", "advance:c1"), store.log)
    }

    @Test
    fun `a batch that cannot be written leaves the cursor where it was`() = runTest {
        val gateway = FakeChangeGateway(listOf(ChangeBatch(cursor = "c1", hasMore = false)))
        val store = FakeSyncStore(cursor = "c0")
        store.failOnSave = true

        assertFailsWith<IllegalStateException> { SyncAccount(gateway, store)() }

        assertEquals("c0", store.cursor())
        assertTrue(store.log.none { it.startsWith("advance") })
    }

    @Test
    fun `a cold start asks without a cursor`() = runTest {
        val gateway = FakeChangeGateway(listOf(ChangeBatch(cursor = "c1", hasMore = false)))

        SyncAccount(gateway, FakeSyncStore(cursor = null))()

        assertEquals(listOf<String?>(null), gateway.asked)
    }

    @Test
    fun `a network failure stops the sync and keeps what was already written`() = runTest {
        val gateway = FakeChangeGateway(
            batches = listOf(ChangeBatch(cursor = "c1", hasMore = true)),
            failAfter = 1,
        )
        val store = FakeSyncStore()

        val result = SyncAccount(gateway, store)()

        assertEquals(ApiError.Network, (result as Outcome.Failure).error)
        assertEquals("c1", store.cursor(), "el primer lote se guardo y cuenta")
    }

    @Test
    fun `a resync with pending writes does not delete anything`() = runTest {
        val store = FakeSyncStore(pending = 2)
        val sync = SyncAccount(FakeChangeGateway(), store)

        val result = ResyncAccount(store, sync)()

        assertEquals(ResyncResult.HasPendingWrites(2), result)
        assertTrue(store.log.isEmpty(), "no se toco la base")
    }

    @Test
    fun `a resync marks itself before deleting and clears the mark at the end`() = runTest {
        val store = FakeSyncStore(cursor = "viejo")
        val gateway = FakeChangeGateway(listOf(ChangeBatch(cursor = "c1", hasMore = false)))

        val result = ResyncAccount(store, SyncAccount(gateway, store))()

        assertEquals(ResyncResult.Done, result)
        assertEquals(listOf("start", "save:c1", "advance:c1", "finish"), store.log)
        assertEquals(listOf<String?>(null), gateway.asked, "empieza desde el principio")
    }

    @Test
    fun `a resync cut in half keeps the mark so the next start finishes it`() = runTest {
        val store = FakeSyncStore()
        val gateway = FakeChangeGateway(
            batches = listOf(ChangeBatch(cursor = "c1", hasMore = true)),
            failAfter = 1,
        )

        val result = ResyncAccount(store, SyncAccount(gateway, store))()

        assertTrue(result is ResyncResult.Failed)
        assertTrue(store.fullResyncPending(), "nadie puede creer que esto esta completo")
        assertTrue(store.log.none { it == "finish" })
    }

    @Test
    fun `finishing an interrupted resync clears the mark`() = runTest {
        val store = FakeSyncStore(cursor = "c1", resyncing = true)
        val gateway = FakeChangeGateway(listOf(ChangeBatch(cursor = "c2", hasMore = false)))

        SyncAccount(gateway, store)()

        assertTrue(!store.fullResyncPending())
    }
}
