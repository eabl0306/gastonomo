package com.bahoque.gastonomo.data.finance

import com.bahoque.gastonomo.core.application.port.IdempotencyKey
import com.bahoque.gastonomo.core.application.port.LoanDraft
import com.bahoque.gastonomo.core.application.port.LoanPaymentDraft
import com.bahoque.gastonomo.core.application.port.MovementDraft
import com.bahoque.gastonomo.core.common.Outcome
import com.bahoque.gastonomo.core.domain.category.CategoryId
import com.bahoque.gastonomo.core.domain.loan.LoanDirection
import com.bahoque.gastonomo.core.domain.loan.LoanId
import com.bahoque.gastonomo.core.domain.money.CurrencyCode
import com.bahoque.gastonomo.core.domain.money.MonetaryAmount
import com.bahoque.gastonomo.core.domain.movement.MovementKind
import com.bahoque.gastonomo.core.testing.fake.FakeSessionStore
import com.bahoque.gastonomo.core.testing.fake.SampleAuth
import com.bahoque.gastonomo.data.network.ApiConfig
import com.bahoque.gastonomo.data.network.createApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.content.TextContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * El mapeo del dinero, que es lo que no puede fallar: los importes viajan como cadena y se
 * leen a BigDecimal, y `base` y `baseCurrency` se omiten los dos juntos o ninguno.
 */
class FinanceGatewaysTest {

    @Test
    fun `a movement keeps every digit of its amount`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createMovementGateway(clientOf(requests) { respondJson(MOVEMENT_PAGE) })
        val page = (gateway.list(FROM, TO) as Outcome.Success).value
        val movement = page.items.single()
        assertEquals(BigDecimal("186400.0000"), movement.money.amount)
        assertEquals(CurrencyCode.COP, movement.money.currency)
        assertEquals("cursor-1", page.nextCursor)
    }

    @Test
    fun `a movement carries its whole category`() = runTest {
        val gateway = createMovementGateway(clientOf(mutableListOf()) { respondJson(MOVEMENT_PAGE) })
        val movement = (gateway.list(FROM, TO) as Outcome.Success).value.items.single()
        assertEquals("Alimentación", movement.category.name)
        assertEquals("restaurant", movement.category.icon)
        assertEquals("#cf5f35", movement.category.color)
    }

    @Test
    fun `a movement without an equivalent uses its own amount`() = runTest {
        val gateway = createMovementGateway(clientOf(mutableListOf()) { respondJson(MOVEMENT_NO_BASE) })
        val movement = (gateway.list(FROM, TO) as Outcome.Success).value.items.single()
        assertEquals(movement.money.amount, movement.money.base)
        assertEquals(movement.money.currency, movement.money.baseCurrency)
    }

    @Test
    fun `the period and the filter travel as query parameters`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createMovementGateway(clientOf(requests) { respondJson(MOVEMENT_PAGE) })
        gateway.list(FROM, TO, MovementKind.OUT, "cursor-1", 4)
        val query = requests.single().url.parameters
        assertEquals("2026-09-01", query["from"])
        assertEquals("2026-09-30", query["to"])
        assertEquals("out", query["kind"])
        assertEquals("cursor-1", query["cursor"])
        assertEquals("4", query["limit"])
    }

    @Test
    fun `a movement in the main currency omits both equivalence fields`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createMovementGateway(clientOf(requests) { respondJson(MOVEMENT) })
        gateway.create(movementDraft(MonetaryAmount.single(AMOUNT, CurrencyCode.COP)), KEY)
        val body = requests.single().bodyText()
        assertTrue(!body.contains("\"base\""))
        assertTrue(!body.contains("\"baseCurrency\""))
        assertEquals(KEY.value, requests.single().headers["Idempotency-Key"])
    }

    @Test
    fun `a movement in another currency sends both equivalence fields`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createMovementGateway(clientOf(requests) { respondJson(MOVEMENT) })
        val money = MonetaryAmount(
            amount = BigDecimal("140"),
            currency = CurrencyCode.USD,
            base = BigDecimal("450000"),
            baseCurrency = CurrencyCode.COP,
        )
        gateway.create(movementDraft(money), KEY)
        val body = requests.single().bodyText()
        assertTrue(body.contains("\"base\":\"450000\""))
        assertTrue(body.contains("\"baseCurrency\":\"COP\""))
    }

    @Test
    fun `an empty note is not sent so the api titles the movement`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createMovementGateway(clientOf(requests) { respondJson(MOVEMENT) })
        gateway.create(movementDraft(MonetaryAmount.single(AMOUNT, CurrencyCode.COP), note = "  "), KEY)
        assertTrue(!requests.single().bodyText().contains("\"note\""))
    }

    @Test
    fun `a loan arrives with its payments embedded`() = runTest {
        val gateway = createLoanGateway(clientOf(mutableListOf()) { respondJson(LOAN_LIST) })
        val loan = (gateway.list() as Outcome.Success).value.single()
        assertEquals("Rosa", loan.person)
        assertEquals(LoanDirection.LENT, loan.direction)
        assertEquals(1, loan.payments.size)
        assertEquals(BigDecimal("200000.0000"), loan.payments.single().money.amount)
        assertEquals("Intereses", loan.payments.single().label)
    }

    @Test
    fun `a loan without a note reads as an empty note`() = runTest {
        val gateway = createLoanGateway(clientOf(mutableListOf()) { respondJson(LOAN_DETAIL) })
        val loan = (gateway.detail(LoanId("l1")) as Outcome.Success).value
        assertEquals("", loan.note)
        assertTrue(loan.payments.isEmpty())
    }

    @Test
    fun `the direction travels as a query parameter`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val gateway = createLoanGateway(clientOf(requests) { respondJson(LOAN_LIST) })
        gateway.list(LoanDirection.BORROWED)
        assertEquals("borrowed", requests.single().url.parameters["direction"])
        assertNull(requests.single().url.parameters["other"])
    }

    @Test
    fun `creating a loan and a payment send their idempotency key`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = clientOf(requests) { request ->
            val isPayment = request.url.encodedPath.endsWith("payments")
            respondJson(if (isPayment) PAYMENT else LOAN_DETAIL)
        }
        val gateway = createLoanGateway(client)
        gateway.create(
            LoanDraft(
                person = "Rosa",
                direction = LoanDirection.LENT,
                openedOn = FROM,
                money = MonetaryAmount.single(AMOUNT, CurrencyCode.COP),
                note = "  ",
            ),
            KEY,
        )
        gateway.addPayment(
            LoanId("l1"),
            LoanPaymentDraft(FROM, MonetaryAmount.single(AMOUNT, CurrencyCode.COP)),
            IdempotencyKey("k2"),
        )
        assertEquals(listOf("k1", "k2"), requests.map { it.headers["Idempotency-Key"] })
    }

    private fun movementDraft(money: MonetaryAmount, note: String? = "Mercado") = MovementDraft(
        occurredOn = FROM,
        kind = MovementKind.OUT,
        categoryId = CategoryId("food"),
        money = money,
        note = note,
    )

    private fun clientOf(
        requests: MutableList<HttpRequestData>,
        handler: MockHandler,
    ) = createApiClient(
        sessionStore = FakeSessionStore(SampleAuth.session),
        config = ApiConfig(baseUrl = "https://api.test", loggingEnabled = false),
        engine = MockEngine { request ->
            requests += request
            handler(request)
        },
    )

    private fun HttpRequestData.bodyText(): String = (body as? TextContent)?.text.orEmpty()

    private companion object {
        val FROM: LocalDate = LocalDate.of(2026, 9, 1)
        val TO: LocalDate = LocalDate.of(2026, 9, 30)
        val AMOUNT: BigDecimal = BigDecimal("186400")
        val KEY = IdempotencyKey("k1")

        const val CATEGORY =
            """{"id":"c1","name":"Alimentación","kind":"out","icon":"restaurant","color":"#cf5f35"}"""

        const val MOVEMENT = """{"id":"m1","occurredOn":"2026-09-15","kind":"out","title":"Mercado",
            "amount":"186400.0000","currency":"COP","base":"186400.0000","baseCurrency":"COP",
            "category":$CATEGORY}"""

        const val MOVEMENT_PAGE = """{"items":[$MOVEMENT],"nextCursor":"cursor-1"}"""

        const val MOVEMENT_NO_BASE = """{"items":[{"id":"m2","occurredOn":"2026-09-15","kind":"in",
            "title":"Salario","amount":"2800000.0000","currency":"COP","category":$CATEGORY}],
            "nextCursor":null}"""

        const val PAYMENT = """{"id":"p1","loanId":"l1","paidOn":"2026-07-29","label":"Intereses",
            "amount":"200000.0000","currency":"COP","base":"200000.0000","baseCurrency":"COP"}"""

        const val LOAN_LIST = """{"items":[{"id":"l1","person":"Rosa","note":"Teléfono",
            "direction":"lent","openedOn":"2026-06-28","amount":"600000.0000","currency":"COP",
            "base":"600000.0000","baseCurrency":"COP","payments":[$PAYMENT]}]}"""

        const val LOAN_DETAIL = """{"id":"l1","person":"Rosa","direction":"lent",
            "openedOn":"2026-06-28","amount":"600000.0000","currency":"COP"}"""

        fun MockRequestHandleScope.respondJson(body: String) =
            respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
    }
}

/** Lo que el MockEngine hace con cada peticion. El tipo completo no cabe en una linea. */
private typealias MockHandler =
    suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData
