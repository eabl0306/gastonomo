package com.bahoque.gastonomo.data.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.error.ConflictCode
import com.bahoque.gastonomo.core.application.error.FieldError
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Lo que pasa cuando vuelve la red.
 *
 * El test que de verdad importa es el de la clave: **un reintento usa la misma clave de
 * idempotencia que el primer intento**. Si usara otra, un timeout que en realidad si llego al
 * servidor crearia el gasto dos veces, y no hay pantalla para borrarlo.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class PendingUploadTest {

    private val scope = TestScope()

    @get:Rule
    val offline = OfflineRule(scope.backgroundScope)

    private val uploader by lazy {
        pendingUploader(offline.movementSpy, offline.loanSpy, offline.store, offline.gate)
    }

    @Test
    fun `what was registered offline goes up when the network comes back`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        assertEquals(1, offline.store.pending.count())

        offline.movementSpy.failure = null
        val outcome = uploader.uploadAll()

        assertEquals(1, outcome.uploaded)
        assertEquals(0, offline.store.pending.count())
    }

    @Test
    fun `the retry uses the same idempotency key as the first attempt`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        // Primer intento de subida, todavia sin red.
        uploader.uploadAll()

        offline.movementSpy.failure = null
        uploader.uploadAll()

        // Lo que la API vio en los dos intentos tiene que ser la misma clave, y la del registro.
        assertTrue(offline.movementSpy.keys.isNotEmpty())
        assertEquals(listOf(KEY, KEY, KEY), offline.movementSpy.keys)
    }

    @Test
    fun `a timeout that did arrive does not duplicate the movement`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        offline.movementSpy.failure = null
        // La API contesta que ya lo tenia: es un reintento de algo ya creado.
        offline.movementSpy.replay = true

        uploader.uploadAll()

        val stored = offline.store.movements.between(FROM, TO)
        assertEquals(1, stored.size)
        assertFalse(stored.single().pending)
    }

    @Test
    fun `the provisional record is swapped for the real one`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        val provisional = offline.store.movements.between(FROM, TO).single().id

        offline.movementSpy.failure = null
        uploader.uploadAll()

        val stored = offline.store.movements.between(FROM, TO).single()
        assertEquals(SampleFinance.groceries.id, stored.id)
        assertTrue(stored.id != provisional)
    }

    @Test
    fun `no network stops the batch and leaves everything pending`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        offline.movements.create(MOVEMENT.copy(note = "otro"), IdempotencyKey("key-2"))

        val outcome = uploader.uploadAll()

        assertEquals(0, outcome.uploaded)
        assertTrue(outcome.retryLater)
        assertEquals(2, offline.store.pending.count())
    }

    @Test
    fun `a rejected write stops retrying and says why`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        offline.movementSpy.failure = ApiError.Validation(
            listOf(FieldError(field = "amount", message = "El importe tiene que ser mayor que cero")),
        )

        uploader.uploadAll()

        val write = offline.store.pending.all().single()
        assertTrue(write.needsFixing)
        assertEquals("El importe tiene que ser mayor que cero", write.failure)
    }

    @Test
    fun `a broken write is skipped on the next round`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        offline.movementSpy.failure = ApiError.Validation(emptyList())
        uploader.uploadAll()
        val attempts = offline.movementSpy.createCalls

        offline.movementSpy.failure = null
        uploader.uploadAll()

        assertEquals(attempts, offline.movementSpy.createCalls)
    }

    @Test
    fun `another attempt in flight is waited for, not given up on`() = runTest {
        offline.categories.list()
        offline.movementSpy.failure = ApiError.Network
        offline.movements.create(MOVEMENT, KEY)
        offline.movementSpy.failure =
            ApiError.Conflict(code = ConflictCode.IDEMPOTENCY_KEY_IN_PROGRESS)

        val outcome = uploader.uploadAll()

        assertTrue(outcome.retryLater)
        assertFalse(offline.store.pending.all().single().needsFixing)
    }

    @Test
    fun `a pending loan goes up before its payment`() = runTest {
        offline.categories.list()
        offline.loanSpy.failure = ApiError.Network
        offline.loans.create(LOAN, KEY)
        val loanId = offline.store.loans.all().single().id
        offline.loans.addPayment(loanId, PAYMENT, IdempotencyKey("key-2"))

        offline.loanSpy.failure = null
        // La primera vuelta solo puede subir el prestamo: el abono todavia depende de el.
        uploader.uploadAll()
        val afterFirst = offline.store.pending.all()
        uploader.uploadAll()

        assertEquals(1, afterFirst.size)
        assertEquals(0, offline.store.pending.count())
    }

    private companion object {
        val KEY = IdempotencyKey("key-1")
        val FROM: LocalDate = LocalDate.parse("2026-09-01")
        val TO: LocalDate = LocalDate.parse("2026-09-30")
        val MONEY = MonetaryAmount.single(BigDecimal("1000"), CurrencyCode.COP)

        val MOVEMENT = MovementDraft(
            occurredOn = LocalDate.parse("2026-09-22"),
            kind = MovementKind.OUT,
            categoryId = SampleFinance.food.id,
            money = MONEY,
            note = "Mercado",
        )

        val LOAN = LoanDraft(
            person = "Rosa",
            direction = LoanDirection.LENT,
            openedOn = LocalDate.parse("2026-09-22"),
            money = MONEY,
        )

        val PAYMENT = LoanPaymentDraft(paidOn = LocalDate.parse("2026-09-22"), money = MONEY)
    }
}
