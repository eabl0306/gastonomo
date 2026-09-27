package com.bahoque.gastonomo.data.cache

import com.bahoque.gastonomo.core.application.error.ApiError
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import kotlinx.coroutines.test.runTest
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Movimientos y prestamos con la cache delante. Lo que se comprueba aqui no es el plazo, que
 * ya tiene sus tests, sino **que cada escritura tira exactamente lo que dejo desfasado**.
 *
 * Esa es la parte que se rompe en silencio: si falta una invalidacion, la pantalla sigue
 * ensenando el saldo de antes y nadie ve un error.
 */
class CachedFinanceGatewaysTest {

    @Test
    fun `going back to a list does not ask again`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.movements.list(FROM, TO)
        wired.movements.list(FROM, TO)

        assertEquals(1, wired.movementSpy.listCalls)
    }

    @Test
    fun `a different period is a different question`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.movements.list(FROM, TO)
        wired.movements.list(FROM.minusMonths(1), TO.minusMonths(1))

        assertEquals(2, wired.movementSpy.listCalls)
    }

    @Test
    fun `the filter and the limit are part of the key`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.movements.list(FROM, TO)
        wired.movements.list(FROM, TO, kind = MovementKind.OUT)
        wired.movements.list(FROM, TO, limit = RECENT)

        assertEquals(3, wired.movementSpy.listCalls)
    }

    @Test
    fun `creating a movement refreshes the list and the categories`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)
        wired.categories.list()

        wired.movements.create(Drafts.movement, Drafts.key)

        wired.movements.list(FROM, TO)
        // `GET /v1/categories` devuelve el conteo de uso, asi que un movimiento nuevo tambien
        // lo deja desfasado.
        wired.categories.list()
        assertEquals(2, wired.movementSpy.listCalls)
        assertEquals(2, wired.categorySpy.listCalls)
    }

    @Test
    fun `a rejected creation does not throw the cache away`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)
        wired.movementSpy.failure = ApiError.Validation(emptyList())

        wired.movements.create(Drafts.movement, Drafts.key)

        wired.movementSpy.failure = null
        wired.movements.list(FROM, TO)
        // Un 422 no cambio nada en el servidor: recargar seria una peticion de mas.
        assertEquals(1, wired.movementSpy.listCalls)
    }

    @Test
    fun `the loan list and the loan detail are cached apart`() = runTest {
        val wired = WiredCaches(backgroundScope)

        wired.loans.list()
        wired.loans.list()
        wired.loans.list(LoanDirection.LENT)
        wired.loans.detail(LOAN)
        wired.loans.detail(LOAN)

        assertEquals(2, wired.loanSpy.listCalls)
        assertEquals(1, wired.loanSpy.detailCalls)
    }

    @Test
    fun `a payment refreshes the loan, the list and the movements`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.loans.list()
        wired.loans.detail(LOAN)
        wired.movements.list(FROM, TO)

        wired.loans.addPayment(LOAN, Drafts.payment, Drafts.key)

        wired.loans.list()
        wired.loans.detail(LOAN)
        wired.movements.list(FROM, TO)
        assertEquals(2, wired.loanSpy.listCalls)
        assertEquals(2, wired.loanSpy.detailCalls)
        // El abono crea tambien su propio movimiento.
        assertEquals(2, wired.movementSpy.listCalls)
    }

    @Test
    fun `creating a loan refreshes the movements too`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.movements.list(FROM, TO)
        wired.loans.list()

        wired.loans.create(Drafts.loan, Drafts.key)

        wired.movements.list(FROM, TO)
        wired.loans.list()
        assertEquals(2, wired.movementSpy.listCalls)
        assertEquals(2, wired.loanSpy.listCalls)
    }

    @Test
    fun `a rejected payment does not throw the cache away`() = runTest {
        val wired = WiredCaches(backgroundScope)
        wired.loans.list()
        wired.loanSpy.failure = ApiError.NotFound

        wired.loans.addPayment(LOAN, Drafts.payment, Drafts.key)

        wired.loanSpy.failure = null
        wired.loans.list()
        assertEquals(1, wired.loanSpy.listCalls)
    }

    private companion object {
        val FROM = LocalDate.parse("2026-09-01")
        val TO = LocalDate.parse("2026-09-30")
        val LOAN = LoanId("l1")

        /** Los cuatro ultimos que ensena Inicio. */
        const val RECENT = 4
    }
}
