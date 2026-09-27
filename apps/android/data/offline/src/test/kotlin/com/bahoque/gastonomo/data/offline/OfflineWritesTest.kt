package com.bahoque.gastonomo.data.offline

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.application.port.CategoryDraft
import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.domain.category.CategoryKind
import com.bahoque.gastonomo.core.domain.loan.LoanPaymentId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.SampleFinance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Que escribir deje la base contando la verdad.
 *
 * Es la parte que se rompe en silencio: si una escritura no guarda lo que creo o no olvida lo que
 * dejo desfasado, la pantalla sigue ensenando el numero de antes y nadie ve un error.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class OfflineWritesTest {

    private val scope = TestScope()

    @get:Rule
    val offline = OfflineRule(scope.backgroundScope)

    @Test
    fun `a new movement lands in the base without going back to the network`() = runTest {
        offline.movements.list(FROM, TO)
        offline.movementSpy.created = SampleFinance.groceries.copy(id = MovementId("nuevo"))

        offline.movements.create(DRAFT, KEY)

        val stored = offline.store.movements.between(FROM, TO).map { it.id.value }
        assertTrue("nuevo" in stored, "el movimiento creado no se guardo: $stored")
    }

    @Test
    fun `a new movement makes the categories stale, because their count changed`() = runTest {
        offline.categories.list()
        val before = offline.categorySpy.listCalls

        offline.movements.create(DRAFT, KEY)
        offline.categories.list()
        scope.runCurrent()

        // `GET /v1/categories` devuelve el conteo de uso: un movimiento nuevo lo deja desfasado.
        assertTrue(offline.categorySpy.listCalls > before)
    }

    @Test
    fun `a rejected creation changes nothing`() = runTest {
        offline.movements.list(FROM, TO)
        val stored = offline.store.movements.between(FROM, TO).size
        offline.movementSpy.failure = ApiError.Validation(emptyList())

        offline.movements.create(DRAFT, KEY)

        assertEquals(stored, offline.store.movements.between(FROM, TO).size)
    }

    @Test
    fun `a payment leaves the loan with its new balance in the base`() = runTest {
        offline.loans.list()
        val loan = SampleFinance.partialLoan
        offline.loanSpy.detailPayments = loan.payments + SampleFinance.payment.copy(id = LoanPaymentId("p9"))

        offline.loans.addPayment(loan.id, PAYMENT, KEY)

        val stored = offline.store.loans.byId(loan.id)!!
        assertEquals(2, stored.payments.size)
    }

    @Test
    fun `changing the main currency does not touch the records`() = runTest {
        offline.movements.list(FROM, TO)
        offline.loans.list()
        val movements = offline.store.movements.between(FROM, TO)
        val loans = offline.store.loans.all()

        offline.settings.update(displayCurrency = CurrencyCode.USD)

        // Los importes se guardan sin convertir y la conversion se hace al pintar: cambiar la
        // moneda cambia como se muestra, no lo que hay.
        assertEquals(movements, offline.store.movements.between(FROM, TO))
        assertEquals(loans, offline.store.loans.all())
        assertEquals(CurrencyCode.USD, offline.store.account.settings()!!.displayCurrency)
    }

    @Test
    fun `writing a category brings the list down again`() = runTest {
        offline.categories.list()
        val before = offline.categorySpy.listCalls

        offline.categories.create(CATEGORY)
        scope.runCurrent()

        assertTrue(offline.categorySpy.listCalls > before)
    }

    @Test
    fun `signing out leaves nothing of the account on disk`() = runTest {
        offline.movements.list(FROM, TO)
        offline.loans.list()
        offline.categories.list()
        offline.settings.load()
        offline.auth.profile()

        offline.auth.logout()

        assertTrue(offline.store.movements.between(FROM, TO).isEmpty())
        assertTrue(offline.store.loans.all().isEmpty())
        assertTrue(offline.store.categories.all().isEmpty())
        assertNull(offline.store.account.settings())
        assertNull(offline.store.account.profile())
    }

    @Test
    fun `signing in starts from an empty base`() = runTest {
        offline.movements.list(FROM, TO)

        offline.auth.login("camilo@correo.com", "secreta")

        assertTrue(offline.store.movements.between(FROM, TO).isEmpty())
    }

    private companion object {
        val FROM: LocalDate = LocalDate.parse("2026-09-01")
        val TO: LocalDate = LocalDate.parse("2026-09-30")
        val KEY = IdempotencyKey("key-1")

        val DRAFT = MovementDraft(
            occurredOn = LocalDate.parse("2026-09-22"),
            kind = MovementKind.OUT,
            categoryId = SampleFinance.food.id,
            money = MonetaryAmount.single(BigDecimal("1000"), CurrencyCode.COP),
        )

        val PAYMENT = LoanPaymentDraft(
            paidOn = LocalDate.parse("2026-09-22"),
            money = MonetaryAmount.single(BigDecimal("500"), CurrencyCode.COP),
        )

        val CATEGORY = CategoryDraft(
            name = "Mascotas",
            kind = CategoryKind.OUT,
            icon = "pets",
            color = "#2f7fb5",
        )
    }
}
