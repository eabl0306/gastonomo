package com.bahoque.gastonomo.data.database

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.application.port.PendingDraft
import com.bahoque.gastonomo.core.application.port.PendingWrite
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import com.bahoque.gastonomo.data.database.local.LocalStore
import com.bahoque.gastonomo.data.database.local.createInMemoryLocalStore
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * La cola de escrituras sin subir.
 *
 * Lo que se comprueba aqui es lo que, si falla, le cuesta dinero al usuario: que la clave de
 * idempotencia se conserve, que el orden se respete, y sobre todo que **un refresco no se lleve
 * por delante lo que todavia no ha salido del telefono**.
 */
@RunWith(AndroidJUnit4::class)
class PendingWritesTest {

    private lateinit var store: LocalStore

    @Before
    fun open() {
        store = createInMemoryLocalStore(ApplicationProvider.getApplicationContext<Context>())
    }

    @After
    fun close() {
        store.close()
    }

    @Test
    fun `a movement draft comes back exactly as it went in`() = runTest {
        store.pending.enqueue(PendingWrite("w1", KEY, PendingDraft.Movement(MOVEMENT)))

        val stored = store.pending.all().single()

        assertEquals(KEY, stored.idempotencyKey)
        assertEquals(PendingDraft.Movement(MOVEMENT), stored.draft)
    }

    @Test
    fun `the amount keeps its scale through the queue`() = runTest {
        val exact = MonetaryAmount.single(BigDecimal("186400.0000"), CurrencyCode.COP)
        store.pending.enqueue(
            PendingWrite("w1", KEY, PendingDraft.Movement(MOVEMENT.copy(money = exact))),
        )

        val draft = store.pending.all().single().draft as PendingDraft.Movement

        assertEquals("186400.0000", draft.value.money.amount.toPlainString())
    }

    @Test
    fun `loans and payments go through too`() = runTest {
        store.pending.enqueue(PendingWrite("w1", KEY, PendingDraft.Loan(LOAN)))
        store.pending.enqueue(PendingWrite("w2", KEY, PendingDraft.Payment(PAYMENT)))

        val drafts = store.pending.all().map { it.draft }

        assertEquals(listOf(PendingDraft.Loan(LOAN), PendingDraft.Payment(PAYMENT)), drafts)
    }

    @Test
    fun `the queue keeps the order in which the user registered`() = runTest {
        listOf("a", "b", "c").forEach {
            store.pending.enqueue(PendingWrite(it, KEY, PendingDraft.Movement(MOVEMENT)))
        }

        assertEquals(listOf("a", "b", "c"), store.pending.all().map { it.id })
    }

    @Test
    fun `correcting a broken write keeps its idempotency key`() = runTest {
        store.pending.enqueue(PendingWrite("w1", KEY, PendingDraft.Movement(MOVEMENT)))
        store.pending.markFailed("w1", "importe invalido")

        store.pending.replaceDraft("w1", PendingDraft.Movement(MOVEMENT.copy(note = "corregido")))

        val stored = store.pending.all().single()
        // Un 422 no quema la clave: corregir y reintentar con la misma es lo correcto (ADR 0012).
        assertEquals(KEY, stored.idempotencyKey)
        assertNull(stored.failure)
        assertEquals("corregido", (stored.draft as PendingDraft.Movement).value.note)
    }

    @Test
    fun `a payment waits for its loan and then points at the real one`() = runTest {
        store.pending.enqueue(PendingWrite("loan", KEY, PendingDraft.Loan(LOAN)))
        store.pending.enqueue(
            PendingWrite("pay", KEY, PendingDraft.Payment(PAYMENT), dependsOn = "loan"),
        )

        store.pending.resolveDependency("loan", LoanId("real-1"))

        val payment = store.pending.all().first { it.id == "pay" }
        assertNull(payment.dependsOn)
        assertEquals(LoanId("real-1"), payment.loanId)
    }

    @Test
    fun `a refresh does not delete a movement that has not been sent`() = runTest {
        val pending = SampleFinance.groceries.copy(pending = true)
        store.movements.save(pending)

        // Esto es lo que hace un refresco: reemplazar el periodo con lo que trae la API.
        store.movements.replace(FROM, TO, SampleFinance.movements)

        val ids = store.movements.between(FROM, TO).map { it.id }
        assertTrue(pending.id in ids, "se perdio un movimiento sin subir")
    }

    @Test
    fun `a refresh does not delete a loan or a payment that have not been sent`() = runTest {
        val loan = SampleFinance.partialLoan.copy(
            id = LoanId("local-1"),
            pending = true,
            payments = emptyList(),
        )
        store.loans.save(loan)
        store.loans.save(SampleFinance.paidLoan)
        val payment = SampleFinance.payment.copy(
            id = com.bahoque.gastonomo.core.domain.loan.LoanPaymentId("local-p"),
            loanId = SampleFinance.paidLoan.id,
            pending = true,
        )
        store.loans.savePayment(payment)

        store.loans.replaceAll(listOf(SampleFinance.paidLoan))

        val stored = store.loans.all()
        assertTrue(stored.any { it.id == LoanId("local-1") }, "se perdio un prestamo sin subir")
        assertTrue(
            stored.first { it.id == SampleFinance.paidLoan.id }.payments.any { it.pending },
            "se perdio un abono sin subir",
        )
    }

    @Test
    fun `removing one takes it out of the queue`() = runTest {
        store.pending.enqueue(PendingWrite("w1", KEY, PendingDraft.Movement(MOVEMENT)))

        store.pending.remove("w1")

        assertEquals(0, store.pending.count())
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
            note = null,
        )

        val PAYMENT = LoanPaymentDraft(
            paidOn = LocalDate.parse("2026-09-22"),
            money = MONEY,
            label = null,
        )
    }
}
