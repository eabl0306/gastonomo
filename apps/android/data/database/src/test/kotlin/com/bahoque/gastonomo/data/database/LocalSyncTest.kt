package com.bahoque.gastonomo.data.database

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.port.ChangeBatch
import com.bahoque.gastonomo.core.domain.category.Category
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.settings.UserSettings
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.data.database.entity.CategoryEntity
import com.bahoque.gastonomo.data.database.entity.MovementEntity
import com.bahoque.gastonomo.data.database.entity.PendingWriteEntity
import com.bahoque.gastonomo.data.database.local.LocalSync
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Lo que guarda un lote de cambios, y lo que **no** borra una resincronizacion completa.
 *
 * La regla que vigila el ultimo test es la que mas duele si se rompe: borrar lo local con
 * escrituras sin subir es perder movimientos del usuario.
 */
@RunWith(AndroidJUnit4::class)
internal class LocalSyncTest {

    @get:Rule
    val local = LocalDatabaseRule()

    private val sync: LocalSync get() = LocalSync(local.database)

    @Test
    fun `a device that never synced has no cursor`() = runTest {
        assertNull(sync.cursor())
    }

    @Test
    fun `the cursor survives and the batch lands`() = runTest {
        val store = sync

        store.save(
            ChangeBatch(
                cursor = "c1",
                hasMore = false,
                movements = listOf(SampleFinance.groceries),
                categories = SampleFinance.categories,
                settings = SETTINGS,
            ),
        )
        store.advance("c1")

        assertEquals("c1", store.cursor())
        assertEquals(1, local.database.movements().between(DAY, DAY).size)
        assertEquals(SampleFinance.categories.size, local.database.categories().all().size)
        assertEquals(SETTINGS.language.wire, local.database.account().settings()?.language)
    }

    @Test
    fun `a batch does not delete what it did not bring`() = runTest {
        val store = sync
        local.database.categories().upsert(listOf(CategoryEntity.of(EXTRA)))

        store.save(ChangeBatch(cursor = "c1", hasMore = false, categories = listOf(SampleFinance.pets)))

        // Un lote es incremental: reemplazar la tabla se llevaria por delante lo que no venia.
        assertTrue(local.database.categories().all().any { it.id == EXTRA.id.value })
    }

    @Test
    fun `a full resync keeps what has not been uploaded yet`() = runTest {
        val store = sync
        local.database.movements().upsert(
            listOf(MovementEntity.of(SampleFinance.groceries).copy(pending = true)),
        )
        local.database.pending().upsert(PENDING_WRITE)

        store.startFullResync()

        assertEquals(
            1,
            local.database.movements().between(DAY, DAY).size,
            "un movimiento sin subir no lo tiene nadie mas: borrarlo es perderlo",
        )
        assertEquals(1, local.database.pending().count())
        assertTrue(store.fullResyncPending())
        assertNull(store.cursor(), "empieza desde el principio")
    }

    @Test
    fun `a full resync clears what is already uploaded`() = runTest {
        val store = sync
        local.database.movements().upsert(listOf(MovementEntity.of(SampleFinance.groceries)))
        local.database.categories().upsert(SampleFinance.categories.map(CategoryEntity::of))

        store.startFullResync()

        assertTrue(local.database.movements().between(DAY, DAY).isEmpty())
        assertTrue(local.database.categories().all().isEmpty())
    }

    @Test
    fun `finishing clears the mark and keeps the cursor`() = runTest {
        val store = sync
        store.startFullResync()
        store.advance("c9")

        store.finishFullResync()

        assertTrue(!store.fullResyncPending())
        assertEquals("c9", store.cursor())
    }

    private companion object {
        val SETTINGS = UserSettings()

        val DAY: LocalDate = LocalDate.parse("2026-09-15")

        /** Una categoria que ningun lote trae, para ver que un lote no borra lo que no menciona. */
        val EXTRA: Category = SampleFinance.pets.copy(id = CategoryId("otra"), name = "Otra")

        val PENDING_WRITE = PendingWriteEntity(
            id = "w1",
            kind = "movement",
            payload = "{}",
            idempotencyKey = "k1",
            loanId = null,
            dependsOn = null,
            queuedAt = 0,
            attempts = 0,
            failure = null,
        )
    }
}
